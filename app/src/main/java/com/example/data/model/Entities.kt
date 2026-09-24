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

/**
 * Room Entity representing a university/academic course.
 * Stores course details including name, instructor, time, and location.
 */
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
    val notes: String = ""
) {
    // Secondary constructor to create a course directly with name, instructor, time, and location
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

/**
 * Room Entity representing an assignment, homework, or task.
 */
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
    val sourceClass: String? = null // For post-class task prompts
) {
    constructor(
        title: String,
        courseName: String = "",
        dueDate: String = "",
        isCompleted: Boolean = false
    ) : this(
        id = 0,
        courseId = 0L,
        courseName = courseName,
        title = title,
        dueDate = dueDate,
        isCompleted = isCompleted
    )

    constructor(
        courseId: Long,
        title: String,
        dueDate: String = "",
        priority: String = "MEDIUM",
        isCompleted: Boolean = false
    ) : this(
        id = 0,
        courseId = courseId,
        courseName = "",
        title = title,
        dueDate = dueDate,
        isCompleted = isCompleted,
        priority = priority
    )
}

typealias TaskEntity = Task

enum class HolidayOverrideType {
    HOLIDAY,   // Silences all classes on this date
    MAKE_UP    // Weekend make-up day (调休): substitutes weekend with a designated weekday's timetable
}

@Entity(tableName = "holiday_overrides")
data class HolidayOverrideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateString: String, // YYYY-MM-DD
    val type: HolidayOverrideType,
    val targetDayOfWeek: Int? = null // 1 = Monday ... 7 = Sunday (used when type == MAKE_UP)
)

enum class NoteType {
    TEXT, DRAWING, IMAGE, VOICE
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val content: String, // Text content, or URI for image/voice/drawing data
    val type: NoteType = NoteType.TEXT,
    val tags: String = "", // Comma-separated tags or category
    val timestampMillis: Long = System.currentTimeMillis()
)

