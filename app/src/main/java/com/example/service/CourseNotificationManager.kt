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
    const val CHANNEL_ONGOING_ID = "channel_ongoing_class_v2"
    const val CHANNEL_NAG_ID = "channel_nag_loop_v2"
    const val CHANNEL_REMINDER_15M_ID = "channel_class_reminder_15m_v2"
    const val CHANNEL_SLEEP_ALARM_ID = "channel_sleep_wake_alarm_v1"
    const val CHANNEL_WIND_DOWN_ID = "channel_bedtime_wind_down_v1"
    const val CHANNEL_FOCUS_LOCK_ID = "channel_focus_lock_v1"

    const val NOTIFICATION_ID_STICKY = 1001
    const val NOTIFICATION_ID_NAG = 1002
    const val NOTIFICATION_BASE_REMINDER_15M = 20000
    const val NOTIFICATION_ID_WAKE_ALARM = 30001
    const val NOTIFICATION_ID_WIND_DOWN = 30002
    const val NOTIFICATION_ID_FOCUS_LOCK = 40001
    const val NOTIFICATION_ID_FOCUS_COMPLETE = 40002

    const val ACTION_MARK_READ = "com.example.ACTION_MARK_READ"
    const val ACTION_DISMISS_STICKY = "com.example.ACTION_DISMISS_STICKY"
    const val ACTION_DISMISS_15M = "com.example.ACTION_DISMISS_15M"
    const val ACTION_DISMISS_WAKE_ALARM = "com.example.ACTION_DISMISS_WAKE_ALARM"
    const val ACTION_SNOOZE_WAKE_ALARM = "com.example.ACTION_SNOOZE_WAKE_ALARM"
    const val ACTION_POST_CLASS_PROMPT = "com.example.ACTION_POST_CLASS_PROMPT"
    const val ACTION_FOCUS_LOCK = "com.example.ACTION_FOCUS_LOCK"
    const val ACTION_STOP_FOCUS_LOCK = "com.example.ACTION_STOP_FOCUS_LOCK"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val ongoingChannel = NotificationChannel(
                CHANNEL_ONGOING_ID,
                "Active Class In-Session",
                NotificationManager.IMPORTANCE_DEFAULT
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

            val reminder15mChannel = NotificationChannel(
                CHANNEL_REMINDER_15M_ID,
                "Upcoming Class Reminders (15m)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts 15 minutes before a class starts with room location and course title"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(ongoingChannel)
            notificationManager.createNotificationChannel(nagChannel)
            notificationManager.createNotificationChannel(reminder15mChannel)

            val sleepAlarmChannel = NotificationChannel(
                CHANNEL_SLEEP_ALARM_ID,
                "Wake-Up Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wake-up alarm with audio and vibration"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val windDownChannel = NotificationChannel(
                CHANNEL_WIND_DOWN_ID,
                "Bedtime Wind-Down",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle bedtime reminder before tomorrow's classes"
                enableVibration(true)
                setShowBadge(true)
            }

            val focusLockChannel = NotificationChannel(
                CHANNEL_FOCUS_LOCK_ID,
                "Study Focus Lock",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing timer while Study Phone Lock is active"
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(sleepAlarmChannel)
            notificationManager.createNotificationChannel(windDownChannel)
            notificationManager.createNotificationChannel(focusLockChannel)
        }
    }

    fun buildFocusLockNotification(
        context: Context,
        remainingFormatted: String,
        courseName: String
    ): android.app.Notification {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_FOCUS_LOCK
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            40001,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(ACTION_STOP_FOCUS_LOCK).apply {
            setPackage(context.packageName)
        }
        val stopPending = PendingIntent.getBroadcast(
            context,
            40003,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (courseName.isNotBlank()) "🔒 Study Lock: $courseName" else "🔒 Study Focus Lock Active"
        val text = "Remaining: $remainingFormatted • Distracting apps blocked"

        return NotificationCompat.Builder(context, CHANNEL_FOCUS_LOCK_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Lock", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    fun showFocusCompleteNotification(context: Context, durationMinutes: Long) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            40002,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_REMINDER_15M_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 Focus Session Complete!")
            .setContentText("Great job! You finished $durationMinutes minutes of deep study.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_FOCUS_COMPLETE, notif)
    }

    /**
     * Ongoing sticky notification with action buttons [Mark as Read] / [Take Notes].
     */
    fun showStickyOngoingClassNotification(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String = "",
        endTime: String = "11:40",
        instructor: String = "",
        notes: String = "",
        minutesRemaining: Long = 45
    ) = showStickyOngoingNotification(context, courseName, classroom, endTime, minutesRemaining)

    fun showNagClassNotification(
        context: Context,
        courseName: String,
        classroom: String,
        startTime: String = "10:00",
        endTime: String = "",
        instructor: String = "",
        nagCount: Int = 1
    ) = showNagReminder(context, courseName, classroom, startTime)

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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
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
            .setPriority(NotificationCompat.PRIORITY_MAX)
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

    /**
     * Reminds users 15 minutes before a class starts, including the location and course title.
     */
    fun show15MinuteClassReminder(
        context: Context,
        courseId: Long,
        courseName: String,
        classroom: String,
        startTime: String,
        endTime: String = "",
        instructor: String = "",
        notes: String = ""
    ) {
        createNotificationChannels(context)

        val notificationId = (NOTIFICATION_BASE_REMINDER_15M + (courseId % 10000)).toInt()

        // Open app when notification clicked
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = "OPEN_COURSE_TIMETABLE"
            putExtra("COURSE_ID", courseId)
            putExtra("COURSE_NAME", courseName)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_15M
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 50000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locationText = if (classroom.isNotBlank()) classroom else "Campus Classroom"
        val timeSpanText = if (endTime.isNotBlank()) "$startTime – $endTime" else "Starts at $startTime"

        val bigText = buildString {
            append("📍 Location: $locationText\n")
            append("⏰ Time: $timeSpanText (Starts in 15 mins)")
            if (instructor.isNotBlank()) {
                append("\n👨‍🏫 Instructor: $instructor")
            }
            if (notes.isNotBlank()) {
                append("\n📝 $notes")
            } else {
                append("\n⚡ Have your textbook & lab materials ready!")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER_15M_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Class in 15 mins: $courseName")
            .setContentText("📍 $locationText • Starts at $startTime")
            .setSubText("Upcoming Class")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "View Timetable", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun dismiss15MinuteReminder(context: Context, courseId: Long) {
        val notificationId = (NOTIFICATION_BASE_REMINDER_15M + (courseId % 10000)).toInt()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    fun showWakeUpAlarmNotification(
        context: Context,
        firstCourseName: String?,
        firstCourseTime: String?,
        classroom: String?
    ) {
        createNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_WAKE_ALARM,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_WAKE_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_WAKE_ALARM + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasClass = !firstCourseName.isNullOrBlank()
        val title = if (hasClass) "⏰ Wake-Up! Class starts soon" else "⏰ Good Morning! Time to wake up"
        val subtitle = if (hasClass) {
            "First class: $firstCourseName at $firstCourseTime (${classroom ?: "TBA"})"
        } else {
            "No early classes today. Enjoy your rest!"
        }

        val bigText = buildString {
            append("🌅 Good Morning!\n")
            if (hasClass) {
                append("📚 Today's First Class: $firstCourseName\n")
                append("⏰ Start Time: $firstCourseTime\n")
                if (!classroom.isNullOrBlank()) {
                    append("📍 Classroom: $classroom\n")
                }
                append("Have a productive day ahead!")
            } else {
                append("No scheduled classes this morning. Have a wonderful day!")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SLEEP_ALARM_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText("Wake-Up Alarm")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "View Timetable", openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_WAKE_ALARM, notification)
    }

    fun showBedtimeReminderNotification(
        context: Context,
        firstCourseName: String?,
        firstCourseTime: String?,
        targetSleepHours: Float
    ) {
        createNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_WIND_DOWN,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasClass = !firstCourseName.isNullOrBlank()
        val title = "🌙 Wind-down Time for Bed"
        val desc = if (hasClass) {
            "Tomorrow's first class is $firstCourseName at $firstCourseTime. Sleep now for $targetSleepHours h of rest!"
        } else {
            "Time to rest and recharge for tomorrow (${targetSleepHours}h sleep goal)."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_WIND_DOWN_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(desc)
            .setStyle(NotificationCompat.BigTextStyle().bigText(desc))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_WIND_DOWN, notification)
    }

    fun dismissWakeUpAlarm(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_WAKE_ALARM)
    }
}
