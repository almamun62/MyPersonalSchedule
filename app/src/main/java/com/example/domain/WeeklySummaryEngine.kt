package com.example.domain

import com.example.domain.model.Course

object WeeklySummaryEngine {
    data class WeeklySummary(
        val totalCourses: Int,
        val totalClassHours: Int,
        val busiestDayName: String,
        val busiestDayCount: Int,
        val freeDaysCount: Int
    )

    fun generateSummary(courses: List<Course>): WeeklySummary {
        val total = courses.size
        var totalHours = 0
        val dayCounts = IntArray(8)

        courses.forEach { c ->
            if (c.dayOfWeek in 1..7) {
                dayCounts[c.dayOfWeek]++
            }
            val span = (c.endPeriod - c.startPeriod + 1).coerceAtLeast(1)
            totalHours += span
        }

        var maxCount = 0
        var busiestDay = 1
        var freeDays = 0

        for (d in 1..7) {
            val count = dayCounts[d]
            if (count > maxCount) {
                maxCount = count
                busiestDay = d
            }
            if (count == 0) {
                freeDays++
            }
        }

        val busiestName = when (busiestDay) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Monday"
        }

        return WeeklySummary(
            totalCourses = total,
            totalClassHours = totalHours,
            busiestDayName = busiestName,
            busiestDayCount = maxCount,
            freeDaysCount = freeDays
        )
    }
}
