package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.data.local.UserPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DndAutomationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: "Lecture"
        val classroom = intent.getStringExtra(EXTRA_CLASSROOM) ?: ""

        val prefs = UserPreferencesManager.getInstance(context)
        val isDndEnabled = prefs.isAutoDndEnabled.value

        if (!isDndEnabled) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_ENABLE_CLASS_DND -> {
                enableDndMode(context, notificationManager, courseName, classroom)
            }
            ACTION_DISABLE_CLASS_DND -> {
                disableDndMode(context, notificationManager, courseName)
            }
        }

        // Reschedule future alarms asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DndAutomationScheduler.scheduleAllDndAlarms(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun enableDndMode(
        context: Context,
        notificationManager: NotificationManager,
        courseName: String,
        classroom: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                try {
                    // Activate Priority Interruption Filter (Do Not Disturb)
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    showDndNotification(
                        context,
                        notificationManager,
                        title = "🤫 Class DND Active: $courseName",
                        text = "Phone silenced for lecture in ${classroom.ifEmpty { "class" }}. DND will auto-turn off when class ends.",
                        notificationId = NOTIFICATION_ID_DND_ACTIVE
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else {
                showDndNotification(
                    context,
                    notificationManager,
                    title = "⚠️ DND Permission Required for $courseName",
                    text = "Tap to grant Do Not Disturb policy access in Android Settings.",
                    notificationId = NOTIFICATION_ID_DND_PERM
                )
            }
        }
    }

    private fun disableDndMode(
        context: Context,
        notificationManager: NotificationManager,
        courseName: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                try {
                    // Restore All Interruption Filter (Normal Ringer Mode)
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    showDndNotification(
                        context,
                        notificationManager,
                        title = "🔔 Class Ended: DND Turned Off",
                        text = "Ringer and notification alerts restored following $courseName.",
                        notificationId = NOTIFICATION_ID_DND_ENDED
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showDndNotification(
        context: Context,
        notificationManager: NotificationManager,
        title: String,
        text: String,
        notificationId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_DND,
                "Class DND Automation",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Automated Do Not Disturb notifications for class schedules"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_DND)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        const val ACTION_ENABLE_CLASS_DND = "com.example.action.ENABLE_CLASS_DND"
        const val ACTION_DISABLE_CLASS_DND = "com.example.action.DISABLE_CLASS_DND"
        const val EXTRA_COURSE_NAME = "extra_course_name"
        const val EXTRA_CLASSROOM = "extra_classroom"

        const val CHANNEL_ID_DND = "channel_class_dnd"
        const val NOTIFICATION_ID_DND_ACTIVE = 1001
        const val NOTIFICATION_ID_DND_ENDED = 1002
        const val NOTIFICATION_ID_DND_PERM = 1003
    }
}
