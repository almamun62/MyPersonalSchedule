package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.data.local.UserPreferencesManager
import kotlinx.coroutines.*

/**
 * Foreground Service for Study Focus Lock.
 * Survives process death and operates with screen off by storing end timestamp.
 */
class FocusLockService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var targetEndTimeMillis: Long = 0L
    private var totalSeconds: Int = 25 * 60
    private var courseName: String = "General Study"
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CourseSchedule:FocusLockWakeLock")
            wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours max
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                val prefs = UserPreferencesManager.getInstance(this)
                prefs.clearFocusSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val prefs = UserPreferencesManager.getInstance(this)
                val durationMin = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 25) ?: 25
                courseName = intent?.getStringExtra(EXTRA_COURSE_NAME) ?: "General Study"
                totalSeconds = durationMin * 60

                val passedEndTime = intent?.getLongExtra(EXTRA_END_TIME_MILLIS, 0L) ?: 0L
                targetEndTimeMillis = if (passedEndTime > System.currentTimeMillis()) {
                    passedEndTime
                } else {
                    System.currentTimeMillis() + (totalSeconds * 1000L)
                }

                // Persist to preferences so session survives process death
                prefs.setFocusSession(
                    active = true,
                    endTimestamp = targetEndTimeMillis,
                    totalMinutes = durationMin,
                    courseName = courseName
                )

                startForegroundWithNotification()
                startTimer()
            }
            else -> {
                // If restarted by system after process death
                val prefs = UserPreferencesManager.getInstance(this)
                if (prefs.focusActive.value && prefs.focusEndTimestamp.value > 0L) {
                    targetEndTimeMillis = prefs.focusEndTimestamp.value
                    courseName = prefs.focusCourseName.value
                    totalSeconds = prefs.focusTotalMinutes.value * 60
                    if (targetEndTimeMillis > System.currentTimeMillis()) {
                        startForegroundWithNotification()
                        startTimer()
                    } else {
                        onFocusCompleted()
                    }
                } else {
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Focus Lock",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live countdown and shield for study focus sessions"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val remainingSec = ((targetEndTimeMillis - now) / 1000L).coerceAtLeast(0L).toInt()

                if (remainingSec <= 0) {
                    onFocusCompleted()
                    break
                }

                updateNotification(remainingSec)
                delay(1000L)
            }
        }
    }

    private fun updateNotification(remainingSeconds: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification(remainingSeconds))
    }

    private fun onFocusCompleted() {
        val prefs = UserPreferencesManager.getInstance(this)
        prefs.clearFocusSession()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val completedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🎉 Focus Session Completed!")
            .setContentText("Great job studying $courseName! Take a short 5-minute break.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm?.notify(NOTIFICATION_ID, completedNotification)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun buildNotification(remainingSeconds: Int? = null): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val remSec = remainingSeconds ?: ((targetEndTimeMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
        val m = remSec / 60
        val s = remSec % 60
        val timeStr = String.format("%02d:%02d", m, s)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏱️ Study Focus: $courseName")
            .setContentText("$timeStr remaining • Distractions shielded")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(totalSeconds, (totalSeconds - remSec).coerceAtLeast(0), false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CHANNEL_ID = "study_focus_timer_channel"
        const val NOTIFICATION_ID = 9002
        const val ACTION_START = "com.example.service.action.START_FOCUS"
        const val ACTION_STOP = "com.example.service.action.STOP_FOCUS"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_END_TIME_MILLIS = "extra_end_time_millis"
        const val EXTRA_COURSE_NAME = "extra_course_name"

        fun startFocus(context: Context, minutes: Int, courseName: String, endTimeMillis: Long = 0L) {
            val intent = Intent(context, FocusLockService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_MINUTES, minutes)
                putExtra(EXTRA_COURSE_NAME, courseName)
                if (endTimeMillis > 0L) {
                    putExtra(EXTRA_END_TIME_MILLIS, endTimeMillis)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopFocus(context: Context) {
            val intent = Intent(context, FocusLockService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
