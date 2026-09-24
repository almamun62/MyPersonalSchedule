package com.example.domain.parser

import com.example.domain.model.ImportedCourse
import java.util.Locale

object ScheduleParser {

    private val PALETTE = listOf(
        "#4F46E5", // Indigo
        "#0EA5E9", // Sky Blue
        "#10B981", // Emerald Green
        "#F59E0B", // Amber
        "#EC4899", // Rose Pink
        "#8B5CF6", // Violet
        "#06B6D4", // Cyan
        "#F97316", // Orange
        "#14B8A6"  // Teal
    )

    fun getColorForIndex(index: Int): String {
        return PALETTE[index % PALETTE.size]
    }

    /**
     * Parses structured CSV or TSV tabular text.
     */
    fun parseCsv(text: String): List<ImportedCourse> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        // Detect delimiter: comma, tab, semicolon, pipe
        val firstLine = lines.first()
        val delimiter = when {
            firstLine.contains('\t') -> "\t"
            firstLine.contains(';') -> ";"
            firstLine.contains('|') -> "|"
            else -> ","
        }

        // Split columns helper
        fun splitLine(line: String): List<String> {
            if (delimiter == ",") {
                // Handle quoted values
                val tokens = mutableListOf<String>()
                val sb = StringBuilder()
                var inQuotes = false
                for (ch in line) {
                    if (ch == '\"') {
                        inQuotes = !inQuotes
                    } else if (ch == ',' && !inQuotes) {
                        tokens.add(sb.toString().trim())
                        sb.clear()
                    } else {
                        sb.append(ch)
                    }
                }
                tokens.add(sb.toString().trim())
                return tokens
            } else {
                return line.split(delimiter).map { it.trim().removeSurrounding("\"") }
            }
        }

        val rows = lines.map { splitLine(it) }
        val header = rows.first().map { it.lowercase(Locale.ROOT) }

        // Find column indices
        var nameCol = -1
        var codeCol = -1
        var dayCol = -1
        var timeCol = -1
        var startCol = -1
        var endCol = -1
        var roomCol = -1
        var instructorCol = -1
        var creditsCol = -1

        for (i in header.indices) {
            val col = header[i]
            when {
                nameCol == -1 && (col.contains("name") || col.contains("title") || col.contains("subject") || col == "course") -> nameCol = i
                codeCol == -1 && (col.contains("code") || col.contains("number") || col == "id" || col == "cid") -> codeCol = i
                dayCol == -1 && (col.contains("day") || col.contains("weekday") || col.contains("days")) -> dayCol = i
                timeCol == -1 && (col == "time" || col == "times" || col.contains("slot") || col.contains("hours")) -> timeCol = i
                startCol == -1 && (col.contains("start") || col.contains("begin")) -> startCol = i
                endCol == -1 && (col.contains("end") || col.contains("finish")) -> endCol = i
                roomCol == -1 && (col.contains("room") || col.contains("class") || col.contains("location") || col.contains("hall") || col.contains("bldg")) -> roomCol = i
                instructorCol == -1 && (col.contains("instructor") || col.contains("prof") || col.contains("teacher") || col.contains("faculty")) -> instructorCol = i
                creditsCol == -1 && (col.contains("credit") || col.contains("unit")) -> creditsCol = i
            }
        }

        // If no explicit header detected, assume standard default column layout:
        // Code, Name, Day, StartTime, EndTime, Room, Instructor
        val isHeaderPresent = nameCol != -1 || codeCol != -1 || dayCol != -1
        val startIndex = if (isHeaderPresent) 1 else 0

        val parsedCourses = mutableListOf<ImportedCourse>()
        var colorIdx = 0

        for (rowIdx in startIndex until rows.size) {
            val row = rows[rowIdx]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            var code = ""
            var name = ""
            var dayStr = ""
            var startTime = "09:00"
            var endTime = "10:15"
            var classroom = ""
            var instructor = ""
            var credits = 3

            if (isHeaderPresent) {
                if (codeCol in row.indices) code = row[codeCol]
                if (nameCol in row.indices) name = row[nameCol]
                if (dayCol in row.indices) dayStr = row[dayCol]
                if (roomCol in row.indices) classroom = row[roomCol]
                if (instructorCol in row.indices) instructor = row[instructorCol]
                if (creditsCol in row.indices) credits = row[creditsCol].toIntOrNull() ?: 3

                if (timeCol in row.indices && row[timeCol].isNotBlank()) {
                    val (s, e) = parseTimeRange(row[timeCol])
                    startTime = s
                    endTime = e
                } else {
                    if (startCol in row.indices) startTime = normalizeTime(row[startCol])
                    if (endCol in row.indices) endTime = normalizeTime(row[endCol])
                }
            } else {
                // Fallback positional index
                code = row.getOrNull(0) ?: ""
                name = row.getOrNull(1) ?: code
                dayStr = row.getOrNull(2) ?: "Monday"
                if (row.size > 4) {
                    startTime = normalizeTime(row.getOrNull(3) ?: "09:00")
                    endTime = normalizeTime(row.getOrNull(4) ?: "10:15")
                    classroom = row.getOrNull(5) ?: ""
                    instructor = row.getOrNull(6) ?: ""
                } else if (row.size > 3) {
                    val (s, e) = parseTimeRange(row[3])
                    startTime = s
                    endTime = e
                }
            }

            if (name.isBlank() && code.isNotBlank()) {
                name = code
            } else if (code.isBlank() && name.isNotBlank()) {
                code = extractCodeFromName(name)
            }

            if (name.isBlank() && code.isBlank()) continue

            // A single row may have multiple days e.g. "MWF", "M, W, F", "Mon, Wed"
            val days = parseDays(dayStr)
            val assignedColor = getColorForIndex(colorIdx++)

            for (day in days) {
                val item = ImportedCourse(
                    name = name,
                    code = code,
                    instructor = instructor,
                    classroom = classroom,
                    dayOfWeek = day,
                    startTime = startTime,
                    endTime = endTime,
                    colorHex = assignedColor,
                    credits = credits,
                    isSelected = true
                )
                validateImportedCourse(item)
                parsedCourses.add(item)
            }
        }

        return parsedCourses
    }

    /**
     * Parses free-form text pasted from a student portal, email, or syllabus.
     */
    fun parseFreeText(text: String): List<ImportedCourse> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Check if Chinese timetable or matrix format or contains schedule keywords
        if (trimmed.contains("星期") || trimmed.contains("周") || trimmed.contains("节") || trimmed.contains("16193") ||
            trimmed.contains("明理楼") || trimmed.contains("明德楼") || trimmed.contains("机房")) {
            val entities = com.example.util.ScheduleImportHelper.parseTextToCourses(
                rawText = text,
                semesterId = 1L,
                autoTranslateToEnglish = true,
                bilingual = true
            )
            if (entities.isNotEmpty()) {
                val list = mutableListOf<ImportedCourse>()
                var colorIdx = 0
                for (entity in entities) {
                    val hex = getColorForIndex(colorIdx++)
                    val item = ImportedCourse(
                        name = entity.name,
                        code = entity.code,
                        instructor = entity.instructor,
                        classroom = entity.classroom,
                        dayOfWeek = entity.dayOfWeek.coerceIn(1, 7),
                        startTime = entity.startTime.ifBlank { "08:00" },
                        endTime = entity.endTime.ifBlank { "09:35" },
                        colorHex = hex,
                        credits = 3,
                        isSelected = true
                    )
                    validateImportedCourse(item)
                    list.add(item)
                }
                return list
            }
        }

        // First, check if it's CSV-like
        if (text.contains(",") || text.contains("\t") || text.contains(";")) {
            val csvResult = parseCsv(text)
            if (csvResult.isNotEmpty()) return csvResult
        }

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<ImportedCourse>()
        var colorIdx = 0

        // Regex patterns
        val codeRegex = Regex("""\b([A-Z]{2,4}\s*[-]?\s*\d{3,4}[A-Z]?)\b""", RegexOption.IGNORE_CASE)
        val timeRangeRegex = Regex("""(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)\s*(?:-|to|–)\s*(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)""", RegexOption.IGNORE_CASE)
        val roomRegex = Regex("""\b(?:Room|Rm\.?|Hall|Bldg|Building|Auditorium|Lab)\s*([A-Za-z0-9\-]+)\b""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val codeMatch = codeRegex.find(line)
            val timeMatch = timeRangeRegex.find(line)

            if (codeMatch != null || timeMatch != null) {
                val code = codeMatch?.value?.uppercase(Locale.ROOT)?.replace("\\s+".toRegex(), " ") ?: "CLASS"
                var name = line
                    .replace(codeMatch?.value ?: "", "")
                    .replace(timeMatch?.value ?: "", "")
                    .replace(roomRegex, "")
                    .trim(' ', ',', '-', ':', ';', '|')
                    .ifEmpty { code }

                if (name.length > 50) {
                    name = name.take(50).trim()
                }

                val (startTime, endTime) = if (timeMatch != null) {
                    val s = normalizeTime(timeMatch.groupValues[1])
                    val e = normalizeTime(timeMatch.groupValues[2])
                    Pair(s, e)
                } else {
                    Pair("10:00", "11:15")
                }

                val room = roomRegex.find(line)?.value ?: ""
                val days = parseDays(line)
                val color = getColorForIndex(colorIdx++)

                for (day in days) {
                    val item = ImportedCourse(
                        name = name,
                        code = code,
                        classroom = room,
                        dayOfWeek = day,
                        startTime = startTime,
                        endTime = endTime,
                        colorHex = color,
                        isSelected = true
                    )
                    validateImportedCourse(item)
                    result.add(item)
                }
            }
        }

        if (result.isEmpty()) {
            val fallbackEntities = com.example.util.ScheduleImportHelper.parseTextToCourses(
                rawText = text,
                semesterId = 1L,
                autoTranslateToEnglish = true,
                bilingual = true
            )
            for (entity in fallbackEntities) {
                val hex = getColorForIndex(colorIdx++)
                val item = ImportedCourse(
                    name = entity.name,
                    code = entity.code,
                    instructor = entity.instructor,
                    classroom = entity.classroom,
                    dayOfWeek = entity.dayOfWeek.coerceIn(1, 7),
                    startTime = entity.startTime.ifBlank { "08:00" },
                    endTime = entity.endTime.ifBlank { "09:35" },
                    colorHex = hex,
                    credits = 3,
                    isSelected = true
                )
                validateImportedCourse(item)
                result.add(item)
            }
        }

        return result
    }

    /**
     * Expands and parses a day representation string into a list of DayOfWeek integers (1=Mon..7=Sun).
     */
    fun parseDays(input: String): List<Int> {
        val days = mutableSetOf<Int>()
        val lower = input.lowercase(Locale.ROOT)

        // Multi-letter patterns: MWF, TR, TTH
        if (input.contains("MWF", ignoreCase = true)) {
            days.addAll(listOf(1, 3, 5))
        }
        if (input.contains("TTh", ignoreCase = true) || input.contains("TR", ignoreCase = true)) {
            days.addAll(listOf(2, 4))
        }
        if (input.contains("MW", ignoreCase = true) && !input.contains("MWF", ignoreCase = true)) {
            days.addAll(listOf(1, 3))
        }

        // Standard day names
        if (lower.contains("mon")) days.add(1)
        if (lower.contains("tue") || lower.contains("tues")) days.add(2)
        if (lower.contains("wed")) days.add(3)
        if (lower.contains("thu") || lower.contains("thurs")) days.add(4)
        if (lower.contains("fri")) days.add(5)
        if (lower.contains("sat")) days.add(6)
        if (lower.contains("sun")) days.add(7)

        // Number notations
        if (days.isEmpty()) {
            for (ch in input) {
                when (ch) {
                    '1' -> days.add(1)
                    '2' -> days.add(2)
                    '3' -> days.add(3)
                    '4' -> days.add(4)
                    '5' -> days.add(5)
                    '6' -> days.add(6)
                    '7' -> days.add(7)
                }
            }
        }

        // Default to Monday if nothing matched
        return if (days.isNotEmpty()) days.toList().sorted() else listOf(1)
    }

    /**
     * Parses combined time strings like "09:00 - 10:15", "9:00am-10:15am", "13:00 - 14:30".
     */
    fun parseTimeRange(timeStr: String): Pair<String, String> {
        val parts = timeStr.split(Regex("""[-–to]""")).map { it.trim() }
        if (parts.size >= 2) {
            return Pair(normalizeTime(parts[0]), normalizeTime(parts[1]))
        }
        return Pair("09:00", "10:15")
    }

    /**
     * Normalizes times from various formats ("9", "9:00", "9:00 AM", "1:30 pm") to "HH:mm" (24h).
     */
    fun normalizeTime(input: String): String {
        val trimmed = input.trim().lowercase(Locale.ROOT)
        val isPm = trimmed.contains("pm")
        val isAm = trimmed.contains("am")
        val clean = trimmed.replace("am", "").replace("pm", "").trim()

        val parts = clean.split(":")
        var h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

        if (isPm && h < 12) h += 12
        if (isAm && h == 12) h = 0

        return String.format(Locale.ROOT, "%02d:%02d", h.coerceIn(0, 23), m.coerceIn(0, 59))
    }

    private fun extractCodeFromName(name: String): String {
        val words = name.split(" ")
        return if (words.size >= 2 && words[0].length in 2..5 && words[1].any { it.isDigit() }) {
            "${words[0]} ${words[1]}"
        } else {
            name.take(6).uppercase(Locale.ROOT)
        }
    }

    private fun validateImportedCourse(item: ImportedCourse) {
        if (item.name.isBlank()) {
            item.isValid = false
            item.validationError = "Missing course name"
            return
        }
        if (item.startTime == item.endTime) {
            item.isValid = false
            item.validationError = "Start and end time cannot be identical"
            return
        }
        item.isValid = true
        item.validationError = null
    }

    fun getMamunFall2026ImportedCourses(bilingual: Boolean = true): List<ImportedCourse> {
        val entities = com.example.util.ScheduleImportHelper.getMamunFall2026Schedule(
            semesterId = 1L,
            autoTranslateToEnglish = true,
            bilingual = bilingual
        )
        return entities.mapIndexed { idx, entity ->
            val hex = getColorForIndex(idx)
            val item = ImportedCourse(
                name = entity.name,
                code = entity.code,
                instructor = entity.instructor,
                classroom = entity.classroom,
                dayOfWeek = entity.dayOfWeek.coerceIn(1, 7),
                startTime = entity.startTime.ifBlank { "08:00" },
                endTime = entity.endTime.ifBlank { "09:35" },
                colorHex = hex,
                credits = 3,
                isSelected = true
            )
            validateImportedCourse(item)
            item
        }
    }

    // Sample Presets for quick student imports
    val SAMPLE_CSV_TEMPLATE = """Course Code,Course Name,Days,Start Time,End Time,Room,Instructor,Credits
CS 101,Intro to Computer Science,MWF,09:00,10:15,Hall 101,Dr. Smith,3
MATH 201,Calculus II,TR,10:30,12:00,Math Bld 204,Prof. Gauss,4
ENG 105,College Writing,MWF,13:00,14:15,Lang Center 12,Dr. Woolf,3
PHYS 150,Physics for Engineers,TR,14:30,16:00,Science Lab A,Dr. Newton,4
HIST 110,World Civilization,Friday,10:30,12:00,Auditorium B,Prof. Toynbee,3"""

    val PRESET_SCHEDULES = mapOf(
        "Fall 2026 Schedule (Mamun) - Bilingual" to """Code,Name,Days,Start Time,End Time,Room,Instructor,Credits
1619304040,Computer Architecture 计算机组成原理,Monday,09:50,12:15,明理楼B105,计算机学院,3
1619304040,Computer Architecture 计算机组成原理,Wednesday,09:50,12:15,明理楼B105,计算机学院,3
1619304040,Computer Architecture 计算机组成原理,Monday,18:30,21:25,明理楼B105,计算机学院,3
1619310040,Desktop App Design 桌面应用程序设计,Monday,14:30,16:05,明理楼软件机房1,计算机学院,3
1619310040,Desktop App Design 桌面应用程序设计,Wednesday,08:00,09:35,明理楼软件机房1,计算机学院,3
1619313040,Data Science Prog 面向数据科学的编程语言,Monday,16:20,17:55,明理楼B402,数据科学学院,3
1619313040,Data Science Prog 面向数据科学的编程语言,Wednesday,14:30,16:05,明理楼B402,数据科学学院,3
1619307040,Database Systems 数据库原理及应用,Friday,09:50,12:15,明理楼软件机房1,计算机学院,3
1619307040,Database Systems 数据库原理及应用,Wednesday,09:50,12:15,明理楼软件机房1,计算机学院,3
1619311040,Neural Networks 神经网络与深度学习,Wednesday,09:50,11:25,明德楼A304,人工智能学院,3
1619311040,Neural Networks 神经网络与深度学习,Friday,16:20,17:55,明德楼A304,人工智能学院,3
1619312040,Machine Learning 数据分析与机器学习,Wednesday,16:20,17:55,明德楼B301,人工智能学院,3
1619312040,Machine Learning 数据分析与机器学习,Friday,08:00,09:35,明德楼B301,人工智能学院,3
1619312040,Machine Learning 数据分析与机器学习,Wednesday,18:30,20:05,明德楼B301,人工智能学院,3""",

        "Fall 2026 Schedule (Mamun) - English" to """Code,Name,Days,Start Time,End Time,Room,Instructor,Credits
1619304040,Computer Organization & Architecture,Monday,09:50,12:15,Mingli Bldg B105,School of CS,3
1619304040,Computer Organization & Architecture,Wednesday,09:50,12:15,Mingli Bldg B105,School of CS,3
1619304040,Computer Organization & Architecture,Monday,18:30,21:25,Mingli Bldg B105,School of CS,3
1619310040,Desktop Application Design,Monday,14:30,16:05,Software Lab 1,School of CS,3
1619310040,Desktop Application Design,Wednesday,08:00,09:35,Software Lab 1,School of CS,3
1619313040,Programming for Data Science,Monday,16:20,17:55,Mingli Bldg B402,Data Science,3
1619313040,Programming for Data Science,Wednesday,14:30,16:05,Mingli Bldg B402,Data Science,3
1619307040,Database Principles & Applications,Friday,09:50,12:15,Software Lab 1,School of CS,3
1619307040,Database Principles & Applications,Wednesday,09:50,12:15,Software Lab 1,School of CS,3
1619311040,Neural Networks & Deep Learning,Wednesday,09:50,11:25,Mingde Bldg A304,School of AI,3
1619311040,Neural Networks & Deep Learning,Friday,16:20,17:55,Mingde Bldg A304,School of AI,3
1619312040,Data Analysis & Machine Learning,Wednesday,16:20,17:55,Mingde Bldg B301,School of AI,3
1619312040,Data Analysis & Machine Learning,Friday,08:00,09:35,Mingde Bldg B301,School of AI,3
1619312040,Data Analysis & Machine Learning,Wednesday,18:30,20:05,Mingde Bldg B301,School of AI,3""",

        "Computer Science (Year 2)" to """Code,Name,Days,Start Time,End Time,Room,Instructor,Credits
CS 201,Data Structures & Algorithms,MWF,09:00,10:15,CS Hall 101,Dr. Turing,4
MATH 240,Linear Algebra,TR,11:00,12:15,Math 302,Prof. Johnson,3
CS 250,Computer Systems & Assembly,MWF,13:00,14:15,Turing Lab 4,Dr. Knuth,4
ENG 210,Technical Communications,TR,14:00,15:15,Humanities 201,Prof. Orwell,3
CS 280,Web & Mobile Systems,Wednesday,15:00,17:00,CS Lab 2B,Dr. Berners-Lee,3""",

        "Pre-Med / Biology" to """Code,Name,Days,Start Time,End Time,Room,Instructor,Credits
BIO 201,Cellular & Molecular Biology,MWF,08:30,09:45,Bio Hall 102,Dr. Franklin,4
CHEM 220,Organic Chemistry I,TR,10:00,11:15,Science 301,Dr. Pasteur,4
CHEM 220L,Organic Chemistry Lab,Thursday,13:00,16:00,Chem Lab B,Dr. Curie,2
PSYC 101,Introduction to Psychology,MWF,11:00,12:15,Auditorium A,Dr. Freud,3
STAT 200,Biostatistics,TR,14:30,15:45,Math 105,Prof. Fisher,3""",

        "Business Administration" to """Code,Name,Days,Start Time,End Time,Room,Instructor,Credits
ECON 101,Principles of Microeconomics,MWF,10:00,11:15,Biz Hall 200,Dr. Keynes,3
ACCT 210,Financial Accounting,TR,09:30,10:45,Commerce 104,Prof. Pacioli,3
MKTG 301,Marketing Strategy & Analytics,MWF,13:00,14:15,Innovation Lab,Dr. Kotler,3
MGMT 320,Organizational Behavior,TR,13:00,14:15,Biz Hall 110,Prof. Drucker,3
FIN 300,Corporate Finance,Tuesday,15:00,17:30,Commerce 205,Dr. Graham,3"""
    )
}
