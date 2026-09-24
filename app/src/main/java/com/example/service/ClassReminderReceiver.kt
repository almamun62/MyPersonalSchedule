package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.domain.ScheduleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ClassReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ClassReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive triggered with action: $action")

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                when (action) {
                    ClassReminderScheduler.ACTION_TRIGGER_CLASS_REMINDER -> {
                        val courseId = intent.getLongExtra(ClassReminderScheduler.EXTRA_COURSE_ID, -1L)
                        val fallbackName = intent.getStringExtra(ClassReminderScheduler.EXTRA_COURSE_NAME) ?: "Upcoming Class"
                        val fallbackClassroom = intent.getStringExtra(ClassReminderScheduler.EXTRA_CLASSROOM) ?: "Classroom"
                        val fallbackStartTime = intent.getStringExtra(ClassReminderScheduler.EXTRA_START_TIME) ?: ""
                        val fallbackEndTime = intent.getStringExtra(ClassReminderScheduler.EXTRA_END_TIME) ?: ""

                        val database = AppDatabase.getInstance(context)
                        val today = LocalDate.now()

                        // Check holiday auto-pause
                        val holiday = database.holidayOverrideDao().getOverrideForDate(today.toString())
                        if (holiday != null && holiday.type == com.example.data.model.HolidayOverrideType.HOLIDAY) {
                            Log.i(TAG, "Skipping 15-minute class reminder due to holiday: ${holiday.name}")
                            return@launch
                        }

                        // Query course from Room to ensure latest data (e.g. if room changed or translated)
                        val dbCourse = if (courseId > 0) database.courseDao().getCourseById(courseId) else null

                        val finalCourseName = dbCourse?.name ?: fallbackName
                        val finalClassroom = dbCourse?.classroom ?: fallbackClassroom
                        val finalStartTime = dbCourse?.startTime ?: fallbackStartTime
                        val finalEndTime = dbCourse?.endTime ?: fallbackEndTime
                        val finalInstructor = dbCourse?.instructor ?: ""
                        val finalNotes = dbCourse?.notes ?: ""

                        Log.i(TAG, "Firing 15-minute reminder for '$finalCourseName' at '$finalClassroom'")

                        CourseNotificationManager.show15MinuteClassReminder(
                            context = context,
                            courseId = courseId.coerceAtLeast(1L),
                            courseName = finalCourseName,
                            classroom = finalClassroom,
                            startTime = finalStartTime,
                            endTime = finalEndTime,
                            instructor = finalInstructor,
                            notes = finalNotes
                        )

                        // Keep upcoming alarm schedule up to date
                        ClassReminderScheduler.scheduleUpcomingAlarms(context)
                        com.example.service.DndAutomationScheduler.scheduleDndAlarms(context)
                    }

                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        Log.i(TAG, "System event $action received, rescheduling all class reminders from Room")
                        ClassReminderScheduler.scheduleUpcomingAlarms(context)
                        com.example.service.DndAutomationScheduler.scheduleDndAlarms(context)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling class reminder alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
