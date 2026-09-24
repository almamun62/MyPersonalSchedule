package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.data.model.HolidayOverrideType
import com.example.domain.ScheduleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * WorkManager worker that queries the local Room database to trigger
 * a system notification 15 minutes before an upcoming class.
 */
class ClassReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        Log.d(TAG, "ClassReminderWorker executing...")

        try {
            val prefs = UserPreferencesManager.getInstance(context)
            val is15mEnabled = prefs.isClassReminder15mEnabled.value
            if (!is15mEnabled) {
                Log.d(TAG, "Class reminder is disabled in user preferences. Skipping notification.")
                return@withContext Result.success()
            }

            val database = AppDatabase.getInstance(context)
            val today = LocalDate.now()

            // 1. Check if today is marked as a holiday in Room database
            val holidayOverride = database.holidayOverrideDao().getOverrideForDate(today.toString())
            if (holidayOverride != null && holidayOverride.type == HolidayOverrideType.HOLIDAY) {
                Log.i(TAG, "Today is an official holiday (${holidayOverride.name}) in Room DB, skipping notification.")
                return@withContext Result.success()
            }

            val specificCourseId = inputData.getLong(EXTRA_COURSE_ID, -1L)

            if (specificCourseId > 0L) {
                // Fetch up-to-date course entity directly from Room database
                val dbCourse = database.courseDao().getCourseById(specificCourseId)
                if (dbCourse == null) {
                    Log.w(TAG, "Course ID $specificCourseId not found in Room database (may have been deleted).")
                    return@withContext Result.success()
                }

                val courseName = dbCourse.name.ifBlank { inputData.getString(EXTRA_COURSE_NAME) ?: "Upcoming Class" }
                val classroom = dbCourse.classroom.ifBlank { inputData.getString(EXTRA_CLASSROOM) ?: "Campus Classroom" }
                val startTime = dbCourse.startTime.ifBlank { inputData.getString(EXTRA_START_TIME) ?: "" }
                val endTime = dbCourse.endTime.ifBlank { inputData.getString(EXTRA_END_TIME) ?: "" }
                val instructor = dbCourse.instructor.ifBlank { inputData.getString(EXTRA_INSTRUCTOR) ?: "" }
                val notes = dbCourse.notes.ifBlank { inputData.getString(EXTRA_NOTES) ?: "" }

                Log.i(TAG, "Triggering 15m notification via WorkManager for: $courseName at $classroom")
                withContext(Dispatchers.Main) {
                    CourseNotificationManager.show15MinuteClassReminder(
                        context = context,
                        courseId = dbCourse.id,
                        courseName = courseName,
                        classroom = classroom,
                        startTime = startTime,
                        endTime = endTime,
                        instructor = instructor,
                        notes = notes
                    )
                }
            } else {
                // Periodic or general check: scan Room database for any active course starting within the 15-minute window
                val activeSemester = database.semesterDao().getActiveSemesterSync()
                if (activeSemester == null) {
                    Log.d(TAG, "No active semester found in Room database.")
                    return@withContext Result.success()
                }

                val allCourses = database.courseDao().getCoursesBySemesterSync(activeSemester.id)
                val allOverrides = database.holidayOverrideDao().getAllHolidayOverridesSync()
                val semStartDate = LocalDate.ofEpochDay(activeSemester.startDateMillis / (24 * 60 * 60 * 1000))

                val dayResolution = ScheduleEngine.resolveDaySchedule(
                    targetDate = today,
                    semesterStartDate = semStartDate,
                    totalWeeks = activeSemester.totalWeeks,
                    allCourses = allCourses,
                    holidayOverrides = allOverrides
                )

                if (dayResolution.isHoliday) {
                    Log.d(TAG, "Day is resolved as holiday: ${dayResolution.holidayName}")
                    return@withContext Result.success()
                }

                val now = LocalTime.now()
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val globalAdvanceMinutes = prefs.classReminderMinutes.value.coerceAtLeast(1)

                for (course in dayResolution.activeCourses) {
                    val courseAdvance = prefs.getCourseReminderMinutes(course.name) ?: globalAdvanceMinutes
                    if (courseAdvance <= 0) {
                        // Muted/disabled for this course
                        continue
                    }

                    val startTime = try {
                        LocalTime.parse(course.startTime, timeFormatter)
                    } catch (e: Exception) {
                        continue
                    }

                    val minutesUntilStart = java.time.Duration.between(now, startTime).toMinutes()
                    // If class starts within window of the alert time
                    if (minutesUntilStart in (courseAdvance - 4)..(courseAdvance + 4)) {
                        Log.i(TAG, "Found upcoming class '${course.name}' starting in $minutesUntilStart minutes. Triggering notification.")
                        withContext(Dispatchers.Main) {
                            CourseNotificationManager.show15MinuteClassReminder(
                                context = context,
                                courseId = course.id,
                                courseName = course.name,
                                classroom = course.classroom.ifBlank { "Campus Classroom" },
                                startTime = course.startTime,
                                endTime = course.endTime,
                                instructor = course.instructor,
                                notes = course.notes
                            )
                        }
                    }
                }

                // Keep future one-time WorkManager requests refreshed
                ClassReminderScheduler.scheduleUpcomingAlarms(context)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ClassReminderWorker execution", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "ClassReminderWorker"

        const val EXTRA_COURSE_ID = "EXTRA_COURSE_ID"
        const val EXTRA_COURSE_NAME = "EXTRA_COURSE_NAME"
        const val EXTRA_CLASSROOM = "EXTRA_CLASSROOM"
        const val EXTRA_START_TIME = "EXTRA_START_TIME"
        const val EXTRA_END_TIME = "EXTRA_END_TIME"
        const val EXTRA_INSTRUCTOR = "EXTRA_INSTRUCTOR"
        const val EXTRA_NOTES = "EXTRA_NOTES"
    }
}
