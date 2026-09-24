package com.example.domain

import com.example.data.model.CourseEntity
import com.example.data.model.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * Recharts-compatible data point representing a single interval (day or week).
 * Compatible with Recharts `<BarChart data={...}>`:
 * ```jsx
 * <ResponsiveContainer width="100%" height={300}>
 *   <BarChart data={data}>
 *     <CartesianGrid strokeDasharray="3 3" />
 *     <XAxis dataKey="name" />
 *     <YAxis unit="h" />
 *     <Tooltip />
 *     <Legend />
 *     <Bar dataKey="classHours" fill="#3B82F6" name="Class Hours" />
 *     <Bar dataKey="studyHours" fill="#10B981" name="Study Tasks" />
 *   </BarChart>
 * </ResponsiveContainer>
 * ```
 */
data class RechartsBarDataPoint(
    val name: String,             // Key used by Recharts XAxis dataKey="name" (e.g. "Mon", "Tue", "W1")
    val label: String,            // Full display label (e.g. "Monday", "Week 3")
    val classHours: Float,        // Hours in lecture / classes
    val studyHours: Float,        // Hours on study tasks / homework
    val totalHours: Float,        // Combined workload (classHours + studyHours)
    val classCount: Int = 0,      // Number of class sessions
    val taskCount: Int = 0,       // Number of assigned / completed tasks
    val dayOfWeek: Int = 0,       // 1 = Monday .. 7 = Sunday (0 for week intervals)
    val classNames: List<String> = emptyList(),
    val taskTitles: List<String> = emptyList()
) {
    /**
     * Converts to valid JSON string compatible with Recharts JavaScript structures.
     */
    fun toJsonString(): String {
        val classJsonArray = classNames.joinToString(prefix = "[", postfix = "]") {
            "\"${it.replace("\"", "\\\"")}\""
        }
        val taskJsonArray = taskTitles.joinToString(prefix = "[", postfix = "]") {
            "\"${it.replace("\"", "\\\"")}\""
        }
        return """{
  "name": "$name",
  "label": "$label",
  "classHours": $classHours,
  "studyHours": $studyHours,
  "totalHours": $totalHours,
  "classCount": $classCount,
  "taskCount": $taskCount,
  "dayOfWeek": $dayOfWeek,
  "classes": $classJsonArray,
  "tasks": $taskJsonArray
}""".trimIndent()
    }
}

/**
 * Complete summary report containing week-level metrics, daily Recharts breakdown,
 * and semester-level trend Recharts data.
 */
data class WeeklySummaryReport(
    val weekNumber: Int,
    val totalClassHours: Float,
    val totalStudyHours: Float,
    val totalHours: Float,
    val studyToClassRatio: Float,
    val ratioStatus: String, // "Optimal", "Class Heavy", "Study Intensive", "Light Load"
    val ratioBenchmarkRecommendation: String,
    val peakDayName: String,
    val peakDayHours: Float,
    val dailyBreakdown: List<RechartsBarDataPoint>,
    val semesterTrend: List<RechartsBarDataPoint>
) {
    /**
     * Generates a formatted Recharts-compatible JSON array string for the daily breakdown.
     */
    fun toDailyRechartsJson(): String {
        return dailyBreakdown.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") {
            "  " + it.toJsonString().replace("\n", "\n  ")
        }
    }

    /**
     * Generates a formatted Recharts-compatible JSON array string for the semester trend.
     */
    fun toSemesterRechartsJson(): String {
        return semesterTrend.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") {
            "  " + it.toJsonString().replace("\n", "\n  ")
        }
    }
}

object WeeklySummaryEngine {

    private val hourPattern = Pattern.compile("(?i)(?:^|\\s|\\()(\\d+(?:\\.\\d+)?)\\s*(?:h|hr|hrs|hours?)(?:$|\\s|\\))")
    private val minPattern = Pattern.compile("(?i)(?:^|\\s|\\()(\\d+)\\s*(?:m|min|mins|minutes?)(?:$|\\s|\\))")

    /**
     * Calculates the estimated or actual duration in hours of a course session.
     */
    fun calculateCourseHours(course: CourseEntity): Float {
        try {
            if (course.startTime.contains(":") && course.endTime.contains(":")) {
                val startParts = course.startTime.split(":")
                val endParts = course.endTime.split(":")
                val startMin = startParts[0].trim().toInt() * 60 + startParts[1].trim().toInt()
                val endMin = endParts[0].trim().toInt() * 60 + endParts[1].trim().toInt()
                val diff = endMin - startMin
                if (diff > 0) {
                    return round1Decimal(diff / 60.0f)
                }
            }
        } catch (_: Exception) {}

        // Fallback: 50 minutes per period block
        val periodSpan = (course.endPeriod - course.startPeriod + 1).coerceAtLeast(1)
        return round1Decimal(periodSpan * (50f / 60f))
    }

    /**
     * Calculates the estimated study hours for a task based on explicit title tags or priority.
     */
    fun calculateTaskHours(task: TaskEntity): Float {
        val title = task.title

        // Check for explicit hour annotation e.g. "Homework 3 (2h)"
        val hourMatcher = hourPattern.matcher(title)
        if (hourMatcher.find()) {
            val h = hourMatcher.group(1)?.toFloatOrNull()
            if (h != null && h > 0f) return round1Decimal(h)
        }

        // Check for minutes annotation e.g. "Read slides (45min)"
        val minMatcher = minPattern.matcher(title)
        if (minMatcher.find()) {
            val m = minMatcher.group(1)?.toFloatOrNull()
            if (m != null && m > 0f) return round1Decimal(m / 60f)
        }

        // Default heuristic based on priority
        return when (task.priority.uppercase(Locale.ROOT)) {
            "HIGH" -> 2.5f
            "MEDIUM" -> 1.5f
            "LOW" -> 0.75f
            else -> 1.5f
        }
    }

    /**
     * Computes the complete WeeklySummaryReport with Recharts-compatible daily and semester-wide data.
     */
    fun computeSummaryReport(
        courses: List<CourseEntity>,
        tasks: List<TaskEntity>,
        targetWeek: Int,
        totalWeeks: Int = 16,
        semesterStartDate: LocalDate
    ): WeeklySummaryReport {
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val fullDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        // 1. Filter active courses for targetWeek
        val activeCoursesThisWeek = courses.filter { course ->
            course.dayOfWeek in 1..7 && course.startPeriod > 0 &&
            ScheduleEngine.isCourseActiveInWeek(
                rule = course.weekRule,
                customWeeks = course.customWeeks,
                currentWeek = targetWeek,
                totalWeeks = totalWeeks
            )
        }

        // Map courses by day of week (1..7)
        val coursesByDay = activeCoursesThisWeek.groupBy { it.dayOfWeek }

        // 2. Map tasks to days in targetWeek
        val tasksByDay = mutableMapOf<Int, MutableList<TaskEntity>>()
        for (d in 1..7) {
            tasksByDay[d] = mutableListOf()
        }

        val unassignedTasks = mutableListOf<TaskEntity>()

        for (task in tasks) {
            var assignedDay: Int? = null

            if (task.dueDateMillis > 0) {
                val dueDate = Instant.ofEpochMilli(task.dueDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val week = ScheduleEngine.calculateAcademicWeek(semesterStartDate, dueDate)
                if (week == targetWeek) {
                    assignedDay = dueDate.dayOfWeek.value
                }
            } else if (task.createdAtMillis > 0) {
                val createdDate = Instant.ofEpochMilli(task.createdAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val week = ScheduleEngine.calculateAcademicWeek(semesterStartDate, createdDate)
                if (week == targetWeek) {
                    assignedDay = createdDate.dayOfWeek.value
                }
            }

            if (assignedDay != null && assignedDay in 1..7) {
                tasksByDay[assignedDay]?.add(task)
            } else {
                unassignedTasks.add(task)
            }
        }

        // Distribute unassigned tasks across active course days or weekdays
        unassignedTasks.forEachIndexed { index, task ->
            // If task is associated with a course that meets this week, assign to that day
            val matchingCourse = activeCoursesThisWeek.firstOrNull {
                task.courseName.isNotBlank() && it.name.contains(task.courseName, ignoreCase = true)
            }

            val targetDay = when {
                matchingCourse != null -> matchingCourse.dayOfWeek
                else -> (index % 5) + 1 // Mon..Fri rotation
            }
            tasksByDay[targetDay]?.add(task)
        }

        // 3. Build Daily RechartsBarDataPoints (Mon..Sun)
        val dailyPoints = (1..7).map { dayNum ->
            val dayCourses = coursesByDay[dayNum] ?: emptyList()
            val dayTasks = tasksByDay[dayNum] ?: emptyList()

            val classHours = round1Decimal(dayCourses.sumOf { calculateCourseHours(it).toDouble() }.toFloat())
            val studyHours = round1Decimal(dayTasks.sumOf { calculateTaskHours(it).toDouble() }.toFloat())
            val totalHours = round1Decimal(classHours + studyHours)

            RechartsBarDataPoint(
                name = dayNames[dayNum - 1],
                label = fullDayNames[dayNum - 1],
                classHours = classHours,
                studyHours = studyHours,
                totalHours = totalHours,
                classCount = dayCourses.size,
                taskCount = dayTasks.size,
                dayOfWeek = dayNum,
                classNames = dayCourses.map { it.name }.distinct(),
                taskTitles = dayTasks.map { it.title }
            )
        }

        val totalClassHours = round1Decimal(dailyPoints.sumOf { it.classHours.toDouble() }.toFloat())
        val totalStudyHours = round1Decimal(dailyPoints.sumOf { it.studyHours.toDouble() }.toFloat())
        val totalCombinedHours = round1Decimal(totalClassHours + totalStudyHours)

        val studyToClassRatio = if (totalClassHours > 0f) {
            round1Decimal(totalStudyHours / totalClassHours)
        } else {
            if (totalStudyHours > 0f) 1.0f else 0.0f
        }

        val (ratioStatus, recommendation) = when {
            totalClassHours == 0f && totalStudyHours == 0f ->
                "No Load" to "No classes or tasks scheduled this week."
            studyToClassRatio < 0.5f ->
                "Class Heavy" to "Classroom heavy. Target at least 1.0h study per class hour for deep retention."
            studyToClassRatio in 0.5f..1.5f ->
                "Optimal" to "Healthy balance between lecture attendance and active independent study."
            studyToClassRatio in 1.5f..2.5f ->
                "Study Intensive" to "Strong self-study focus. Suitable for exam preparation and projects."
            else ->
                "Very High Load" to "High independent workload. Pace yourself to prevent burnout."
        }

        val peakPoint = dailyPoints.maxByOrNull { it.totalHours } ?: dailyPoints.first()

        // 4. Build Semester Trend RechartsBarDataPoints (W1..W[totalWeeks])
        val semesterTrendPoints = (1..totalWeeks.coerceIn(1, 24)).map { w ->
            val weekCourses = courses.filter { course ->
                course.dayOfWeek in 1..7 && course.startPeriod > 0 &&
                ScheduleEngine.isCourseActiveInWeek(
                    rule = course.weekRule,
                    customWeeks = course.customWeeks,
                    currentWeek = w,
                    totalWeeks = totalWeeks
                )
            }

            val weekClassHours = round1Decimal(weekCourses.sumOf { calculateCourseHours(it).toDouble() }.toFloat())

            // Compute or scale estimated study tasks across weeks
            val weekStudyHours = round1Decimal(
                if (w == targetWeek) {
                    totalStudyHours
                } else {
                    // Estimated study tasks proportional to class load
                    (weekClassHours * 0.7f).coerceAtLeast(2.0f)
                }
            )

            RechartsBarDataPoint(
                name = "W$w",
                label = "Week $w",
                classHours = weekClassHours,
                studyHours = weekStudyHours,
                totalHours = round1Decimal(weekClassHours + weekStudyHours),
                classCount = weekCourses.size,
                taskCount = if (w == targetWeek) tasks.size else (weekCourses.size * 0.8f).toInt(),
                dayOfWeek = 0
            )
        }

        return WeeklySummaryReport(
            weekNumber = targetWeek,
            totalClassHours = totalClassHours,
            totalStudyHours = totalStudyHours,
            totalHours = totalCombinedHours,
            studyToClassRatio = studyToClassRatio,
            ratioStatus = ratioStatus,
            ratioBenchmarkRecommendation = recommendation,
            peakDayName = peakPoint.label,
            peakDayHours = peakPoint.totalHours,
            dailyBreakdown = dailyPoints,
            semesterTrend = semesterTrendPoints
        )
    }

    private fun round1Decimal(value: Float): Float {
        return (value * 10).roundToInt() / 10f
    }
}
