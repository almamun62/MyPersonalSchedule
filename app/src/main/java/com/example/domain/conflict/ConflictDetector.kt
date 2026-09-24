package com.example.domain.conflict

import com.example.domain.model.Course
import com.example.domain.model.CourseConflict
import com.example.domain.model.ImportedCourse

object ConflictDetector {

    /**
     * Converts a time string like "09:30", "9:30", "14:00" to minutes from midnight (0..1439).
     * Returns -1 if invalid.
     */
    fun timeToMinutes(timeStr: String): Int {
        val trimmed = timeStr.trim()
        val parts = trimmed.split(":")
        if (parts.size != 2) return -1
        val hours = parts[0].toIntOrNull() ?: return -1
        val minutes = parts[1].toIntOrNull() ?: return -1
        if (hours !in 0..23 || minutes !in 0..59) return -1
        return hours * 60 + minutes
    }

    /**
     * Checks if two time intervals on the same day overlap.
     * Overlap occurs when max(startA, startB) < min(endA, endB).
     */
    fun isOverlapping(
        startA: String,
        endA: String,
        startB: String,
        endB: String
    ): Boolean {
        val sA = timeToMinutes(startA)
        val eA = timeToMinutes(endA)
        val sB = timeToMinutes(startB)
        val eB = timeToMinutes(endB)

        if (sA < 0 || eA < 0 || sB < 0 || eB < 0) return false
        if (eA <= sA || eB <= sB) return false

        return maxOf(sA, sB) < minOf(eA, eB)
    }

    /**
     * Evaluates conflicts for a batch of imported courses:
     * 1. Internal conflicts among the imported courses themselves.
     * 2. External conflicts against currently enrolled courses in the schedule.
     * Updates the `hasConflict` and `conflictDescription` flags on the `ImportedCourse` objects.
     */
    fun evaluateConflicts(
        importedCourses: List<ImportedCourse>,
        existingCourses: List<Course>
    ): List<CourseConflict> {
        val conflicts = mutableListOf<CourseConflict>()

        // Reset flags
        for (item in importedCourses) {
            item.hasConflict = false
            item.conflictDescription = null
        }

        // 1. Check against existing enrolled courses
        for (imported in importedCourses) {
            if (!imported.isValid) continue
            for (existing in existingCourses) {
                if (imported.dayOfWeek == existing.dayOfWeek &&
                    isOverlapping(imported.startTime, imported.endTime, existing.startTime, existing.endTime)
                ) {
                    val conflictDesc = "Overlaps with enrolled course: ${existing.code.ifEmpty { existing.name }} (${existing.startTime} - ${existing.endTime})"
                    imported.hasConflict = true
                    imported.conflictDescription = conflictDesc

                    conflicts.add(
                        CourseConflict(
                            courseNameA = imported.name,
                            courseCodeA = imported.code,
                            courseNameB = existing.name,
                            courseCodeB = existing.code,
                            dayOfWeek = imported.dayOfWeek,
                            timeSlotA = "${imported.startTime} - ${imported.endTime}",
                            timeSlotB = "${existing.startTime} - ${existing.endTime}",
                            description = conflictDesc
                        )
                    )
                }
            }
        }

        // 2. Check internal conflicts within the imported list
        for (i in importedCourses.indices) {
            val itemA = importedCourses[i]
            if (!itemA.isValid) continue
            for (j in (i + 1) until importedCourses.size) {
                val itemB = importedCourses[j]
                if (!itemB.isValid) continue

                if (itemA.dayOfWeek == itemB.dayOfWeek &&
                    isOverlapping(itemA.startTime, itemA.endTime, itemB.startTime, itemB.endTime)
                ) {
                    val descA = "Overlaps with imported: ${itemB.code.ifEmpty { itemB.name }} (${itemB.startTime} - ${itemB.endTime})"
                    val descB = "Overlaps with imported: ${itemA.code.ifEmpty { itemA.name }} (${itemA.startTime} - ${itemA.endTime})"

                    itemA.hasConflict = true
                    itemA.conflictDescription = descA
                    itemB.hasConflict = true
                    itemB.conflictDescription = descB

                    conflicts.add(
                        CourseConflict(
                            courseNameA = itemA.name,
                            courseCodeA = itemA.code,
                            courseNameB = itemB.name,
                            courseCodeB = itemB.code,
                            dayOfWeek = itemA.dayOfWeek,
                            timeSlotA = "${itemA.startTime} - ${itemA.endTime}",
                            timeSlotB = "${itemB.startTime} - ${itemB.endTime}",
                            description = "Internal conflict between imported courses: ${itemA.code} and ${itemB.code}"
                        )
                    )
                }
            }
        }

        return conflicts
    }
}
