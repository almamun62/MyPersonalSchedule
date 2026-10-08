package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.UserPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for Class Reminders.
 * Handles exact alarms before classes start and BOOT_COMPLETED events.
 */
class ClassReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (Intent.ACTION_BOOT_COMPLETED == action) {
            // Reschedule all alarms on device boot
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ClassReminderScheduler.rescheduleAllAlarms(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }

        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: "Upcoming Class"
        val classroom = intent.getStringExtra(EXTRA_CLASSROOM) ?: ""
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val courseId = intent.getLongExtra(EXTRA_COURSE_ID, 0L)
        val minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 15)

        val prefs = UserPreferencesManager.getInstance(context)
        if (!prefs.isClassReminder15mEnabled.value) return

        showClassReminderNotification(
            context = context,
            courseName = courseName,
            classroom = classroom,
            startTime = startTime,
            courseId = courseId,
            minutesBefore = minutesBefore
        )
    }

    private fun showClassReminderNotification(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String,
        courseId: Long,
        minutesBefore: Int
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Advance notifications for upcoming lectures and lab classes"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (courseId * 10).toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, (NOTIFICATION_ID_OFFSET + courseId).toInt())
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (courseId * 10 + 1).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(EXTRA_COURSE_NAME, courseName)
            putExtra(EXTRA_CLASSROOM, classroom)
            putExtra(EXTRA_START_TIME, startTime)
            putExtra(EXTRA_COURSE_ID, courseId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, (NOTIFICATION_ID_OFFSET + courseId).toInt())
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (courseId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roomText = if (classroom.isNotBlank()) " in $classroom" else ""
        val timeText = if (startTime.isNotBlank()) " at $startTime" else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🔔 $courseName starts in $minutesBefore min")
            .setContentText("Class begins$timeText$roomText. Head over to class now!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Class begins$timeText$roomText.\nMake sure you have your notes and materials ready.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 5m", snoozePendingIntent)
            .build()

        nm.notify((NOTIFICATION_ID_OFFSET + courseId).toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "class_reminders_channel"
        const val NOTIFICATION_ID_OFFSET = 70000L
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_CLASSROOM = "extra_classroom"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"
    }
}
