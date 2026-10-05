package com.example.domain

import com.example.domain.parser.ScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleParserTest {

    @Test
    fun testMamunPresetCourses() {
        val courses = ScheduleParser.getMamunFall2026ImportedCourses()
        assertEquals(6, courses.size)
        assertTrue(courses.any { it.name.contains("Desktop Application Design") })
        assertTrue(courses.any { it.name.contains("Data Analysis & Machine Learning") })
        assertTrue(courses.any { it.name.contains("Computer Architecture") })
    }

    @Test
    fun testCsvParser() {
        val sampleCsv = """
            Course Name,Course Code,Classroom,Instructor,Day (1-7),Start Period,End Period,Start Time,End Time
            Software Engineering,SE301,Room A101,Dr. Smith,1,1,2,08:00,09:35
            Database Systems,CS302,Room B202,Prof. Jones,3,3,5,09:50,12:15
        """.trimIndent()

        val parsed = ScheduleParser.parseCsvSchedule(sampleCsv)
        assertEquals(2, parsed.size)

        val course1 = parsed[0]
        assertEquals("Software Engineering", course1.name)
        assertEquals("SE301", course1.code)
        assertEquals(1, course1.dayOfWeek)
        assertEquals(1, course1.startPeriod)
        assertEquals(2, course1.endPeriod)

        val course2 = parsed[1]
        assertEquals("Database Systems", course2.name)
        assertEquals(3, course2.dayOfWeek)
        assertEquals(3, course2.startPeriod)
        assertEquals(5, course2.endPeriod)
    }
}
