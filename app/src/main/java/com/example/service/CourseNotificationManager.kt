package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object CourseNotificationManager {
    const val CHANNEL_ONGOING_ID = "channel_ongoing_class"
    const val CHANNEL_NAG_ID = "channel_nag_loop"
    const val NOTIFICATION_ID_STICKY = 1001
    const val NOTIFICATION_ID_NAG = 1002

    const val ACTION_MARK_READ = "com.example.ACTION_MARK_READ"
    const val ACTION_DISMISS_STICKY = "com.example.ACTION_DISMISS_STICKY"
    const val ACTION_POST_CLASS_PROMPT = "com.example.ACTION_POST_CLASS_PROMPT"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val ongoingChannel = NotificationChannel(
                CHANNEL_ONGOING_ID,
                "Active Class In-Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing sticky notification during active class sessions"
                setShowBadge(false)
            }

            val nagChannel = NotificationChannel(
                CHANNEL_NAG_ID,
                "Class Starting Nag & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "5-minute repeating nag reminders for upcoming classes"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(ongoingChannel)
            notificationManager.createNotificationChannel(nagChannel)
        }
    }

    /**
     * Ongoing sticky notification with action buttons [Mark as Read] / [Take Notes].
     */
    fun showStickyOngoingNotification(
        context: Context,
        courseName: String,
        classroom: String,
        endTime: String,
        minutesRemaining: Long
    ) {
        createNotificationChannels(context)

        // Intent to open MainActivity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark as Read / Dismiss
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_STICKY
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Post-Class Task Prompt
        val taskIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_POST_CLASS_PROMPT
            putExtra("COURSE_NAME", courseName)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val taskPendingIntent = PendingIntent.getActivity(
            context,
            2,
            taskIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ONGOING_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("In Class: $courseName")
            .setContentText("$classroom • Ends at $endTime ($minutesRemaining mins left)")
            .setSubText("Do Not Disturb Active")
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark as Read", dismissPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "+ Homework Task", taskPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_STICKY, notification)
    }

    /**
     * 5-minute repeating nag reminder until acknowledged.
     */
    fun showNagReminder(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String
    ) {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_STICKY
            putExtra("NOTIFICATION_ID", NOTIFICATION_ID_NAG)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            11,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NAG_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Class Starts in 5 Mins!")
            .setContentText("$courseName begins at $startTime in $classroom")
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Acknowledge", dismissPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_NAG, notification)
    }

    fun dismissStickyNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_STICKY)
    }

    fun dismissNagNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_NAG)
    }
}
