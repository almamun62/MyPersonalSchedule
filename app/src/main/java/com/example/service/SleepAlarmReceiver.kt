package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * BroadcastReceiver for Evening/Bedtime Schedule Sync.
 * Checks tomorrow's classes and posts a bedtime summary with the time of the first class.
 */
class SleepAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                showBedtimeSummary(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun showBedtimeSummary(context: Context) {
        val db = AppDatabase.getInstance(context)
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowDayOfWeek = tomorrow.dayOfWeek.value // 1 = Mon, 7 = Sun

        val allCourses = db.courseDao().getAllCourses().first()
        val tomorrowCourses = allCourses
            .filter { it.dayOfWeek == tomorrowDayOfWeek }
            .sortedBy { it.startPeriod }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sleep & Bedtime Alarms",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nightly summary of tomorrow's schedule and wake-up preparation"
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            9001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val body: String

        if (tomorrowCourses.isNotEmpty()) {
            val first = tomorrowCourses.first()
            val time = first.startTime.ifBlank { "Section ${first.startPeriod}" }
            val room = if (first.classroom.isNotBlank()) " in ${first.classroom}" else ""
            title = "🌙 Tomorrow's First Class: ${first.name}"
            body = "Starts at $time$room. ${tomorrowCourses.size} total classes scheduled tomorrow. Sleep well!"
        } else {
            title = "🌙 No Classes Tomorrow!"
            body = "You have a free day tomorrow. Rest well and enjoy your schedule!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "sleep_bedtime_channel"
        const val NOTIFICATION_ID = 8888
    }
}
