package com.example.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.SectionTiming
import com.example.domain.model.Course
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class HyperIslandState {
    IDLE,
    ACTIVE,
    URGENT
}

data class HyperIslandStatus(
    val state: HyperIslandState,
    val title: String,
    val subtitle: String,
    val timeBadge: String,
    val room: String = "",
    val courseName: String = "",
    val progress: Float = 0f,
    val course: Course? = null
)

object HyperIslandManager {
    const val CHANNEL_ID = "hyperisland_live_status"
    const val NOTIFICATION_ID = 9001

    private fun parseTimeToMinutes(timeStr: String): Int {
        val clean = timeStr.trim()
        val parts = clean.split(":")
        if (parts.size >= 2) {
            val h = parts[0].trim().toIntOrNull() ?: 0
            val m = parts[1].trim().toIntOrNull() ?: 0
            return h * 60 + m
        }
        return 0
    }

    fun calculateStatus(
        todayCourses: List<Course>,
        timings: List<SectionTiming>,
        nowTime: LocalTime = LocalTime.now()
    ): HyperIslandStatus {
        if (todayCourses.isEmpty()) {
            return HyperIslandStatus(
                state = HyperIslandState.IDLE,
                title = "No Classes Today",
                subtitle = "Enjoy your free day or prepare ahead",
                timeBadge = "Free",
                progress = 0f
            )
        }

        val currentMin = nowTime.hour * 60 + nowTime.minute

        // 1. Check for Active / In-Class course
        for (course in todayCourses) {
            val sMin = parseTimeToMinutes(course.startTime)
            val eMin = parseTimeToMinutes(course.endTime)

            if (currentMin in sMin..eMin) {
                val totalDuration = (eMin - sMin).coerceAtLeast(1)
                val elapsed = (currentMin - sMin).coerceAtLeast(0)
                val remaining = (eMin - currentMin).coerceAtLeast(0)
                val prog = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                return HyperIslandStatus(
                    state = HyperIslandState.ACTIVE,
                    title = "In Class: ${course.name}",
                    subtitle = if (course.classroom.isNotBlank()) "Room: ${course.classroom}" else "Active lecture",
                    timeBadge = "${remaining}m left",
                    room = course.classroom,
                    courseName = course.name,
                    progress = prog,
                    course = course
                )
            }
        }

        // 2. Check for Upcoming course today
        val sortedUpcoming = todayCourses
            .map { it to parseTimeToMinutes(it.startTime) }
            .filter { it.second > currentMin }
            .sortedBy { it.second }

        val nextEntry = sortedUpcoming.firstOrNull()
        if (nextEntry != null) {
            val (nextCourse, sMin) = nextEntry
            val diffMin = sMin - currentMin

            if (diffMin <= 15) {
                return HyperIslandStatus(
                    state = HyperIslandState.URGENT,
                    title = "Starting in ${diffMin}m: ${nextCourse.name}",
                    subtitle = if (nextCourse.classroom.isNotBlank()) "Room: ${nextCourse.classroom}" else "Class starts soon",
                    timeBadge = "${diffMin}m",
                    room = nextCourse.classroom,
                    courseName = nextCourse.name,
                    progress = 1f - (diffMin.toFloat() / 15f),
                    course = nextCourse
                )
            } else {
                return HyperIslandStatus(
                    state = HyperIslandState.IDLE,
                    title = "Next: ${nextCourse.name}",
                    subtitle = "${nextCourse.startTime} • ${if (nextCourse.classroom.isNotBlank()) nextCourse.classroom else "Classroom"}",
                    timeBadge = nextCourse.startTime,
                    room = nextCourse.classroom,
                    courseName = nextCourse.name,
                    progress = 0f,
                    course = nextCourse
                )
            }
        }

        // 3. All classes today finished
        return HyperIslandStatus(
            state = HyperIslandState.IDLE,
            title = "All Classes Done",
            subtitle = "Rest, review notes, or check tomorrow's tasks",
            timeBadge = "Done",
            progress = 1f
        )
    }

    fun updateLiveNotification(context: Context, status: HyperIslandStatus) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "HyperIsland Live Status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Glanceable live class status for HyperIsland and Lock Screen"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val iconRes = android.R.drawable.ic_lock_idle_alarm

            val calcTicker = CalculatorMemoryManager.lastResultTicker.value
            val contentSubtext = if (!calcTicker.isNullOrBlank()) {
                "${status.timeBadge} • 🧮 $calcTicker"
            } else {
                status.timeBadge
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(status.title)
                .setContentText(if (!calcTicker.isNullOrBlank()) "${status.subtitle} | Calc: $calcTicker" else status.subtitle)
                .setSubText(contentSubtext)
                .setContentIntent(pendingIntent)
                .setOngoing(status.state == HyperIslandState.ACTIVE)
                .setOnlyAlertOnce(true)
                .setPriority(if (status.state == HyperIslandState.URGENT) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
