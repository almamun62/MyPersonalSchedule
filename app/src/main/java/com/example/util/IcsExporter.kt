package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.domain.model.Course
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object IcsExporter {

    fun generateIcs(courses: List<Course>, semesterName: String): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//WakeUp Timetable//Course Schedule//EN\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")
        sb.append("X-WR-CALNAME:").append(semesterName).append(" Timetable\r\n")
        sb.append("X-WR-TIMEZONE:Asia/Shanghai\r\n")

        val today = LocalDate.now()
        val mondayThisWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())

        courses.forEachIndexed { index, course ->
            val classDate = mondayThisWeek.plusDays((course.dayOfWeek - 1).toLong())
            val dateStr = classDate.format(DateTimeFormatter.BASIC_ISO_DATE) // YYYYMMDD
            val startClean = course.startTime.replace(":", "") + "00"
            val endClean = course.endTime.replace(":", "") + "00"

            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:course-").append(course.id).append("-").append(index).append("@timetable.app\r\n")
            sb.append("DTSTAMP:").append(dateStr).append("T080000Z\r\n")
            sb.append("DTSTART;TZID=Asia/Shanghai:").append(dateStr).append("T").append(startClean).append("\r\n")
            sb.append("DTEND;TZID=Asia/Shanghai:").append(dateStr).append("T").append(endClean).append("\r\n")
            sb.append("SUMMARY:").append(escapeIcs(course.name)).append("\r\n")
            if (course.classroom.isNotBlank()) {
                sb.append("LOCATION:").append(escapeIcs(course.classroom)).append("\r\n")
            }
            val desc = buildString {
                if (course.instructor.isNotBlank()) append("Instructor: ").append(course.instructor).append("\\n")
                if (course.code.isNotBlank()) append("Code: ").append(course.code).append("\\n")
                if (course.notes.isNotBlank()) append("Notes: ").append(course.notes)
            }
            if (desc.isNotBlank()) {
                sb.append("DESCRIPTION:").append(escapeIcs(desc)).append("\r\n")
            }
            sb.append("RRULE:FREQ=WEEKLY;COUNT=18\r\n")
            sb.append("STATUS:CONFIRMED\r\n")
            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    private fun escapeIcs(value: String): String {
        return value.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }

    fun shareIcs(context: Context, courses: List<Course>, semesterName: String) {
        try {
            val icsContent = generateIcs(courses, semesterName)
            val cacheDir = File(context.cacheDir, "calendar")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val icsFile = File(cacheDir, "schedule_${System.currentTimeMillis()}.ics")
            val writer = FileWriter(icsFile)
            writer.write(icsContent)
            writer.close()

            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = try {
                FileProvider.getUriForFile(context, authority, icsFile)
            } catch (e: Exception) {
                Uri.fromFile(icsFile)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "$semesterName Timetable")
                putExtra(Intent.EXTRA_TEXT, "Here is my semester course schedule (.ics)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Calendar (.ics)"))
        } catch (e: Exception) {
            // Fallback to text share
            shareScheduleText(context, courses, semesterName)
        }
    }

    fun shareScheduleText(context: Context, courses: List<Course>, semesterName: String) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val sb = StringBuilder()
        sb.append("📅 ").append(semesterName).append(" Course Timetable\n\n")

        for (dayIdx in 1..7) {
            val dayCourses = courses.filter { it.dayOfWeek == dayIdx }.sortedBy { it.startTime }
            if (dayCourses.isNotEmpty()) {
                sb.append("【").append(days[dayIdx - 1]).append("】\n")
                dayCourses.forEach { c ->
                    sb.append("• ").append(c.startTime).append("-").append(c.endTime).append(" ")
                    sb.append(c.name)
                    if (c.classroom.isNotBlank()) sb.append(" @ ").append(c.classroom)
                    if (c.instructor.isNotBlank()) sb.append(" (").append(c.instructor).append(")")
                    sb.append("\n")
                }
                sb.append("\n")
            }
        }

        val text = sb.toString().trim()
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Timetable"))
    }

    fun generateShareableCode(courses: List<Course>): String {
        val sb = StringBuilder()
        sb.append("Course Name,Code,Classroom,Instructor,DayOfWeek,StartTime,EndTime,Color\n")
        courses.forEach { c ->
            sb.append("${c.name},${c.code},${c.classroom},${c.instructor},${c.dayOfWeek},${c.startTime},${c.endTime},${c.colorHex}\n")
        }
        return sb.toString()
    }

    fun copyShareCode(context: Context, courses: List<Course>, semesterName: String) {
        val code = generateShareableCode(courses)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Timetable Share Code", code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Timetable code copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
