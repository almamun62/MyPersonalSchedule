package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import com.example.domain.model.ImportedCourse
import com.example.domain.parser.ExcelParser
import com.example.domain.parser.JsonScheduleParser
import com.example.domain.parser.ScheduleParser
import com.example.domain.parser.ShareCodeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ImportTab {
    FILE_PICKER,
    FREE_TEXT,
    VISUAL_GRID,
    SHARE_CODE,
    PRESETS
}

class ImportViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentTab = MutableStateFlow(ImportTab.FILE_PICKER)
    val currentTab: StateFlow<ImportTab> = _currentTab.asStateFlow()

    private val _parsedCourses = MutableStateFlow<List<ImportedCourse>>(emptyList())
    val parsedCourses: StateFlow<List<ImportedCourse>> = _parsedCourses.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _replaceExisting = MutableStateFlow(true)
    val replaceExisting: StateFlow<Boolean> = _replaceExisting.asStateFlow()

    fun setTab(tab: ImportTab) {
        _currentTab.value = tab
    }

    fun setReplaceExisting(replace: Boolean) {
        _replaceExisting.value = replace
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun parseCsvContent(csvContent: String) {
        val result = ScheduleParser.parseCsvSchedule(csvContent)
        _parsedCourses.value = result
        updateStatusAfterParse(result, "CSV / Spreadsheet data")
    }

    fun parseExcelContent(excelContent: String) {
        val result = ExcelParser.parseExcelContent(excelContent)
        _parsedCourses.value = result
        updateStatusAfterParse(result, "Excel content")
    }

    fun parseFreeText(text: String) {
        val result = ScheduleParser.parseFreeTextSchedule(text)
        _parsedCourses.value = result
        updateStatusAfterParse(result, "Text schedule")
    }

    fun parseHtmlContent(htmlContent: String) {
        val result = ScheduleParser.parseHtmlSchedule(htmlContent)
        _parsedCourses.value = result
        updateStatusAfterParse(result, "HTML web timetable")
    }

    fun parseIcsContent(icsContent: String) {
        val result = ScheduleParser.parseIcsSchedule(icsContent)
        _parsedCourses.value = result
        updateStatusAfterParse(result, "iCalendar (.ics)")
    }

    fun parseJsonContent(jsonContent: String) {
        val result = JsonScheduleParser.parseJsonSchedule(jsonContent)
        _parsedCourses.value = result
        updateStatusAfterParse(result, "JSON schedule")
    }

    fun parseShareCode(shareCode: String) {
        val result = ShareCodeManager.parseShareCode(shareCode)
        _parsedCourses.value = result
        updateStatusAfterParse(result, "Share Code")
    }

    fun addManualCourse(course: ImportedCourse) {
        val list = _parsedCourses.value.toMutableList()
        list.add(course)
        _parsedCourses.value = list
        _statusMessage.value = "Added course: ${course.name}"
    }

    fun loadPresetSchedule(presetType: String = "COMPUTER_SCI") {
        val result = when (presetType) {
            "SOFTWARE_ENG" -> ScheduleParser.getSoftwareEngineeringPreset()
            else -> ScheduleParser.getComputerSciencePreset()
        }
        _parsedCourses.value = result
        _statusMessage.value = "Loaded preset with ${result.size} courses."
    }

    fun removeParsedCourse(course: ImportedCourse) {
        _parsedCourses.value = _parsedCourses.value.filter { it != course }
    }

    fun toggleCourseSelection(index: Int) {
        val list = _parsedCourses.value.toMutableList()
        if (index in list.indices) {
            val item = list[index]
            list[index] = item.copy(isSelected = !item.isSelected)
            _parsedCourses.value = list
        }
    }

    fun selectAll(selected: Boolean) {
        _parsedCourses.value = _parsedCourses.value.map { it.copy(isSelected = selected) }
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
        _statusMessage.value = null
    }

    /**
     * 100% Offline Safe Universal File Parser.
     * Uses binary InputStream for Excel (.xlsx/.xls) to prevent encoding corruption.
     */
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

            val lowerName = fileName.lowercase()

            // 1. Excel (.xlsx / .xls) -> MUST parse as raw binary stream
            if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val courses = ExcelParser.parseExcelStream(stream, fileName)
                    _parsedCourses.value = courses
                    updateStatusAfterParse(courses, fileName)
                }
                return
            }

            // 2. Text-based files (.ics, .json, .csv, .tsv, .html, .txt)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            if (bytes.isEmpty()) {
                _statusMessage.value = "Selected file '$fileName' is empty."
                return
            }

            // Attempt UTF-8, fallback to GBK / default charset if needed
            val content = try {
                String(bytes, Charsets.UTF_8)
            } catch (e: Exception) {
                String(bytes)
            }

            val courses = when {
                lowerName.endsWith(".ics") || content.contains("BEGIN:VCALENDAR", ignoreCase = true) -> {
                    ScheduleParser.parseIcsSchedule(content)
                }
                lowerName.endsWith(".json") || content.trimStart().startsWith("[") || content.trimStart().startsWith("{") -> {
                    JsonScheduleParser.parseJsonSchedule(content)
                }
                lowerName.endsWith(".html") || lowerName.endsWith(".htm") || content.contains("<table", ignoreCase = true) -> {
                    ScheduleParser.parseHtmlSchedule(content)
                }
                lowerName.endsWith(".csv") || lowerName.endsWith(".tsv") -> {
                    ScheduleParser.parseCsvSchedule(content)
                }
                content.contains("<Workbook", ignoreCase = true) || content.contains("<Table", ignoreCase = true) -> {
                    ExcelParser.parseExcelContent(content)
                }
                else -> {
                    ScheduleParser.parseFreeTextSchedule(content)
                }
            }

            _parsedCourses.value = courses
            updateStatusAfterParse(courses, fileName)

        } catch (e: Exception) {
            e.printStackTrace()
            _statusMessage.value = "Failed to parse file: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    private fun updateStatusAfterParse(courses: List<ImportedCourse>, source: String) {
        if (courses.isNotEmpty()) {
            _statusMessage.value = "Successfully recognized ${courses.size} courses from $source!"
        } else {
            _statusMessage.value = "No valid courses could be identified from $source. Check the formatting or try pasting into Smart Text."
        }
    }
}
