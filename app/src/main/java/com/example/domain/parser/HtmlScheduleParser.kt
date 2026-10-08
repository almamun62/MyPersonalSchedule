package com.example.domain.parser

import com.example.domain.model.ImportedCourse

object HtmlScheduleParser {

    /**
     * Highly resilient HTML Educational Portal Timetable Parser.
     * Supports:
     * - 正方教务系统 (Zhengfang) table formats
     * - 强智教务 (Qiangzhi)
     * - 清华/北大/浙大/川大 standard HTML exports
     * - International HTML tables (Mon-Fri column grids or chronological tables)
     */
    fun parseHtmlContent(html: String): List<ImportedCourse> {
        val list = mutableListOf<ImportedCourse>()

        // 1. Check if the HTML is a 2D Grid Table (Columns = Days Mon-Sun, Rows = Periods 1-12)
        // Extract <tr> rows
        val trRegex = Regex("(?i)(?s)<tr[^>]*>(.*?)</tr>")
        val tdThRegex = Regex("(?i)(?s)<(td|th)[^>]*>(.*?)</\\1>")

        val rows = trRegex.findAll(html).map { trMatch ->
            tdThRegex.findAll(trMatch.groupValues[1]).map { cellMatch ->
                // Strip HTML tags and entities
                cellMatch.groupValues[2]
                    .replace(Regex("(?i)<br\\s*/?>"), "\n")
                    .replace(Regex("<[^>]*>"), " ")
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .trim()
            }.toList()
        }.toList()

        if (rows.isEmpty()) {
            return ScheduleParser.parseFreeTextSchedule(html)
        }

        // Try high-precision 2D Matrix Timetable engine first
        val matrixCourses = MatrixTimetableParser.parseMatrixGrid(rows)
        if (matrixCourses.isNotEmpty()) {
            return matrixCourses
        }

        // Check if there is a header row with days of the week (星期一..星期日 or Mon..Sun)
        var dayColumnMap = mutableMapOf<Int, Int>() // columnIndex -> dayOfWeek (1..7)
        for (row in rows.take(5)) {
            row.forEachIndexed { colIdx, text ->
                val day = parseDayHeader(text)
                if (day != null) {
                    dayColumnMap[colIdx] = day
                }
            }
            if (dayColumnMap.isNotEmpty()) break
        }

        // If day columns detected, parse 2D timetable grid:
        if (dayColumnMap.size >= 3) {
            var periodCounter = 1
            for (row in rows) {
                // Determine if row has period indicator in first 1-2 columns
                val periodInRow = row.firstOrNull { it.matches(Regex(".*(?:第?(\\d{1,2})节|period\\s*(\\d{1,2})|\\b(\\d{1,2})\\b).*")) }
                val parsedRowPeriod = periodInRow?.let { extractPeriodNumber(it) } ?: periodCounter

                for ((colIdx, dayOfWeek) in dayColumnMap) {
                    val cellText = row.getOrNull(colIdx) ?: continue
                    if (cellText.isNotBlank() && cellText.length >= 2 && !cellText.contains("星期") && !cellText.contains("节次")) {
                        val parsedInCell = parseCellCourses(cellText, dayOfWeek, parsedRowPeriod)
                        list.addAll(parsedInCell)
                    }
                }
                periodCounter++
            }
        } else {
            // Check for record-based tables (Columns = [Course, Day, Time, Room, Teacher])
            val headerRow = rows.firstOrNull { r ->
                r.any { it.contains("课程") || it.contains("Course") || it.contains("Name") || it.contains("Title") }
            }

            if (headerRow != null) {
                val csvBuffer = StringBuilder()
                for (row in rows) {
                    val line = row.joinToString(",") { cell ->
                        val escaped = cell.replace("\"", "\"\"").replace("\n", " ")
                        "\"$escaped\""
                    }
                    csvBuffer.append(line).append("\n")
                }
                return ScheduleParser.parseCsvSchedule(csvBuffer.toString())
            }

            // Fallback: extract any rich course text found in cells
            for (row in rows) {
                for (cell in row) {
                    if (cell.length > 6 && (cell.contains("节") || cell.contains(":") || cell.contains("周") || cell.contains("Room"))) {
                        list.addAll(ScheduleParser.parseFreeTextSchedule(cell))
                    }
                }
            }
        }

        return list.ifEmpty { ScheduleParser.parseFreeTextSchedule(html) }
    }

    private fun parseDayHeader(text: String): Int? {
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
     * Parses a single cell in a Chinese/University schedule table which often has:
     * Course Name
     * [1-16周] / 单周
     * [1-2节] / 8:00-9:35
     * 软件楼301 / Room 301
     * 张教授 / Prof. Zhang
     */
    private fun parseCellCourses(cellText: String, dayOfWeek: Int, fallbackPeriod: Int): List<ImportedCourse> {
        val lines = cellText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val results = mutableListOf<ImportedCourse>()
        // A single cell might have multiple classes separated by empty lines or dashes
        val courseBlocks = cellText.split(Regex("\n{2,}")).map { it.trim() }.filter { it.length > 2 }

        for (block in courseBlocks) {
            val blockLines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            val name = blockLines.firstOrNull() ?: "Course"

            var room = ""
            var teacher = ""
            var startP = fallbackPeriod
            var endP = (fallbackPeriod + 1).coerceAtMost(12)

            for (l in blockLines.drop(1)) {
                // Check period range: 1-2节, 第3-4节, P3-5
                val pMatch = Regex("(?:第|p)?\\s*(\\d{1,2})\\s*[-~至到]\\s*(\\d{1,2})\\s*节?", RegexOption.IGNORE_CASE).find(l)
                if (pMatch != null) {
                    startP = pMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: startP
                    endP = pMatch.groupValues[2].toIntOrNull()?.coerceIn(startP, 12) ?: (startP + 1)
                }

                // Check classroom
                if (l.contains("楼") || l.contains("室") || l.contains("馆") || l.contains("院") || l.contains("Room") || l.contains("Lab") || l.contains("Hall")) {
                    room = l
                }

                // Check instructor
                if (l.contains("老师") || l.contains("教授") || l.contains("讲师") || l.contains("Prof") || l.contains("Dr.")) {
                    teacher = l
                }
            }

            val defaultTimes = ScheduleParser.defaultTimesForPeriods(startP, endP)
            results.add(
                ImportedCourse(
                    name = name,
                    code = "",
                    classroom = room.ifBlank { "Classroom" },
                    instructor = teacher.ifBlank { "Instructor" },
                    dayOfWeek = dayOfWeek,
                    startPeriod = startP,
                    endPeriod = endP,
                    startTime = defaultTimes.first,
                    endTime = defaultTimes.second,
                    colorHex = listOf("#2563EB", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6", "#EF4444")[results.size % 6]
                )
            )
        }

        return results
    }
}
