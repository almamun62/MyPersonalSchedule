package com.example.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.data.model.Course
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object DndAutomationScheduler {

    suspend fun scheduleAllDndAlarms(context: Context) {
        val prefs = UserPreferencesManager.getInstance(context)
        if (!prefs.isAutoDndEnabled.value) {
            cancelAllAlarms(context)
            return
        }

        val db = AppDatabase.getInstance(context)
        val courses = db.courseDao().getAllCourses().first()
        val timings = prefs.sectionTimings.value

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val now = LocalDateTime.now()
        val today = LocalDate.now()

        // Schedule DND start and end alarms for courses over the next 7 days
        for (dayOffset in 0..6) {
            val targetDate = today.plusDays(dayOffset.toLong())
            val targetDayOfWeek = targetDate.dayOfWeek.value // 1 = Mon, 7 = Sun

            val daysCourses = courses.filter { it.dayOfWeek == targetDayOfWeek && it.dndEnabled }

            for (course in daysCourses) {
                val startLocalTime = parseLocalTime(course.startTime, defaultHour = 8, defaultMin = 0)
                val endLocalTime = parseLocalTime(course.endTime, defaultHour = 9, defaultMin = 35)

                val startDateTime = LocalDateTime.of(targetDate, startLocalTime)
                val endDateTime = LocalDateTime.of(targetDate, endLocalTime)

                val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val courseId = course.id.toInt()

                if (startDateTime.isAfter(now)) {
                    val startIntent = Intent(context, DndAutomationReceiver::class.java).apply {
                        action = DndAutomationReceiver.ACTION_ENABLE_CLASS_DND
                        putExtra(DndAutomationReceiver.EXTRA_COURSE_NAME, course.name)
                        putExtra(DndAutomationReceiver.EXTRA_CLASSROOM, course.classroom.ifEmpty { course.location })
                    }
                    val startPendingIntent = PendingIntent.getBroadcast(
                        context,
                        (courseId * 1000 + dayOffset * 10 + 1),
                        startIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setAlarm(alarmManager, startMillis, startPendingIntent)
                }

                if (endDateTime.isAfter(now)) {
                    val endIntent = Intent(context, DndAutomationReceiver::class.java).apply {
                        action = DndAutomationReceiver.ACTION_DISABLE_CLASS_DND
                        putExtra(DndAutomationReceiver.EXTRA_COURSE_NAME, course.name)
                        putExtra(DndAutomationReceiver.EXTRA_CLASSROOM, course.classroom.ifEmpty { course.location })
                    }
                    val endPendingIntent = PendingIntent.getBroadcast(
                        context,
                        (courseId * 1000 + dayOffset * 10 + 2),
                        endIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setAlarm(alarmManager, endMillis, endPendingIntent)
                }
            }
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAllAlarms(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            // If DND is disabled, restore normal interruption filter if access is granted
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerImmediateTestDnd(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Toast.makeText(context, "Please grant Notification Policy Access permission first!", Toast.LENGTH_LONG).show()
                return
            }

            try {
                // Turn DND ON
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                Toast.makeText(context, "🤫 Do Not Disturb Mode Activated! Will auto-restore in 10 seconds.", Toast.LENGTH_SHORT).show()

                // Schedule DND OFF in 10 seconds
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val disableIntent = Intent(context, DndAutomationReceiver::class.java).apply {
                    action = DndAutomationReceiver.ACTION_DISABLE_CLASS_DND
                    putExtra(DndAutomationReceiver.EXTRA_COURSE_NAME, "Test Class Session")
                    putExtra(DndAutomationReceiver.EXTRA_CLASSROOM, "Demo Room")
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    9999,
                    disableIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val restoreTime = System.currentTimeMillis() + 10000L
                setAlarm(alarmManager!!, restoreTime, pendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error toggling DND: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Do Not Disturb API requires Android 6.0+", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseLocalTime(timeStr: String, defaultHour: Int, defaultMin: Int): LocalTime {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val h = parts[0].trim().toInt()
                val m = parts[1].trim().toInt()
                LocalTime.of(h, m)
            } else {
                LocalTime.of(defaultHour, defaultMin)
            }
        } catch (e: Exception) {
            LocalTime.of(defaultHour, defaultMin)
        }
    }
}
