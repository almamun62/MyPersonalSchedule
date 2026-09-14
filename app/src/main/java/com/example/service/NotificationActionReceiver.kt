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
        }
    }
}
