package com.example.domain.backup

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

data class BackupRestoreSummary(
    val coursesCount: Int,
    val tasksCount: Int,
    val examsCount: Int,
    val notesCount: Int,
    val remindersCount: Int,
    val backupTimestamp: Long
)

/**
 * 100% Offline Backup & Restore Engine.
 * Serializes all user data with SHA-256 checksum integrity verification.
 */
object OfflineBackupManager {

    private const val BACKUP_VERSION = 1

    suspend fun createBackupJson(database: AppDatabase): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "CourseScheduleOffline")

        val dataObj = JSONObject()

        // 1. Courses
        val courses = database.courseDao().getAllCourses().first()
        val coursesArray = JSONArray()
        courses.forEach { c ->
            val obj = JSONObject().apply {
                put("name", c.name)
                put("code", c.code)
                put("instructor", c.instructor)
                put("classroom", c.classroom)
                put("dayOfWeek", c.dayOfWeek)
                put("startPeriod", c.startPeriod)
                put("endPeriod", c.endPeriod)
                put("startTime", c.startTime)
                put("endTime", c.endTime)
                put("colorHex", c.colorHex)
                put("weekRule", c.weekRule.name)
                put("semesterId", c.semesterId)
                put("isRetake", c.isRetake)
                put("notes", c.notes)
            }
            coursesArray.put(obj)
        }
        dataObj.put("courses", coursesArray)

        // 2. Tasks
        val tasks = database.taskDao().getAllTasks().first()
        val tasksArray = JSONArray()
        tasks.forEach { t ->
            val obj = JSONObject().apply {
                put("title", t.title)
                put("courseName", t.courseName)
                put("dueDate", t.dueDate)
                put("priority", t.priority)
                put("isCompleted", t.isCompleted)
            }
            tasksArray.put(obj)
        }
        dataObj.put("tasks", tasksArray)

        // 3. Exams
        val exams = database.examDao().getAllExams().first()
        val examsArray = JSONArray()
        exams.forEach { e ->
            val obj = JSONObject().apply {
                put("courseName", e.courseName)
                put("classroom", e.classroom)
                put("examDateMillis", e.examDateMillis)
                put("startTime", e.startTime)
                put("endTime", e.endTime)
                put("seatNumber", e.seatNumber)
                put("notes", e.notes)
            }
            examsArray.put(obj)
        }
        dataObj.put("exams", examsArray)

        // 4. Notes
        val notes = database.noteDao().getAllNotes().first()
        val notesArray = JSONArray()
        notes.forEach { n ->
            val obj = JSONObject().apply {
                put("title", n.title)
                put("content", n.content)
                put("courseId", n.courseId)
                put("drawingDataJson", n.drawingDataJson ?: "")
                put("createdAtMillis", n.createdAtMillis)
                put("updatedAtMillis", n.updatedAtMillis)
            }
            notesArray.put(obj)
        }
        dataObj.put("notes", notesArray)

        // 5. Reminders
        val reminders = database.reminderDao().getAllReminders().first()
        val remindersArray = JSONArray()
        reminders.forEach { r ->
            val obj = JSONObject().apply {
                put("title", r.title)
                put("description", r.description)
                put("triggerTimeMillis", r.triggerTimeMillis)
                put("repeatInterval", r.repeatInterval)
                put("relatedType", r.relatedType)
                put("relatedId", r.relatedId)
                put("isEnabled", r.isEnabled)
            }
            remindersArray.put(obj)
        }
        dataObj.put("reminders", remindersArray)

        val rawDataString = dataObj.toString()
        val checksum = calculateSha256(rawDataString)

        root.put("data", dataObj)
        root.put("sha256", checksum)

        return root.toString(2)
    }

    suspend fun restoreBackupJson(
        backupJsonString: String,
        database: AppDatabase,
        replaceExisting: Boolean
    ): Result<BackupRestoreSummary> {
        return try {
            val root = JSONObject(backupJsonString)
            if (!root.has("data") || !root.has("sha256")) {
                return Result.failure(IllegalArgumentException("Invalid backup file: Missing payload or checksum."))
            }

            val expectedSha256 = root.getString("sha256")
            val dataObj = root.getJSONObject("data")
            val actualSha256 = calculateSha256(dataObj.toString())

            if (!expectedSha256.equals(actualSha256, ignoreCase = true)) {
                return Result.failure(IllegalStateException("Integrity Check Failed: SHA-256 checksum does not match. File may be corrupted or tampered with."))
            }

            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            if (replaceExisting) {
                database.courseDao().deleteAllCourses()
            }

            // Restore Courses
            var coursesRestored = 0
            val coursesArray = dataObj.optJSONArray("courses") ?: JSONArray()
            for (i in 0 until coursesArray.length()) {
                val obj = coursesArray.getJSONObject(i)
                val colorVal = try {
                    val rawCol = obj.opt("colorHex")
                    when (rawCol) {
                        is Number -> rawCol.toLong()
                        is String -> {
                            val clean = rawCol.removePrefix("#")
                            if (clean.length == 6) 0xFF000000 or clean.toLong(16) else clean.toLong(16)
                        }
                        else -> 0xFF4F46E5
                    }
                } catch (e: Exception) {
                    0xFF4F46E5
                }
                val course = CourseEntity(
                    id = 0,
                    name = obj.getString("name"),
                    code = obj.optString("code", ""),
                    instructor = obj.optString("instructor", ""),
                    classroom = obj.optString("classroom", ""),
                    dayOfWeek = obj.getInt("dayOfWeek"),
                    startPeriod = obj.getInt("startPeriod"),
                    endPeriod = obj.getInt("endPeriod"),
                    startTime = obj.optString("startTime", "08:00"),
                    endTime = obj.optString("endTime", "09:35"),
                    colorHex = colorVal,
                    weekRule = try {
                        WeekRule.valueOf(obj.optString("weekRule", "ALL"))
                    } catch (e: Exception) {
                        WeekRule.ALL
                    },
                    semesterId = obj.optLong("semesterId", 1L),
                    isRetake = obj.optBoolean("isRetake", false),
                    notes = obj.optString("notes", "")
                )
                database.courseDao().insertCourse(course)
                coursesRestored++
            }

            // Restore Tasks
            var tasksRestored = 0
            val tasksArray = dataObj.optJSONArray("tasks") ?: JSONArray()
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                val task = Task(
                    id = 0,
                    title = obj.getString("title"),
                    courseName = obj.optString("courseName", ""),
                    dueDate = obj.optString("dueDate", ""),
                    priority = obj.optString("priority", "MEDIUM"),
                    isCompleted = obj.optBoolean("isCompleted", false)
                )
                database.taskDao().insertTask(task)
                tasksRestored++
            }

            // Restore Exams
            var examsRestored = 0
            val examsArray = dataObj.optJSONArray("exams") ?: JSONArray()
            for (i in 0 until examsArray.length()) {
                val obj = examsArray.getJSONObject(i)
                val exam = ExamEntity(
                    id = 0,
                    courseName = obj.getString("courseName"),
                    classroom = obj.optString("classroom", ""),
                    examDateMillis = obj.optLong("examDateMillis", System.currentTimeMillis()),
                    startTime = obj.optString("startTime", "09:00"),
                    endTime = obj.optString("endTime", "11:00"),
                    seatNumber = obj.optString("seatNumber", ""),
                    notes = obj.optString("notes", "")
                )
                database.examDao().insertExam(exam)
                examsRestored++
            }

            // Restore Notes
            var notesRestored = 0
            val notesArray = dataObj.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                val drawingJson = obj.optString("drawingDataJson").ifBlank { null }
                val note = NoteEntity(
                    id = 0,
                    title = obj.getString("title"),
                    content = obj.optString("content", ""),
                    courseId = obj.optLong("courseId", 0L),
                    drawingDataJson = drawingJson,
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                    updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                )
                database.noteDao().insertNote(note)
                notesRestored++
            }

            // Restore Reminders
            var remindersRestored = 0
            val remindersArray = dataObj.optJSONArray("reminders") ?: JSONArray()
            for (i in 0 until remindersArray.length()) {
                val obj = remindersArray.getJSONObject(i)
                val reminder = ReminderEntity(
                    id = 0,
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    triggerTimeMillis = obj.getLong("triggerTimeMillis"),
                    repeatInterval = obj.optString("repeatInterval", "NONE"),
                    relatedType = obj.optString("relatedType", "GENERAL"),
                    relatedId = obj.optLong("relatedId", 0L),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
                database.reminderDao().insertReminder(reminder)
                remindersRestored++
            }

            Result.success(
                BackupRestoreSummary(
                    coursesCount = coursesRestored,
                    tasksCount = tasksRestored,
                    examsCount = examsRestored,
                    notesCount = notesRestored,
                    remindersCount = remindersRestored,
                    backupTimestamp = timestamp
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun calculateSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
