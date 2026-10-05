package com.example.domain.model

import com.example.data.model.CourseEntity
import com.example.data.model.WeekRule

data class Course(
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val instructor: String = "",
    val classroom: String = "",
    val dayOfWeek: Int = 1, // 1 = Mon .. 7 = Sun
    val startPeriod: Int = 1,
    val endPeriod: Int = 2,
    val startTime: String = "08:00",
    val endTime: String = "09:35",
    val colorHex: String = "#5B9BF3",
    val semester: String = "Fall 2026",
    val credits: Int = 3,
    val notes: String = ""
) {
    val dayName: String
        get() = when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Unknown"
        }

    val dayShortName: String
        get() = when (dayOfWeek) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> "?"
        }

    fun toEntity(semesterIdOverride: Long? = null): CourseEntity {
        val parsedColor = try {
            val clean = colorHex.removePrefix("#")
            if (clean.length == 6) {
                0xFF000000 or clean.toLong(16)
            } else {
                clean.toLong(16)
            }
        } catch (e: Exception) {
            0xFF5B9BF3
        }

        val semId = semesterIdOverride ?: semester.filter { it.isDigit() }.toLongOrNull() ?: 1L

        return CourseEntity(
            id = id,
            semesterId = semId,
            name = name,
            code = code,
            classroom = classroom,
            instructor = instructor,
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            endPeriod = endPeriod,
            startTime = startTime,
            endTime = endTime,
            weekRule = WeekRule.ALL,
            customWeeks = "",
            colorHex = parsedColor,
            dndEnabled = true,
            notes = notes
        )
    }

    companion object {
        fun fromEntity(entity: CourseEntity): Course {
            val hexString = String.format("#%06X", (0xFFFFFF and entity.colorHex.toInt()))
            val semName = if (entity.semesterId == 1L) "Fall 2026" else "Semester ${entity.semesterId}"
            val cleanName = com.example.domain.ScheduleTranslationEngine.cleanText(entity.name)
            val cleanRoom = com.example.domain.ScheduleTranslationEngine.cleanText(entity.classroom)
            val cleanTeacher = com.example.domain.ScheduleTranslationEngine.cleanText(entity.instructor)
            return Course(
                id = entity.id,
                name = cleanName,
                code = entity.code,
                instructor = cleanTeacher,
                classroom = cleanRoom,
                dayOfWeek = entity.dayOfWeek,
                startPeriod = entity.startPeriod,
                endPeriod = entity.endPeriod,
                startTime = entity.startTime,
                endTime = entity.endTime,
                colorHex = hexString,
                semester = semName,
                credits = 3,
                notes = entity.notes
            )
        }
    }
}
