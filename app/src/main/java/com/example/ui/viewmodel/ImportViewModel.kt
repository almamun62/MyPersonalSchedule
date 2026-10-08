package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import com.example.domain.model.ImportedCourse
import com.example.domain.parser.ExcelParser
import com.example.domain.parser.PdfScheduleParser
import com.example.domain.parser.ScheduleParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val _parsedCourses = MutableStateFlow<List<ImportedCourse>>(emptyList())
    val parsedCourses: StateFlow<List<ImportedCourse>> = _parsedCourses.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isWarning = MutableStateFlow(false)
    val isWarning: StateFlow<Boolean> = _isWarning.asStateFlow()

    private val _replaceExisting = MutableStateFlow(true)
    val replaceExisting: StateFlow<Boolean> = _replaceExisting.asStateFlow()

    fun setReplaceExisting(replace: Boolean) {
        _replaceExisting.value = replace
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
        _isWarning.value = false
    }

    fun parseCsvContent(csvContent: String) {
        val result = ScheduleParser.parseCsvSchedule(csvContent)
        _parsedCourses.value = result
        updateStatusAfterParse(result, "CSV text")
    }

    fun parseFreeText(text: String) {
        val result = ScheduleParser.parseFreeTextSchedule(text)
        _parsedCourses.value = result
        updateStatusAfterParse(result, "Pasted text")
    }

    fun addManualCourse(course: ImportedCourse) {
        val list = _parsedCourses.value.toMutableList()
        list.add(course)
        _parsedCourses.value = list
        _statusMessage.value = "Added course: ${course.name}"
        _isWarning.value = false
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
        _isWarning.value = false
    }

    /**
     * 100% Offline Universal File Parser for CSV, XLSX, and PDF.
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

            // 1. PDF File -> Offline text extraction via pdfbox-android
            if (lowerName.endsWith(".pdf")) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val (courses, hasText) = PdfScheduleParser.parsePdfStream(context, stream)
                    if (!hasText) {
                        _parsedCourses.value = emptyList()
                        _statusMessage.value = "No selectable text found in this PDF (it may be a scanned image). Please use manual add."
                        _isWarning.value = true
                        return
                    }
                    _parsedCourses.value = courses
                    updateStatusAfterParse(courses, fileName)
                }
                return
            }

            // 2. Excel (.xlsx / .xls) -> Raw binary stream
            if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val courses = ExcelParser.parseExcelStream(stream, fileName)
                    _parsedCourses.value = courses
                    updateStatusAfterParse(courses, fileName)
                }
                return
            }

            // 3. Text / CSV / TSV
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            if (bytes.isEmpty()) {
                _statusMessage.value = "Selected file '$fileName' is empty."
                _isWarning.value = true
                return
            }

            val content = try {
                String(bytes, Charsets.UTF_8)
            } catch (e: Exception) {
                String(bytes)
            }

            val courses = if (lowerName.endsWith(".csv") || lowerName.endsWith(".tsv") || content.contains(",")) {
                ScheduleParser.parseCsvSchedule(content)
            } else {
                ScheduleParser.parseFreeTextSchedule(content)
            }

            _parsedCourses.value = courses
            updateStatusAfterParse(courses, fileName)

        } catch (e: Exception) {
            e.printStackTrace()
            _statusMessage.value = "Error reading file: ${e.localizedMessage ?: "Unknown error"}"
            _isWarning.value = true
        }
    }

    private fun updateStatusAfterParse(courses: List<ImportedCourse>, source: String) {
        if (courses.isNotEmpty()) {
            _statusMessage.value = "Recognized ${courses.size} courses from $source!"
            _isWarning.value = false
        } else {
            _statusMessage.value = "No valid courses could be identified from $source. Check the formatting or use manual add."
            _isWarning.value = true
        }
    }

    fun getSampleCsvTemplate(): String {
        return """
            Course,Classroom,Instructor,Day,StartPeriod,EndPeriod,WeekRule
            Calculus I,Room 101,Prof. Newton,1,1,2,ALL
            Linear Algebra,Room 203,Dr. Gauss,2,3,4,ALL
            Data Structures,Software Lab 2,Prof. Turing,3,6,7,ALL
            Physics Lab,Science Bldg B12,Dr. Curie,4,1,3,ODD
            English Academic Writing,Liberal Arts 304,Ms. Austen,5,8,9,EVEN
        """.trimIndent()
    }
}
