package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.data.model.*
import com.example.data.repository.CourseRepository
import com.example.data.repository.ScheduleRepository
import com.example.domain.*
import com.example.domain.model.Course
import com.example.service.ClassReminderScheduler
import com.example.service.CourseNotificationManager
import com.example.service.DndAutomationScheduler
import com.example.service.DndManager
import com.example.service.FocusLockService
import com.example.service.SleepAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ScheduleUiState(
    val activeSemester: SemesterEntity? = null,
    val currentAcademicWeek: Int = 1,
    val semesterProgressPercent: Float = 0f,
    val upcomingHoliday: HolidayOverrideEntity? = null,
    val courses: List<CourseEntity> = emptyList(),
    val todayCoursesWithStatus: List<CourseWithStatus> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val pendingTasks: List<TaskEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val allNotes: List<NoteEntity> = emptyList(),
    val holidayOverrides: List<HolidayOverrideEntity> = emptyList(),
    val isAutoDndEnabled: Boolean = true,
    val isDndPermissionGranted: Boolean = false,
    val isDndActive: Boolean = false,
    val isClassReminder15mEnabled: Boolean = true,
    val classReminderMinutes: Int = 15,
    val scheduledRemindersCount: Int = 0,
    val selectedWeekConflicts: List<CourseConflict> = emptyList(),
    val preferredConflictCourseIds: Set<Long> = emptySet(),
    val timeBlockPermanentPreferences: Map<String, Long> = emptyMap(),
    val sessionCoursePriorities: Map<String, Long> = emptyMap(),
    val hideAuditedInConflict: Boolean = false,
    val todayResolution: DayResolutionResult? = null,
    val tomorrowResolution: DayResolutionResult? = null,
    val weeklySummary: WeeklySummaryReport? = null,
    val calendarImageUri: String? = null,
    val weeklyClassHours: Float = 0f,
    val todayClassHours: Float = 0f,
    val totalCredits: Float = 0f,
    val courseCreditsMap: Map<String, Float> = emptyMap(),
    val isSleepAlarmEnabled: Boolean = false,
    val sleepBedtime: String = "23:00",
    val sleepWakeTime: String = "07:00",
    val sleepSmartWakeEnabled: Boolean = true,
    val sleepWakeAdvanceMinutes: Int = 60,
    val sleepTargetHours: Float = 8.0f,
    val sleepAlarmVibrate: Boolean = true,
    val sleepAlarmSound: Boolean = true,
    val courseReminderMinutes: Map<String, Int> = emptyMap(),
    val focusWhitelistedPackages: Set<String> = emptySet(),
    val focusLockEndTimeMillis: Long = 0L,
    val focusLockDurationSeconds: Long = 1500L,
    val focusLockCourseName: String = ""
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getInstance(application)
    val repository = ScheduleRepository(database)
    val courseRepository = CourseRepository(database.courseDao())
    val userPreferencesManager = UserPreferencesManager.getInstance(application)

    private val _isDndPermissionGranted = MutableStateFlow(DndManager.checkDndPermission(application))
    private val _isDndActive = MutableStateFlow(DndManager.isDndActive(application))
    private val _scheduledRemindersCount = MutableStateFlow(0)

    private data class CoreData(
        val semester: SemesterEntity?,
        val tasks: List<TaskEntity>,
        val pendingTasks: List<TaskEntity>,
        val exams: List<ExamEntity>,
        val notes: List<NoteEntity>
    )

    private data class ScheduleData(
        val courses: List<CourseEntity>,
        val holidays: List<HolidayOverrideEntity>,
        val remindersCount: Int,
        val dndGranted: Boolean,
        val dndActive: Boolean
    )

    private data class SleepPrefsData(
        val enabled: Boolean,
        val bedtime: String,
        val wakeTime: String,
        val smartWake: Boolean,
        val advanceMin: Int,
        val targetHours: Float,
        val vibrate: Boolean,
        val sound: Boolean
    )

    private data class CoursePrefsPartial(
        val prefIds: Set<Long>,
        val sessMap: Map<String, Long>,
        val blockMap: Map<String, Long>,
        val creditsMap: Map<String, Float>,
        val remMap: Map<String, Int>
    )

    private data class CoursePrefs(
        val prefIds: Set<Long>,
        val sessMap: Map<String, Long>,
        val blockMap: Map<String, Long>,
        val creditsMap: Map<String, Float>,
        val remMap: Map<String, Int>,
        val whitelist: Set<String>
    )

    private data class PrefsData(
        val autoDnd: Boolean,
        val reminder15m: Boolean,
        val reminderMin: Int,
        val hideAudited: Boolean,
        val calUri: String?,
        val preferredConflictCourseIds: Set<Long>,
        val sessionCoursePriorities: Map<String, Long>,
        val timeBlockPermanentPreferences: Map<String, Long>,
        val courseCredits: Map<String, Float>,
        val courseReminderMinutes: Map<String, Int>,
        val focusWhitelistedPackages: Set<String>,
        val sleepPrefs: SleepPrefsData,
        val focusLockEndTime: Long,
        val focusLockDuration: Long,
        val focusLockCourse: String
    )

    private val coreDataFlow: Flow<CoreData> = combine(
        repository.activeSemester,
        repository.allTasks,
        repository.pendingTasks,
        database.examDao().getAllExams(),
        repository.allNotes
    ) { sem, tasks, pendingTasks, exams, notes ->
        CoreData(sem, tasks, pendingTasks, exams, notes)
    }

    private val scheduleDataFlow: Flow<ScheduleData> = combine(
        database.courseDao().getAllCourses(),
        repository.allHolidayOverrides,
        _scheduledRemindersCount,
        _isDndPermissionGranted,
        _isDndActive
    ) { courses, holidays, remCount, dndGranted, dndActive ->
        ScheduleData(courses, holidays, remCount, dndGranted, dndActive)
    }

    private val sleepPrefsFlow: Flow<SleepPrefsData> = combine(
        combine(
            userPreferencesManager.isSleepAlarmEnabled,
            userPreferencesManager.sleepBedtime,
            userPreferencesManager.sleepWakeTime,
            userPreferencesManager.isSleepSmartWakeEnabled
        ) { enabled, bedtime, wakeTime, smartWake ->
            SleepPrefsData(
                enabled = enabled,
                bedtime = bedtime,
                wakeTime = wakeTime,
                smartWake = smartWake,
                advanceMin = 60,
                targetHours = 8.0f,
                vibrate = true,
                sound = true
            )
        },
        combine(
            userPreferencesManager.sleepWakeAdvanceMinutes,
            userPreferencesManager.sleepTargetHours,
            userPreferencesManager.isSleepAlarmVibrate,
            userPreferencesManager.isSleepAlarmSound
        ) { advanceMin, targetHours, vibrate, sound ->
            Triple(advanceMin, targetHours, vibrate to sound)
        }
    ) { base, extra ->
        base.copy(
            advanceMin = extra.first,
            targetHours = extra.second,
            vibrate = extra.third.first,
            sound = extra.third.second
        )
    }

    private val prefsDataFlow: Flow<PrefsData> = combine(
        combine(
            userPreferencesManager.isAutoDndEnabled,
            userPreferencesManager.isClassReminder15mEnabled,
            userPreferencesManager.classReminderMinutes,
            userPreferencesManager.hideAuditedInConflict,
            userPreferencesManager.calendarImageUri
        ) { autoDnd, rem15m, remMin, hideAud, calUri ->
            Pair(autoDnd to rem15m, Triple(remMin, hideAud, calUri))
        },
        combine(
            combine(
                userPreferencesManager.preferredConflictCourseIds,
                userPreferencesManager.sessionCoursePriorities,
                userPreferencesManager.timeBlockPermanentPreferences,
                userPreferencesManager.courseCreditsMap,
                userPreferencesManager.courseReminderMinutes
            ) { prefIds, sessMap, blockMap, creditsMap, remMap ->
                CoursePrefsPartial(prefIds, sessMap, blockMap, creditsMap, remMap)
            },
            userPreferencesManager.focusWhitelistedPackages
        ) { cp, whitelist ->
            CoursePrefs(cp.prefIds, cp.sessMap, cp.blockMap, cp.creditsMap, cp.remMap, whitelist)
        },
        sleepPrefsFlow,
        combine(
            userPreferencesManager.focusLockEndTimeMillis,
            userPreferencesManager.focusLockDurationSeconds,
            userPreferencesManager.focusLockCourseName
        ) { endTime, duration, courseName ->
            Triple(endTime, duration, courseName)
        }
    ) { (g1, g2), cp, sleepData, focusData ->
        PrefsData(
            autoDnd = g1.first,
            reminder15m = g1.second,
            reminderMin = g2.first,
            hideAudited = g2.second,
            calUri = g2.third,
            preferredConflictCourseIds = cp.prefIds,
            sessionCoursePriorities = cp.sessMap,
            timeBlockPermanentPreferences = cp.blockMap,
            courseCredits = cp.creditsMap,
            courseReminderMinutes = cp.remMap,
            focusWhitelistedPackages = cp.whitelist,
            sleepPrefs = sleepData,
            focusLockEndTime = focusData.first,
            focusLockDuration = focusData.second,
            focusLockCourse = focusData.third
        )
    }

    val uiState: StateFlow<ScheduleUiState> = combine(
        coreDataFlow,
        scheduleDataFlow,
        prefsDataFlow
    ) { core, sched, prefs ->
        val sem = core.semester
        val semStartDate = if (sem != null) {
            Instant.ofEpochMilli(sem.startDateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } else {
            LocalDate.now()
        }
        val totalWeeks = sem?.totalWeeks ?: 18
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val currentAcademicWeek = ScheduleEngine.calculateAcademicWeek(semStartDate, today).coerceIn(1, totalWeeks)
        val totalDays = totalWeeks * 7L
        val elapsedDays = ChronoUnit.DAYS.between(semStartDate, today).coerceAtLeast(0)
        val progress = if (totalDays > 0) (elapsedDays.toFloat() / totalDays).coerceIn(0f, 1f) else 0f

        val upcomingHoliday = sched.holidays.filter {
            try {
                LocalDate.parse(it.dateString) >= today
            } catch (e: Exception) {
                false
            }
        }.minByOrNull { it.dateString }

        val todayResolution = ScheduleEngine.resolveDaySchedule(today, semStartDate, totalWeeks, sched.courses, sched.holidays)
        val tomorrowResolution = ScheduleEngine.resolveDaySchedule(tomorrow, semStartDate, totalWeeks, sched.courses, sched.holidays)
        val todayCoursesWithStatus = ScheduleEngine.resolveCourseStatuses(todayResolution.activeCourses, LocalTime.now())
        val selectedWeekConflicts = CourseConflictDetector.detectConflicts(sched.courses, totalWeeks)
        val weeklySummary = WeeklySummaryEngine.computeSummaryReport(sched.courses, core.tasks, currentAcademicWeek, totalWeeks, semStartDate)

        val distinctCourseNames = sched.courses.map { it.name }.filter { it.isNotBlank() }.distinct()
        val totalCredits = distinctCourseNames.sumOf { name ->
            (prefs.courseCredits[name] ?: 3.0f).toDouble()
        }.toFloat()

        fun calcCourseHours(c: CourseEntity): Float {
            if (c.startTime.isNotBlank() && c.endTime.isNotBlank()) {
                try {
                    val sParts = c.startTime.split(":")
                    val eParts = c.endTime.split(":")
                    val sMin = sParts[0].trim().toInt() * 60 + sParts[1].trim().toInt()
                    val eMin = eParts[0].trim().toInt() * 60 + eParts[1].trim().toInt()
                    if (eMin > sMin) {
                        return (eMin - sMin) / 60f
                    }
                } catch (_: Exception) {}
            }
            val span = (c.endPeriod - c.startPeriod + 1).coerceAtLeast(1)
            return span * 0.75f
        }

        val todayHours = todayResolution.activeCourses.sumOf { c ->
            calcCourseHours(c).toDouble()
        }.toFloat()

        val weeklyActiveCourses = sched.courses.filter { c ->
            ScheduleEngine.isCourseActiveInWeek(c.weekRule, c.customWeeks, currentAcademicWeek, sem?.totalWeeks ?: 20)
        }
        val weeklyHours = weeklyActiveCourses.sumOf { c ->
            calcCourseHours(c).toDouble()
        }.toFloat()

        ScheduleUiState(
            activeSemester = sem,
            currentAcademicWeek = currentAcademicWeek,
            semesterProgressPercent = progress,
            upcomingHoliday = upcomingHoliday,
            courses = sched.courses,
            todayCoursesWithStatus = todayCoursesWithStatus,
            tasks = core.tasks,
            pendingTasks = core.pendingTasks,
            exams = core.exams,
            allNotes = core.notes,
            holidayOverrides = sched.holidays,
            isAutoDndEnabled = prefs.autoDnd,
            isDndPermissionGranted = sched.dndGranted,
            isDndActive = sched.dndActive,
            isClassReminder15mEnabled = prefs.reminder15m,
            classReminderMinutes = prefs.reminderMin,
            scheduledRemindersCount = sched.remindersCount,
            selectedWeekConflicts = selectedWeekConflicts,
            preferredConflictCourseIds = prefs.preferredConflictCourseIds,
            timeBlockPermanentPreferences = prefs.timeBlockPermanentPreferences,
            sessionCoursePriorities = prefs.sessionCoursePriorities,
            hideAuditedInConflict = prefs.hideAudited,
            todayResolution = todayResolution,
            tomorrowResolution = tomorrowResolution,
            weeklySummary = weeklySummary,
            calendarImageUri = prefs.calUri,
            weeklyClassHours = weeklyHours,
            todayClassHours = todayHours,
            totalCredits = totalCredits,
            courseCreditsMap = prefs.courseCredits,
            isSleepAlarmEnabled = prefs.sleepPrefs.enabled,
            sleepBedtime = prefs.sleepPrefs.bedtime,
            sleepWakeTime = prefs.sleepPrefs.wakeTime,
            sleepSmartWakeEnabled = prefs.sleepPrefs.smartWake,
            sleepWakeAdvanceMinutes = prefs.sleepPrefs.advanceMin,
            sleepTargetHours = prefs.sleepPrefs.targetHours,
            sleepAlarmVibrate = prefs.sleepPrefs.vibrate,
            sleepAlarmSound = prefs.sleepPrefs.sound,
            courseReminderMinutes = prefs.courseReminderMinutes,
            focusWhitelistedPackages = prefs.focusWhitelistedPackages,
            focusLockEndTimeMillis = prefs.focusLockEndTime,
            focusLockDurationSeconds = prefs.focusLockDuration,
            focusLockCourseName = prefs.focusLockCourse
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ScheduleUiState()
    )

    fun stopFocusLock(context: android.content.Context) {
        userPreferencesManager.stopFocusLock()
        com.example.service.FocusLockService.stop(context)
    }

    fun setClassReminderMinutes(minutes: Int) {
        userPreferencesManager.setClassReminderMinutes(minutes)
        viewModelScope.launch(Dispatchers.IO) {
            val reminders = ClassReminderScheduler.scheduleUpcomingAlarms(getApplication())
            _scheduledRemindersCount.value = reminders.size
        }
    }

    fun setCourseReminderMinutes(courseName: String, minutes: Int?) {
        userPreferencesManager.setCourseReminderMinutes(courseName, minutes)
        viewModelScope.launch(Dispatchers.IO) {
            val reminders = ClassReminderScheduler.scheduleUpcomingAlarms(getApplication())
            _scheduledRemindersCount.value = reminders.size
        }
    }

    fun toggleSleepAlarm(context: Context, enabled: Boolean) {
        userPreferencesManager.setSleepAlarmEnabled(enabled)
        viewModelScope.launch {
            if (enabled) {
                SleepAlarmScheduler.scheduleNextSleepAlarms(context)
            } else {
                SleepAlarmScheduler.cancelSleepAlarms(context)
            }
        }
    }

    fun updateSleepSchedule(
        context: Context,
        enabled: Boolean,
        bedtime: String,
        wakeTime: String,
        smartWake: Boolean,
        advanceMin: Int,
        targetHours: Float,
        vibrate: Boolean,
        sound: Boolean
    ) {
        userPreferencesManager.setSleepAlarmEnabled(enabled)
        userPreferencesManager.setSleepSchedule(
            bedtime = bedtime,
            wakeTime = wakeTime,
            smartWake = smartWake,
            advanceMinutes = advanceMin,
            targetHours = targetHours,
            vibrate = vibrate,
            sound = sound
        )
        viewModelScope.launch {
            if (enabled) {
                SleepAlarmScheduler.scheduleNextSleepAlarms(context)
            } else {
                SleepAlarmScheduler.cancelSleepAlarms(context)
            }
        }
    }

    fun testSleepAlarm(context: Context) {
        SleepAlarmScheduler.triggerImmediateTestAlarm(context)
    }

    fun setAlarmInClockApp(context: Context, hour: Int, minute: Int, message: String): Boolean {
        return SleepAlarmScheduler.setInDeviceClockApp(context, hour, minute, message)
    }

    fun setCourseCredits(courseName: String, credits: Float) {
        userPreferencesManager.setCourseCredit(courseName, credits)
    }

    // Legacy and timetable support
    private val _selectedSemester = MutableStateFlow("Fall 2025")
    val selectedSemester: StateFlow<String> = _selectedSemester.asStateFlow()

    private val _selectedDay = MutableStateFlow(java.time.LocalDate.now().dayOfWeek.value) // 1 = Monday .. 7 = Sunday
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _selectedWeek = MutableStateFlow(4) // default to Week 4 (or calculated academic week)
    val selectedWeek: StateFlow<Int> = _selectedWeek.asStateFlow()

    fun setSelectedWeek(week: Int) {
        _selectedWeek.value = week.coerceIn(1, 25)
    }

    private val _isFullWeekView = MutableStateFlow(false)
    val isFullWeekView: StateFlow<Boolean> = _isFullWeekView.asStateFlow()

    fun toggleFullWeekView() {
        _isFullWeekView.value = !_isFullWeekView.value
    }

    private val _isCompactMode = MutableStateFlow(false)
    val isCompactMode: StateFlow<Boolean> = _isCompactMode.asStateFlow()

    fun toggleCompactMode() {
        _isCompactMode.value = !_isCompactMode.value
    }

    fun toggleThemeMode() {
        val current = userPreferencesManager.themeMode.value
        val newMode = if (current == com.example.ui.theme.AppThemeMode.NIGHT) {
            com.example.ui.theme.AppThemeMode.DAY
        } else {
            com.example.ui.theme.AppThemeMode.NIGHT
        }
        userPreferencesManager.setThemeMode(newMode)
    }

    fun toggleClassReminder15m() {
        val current = userPreferencesManager.isClassReminder15mEnabled.value
        userPreferencesManager.setClassReminder15mEnabled(!current)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val semesters: StateFlow<List<String>> = courseRepository.semesters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Fall 2025"))

    val allCourses: StateFlow<List<Course>> = courseRepository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCourses: StateFlow<List<Course>> = combine(
        allCourses,
        _selectedSemester,
        _searchQuery
    ) { courses, sem, query ->
        courses.filter { course ->
            val matchSemester = sem == "All Semesters" || course.semester == sem
            val matchQuery = query.isBlank() ||
                    course.name.contains(query, ignoreCase = true) ||
                    course.code.contains(query, ignoreCase = true) ||
                    course.instructor.contains(query, ignoreCase = true) ||
                    course.classroom.contains(query, ignoreCase = true)
            matchSemester && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coursesForSelectedDay: StateFlow<List<Course>> = combine(
        filteredCourses,
        _selectedDay
    ) { courses, day ->
        courses.filter { it.dayOfWeek == day }
            .sortedBy { it.startTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
            refreshDndPermission()
            try {
                val reminders = ClassReminderScheduler.scheduleUpcomingAlarms(getApplication())
                _scheduledRemindersCount.value = reminders.size
            } catch (e: Exception) {
                // Ignore background alarm failures during startup
            }
        }
    }

    fun setSelectedSemester(semester: String) {
        _selectedSemester.value = semester
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshDndPermission(activity: Activity? = null) {
        _isDndPermissionGranted.value = DndManager.checkDndPermission(getApplication())
        _isDndActive.value = DndManager.isDndActive(getApplication())
    }

    fun requestDndPermission(activityOrContext: Context? = null) {
        DndManager.requestDndPermission(activityOrContext ?: getApplication())
    }

    fun toggleAutoDnd(enabled: Boolean) {
        userPreferencesManager.setAutoDndEnabled(enabled)
        viewModelScope.launch(Dispatchers.IO) {
            DndAutomationScheduler.scheduleDndAlarms(getApplication())
        }
    }

    fun toggleClassReminder15m(enabled: Boolean) {
        userPreferencesManager.setClassReminder15mEnabled(enabled)
        viewModelScope.launch(Dispatchers.IO) {
            val reminders = ClassReminderScheduler.scheduleUpcomingAlarms(getApplication())
            _scheduledRemindersCount.value = reminders.size
        }
    }

    fun test15MinuteReminder(context: Context, onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val course = ClassReminderScheduler.triggerTest15MinuteReminder(context)
            val name = course?.name ?: "Class Reminder"
            onComplete?.let { cb ->
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    cb(name)
                }
            }
        }
    }

    fun rescheduleClassReminders(context: Context, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val reminders = ClassReminderScheduler.scheduleUpcomingAlarms(context)
            _scheduledRemindersCount.value = reminders.size
            onComplete?.let { cb ->
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    cb(reminders.size)
                }
            }
        }
    }

    fun testStickyNotification(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val courses = repository.getCoursesBySemesterSync(uiState.value.activeSemester?.id ?: 1L)
            val sample = courses.firstOrNull() ?: CourseEntity(
                semesterId = 1L,
                name = "Advanced Systems",
                code = "CS-401",
                classroom = "Hall A-302",
                instructor = "Prof. Zhang",
                dayOfWeek = 1,
                startPeriod = 1,
                endPeriod = 2,
                startTime = "08:00",
                endTime = "09:35",
                notes = "Test Session"
            )
            CourseNotificationManager.showStickyOngoingClassNotification(
                context = context,
                courseName = sample.name,
                classroom = sample.classroom,
                startTime = sample.startTime,
                endTime = sample.endTime,
                instructor = sample.instructor,
                notes = sample.notes
            )
        }
    }

    fun testNagNotification(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val courses = repository.getCoursesBySemesterSync(uiState.value.activeSemester?.id ?: 1L)
            val sample = courses.firstOrNull() ?: CourseEntity(
                semesterId = 1L,
                name = "Advanced Systems",
                code = "CS-401",
                classroom = "Hall A-302",
                instructor = "Prof. Zhang",
                dayOfWeek = 1,
                startPeriod = 1,
                endPeriod = 2,
                startTime = "08:00",
                endTime = "09:35",
                notes = "Test Session"
            )
            CourseNotificationManager.showNagClassNotification(
                context = context,
                courseName = sample.name,
                classroom = sample.classroom,
                startTime = sample.startTime,
                endTime = sample.endTime,
                instructor = sample.instructor,
                nagCount = 1
            )
        }
    }

    fun prioritizeCourseForCurrentSession(
        week: Int,
        dayOfWeek: Int,
        startPeriod: Int,
        prioritized: CourseEntity,
        other: CourseEntity?
    ) {
        userPreferencesManager.setSessionPriority(week, dayOfWeek, startPeriod, prioritized.id)
    }

    fun setTimeBlockPermanentPreference(
        dayOfWeek: Int,
        startPeriod: Int,
        endPeriod: Int,
        prioritized: CourseEntity,
        other: CourseEntity?
    ) {
        userPreferencesManager.setTimeBlockPermanentPreference(dayOfWeek, startPeriod, endPeriod, prioritized.id, other?.id)
    }

    fun clearConflictPreference(conflict: CourseConflict, currentWeek: Int) {
        userPreferencesManager.clearSessionPriority(currentWeek, conflict.dayOfWeek, conflict.overlapStartPeriod)
        userPreferencesManager.clearTimeBlockPermanentPreference(conflict.dayOfWeek, conflict.overlapStartPeriod, conflict.overlapEndPeriod)
        userPreferencesManager.removeCourseFromConflictPreference(conflict.course1.id)
        userPreferencesManager.removeCourseFromConflictPreference(conflict.course2.id)
    }

    fun setHideAuditedInConflict(hide: Boolean) {
        userPreferencesManager.setHideAuditedInConflict(hide)
    }

    fun updateCalendarImageUri(uri: String?) {
        userPreferencesManager.setCalendarImageUri(uri)
    }

    fun addTask(title: String, courseName: String, priority: String, dueDateMillis: Long = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTask(
                TaskEntity(
                    title = title,
                    courseName = courseName,
                    priority = priority,
                    dueDateMillis = if (dueDateMillis > 0) dueDateMillis else System.currentTimeMillis() + 86400000L * 3
                )
            )
        }
    }

    fun addTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTask(task)
        }
    }

    fun toggleTaskCompleted(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setTaskCompleted(taskId, isCompleted)
        }
    }

    fun toggleTaskCompleted(task: TaskEntity, isCompleted: Boolean = !task.isCompleted) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setTaskCompleted(task.id, isCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = repository.allTasks.firstOrNull()?.find { it.id == taskId }
            if (task != null) {
                repository.deleteTask(task)
            }
        }
    }

    fun addExam(exam: ExamEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExam(exam)
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExam(exam)
        }
    }

    fun deleteExam(examId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.examDao().deleteExamById(examId)
        }
    }

    fun addNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.noteDao().insertNote(note)
        }
    }

    fun addNote(courseId: Long, content: String, tags: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.noteDao().insertNote(
                NoteEntity(
                    courseId = courseId,
                    content = content,
                    tags = tags
                )
            )
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.noteDao().updateNote(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.noteDao().deleteNote(note)
        }
    }

    fun getNotesForCourse(courseId: Long): Flow<List<NoteEntity>> {
        return repository.database.noteDao().getNotesForCourse(courseId)
    }

    fun addCourse(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeSemId = repository.getActiveSemesterSync()?.id ?: 1L
            repository.insertCourse(course.toEntity(activeSemId))
        }
    }

    fun addCourse(course: CourseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCourse(course)
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeSemId = repository.getActiveSemesterSync()?.id ?: 1L
            repository.updateCourse(course.toEntity(activeSemId))
        }
    }

    fun updateCourse(course: CourseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCourse(course)
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCourse(course.toEntity())
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCourse(course)
        }
    }

    fun deleteCourseById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.courseDao().deleteCourseById(id)
        }
    }

    fun clearSemesterCourses(semester: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val semId = semester.filter { it.isDigit() }.toLongOrNull() ?: 1L
            repository.clearCoursesBySemester(semId)
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.courseDao().clearAll()
        }
    }

    fun loadPresetMamunSchedule(bilingual: Boolean = true, replaceExisting: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeSem = repository.getActiveSemesterSync()
            val semId = activeSem?.id ?: 1L
            if (replaceExisting) {
                repository.clearCoursesBySemester(semId)
            }
            val courses = com.example.util.ScheduleImportHelper.getMamunFall2026Schedule(
                semesterId = semId,
                autoTranslateToEnglish = true,
                bilingual = bilingual
            )
            repository.insertCourses(courses)
            repository.deduplicateCourses(semId)
        }
    }

    fun importCourseEntities(courses: List<CourseEntity>, replaceExisting: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeSem = repository.getActiveSemesterSync()
            val semId = activeSem?.id ?: 1L
            if (replaceExisting) {
                repository.clearCoursesBySemester(semId)
            }
            val adjusted = courses.map { it.copy(semesterId = semId) }
            repository.insertCourses(adjusted)
            repository.deduplicateCourses(semId)
        }
    }

    fun exportToCsv(): String {
        val courses = allCourses.value
        val sb = StringBuilder()
        sb.append("Course Code,Course Name,Day,Start Time,End Time,Room,Instructor,Semester,Credits\n")
        for (c in courses) {
            sb.append("\"${c.code}\",\"${c.name}\",\"${c.dayName}\",\"${c.startTime}\",\"${c.endTime}\",\"${c.classroom}\",\"${c.instructor}\",\"${c.semester}\",${c.credits}\n")
        }
        return sb.toString()
    }

    fun startFocusLock(context: Context, durationSeconds: Long, courseName: String) {
        userPreferencesManager.startFocusLock(durationSeconds, courseName)
        FocusLockService.start(context)
    }

    fun setFocusWhitelistedPackages(packages: Set<String>) {
        userPreferencesManager.setFocusWhitelistedPackages(packages)
    }
}
