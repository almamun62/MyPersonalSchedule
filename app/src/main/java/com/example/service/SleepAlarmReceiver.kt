package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SleepAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("SleepAlarmReceiver", "Received sleep alarm action: $action")

        when (action) {
            ACTION_FIRE_WAKE_UP_ALARM -> {
                val courseName = intent.getStringExtra("COURSE_NAME")
                val courseTime = intent.getStringExtra("COURSE_TIME")
                val classroom = intent.getStringExtra("CLASSROOM")
                val vibrate = intent.getBooleanExtra("VIBRATE", true)

                if (vibrate) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                            val vibrator = vibratorManager?.defaultVibrator
                            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            vibrator?.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
                        }
                    } catch (e: Exception) {
                        Log.e("SleepAlarmReceiver", "Error vibrating: ${e.message}")
                    }
                }

                CourseNotificationManager.showWakeUpAlarmNotification(
                    context = context,
                    firstCourseName = courseName,
                    firstCourseTime = courseTime,
                    classroom = classroom
                )

                // Reschedule for next day in coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    SleepAlarmScheduler.scheduleNextSleepAlarms(context)
                }
            }

            ACTION_FIRE_BEDTIME_REMINDER -> {
                val courseName = intent.getStringExtra("COURSE_NAME")
                val courseTime = intent.getStringExtra("COURSE_TIME")
                val targetHours = intent.getFloatExtra("TARGET_HOURS", 8.0f)

                CourseNotificationManager.showBedtimeReminderNotification(
                    context = context,
                    firstCourseName = courseName,
                    firstCourseTime = courseTime,
                    targetSleepHours = targetHours
                )
            }
        }
    }

    companion object {
        const val ACTION_FIRE_WAKE_UP_ALARM = "com.example.ACTION_FIRE_WAKE_UP_ALARM"
        const val ACTION_FIRE_BEDTIME_REMINDER = "com.example.ACTION_FIRE_BEDTIME_REMINDER"
    }
}
