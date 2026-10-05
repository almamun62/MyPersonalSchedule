package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ClassScheduleGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value
        val nowStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val todayCourses = try {
            withContext(Dispatchers.IO) {
                database.courseDao().getCoursesBySemester(1).first()
            }.filter { it.dayOfWeek == dayOfWeek }
                .sortedBy { it.startPeriod }
        } catch (e: Exception) {
            emptyList()
        }

        val activeCourse = todayCourses.find { it.startTime <= nowStr && it.endTime >= nowStr }
        val nextCourse = todayCourses.find { it.startTime > nowStr }
        val displayCourse = activeCourse ?: nextCourse
        val isNowActive = activeCourse != null

        provideContent {
            WidgetContent(
                course = displayCourse,
                isNowActive = isNowActive,
                totalToday = todayCourses.size,
                dayName = today.dayOfWeek.name.take(3)
            )
        }
    }

    @Composable
    private fun WidgetContent(
        course: Course?,
        isNowActive: Boolean,
        totalToday: Int,
        dayName: String
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF0F172A)))
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Widget Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 $dayName Schedule",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "$totalToday classes",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF38BDF8)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Course Card
                if (course != null) {
                    val statusText = if (isNowActive) "🟢 NOW IN SESSION" else "⏰ NEXT CLASS"
                    val statusColor = if (isNowActive) Color(0xFF22C55E) else Color(0xFFF59E0B)

                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .background(ColorProvider(Color(0xFF1E293B)))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = statusText,
                                style = TextStyle(
                                    color = ColorProvider(statusColor),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = GlanceModifier.height(4.dp))

                            Text(
                                text = course.name,
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = GlanceModifier.height(2.dp))

                            Text(
                                text = "Periods ${course.startPeriod}-${course.endPeriod} (${course.startTime} - ${course.endTime})",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFFCBD5E1)),
                                    fontSize = 11.sp
                                )
                            )

                            if (course.classroom.isNotBlank()) {
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = "📍 ${course.classroom}",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFF94A3B8)),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .background(ColorProvider(Color(0xFF1E293B)))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 No More Classes Today",
                                style = TextStyle(
                                    color = ColorProvider(Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = "Tap to view full week timetable",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF94A3B8)),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

class CourseScheduleWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClassScheduleGlanceWidget()
}
