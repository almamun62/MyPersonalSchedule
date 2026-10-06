package com.example.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CalculatorMemoryManager {
    // Last calculated result ticker (visible across HyperIsland and widgets)
    private val _lastResultTicker = MutableStateFlow<String?>(null)
    val lastResultTicker: StateFlow<String?> = _lastResultTicker.asStateFlow()

    // Course linked grade buffer (e.g., "Computer Arch" -> "85 * 0.4 = 34.0")
    private val _savedCourseScores = MutableStateFlow<Map<String, String>>(emptyMap())
    val savedCourseScores: StateFlow<Map<String, String>> = _savedCourseScores.asStateFlow()

    fun updateLastResult(result: String, equation: String = "") {
        _lastResultTicker.value = if (equation.isNotBlank()) "$equation = $result" else result
    }

    fun clearTicker() {
        _lastResultTicker.value = null
    }

    fun saveScoreToCourse(courseName: String, value: String, description: String = "") {
        val current = _savedCourseScores.value.toMutableMap()
        current[courseName] = if (description.isNotBlank()) "$description: $value" else value
        _savedCourseScores.value = current
        _lastResultTicker.value = "$courseName: $value"
    }

    fun getScoreForCourse(courseName: String): String? {
        return _savedCourseScores.value[courseName]
    }
}
