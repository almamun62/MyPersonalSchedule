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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.domain.HyperIslandManager
import com.example.domain.HyperIslandState
import com.example.domain.HyperIslandStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

class HyperIslandLiveService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null

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
            ACTION_START, ACTION_UPDATE -> {
                startForegroundWithInitialNotification()
                startTickerLoop()
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HyperIslandManager.CHANNEL_ID,
                "HyperIsland Live Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Glanceable live class status and countdowns for HyperIsland and lock screen"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithInitialNotification() {
        val initialStatus = HyperIslandStatus(
            state = HyperIslandState.IDLE,
            title = "HyperIsland Active",
            subtitle = "Monitoring today's class schedule...",
            timeBadge = "Active"
        )
        val notification = buildRemoteViewsNotification(initialStatus)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                HyperIslandManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(HyperIslandManager.NOTIFICATION_ID, notification)
        }
    }

    private fun startTickerLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                updateLiveStatus()
                delay(30_000L) // Update every 30 seconds for live countdown accuracy
            }
        }
    }

    private suspend fun updateLiveStatus() = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val prefs = UserPreferencesManager.getInstance(applicationContext)

            val courseEntities = db.courseDao().getAllCourses().first()
            val courses = courseEntities.map { com.example.domain.model.Course.fromEntity(it) }
            val timings = prefs.sectionTimings.value

            val todayDow = LocalDate.now().dayOfWeek.value
            val todayCourses = courses.filter { it.dayOfWeek == todayDow }.sortedBy { it.startPeriod }

            val status = HyperIslandManager.calculateStatus(todayCourses, timings, LocalTime.now())

            withContext(Dispatchers.Main) {
                val notification = buildRemoteViewsNotification(status)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(HyperIslandManager.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildRemoteViewsNotification(status: HyperIslandStatus): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // 1. Collapsed RemoteViews (Compact pill)
        val collapsedViews = RemoteViews(packageName, R.layout.notification_hyper_island_collapsed).apply {
            setTextViewText(R.id.island_collapsed_title, status.title)
            setTextViewText(R.id.island_collapsed_subtitle, status.subtitle)
            setTextViewText(R.id.island_collapsed_badge, status.timeBadge)
        }

        // 2. Expanded RemoteViews (Rich card)
        val expandedViews = RemoteViews(packageName, R.layout.notification_hyper_island_expanded).apply {
            val statusTag = when (status.state) {
                HyperIslandState.ACTIVE -> "🟢 IN CLASS NOW"
                HyperIslandState.URGENT -> "🔔 CLASS STARTING SOON"
                HyperIslandState.IDLE -> "⚡ HYPERISLAND SCHEDULE"
            }
            setTextViewText(R.id.island_status_tag, statusTag)
            setTextViewText(R.id.island_time_countdown, status.timeBadge)

            val courseTitle = if (status.courseName.isNotBlank()) status.courseName else status.title
            setTextViewText(R.id.island_course_name, courseTitle)

            val locationText = if (status.room.isNotBlank()) "📍 Room ${status.room} • ${status.subtitle}" else status.subtitle
            setTextViewText(R.id.island_location_info, locationText)

            val progressInt = (status.progress * 100).toInt().coerceIn(0, 100)
            setProgressBar(R.id.island_progress_bar, 100, progressInt, false)
        }

        val iconRes = android.R.drawable.ic_lock_idle_alarm

        return NotificationCompat.Builder(this, HyperIslandManager.CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(status.title)
            .setContentText(status.subtitle)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(if (status.state == HyperIslandState.URGENT) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "com.example.service.action.START_HYPER_ISLAND"
        const val ACTION_STOP = "com.example.service.action.STOP_HYPER_ISLAND"
        const val ACTION_UPDATE = "com.example.service.action.UPDATE_HYPER_ISLAND"

        fun startService(context: Context) {
            val intent = Intent(context, HyperIslandLiveService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, HyperIslandLiveService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
