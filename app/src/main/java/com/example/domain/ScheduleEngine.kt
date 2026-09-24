package com.example.domain

import com.example.data.model.CourseEntity
import com.example.data.model.HolidayOverrideEntity
import com.example.data.model.HolidayOverrideType
import com.example.data.model.WeekRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class DayResolutionResult(
    val date: LocalDate,
    val academicWeek: Int,
    val isHoliday: Boolean,
    val holidayName: String? = null,
    val isMakeUpDay: Boolean = false,
    val effectiveDayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val makeUpDescription: String? = null,
    val activeCourses: List<CourseEntity>
)

enum class CourseClassStatus {
    COMPLETED,
    ACTIVE,
    NEXT_UP,
    UPCOMING
}

data class CourseWithStatus(
    val course: CourseEntity,
    val status: CourseClassStatus,
    val minutesUntilStart: Long = 0,
    val minutesRemaining: Long = 0
)

object ScheduleEngine {

    /**
     * 1. Dynamic academic week calculation from semester start date.
     * Normalized to Monday of the starting week.
     */
    fun calculateAcademicWeek(semesterStartDate: LocalDate, targetDate: LocalDate): Int {
        val semMonday = semesterStartDate.minusDays((semesterStartDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        val daysBetween = ChronoUnit.DAYS.between(semMonday, targetDate)
        return if (daysBetween < 0) {
            0
        } else {
            (daysBetween / 7).toInt() + 1
        }
    }

    /**
     * 2. Rule evaluation: ALL, ODD, EVEN, and CUSTOM (e.g., '1-8,10-16').
     */
    fun isCourseActiveInWeek(rule: WeekRule, customWeeks: String, currentWeek: Int, totalWeeks: Int = 20): Boolean {
        if (currentWeek < 1 || currentWeek > totalWeeks) return false
        return when (rule) {
            WeekRule.ALL -> true
            WeekRule.ODD -> currentWeek % 2 == 1
            WeekRule.EVEN -> currentWeek % 2 == 0
            WeekRule.CUSTOM -> parseCustomWeeks(customWeeks).contains(currentWeek)
        }
    }

    /**
     * Parses custom week ranges such as "1-8,10-16", "1,3,5-7", or single week "4".
     */
    fun parseCustomWeeks(customString: String): Set<Int> {
        val result = mutableSetOf<Int>()
        if (customString.isBlank()) return result
        val parts = customString.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains("-")) {
                val rangeParts = trimmed.split("-")
                if (rangeParts.size == 2) {
                    val start = rangeParts[0].trim().toIntOrNull()
                    val end = rangeParts[1].trim().toIntOrNull()
                    if (start != null && end != null && start <= end) {
                        for (w in start..end) {
                            result.add(w)
                        }
                    }
                }
            } else {
                trimmed.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    /**
     * 3 & 4. Resolves daily schedule:
     * - Chinese holiday auto-pause (silences all classes on holidays)
     * - Weekend make-up days (调休), substituting Saturday/Sunday with designated weekday's timetable
     */
    fun resolveDaySchedule(
        targetDate: LocalDate,
        semesterStartDate: LocalDate,
        totalWeeks: Int,
        allCourses: List<CourseEntity>,
        holidayOverrides: List<HolidayOverrideEntity>
    ): DayResolutionResult {
        val academicWeek = calculateAcademicWeek(semesterStartDate, targetDate)
        val dateString = targetDate.toString()
        val override = holidayOverrides.find { it.dateString == dateString }

        // 3. Chinese holiday auto-pause: silences all classes on holidays
        if (override != null && override.type == HolidayOverrideType.HOLIDAY) {
            return DayResolutionResult(
                date = targetDate,
                academicWeek = academicWeek,
                isHoliday = true,
                holidayName = override.name,
                isMakeUpDay = false,
                effectiveDayOfWeek = targetDate.dayOfWeek.value,
                activeCourses = emptyList()
            )
        }

        // 4. Weekend make-up days (调休): substituting weekend with designated weekday's timetable
        val isMakeUp = override != null && override.type == HolidayOverrideType.MAKE_UP && override.targetDayOfWeek != null
        val effectiveDayOfWeek = if (isMakeUp) {
            override!!.targetDayOfWeek!!
        } else {
            targetDate.dayOfWeek.value
        }

        val makeUpDesc = if (isMakeUp) {
            val dayName = when (effectiveDayOfWeek) {
                1 -> "Monday"
                2 -> "Tuesday"
                3 -> "Wednesday"
                4 -> "Thursday"
                5 -> "Friday"
                6 -> "Saturday"
                7 -> "Sunday"
                else -> "Day $effectiveDayOfWeek"
            }
            "${override?.name ?: "Make-up Day"} (Following $dayName Timetable)"
        } else null

        // Filter courses matching effectiveDayOfWeek and active in academicWeek
        val courses = allCourses
            .filter { course ->
                course.dayOfWeek == effectiveDayOfWeek &&
                        isCourseActiveInWeek(course.weekRule, course.customWeeks, academicWeek, totalWeeks)
            }
            .sortedWith(compareBy({ it.startPeriod }, { it.startTime }))

        return DayResolutionResult(
            date = targetDate,
            academicWeek = academicWeek,
            isHoliday = false,
            holidayName = null,
            isMakeUpDay = isMakeUp,
            effectiveDayOfWeek = effectiveDayOfWeek,
            makeUpDescription = makeUpDesc,
            activeCourses = courses
        )
    }

    /**
     * Resolves course status with live countdown:
     * - Completed (greyed out)
     * - Active (currently running)
     * - Next Up (first upcoming class today)
     * - Upcoming
     */
    fun resolveCourseStatuses(
        courses: List<CourseEntity>,
        currentTime: LocalTime
    ): List<CourseWithStatus> {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        var nextUpFound = false

        return courses.map { course ->
            val start = try {
                LocalTime.parse(course.startTime, formatter)
            } catch (e: Exception) {
                LocalTime.of(8, 0)
            }
            val end = try {
                LocalTime.parse(course.endTime, formatter)
            } catch (e: Exception) {
                LocalTime.of(9, 40)
            }

            val status: CourseClassStatus
            var minsUntilStart: Long = 0
            var minsRemaining: Long = 0

            when {
                currentTime.isAfter(end) -> {
                    status = CourseClassStatus.COMPLETED
                }
                !currentTime.isBefore(start) && !currentTime.isAfter(end) -> {
                    status = CourseClassStatus.ACTIVE
                    minsRemaining = ChronoUnit.MINUTES.between(currentTime, end).coerceAtLeast(0)
                }
                currentTime.isBefore(start) && !nextUpFound -> {
                    status = CourseClassStatus.NEXT_UP
                    nextUpFound = true
                    minsUntilStart = ChronoUnit.MINUTES.between(currentTime, start).coerceAtLeast(0)
                }
                else -> {
                    status = CourseClassStatus.UPCOMING
                    minsUntilStart = ChronoUnit.MINUTES.between(currentTime, start).coerceAtLeast(0)
                }
            }

            CourseWithStatus(
                course = course,
                status = status,
                minutesUntilStart = minsUntilStart,
                minutesRemaining = minsRemaining
            )
        }
    }
}
