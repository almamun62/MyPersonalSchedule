package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CourseNotificationManager.ACTION_DISMISS_STICKY -> {
                val notifId = intent.getIntExtra("NOTIFICATION_ID", CourseNotificationManager.NOTIFICATION_ID_STICKY)
                if (notifId == CourseNotificationManager.NOTIFICATION_ID_NAG) {
                    CourseNotificationManager.dismissNagNotification(context)
                } else {
                    CourseNotificationManager.dismissStickyNotification(context)
                }
            }
            CourseNotificationManager.ACTION_MARK_READ -> {
                CourseNotificationManager.dismissStickyNotification(context)
            }
            CourseNotificationManager.ACTION_DISMISS_15M -> {
                val notifId = intent.getIntExtra("NOTIFICATION_ID", 0)
                if (notifId > 0) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    nm.cancel(notifId)
                }
            }
            CourseNotificationManager.ACTION_DISMISS_WAKE_ALARM -> {
                CourseNotificationManager.dismissWakeUpAlarm(context)
            }
            CourseNotificationManager.ACTION_STOP_FOCUS_LOCK -> {
                val prefs = com.example.data.local.UserPreferencesManager.getInstance(context)
                prefs.stopFocusLock()
                val stopServiceIntent = Intent(context, FocusLockService::class.java).apply {
                    action = FocusLockService.ACTION_STOP
                }
                context.startService(stopServiceIntent)
            }
        }
    }
}
