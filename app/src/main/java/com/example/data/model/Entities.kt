package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startDateMillis: Long,
    val totalWeeks: Int = 16,
    val isActive: Boolean = true
)

enum class WeekRule {
    ALL,
    ODD,
    EVEN,
    CUSTOM
}

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long = 1,
    val name: String,
    val instructor: String = "",
    val time: String = "",
    val location: String = "",
    val classroom: String = location,
    val code: String = "",
    val dayOfWeek: Int = 1, // 1 = Monday, 7 = Sunday
    val startPeriod: Int = 1,
    val endPeriod: Int = 2,
    val startTime: String = "08:00",
    val endTime: String = "09:35",
    val weekRule: WeekRule = WeekRule.ALL,
    val customWeeks: String = "",
    val colorHex: Long = 0xFF4F46E5,
    val dndEnabled: Boolean = true,
    val isRetake: Boolean = false,
    val notes: String = ""
) {
    constructor(
        name: String,
        instructor: String,
        time: String,
        location: String
    ) : this(
        id = 0,
        name = name,
        instructor = instructor,
        time = time,
        location = location,
        classroom = location,
        startTime = if (time.contains("-")) time.split("-")[0].trim() else time.ifBlank { "08:00" },
        endTime = if (time.contains("-")) time.split("-")[1].trim() else "09:35"
    )
}

typealias CourseEntity = Course

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long = 1,
    val courseName: String,
    val classroom: String,
    val examDateMillis: Long,
    val startTime: String,
    val endTime: String,
    val seatNumber: String = "",
    val notes: String = ""
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long = 0L,
    val courseName: String = "",
    val title: String,
    val dueDateMillis: Long = 0,
    val dueDate: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val createdAtMillis: Long = System.currentTimeMillis(),
    val sourceClass: String? = null
)

typealias TaskEntity = Task

enum class HolidayOverrideType {
    HOLIDAY,
    MAKE_UP
}

@Entity(tableName = "holiday_overrides")
data class HolidayOverrideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateString: String,
    val type: HolidayOverrideType,
    val targetDayOfWeek: Int = 1
)

@Entity(tableName = "academic_calendar_files")
data class AcademicCalendarFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileType: String, // "PHOTO", "PDF", "WORD", "EXCEL", "OTHER"
    val mimeType: String,
    val localPath: String,
    val fileSizeBytes: Long,
    val addedAtMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)

