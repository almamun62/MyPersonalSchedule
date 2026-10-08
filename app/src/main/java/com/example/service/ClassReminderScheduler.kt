package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.data.model.Course
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * High-precision exact alarm scheduler.
 * Schedules:
 * 1. Class reminders (15 min advance alerts)
 * 2. Generic task/exam reminders stored in Room
 * 3. DND automation alarms
 * 4. Bedtime sleep review alarms
 */
object ClassReminderScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun getExactAlarmSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    suspend fun rescheduleAllAlarms(context: Context) {
        val prefs = UserPreferencesManager.getInstance(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Reschedule DND alarms
        try {
            DndAutomationScheduler.scheduleAllDndAlarms(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Reschedule Class reminders
        if (prefs.isClassReminder15mEnabled.value) {
            scheduleClassReminders(context, alarmManager, prefs.classReminderMinutes.value)
        }

        // 3. Reschedule Generic Room reminders (tasks/exams)
        scheduleGenericReminders(context, alarmManager)

        // 4. Schedule Nightly Sleep Summary
        scheduleSleepAlarm(context, alarmManager)
    }

    private suspend fun scheduleClassReminders(
        context: Context,
        alarmManager: AlarmManager,
        minutesBefore: Int
    ) {
        val db = AppDatabase.getInstance(context)
        val courses = db.courseDao().getAllCourses().first()
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        for (dayOffset in 0..6) {
            val targetDate = today.plusDays(dayOffset.toLong())
            val targetDayOfWeek = targetDate.dayOfWeek.value

            val daysCourses = courses.filter { it.dayOfWeek == targetDayOfWeek }
            for (course in daysCourses) {
                val startTime = parseLocalTime(course.startTime, 8, 0)
                val classDateTime = LocalDateTime.of(targetDate, startTime)
                val reminderDateTime = classDateTime.minusMinutes(minutesBefore.toLong())

                if (reminderDateTime.isAfter(now)) {
                    val triggerMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    val intent = Intent(context, ClassReminderReceiver::class.java).apply {
                        putExtra(ClassReminderReceiver.EXTRA_COURSE_NAME, course.name)
                        putExtra(ClassReminderReceiver.EXTRA_CLASSROOM, course.classroom.ifEmpty { course.location })
                        putExtra(ClassReminderReceiver.EXTRA_START_TIME, course.startTime)
                        putExtra(ClassReminderReceiver.EXTRA_COURSE_ID, course.id)
                        putExtra(ClassReminderReceiver.EXTRA_MINUTES_BEFORE, minutesBefore)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        (course.id.toInt() * 100 + dayOffset * 10),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setExactAlarm(alarmManager, triggerMillis, pendingIntent)
                }
            }
        }
    }

    private suspend fun scheduleGenericReminders(
        context: Context,
        alarmManager: AlarmManager
    ) {
        val db = AppDatabase.getInstance(context)
        val nowMillis = System.currentTimeMillis()
        val upcomingReminders = db.reminderDao().getUpcomingReminders(nowMillis).first()

        for (reminder in upcomingReminders) {
            if (reminder.triggerTimeMillis > nowMillis) {
                val intent = Intent(context, ClassReminderReceiver::class.java).apply {
                    putExtra(ClassReminderReceiver.EXTRA_COURSE_NAME, reminder.title)
                    putExtra(ClassReminderReceiver.EXTRA_CLASSROOM, reminder.description)
                    putExtra(ClassReminderReceiver.EXTRA_COURSE_ID, reminder.id + 100000L)
                    putExtra(ClassReminderReceiver.EXTRA_MINUTES_BEFORE, 0)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (reminder.id.toInt() + 200000),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                setExactAlarm(alarmManager, reminder.triggerTimeMillis, pendingIntent)
            }
        }
    }

    private fun scheduleSleepAlarm(context: Context, alarmManager: AlarmManager) {
        val now = LocalDateTime.now()
        var sleepTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(22, 30))
        if (now.isAfter(sleepTime)) {
            sleepTime = sleepTime.plusDays(1)
        }

        val triggerMillis = sleepTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, SleepAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, triggerMillis, pendingIntent)
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseLocalTime(timeStr: String, defaultHour: Int, defaultMin: Int): LocalTime {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val h = parts[0].trim().toInt().coerceIn(0, 23)
                val m = parts[1].trim().take(2).toInt().coerceIn(0, 59)
                LocalTime.of(h, m)
            } else {
                LocalTime.of(defaultHour, defaultMin)
            }
        } catch (e: Exception) {
            LocalTime.of(defaultHour, defaultMin)
        }
    }
}
