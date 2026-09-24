package com.example.domain

import com.example.data.model.CourseEntity
import com.example.data.model.WeekRule

data class CourseConflict(
    val id: String,
    val course1: CourseEntity,
    val course2: CourseEntity,
    val dayOfWeek: Int,
    val overlapStartPeriod: Int,
    val overlapEndPeriod: Int,
    val overlapStartTime: String,
    val overlapEndTime: String,
    val conflictingWeeks: List<Int>, // e.g. [1, 2, 6, 7, 8, 9, 10]
    val formattedWeeks: String,      // e.g. "Weeks 1-2, 6-10"
    val dayName: String,             // e.g. "Wednesday"
    val summary: String,
    val recommendation: String
)

object CourseConflictDetector {

    val DAY_NAMES = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    val DAY_NAMES_CN = listOf(
        "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"
    )

    /**
     * Detects time and academic week conflicts between all scheduled courses.
     */
    fun detectConflicts(courses: List<CourseEntity>, totalWeeks: Int = 18): List<CourseConflict> {
        val conflicts = mutableListOf<CourseConflict>()
        val scheduledCourses = courses.filter { it.dayOfWeek in 1..7 && it.startPeriod > 0 }

        // Group by day of week
        val byDay = scheduledCourses.groupBy { it.dayOfWeek }

        for ((day, dayCourses) in byDay) {
            for (i in 0 until dayCourses.size) {
                for (j in i + 1 until dayCourses.size) {
                    val c1 = dayCourses[i]
                    val c2 = dayCourses[j]

                    // Check if periods overlap:
                    val overlapStart = maxOf(c1.startPeriod, c2.startPeriod)
                    val overlapEnd = minOf(c1.endPeriod, c2.endPeriod)

                    if (overlapStart <= overlapEnd) {
                        // Check if academic weeks overlap
                        val overlappingWeeks = (1..totalWeeks).filter { w ->
                            ScheduleEngine.isCourseActiveInWeek(c1.weekRule, c1.customWeeks, w, totalWeeks) &&
                            ScheduleEngine.isCourseActiveInWeek(c2.weekRule, c2.customWeeks, w, totalWeeks)
                        }

                        if (overlappingWeeks.isNotEmpty()) {
                            val formattedWeeks = formatWeekRanges(overlappingWeeks)
                            val dayName = DAY_NAMES.getOrElse(day - 1) { "Day $day" }
                            val dayNameCn = DAY_NAMES_CN.getOrElse(day - 1) { "周$day" }

                            val startTime = if (c1.startPeriod >= c2.startPeriod) c1.startTime else c2.startTime
                            val endTime = if (c1.endPeriod <= c2.endPeriod) c1.endTime else c2.endTime

                            val conflict = CourseConflict(
                                id = "conflict_${c1.id}_${c2.id}_${day}_${overlapStart}_${overlapEnd}",
                                course1 = c1,
                                course2 = c2,
                                dayOfWeek = day,
                                overlapStartPeriod = overlapStart,
                                overlapEndPeriod = overlapEnd,
                                overlapStartTime = startTime,
                                overlapEndTime = endTime,
                                conflictingWeeks = overlappingWeeks,
                                formattedWeeks = formattedWeeks,
                                dayName = dayName,
                                summary = "${c1.name} & ${c2.name} overlap on $dayName ($dayNameCn) Sec $overlapStart-$overlapEnd ($startTime-$endTime)",
                                recommendation = "Overlap occurs in $formattedWeeks. Contact your international college (国际学院) or course instructors (${c1.instructor.ifBlank { "TBD" }}, ${c2.instructor.ifBlank { "TBD" }}) for attendance exemptions or session rescheduling."
                            )
                            conflicts.add(conflict)
                        }
                    }
                }
            }
        }
        return conflicts
    }

    /**
     * Formats a list of week numbers into concise ranges:
     * e.g. [1, 2, 6, 7, 8, 9, 10] -> "Weeks 1-2, 6-10"
     */
    fun formatWeekRanges(weeks: List<Int>): String {
        if (weeks.isEmpty()) return "None"
        val sorted = weeks.sorted()
        val ranges = mutableListOf<String>()
        var start = sorted.first()
        var prev = start

        for (k in 1 until sorted.size) {
            val curr = sorted[k]
            if (curr == prev + 1) {
                prev = curr
            } else {
                ranges.add(if (start == prev) "$start" else "$start-$prev")
                start = curr
                prev = curr
            }
        }
        ranges.add(if (start == prev) "$start" else "$start-$prev")
        return "Weeks " + ranges.joinToString(", ")
    }

    /**
     * Checks whether a specific conflict is active in the given week.
     */
    fun isConflictActiveInWeek(conflict: CourseConflict, week: Int): Boolean {
        return conflict.conflictingWeeks.contains(week)
    }

    /**
     * Returns whether a course is part of any conflict.
     */
    fun isCourseConflicted(course: CourseEntity, conflicts: List<CourseConflict>): Boolean {
        return conflicts.any { it.course1.id == course.id || it.course2.id == course.id ||
                (it.course1.name == course.name && it.course1.dayOfWeek == course.dayOfWeek && it.course1.startPeriod == course.startPeriod) ||
                (it.course2.name == course.name && it.course2.dayOfWeek == course.dayOfWeek && it.course2.startPeriod == course.startPeriod) }
    }

    /**
     * Resolves all active academic week numbers for a course.
     */
    fun calculateActiveWeeks(rule: WeekRule, customWeeks: String, totalWeeks: Int = 20): Set<Int> {
        return (1..totalWeeks).filter { w ->
            ScheduleEngine.isCourseActiveInWeek(rule, customWeeks, w, totalWeeks)
        }.toSet()
    }

    /**
     * Formats integer weeks into custom weeks format (e.g., "1-2,6-10" without "Weeks " prefix).
     */
    fun formatRawWeekRanges(weeks: Collection<Int>): String {
        if (weeks.isEmpty()) return ""
        val sorted = weeks.sorted()
        val ranges = mutableListOf<String>()
        var start = sorted.first()
        var prev = start

        for (k in 1 until sorted.size) {
            val curr = sorted[k]
            if (curr == prev + 1) {
                prev = curr
            } else {
                ranges.add(if (start == prev) "$start" else "$start-$prev")
                start = curr
                prev = curr
            }
        }
        ranges.add(if (start == prev) "$start" else "$start-$prev")
        return ranges.joinToString(",")
    }

    /**
     * Splits two conflicting courses across Odd and Even weeks so both can be attended.
     */
    fun splitOddEvenWeeks(
        course1: CourseEntity,
        course2: CourseEntity,
        totalWeeks: Int = 20
    ): Pair<CourseEntity, CourseEntity> {
        val c1Weeks = calculateActiveWeeks(course1.weekRule, course1.customWeeks, totalWeeks).filter { it % 2 == 1 }
        val c2Weeks = calculateActiveWeeks(course2.weekRule, course2.customWeeks, totalWeeks).filter { it % 2 == 0 }

        val updatedC1 = course1.copy(
            weekRule = WeekRule.CUSTOM,
            customWeeks = formatRawWeekRanges(c1Weeks)
        )
        val updatedC2 = course2.copy(
            weekRule = WeekRule.CUSTOM,
            customWeeks = formatRawWeekRanges(c2Weeks)
        )
        return Pair(updatedC1, updatedC2)
    }

    /**
     * Removes the clashing weeks from a course so the student attends the other course during those weeks.
     * If no weeks remain, marks the course as unscheduled/auditing.
     */
    fun omitClashingWeeks(
        course: CourseEntity,
        clashingWeeks: Collection<Int>,
        totalWeeks: Int = 20
    ): CourseEntity {
        val activeWeeks = calculateActiveWeeks(course.weekRule, course.customWeeks, totalWeeks)
        val remainingWeeks = activeWeeks.minus(clashingWeeks.toSet()).sorted()

        return if (remainingWeeks.isEmpty()) {
            moveCourseToAudit(course)
        } else {
            course.copy(
                weekRule = WeekRule.CUSTOM,
                customWeeks = formatRawWeekRanges(remainingWeeks)
            )
        }
    }

    /**
     * Moves a course to Auditing / Unscheduled so it doesn't collide in the timetable grid,
     * while safely preserving all its room, notes, assignments, and course code information.
     */
    fun moveCourseToAudit(course: CourseEntity): CourseEntity {
        val auditTag = "[Auditing]"
        val updatedNotes = if (course.notes.contains(auditTag)) {
            course.notes
        } else {
            "$auditTag ${course.notes}".trim()
        }
        return course.copy(
            dayOfWeek = 0,
            startPeriod = 0,
            endPeriod = 0,
            notes = updatedNotes
        )
    }
}
