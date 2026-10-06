package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesManager
import com.example.data.model.AcademicCalendarFileEntity
import com.example.data.model.Course
import com.example.data.model.CourseEntity
import com.example.data.model.ExamEntity
import com.example.data.model.Task
import com.example.data.repository.ScheduleRepository
import com.example.domain.model.CourseConflict
import com.example.domain.model.ImportedCourse
import com.example.domain.parser.ScheduleParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    val database = AppDatabase.getInstance(application)
    val userPreferencesManager = UserPreferencesManager.getInstance(application)
    val repository = ScheduleRepository(database)

    val allCourses: StateFlow<List<Course>> = repository.getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExams: StateFlow<List<ExamEntity>> = repository.getAllExams()
        .map { list -> list.distinctBy { it.courseName + "_" + it.startTime } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCalendarFiles: StateFlow<List<AcademicCalendarFileEntity>> = repository.getAllCalendarFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<com.example.data.model.NoteEntity>> = repository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCourseMaterials: StateFlow<List<com.example.data.model.CourseMaterialEntity>> = repository.getAllCourseMaterials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scheduleViewMode = MutableStateFlow(ScheduleViewMode.DAY)
    val scheduleViewMode: StateFlow<ScheduleViewMode> = _scheduleViewMode.asStateFlow()

    private val _targetedWeekStart = MutableStateFlow(1)
    val targetedWeekStart: StateFlow<Int> = _targetedWeekStart.asStateFlow()

    private val _targetedWeekEnd = MutableStateFlow(16)
    val targetedWeekEnd: StateFlow<Int> = _targetedWeekEnd.asStateFlow()


    private val _selectedDay = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _selectedWeek = MutableStateFlow(1)
    val selectedWeek: StateFlow<Int> = _selectedWeek.asStateFlow()

    private val _selectedSemester = MutableStateFlow("Fall 2026")
    val selectedSemester: StateFlow<String> = _selectedSemester.asStateFlow()

    private val _isFullWeekView = MutableStateFlow(false)
    val isFullWeekView: StateFlow<Boolean> = _isFullWeekView.asStateFlow()

    private val _isCompactMode = MutableStateFlow(false)
    val isCompactMode: StateFlow<Boolean> = _isCompactMode.asStateFlow()

    val filteredCourses: StateFlow<List<com.example.domain.model.Course>> = allCourses.map { list ->
        list.map { com.example.domain.model.Course.fromEntity(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val list = allCourses.first()
            if (list.isEmpty()) {
                loadComputerSciencePreset()
            }
            val exams = allExams.first()
            if (exams.isEmpty()) {
                loadPresetExams()
            }
        }
    }

    fun setSelectedDay(day: Int) {
        _selectedDay.value = day
    }

    fun setSelectedWeek(week: Int) {
        _selectedWeek.value = week
    }

    fun toggleFullWeekView() {
        _isFullWeekView.value = !_isFullWeekView.value
    }

    fun toggleCompactMode() {
        _isCompactMode.value = !_isCompactMode.value
    }

    fun toggleClassReminder15m() {
        userPreferencesManager.setClassReminder15mEnabled(!userPreferencesManager.isClassReminder15mEnabled.value)
    }

    fun toggleAutoDnd() {
        val next = !userPreferencesManager.isAutoDndEnabled.value
        userPreferencesManager.setAutoDndEnabled(next)
        viewModelScope.launch {
            com.example.service.DndAutomationScheduler.scheduleAllDndAlarms(getApplication())
        }
    }

    fun rescheduleDndAlarms() {
        viewModelScope.launch {
            com.example.service.DndAutomationScheduler.scheduleAllDndAlarms(getApplication())
        }
    }

    fun addCourse(course: com.example.domain.model.Course) {
        viewModelScope.launch {
            repository.insertCourse(course.toEntity())
            rescheduleDndAlarms()
        }
    }

    fun updateCourse(course: com.example.domain.model.Course) {
        viewModelScope.launch {
            repository.updateCourse(course.toEntity())
            rescheduleDndAlarms()
        }
    }

    fun deleteCourse(course: com.example.domain.model.Course) {
        viewModelScope.launch {
            repository.deleteCourse(course.toEntity())
            rescheduleDndAlarms()
        }
    }


    fun addTask(title: String, courseName: String = "", priority: String = "MEDIUM") {
        viewModelScope.launch {
            repository.insertTask(Task(title = title, courseName = courseName, priority = priority))
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun addExam(courseName: String, classroom: String, dateMillis: Long, sTime: String, eTime: String, seatNumber: String = "") {
        viewModelScope.launch {
            repository.insertExam(
                ExamEntity(
                    courseName = courseName,
                    classroom = classroom,
                    examDateMillis = dateMillis,
                    startTime = sTime,
                    endTime = eTime,
                    seatNumber = seatNumber
                )
            )
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
        }
    }

    fun deleteAllCourses() {
        viewModelScope.launch {
            repository.deleteAllCourses()
        }
    }

    fun loadComputerSciencePreset() {
        viewModelScope.launch {
            repository.deleteAllCourses()
            val imported = ScheduleParser.getComputerSciencePreset()
            val entities = imported.map { it.toCourse("Fall 2026").toEntity() }
            repository.insertCourses(entities)
        }
    }

    fun loadPresetExams() {
        viewModelScope.launch {
            val existing = repository.getAllExams().first()
            if (existing.isNotEmpty()) return@launch
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            repository.insertExam(
                ExamEntity(
                    courseName = "Computer Architecture Final Exam",
                    classroom = "Mingli Hall B105",
                    examDateMillis = now + 12 * dayMs,
                    startTime = "09:00",
                    endTime = "11:00",
                    seatNumber = "Seat #34",
                    notes = "Bring Student ID & 2B Pencils"
                )
            )
            repository.insertExam(
                ExamEntity(
                    courseName = "Database Systems & Application",
                    classroom = "Software Lab 1",
                    examDateMillis = now + 18 * dayMs,
                    startTime = "14:30",
                    endTime = "16:30",
                    seatNumber = "Seat #12",
                    notes = "Open book for SQL chapter 4"
                )
            )
        }
    }

    fun addCalendarFile(file: AcademicCalendarFileEntity) {
        viewModelScope.launch {
            repository.insertCalendarFile(file)
        }
    }

    fun deleteCalendarFile(file: AcademicCalendarFileEntity) {
        viewModelScope.launch {
            try {
                if (file.localPath.isNotEmpty()) {
                    val localF = File(file.localPath)
                    if (localF.exists()) {
                        localF.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteCalendarFile(file)
        }
    }

    fun syncScheduleToSystemCalendar(context: android.content.Context): Int {
        var count = 0
        try {
            val courses = filteredCourses.value
            val contentResolver = context.contentResolver
            val calId = 1L // Primary calendar

            courses.forEach { course ->
                val values = android.content.ContentValues().apply {
                    put(android.provider.CalendarContract.Events.DTSTART, System.currentTimeMillis() + 3600000L)
                    put(android.provider.CalendarContract.Events.DTEND, System.currentTimeMillis() + 7200000L)
                    put(android.provider.CalendarContract.Events.TITLE, "${course.name} (${course.code})")
                    put(android.provider.CalendarContract.Events.DESCRIPTION, "Instructor: ${course.instructor} • Room: ${course.classroom}")
                    put(android.provider.CalendarContract.Events.EVENT_LOCATION, course.classroom)
                    put(android.provider.CalendarContract.Events.CALENDAR_ID, calId)
                    put(android.provider.CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
                    val byDay = when (course.dayOfWeek) {
                        1 -> "MO"; 2 -> "TU"; 3 -> "WE"; 4 -> "TH"; 5 -> "FR"; 6 -> "SA"; else -> "SU"
                    }
                    put(android.provider.CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=$byDay")
                }
                val uri = contentResolver.insert(android.provider.CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) count++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    fun setScheduleViewMode(mode: ScheduleViewMode) {
        _scheduleViewMode.value = mode
    }

    fun setTargetedRange(start: Int, end: Int) {
        _targetedWeekStart.value = start
        _targetedWeekEnd.value = end
    }

    fun insertNote(title: String, content: String, courseId: Long = 0, drawingJson: String? = null) {
        viewModelScope.launch {
            repository.insertNote(
                com.example.data.model.NoteEntity(
                    title = title,
                    content = content,
                    courseId = courseId,
                    drawingDataJson = drawingJson,
                    createdAtMillis = System.currentTimeMillis(),
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteNote(note: com.example.data.model.NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun insertCourseMaterial(material: com.example.data.model.CourseMaterialEntity) {
        viewModelScope.launch {
            repository.insertCourseMaterial(material)
        }
    }

    fun deleteCourseMaterial(material: com.example.data.model.CourseMaterialEntity) {
        viewModelScope.launch {
            repository.deleteCourseMaterial(material)
        }
    }

    fun generateShareCode(): String {
        return com.example.domain.parser.ShareCodeManager.generateShareCode(filteredCourses.value)
    }
}

enum class ScheduleViewMode {
    DAY,
    WEEK_GRID,
    TARGETED_RANGE
}

