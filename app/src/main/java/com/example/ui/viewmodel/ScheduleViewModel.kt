package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ScheduleRepository
import com.example.domain.*
import com.example.service.CourseNotificationManager
import com.example.service.DndManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class ScheduleUiState(
    val activeSemester: SemesterEntity? = null,
    val allSemesters: List<SemesterEntity> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val pendingTasks: List<TaskEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val holidayOverrides: List<HolidayOverrideEntity> = emptyList(),
    val currentAcademicWeek: Int = 1,
    val selectedScheduleWeek: Int = 1,
    val todayResolution: DayResolutionResult? = null,
    val todayCoursesWithStatus: List<CourseWithStatus> = emptyList(),
    val tomorrowResolution: DayResolutionResult? = null,
    val semesterProgressPercent: Float = 0f,
    val upcomingHoliday: HolidayOverrideEntity? = null,
    val isDndPermissionGranted: Boolean = false,
    val isDndActive: Boolean = false,
    val isAutoDndEnabled: Boolean = true,
    val stickyNotificationShown: Boolean = false,
    val quickTaskDialogCourse: String? = null
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScheduleRepository

    private val _selectedWeek = MutableStateFlow(1)
    private val _isAutoDndEnabled = MutableStateFlow(true)
    private val _quickTaskDialogCourse = MutableStateFlow<String?>(null)
    private val _clockTicker = MutableStateFlow(System.currentTimeMillis())

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = ScheduleRepository(database)

        // Initialize repository data if empty
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }

        // Live periodic ticker (updates every 10 seconds for live countdown needle and class status)
        viewModelScope.launch {
            while (isActive) {
                _clockTicker.value = System.currentTimeMillis()
                delay(10000)
            }
        }

        // Combine all data sources to produce reactive ScheduleUiState
        viewModelScope.launch {
            combine(
                repository.activeSemester,
                repository.allSemesters,
                repository.allTasks,
                repository.allHolidayOverrides,
                _selectedWeek,
                _isAutoDndEnabled,
                _quickTaskDialogCourse,
                _clockTicker
            ) { values ->
                val sem = values[0] as? SemesterEntity
                val allSem = values[1] as List<SemesterEntity>
                val tasks = values[2] as List<TaskEntity>
                val holidays = values[3] as List<HolidayOverrideEntity>
                val selWeek = values[4] as Int
                val autoDnd = values[5] as Boolean
                val quickCourse = values[6] as? String
                val ticker = values[7] as Long

                val courses = if (sem != null) repository.getCoursesBySemesterSync(sem.id) else emptyList()
                val exams = if (sem != null) repository.database.examDao().getExamsBySemester(sem.id).firstOrNull() ?: emptyList() else emptyList()

                buildUiState(
                    sem = sem,
                    allSem = allSem,
                    courses = courses,
                    tasks = tasks,
                    exams = exams,
                    holidays = holidays,
                    selectedWeek = selWeek,
                    autoDnd = autoDnd,
                    quickCourse = quickCourse
                )
            }.collect { newState ->
                _uiState.value = newState
                handleAutomationAndNotifications(newState)
            }
        }
    }

    private fun buildUiState(
        sem: SemesterEntity?,
        allSem: List<SemesterEntity>,
        courses: List<CourseEntity>,
        tasks: List<TaskEntity>,
        exams: List<ExamEntity>,
        holidays: List<HolidayOverrideEntity>,
        selectedWeek: Int,
        autoDnd: Boolean,
        quickCourse: String?
    ): ScheduleUiState {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val nowTime = LocalTime.now()

        val semStartDate = if (sem != null) {
            LocalDate.ofEpochDay(sem.startDateMillis / (24 * 60 * 60 * 1000))
        } else {
            today
        }
        val totalWeeks = sem?.totalWeeks ?: 16

        // Dynamic week calculation
        val currentAcademicWeek = ScheduleEngine.calculateAcademicWeek(semStartDate, today).coerceIn(1, totalWeeks)

        // Today resolution (handles Chinese holidays and make-up days)
        val todayResolution = ScheduleEngine.resolveDaySchedule(
            targetDate = today,
            semesterStartDate = semStartDate,
            totalWeeks = totalWeeks,
            allCourses = courses,
            holidayOverrides = holidays
        )

        // Status of today's courses (completed, active, next up with countdown needle)
        val todayWithStatus = ScheduleEngine.resolveCourseStatuses(
            courses = todayResolution.activeCourses,
            currentTime = nowTime
        )

        // Tomorrow resolution
        val tomorrowResolution = ScheduleEngine.resolveDaySchedule(
            targetDate = tomorrow,
            semesterStartDate = semStartDate,
            totalWeeks = totalWeeks,
            allCourses = courses,
            holidayOverrides = holidays
        )

        // Semester progress
        val totalDays = (totalWeeks * 7).toFloat()
        val daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(semStartDate, today).coerceAtLeast(0).toFloat()
        val progress = (daysElapsed / totalDays).coerceIn(0f, 1f)

        // Upcoming holiday override
        val upcomingHoliday = holidays
            .filter { try { LocalDate.parse(it.dateString).isAfter(today.minusDays(1)) } catch (e: Exception) { false } }
            .minByOrNull { it.dateString }

        val context = getApplication<Application>()
        val dndPermGranted = DndManager.checkDndPermission(context)
        val dndActive = DndManager.isDndActive(context)

        return ScheduleUiState(
            activeSemester = sem,
            allSemesters = allSem,
            courses = courses,
            tasks = tasks,
            pendingTasks = tasks.filter { !it.isCompleted },
            exams = exams,
            holidayOverrides = holidays,
            currentAcademicWeek = currentAcademicWeek,
            selectedScheduleWeek = if (selectedWeek == 1 && currentAcademicWeek > 1) currentAcademicWeek else selectedWeek,
            todayResolution = todayResolution,
            todayCoursesWithStatus = todayWithStatus,
            tomorrowResolution = tomorrowResolution,
            semesterProgressPercent = progress,
            upcomingHoliday = upcomingHoliday,
            isDndPermissionGranted = dndPermGranted,
            isDndActive = dndActive,
            isAutoDndEnabled = autoDnd,
            quickTaskDialogCourse = quickCourse
        )
    }

    private var wasInClass = false

    private fun handleAutomationAndNotifications(state: ScheduleUiState) {
        val context = getApplication<Application>()
        val activeCourseStatus = state.todayCoursesWithStatus.find { it.status == CourseClassStatus.ACTIVE }

        if (activeCourseStatus != null) {
            // Currently in class
            wasInClass = true
            if (state.isAutoDndEnabled && state.isDndPermissionGranted && !state.isDndActive) {
                DndManager.setDnd(context, enable = true)
            }
            // Show sticky ongoing notification
            CourseNotificationManager.showStickyOngoingNotification(
                context = context,
                courseName = activeCourseStatus.course.name,
                classroom = activeCourseStatus.course.classroom,
                endTime = activeCourseStatus.course.endTime,
                minutesRemaining = activeCourseStatus.minutesRemaining
            )
        } else {
            // Not in class
            if (wasInClass) {
                wasInClass = false
                // Restore DND if we had auto-enabled it
                if (state.isAutoDndEnabled && state.isDndPermissionGranted && state.isDndActive) {
                    DndManager.setDnd(context, enable = false)
                }
                CourseNotificationManager.dismissStickyNotification(context)
            }
        }
    }

    fun setSelectedScheduleWeek(week: Int) {
        _selectedWeek.value = week
    }

    fun toggleAutoDnd(enabled: Boolean) {
        _isAutoDndEnabled.value = enabled
    }

    fun setQuickTaskCourse(courseName: String?) {
        _quickTaskDialogCourse.value = courseName
    }

    fun toggleDndManual(context: android.content.Context, enable: Boolean) {
        val success = DndManager.setDnd(context, enable)
        if (success) {
            _uiState.update { it.copy(isDndActive = enable) }
        }
    }

    fun requestDndPermission(context: android.content.Context) {
        DndManager.requestDndPermission(context)
    }

    fun testStickyNotification(context: android.content.Context) {
        CourseNotificationManager.showStickyOngoingNotification(
            context = context,
            courseName = "Operating Systems Lab",
            classroom = "Turing 204",
            endTime = "11:40",
            minutesRemaining = 35
        )
    }

    fun testNagNotification(context: android.content.Context) {
        CourseNotificationManager.showNagReminder(
            context = context,
            courseName = "Machine Learning",
            classroom = "Auditorium A",
            startTime = "14:00"
        )
    }

    fun addCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.insertCourse(course)
        }
    }

    fun updateCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.updateCourse(course)
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }

    fun addTask(title: String, courseName: String, priority: String, dueDateMillis: Long = 0) {
        viewModelScope.launch {
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

    fun toggleTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompleted(taskId, completed)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun addExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.insertExam(exam)
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
        }
    }

    fun addHolidayOverride(override: HolidayOverrideEntity) {
        viewModelScope.launch {
            repository.insertHolidayOverride(override)
        }
    }

    fun deleteHolidayOverride(override: HolidayOverrideEntity) {
        viewModelScope.launch {
            repository.deleteHolidayOverride(override)
        }
    }

    fun updateSemester(startDateMillis: Long, totalWeeks: Int) {
        viewModelScope.launch {
            val sem = _uiState.value.activeSemester
            if (sem != null) {
                repository.updateSemester(
                    sem.copy(startDateMillis = startDateMillis, totalWeeks = totalWeeks)
                )
            }
        }
    }
}
