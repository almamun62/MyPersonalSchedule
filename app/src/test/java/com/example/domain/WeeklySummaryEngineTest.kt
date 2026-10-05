package com.example.domain

import com.example.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklySummaryEngineTest {

    @Test
    fun testGenerateSummary() {
        val courses = listOf(
            Course(
                name = "Computer Architecture",
                dayOfWeek = 1,
                startPeriod = 3,
                endPeriod = 5
            ),
            Course(
                name = "Desktop Application Design",
                dayOfWeek = 1,
                startPeriod = 6,
                endPeriod = 7
            ),
            Course(
                name = "Data Analysis",
                dayOfWeek = 3,
                startPeriod = 8,
                endPeriod = 9
            )
        )

        val summary = WeeklySummaryEngine.generateSummary(courses)

        assertEquals(3, summary.totalCourses)
        assertEquals(7, summary.totalClassHours) // (5-3+1) + (7-6+1) + (9-8+1) = 3 + 2 + 2 = 7
        assertEquals("Monday", summary.busiestDayName)
        assertEquals(2, summary.busiestDayCount)
        assertEquals(5, summary.freeDaysCount) // Tue, Thu, Fri, Sat, Sun
    }
}
