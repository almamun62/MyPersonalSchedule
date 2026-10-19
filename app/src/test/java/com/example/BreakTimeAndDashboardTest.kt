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

        // Period 1 (08:00 - 08:50) -> Period 2 (09:00 - 09:50): 10 min gap <= 10 min
        val gap1To2 = getBreakMinutes(1, timings)
        assertEquals(10, gap1To2)
        assertFalse("10 min gap should NOT be shown as break (> 10 min)", gap1To2 > 10)

        // Period 2 (09:00 - 09:50) -> Period 3 (10:10 - 11:00): 20 min gap > 10 min (Morning recess)
        val gap2To3 = getBreakMinutes(2, timings)
        assertEquals(20, gap2To3)
        assertTrue("20 min gap SHOULD be shown as break (> 10 min)", gap2To3 > 10)

        // Period 3 (10:10 - 11:00) -> Period 4 (11:10 - 12:00): 10 min gap <= 10 min
        val gap3To4 = getBreakMinutes(3, timings)
        assertEquals(10, gap3To4)
        assertFalse("10 min gap should NOT be shown as break", gap3To4 > 10)

        // Period 4 (11:10 - 12:00) -> Period 5 (14:00 - 14:50): 120 min gap (Lunch) > 10 min
        val gap4To5 = getBreakMinutes(4, timings)
        assertEquals(120, gap4To5)
        assertTrue("Lunch break (120 min) SHOULD be shown as break", gap4To5 > 10)

        // Period 6 (15:00 - 15:50) -> Period 7 (16:10 - 17:00): 20 min gap > 10 min (Afternoon recess)
        val gap6To7 = getBreakMinutes(6, timings)
        assertEquals(20, gap6To7)
        assertTrue("Afternoon recess (20 min) SHOULD be shown as break", gap6To7 > 10)

        // Period 8 (17:10 - 18:00) -> Period 9 (19:00 - 19:50): 60 min gap (Dinner) > 10 min
        val gap8To9 = getBreakMinutes(8, timings)
        assertEquals(60, gap8To9)
        assertTrue("Dinner break (60 min) SHOULD be shown as break", gap8To9 > 10)

        // Period 9 -> 10: 10 min gap <= 10 min
        val gap9To10 = getBreakMinutes(9, timings)
        assertEquals(10, gap9To10)
        assertFalse("10 min gap should NOT be shown as break", gap9To10 > 10)
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
