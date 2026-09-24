package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.data.model.CourseEntity
import com.example.domain.ScheduleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object ClassReminderScheduler {
    private const val TAG = "ClassReminderScheduler"
    const val ACTION_TRIGGER_CLASS_REMINDER = "com.example.ACTION_TRIGGER_CLASS_REMINDER"
    
    // For manual triggers
    const val EXTRA_COURSE_ID = "EXTRA_COURSE_ID"
    const val EXTRA_COURSE_NAME = "EXTRA_COURSE_NAME"
    const val EXTRA_CLASSROOM = "EXTRA_CLASSROOM"
    const val EXTRA_START_TIME = "EXTRA_START_TIME"
    const val EXTRA_END_TIME = "EXTRA_END_TIME"
    const val EXTRA_INSTRUCTOR = "EXTRA_INSTRUCTOR"
    const val EXTRA_NOTES = "EXTRA_NOTES"

    data class ScheduledReminderInfo(
        val courseId: Long,
        val courseName: String,
        val classroom: String,
        val date: LocalDate,
        val startTime: String,
        val reminderTimeMillis: Long
    )

    /**
     * Queries the Room database for the active semester, courses, and holiday overrides,
     * and schedules WorkManager workers to trigger class reminders for the next 7 days.
     */
    suspend fun scheduleUpcomingAlarms(context: Context): List<ScheduledReminderInfo> = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val activeSemester = database.semesterDao().getActiveSemesterSync() ?: run {
            Log.d(TAG, "No active semester found in Room database, skipping alarm scheduling")
            return@withContext emptyList()
        }
        val courses = database.courseDao().getCoursesBySemesterSync(activeSemester.id)
        val holidayOverrides = database.holidayOverrideDao().getAllHolidayOverridesSync()

        if (courses.isEmpty()) {
            Log.d(TAG, "No courses found for semester ${activeSemester.name}")
            return@withContext emptyList()
        }

        val prefs = UserPreferencesManager.getInstance(context)
        val advanceMinutes = prefs.classReminderMinutes.value

        val today = LocalDate.now()
        val semStartDate = LocalDate.ofEpochDay(activeSemester.startDateMillis / (24 * 60 * 60 * 1000))
        val totalWeeks = activeSemester.totalWeeks
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        
        try {
            val workManager = WorkManager.getInstance(context)
            val scheduledList = mutableListOf<ScheduledReminderInfo>()
            val currentTimeMillis = System.currentTimeMillis()

            // Cancel previously scheduled work
            workManager.cancelAllWorkByTag("class_reminder_work")

            // Look ahead 7 days
            for (dayOffset in 0..7) {
                val targetDate = today.plusDays(dayOffset.toLong())
                val dayResolution = ScheduleEngine.resolveDaySchedule(
                    targetDate = targetDate,
                    semesterStartDate = semStartDate,
                    totalWeeks = totalWeeks,
                    allCourses = courses,
                    holidayOverrides = holidayOverrides
                )

                // Chinese holiday auto-pause: do not remind on official holiday dates
                if (dayResolution.isHoliday) {
                    continue
                }

                for (course in dayResolution.activeCourses) {
                    val startTime = try {
                        LocalTime.parse(course.startTime, timeFormatter)
                    } catch (e: Exception) {
                        continue
                    }
                    
                    val courseAdvance = prefs.getCourseReminderMinutes(course.name) ?: advanceMinutes
                    if (courseAdvance <= 0) {
                        // Reminder disabled course-wise
                        continue
                    }

                    val classDateTime = LocalDateTime.of(targetDate, startTime)
                    val reminderDateTime = classDateTime.minusMinutes(courseAdvance.toLong())
                    val reminderMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    // Only schedule future reminders
                    if (reminderMillis > currentTimeMillis) {
                        val delayMillis = reminderMillis - currentTimeMillis
                        
                        val inputData = Data.Builder()
                            .putLong(ClassReminderWorker.EXTRA_COURSE_ID, course.id)
                            .putString(ClassReminderWorker.EXTRA_COURSE_NAME, course.name)
                            .putString(ClassReminderWorker.EXTRA_CLASSROOM, course.classroom)
                            .putString(ClassReminderWorker.EXTRA_START_TIME, course.startTime)
                            .putString(ClassReminderWorker.EXTRA_END_TIME, course.endTime)
                            .putString(ClassReminderWorker.EXTRA_INSTRUCTOR, course.instructor)
                            .putString(ClassReminderWorker.EXTRA_NOTES, course.notes)
                            .build()
                            
                        val workRequest = OneTimeWorkRequestBuilder<ClassReminderWorker>()
                            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                            .setInputData(inputData)
                            .addTag("class_reminder_work")
                            .build()
                            
                        // Unique work name for this specific class occurrence
                        val workName = "reminder_${course.id}_${targetDate}"
                        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, workRequest)

                        scheduledList.add(
                            ScheduledReminderInfo(
                                courseId = course.id,
                                courseName = course.name,
                                classroom = course.classroom,
                                date = targetDate,
                                startTime = course.startTime,
                                reminderTimeMillis = reminderMillis
                            )
                        )
                    }
                }
            }

            Log.i(TAG, "Scheduled ${scheduledList.size} class reminders using WorkManager")
            
            // Also ensure periodic guardian check is enqueued in WorkManager
            schedulePeriodicClassCheck(context)
            
            return@withContext scheduledList
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule with WorkManager", e)
            return@withContext emptyList()
        }
    }

    /**
     * Enqueues a repeating WorkManager task that checks the Room database every 15 minutes
     * to ensure upcoming class alerts are triggered even across reboots or app standby.
     */
    fun schedulePeriodicClassCheck(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            val periodicRequest = androidx.work.PeriodicWorkRequestBuilder<ClassReminderWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .addTag("periodic_class_reminder_work")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "periodic_class_reminder_work",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Periodic 15-minute Room DB class reminder worker enqueued")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic check", e)
        }
    }

    /**
     * Cancels all scheduled class reminder WorkManager tasks.
     */
    fun cancelAllReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("class_reminder_work")
        workManager.cancelUniqueWork("periodic_class_reminder_work")
        Log.i(TAG, "Cancelled all class reminder workers in WorkManager")
    }

    /**
     * Immediately triggers a class reminder notification via WorkManager by querying
     * the next upcoming or active course from the Room database.
     */
    suspend fun triggerTest15MinuteReminder(context: Context): CourseEntity? = triggerTestReminderFromRoom(context)

    suspend fun triggerTestReminderFromRoom(context: Context): CourseEntity? = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val activeSemester = database.semesterDao().getActiveSemesterSync()
        val allCourses = if (activeSemester != null) {
            database.courseDao().getCoursesBySemesterSync(activeSemester.id)
        } else {
            emptyList()
        }

        val targetCourse: CourseEntity = allCourses.firstOrNull { it.classroom.isNotBlank() }
            ?: allCourses.firstOrNull()
            ?: CourseEntity(
                id = 9999L,
                semesterId = activeSemester?.id ?: 1L,
                name = "Embedded Systems & IoT Lab",
                code = "CS-402",
                classroom = "Science Building Room 304",
                instructor = "Prof. Zhang",
                dayOfWeek = 2,
                startPeriod = 3,
                endPeriod = 4,
                startTime = "10:00",
                endTime = "11:40",
                notes = "Hardware kits provided in Room 304"
            )

        // Enqueue WorkManager task to execute worker with the course from Room
        val inputData = Data.Builder()
            .putLong(ClassReminderWorker.EXTRA_COURSE_ID, targetCourse.id)
            .putString(ClassReminderWorker.EXTRA_COURSE_NAME, targetCourse.name)
            .putString(ClassReminderWorker.EXTRA_CLASSROOM, targetCourse.classroom)
            .putString(ClassReminderWorker.EXTRA_START_TIME, targetCourse.startTime)
            .putString(ClassReminderWorker.EXTRA_END_TIME, targetCourse.endTime)
            .putString(ClassReminderWorker.EXTRA_INSTRUCTOR, targetCourse.instructor)
            .putString(ClassReminderWorker.EXTRA_NOTES, targetCourse.notes)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ClassReminderWorker>()
            .setInputData(inputData)
            .addTag("class_reminder_test")
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)

        withContext(Dispatchers.Main) {
            CourseNotificationManager.show15MinuteClassReminder(
                context = context,
                courseId = targetCourse.id,
                courseName = targetCourse.name,
                classroom = targetCourse.classroom.ifBlank { "Campus Hall B" },
                startTime = targetCourse.startTime.ifBlank { "10:00" },
                endTime = targetCourse.endTime.ifBlank { "11:40" },
                instructor = targetCourse.instructor,
                notes = targetCourse.notes
            )
        }

        return@withContext targetCourse
    }
}
