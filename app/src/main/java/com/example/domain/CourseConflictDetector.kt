package com.example.domain

import com.example.data.model.WeekRule
import com.example.domain.model.Course
import com.example.domain.model.CourseConflict

object CourseConflictDetector {

    /**
     * Checks if two courses' week rules overlap (e.g. ALL overlaps with ODD, but ODD and EVEN do not overlap).
     */
    fun doWeekRulesOverlap(w1: WeekRule, w2: WeekRule): Boolean {
        if (w1 == WeekRule.ALL || w2 == WeekRule.ALL) return true
        if (w1 == w2) return true
        return false // e.g. ODD vs EVEN
    }

    /**
     * Checks if two courses conflict on day of week and period range.
     */
    fun isConflict(c1: Course, c2: Course): Boolean {
        if (c1.id != 0L && c2.id != 0L && c1.id == c2.id) return false
        if (c1.dayOfWeek != c2.dayOfWeek) return false
        if (!doWeekRulesOverlap(c1.weekRule, c2.weekRule)) return false
        return (c1.startPeriod <= c2.endPeriod) && (c2.startPeriod <= c1.endPeriod)
    }

    /**
     * Detects all conflicts within a list of courses.
     */
    fun detectConflicts(courses: List<Course>): List<CourseConflict> {
        val conflicts = mutableListOf<CourseConflict>()
        for (i in courses.indices) {
            for (j in i + 1 until courses.size) {
                val c1 = courses[i]
                val c2 = courses[j]
                if (isConflict(c1, c2)) {
                    val overlapWeeks = when {
                        c1.weekRule == WeekRule.ALL && c2.weekRule == WeekRule.ALL -> "Every Week"
                        c1.weekRule == WeekRule.ODD || c2.weekRule == WeekRule.ODD -> "Odd Weeks"
                        c1.weekRule == WeekRule.EVEN || c2.weekRule == WeekRule.EVEN -> "Even Weeks"
                        else -> "Full Term"
                    }
                    val overlapStart = maxOf(c1.startPeriod, c2.startPeriod)
                    val overlapEnd = minOf(c1.endPeriod, c2.endPeriod)
                    val periodDesc = if (overlapStart == overlapEnd) "Period $overlapStart" else "Periods $overlapStart-$overlapEnd"
                    conflicts.add(
                        CourseConflict(
                            course1 = c1,
                            course2 = c2,
                            overlapWeeksDescription = overlapWeeks,
                            timeSlotDescription = "${c1.dayShortName} • $periodDesc"
                        )
                    )
                }
            }
        }
        return conflicts
    }

    /**
     * Returns true if the course has a conflict with any other course in the provided list.
     */
    fun hasConflict(course: Course, allCourses: List<Course>): Boolean {
        return allCourses.any { isConflict(course, it) }
    }

    /**
     * Returns conflicting courses for a given course.
     */
    fun getConflictingCourses(course: Course, allCourses: List<Course>): List<Course> {
        return allCourses.filter { isConflict(course, it) }
    }
}
