package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.example.data.model.CourseEntity
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CalendarExportHelper {

    fun exportCourseToCalendar(context: Context, course: CourseEntity) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, course.name)
            putExtra(CalendarContract.Events.EVENT_LOCATION, course.classroom.ifBlank { "Campus" })
            putExtra(CalendarContract.Events.DESCRIPTION, "Instructor: ${course.instructor}\nNotes: ${course.notes}")
            
            val now = LocalDate.now()
            var targetDate = now
            val targetDayOfWeek = DayOfWeek.of(course.dayOfWeek.coerceIn(1, 7))
            while (targetDate.dayOfWeek != targetDayOfWeek) {
                targetDate = targetDate.plusDays(1)
            }

            try {
                val startTime = LocalTime.parse(course.startTime)
                val endTime = LocalTime.parse(course.endTime)
                val startDateTime = targetDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endDateTime = targetDate.atTime(endTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDateTime)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDateTime)
                putExtra(CalendarContract.Events.RRULE, "FREQ=WEEKLY;COUNT=16")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportWeeklyScheduleToIcs(context: Context, courses: List<CourseEntity>) {
        val icsBuilder = StringBuilder().apply {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//AI Studio//Course Schedule//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")

            val now = LocalDate.now()
            val dtStamp = java.time.ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))

            for (course in courses) {
                val targetDayOfWeek = DayOfWeek.of(course.dayOfWeek.coerceIn(1, 7))
                var targetDate = now
                while (targetDate.dayOfWeek != targetDayOfWeek) {
                    targetDate = targetDate.plusDays(1)
                }

                try {
                    val startTime = LocalTime.parse(course.startTime)
                    val endTime = LocalTime.parse(course.endTime)
                    val startDt = targetDate.atTime(startTime)
                    val endDt = targetDate.atTime(endTime)

                    val fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                    val startStr = startDt.format(fmt)
                    val endStr = endDt.format(fmt)

                    appendLine("BEGIN:VEVENT")
                    appendLine("UID:${java.util.UUID.randomUUID()}@courschedule.app")
                    appendLine("DTSTAMP:$dtStamp")
                    appendLine("DTSTART:$startStr")
                    appendLine("DTEND:$endStr")
                    appendLine("SUMMARY:${course.name}")
                    appendLine("LOCATION:${course.classroom}")
                    appendLine("DESCRIPTION:Instructor: ${course.instructor} - Notes: ${course.notes}")
                    appendLine("RRULE:FREQ=WEEKLY;COUNT=16")
                    appendLine("END:VEVENT")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            appendLine("END:VCALENDAR")
        }

        try {
            val file = File(context.cacheDir, "semester_schedule.ics")
            file.writeText(icsBuilder.toString())

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Semester Schedule (.ics)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Export Calendar (.ics)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
