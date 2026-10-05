package com.example.domain

import com.example.domain.model.Course
import com.example.domain.model.CourseConflict

object CourseConflictDetector {
    fun detectConflicts(courses: List<Course>): List<CourseConflict> {
        val conflicts = mutableListOf<CourseConflict>()
        for (i in courses.indices) {
            for (j in i + 1 until courses.size) {
                val c1 = courses[i]
                val c2 = courses[j]
                if (c1.dayOfWeek == c2.dayOfWeek && c1.id != c2.id) {
                    val overlap = (c1.startPeriod <= c2.endPeriod) && (c2.startPeriod <= c1.endPeriod)
                    if (overlap) {
                        conflicts.add(
                            CourseConflict(
                                course1 = c1,
                                course2 = c2,
                                overlapWeeksDescription = "Full Term",
                                timeSlotDescription = "Day ${c1.dayOfWeek} • Periods ${maxOf(c1.startPeriod, c2.startPeriod)}-${minOf(c1.endPeriod, c2.endPeriod)}"
                            )
                        )
                    }
                }
            }
        }
        return conflicts
    }
}
