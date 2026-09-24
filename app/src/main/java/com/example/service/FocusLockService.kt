package com.example.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.local.UserPreferencesManager
import kotlinx.coroutines.*

class FocusLockService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopFocusMonitoring()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val prefs = UserPreferencesManager.getInstance(applicationContext)
        val endTime = prefs.focusLockEndTimeMillis.value
        val courseName = prefs.focusLockCourseName.value

        if (endTime <= System.currentTimeMillis()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val initialFormatted = formatRemaining(endTime - System.currentTimeMillis())
        val notification = CourseNotificationManager.buildFocusLockNotification(
            context = this,
            remainingFormatted = initialFormatted,
            courseName = courseName
        )

        try {
            startForeground(CourseNotificationManager.NOTIFICATION_ID_FOCUS_LOCK, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }

        startFocusMonitoring(endTime, courseName)

        return START_STICKY
    }

    private fun startFocusMonitoring(endTimeMillis: Long, courseName: String) {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val prefs = UserPreferencesManager.getInstance(applicationContext)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

            var lastNotificationUpdate = 0L

            while (isActive) {
                val now = System.currentTimeMillis()
                val remainingMillis = endTimeMillis - now

                if (remainingMillis <= 0) {
                    // Focus session complete!
                    val totalDurationSec = prefs.focusLockDurationSeconds.value
                    prefs.stopFocusLock()
                    CourseNotificationManager.showFocusCompleteNotification(
                        context = this@FocusLockService,
                        durationMinutes = (totalDurationSec / 60).coerceAtLeast(1)
                    )
                    vibrateComplete()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                // Update notification every 1 second
                if (now - lastNotificationUpdate >= 1000L) {
                    lastNotificationUpdate = now
                    val formatted = formatRemaining(remainingMillis)
                    val updatedNotification = CourseNotificationManager.buildFocusLockNotification(
                        context = this@FocusLockService,
                        remainingFormatted = formatted,
                        courseName = courseName
                    )
                    notificationManager.notify(CourseNotificationManager.NOTIFICATION_ID_FOCUS_LOCK, updatedNotification)
                }

                // Check foreground app
                val whitelisted = prefs.focusWhitelistedPackages.value
                val currentPackage = getForegroundPackage(usageStatsManager)

                if (currentPackage != null && !isPackageAllowed(currentPackage, whitelisted)) {
                    Log.w(TAG, "Restricted app launched during focus: $currentPackage. Bringing app to front.")
                    bringStudyAppToFront()
                }

                delay(300L)
            }
        }
    }

    private fun isPackageAllowed(pkg: String, whitelisted: Set<String>): Boolean {
        // System UI, keyboards, and phone calls should never be blocked. Do NOT allow launchers!
        if (pkg == packageName || pkg == "com.example" || pkg.startsWith("com.aistudio.")) return true
        if (pkg == "android" || pkg == "com.android.systemui" || pkg.contains("inputmethod")) return true
        if (whitelisted.contains(pkg)) return true
        return false
    }

    private fun getForegroundPackage(usm: UsageStatsManager?): String? {
        if (usm == null) return null
        return try {
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 2000, now)
            var lastPkg: String? = null
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val type = event.eventType
                if (type == UsageEvents.Event.ACTIVITY_RESUMED || 
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && type == UsageEvents.Event.MOVE_TO_FOREGROUND)) {
                    lastPkg = event.packageName
                }
            }
            lastPkg
        } catch (_: Exception) {
            null
        }
    }

    private fun bringStudyAppToFront() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = CourseNotificationManager.ACTION_FOCUS_LOCK
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)

            // Vibrate feedback
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bring study app to front", e)
        }
    }

    private fun vibrateComplete() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300, 150, 500), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 300, 150, 300, 150, 500), -1)
        }
    }

    private fun formatRemaining(millis: Long): String {
        val totalSec = (millis / 1000).coerceAtLeast(0)
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return String.format("%02d:%02d:%02d", hours, mins, secs)
    }

    private fun stopFocusMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "FocusLockService"
        const val ACTION_START = "com.example.service.ACTION_START_FOCUS_LOCK"
        const val ACTION_STOP = "com.example.service.ACTION_STOP_FOCUS_LOCK"

        fun start(context: Context) {
            val intent = Intent(context, FocusLockService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusLockService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun hasUsageStatsPermission(context: Context): Boolean {
            return com.example.util.UsagePermissionHelper.hasUsageStatsPermission(context)
        }

        fun requestUsageStatsPermission(context: Context) {
            com.example.util.UsagePermissionHelper.requestUsageStatsPermission(context)
        }
    }
}
