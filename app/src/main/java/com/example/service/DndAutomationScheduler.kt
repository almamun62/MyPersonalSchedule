package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.domain.ScheduleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DndAutomationScheduler {
    private const val TAG = "DndAutomationScheduler"

    suspend fun scheduleDndAlarms(context: Context) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val activeSemester = database.semesterDao().getActiveSemesterSync() ?: run {
            Log.d(TAG, "No active semester found, skipping DND scheduling")
            return@withContext
        }
        val courses = database.courseDao().getCoursesBySemesterSync(activeSemester.id)
        val holidayOverrides = database.holidayOverrideDao().getAllHolidayOverridesSync()

        if (courses.isEmpty()) {
            Log.d(TAG, "No courses found, skipping DND scheduling")
            return@withContext
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val today = LocalDate.now()
        val semStartDate = LocalDate.ofEpochDay(activeSemester.startDateMillis / (24 * 60 * 60 * 1000))
        val currentTimeMillis = System.currentTimeMillis()

        var scheduledCount = 0

        // Look ahead 7 days
        for (dayOffset in 0..7) {
            val targetDate = today.plusDays(dayOffset.toLong())
            val dayResolution = ScheduleEngine.resolveDaySchedule(
                targetDate = targetDate,
                semesterStartDate = semStartDate,
                totalWeeks = activeSemester.totalWeeks,
                allCourses = courses,
                holidayOverrides = holidayOverrides
            )

            // Skip DND for holidays
            if (dayResolution.isHoliday) {
                continue
            }

            for (course in dayResolution.activeCourses) {
                try {
                    val startTime = LocalTime.parse(course.startTime, timeFormatter)
                    val endTime = LocalTime.parse(course.endTime, timeFormatter)

                    val startMillis = LocalDateTime.of(targetDate, startTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMillis = LocalDateTime.of(targetDate, endTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    val baseReqCode = (targetDate.dayOfYear * 10000) + (course.id.toInt() % 10000)

                    // Schedule ON
                    if (startMillis > currentTimeMillis) {
                        scheduleExactAlarm(
                            context = context,
                            alarmManager = alarmManager,
                            timeMillis = startMillis,
                            reqCode = baseReqCode,
                            actionStr = DndAutomationReceiver.ACTION_DND_TURN_ON
                        )
                        scheduledCount++
                    }

                    // Schedule OFF
                    if (endMillis > currentTimeMillis) {
                        scheduleExactAlarm(
                            context = context,
                            alarmManager = alarmManager,
                            timeMillis = endMillis,
                            reqCode = baseReqCode + 100000,
                            actionStr = DndAutomationReceiver.ACTION_DND_TURN_OFF
                        )
                        scheduledCount++
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error scheduling DND for course: ${course.name}", e)
                }
            }
        }
        Log.i(TAG, "Scheduled $scheduledCount exact DND toggles for the next 7 days.")
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        timeMillis: Long,
        reqCode: Int,
        actionStr: String
    ) {
        val intent = Intent(context, DndAutomationReceiver::class.java).apply {
            action = actionStr
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission restricted, falling back to inexact alarm", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }
}
