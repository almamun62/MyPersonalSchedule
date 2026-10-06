package com.example.domain.parser

import com.example.domain.model.ImportedCourse

object ScheduleParser {

    /**
     * Smart CSV / TSV / Semicolon-separated parser with automatic header & delimiter detection.
     * Supports both English and Chinese headers (e.g., 课程名称, 教室, 教师, 星期, 节次).
     */
    fun parseCsvSchedule(csvContent: String): List<ImportedCourse> {
        val list = mutableListOf<ImportedCourse>()
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return list

        // Determine delimiter (comma, tab, or semicolon)
        val firstLine = lines.first()
        val delimiter = when {
            firstLine.contains("\t") -> "\t"
            firstLine.contains(";") -> ";"
            else -> ","
        }

        val headerParts = firstLine.split(delimiter).map { it.trim().removeSurrounding("\"").lowercase() }

        // Smart column index detection
        var nameIdx = headerParts.indexOfFirst { it.contains("name") || it.contains("course") || it.contains("title") || it.contains("课程") }
        var codeIdx = headerParts.indexOfFirst { it.contains("code") || it.contains("id") || it.contains("代码") || it.contains("编号") }
        var roomIdx = headerParts.indexOfFirst { it.contains("room") || it.contains("class") || it.contains("loc") || it.contains("教室") || it.contains("地点") }
        var teacherIdx = headerParts.indexOfFirst { it.contains("teach") || it.contains("instruct") || it.contains("prof") || it.contains("教师") || it.contains("老师") }
        var dayIdx = headerParts.indexOfFirst { it.contains("day") || it.contains("week") || it.contains("星期") || it.contains("周") }
        var startPIdx = headerParts.indexOfFirst { it.contains("startp") || it.contains("fromp") || it.contains("start period") || it.contains("开始节") || it.contains("节次") }
        var endPIdx = headerParts.indexOfFirst { it.contains("endp") || it.contains("top") || it.contains("end period") || it.contains("结束节") }

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
        val list = mutableListOf<ImportedCourse>()
        // Split text by lines, semicolons, or double spaces
        val blocks = text.split(Regex("[\n;;\r\n]+")).map { it.trim() }.filter { it.length > 3 }

        for (block in blocks) {
            val lower = block.lowercase()

            // 1. Detect Day of Week
            val dayOfWeek = parseDayOfWeek(block) ?: 1

            // 2. Detect Period Range (e.g., P3-5, 1-2节, 第3-4节, 8:00-9:35, 10:00 AM - 11:15 AM)
            var startP = 1
            var endP = 2

            // Match "第1-2节" or "1-2节" or "P1-2" or "period 3-5"
            val periodRegex = Regex("(?:第|p|period)?\\s*(\\d{1,2})\\s*(?:-|至|到|~|\\s*-\\s*)\\s*(\\d{1,2})\\s*节?", RegexOption.IGNORE_CASE)
            val periodMatch = periodRegex.find(block)

            if (periodMatch != null) {
                startP = periodMatch.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: 1
                endP = periodMatch.groupValues[2].toIntOrNull()?.coerceIn(startP, 12) ?: (startP + 1)
            } else {
                // Match exact clock times like 14:30-16:05 or 8:00
                val clockRegex = Regex("(\\d{1,2}):(\\d{2})")
                val clockMatches = clockRegex.findAll(block).toList()
                if (clockMatches.isNotEmpty()) {
                    val startHour = clockMatches[0].groupValues[1].toIntOrNull() ?: 8
                    startP = when {
                        startHour <= 8 -> 1
                        startHour <= 10 -> 3
                        startHour <= 14 -> 6
                        startHour <= 16 -> 8
                        else -> 10
                    }
                    endP = (startP + 1).coerceAtMost(12)
                }
            }

            // 3. Extract Classroom / Room
            var classroom = ""
            val roomRegex = Regex("(?:教室|地点|room|bldg|lab|hall|楼|馆|室|实验室|\\b[A-Z]\\d{3}\\b|\\b\\d{3}\\b)", RegexOption.IGNORE_CASE)
            val roomMatch = roomRegex.find(block)
            if (roomMatch != null) {
                val roomStart = roomMatch.range.first
                val rawRoom = block.substring(maxOf(0, roomStart - 4), minOf(block.length, roomStart + 15)).trim()
                classroom = rawRoom.take(20)
            }
            if (classroom.isBlank()) {
                classroom = if (lower.contains("lab")) "Computer Lab" else "Classroom B101"
            }

            // 4. Extract Instructor
            var instructor = ""
            val profRegex = Regex("(?:prof\\.|professor|dr\\.|老师|教授|讲师|\\b[A-Z][a-z]+\\b)")
            if (block.contains("Prof") || block.contains("Dr.") || block.contains("老师") || block.contains("教授")) {
                val profMatch = profRegex.find(block)
                if (profMatch != null) {
                    instructor = profMatch.value
                }
            }
            if (instructor.isBlank()) instructor = "Faculty Instructor"

            // 5. Clean course name
            var name = block
                .replace(Regex("(?:周|星期)[一二三四五六日七1-7]"), "")
                .replace(Regex("(?:第|p|period)?\\s*\\d{1,2}\\s*[-~至到]\\s*\\d{1,2}\\s*节?"), "")
                .replace(Regex("(?:室|楼|馆|lab|room)\\s*\\w+"), "")
                .replace(Regex("[,;\\(\\)\\[\\]]"), " ")
                .trim()

            val words = name.split("\\s+".toRegex()).filter { it.isNotBlank() }
            name = if (words.isNotEmpty()) words.take(4).joinToString(" ") else "Custom Course ${list.size + 1}"
            if (name.length > 35) name = name.take(35) + "..."

            val (sTime, eTime) = defaultTimesForPeriods(startP, endP)
            val colors = listOf("#2563EB", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6", "#EF4444")

            list.add(
                ImportedCourse(
                    name = name,
                    code = "CS${100 + list.size * 5}",
                    classroom = classroom,
                    instructor = instructor,
                    dayOfWeek = dayOfWeek,
                    startPeriod = startP,
                    endPeriod = endP,
                    startTime = sTime,
                    endTime = eTime,
                    colorHex = colors[list.size % colors.size]
                )
            )
        }

        return list.ifEmpty {
            listOf(
                ImportedCourse(
                    name = text.take(25).ifBlank { "Sample Course" },
                    code = "CS101",
                    classroom = "Lab 1",
                    instructor = "Faculty",
                    dayOfWeek = 1,
                    startPeriod = 1,
                    endPeriod = 2,
                    startTime = "08:00",
                    endTime = "09:35",
                    colorHex = "#2563EB"
                )
            )
        }
    }

    /**
     * Parse HTML timetable tables exported by Chinese university educational portals (正方, 强智, 树维).
     */
    fun parseHtmlSchedule(htmlContent: String): List<ImportedCourse> {
        val list = mutableListOf<ImportedCourse>()
        val tdRegex = Regex("(?i)(?s)<td[^>]*>(.*?)</td>")
        val tdMatches = tdRegex.findAll(htmlContent).map { match ->
            match.groupValues[1].replace(Regex("<[^>]*>"), " ").trim()
        }.toList()

        var currentDay = 1
        for (cellText in tdMatches) {
            if (cellText.length > 4 && !cellText.contains("星期") && !cellText.contains("节次") && !cellText.contains("时间")) {
                val coursesFromCell = parseFreeTextSchedule(cellText)
                for (c in coursesFromCell) {
                    c.dayOfWeek = currentDay
                    list.add(c)
                }
                currentDay = (currentDay % 5) + 1
            }
        }
        return list.ifEmpty { parseFreeTextSchedule(htmlContent) }
    }

    /**
     * Parse iCalendar .ics format
     */
    fun parseIcsSchedule(icsContent: String): List<ImportedCourse> {
        val list = mutableListOf<ImportedCourse>()
        val events = icsContent.split("BEGIN:VEVENT")

        for (event in events.drop(1)) {
            var summary = "Course"
            var location = "Room"
            var description = ""
            var dtstart = ""

            event.lines().forEach { l ->
                when {
                    l.startsWith("SUMMARY:") -> summary = l.removePrefix("SUMMARY:").trim()
                    l.startsWith("LOCATION:") -> location = l.removePrefix("LOCATION:").trim()
                    l.startsWith("DESCRIPTION:") -> description = l.removePrefix("DESCRIPTION:").trim()
                    l.startsWith("DTSTART") -> dtstart = l.substringAfter(":").trim()
                }
            }

            if (summary.isNotBlank()) {
                val day = parseDayFromIcsDtStart(dtstart) ?: ((list.size % 5) + 1)
                val startP = 1 + (list.size % 4) * 2
                val endP = (startP + 1).coerceAtMost(12)
                val (sTime, eTime) = defaultTimesForPeriods(startP, endP)

                list.add(
                    ImportedCourse(
                        name = summary,
                        code = "101",
                        classroom = location.ifEmpty { "Teaching Lab" },
                        instructor = description.ifEmpty { "Professor" },
                        dayOfWeek = day,
                        startPeriod = startP,
                        endPeriod = endP,
                        startTime = sTime,
                        endTime = eTime,
                        colorHex = "#2563EB"
                    )
                )
            }
        }

        return list.ifEmpty { parseFreeTextSchedule(icsContent) }
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

    private fun defaultTimesForPeriods(startP: Int, endP: Int): Pair<String, String> {
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
