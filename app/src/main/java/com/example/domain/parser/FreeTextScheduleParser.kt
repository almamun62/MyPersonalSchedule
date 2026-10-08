package com.example.domain.parser

import com.example.data.model.WeekRule
import com.example.domain.model.ImportedCourse

object FreeTextScheduleParser {

    private val COLORS = listOf(
        "#2563EB", "#10B981", "#F59E0B", "#EC4899",
        "#8B5CF6", "#EF4444", "#06B6D4", "#F97316"
    )

    /**
     * Advanced Universal Text Parser.
     * Accurately parses:
     * - Multi-line blocks (e.g. Course \n Room \n Day Time \n Teacher)
     * - Standard line-by-line schedules (e.g. "Linear Algebra, Mon 10:00-11:35, Room 204, Dr. Euler")
     * - Chinese educational portal copied text (e.g. "高等数学(上) [1-16周(单)] 星期一 1-2节 软件楼301 张伟教授")
     * - Tab-separated or matrix grids copied directly from spreadsheets
     * - WakeUp / JSON text blocks
     */
    fun parseText(text: String): List<ImportedCourse> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. Check if user pasted a Share Code (SCH#...)
        if (trimmed.startsWith("SCH#")) {
            val shareCourses = ShareCodeManager.parseShareCode(trimmed)
            if (shareCourses.isNotEmpty()) return shareCourses
        }

        // 2. Check if user pasted JSON
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            val jsonCourses = JsonScheduleParser.parseJsonSchedule(trimmed)
            if (jsonCourses.isNotEmpty()) return jsonCourses
        }

        // 3. Check if user pasted tab-separated or matrix table
        if (trimmed.contains("\t")) {
            val rows = trimmed.lines().filter { it.isNotBlank() }.map { it.split("\t") }
            val matrixCourses = MatrixTimetableParser.parseMatrixGrid(rows)
            if (matrixCourses.isNotEmpty()) return matrixCourses
        }

        // 4. Split by multiple newlines or single lines/semicolons
        val rawBlocks = if (trimmed.contains("\n\n")) {
            trimmed.split(Regex("\n{2,}")).map { it.trim() }.filter { it.isNotBlank() }
        } else {
            trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
        }

        val list = mutableListOf<ImportedCourse>()

        for (block in rawBlocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            // 1. Detect Day of Week
            val day = parseDayOfWeek(block)

            // 2. Detect Period Range (e.g. 1-2节, 第3-4节, P5-6, or clock time 14:00-15:35)
            val periodRange = extractPeriodRange(block)
            val hasExplicitPeriod = hasPeriodOrTime(block)

            // If neither day nor period/time is in this block, check if it's just a header or non-schedule text
            if (day == null && !hasExplicitPeriod) {
                continue
            }

            val finalDay = day ?: 1
            val startP = periodRange?.first ?: 1
            val endP = periodRange?.second ?: (startP + 1).coerceAtMost(12)

            // 3. Detect Clock Times (or derive from period numbers)
            val times = extractClockTimes(block, startP, endP)

            // 4. Extract Classroom
            val classroom = extractClassroom(lines, block)

            // 5. Extract Instructor
            val instructor = extractInstructor(lines, block)

            // 6. Extract Course Name
            val courseName = extractCourseName(lines, block)
            if (courseName.isBlank() || isMetadataLine(courseName)) continue

            // 7. Extract Course Code if present
            val code = extractCode(block)

            // 8. Extract Week Rule (ODD / EVEN / ALL)
            val weekRule = extractWeekRule(block)

            list.add(
                ImportedCourse(
                    name = courseName,
                    code = code,
                    classroom = classroom,
                    instructor = instructor,
                    dayOfWeek = finalDay,
                    startPeriod = startP,
                    endPeriod = endP,
                    startTime = times.first,
                    endTime = times.second,
                    weekRule = weekRule,
                    colorHex = COLORS[list.size % COLORS.size]
                )
            )
        }

        return list
    }

    private fun hasPeriodOrTime(text: String): Boolean {
        return text.contains("节") ||
                text.contains(":") ||
                Regex("(?i)\\bP\\d").containsMatchIn(text) ||
                Regex("\\b\\d{1,2}:\\d{2}\\b").containsMatchIn(text)
    }

    private fun extractWeekRule(text: String): WeekRule {
        val lower = text.lowercase()
        return when {
            lower.contains("单周") || lower.contains("(单)") || lower.contains("odd", ignoreCase = true) -> WeekRule.ODD
            lower.contains("双周") || lower.contains("(双)") || lower.contains("even", ignoreCase = true) -> WeekRule.EVEN
            else -> WeekRule.ALL
        }
    }

    private fun parseDayOfWeek(text: String): Int? {
        val lower = text.lowercase()
        return when {
            lower.contains("星期一") || lower.contains("周一") || lower.contains("礼拜一") || lower.contains("mon") -> 1
            lower.contains("星期二") || lower.contains("周二") || lower.contains("礼拜二") || lower.contains("tue") -> 2
            lower.contains("星期三") || lower.contains("周三") || lower.contains("礼拜三") || lower.contains("wed") -> 3
            lower.contains("星期四") || lower.contains("周四") || lower.contains("礼拜四") || lower.contains("thu") -> 4
            lower.contains("星期五") || lower.contains("周五") || lower.contains("礼拜五") || lower.contains("fri") -> 5
            lower.contains("星期六") || lower.contains("周六") || lower.contains("礼拜六") || lower.contains("sat") -> 6
            lower.contains("星期日") || lower.contains("星期天") || lower.contains("周日") || lower.contains("周天") || lower.contains("sun") -> 7
            else -> null
        }
    }

    private fun extractPeriodRange(text: String): Pair<Int, Int>? {
        // e.g. "第1-2节", "1-2节", "1-3", "P1-2", "Periods 3-5", "节次: 1-2"
        val regex = Regex("(?:第|p|period|节次)?\\s*(\\d{1,2})\\s*[-~至到]\\s*(\\d{1,2})\\s*节?", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        if (match != null) {
            val s = match.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: 1
            val e = match.groupValues[2].toIntOrNull()?.coerceIn(s, 12) ?: (s + 1)
            return Pair(s, e)
        }

        // Single period: e.g. "第3节"
        val singleRegex = Regex("(?:第|p|period)\\s*(\\d{1,2})\\s*节?", RegexOption.IGNORE_CASE)
        val singleMatch = singleRegex.find(text)
        if (singleMatch != null) {
            val s = singleMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: 1
            return Pair(s, s)
        }

        // Infer from clock time if present: e.g. 14:00 -> period 6
        val clockRegex = Regex("(\\d{1,2}):(\\d{2})")
        val clockMatches = clockRegex.findAll(text).toList()
        if (clockMatches.isNotEmpty()) {
            val h = clockMatches[0].groupValues[1].toIntOrNull() ?: 8
            val s = when {
                h <= 8 -> 1
                h == 9 -> 2
                h == 10 -> 3
                h == 11 -> 4
                h in 12..13 -> 5
                h == 14 -> 6
                h == 15 -> 7
                h == 16 -> 8
                h == 17 -> 9
                h in 18..19 -> 10
                h == 20 -> 11
                else -> 12
            }
            return Pair(s, (s + 1).coerceAtMost(12))
        }

        return null
    }

    private fun extractClockTimes(text: String, startP: Int, endP: Int): Pair<String, String> {
        val clockRegex = Regex("(\\d{1,2}):(\\d{2})")
        val matches = clockRegex.findAll(text).toList()
        if (matches.size >= 2) {
            val s = "%02d:%s".format(matches[0].groupValues[1].toInt(), matches[0].groupValues[2])
            val e = "%02d:%s".format(matches[1].groupValues[1].toInt(), matches[1].groupValues[2])
            return Pair(s, e)
        }
        return ScheduleParser.defaultTimesForPeriods(startP, endP)
    }

    private fun extractClassroom(lines: List<String>, block: String): String {
        for (line in lines) {
            if (line.contains("教室") || line.contains("地点") || line.contains("Room") || line.contains("Lab") || line.contains("Hall")) {
                val cleaned = line.replace(Regex("(?:教室|地点|上课地点|Room|Location)\\s*[:：]?"), "").trim()
                if (cleaned.isNotBlank()) return cleaned.take(30)
            }
            if (line.matches(Regex(".*(?:楼|室|馆|实验室|Hall|Lab)\\s*\\d*.*", RegexOption.IGNORE_CASE))) {
                return line.trim().take(30)
            }
        }

        val match = Regex("(?:教室|地点|Room|Lab|Building)\\s*[:：]?\\s*([A-Za-z0-9\\u4e00-\\u9fa5\\-\\s]{2,15})").find(block)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        val codeRoomMatch = Regex("\\b([A-Z]\\d{3,4}|\\d{3,4}[A-Z]?)\\b").find(block)
        if (codeRoomMatch != null) {
            return "Room " + codeRoomMatch.value
        }

        return "Classroom"
    }

    private fun extractInstructor(lines: List<String>, block: String): String {
        for (line in lines) {
            if (line.contains("教师") || line.contains("老师") || line.contains("讲师") || line.contains("教授") || line.contains("Teacher") || line.contains("Instructor") || line.contains("Prof")) {
                val cleaned = line.replace(Regex("(?:授课教师|教师|老师|任课教师|Instructor|Teacher|Prof|Professor)\\s*[:：]?"), "").trim()
                if (cleaned.isNotBlank()) return cleaned.take(20)
            }
        }

        val profMatch = Regex("(?:教师|老师|Instructor|Prof|Professor|Dr\\.)\\s*[:：]?\\s*([A-Za-z\\u4e00-\\u9fa5]{2,10})").find(block)
        if (profMatch != null) {
            return profMatch.groupValues[1].trim()
        }

        return ""
    }

    private fun extractCourseName(lines: List<String>, block: String): String {
        val firstLine = lines.firstOrNull()?.trim() ?: ""
        if (firstLine.isNotBlank() && !isMetadataLine(firstLine)) {
            val clean = firstLine
                .replace(Regex("\\[.*?\\]"), "")
                .replace(Regex("【.*?】"), "")
                .replace(Regex("（.*?）"), "")
                .replace(Regex("\\(.*?\\)"), "")
                .trim()
            if (clean.isNotBlank()) return clean.take(35)
        }

        var cleaned = block
            .replace(Regex("(?:周|星期)[一二三四五六日天1-7]"), "")
            .replace(Regex("(?:第|p|period)?\\s*\\d{1,2}\\s*[-~至到]\\s*\\d{1,2}\\s*节?"), "")
            .replace(Regex("\\[\\d+-\\d+周.*?\\]"), "")
            .replace(Regex("(?:教室|地点|Room|Lab).*"), "")
            .replace(Regex("(?:教师|老师|Prof|Dr\\.).*"), "")
            .replace(Regex("[,;:()\\[\\]]"), " ")
            .trim()

        val words = cleaned.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val result = if (words.isNotEmpty()) words.take(4).joinToString(" ") else ""
        return result.take(35)
    }

    private fun isMetadataLine(line: String): Boolean {
        val lower = line.lowercase()
        return lower.startsWith("星期") || lower.startsWith("周") || lower.startsWith("mon") || lower.startsWith("tue") ||
               lower.startsWith("wed") || lower.startsWith("thu") || lower.startsWith("fri") || lower.startsWith("time") ||
               lower.startsWith("room") || lower.startsWith("teacher") || lower.startsWith("节次")
    }

    private fun extractCode(block: String): String {
        val match = Regex("\\b([A-Z]{2,4}\\d{3,4}|\\d{8,10})\\b").find(block)
        return match?.value ?: ""
    }
}
