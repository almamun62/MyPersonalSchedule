package com.example.domain

import com.example.data.model.CourseEntity
import com.example.data.model.HolidayOverrideEntity
import com.example.data.model.HolidayOverrideType
import com.example.data.model.WeekRule
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ScheduleEngineTest {

    // 1. Dynamic week calculation from semester start date
    @Test
    fun testDynamicWeekCalculation() {
        val semesterStart = LocalDate.of(2026, 9, 1) // Tuesday (Monday is Aug 31)

        // Same week should be week 1
        assertEquals(1, ScheduleEngine.calculateAcademicWeek(semesterStart, LocalDate.of(2026, 9, 1)))
        assertEquals(1, ScheduleEngine.calculateAcademicWeek(semesterStart, LocalDate.of(2026, 9, 6)))

        // Next Monday should be week 2
        assertEquals(2, ScheduleEngine.calculateAcademicWeek(semesterStart, LocalDate.of(2026, 9, 7)))

        // Week 5
        assertEquals(5, ScheduleEngine.calculateAcademicWeek(semesterStart, LocalDate.of(2026, 9, 28)))

        // Before semester starts
        assertEquals(0, ScheduleEngine.calculateAcademicWeek(semesterStart, LocalDate.of(2026, 8, 20)))
    }

    // 2. Rule evaluation: ALL, ODD, EVEN, and CUSTOM
    @Test
    fun testRuleEvaluation() {
        // ALL
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.ALL, "", currentWeek = 1))
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.ALL, "", currentWeek = 8))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.ALL, "", currentWeek = 0))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.ALL, "", currentWeek = 17, totalWeeks = 16))

        // ODD
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.ODD, "", currentWeek = 1))
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.ODD, "", currentWeek = 5))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.ODD, "", currentWeek = 2))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.ODD, "", currentWeek = 6))

        // EVEN
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.EVEN, "", currentWeek = 2))
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.EVEN, "", currentWeek = 8))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.EVEN, "", currentWeek = 1))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.EVEN, "", currentWeek = 7))

        // CUSTOM: '1-8,10-16'
        val customRule = "1-8,10-16"
        val parsed = ScheduleEngine.parseCustomWeeks(customRule)
        assertTrue(parsed.contains(1))
        assertTrue(parsed.contains(8))
        assertFalse(parsed.contains(9)) // Skipped week
        assertTrue(parsed.contains(10))
        assertTrue(parsed.contains(16))

        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.CUSTOM, customRule, currentWeek = 5))
        assertFalse(ScheduleEngine.isCourseActiveInWeek(WeekRule.CUSTOM, customRule, currentWeek = 9))
        assertTrue(ScheduleEngine.isCourseActiveInWeek(WeekRule.CUSTOM, customRule, currentWeek = 12))
    }

    // 3. Chinese holiday auto-pause (silences all classes on holidays)
    @Test
    fun testChineseHolidayAutoPause() {
        val semesterStart = LocalDate.of(2026, 9, 7) // Week 1 Monday
        val mondayCourse = CourseEntity(
            id = 1,
            semesterId = 1,
            name = "Advanced Operating Systems",
            code = "CS-401",
            classroom = "A-302",
            instructor = "Prof. Zhang",
            dayOfWeek = 1,
            startPeriod = 1,
            endPeriod = 2,
            startTime = "08:00",
            endTime = "09:40",
            weekRule = WeekRule.ALL
        )

        val holidayDate = LocalDate.of(2026, 9, 7) // A holiday on Monday
        val overrides = listOf(
            HolidayOverrideEntity(
                name = "Mid-Autumn Festival",
                dateString = holidayDate.toString(),
                type = HolidayOverrideType.HOLIDAY
            )
        )

        val result = ScheduleEngine.resolveDaySchedule(
            targetDate = holidayDate,
            semesterStartDate = semesterStart,
            totalWeeks = 16,
            allCourses = listOf(mondayCourse),
            holidayOverrides = overrides
        )

        assertTrue("Should be marked as holiday", result.isHoliday)
        assertEquals("Mid-Autumn Festival", result.holidayName)
        assertTrue("Classes should be silenced on holiday", result.activeCourses.isEmpty())
    }

    // 4. Weekend make-up days (调休), substituting Saturday/Sunday with designated weekday's timetable
    @Test
    fun testWeekendMakeUpDays() {
        val semesterStart = LocalDate.of(2026, 9, 7) // Week 1 Monday
        val mondayCourse = CourseEntity(
            id = 1,
            semesterId = 1,
            name = "Database Systems & Architecture",
            code = "CS-425",
            classroom = "Turing Lab",
            instructor = "Dr. Lin",
            dayOfWeek = 1, // Monday course
            startPeriod = 3,
            endPeriod = 4,
            startTime = "10:00",
            endTime = "11:40",
            weekRule = WeekRule.ALL
        )

        // Saturday (normally no classes, dayOfWeek = 6)
        val saturdayDate = LocalDate.of(2026, 9, 12)
        val overrides = listOf(
            HolidayOverrideEntity(
                name = "Mid-Autumn Make-Up Day",
                dateString = saturdayDate.toString(),
                type = HolidayOverrideType.MAKE_UP,
                targetDayOfWeek = 1 // Make-up day executes Monday's timetable!
            )
        )

        val result = ScheduleEngine.resolveDaySchedule(
            targetDate = saturdayDate,
            semesterStartDate = semesterStart,
            totalWeeks = 16,
            allCourses = listOf(mondayCourse),
            holidayOverrides = overrides
        )

        assertFalse("Make-up day is not silenced", result.isHoliday)
        assertTrue("Must be marked as make up day", result.isMakeUpDay)
        assertEquals(1, result.effectiveDayOfWeek)
        assertEquals(1, result.activeCourses.size)
        assertEquals("Database Systems & Architecture", result.activeCourses[0].name)
    }
}
