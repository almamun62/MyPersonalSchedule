package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CourseScheduleWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_course_schedule)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        scope.launch {
            try {
                val db = AppDatabase.getInstance(context)
                val activeSemester = db.semesterDao().getActiveSemesterSync()
                val activeSemesterId = activeSemester?.id ?: 1L
                val courses = db.courseDao().getCoursesBySemesterSync(activeSemesterId)

                val now = LocalTime.now()
                val todayDayOfWeek = LocalDate.now().dayOfWeek.value
                val todayFormatter = DateTimeFormatter.ofPattern("M/d/yy")
                views.setTextViewText(R.id.widget_date, LocalDate.now().format(todayFormatter))

                val todayCourses = courses.filter { it.dayOfWeek == todayDayOfWeek }.sortedBy { it.startTime }

                var currentTitle = "No ongoing class"
                var nextTitle = "No more upcoming classes today"

                for (course in todayCourses) {
                    try {
                        val start = LocalTime.parse(course.startTime)
                        val end = LocalTime.parse(course.endTime)
                        if (!now.isBefore(start) && !now.isAfter(end)) {
                            val remainingMins = java.time.Duration.between(now, end).toMinutes().toInt()
                            currentTitle = "Now: ${course.name} (${remainingMins}m left)"
                        } else if (now.isBefore(start)) {
                            nextTitle = "Next: ${course.name} at ${course.startTime} (${course.classroom})"
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                views.setTextViewText(R.id.widget_current_class, currentTitle)
                views.setTextViewText(R.id.widget_next_class, nextTitle)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        job.cancel()
    }
}
