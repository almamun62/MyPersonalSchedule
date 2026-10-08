package com.example.domain.model

import java.util.UUID

data class ImportedCourse(
    val tempId: String = UUID.randomUUID().toString(),
    var name: String,
    var code: String = "",
    var instructor: String = "",
    var classroom: String = "",
    var dayOfWeek: Int = 1, // 1 = Mon .. 7 = Sun
    var startPeriod: Int = 1,
    var endPeriod: Int = 2,
    var startTime: String = "08:00", // HH:mm
    var endTime: String = "09:35", // HH:mm
    var colorHex: String = "#5B9BF3",
    var credits: Int = 3,
    var isRetake: Boolean = false,
    var weekRule: com.example.data.model.WeekRule = com.example.data.model.WeekRule.ALL,
    var isSelected: Boolean = true,
    var hasConflict: Boolean = false,
    var conflictDescription: String? = null,
    var isValid: Boolean = true,
    var validationError: String? = null
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
            else -> "Day $dayOfWeek"
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

    fun toCourse(semester: String): Course = Course(
        id = 0,
        name = name.trim(),
        code = code.trim(),
        instructor = instructor.trim(),
        classroom = classroom.trim(),
        dayOfWeek = dayOfWeek,
        startPeriod = startPeriod,
        endPeriod = endPeriod,
        startTime = startTime.trim(),
        endTime = endTime.trim(),
        weekRule = weekRule,
        colorHex = colorHex,
        semester = semester,
        credits = credits,
        isRetake = isRetake
    )
}
