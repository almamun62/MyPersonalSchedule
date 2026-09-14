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
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long,
    val name: String,
    val code: String,
    val classroom: String,
    val instructor: String,
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val startPeriod: Int,
    val endPeriod: Int,
    val startTime: String, // e.g. "08:00"
    val endTime: String,   // e.g. "09:40"
    val weekRule: WeekRule = WeekRule.ALL,
    val customWeeks: String = "", // e.g. "1-8,10-16"
    val colorHex: Long = 0xFF4F46E5,
    val dndEnabled: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semesterId: Long,
    val courseName: String,
    val classroom: String,
    val examDateMillis: Long,
    val startTime: String,
    val endTime: String,
    val seatNumber: String = "",
    val notes: String = ""
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseName: String = "",
    val title: String,
    val dueDateMillis: Long = 0,
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val createdAtMillis: Long = System.currentTimeMillis(),
    val sourceClass: String? = null // For post-class task prompts
)

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
