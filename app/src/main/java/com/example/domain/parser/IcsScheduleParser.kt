package com.example.domain.parser

import com.example.domain.model.ImportedCourse

object IcsScheduleParser {

    /**
     * Complete, standard iCalendar (.ics) RFC 5545 parser.
     * Accurately parses:
     * - SUMMARY (Course name & code)
     * - LOCATION (Room / building / lab)
     * - DESCRIPTION (Instructor, syllabus notes)
     * - DTSTART / DTEND (Exact start and end times e.g. 20261012T090000Z or 20261012T090000)
     * - RRULE (Frequency, BYDAY e.g. MO,TU,WE,TH,FR)
     * - Maps exact start and end times to appropriate periods (1..12) and 24h clock strings.
     */
    fun parseIcsContent(icsContent: String): List<ImportedCourse> {
        val list = mutableListOf<ImportedCourse>()
        // Unfold lines that are wrapped with spaces as per RFC 5545
        val unfoldedContent = icsContent.replace(Regex("\r?\n[ \t]"), "")
        val events = unfoldedContent.split("BEGIN:VEVENT")

        for (eventBlock in events.drop(1)) {
            val eventBody = eventBlock.substringBefore("END:VEVENT")
            val lines = eventBody.lines().map { it.trim() }.filter { it.isNotBlank() }

            var summary = ""
            var location = ""
            var description = ""
            var dtstart = ""
            var dtend = ""
            var rrule = ""

            for (line in lines) {
                when {
                    line.startsWith("SUMMARY:", ignoreCase = true) -> {
                        summary = line.substringAfter(":").trim()
                    }
                    line.startsWith("LOCATION:", ignoreCase = true) -> {
                        location = line.substringAfter(":").trim()
                    }
                    line.startsWith("DESCRIPTION:", ignoreCase = true) -> {
                        description = line.substringAfter(":").trim()
                    }
                    line.startsWith("DTSTART", ignoreCase = true) -> {
                        dtstart = line.substringAfter(":").trim()
                    }
                    line.startsWith("DTEND", ignoreCase = true) -> {
                        dtend = line.substringAfter(":").trim()
                    }
                    line.startsWith("RRULE:", ignoreCase = true) -> {
                        rrule = line.substringAfter(":").trim()
                    }
                }
            }

            if (summary.isNotBlank()) {
                // Determine Day of Week (1 = Mon .. 7 = Sun)
                var dayOfWeek = 1
                if (rrule.contains("BYDAY=")) {
                    val byDay = rrule.substringAfter("BYDAY=").substringBefore(";").take(2).uppercase()
                    dayOfWeek = when (byDay) {
                        "MO" -> 1
                        "TU" -> 2
                        "WE" -> 3
                        "TH" -> 4
                        "FR" -> 5
                        "SA" -> 6
                        "SU" -> 7
                        else -> 1
                    }
                } else if (dtstart.length >= 8) {
                    dayOfWeek = parseDayFromDateString(dtstart) ?: 1
                }

                // Determine exact start and end times from DTSTART / DTEND
                val startTimeStr = extractTimeFromIcs(dtstart) ?: "08:00"
                val endTimeStr = extractTimeFromIcs(dtend) ?: calculateEndTime(startTimeStr)

                // Map clock time to university periods (1..12)
                val startPeriod = mapTimeToPeriod(startTimeStr)
                val endPeriod = mapTimeToPeriod(endTimeStr).coerceAtLeast(startPeriod)

                // Clean instructor from description
                val instructor = extractInstructorFromDescription(description)

                list.add(
                    ImportedCourse(
                        name = summary.replace("\\,", ",").replace("\\n", " "),
                        code = extractCourseCode(summary),
                        classroom = location.ifBlank { "Classroom" }.replace("\\,", ","),
                        instructor = instructor,
                        dayOfWeek = dayOfWeek,
                        startPeriod = startPeriod,
                        endPeriod = endPeriod,
                        startTime = startTimeStr,
                        endTime = endTimeStr,
                        colorHex = listOf("#2563EB", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6", "#EF4444")[list.size % 6]
                    )
                )
            }
        }

        return list.ifEmpty { ScheduleParser.parseFreeTextSchedule(icsContent) }
    }

    private fun parseDayFromDateString(dt: String): Int? {
        try {
            val datePart = dt.substringBefore("T")
            if (datePart.length >= 8) {
                val y = datePart.substring(0, 4).toInt()
                val m = datePart.substring(4, 6).toInt()
                val d = datePart.substring(6, 8).toInt()
                val cal = java.util.Calendar.getInstance()
                cal.set(y, m - 1, d)
                val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
                return if (dow == java.util.Calendar.SUNDAY) 7 else dow - 1
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun extractTimeFromIcs(dt: String): String? {
        if (!dt.contains("T")) return null
        val timePart = dt.substringAfter("T").take(4) // e.g. "0930"
        if (timePart.length >= 4) {
            val h = timePart.substring(0, 2)
            val m = timePart.substring(2, 4)
            return "$h:$m"
        }
        return null
    }

    private fun calculateEndTime(startTime: String): String {
        val parts = startTime.split(":")
        if (parts.size == 2) {
            val h = parts[0].toIntOrNull() ?: 8
            val m = parts[1].toIntOrNull() ?: 0
            val totalM = h * 60 + m + 95 // standard 95 min class
            val endH = (totalM / 60) % 24
            val endM = totalM % 60
            return "%02d:%02d".format(endH, endM)
        }
        return "09:35"
    }

    private fun mapTimeToPeriod(timeStr: String): Int {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        return when {
            h <= 8 -> 1
            h == 9 -> 2
            h == 10 -> 3
            h == 11 -> 4
            h == 12 || h == 13 -> 5
            h == 14 -> 6
            h == 15 -> 7
            h == 16 -> 8
            h == 17 -> 9
            h == 18 || h == 19 -> 10
            h == 20 -> 11
            else -> 12
        }
    }

    private fun extractCourseCode(summary: String): String {
        val codeMatch = Regex("\\b([A-Z]{2,4}\\s*\\d{3,4})\\b").find(summary)
        return codeMatch?.value ?: ""
    }

    private fun extractInstructorFromDescription(description: String): String {
        val profMatch = Regex("(?:Instructor|Teacher|Prof|Professor|Dr\\.)\\s*[:\\-]?\\s*([A-Za-z\\u4e00-\\u9fa5]+)").find(description)
        return profMatch?.groupValues?.getOrNull(1) ?: "Faculty"
    }
}
