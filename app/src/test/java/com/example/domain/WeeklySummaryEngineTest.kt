package com.example.domain

import com.example.data.model.CourseEntity
import com.example.data.model.TaskEntity
import com.example.data.model.WeekRule
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeeklySummaryEngineTest {

    @Test
    fun testRechartsDataStructureAndJsonOutput() {
        val semStart = LocalDate.of(2026, 9, 7) // Monday of Week 1

        val courses = listOf(
            CourseEntity(
                id = 1,
                semesterId = 1,
                name = "Computer Architecture",
                code = "CS201",
                classroom = "Room 301",
                instructor = "Prof. Zhang",
                dayOfWeek = 1, // Monday
                startPeriod = 1,
                endPeriod = 2,
                startTime = "08:00",
                endTime = "09:40",
                weekRule = WeekRule.ALL
            ),
            CourseEntity(
                id = 2,
                semesterId = 1,
                name = "Discrete Mathematics",
                code = "MATH101",
                classroom = "Room 202",
                instructor = "Prof. Liu",
                dayOfWeek = 3, // Wednesday
                startPeriod = 3,
                endPeriod = 4,
                startTime = "10:00",
                endTime = "11:40",
                weekRule = WeekRule.ALL
            )
        )

        val tasks = listOf(
            TaskEntity(
                id = 1,
                title = "CS201 Assignment 1 (2.5h)",
                courseName = "Computer Architecture",
                priority = "HIGH",
                dueDateMillis = semStart.toEpochDay() * 86400000L // Monday of week 1
            ),
            TaskEntity(
                id = 2,
                title = "Math proof exercises",
                courseName = "Discrete Mathematics",
                priority = "MEDIUM",
                dueDateMillis = semStart.plusDays(2).toEpochDay() * 86400000L // Wednesday of week 1
            )
        )

        val report = WeeklySummaryEngine.computeSummaryReport(
            courses = courses,
            tasks = tasks,
            targetWeek = 1,
            totalWeeks = 16,
            semesterStartDate = semStart
        )

        // 7 days in daily breakdown
        assertEquals(7, report.dailyBreakdown.size)

        // Monday check
        val mon = report.dailyBreakdown[0]
        assertEquals("Mon", mon.name)
        assertEquals("Monday", mon.label)
        assertEquals(1.7f, mon.classHours) // 08:00 to 09:40 is 100 min = 1.67 -> 1.7h
        assertEquals(2.5f, mon.studyHours) // parsed "2.5h"
        assertEquals(4.2f, mon.totalHours)
        assertTrue(mon.classNames.contains("Computer Architecture"))
        assertTrue(mon.taskTitles.contains("CS201 Assignment 1 (2.5h)"))

        // Wednesday check
        val wed = report.dailyBreakdown[2]
        assertEquals("Wed", wed.name)
        assertEquals(1.7f, wed.classHours)
        assertEquals(1.5f, wed.studyHours) // MEDIUM priority fallback = 1.5h

        // Recharts JSON check
        val dailyJson = report.toDailyRechartsJson()
        assertTrue(dailyJson.startsWith("["))
        assertTrue(dailyJson.endsWith("]"))
        assertTrue(dailyJson.contains("\"name\": \"Mon\""))
        assertTrue(dailyJson.contains("\"classHours\": 1.7"))
        assertTrue(dailyJson.contains("\"studyHours\": 2.5"))

        // Total hours and metrics
        assertTrue(report.totalClassHours > 0f)
        assertTrue(report.totalStudyHours > 0f)
        assertTrue(report.studyToClassRatio > 0f)
        assertEquals(16, report.semesterTrend.size)
    }

    @Test
    fun testTaskDurationParser() {
        val task1 = TaskEntity(title = "Read Chapter 4 (2h)", priority = "LOW")
        val task2 = TaskEntity(title = "Coding lab (45min)", priority = "LOW")
        val task3 = TaskEntity(title = "General revision", priority = "HIGH")

        assertEquals(2.0f, WeeklySummaryEngine.calculateTaskHours(task1))
        assertEquals(0.8f, WeeklySummaryEngine.calculateTaskHours(task2)) // 45 / 60 = 0.75 -> 0.8
        assertEquals(2.5f, WeeklySummaryEngine.calculateTaskHours(task3)) // HIGH fallback = 2.5
    }
}
