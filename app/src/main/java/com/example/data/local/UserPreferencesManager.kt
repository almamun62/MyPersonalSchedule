package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SectionTiming
import com.example.ui.theme.AppAccentColor
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(loadAccentColor())
    val accentColor: StateFlow<AppAccentColor> = _accentColor.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, true))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _isAutoDndEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_DND, true))
    val isAutoDndEnabled: StateFlow<Boolean> = _isAutoDndEnabled.asStateFlow()

    private val _isClassReminder15mEnabled = MutableStateFlow(prefs.getBoolean(KEY_CLASS_REMINDER_15M, true))
    val isClassReminder15mEnabled: StateFlow<Boolean> = _isClassReminder15mEnabled.asStateFlow()

    private val _lastUsedRoom = MutableStateFlow(prefs.getString(KEY_LAST_USED_ROOM, "Room 101") ?: "Room 101")
    val lastUsedRoom: StateFlow<String> = _lastUsedRoom.asStateFlow()

    private val _lastUsedSemester = MutableStateFlow(prefs.getString(KEY_LAST_USED_SEMESTER, "Fall 2026") ?: "Fall 2026")
    val lastUsedSemester: StateFlow<String> = _lastUsedSemester.asStateFlow()

    fun setLastUsedCourseMetadata(room: String, semester: String) {
        if (room.isNotBlank()) {
            prefs.edit().putString(KEY_LAST_USED_ROOM, room.trim()).apply()
            _lastUsedRoom.value = room.trim()
        }
        if (semester.isNotBlank()) {
            prefs.edit().putString(KEY_LAST_USED_SEMESTER, semester.trim()).apply()
            _lastUsedSemester.value = semester.trim()
        }
    }

    private val _focusActive = MutableStateFlow(prefs.getBoolean(KEY_FOCUS_ACTIVE, false))
    val focusActive: StateFlow<Boolean> = _focusActive.asStateFlow()

    private val _focusEndTimestamp = MutableStateFlow(prefs.getLong(KEY_FOCUS_END_TIMESTAMP, 0L))
    val focusEndTimestamp: StateFlow<Long> = _focusEndTimestamp.asStateFlow()

    private val _focusTotalMinutes = MutableStateFlow(prefs.getInt(KEY_FOCUS_TOTAL_MINUTES, 25))
    val focusTotalMinutes: StateFlow<Int> = _focusTotalMinutes.asStateFlow()

    private val _focusCourseName = MutableStateFlow(prefs.getString(KEY_FOCUS_COURSE_NAME, "General Study") ?: "General Study")
    val focusCourseName: StateFlow<String> = _focusCourseName.asStateFlow()

    fun setFocusSession(active: Boolean, endTimestamp: Long = 0L, totalMinutes: Int = 25, courseName: String = "General Study") {
        prefs.edit()
            .putBoolean(KEY_FOCUS_ACTIVE, active)
            .putLong(KEY_FOCUS_END_TIMESTAMP, endTimestamp)
            .putInt(KEY_FOCUS_TOTAL_MINUTES, totalMinutes)
            .putString(KEY_FOCUS_COURSE_NAME, courseName)
            .apply()
        _focusActive.value = active
        _focusEndTimestamp.value = endTimestamp
        _focusTotalMinutes.value = totalMinutes
        _focusCourseName.value = courseName
    }

    fun clearFocusSession() {
        setFocusSession(active = false, endTimestamp = 0L, totalMinutes = 25, courseName = "General Study")
    }

    private val _showNotchMode = MutableStateFlow(prefs.getBoolean(KEY_SHOW_NOTCH, true))
    val showNotchMode: StateFlow<Boolean> = _showNotchMode.asStateFlow()

    private val defaultWidgetsOrder = "HYPER_ISLAND,NEXT_CLASS,TODAY_TIMELINE,TASKS_PREVIEW,SEMESTER_PROGRESS,FOCUS_TIMER,QUICK_ACTIONS"

    private val _dashboardWidgetOrder = MutableStateFlow(loadDashboardWidgetOrder())
    val dashboardWidgetOrder: StateFlow<List<String>> = _dashboardWidgetOrder.asStateFlow()

    private val _dashboardHiddenWidgets = MutableStateFlow(loadDashboardHiddenWidgets())
    val dashboardHiddenWidgets: StateFlow<Set<String>> = _dashboardHiddenWidgets.asStateFlow()

    private val defaultAllowedApps = "internal_calculator,internal_notes,internal_materials"
    private val _focusAllowedApps = MutableStateFlow(loadFocusAllowedApps())
    val focusAllowedApps: StateFlow<List<String>> = _focusAllowedApps.asStateFlow()

    private val _classReminderMinutes = MutableStateFlow(prefs.getInt(KEY_CLASS_REMINDER_MINUTES, 15))
    val classReminderMinutes: StateFlow<Int> = _classReminderMinutes.asStateFlow()

    private val defaultLang = if (java.util.Locale.getDefault().language.startsWith("zh")) "zh" else "en"
    private val _appLanguage = MutableStateFlow(prefs.getString(KEY_APP_LANGUAGE, defaultLang) ?: defaultLang)
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _sectionTimings = MutableStateFlow(loadSectionTimings())
    val sectionTimings: StateFlow<List<SectionTiming>> = _sectionTimings.asStateFlow()

    fun setSectionTimings(timings: List<SectionTiming>) {
        val jsonString = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(SectionTiming.serializer()),
            timings
        )
        prefs.edit().putString(KEY_SECTION_TIMINGS, jsonString).apply()
        _sectionTimings.value = timings
    }

    private fun loadSectionTimings(): List<SectionTiming> {
        val jsonString = prefs.getString(KEY_SECTION_TIMINGS, null)
        if (jsonString != null) {
            try {
                return kotlinx.serialization.json.Json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(SectionTiming.serializer()),
                    jsonString
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return defaultSectionTimings
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAccentColor(accent: AppAccentColor) {
        prefs.edit().putString(KEY_ACCENT_ID, accent.id).apply()
        _accentColor.value = accent
    }

    fun setAppLanguage(lang: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()
        _appLanguage.value = lang
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    fun setAutoDndEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DND, enabled).apply()
        _isAutoDndEnabled.value = enabled
    }

    fun setClassReminder15mEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLASS_REMINDER_15M, enabled).apply()
        _isClassReminder15mEnabled.value = enabled
    }

    fun setClassReminderMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_CLASS_REMINDER_MINUTES, minutes).apply()
        _classReminderMinutes.value = minutes
    }

    fun setShowNotchMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_NOTCH, enabled).apply()
        _showNotchMode.value = enabled
    }

    fun setDashboardWidgetOrder(order: List<String>) {
        val str = order.joinToString(",")
        prefs.edit().putString(KEY_DASHBOARD_ORDER, str).apply()
        _dashboardWidgetOrder.value = order
    }

    fun setDashboardHiddenWidgets(hidden: Set<String>) {
        val str = hidden.joinToString(",")
        prefs.edit().putString(KEY_DASHBOARD_HIDDEN, str).apply()
        _dashboardHiddenWidgets.value = hidden
    }

    fun resetDashboardWidgets() {
        val defaultList = defaultWidgetsOrder.split(",")
        prefs.edit().remove(KEY_DASHBOARD_ORDER).remove(KEY_DASHBOARD_HIDDEN).apply()
        _dashboardWidgetOrder.value = defaultList
        _dashboardHiddenWidgets.value = emptySet()
    }

    fun setFocusAllowedApps(apps: List<String>) {
        val trimmed = apps.take(3)
        val str = trimmed.joinToString(",")
        prefs.edit().putString(KEY_FOCUS_ALLOWED_APPS, str).apply()
        _focusAllowedApps.value = trimmed
    }

    private fun loadFocusAllowedApps(): List<String> {
        val raw = prefs.getString(KEY_FOCUS_ALLOWED_APPS, defaultAllowedApps) ?: defaultAllowedApps
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3)
    }

    private fun loadDashboardWidgetOrder(): List<String> {
        val raw = prefs.getString(KEY_DASHBOARD_ORDER, defaultWidgetsOrder) ?: defaultWidgetsOrder
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun loadDashboardHiddenWidgets(): Set<String> {
        val raw = prefs.getString(KEY_DASHBOARD_HIDDEN, "") ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try { AppThemeMode.valueOf(name) } catch (e: Exception) { AppThemeMode.SYSTEM }
    }

    private fun loadAccentColor(): AppAccentColor {
        val id = prefs.getString(KEY_ACCENT_ID, AppAccentColor.BLUE.id) ?: AppAccentColor.BLUE.id
        return AppAccentColor.values().find { it.id == id } ?: AppAccentColor.BLUE
    }

    companion object {
        private const val PREFS_NAME = "course_schedule_prefs"
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_ACCENT_ID = "pref_accent_id"
        private const val KEY_ONBOARDING_COMPLETED = "pref_onboarding_completed"
        private const val KEY_AUTO_DND = "pref_auto_dnd"
        private const val KEY_CLASS_REMINDER_15M = "pref_class_reminder_15m"
        private const val KEY_CLASS_REMINDER_MINUTES = "pref_class_reminder_minutes"
        private const val KEY_SHOW_NOTCH = "pref_show_notch"
        private const val KEY_APP_LANGUAGE = "pref_app_language"
        private const val KEY_SECTION_TIMINGS = "pref_section_timings"
        private const val KEY_DASHBOARD_ORDER = "pref_dashboard_order"
        private const val KEY_DASHBOARD_HIDDEN = "pref_dashboard_hidden"
        private const val KEY_FOCUS_ALLOWED_APPS = "pref_focus_allowed_apps"
        private const val KEY_LAST_USED_ROOM = "pref_last_used_room"
        private const val KEY_LAST_USED_SEMESTER = "pref_last_used_semester"
        private const val KEY_FOCUS_ACTIVE = "pref_focus_active"
        private const val KEY_FOCUS_END_TIMESTAMP = "pref_focus_end_timestamp"
        private const val KEY_FOCUS_TOTAL_MINUTES = "pref_focus_total_minutes"
        private const val KEY_FOCUS_COURSE_NAME = "pref_focus_course_name"

        val defaultSectionTimings = listOf(
            SectionTiming(1, "08:00", "08:45"),
            SectionTiming(2, "08:50", "09:35"),
            SectionTiming(3, "09:50", "10:35"),
            SectionTiming(4, "10:40", "11:25"),
            SectionTiming(5, "11:30", "12:15"),
            SectionTiming(6, "14:30", "15:15"), // Section 6 after lunch break
            SectionTiming(7, "15:20", "16:05"),
            SectionTiming(8, "16:20", "17:05"),
            SectionTiming(9, "17:10", "17:55"),
            SectionTiming(10, "19:00", "19:45"),
            SectionTiming(11, "19:50", "20:35"),
            SectionTiming(12, "20:40", "21:25")
        )

        @Volatile
        private var INSTANCE: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
