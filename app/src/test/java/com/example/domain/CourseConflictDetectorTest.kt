package com.example.domain

import com.example.util.ScheduleImportHelper
import org.junit.Assert.*
import org.junit.Test

class CourseConflictDetectorTest {

    @Test
    fun testParseMamunScheduleAndDetectConflicts() {
        val courses = ScheduleImportHelper.getMamunFall2026Schedule(semesterId = 1)
        val unscheduled = ScheduleImportHelper.getMamunUnscheduledCourses(semesterId = 1)

        // 15 scheduled session blocks + 2 unscheduled courses
        assertEquals(15, courses.size)
        assertEquals(2, unscheduled.size)

        // Detect conflicts
        val conflicts = CourseConflictDetector.detectConflicts(courses, totalWeeks = 20)

        // There should be a conflict detected on Wednesday Sec 3-4 between 计算机组成原理 and 神经网络与深度学习导论
        assertTrue(conflicts.isNotEmpty())
        val wedConflict = conflicts.firstOrNull { it.dayOfWeek == 3 }
        assertNotNull("Wednesday conflict must be detected", wedConflict)

        assertEquals(3, wedConflict!!.overlapStartPeriod)
        assertEquals(4, wedConflict.overlapEndPeriod)

        // Check that overlapping weeks contain 1, 2, 6, 7, 8, 9, 10
        assertTrue(wedConflict.conflictingWeeks.contains(1))
        assertTrue(wedConflict.conflictingWeeks.contains(2))
        assertTrue(wedConflict.conflictingWeeks.contains(6))
        assertTrue(wedConflict.conflictingWeeks.contains(10))
        assertFalse(wedConflict.conflictingWeeks.contains(3)) // 计算机组成原理 is not active in week 3
    }

    @Test
    fun testRawTextParser() {
        val parsed = ScheduleImportHelper.parseChineseScheduleFormat(
            ScheduleImportHelper.MAMUN_SCHEDULE_RAW_TEXT,
            semesterId = 1
        )

        assertTrue(parsed.isNotEmpty())
        assertTrue(parsed.any { it.name.contains("计算机组成原理") })
        assertTrue(parsed.any { it.name.contains("桌面应用程序设计") })
        assertTrue(parsed.any { it.name.contains("面向数据科学的编程语言") })
        assertTrue(parsed.any { it.name.contains("数据库原理及应用") })
        assertTrue(parsed.any { it.name.contains("神经网络与深度学习导论") })
        assertTrue(parsed.any { it.name.contains("数据分析与机器学习") })
    }
}
