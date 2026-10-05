package com.example.domain

import com.example.data.model.CourseEntity
import com.example.data.model.SectionTiming
import com.example.data.model.WeekRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleEngineTest {

    @Test
    fun testCalculateAcademicWeek() {
        val semStart = LocalDate.of(2026, 9, 7) // Monday of week 1
        assertEquals(1, ScheduleEngine.calculateAcademicWeek(semStart, semStart))
        assertEquals(1, ScheduleEngine.calculateAcademicWeek(semStart, semStart.plusDays(6))) // Sunday
        assertEquals(2, ScheduleEngine.calculateAcademicWeek(semStart, semStart.plusDays(7))) // Mon of week 2
    }

    @Test
    fun testIsCourseActiveInWeek() {
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.ALL, "", 5))
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.ODD, "", 5))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.ODD, "", 6))
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.EVEN, "", 6))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.EVEN, "", 5))

        // Custom weeks
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.CUSTOM, "1-5,8", 3))
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.CUSTOM, "1-5,8", 8))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.CUSTOM, "1-5,8", 6))
    }

    @Test
    fun testCalculateCurrentPeriod() {
        val timings = listOf(
            SectionTiming(section = 1, startTime = "08:00", endTime = "08:45"),
            SectionTiming(section = 2, startTime = "08:50", endTime = "09:35"),
            SectionTiming(section = 3, startTime = "09:50", endTime = "10:35")
        )

        assertEquals(1, ScheduleEngine.calculateCurrentPeriod(timings, "08:20"))
        assertEquals(2, ScheduleEngine.calculateCurrentPeriod(timings, "09:00"))
        assertEquals(3, ScheduleEngine.calculateCurrentPeriod(timings, "10:00"))
        assertNull(ScheduleEngine.calculateCurrentPeriod(timings, "09:40")) // Between period 2 & 3
    }
}
