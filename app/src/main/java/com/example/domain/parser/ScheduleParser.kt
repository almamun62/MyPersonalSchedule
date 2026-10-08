package com.example.domain.parser

import com.example.domain.model.ImportedCourse

object ScheduleParser {

    /**
     * Smart CSV / TSV / Semicolon-separated parser with automatic header & delimiter detection.
     * Supports both English and Chinese headers (e.g., 课程名称, 教室, 教师, 星期, 节次).
     */
    fun parseCsvSchedule(csvContent: String): List<ImportedCourse> {
        val trimmed = csvContent.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lines = trimmed.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        // Determine delimiter (comma, tab, or semicolon)
        val firstLine = lines.first()
        val delimiter = when {
            firstLine.contains("\t") -> "\t"
            firstLine.contains(";") -> ";"
            else -> ","
        }

        // 2. Check if this is a 2D Matrix Grid (Columns = Days, Rows = Periods or vice-versa)
        val rowList = lines.map { it.split(delimiter).map { cell -> cell.trim().removeSurrounding("\"") } }
        val matrixCourses = MatrixTimetableParser.parseMatrixGrid(rowList)
        if (matrixCourses.isNotEmpty()) {
            return matrixCourses
        }

        val list = mutableListOf<ImportedCourse>()
        val headerParts = firstLine.split(delimiter).map { it.trim().removeSurrounding("\"").lowercase() }

        // Smart column index detection
        var nameIdx = headerParts.indexOfFirst { it.contains("name") || it.contains("course") || it.contains("title") || it.contains("课程") }
        var codeIdx = headerParts.indexOfFirst { it.contains("code") || it.contains("id") || it.contains("代码") || it.contains("编号") }
        var roomIdx = headerParts.indexOfFirst { it.contains("room") || it.contains("class") || it.contains("loc") || it.contains("教室") || it.contains("地点") }
        var teacherIdx = headerParts.indexOfFirst { it.contains("teach") || it.contains("instruct") || it.contains("prof") || it.contains("教师") || it.contains("老师") }
        var dayIdx = headerParts.indexOfFirst { it.contains("day") || it.contains("week") || it.contains("星期") || it.contains("周") }
        var startPIdx = headerParts.indexOfFirst { it.contains("startp") || it.contains("fromp") || it.contains("start period") || it.contains("开始节") || it.contains("节次") }
        var endPIdx = headerParts.indexOfFirst { it.contains("endp") || it.contains("top") || it.contains("end period") || it.contains("结束节") }
        var weekRuleIdx = headerParts.indexOfFirst { it.contains("rule") || it.contains("odd") || it.contains("even") || it.contains("单双") || it.contains("周次") }

        val hasHeader = nameIdx != -1 || dayIdx != -1 || roomIdx != -1
        val startLineIdx = if (hasHeader) 1 else 0

        // Defaults if header missing
        if (nameIdx == -1) nameIdx = 0
        if (codeIdx == -1) codeIdx = 1
        if (roomIdx == -1) roomIdx = 2
        if (teacherIdx == -1) teacherIdx = 3
        if (dayIdx == -1) dayIdx = 4
        if (startPIdx == -1) startPIdx = 5
        if (endPIdx == -1) endPIdx = 6

        for (i in startLineIdx until lines.size) {
            val line = lines[i]
            val parts = line.split(delimiter).map { it.trim().removeSurrounding("\"") }
            if (parts.size >= 2) {
                val name = parts.getOrNull(nameIdx)?.ifEmpty { "Course $i" } ?: "Course $i"
                val code = parts.getOrNull(codeIdx) ?: ""
                val classroom = parts.getOrNull(roomIdx) ?: ""
                val instructor = parts.getOrNull(teacherIdx) ?: ""

                val rawDay = parts.getOrNull(dayIdx)
                val day = parseDayOfWeek(rawDay ?: "") ?: ((i % 5) + 1)

                val startP = parts.getOrNull(startPIdx)?.toIntOrNull()?.coerceIn(1, 12) ?: 1
                val endP = parts.getOrNull(endPIdx)?.toIntOrNull()?.coerceIn(startP, 12) ?: (startP + 1)

                val rawRule = parts.getOrNull(weekRuleIdx)?.lowercase() ?: ""
                val weekRule = when {
                    rawRule.contains("odd") || rawRule.contains("单") -> com.example.data.model.WeekRule.ODD
                    rawRule.contains("even") || rawRule.contains("双") -> com.example.data.model.WeekRule.EVEN
                    else -> com.example.data.model.WeekRule.ALL
                }

                val (sTime, eTime) = defaultTimesForPeriods(startP, endP)
                val color = listOf("#2563EB", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6", "#EF4444")[list.size % 6]

                list.add(
                    ImportedCourse(
                        name = name,
                        code = code,
                        classroom = classroom,
                        instructor = instructor,
                        dayOfWeek = day.coerceIn(1, 7),
                        startPeriod = startP,
                        endPeriod = endP,
                        startTime = sTime,
                        endTime = eTime,
                        weekRule = weekRule,
                        colorHex = color
                    )
                )
            }
        }
        return list
    }

    /**
     * Advanced Smart Text Parser for copy-pasted text blocks from university portals (e.g. Chinese & International).
     * Extracts Course Name, Day, Periods (P1-2 / 第1-2节), Room, Instructor, and Week Rules.
     */
    fun parseFreeTextSchedule(text: String): List<ImportedCourse> {
        return FreeTextScheduleParser.parseText(text)
    }

    private fun parseDayOfWeek(text: String): Int? {
        val trimmed = text.trim()
        val intVal = trimmed.toIntOrNull()
        if (intVal != null && intVal in 1..7) return intVal

        val lower = trimmed.lowercase()
        return when {
            lower.contains("mon") || lower.contains("monday") || lower.contains("周一") || lower.contains("星期一") || lower.contains("一") -> 1
            lower.contains("tue") || lower.contains("tuesday") || lower.contains("周二") || lower.contains("星期二") || lower.contains("二") -> 2
            lower.contains("wed") || lower.contains("wednesday") || lower.contains("周三") || lower.contains("星期三") || lower.contains("三") -> 3
            lower.contains("thu") || lower.contains("thursday") || lower.contains("周四") || lower.contains("星期四") || lower.contains("四") -> 4
            lower.contains("fri") || lower.contains("friday") || lower.contains("周五") || lower.contains("星期五") || lower.contains("五") -> 5
            lower.contains("sat") || lower.contains("saturday") || lower.contains("周六") || lower.contains("星期六") || lower.contains("六") -> 6
            lower.contains("sun") || lower.contains("sunday") || lower.contains("周日") || lower.contains("星期日") || lower.contains("日") -> 7
            else -> null
        }
    }

    private fun parseDayFromIcsDtStart(dtstart: String): Int? {
        if (dtstart.length >= 8) {
            val year = dtstart.substring(0, 4).toIntOrNull() ?: 2026
            val month = dtstart.substring(4, 6).toIntOrNull() ?: 10
            val day = dtstart.substring(6, 8).toIntOrNull() ?: 5
            val cal = java.util.Calendar.getInstance()
            cal.set(year, month - 1, day)
            val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
            return if (dow == java.util.Calendar.SUNDAY) 7 else dow - 1
        }
        return null
    }

    fun defaultTimesForPeriods(startP: Int, endP: Int): Pair<String, String> {
        val s = when (startP) {
            1 -> "08:00"; 2 -> "08:50"; 3 -> "09:50"; 4 -> "10:40"; 5 -> "11:30"; 6 -> "14:30"; 7 -> "15:20"; 8 -> "16:20"; 9 -> "17:10"; 10 -> "19:00"; 11 -> "19:50"; else -> "20:40"
        }
        val e = when (endP) {
            1 -> "08:45"; 2 -> "09:35"; 3 -> "10:35"; 4 -> "11:25"; 5 -> "12:15"; 6 -> "15:15"; 7 -> "16:05"; 8 -> "17:05"; 9 -> "17:55"; 10 -> "19:45"; 11 -> "20:35"; else -> "21:25"
        }
        return Pair(s, e)
    }

    fun getComputerSciencePreset(): List<ImportedCourse> {
        return listOf(
            ImportedCourse(
                name = "Desktop Application Design",
                code = "CS301",
                classroom = "Software Lab 1",
                instructor = "Prof. Smith",
                dayOfWeek = 1,
                startPeriod = 6,
                endPeriod = 7,
                startTime = "14:30",
                endTime = "16:05",
                colorHex = "#2563EB"
            ),
            ImportedCourse(
                name = "Desktop Application Design (Desktop App)",
                code = "1619310040",
                classroom = "Teaching Bldg Software Lab 1",
                instructor = "Prof. Computer Sci",
                dayOfWeek = 3,
                startPeriod = 1,
                endPeriod = 2,
                startTime = "08:00",
                endTime = "09:35",
                colorHex = "#2563EB"
            ),
            ImportedCourse(
                name = "Data Analysis & Machine Learning",
                code = "1619312040",
                classroom = "Liberal Arts Bldg B301",
                instructor = "Prof. Wang",
                dayOfWeek = 3,
                startPeriod = 8,
                endPeriod = 9,
                startTime = "16:20",
                endTime = "17:55",
                colorHex = "#10B981"
            ),
            ImportedCourse(
                name = "Computer Architecture",
                code = "1619304040",
                classroom = "Science Bldg B105",
                instructor = "Prof. Zhang",
                dayOfWeek = 1,
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                colorHex = "#F59E0B"
            ),
            ImportedCourse(
                name = "Database Systems & Application",
                code = "1619307040",
                classroom = "Teaching Bldg Software Lab 1",
                instructor = "Prof. Chen",
                dayOfWeek = 5,
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                colorHex = "#EC4899"
            ),
            ImportedCourse(
                name = "Neural Networks & Deep Learning",
                code = "1619311040",
                classroom = "Liberal Arts Bldg A304",
                instructor = "Dr. Liu",
                dayOfWeek = 5,
                startPeriod = 8,
                endPeriod = 9,
                startTime = "16:20",
                endTime = "17:55",
                colorHex = "#8B5CF6"
            )
        )
    }

    fun getSoftwareEngineeringPreset(): List<ImportedCourse> {
        return listOf(
            ImportedCourse(
                name = "Software Architecture & Design",
                code = "SE201",
                classroom = "Mingli Hall 302",
                instructor = "Dr. Adams",
                dayOfWeek = 2,
                startPeriod = 1,
                endPeriod = 2,
                startTime = "08:00",
                endTime = "09:35",
                colorHex = "#2563EB"
            ),
            ImportedCourse(
                name = "Mobile App Development (Android/Compose)",
                code = "SE202",
                classroom = "Software Lab 3",
                instructor = "Prof. Martinez",
                dayOfWeek = 2,
                startPeriod = 6,
                endPeriod = 7,
                startTime = "14:30",
                endTime = "16:05",
                colorHex = "#10B981"
            ),
            ImportedCourse(
                name = "Agile Project Management",
                code = "SE203",
                classroom = "Liberal Arts A201",
                instructor = "Prof. Taylor",
                dayOfWeek = 4,
                startPeriod = 3,
                endPeriod = 5,
                startTime = "09:50",
                endTime = "12:15",
                colorHex = "#8B5CF6"
            )
        )
    }
}
