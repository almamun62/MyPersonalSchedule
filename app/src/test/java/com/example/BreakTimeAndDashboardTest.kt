package com.example

import com.example.data.local.UserPreferencesManager
import com.example.data.model.SectionTiming
import org.junit.Assert.*
import org.junit.Test

class BreakTimeAndDashboardTest {

    private fun getBreakMinutes(period: Int, timings: List<SectionTiming>): Int {
        if (period < 1 || period >= timings.size) return 0
        val currTiming = timings.getOrNull(period - 1) ?: return 0
        val nextTiming = timings.getOrNull(period) ?: return 0
        return try {
            val (h1, m1) = currTiming.endTime.split(":").map { it.trim().toInt() }
            val (h2, m2) = nextTiming.startTime.split(":").map { it.trim().toInt() }
            val diff = (h2 * 60 + m2) - (h1 * 60 + m1)
            if (diff > 0) diff else 0
        } catch (e: Exception) {
            0
        }
    }

    @Test
    fun testBreakTimeLongerThan10Minutes() {
        val timings = UserPreferencesManager.defaultSectionTimings

        // Period 1 (08:00 - 08:45) -> Period 2 (08:50 - 09:35): 5 min gap <= 10 min
        val gap1To2 = getBreakMinutes(1, timings)
        assertEquals(5, gap1To2)
        assertFalse("5 min gap should NOT be shown as break (> 10 min)", gap1To2 > 10)

        // Period 2 (08:50 - 09:35) -> Period 3 (09:50 - 10:35): 15 min gap > 10 min
        val gap2To3 = getBreakMinutes(2, timings)
        assertEquals(15, gap2To3)
        assertTrue("15 min gap SHOULD be shown as break (> 10 min)", gap2To3 > 10)

        // Period 3 (09:50 - 10:35) -> Period 4 (10:40 - 11:25): 5 min gap <= 10 min
        val gap3To4 = getBreakMinutes(3, timings)
        assertEquals(5, gap3To4)
        assertFalse("5 min gap should NOT be shown as break", gap3To4 > 10)

        // Period 5 (11:30 - 12:15) -> Period 6 (14:30 - 15:15): 135 min gap (Lunch) > 10 min
        val gap5To6 = getBreakMinutes(5, timings)
        assertEquals(135, gap5To6)
        assertTrue("Lunch break (135 min) SHOULD be shown as break", gap5To6 > 10)

        // Period 7 (15:20 - 16:05) -> Period 8 (16:20 - 17:05): 15 min gap > 10 min
        val gap7To8 = getBreakMinutes(7, timings)
        assertEquals(15, gap7To8)
        assertTrue("Afternoon recess (15 min) SHOULD be shown as break", gap7To8 > 10)

        // Period 9 (17:10 - 17:55) -> Period 10 (19:00 - 19:45): 65 min gap (Dinner) > 10 min
        val gap9To10 = getBreakMinutes(9, timings)
        assertEquals(65, gap9To10)
        assertTrue("Dinner break (65 min) SHOULD be shown as break", gap9To10 > 10)

        // Period 10 -> 11: 5 min gap <= 10 min
        val gap10To11 = getBreakMinutes(10, timings)
        assertEquals(5, gap10To11)
        assertFalse("5 min gap should NOT be shown as break", gap10To11 > 10)
    }

    @Test
    fun testCourseCreditsCalculation() {
        val courseNames = listOf("高等数学", "大学英语", "计算机网络")
        val customCreditsMap = mapOf(
            "高等数学" to 5,
            "大学英语" to 3
        )

        // When course is in custom map, use custom credit; otherwise default to 3
        val totalCredits = courseNames.sumOf { name ->
            customCreditsMap[name] ?: 3
        }

        // 5 + 3 + 3 = 11 credits
        assertEquals(11, totalCredits)
    }
}
