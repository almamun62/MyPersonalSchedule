package com.example.domain

import com.example.data.model.CourseEntity
import com.example.data.model.HolidayOverrideEntity
import com.example.data.model.HolidayOverrideType
import com.example.data.model.SectionTiming
import com.example.data.model.WeekRule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ResolvedDaySchedule(
    val isHoliday: Boolean = false,
    val holidayName: String? = null,
    val isMakeUpDay: Boolean = false,
    val effectiveDayOfWeek: Int = 1,
    val activeCourses: List<CourseEntity> = emptyList()
)

object ScheduleEngine {
    fun calculateCurrentPeriod(
        timings: List<SectionTiming>,
        currentTimeStr: String
    ): Int? {
        val (currH, currM) = parseTime(currentTimeStr) ?: return null
        val currTotal = currH * 60 + currM

        timings.forEach { timing ->
            val (sH, sM) = parseTime(timing.startTime) ?: return@forEach
            val (eH, eM) = parseTime(timing.endTime) ?: return@forEach
            val sTotal = sH * 60 + sM
            val eTotal = eH * 60 + eM

            if (currTotal in sTotal..eTotal) {
                return timing.section
            }
        }
        return null
    }

    fun calculateAcademicWeek(semesterStart: LocalDate, currentDate: LocalDate): Int {
        if (currentDate.isBefore(semesterStart)) return 0
        // Find Monday of the week containing semesterStart
        val startMonday = semesterStart.minusDays((semesterStart.dayOfWeek.value - 1).toLong())
        val daysDiff = ChronoUnit.DAYS.between(startMonday, currentDate)
        val weekNum = (daysDiff / 7).toInt() + 1
        return weekNum
    }

    fun isCourseActiveInWeek(rule: WeekRule, customWeeks: String, currentWeek: Int, totalWeeks: Int = 16): Boolean {
        if (currentWeek < 1 || currentWeek > totalWeeks) return false
        return when (rule) {
            WeekRule.ALL -> true
            WeekRule.ODD -> currentWeek % 2 != 0
            WeekRule.EVEN -> currentWeek % 2 == 0
            WeekRule.CUSTOM -> {
                val parsed = parseCustomWeeks(customWeeks)
                parsed.contains(currentWeek)
            }
        }
    }

    fun parseCustomWeeks(customWeeks: String): Set<Int> {
        val result = mutableSetOf<Int>()
        val parts = customWeeks.split(",")
        for (part in parts) {
            val clean = part.trim()
            if (clean.contains("-")) {
                val bounds = clean.split("-")
                val start = bounds.getOrNull(0)?.toIntOrNull()
                val end = bounds.getOrNull(1)?.toIntOrNull()
                if (start != null && end != null) {
                    for (w in start..end) {
                        result.add(w)
                    }
                }
            } else {
                clean.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    fun resolveDaySchedule(
        targetDate: LocalDate,
        semesterStartDate: LocalDate,
        totalWeeks: Int = 16,
        allCourses: List<CourseEntity>,
        holidayOverrides: List<HolidayOverrideEntity> = emptyList()
    ): ResolvedDaySchedule {
        val override = holidayOverrides.find { it.dateString == targetDate.toString() }
        if (override != null) {
            if (override.type == HolidayOverrideType.HOLIDAY) {
                return ResolvedDaySchedule(
                    isHoliday = true,
                    holidayName = override.name,
                    isMakeUpDay = false,
                    effectiveDayOfWeek = targetDate.dayOfWeek.value,
                    activeCourses = emptyList()
                )
            } else if (override.type == HolidayOverrideType.MAKE_UP) {
                val effectiveDay = override.targetDayOfWeek
                val weekNum = calculateAcademicWeek(semesterStartDate, targetDate)
                val active = allCourses.filter { course ->
                    course.dayOfWeek == effectiveDay && isCourseActiveInWeek(course.weekRule, course.customWeeks, weekNum, totalWeeks)
                }
                return ResolvedDaySchedule(
                    isHoliday = false,
                    holidayName = override.name,
                    isMakeUpDay = true,
                    effectiveDayOfWeek = effectiveDay,
                    activeCourses = active
                )
            }
        }

        val dayOfW = targetDate.dayOfWeek.value
        val weekNum = calculateAcademicWeek(semesterStartDate, targetDate)
        val active = allCourses.filter { course ->
            course.dayOfWeek == dayOfW && isCourseActiveInWeek(course.weekRule, course.customWeeks, weekNum, totalWeeks)
        }

        return ResolvedDaySchedule(
            isHoliday = false,
            effectiveDayOfWeek = dayOfW,
            activeCourses = active
        )
    }

    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.trim().split(":")
        if (parts.size >= 2) {
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            return Pair(h, m)
        }
        return null
    }
}
