package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.domain.model.ImportedCourse
import com.example.domain.parser.ScheduleParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ImportTab {
    QUICK_PASTE,
    FILE_CSV,
    CAMERA_OCR,
    PRESETS
}

class ImportViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentTab = MutableStateFlow(ImportTab.QUICK_PASTE)
    val currentTab: StateFlow<ImportTab> = _currentTab.asStateFlow()

    private val _parsedCourses = MutableStateFlow<List<ImportedCourse>>(emptyList())
    val parsedCourses: StateFlow<List<ImportedCourse>> = _parsedCourses.asStateFlow()

    fun setTab(tab: ImportTab) {
        _currentTab.value = tab
    }

    fun parseCsvContent(csvContent: String) {
        _parsedCourses.value = ScheduleParser.parseCsvSchedule(csvContent)
    }

    fun loadPresetSchedule() {
        _parsedCourses.value = ScheduleParser.getMamunFall2026ImportedCourses()
    }
}
