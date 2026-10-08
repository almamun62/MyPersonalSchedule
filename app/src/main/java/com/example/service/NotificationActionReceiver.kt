package com.example.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Handles action buttons on notifications (Dismiss, Snooze).
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        when (action) {
            ACTION_DISMISS -> {
                if (notificationId != 0) {
                    nm?.cancel(notificationId)
                }
            }
            ACTION_SNOOZE -> {
                if (notificationId != 0) {
                    nm?.cancel(notificationId)
                }
                // Schedule a snoozed reminder in 5 minutes
                val courseName = intent.getStringExtra(ClassReminderReceiver.EXTRA_COURSE_NAME) ?: "Upcoming Class"
                val classroom = intent.getStringExtra(ClassReminderReceiver.EXTRA_CLASSROOM) ?: ""
                val startTime = intent.getStringExtra(ClassReminderReceiver.EXTRA_START_TIME) ?: ""
                val courseId = intent.getLongExtra(ClassReminderReceiver.EXTRA_COURSE_ID, 0L)

                val snoozeIntent = Intent(context, ClassReminderReceiver::class.java).apply {
                    putExtra(ClassReminderReceiver.EXTRA_COURSE_NAME, courseName)
                    putExtra(ClassReminderReceiver.EXTRA_CLASSROOM, classroom)
                    putExtra(ClassReminderReceiver.EXTRA_START_TIME, startTime)
                    putExtra(ClassReminderReceiver.EXTRA_COURSE_ID, courseId)
                    putExtra(ClassReminderReceiver.EXTRA_MINUTES_BEFORE, 5)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    (courseId * 10 + 99).toInt(),
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val triggerAt = System.currentTimeMillis() + (5 * 60 * 1000L)
                alarmManager?.let { am ->
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                        } else {
                            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.example.service.action.DISMISS"
        const val ACTION_SNOOZE = "com.example.service.action.SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
