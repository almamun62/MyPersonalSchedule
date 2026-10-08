package com.example.domain.parser

import com.example.data.model.WeekRule
import com.example.domain.model.ImportedCourse

/**
 * High-performance 100% offline 2D Matrix Timetable Parser.
 * Handles spreadsheet grids (Excel / CSV / TSV / HTML tables) where:
 * - Columns are Days of Week (Mon - Sun / 星期一 - 星期日)
 * - Rows are Periods (1 - 12 / 第1节 - 第12节 / 08:00 - 21:00)
 * OR vice-versa!
 */
object MatrixTimetableParser {

    private val COLORS = listOf(
        "#2563EB", "#10B981", "#F59E0B", "#EC4899",
        "#8B5CF6", "#EF4444", "#06B6D4", "#F97316"
    )

    /**
     * Parses a 2D matrix of strings (rows and columns).
     * Returns empty list if the table is not a 2D timetable matrix.
     */
    fun parseMatrixGrid(rows: List<List<String>>): List<ImportedCourse> {
        if (rows.size < 2) return emptyList()

        // 1. Check if Days of Week are along COLUMNS (most common)
        val dayColumnMap = findDayColumns(rows)
        if (dayColumnMap.size >= 3) {
            return parseColumnBasedMatrix(rows, dayColumnMap)
        }

        // 2. Check if Days of Week are along ROWS
        val dayRowMap = findDayRows(rows)
        if (dayRowMap.size >= 3) {
            return parseRowBasedMatrix(rows, dayRowMap)
        }

        return emptyList()
    }

    private fun findDayColumns(rows: List<List<String>>): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        // Inspect the first 5 rows for day headers
        for (row in rows.take(5)) {
            row.forEachIndexed { colIdx, text ->
                val day = parseDayOfWeek(text)
                if (day != null && !map.containsKey(colIdx)) {
                    map[colIdx] = day
                }
            }
            if (map.size >= 3) break
        }
        return map
    }

    private fun findDayRows(rows: List<List<String>>): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        rows.forEachIndexed { rowIdx, row ->
            val firstCell = row.firstOrNull { it.isNotBlank() } ?: ""
            val day = parseDayOfWeek(firstCell)
            if (day != null) {
                map[rowIdx] = day
            }
        }
        return map
    }

    private fun parseColumnBasedMatrix(rows: List<List<String>>, dayColumnMap: Map<Int, Int>): List<ImportedCourse> {
        val courses = mutableListOf<ImportedCourse>()
        var periodCounter = 1

        for (row in rows) {
            // Check if this row is itself a header row containing "星期" or "Day"
            val isHeaderRow = row.count { parseDayOfWeek(it) != null } >= 3
            if (isHeaderRow) continue

            // Check if row has explicit period indicator in first cells
            val periodInRow = row.take(2).firstNotNullOfOrNull { extractPeriodNumber(it) }
            val currentPeriod = periodInRow ?: periodCounter

            for ((colIdx, dayOfWeek) in dayColumnMap) {
                val cellText = row.getOrNull(colIdx)?.trim() ?: continue
                if (cellText.length >= 2 && !cellText.contains("星期") && !cellText.contains("节次")) {
                    val parsedCourses = parseCellContent(cellText, dayOfWeek, currentPeriod, courses.size)
                    courses.addAll(parsedCourses)
                }
            }
            periodCounter++
        }

        return deduplicateAndMergeCourses(courses)
    }

    private fun parseRowBasedMatrix(rows: List<List<String>>, dayRowMap: Map<Int, Int>): List<ImportedCourse> {
        val courses = mutableListOf<ImportedCourse>()

        for ((rowIdx, dayOfWeek) in dayRowMap) {
            val row = rows.getOrNull(rowIdx) ?: continue
            for (colIdx in 1 until row.size) {
                val cellText = row.getOrNull(colIdx)?.trim() ?: continue
                if (cellText.length >= 2) {
                    val period = colIdx.coerceIn(1, 12)
                    val parsedCourses = parseCellContent(cellText, dayOfWeek, period, courses.size)
                    courses.addAll(parsedCourses)
                }
            }
        }

        return deduplicateAndMergeCourses(courses)
    }

    /**
     * Parses a cell containing course information.
     * Often formatted as:
     * Course Name
     * [1-16周(单)] or 1-16周
     * [3-4节]
     * Room B101 / 软件楼201
     * Prof. Zhang / 张教授
     */
    fun parseCellContent(cellText: String, dayOfWeek: Int, fallbackPeriod: Int, colorOffset: Int): List<ImportedCourse> {
        val blocks = cellText.split(Regex("\n{2,}|;\\s*")).map { it.trim() }.filter { it.length >= 2 }
        val result = mutableListOf<ImportedCourse>()

        for ((idx, block) in blocks.withIndex()) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            val rawName = lines.first()
            if (rawName.contains("午休") || rawName.contains("课间") || rawName.contains("晚上")) continue

            var room = ""
            var teacher = ""
            var startP = fallbackPeriod
            var endP = (fallbackPeriod + 1).coerceAtMost(12)
            var weekRule = WeekRule.ALL

            // Parse week rule
            if (block.contains("单周") || block.contains("(单)") || block.contains("odd", ignoreCase = true)) {
                weekRule = WeekRule.ODD
            } else if (block.contains("双周") || block.contains("(双)") || block.contains("even", ignoreCase = true)) {
                weekRule = WeekRule.EVEN
            }

            for (line in lines.drop(1)) {
                // Period range regex: e.g. 1-2节, 第3-4节, P3-5, 3-4
                val pMatch = Regex("(?:第|p|period)?\\s*(\\d{1,2})\\s*[-~至到]\\s*(\\d{1,2})\\s*节?", RegexOption.IGNORE_CASE).find(line)
                if (pMatch != null) {
                    startP = pMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: startP
                    endP = pMatch.groupValues[2].toIntOrNull()?.coerceIn(startP, 12) ?: (startP + 1)
                    continue
                }

                // Room indicators: 楼, 室, 馆, 房, 操场, 实验室, Room, Hall, Lab, Bldg
                if (room.isEmpty() && (line.contains("楼") || line.contains("室") || line.contains("馆") || line.contains("房") ||
                                line.contains("实验") || line.contains("Room", ignoreCase = true) || line.contains("Lab", ignoreCase = true) ||
                                line.contains("Hall", ignoreCase = true) || line.contains("Bldg", ignoreCase = true) ||
                                line.matches(Regex(".*[A-Za-z]\\d{2,4}.*")))) {
                    room = line.replace(Regex("[\\[\\]()]"), "").trim()
                    continue
                }

                // Teacher indicators: 老师, 教授, 讲师, 助教, Prof, Dr, Mr, Ms
                if (teacher.isEmpty() && (line.contains("老师") || line.contains("教授") || line.contains("讲师") ||
                                line.contains("Prof", ignoreCase = true) || line.contains("Dr", ignoreCase = true) ||
                                (line.length in 2..4 && !line.any { it.isDigit() } && !line.contains("周")))) {
                    teacher = line.replace(Regex("[\\[\\]()]"), "").trim()
                    continue
                }
            }

            val (sTime, eTime) = ScheduleParser.defaultTimesForPeriods(startP, endP)
            val color = COLORS[(colorOffset + idx) % COLORS.size]

            result.add(
                ImportedCourse(
                    name = cleanCourseName(rawName),
                    classroom = room.ifBlank { "Classroom" },
                    instructor = teacher,
                    dayOfWeek = dayOfWeek,
                    startPeriod = startP,
                    endPeriod = endP,
                    startTime = sTime,
                    endTime = eTime,
                    weekRule = weekRule,
                    colorHex = color
                )
            )
        }

        return result
    }

    private fun cleanCourseName(name: String): String {
        return name
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("【.*?】"), "")
            .replace(Regex("（.*?）"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .trim()
            .ifBlank { name }
    }

    private fun parseDayOfWeek(text: String): Int? {
        val lower = text.trim().lowercase()
        return when {
            lower.contains("星期一") || lower.contains("周一") || lower.contains("mon") -> 1
            lower.contains("星期二") || lower.contains("周二") || lower.contains("tue") -> 2
            lower.contains("星期三") || lower.contains("周三") || lower.contains("wed") -> 3
            lower.contains("星期四") || lower.contains("周四") || lower.contains("thu") -> 4
            lower.contains("星期五") || lower.contains("周五") || lower.contains("fri") -> 5
            lower.contains("星期六") || lower.contains("周六") || lower.contains("sat") -> 6
            lower.contains("星期日") || lower.contains("星期天") || lower.contains("周日") || lower.contains("sun") -> 7
            else -> null
        }
    }

    private fun extractPeriodNumber(text: String): Int? {
        val match = Regex("(\\d{1,2})").find(text)
        return match?.value?.toIntOrNull()?.coerceIn(1, 12)
    }

    /**
     * If multiple cells in adjacent rows belong to the same course (e.g. Row 1: Math, Row 2: Math),
     * merge them into one continuous course.
     */
    private fun deduplicateAndMergeCourses(courses: List<ImportedCourse>): List<ImportedCourse> {
        val merged = mutableListOf<ImportedCourse>()

        for (c in courses) {
            val existing = merged.firstOrNull {
                it.name == c.name &&
                it.dayOfWeek == c.dayOfWeek &&
                it.weekRule == c.weekRule &&
                (it.endPeriod + 1 == c.startPeriod || it.endPeriod >= c.startPeriod && it.startPeriod <= c.endPeriod)
            }

            if (existing != null) {
                existing.startPeriod = minOf(existing.startPeriod, c.startPeriod)
                existing.endPeriod = maxOf(existing.endPeriod, c.endPeriod)
                val (s, e) = ScheduleParser.defaultTimesForPeriods(existing.startPeriod, existing.endPeriod)
                existing.startTime = s
                existing.endTime = e
                if (existing.classroom == "Classroom" && c.classroom != "Classroom") {
                    existing.classroom = c.classroom
                }
                if (existing.instructor.isBlank() && c.instructor.isNotBlank()) {
                    existing.instructor = c.instructor
                }
            } else {
                merged.add(c)
            }
        }

        return merged
    }
}
