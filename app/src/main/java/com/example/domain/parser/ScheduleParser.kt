package com.example.domain.parser

import com.example.data.model.CourseEntity
import com.example.data.model.WeekRule
import com.example.domain.model.ImportedCourse

object ScheduleParser {
    fun parseCsvSchedule(csvContent: String): List<ImportedCourse> {
        val list = mutableListOf<ImportedCourse>()
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return list

        lines.drop(1).forEach { line ->
            val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
            if (parts.size >= 5) {
                val name = parts.getOrNull(0) ?: "Course"
                val code = parts.getOrNull(1) ?: ""
                val classroom = parts.getOrNull(2) ?: ""
                val instructor = parts.getOrNull(3) ?: ""
                val day = parts.getOrNull(4)?.toIntOrNull() ?: 1
                val startP = parts.getOrNull(5)?.toIntOrNull() ?: 1
                val endP = parts.getOrNull(6)?.toIntOrNull() ?: (startP + 1)
                val sTime = parts.getOrNull(7) ?: "08:00"
                val eTime = parts.getOrNull(8) ?: "09:35"
                val color = parts.getOrNull(11) ?: "#5B9BF3"

                list.add(
                    ImportedCourse(
                        name = name,
                        code = code,
                        classroom = classroom,
                        instructor = instructor,
                        dayOfWeek = day.coerceIn(1, 7),
                        startPeriod = startP.coerceIn(1, 12),
                        endPeriod = endP.coerceIn(startP, 12),
                        startTime = sTime,
                        endTime = eTime,
                        colorHex = color
                    )
                )
            }
        }
        return list
    }

    fun getMamunFall2026ImportedCourses(): List<ImportedCourse> {
        return listOf(
            ImportedCourse(
                name = "Desktop Application Design (Desktop App)",
                code = "1619310040",
                classroom = "Teaching Bldg Software Lab 1",
                instructor = "Prof. Computer Sci",
                dayOfWeek = 1, // Monday
                startPeriod = 6, // After lunch
                endPeriod = 7,
                startTime = "14:30",
                endTime = "16:05",
                colorHex = "#2563EB"
            ),
            ImportedCourse(
                name = "Desktop Application Design (Desktop App)",
                code = "1619310040",
                classroom = "Teaching Bldg Software Lab 1",
                instructor = "Prof. Computer Sci",
                dayOfWeek = 3, // Wednesday
                startPeriod = 1,
                endPeriod = 2,
                startTime = "08:00",
                endTime = "09:35",
                colorHex = "#2563EB"
            ),
            ImportedCourse(
                name = "Data Analysis & Machine Learning",
                code = "1619312040",
                classroom = "Liberal Arts Bldg B301",
                instructor = "Prof. Wang",
                dayOfWeek = 3, // Wednesday
                startPeriod = 8,
                endPeriod = 9,
                startTime = "16:20",
                endTime = "17:55",
                colorHex = "#10B981"
            ),
            ImportedCourse(
                name = "Computer Architecture",
                code = "1619304040",
                classroom = "Science Bldg B105",
                instructor = "Prof. Zhang",
                dayOfWeek = 1, // Monday
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                colorHex = "#F59E0B"
            ),
            ImportedCourse(
                name = "Database Systems & Application",
                code = "1619307040",
                classroom = "Teaching Bldg Software Lab 1",
                instructor = "Prof. Chen",
                dayOfWeek = 5, // Friday
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                colorHex = "#EC4899"
            ),
            ImportedCourse(
                name = "Neural Networks & Deep Learning",
                code = "1619311040",
                classroom = "Liberal Arts Bldg A304",
                instructor = "Dr. Liu",
                dayOfWeek = 5, // Friday
                startPeriod = 8,
                endPeriod = 9,
                startTime = "16:20",
                endTime = "17:55",
                colorHex = "#8B5CF6"
            )
        )
    }
}
