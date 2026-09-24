package com.example.domain.model

data class CourseConflict(
    val courseNameA: String,
    val courseCodeA: String,
    val courseNameB: String,
    val courseCodeB: String,
    val dayOfWeek: Int,
    val timeSlotA: String,
    val timeSlotB: String,
    val description: String
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
}
