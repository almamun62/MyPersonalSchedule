package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class ScheduleRepository(val database: AppDatabase) {
    val activeSemester: Flow<SemesterEntity?> = database.semesterDao().getActiveSemester()
    val allSemesters: Flow<List<SemesterEntity>> = database.semesterDao().getAllSemesters()
    val allTasks: Flow<List<TaskEntity>> = database.taskDao().getAllTasks()
    val pendingTasks: Flow<List<TaskEntity>> = database.taskDao().getPendingTasks()
    val allHolidayOverrides: Flow<List<HolidayOverrideEntity>> = database.holidayOverrideDao().getAllHolidayOverrides()
    val allNotes: Flow<List<com.example.data.model.NoteEntity>> = database.noteDao().getAllNotes()

    fun getCoursesBySemester(semesterId: Long): Flow<List<CourseEntity>> =
        database.courseDao().getCoursesBySemester(semesterId)

    fun getExamsBySemester(semesterId: Long): Flow<List<ExamEntity>> =
        database.examDao().getExamsBySemester(semesterId)

    suspend fun getActiveSemesterSync(): SemesterEntity? =
        database.semesterDao().getActiveSemesterSync()

    suspend fun getCoursesBySemesterSync(semesterId: Long): List<CourseEntity> =
        database.courseDao().getCoursesBySemesterSync(semesterId)

    suspend fun getAllHolidayOverridesSync(): List<HolidayOverrideEntity> =
        database.holidayOverrideDao().getAllHolidayOverridesSync()

    suspend fun insertSemester(semester: SemesterEntity): Long =
        database.semesterDao().insertSemester(semester)

    suspend fun updateSemester(semester: SemesterEntity) =
        database.semesterDao().updateSemester(semester)

    suspend fun setActiveSemester(id: Long) =
        database.semesterDao().setActiveSemester(id)

    suspend fun insertCourse(course: CourseEntity): Long =
        database.courseDao().insertCourse(course)

    suspend fun insertCourses(courses: List<CourseEntity>) =
        database.courseDao().insertCourses(courses)

    suspend fun updateCourse(course: CourseEntity) =
        database.courseDao().updateCourse(course)

    suspend fun deleteCourse(course: CourseEntity) =
        database.courseDao().deleteCourse(course)

    suspend fun clearCoursesBySemester(semesterId: Long) =
        database.courseDao().deleteCoursesBySemester(semesterId)

    suspend fun deduplicateCourses(semesterId: Long) {
        val all = database.courseDao().getCoursesBySemesterSync(semesterId)
        val seen = mutableSetOf<String>()
        val duplicatesToDelete = mutableListOf<CourseEntity>()

        for (c in all) {
            // Unify course duplicate identification by name or code and schedule timing
            val normName = c.name.replace(Regex("""[\[\(（【]国际学生[\]\)）】]"""), "").trim().lowercase()
            val timingKey = "${c.dayOfWeek}_${c.startPeriod}_${c.endPeriod}_${c.customWeeks.trim()}"
            val key = if (c.code.isNotBlank()) "${c.code.trim()}_$timingKey" else "${normName}_$timingKey"

            if (!seen.add(key)) {
                duplicatesToDelete.add(c)
            }
        }

        for (dup in duplicatesToDelete) {
            database.courseDao().deleteCourse(dup)
        }
    }

    suspend fun insertExam(exam: ExamEntity): Long =
        database.examDao().insertExam(exam)

    suspend fun deleteExam(exam: ExamEntity) =
        database.examDao().deleteExam(exam)

    suspend fun insertTask(task: TaskEntity): Long =
        database.taskDao().insertTask(task)

    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) =
        database.taskDao().setTaskCompleted(id, isCompleted)

    suspend fun deleteTask(task: TaskEntity) =
        database.taskDao().deleteTask(task)

    suspend fun insertHolidayOverride(override: HolidayOverrideEntity): Long =
        database.holidayOverrideDao().insertOverride(override)

    suspend fun deleteHolidayOverride(override: HolidayOverrideEntity) =
        database.holidayOverrideDao().deleteOverride(override)

    suspend fun populateInitialDataIfEmpty() {
        val existing = database.semesterDao().getActiveSemesterSync()
        if (existing == null) {
            // Fall 2026 Academic Term (Starts Aug 31, 2026)
            val startDate = java.time.LocalDate.of(2026, 8, 31)
            val startMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

            val semId = database.semesterDao().insertSemester(
                com.example.data.model.SemesterEntity(
                    name = "2026-2027 Fall Semester",
                    startDateMillis = startMillis,
                    totalWeeks = 20,
                    isActive = true
                )
            )

            // Insert standard university courses across the week
            val sampleCourses = listOf(
                CourseEntity(
                    semesterId = semId,
                    name = "Advanced Operating Systems",
                    code = "CS-401",
                    classroom = "Hall A-302",
                    instructor = "Prof. Zhang",
                    dayOfWeek = 1, // Monday
                    startPeriod = 1,
                    endPeriod = 2,
                    startTime = "08:00",
                    endTime = "09:35",
                    weekRule = WeekRule.ALL,
                    colorHex = 0xFF4F46E5, // Indigo
                    dndEnabled = true,
                    notes = "Bring laptop for kernel labs"
                ),
                CourseEntity(
                    semesterId = semId,
                    name = "Database Systems & Architecture",
                    code = "CS-425",
                    classroom = "Turing Lab 204",
                    instructor = "Dr. Lin",
                    dayOfWeek = 1, // Monday
                    startPeriod = 3,
                    endPeriod = 4,
                    startTime = "10:00",
                    endTime = "11:40",
                    weekRule = WeekRule.ALL,
                    colorHex = 0xFF0D9488, // Teal
                    dndEnabled = true,
                    notes = "Query optimization modules"
                ),
                CourseEntity(
                    semesterId = semId,
                    name = "Distributed Algorithms",
                    code = "CS-480",
                    classroom = "Science Bldg 110",
                    instructor = "Prof. Chen",
                    dayOfWeek = 2, // Tuesday
                    startPeriod = 2,
                    endPeriod = 3,
                    startTime = "09:00",
                    endTime = "10:40",
                    weekRule = WeekRule.ODD,
                    colorHex = 0xFFD97706, // Amber
                    dndEnabled = true,
                    notes = "Odd weeks only"
                ),
                CourseEntity(
                    semesterId = semId,
                    name = "Computer Network Protocols",
                    code = "EE-350",
                    classroom = "Engineering West 408",
                    instructor = "Dr. Wang",
                    dayOfWeek = 2, // Tuesday
                    startPeriod = 5,
                    endPeriod = 6,
                    startTime = "14:00",
                    endTime = "15:40",
                    weekRule = WeekRule.ALL,
                    colorHex = 0xFF2563EB, // Blue
                    dndEnabled = true,
                    notes = "Wireshark packet tracing lab"
                ),
                CourseEntity(
                    semesterId = semId,
                    name = "Machine Learning Foundations",
                    code = "AI-501",
                    classroom = "Lecture Hall 1",
                    instructor = "Prof. Zhao",
                    dayOfWeek = 3, // Wednesday
                    startPeriod = 1,
                    endPeriod = 2,
                    startTime = "08:00",
                    endTime = "09:35",
                    weekRule = WeekRule.CUSTOM,
                    customWeeks = "1-8,10-16",
                    colorHex = 0xFF7C3AED, // Violet
                    dndEnabled = true,
                    notes = "Midterm exam during week 9"
                ),
                CourseEntity(
                    semesterId = semId,
                    name = "Software Engineering Practices",
                    code = "SE-310",
                    classroom = "Innovation Hub 101",
                    instructor = "Prof. Liu",
                    dayOfWeek = 4, // Thursday
                    startPeriod = 3,
                    endPeriod = 4,
                    startTime = "10:00",
                    endTime = "11:40",
                    weekRule = WeekRule.ALL,
                    colorHex = 0xFF059669, // Emerald
                    dndEnabled = true,
                    notes = "Scrum sprint deliverables"
                ),
                CourseEntity(
                    semesterId = semId,
                    name = "Cryptography & Network Security",
                    code = "SEC-412",
                    classroom = "Cyber Lab 305",
                    instructor = "Dr. Huang",
                    dayOfWeek = 5, // Friday
                    startPeriod = 1,
                    endPeriod = 2,
                    startTime = "08:00",
                    endTime = "09:35",
                    weekRule = WeekRule.EVEN,
                    colorHex = 0xFFDC2626, // Crimson
                    dndEnabled = true,
                    notes = "Even weeks only"
                )
            )
            database.courseDao().insertCourses(sampleCourses)

            // Insert initial sample tasks
            database.taskDao().insertTask(
                TaskEntity(
                    courseName = "Advanced Operating Systems",
                    title = "Finish Linux Kernel Module lab report",
                    dueDateMillis = System.currentTimeMillis() + 86400000L * 2,
                    priority = "HIGH"
                )
            )
            database.taskDao().insertTask(
                TaskEntity(
                    courseName = "Database Systems & Architecture",
                    title = "Submit B+ Tree indexing assignment",
                    dueDateMillis = System.currentTimeMillis() + 86400000L * 4,
                    priority = "MEDIUM"
                )
            )

            // Insert sample exams
            database.examDao().insertExam(
                ExamEntity(
                    semesterId = semId,
                    courseName = "Machine Learning Foundations",
                    classroom = "Main Auditorium 1",
                    examDateMillis = System.currentTimeMillis() + 86400000L * 14,
                    startTime = "09:00",
                    endTime = "11:00",
                    seatNumber = "Seat B-24",
                    notes = "Closed book, 1 double-sided cheat sheet allowed"
                )
            )

            // SWPU Academic Calendar 2026-2027 Holidays
            val holidays = listOf(
                com.example.data.model.HolidayOverrideEntity(name = "Mid-Autumn Festival (中秋节)", dateString = "2026-09-25", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "Mid-Autumn Festival (中秋节)", dateString = "2026-09-26", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "Mid-Autumn Festival (中秋节)", dateString = "2026-09-27", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-01", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-02", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-03", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-04", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-05", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-06", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "National Day (国庆节)", dateString = "2026-10-07", type = com.example.data.model.HolidayOverrideType.HOLIDAY),
                com.example.data.model.HolidayOverrideEntity(name = "New Year's Day (元旦)", dateString = "2027-01-01", type = com.example.data.model.HolidayOverrideType.HOLIDAY)
            )
            database.holidayOverrideDao().insertOverrides(holidays)
        }
    }
}
