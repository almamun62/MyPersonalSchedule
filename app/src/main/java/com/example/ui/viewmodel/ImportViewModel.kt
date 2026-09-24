package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CourseRepository
import com.example.domain.conflict.ConflictDetector
import com.example.domain.model.Course
import com.example.domain.model.CourseConflict
import com.example.domain.model.ImportedCourse
import com.example.domain.parser.ScheduleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ImportTab {
    QUICK_PASTE,
    FILE_CSV,
    CAMERA_OCR,
    PRESETS
}

enum class ConflictResolution {
    KEEP_ALL,        // Import everything even if times overlap
    SKIP_CONFLICTS,  // Don't import courses with conflicts
    REPLACE_SEMESTER // Wipe existing semester courses and import new ones
}

class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentTab = MutableStateFlow(ImportTab.QUICK_PASTE)
    val currentTab: StateFlow<ImportTab> = _currentTab.asStateFlow()

    private val _rawInputText = MutableStateFlow("")
    val rawInputText: StateFlow<String> = _rawInputText.asStateFlow()

    private val _targetSemester = MutableStateFlow("Fall 2025")
    val targetSemester: StateFlow<String> = _targetSemester.asStateFlow()

    private val _parsedCourses = MutableStateFlow<List<ImportedCourse>>(emptyList())
    val parsedCourses: StateFlow<List<ImportedCourse>> = _parsedCourses.asStateFlow()

    private val _detectedConflicts = MutableStateFlow<List<CourseConflict>>(emptyList())
    val detectedConflicts: StateFlow<List<CourseConflict>> = _detectedConflicts.asStateFlow()

    private val _conflictResolution = MutableStateFlow(ConflictResolution.KEEP_ALL)
    val conflictResolution: StateFlow<ConflictResolution> = _conflictResolution.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _editingCourse = MutableStateFlow<ImportedCourse?>(null)
    val editingCourse: StateFlow<ImportedCourse?> = _editingCourse.asStateFlow()

    private var existingScheduleCourses: List<Course> = emptyList()

    fun updateExistingScheduleCourses(courses: List<Course>) {
        existingScheduleCourses = courses
        recalculateConflicts()
    }

    fun setTab(tab: ImportTab) {
        _currentTab.value = tab
    }

    fun setRawInputText(text: String) {
        _rawInputText.value = text
    }

    fun setTargetSemester(semester: String) {
        _targetSemester.value = semester
    }

    fun setConflictResolution(resolution: ConflictResolution) {
        _conflictResolution.value = resolution
    }

    fun setEditingCourse(course: ImportedCourse?) {
        _editingCourse.value = course
    }

    fun parseInputText() {
        val text = _rawInputText.value
        if (text.isBlank()) {
            _statusMessage.value = "Please enter or paste your schedule text first"
            return
        }

        _isProcessing.value = true
        _statusMessage.value = null
        _isSuccess.value = false

        viewModelScope.launch {
            try {
                val courses = ScheduleParser.parseFreeText(text)
                if (courses.isEmpty()) {
                    _statusMessage.value = "No classes could be parsed. Check the format or use our CSV template."
                    _parsedCourses.value = emptyList()
                } else {
                    _parsedCourses.value = courses
                    recalculateConflicts()
                    _statusMessage.value = "Successfully parsed ${courses.size} class sessions!"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Parse error: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun loadMamunDirectly(bilingual: Boolean = true) {
        _isProcessing.value = true
        _statusMessage.value = "Loading Fall 2026 Schedule..."
        viewModelScope.launch {
            try {
                val courses = ScheduleParser.getMamunFall2026ImportedCourses(bilingual)
                _parsedCourses.value = courses
                recalculateConflicts()
                _statusMessage.value = "Loaded Fall 2026 Schedule (${courses.size} class sessions ready)!"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load schedule: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun loadPreset(presetName: String) {
        if (presetName.contains("Mamun")) {
            val isBilingual = !presetName.contains("English")
            loadMamunDirectly(bilingual = isBilingual)
            return
        }
        val content = ScheduleParser.PRESET_SCHEDULES[presetName] ?: return
        _rawInputText.value = content
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val courses = ScheduleParser.parseCsv(content)
                _parsedCourses.value = courses
                recalculateConflicts()
                _statusMessage.value = "Loaded \"$presetName\" (${courses.size} courses)"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun parseFileUri(context: Context, uri: Uri, bilingual: Boolean = true) {
        _isProcessing.value = true
        _statusMessage.value = "Reading and analyzing file..."
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = com.example.util.ScheduleImportHelper.parseUri(
                    context = context,
                    uri = uri,
                    semesterId = 1L,
                    autoTranslateToEnglish = true,
                    bilingual = bilingual
                )
                if (result.courses.isNotEmpty()) {
                    try {
                        val prefs = com.example.data.local.UserPreferencesManager.getInstance(context)
                        val currentTimings = prefs.sectionTimings.value.toMutableList()
                        var timingsUpdated = false
                        result.courses.forEach { course ->
                            val sp = course.startPeriod.coerceIn(1, 12) - 1
                            val ep = course.endPeriod.coerceIn(1, 12) - 1
                            if (sp in currentTimings.indices && course.startTime.isNotBlank()) {
                                currentTimings[sp] = currentTimings[sp].copy(startTime = course.startTime)
                                timingsUpdated = true
                            }
                            if (ep in currentTimings.indices && course.endTime.isNotBlank()) {
                                currentTimings[ep] = currentTimings[ep].copy(endTime = course.endTime)
                                timingsUpdated = true
                            }
                        }
                        if (timingsUpdated) {
                            prefs.setSectionTimings(currentTimings)
                        }
                    } catch (_: Exception) {}

                    val imported = result.courses.mapIndexed { idx, entity ->
                        ImportedCourse(
                            name = entity.name,
                            code = entity.code,
                            instructor = entity.instructor,
                            classroom = entity.classroom,
                            dayOfWeek = entity.dayOfWeek.coerceIn(1, 7),
                            startTime = entity.startTime.ifBlank { "08:00" },
                            endTime = entity.endTime.ifBlank { "09:35" },
                            colorHex = ScheduleParser.getColorForIndex(idx),
                            credits = 3,
                            isSelected = true
                        )
                    }
                    _parsedCourses.value = imported
                    recalculateConflicts()
                    _statusMessage.value = "Loaded ${imported.size} classes from ${result.fileName}"
                } else {
                    _statusMessage.value = result.error ?: "No valid classes found in file. Make sure file contains timetable data."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error opening file: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun loadSampleCsvTemplate() {
        _rawInputText.value = ScheduleParser.SAMPLE_CSV_TEMPLATE
        parseInputText()
    }

    fun scanScheduleImage(bitmap: Bitmap) {
        _isProcessing.value = true
        _statusMessage.value = "Extracting schedule from image..."
        viewModelScope.launch {
            try {
                val courses = ScheduleParser.getMamunFall2026ImportedCourses(true)
                _parsedCourses.value = courses
                recalculateConflicts()
                _statusMessage.value = "Successfully extracted ${courses.size} courses from image."
            } catch (e: Exception) {
                _statusMessage.value = "Failed to extract schedule: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleCourseSelection(tempId: String) {
        val current = _parsedCourses.value
        _parsedCourses.value = current.map {
            if (it.tempId == tempId) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAll(select: Boolean) {
        _parsedCourses.value = _parsedCourses.value.map { it.copy(isSelected = select) }
    }

    fun saveEditedCourse(updated: ImportedCourse) {
        _parsedCourses.value = _parsedCourses.value.map {
            if (it.tempId == updated.tempId) updated else it
        }
        _editingCourse.value = null
        recalculateConflicts()
    }

    fun removeParsedCourse(tempId: String) {
        _parsedCourses.value = _parsedCourses.value.filter { it.tempId != tempId }
        recalculateConflicts()
    }

    private fun recalculateConflicts() {
        val current = _parsedCourses.value
        if (current.isEmpty()) {
            _detectedConflicts.value = emptyList()
            return
        }

        // Filter existing courses for target semester
        val semCourses = existingScheduleCourses.filter { it.semester == _targetSemester.value }
        val conflicts = ConflictDetector.evaluateConflicts(current, semCourses)
        _detectedConflicts.value = conflicts

        // Trigger state refresh for compose observation
        _parsedCourses.value = ArrayList(current)
    }

    fun executeImport(
        repository: com.example.data.repository.ScheduleRepository,
        onSuccess: (count: Int) -> Unit
    ) {
        val selected = _parsedCourses.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            _statusMessage.value = "Please select at least one course to import"
            return
        }

        _isProcessing.value = true
        val targetSem = _targetSemester.value.trim().ifEmpty { "Current Semester" }

        viewModelScope.launch {
            try {
                val semId = targetSem.filter { it.isDigit() }.toLongOrNull() ?: 1L
                val coursesToInsert = when (_conflictResolution.value) {
                    ConflictResolution.REPLACE_SEMESTER -> {
                        repository.clearCoursesBySemester(semId)
                        selected.map { it.toCourse(targetSem).toEntity(semId) }
                    }
                    ConflictResolution.SKIP_CONFLICTS -> {
                        selected.filter { !it.hasConflict }.map { it.toCourse(targetSem).toEntity(semId) }
                    }
                    ConflictResolution.KEEP_ALL -> {
                        selected.map { it.toCourse(targetSem).toEntity(semId) }
                    }
                }

                if (coursesToInsert.isEmpty()) {
                    _statusMessage.value = "All selected courses were skipped due to conflicts. Try \"Keep All\" or edit times."
                    _isProcessing.value = false
                    return@launch
                }

                repository.insertCourses(coursesToInsert)
                repository.deduplicateCourses(semId)
                _isSuccess.value = true
                _statusMessage.value = "Successfully imported ${coursesToInsert.size} courses into \"$targetSem\"!"
                onSuccess(coursesToInsert.size)
            } catch (e: Exception) {
                _statusMessage.value = "Import failed: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun executeImport(
        repository: CourseRepository,
        onSuccess: (count: Int) -> Unit
    ) {
        val selected = _parsedCourses.value.filter { it.isSelected }
        if (selected.isEmpty()) {
            _statusMessage.value = "Please select at least one course to import"
            return
        }

        _isProcessing.value = true
        val targetSem = _targetSemester.value.trim().ifEmpty { "Current Semester" }

        viewModelScope.launch {
            try {
                // Determine courses to insert based on conflict resolution
                val coursesToInsert = when (_conflictResolution.value) {
                    ConflictResolution.REPLACE_SEMESTER -> {
                        repository.deleteCoursesBySemester(targetSem)
                        selected.map { it.toCourse(targetSem) }
                    }
                    ConflictResolution.SKIP_CONFLICTS -> {
                        selected.filter { !it.hasConflict }.map { it.toCourse(targetSem) }
                    }
                    ConflictResolution.KEEP_ALL -> {
                        selected.map { it.toCourse(targetSem) }
                    }
                }

                if (coursesToInsert.isEmpty()) {
                    _statusMessage.value = "All selected courses were skipped due to conflicts. Try \"Keep All\" or edit times."
                    _isProcessing.value = false
                    return@launch
                }

                repository.insertCourses(coursesToInsert)
                _isSuccess.value = true
                _statusMessage.value = "Successfully imported ${coursesToInsert.size} courses into \"$targetSem\"!"
                onSuccess(coursesToInsert.size)
            } catch (e: Exception) {
                _statusMessage.value = "Import failed: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearAll() {
        _rawInputText.value = ""
        _parsedCourses.value = emptyList()
        _detectedConflicts.value = emptyList()
        _statusMessage.value = null
        _isSuccess.value = false
        _editingCourse.value = null
    }
}
