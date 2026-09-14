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

    suspend fun updateCourse(course: CourseEntity) =
        database.courseDao().updateCourse(course)

    suspend fun deleteCourse(course: CourseEntity) =
        database.courseDao().deleteCourse(course)

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
            // Create current semester starting on the most recent Monday
            val today = LocalDate.now()
            val startMonday = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
            val startMillis = startMonday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val semId = database.semesterDao().insertSemester(
                SemesterEntity(
                    name = "Fall 2026 Academic Term",
                    startDateMillis = startMillis,
                    totalWeeks = 16,
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
                    endTime = "09:40",
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
                    endTime = "09:40",
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
                    endTime = "09:40",
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

            // Insert preloaded holiday & make-up overrides (e.g. Mid-Autumn Festival & National Day Holiday + Weekend Make-Up Day)
            val holidays = listOf(
                HolidayOverrideEntity(
                    name = "Mid-Autumn Festival Holiday",
                    dateString = today.plusDays(4).toString(),
                    type = HolidayOverrideType.HOLIDAY
                ),
                HolidayOverrideEntity(
                    name = "National Day Holiday Break",
                    dateString = today.plusDays(18).toString(),
                    type = HolidayOverrideType.HOLIDAY
                ),
                HolidayOverrideEntity(
                    name = "Weekend Make-up Day (调休 -> Monday)",
                    dateString = today.plusDays(5).toString(), // e.g. Upcoming weekend
                    type = HolidayOverrideType.MAKE_UP,
                    targetDayOfWeek = 1 // Follows Monday timetable
                )
            )
            database.holidayOverrideDao().insertOverrides(holidays)
        }
    }
}
