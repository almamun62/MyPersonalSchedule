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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import kotlinx.coroutines.*

class FocusLockService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var remainingSeconds = 25 * 60
    private var totalSeconds = 25 * 60
    private var courseName = "General Study"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val durationMin = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 25) ?: 25
                courseName = intent?.getStringExtra(EXTRA_COURSE_NAME) ?: "General Study"
                totalSeconds = durationMin * 60
                remainingSeconds = totalSeconds

                startForegroundWithNotification()
                startTimer()
            }
        }

        return START_NOT_STICKY
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
            while (isActive && remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
                updateNotification()
            }

            if (remainingSeconds <= 0) {
                onFocusCompleted()
            }
        }
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun onFocusCompleted() {
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

    private fun buildNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        val timeStr = String.format("%02d:%02d", m, s)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏱️ Study Focus: $courseName")
            .setContentText("$timeStr remaining • Distractions shielded")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(totalSeconds, totalSeconds - remainingSeconds, false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "study_focus_timer_channel"
        const val NOTIFICATION_ID = 9002
        const val ACTION_START = "com.example.service.action.START_FOCUS"
        const val ACTION_STOP = "com.example.service.action.STOP_FOCUS"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_COURSE_NAME = "extra_course_name"

        fun startFocus(context: Context, minutes: Int, courseName: String) {
            val intent = Intent(context, FocusLockService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_MINUTES, minutes)
                putExtra(EXTRA_COURSE_NAME, courseName)
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
