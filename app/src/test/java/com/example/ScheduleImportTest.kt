package com.example

import com.example.domain.conflict.ConflictDetector
import com.example.domain.model.Course
import com.example.domain.model.ImportedCourse
import com.example.domain.parser.ScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleImportTest {

    @Test
    fun testParseCsvTemplate() {
        val courses = ScheduleParser.parseCsv(ScheduleParser.SAMPLE_CSV_TEMPLATE)
        assertTrue("Courses should not be empty", courses.isNotEmpty())

        // MWF courses should have expanded into Monday, Wednesday, Friday
        val cs101Entries = courses.filter { it.code == "CS 101" }
        assertEquals(3, cs101Entries.size)
        assertTrue(cs101Entries.any { it.dayOfWeek == 1 }) // Mon
        assertTrue(cs101Entries.any { it.dayOfWeek == 3 }) // Wed
        assertTrue(cs101Entries.any { it.dayOfWeek == 5 }) // Fri
    }

    @Test
    fun testConflictDetectionSameDayOverlap() {
        val course1 = ImportedCourse(
            tempId = "1",
            code = "CS 101",
            name = "Intro to CS",
            dayOfWeek = 1, // Mon
            startTime = "09:00",
            endTime = "10:15"
        )

        val course2 = ImportedCourse(
            tempId = "2",
            code = "MATH 201",
            name = "Calculus",
            dayOfWeek = 1, // Mon
            startTime = "09:30",
            endTime = "11:00"
        )

        val conflicts = ConflictDetector.evaluateConflicts(listOf(course1, course2), emptyList())
        assertEquals(1, conflicts.size)
        assertTrue(course1.hasConflict)
        assertTrue(course2.hasConflict)
    }

    @Test
    fun testConflictDetectionDifferentDaysNoConflict() {
        val course1 = ImportedCourse(
            tempId = "1",
            code = "CS 101",
            name = "Intro to CS",
            dayOfWeek = 1, // Mon
            startTime = "09:00",
            endTime = "10:15"
        )

        val course2 = ImportedCourse(
            tempId = "2",
            code = "MATH 201",
            name = "Calculus",
            dayOfWeek = 2, // Tue
            startTime = "09:00",
            endTime = "10:15"
        )

        val conflicts = ConflictDetector.evaluateConflicts(listOf(course1, course2), emptyList())
        assertEquals(0, conflicts.size)
        assertFalse(course1.hasConflict)
        assertFalse(course2.hasConflict)
    }

    @Test
    fun testConflictWithEnrolledCourses() {
        val parsedCourse = ImportedCourse(
            tempId = "1",
            code = "PHYS 150",
            name = "Physics Lab",
            dayOfWeek = 3, // Wed
            startTime = "14:00",
            endTime = "17:00"
        )

        val existingCourse = Course(
            id = 10,
            code = "CHEM 101",
            name = "General Chemistry",
            dayOfWeek = 3, // Wed
            startTime = "15:00",
            endTime = "16:30",
            semester = "Fall 2025"
        )

        val conflicts = ConflictDetector.evaluateConflicts(
            listOf(parsedCourse),
            listOf(existingCourse)
        )

        assertEquals(1, conflicts.size)
        assertTrue(parsedCourse.hasConflict)
        assertTrue(parsedCourse.conflictDescription?.contains("CHEM 101") == true)
    }
}
