package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import com.example.domain.model.ImportedCourse
import com.example.domain.parser.ExcelParser
import com.example.domain.parser.ScheduleParser
import com.example.domain.parser.ShareCodeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ImportTab {
    PRESETS,
    PORTAL_SYNC,
    SHARE_CODE,
    FREE_TEXT,
    FILE_PICKER,
    OCR_IMAGE,
    CSV_PASTE
}

class ImportViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentTab = MutableStateFlow(ImportTab.PRESETS)
    val currentTab: StateFlow<ImportTab> = _currentTab.asStateFlow()

    private val _parsedCourses = MutableStateFlow<List<ImportedCourse>>(emptyList())
    val parsedCourses: StateFlow<List<ImportedCourse>> = _parsedCourses.asStateFlow()

    fun setTab(tab: ImportTab) {
        _currentTab.value = tab
    }

    fun parseCsvContent(csvContent: String) {
        _parsedCourses.value = ScheduleParser.parseCsvSchedule(csvContent)
    }

    fun parseExcelContent(excelContent: String) {
        _parsedCourses.value = ExcelParser.parseExcelContent(excelContent)
    }

    fun parseFreeText(text: String) {
        _parsedCourses.value = ScheduleParser.parseFreeTextSchedule(text)
    }

    fun parseHtmlContent(htmlContent: String) {
        _parsedCourses.value = ScheduleParser.parseHtmlSchedule(htmlContent)
    }

    fun parseIcsContent(icsContent: String) {
        _parsedCourses.value = ScheduleParser.parseIcsSchedule(icsContent)
    }

    fun parseOcrImageText(ocrText: String) {
        _parsedCourses.value = ScheduleParser.parseFreeTextSchedule(ocrText)
    }

    fun parseShareCode(shareCode: String) {
        _parsedCourses.value = ShareCodeManager.parseShareCode(shareCode)
    }

    fun setDirectFetchedCourses(courses: List<ImportedCourse>) {
        _parsedCourses.value = courses
    }

    fun loadPresetSchedule(presetType: String = "COMPUTER_SCI") {
        _parsedCourses.value = when (presetType) {
            "SOFTWARE_ENG" -> ScheduleParser.getSoftwareEngineeringPreset()
            else -> ScheduleParser.getComputerSciencePreset()
        }
    }

    fun removeParsedCourse(course: ImportedCourse) {
        _parsedCourses.value = _parsedCourses.value.filter { it != course }
    }

    fun updateParsedCourse(index: Int, updated: ImportedCourse) {
        val list = _parsedCourses.value.toMutableList()
        if (index in list.indices) {
            list[index] = updated
            _parsedCourses.value = list
        }
    }

    fun clearParsedCourses() {
        _parsedCourses.value = emptyList()
    }

    fun parseFileFromUri(uri: Uri) {
        try {
            val context = getApplication<Application>()
            var fileName = "schedule_file"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIdx != -1) {
                    fileName = cursor.getString(nameIdx)
                }
            }

            val text = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: ""

            when {
                fileName.endsWith(".ics", ignoreCase = true) || text.contains("BEGIN:VCALENDAR", ignoreCase = true) -> {
                    parseIcsContent(text)
                }
                fileName.endsWith(".xls", ignoreCase = true) || fileName.endsWith(".xlsx", ignoreCase = true) || text.contains("<Workbook", ignoreCase = true) -> {
                    parseExcelContent(text)
                }
                fileName.endsWith(".html", ignoreCase = true) || fileName.endsWith(".htm", ignoreCase = true) || text.contains("<table", ignoreCase = true) -> {
                    parseHtmlContent(text)
                }
                fileName.endsWith(".csv", ignoreCase = true) || fileName.endsWith(".tsv", ignoreCase = true) -> {
                    parseCsvContent(text)
                }
                else -> {
                    parseFreeText(text)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
