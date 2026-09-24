package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.domain.ScheduleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SleepAlarmScheduler {
    private const val TAG = "SleepAlarmScheduler"
    private const val REQUEST_CODE_WAKE = 30010
    private const val REQUEST_CODE_BEDTIME = 30020

    suspend fun scheduleNextSleepAlarms(context: Context) = withContext(Dispatchers.IO) {
        val prefs = UserPreferencesManager.getInstance(context)
        val isEnabled = prefs.isSleepAlarmEnabled.value

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available")
            return@withContext
        }

        if (!isEnabled) {
            cancelSleepAlarms(context)
            Log.d(TAG, "Sleep alarm is disabled. Alarms cancelled.")
            return@withContext
        }

        val bedtimeStr = prefs.sleepBedtime.value
        val defaultWakeStr = prefs.sleepWakeTime.value
        val smartWake = prefs.isSleepSmartWakeEnabled.value
        val advanceMin = prefs.sleepWakeAdvanceMinutes.value
        val targetHours = prefs.sleepTargetHours.value
        val vibrate = prefs.isSleepAlarmVibrate.value

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        // Find tomorrow's first course if smart wake is enabled
        var targetCourseName: String? = null
        var targetCourseTime: String? = null
        var targetClassroom: String? = null
        var calculatedWakeTime = try {
            LocalTime.parse(defaultWakeStr, timeFormatter)
        } catch (_: Exception) {
            LocalTime.of(7, 0)
        }

        if (smartWake) {
            try {
                val db = AppDatabase.getInstance(context)
                val activeSemester = db.semesterDao().getActiveSemesterSync()
                if (activeSemester != null) {
                    val courses = db.courseDao().getCoursesBySemesterSync(activeSemester.id)
                    val holidayOverrides = db.holidayOverrideDao().getAllHolidayOverridesSync()
                    val semStartDate = LocalDate.ofEpochDay(activeSemester.startDateMillis / (24 * 60 * 60 * 1000))

                    val tomorrowResolution = ScheduleEngine.resolveDaySchedule(
                        targetDate = tomorrow,
                        semesterStartDate = semStartDate,
                        totalWeeks = activeSemester.totalWeeks,
                        allCourses = courses,
                        holidayOverrides = holidayOverrides
                    )

                    if (!tomorrowResolution.isHoliday && tomorrowResolution.activeCourses.isNotEmpty()) {
                        val firstCourse = tomorrowResolution.activeCourses.minByOrNull {
                            try {
                                LocalTime.parse(it.startTime, timeFormatter)
                            } catch (_: Exception) {
                                LocalTime.of(23, 59)
                            }
                        }

                        if (firstCourse != null) {
                            val courseStartTime = LocalTime.parse(firstCourse.startTime, timeFormatter)
                            calculatedWakeTime = courseStartTime.minusMinutes(advanceMin.toLong())
                            targetCourseName = firstCourse.name
                            targetCourseTime = firstCourse.startTime
                            targetClassroom = firstCourse.classroom
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating smart wake time: ${e.message}")
            }
        }

        // Determine wake-up trigger time
        var wakeDateTime = LocalDateTime.of(today, calculatedWakeTime)
        if (wakeDateTime.isBefore(now)) {
            wakeDateTime = LocalDateTime.of(tomorrow, calculatedWakeTime)
        }
        val wakeMillis = wakeDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Determine bedtime trigger time
        val bedtimeTime = try {
            LocalTime.parse(bedtimeStr, timeFormatter)
        } catch (_: Exception) {
            LocalTime.of(23, 0)
        }
        var bedtimeDateTime = LocalDateTime.of(today, bedtimeTime)
        if (bedtimeDateTime.isBefore(now)) {
            bedtimeDateTime = LocalDateTime.of(tomorrow, bedtimeTime)
        }
        val bedtimeMillis = bedtimeDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 1. Schedule Wake-Up Alarm
        val wakeIntent = Intent(context, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_FIRE_WAKE_UP_ALARM
            putExtra("COURSE_NAME", targetCourseName)
            putExtra("COURSE_TIME", targetCourseTime)
            putExtra("CLASSROOM", targetClassroom)
            putExtra("VIBRATE", vibrate)
        }
        val wakePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WAKE,
            wakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmExact(alarmManager, wakeMillis, wakePendingIntent)
        Log.d(TAG, "Scheduled Wake-Up Alarm at $wakeDateTime ($wakeMillis)")

        // 2. Schedule Bedtime Reminder
        val bedtimeIntent = Intent(context, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_FIRE_BEDTIME_REMINDER
            putExtra("COURSE_NAME", targetCourseName)
            putExtra("COURSE_TIME", targetCourseTime)
            putExtra("TARGET_HOURS", targetHours)
        }
        val bedtimePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BEDTIME,
            bedtimeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmExact(alarmManager, bedtimeMillis, bedtimePendingIntent)
        Log.d(TAG, "Scheduled Bedtime Reminder at $bedtimeDateTime ($bedtimeMillis)")
    }

    fun cancelSleepAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val wakeIntent = Intent(context, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_FIRE_WAKE_UP_ALARM
        }
        val wakePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WAKE,
            wakeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (wakePendingIntent != null) {
            alarmManager.cancel(wakePendingIntent)
            wakePendingIntent.cancel()
        }

        val bedtimeIntent = Intent(context, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_FIRE_BEDTIME_REMINDER
        }
        val bedtimePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BEDTIME,
            bedtimeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (bedtimePendingIntent != null) {
            alarmManager.cancel(bedtimePendingIntent)
            bedtimePendingIntent.cancel()
        }
    }

    fun triggerImmediateTestAlarm(context: Context) {
        CourseNotificationManager.showWakeUpAlarmNotification(
            context = context,
            firstCourseName = "Operating Systems",
            firstCourseTime = "08:00",
            classroom = "Building 3, Rm 204"
        )
    }

    fun openClockApp(context: Context): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Could not open clock app: ${e.message}")
                false
            }
        }
    }

    fun setInDeviceClockApp(context: Context, hour: Int, minute: Int, message: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not open system clock app: ${e.message}")
            false
        }
    }

    private fun scheduleAlarmExact(alarmManager: AlarmManager, timeMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission denied, fallback to setAndAllowWhileIdle: ${e.message}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
        }
    }
}
