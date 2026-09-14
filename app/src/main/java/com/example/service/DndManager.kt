package com.example.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

object DndManager {
    private const val TAG = "DndManager"

    /**
     * Checks if notification policy access (Do Not Disturb access) is granted.
     */
    fun checkDndPermission(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.isNotificationPolicyAccessGranted == true
    }

    /**
     * Opens system settings to request DND policy access.
     */
    fun requestDndPermission(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Enables or disables Do Not Disturb mode.
     * Uses INTERRUPTION_FILTER_PRIORITY when enabled (allowing alarms/priority calls during class),
     * and INTERRUPTION_FILTER_ALL when disabled.
     */
    fun setDnd(context: Context, enable: Boolean): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "Cannot set DND: notification policy access not granted")
            return false
        }

        return try {
            val filter = if (enable) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            notificationManager.setInterruptionFilter(filter)
            Log.i(TAG, "DND interruption filter set to: $filter")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting DND filter", e)
            false
        }
    }

    /**
     * Returns true if DND is currently active (priority, none, or alarms only).
     */
    fun isDndActive(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        val current = notificationManager.currentInterruptionFilter
        return current != NotificationManager.INTERRUPTION_FILTER_ALL &&
                current != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }
}
