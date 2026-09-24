package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.AppAccentColor
import com.example.ui.theme.AppAccents
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConflictPreferenceStatus(
    val preferredCourseId: Long?,
    val isPermanent: Boolean,
    val scopeDescription: String
)

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

    private val _isSleepAlarmEnabled = MutableStateFlow(prefs.getBoolean(KEY_SLEEP_ALARM_ENABLED, false))
    val isSleepAlarmEnabled: StateFlow<Boolean> = _isSleepAlarmEnabled.asStateFlow()

    private val _sleepBedtime = MutableStateFlow(prefs.getString(KEY_SLEEP_BEDTIME, "23:00") ?: "23:00")
    val sleepBedtime: StateFlow<String> = _sleepBedtime.asStateFlow()

    private val _sleepWakeTime = MutableStateFlow(prefs.getString(KEY_SLEEP_WAKE_TIME, "07:00") ?: "07:00")
    val sleepWakeTime: StateFlow<String> = _sleepWakeTime.asStateFlow()

    private val _isSleepSmartWakeEnabled = MutableStateFlow(prefs.getBoolean(KEY_SLEEP_SMART_WAKE, true))
    val isSleepSmartWakeEnabled: StateFlow<Boolean> = _isSleepSmartWakeEnabled.asStateFlow()

    private val _sleepWakeAdvanceMinutes = MutableStateFlow(prefs.getInt(KEY_SLEEP_WAKE_ADVANCE, 60))
    val sleepWakeAdvanceMinutes: StateFlow<Int> = _sleepWakeAdvanceMinutes.asStateFlow()

    private val _sleepTargetHours = MutableStateFlow(prefs.getFloat(KEY_SLEEP_TARGET_HOURS, 8.0f))
    val sleepTargetHours: StateFlow<Float> = _sleepTargetHours.asStateFlow()

    private val _isSleepAlarmVibrate = MutableStateFlow(prefs.getBoolean(KEY_SLEEP_ALARM_VIBRATE, true))
    val isSleepAlarmVibrate: StateFlow<Boolean> = _isSleepAlarmVibrate.asStateFlow()

    private val _isSleepAlarmSound = MutableStateFlow(prefs.getBoolean(KEY_SLEEP_ALARM_SOUND, true))
    val isSleepAlarmSound: StateFlow<Boolean> = _isSleepAlarmSound.asStateFlow()

    private val _isBilingualPreferred = MutableStateFlow(prefs.getBoolean(KEY_BILINGUAL, true))
    private val defaultLang = if (java.util.Locale.getDefault().language.startsWith("zh")) "zh" else "en"
    private val _appLanguage = MutableStateFlow(prefs.getString(KEY_APP_LANGUAGE, defaultLang))
    val appLanguage: StateFlow<String?> = _appLanguage.asStateFlow()
    val isBilingualPreferred: StateFlow<Boolean> = _isBilingualPreferred.asStateFlow()

    private val _isNightlySummaryEnabled = MutableStateFlow(prefs.getBoolean(KEY_NIGHTLY_SUMMARY_ENABLED, true))
    val isNightlySummaryEnabled: StateFlow<Boolean> = _isNightlySummaryEnabled.asStateFlow()

    private val _nightlySummaryTime = MutableStateFlow(prefs.getString(KEY_NIGHTLY_SUMMARY_TIME, "21:00") ?: "21:00")
    val nightlySummaryTime: StateFlow<String> = _nightlySummaryTime.asStateFlow()

    fun setNightlySummaryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NIGHTLY_SUMMARY_ENABLED, enabled).apply()
        _isNightlySummaryEnabled.value = enabled
    }

    fun setNightlySummaryTime(time: String) {
        prefs.edit().putString(KEY_NIGHTLY_SUMMARY_TIME, time).apply()
        _nightlySummaryTime.value = time
    }

    private val _geminiApiKey = MutableStateFlow(prefs.getString(KEY_GEMINI_API_KEY, ""))
    val geminiApiKey: StateFlow<String?> = _geminiApiKey.asStateFlow()

    private val _aiBaseUrl = MutableStateFlow(prefs.getString(KEY_AI_BASE_URL, "https://generativelanguage.googleapis.com/v1beta/openai/"))
    val aiBaseUrl: StateFlow<String?> = _aiBaseUrl.asStateFlow()

    private val _aiModelName = MutableStateFlow(prefs.getString(KEY_AI_MODEL_NAME, "gemini-1.5-flash"))
    val aiModelName: StateFlow<String?> = _aiModelName.asStateFlow()

    private val _calendarImageUri = MutableStateFlow(prefs.getString(KEY_CALENDAR_IMAGE_URI, null))
    val calendarImageUri: StateFlow<String?> = _calendarImageUri.asStateFlow()

    private val _preferredConflictCourseIds = MutableStateFlow(loadPreferredConflictCourseIds())
    val preferredConflictCourseIds: StateFlow<Set<Long>> = _preferredConflictCourseIds.asStateFlow()

    private val _sessionCoursePriorities = MutableStateFlow(deserializeMap(prefs.getString(KEY_SESSION_COURSE_PRIORITIES, null)))
    val sessionCoursePriorities: StateFlow<Map<String, Long>> = _sessionCoursePriorities.asStateFlow()

    private val _timeBlockPermanentPreferences = MutableStateFlow(deserializeMap(prefs.getString(KEY_TIME_BLOCK_PERMANENT_PREFERENCES, null)))
    val timeBlockPermanentPreferences: StateFlow<Map<String, Long>> = _timeBlockPermanentPreferences.asStateFlow()

    private val _hideAuditedInConflict = MutableStateFlow(prefs.getBoolean(KEY_HIDE_AUDITED_IN_CONFLICT, false))
    val hideAuditedInConflict: StateFlow<Boolean> = _hideAuditedInConflict.asStateFlow()

    private val _sectionTimings = MutableStateFlow(loadSectionTimings())
    val sectionTimings: StateFlow<List<com.example.data.model.SectionTiming>> = _sectionTimings.asStateFlow()

    fun setSectionTimings(timings: List<com.example.data.model.SectionTiming>) {
        val jsonString = kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(com.example.data.model.SectionTiming.serializer()), timings)
        prefs.edit().putString(KEY_SECTION_TIMINGS, jsonString).apply()
        _sectionTimings.value = timings
    }

    private fun loadSectionTimings(): List<com.example.data.model.SectionTiming> {
        val jsonString = prefs.getString(KEY_SECTION_TIMINGS, null)
        if (jsonString != null) {
            try {
                return kotlinx.serialization.json.Json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(com.example.data.model.SectionTiming.serializer()), jsonString)
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

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    fun setAutoDndEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DND, enabled).apply()
        _isAutoDndEnabled.value = enabled
    }

    
    private val _classReminderMinutes = MutableStateFlow(prefs.getInt(KEY_CLASS_REMINDER_MINUTES, 15))
    val classReminderMinutes: StateFlow<Int> = _classReminderMinutes.asStateFlow()

    fun setClassReminderMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_CLASS_REMINDER_MINUTES, minutes).apply()
        _classReminderMinutes.value = minutes
    }
    fun setClassReminder15mEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLASS_REMINDER_15M, enabled).apply()
        _isClassReminder15mEnabled.value = enabled
    }

    fun setSleepAlarmEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SLEEP_ALARM_ENABLED, enabled).apply()
        _isSleepAlarmEnabled.value = enabled
    }

    fun setSleepSchedule(
        bedtime: String,
        wakeTime: String,
        smartWake: Boolean,
        advanceMinutes: Int,
        targetHours: Float,
        vibrate: Boolean,
        sound: Boolean
    ) {
        prefs.edit()
            .putString(KEY_SLEEP_BEDTIME, bedtime)
            .putString(KEY_SLEEP_WAKE_TIME, wakeTime)
            .putBoolean(KEY_SLEEP_SMART_WAKE, smartWake)
            .putInt(KEY_SLEEP_WAKE_ADVANCE, advanceMinutes)
            .putFloat(KEY_SLEEP_TARGET_HOURS, targetHours)
            .putBoolean(KEY_SLEEP_ALARM_VIBRATE, vibrate)
            .putBoolean(KEY_SLEEP_ALARM_SOUND, sound)
            .apply()
        _sleepBedtime.value = bedtime
        _sleepWakeTime.value = wakeTime
        _isSleepSmartWakeEnabled.value = smartWake
        _sleepWakeAdvanceMinutes.value = advanceMinutes
        _sleepTargetHours.value = targetHours
        _isSleepAlarmVibrate.value = vibrate
        _isSleepAlarmSound.value = sound
    }

    fun setAppLanguage(lang: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()
        _appLanguage.value = lang
    }

    fun setBilingualPreferred(bilingual: Boolean) {
        prefs.edit().putBoolean(KEY_BILINGUAL, bilingual).apply()
        _isBilingualPreferred.value = bilingual
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()
        _geminiApiKey.value = key
    }

    fun setAiBaseUrl(url: String) {
        val validUrl = if (url.isBlank()) "https://generativelanguage.googleapis.com/v1beta/openai/" else url
        prefs.edit().putString(KEY_AI_BASE_URL, validUrl).apply()
        _aiBaseUrl.value = validUrl
    }

    fun setAiModelName(model: String) {
        val validModel = if (model.isBlank()) "gemini-1.5-flash" else model
        prefs.edit().putString(KEY_AI_MODEL_NAME, validModel).apply()
        _aiModelName.value = validModel
    }

    fun setCalendarImageUri(uri: String?) {
        prefs.edit().putString(KEY_CALENDAR_IMAGE_URI, uri).apply()
        _calendarImageUri.value = uri
    }

    fun setPreferredCourseForConflict(preferredCourseId: Long, otherCourseId: Long? = null) {
        val current = _preferredConflictCourseIds.value.toMutableSet()
        current.add(preferredCourseId)
        if (otherCourseId != null) {
            current.remove(otherCourseId)
        }
        val serialized = current.joinToString(",")
        prefs.edit().putString(KEY_PREFERRED_CONFLICT_COURSES, serialized).apply()
        _preferredConflictCourseIds.value = current
    }

    fun removeCourseFromConflictPreference(courseId: Long) {
        val current = _preferredConflictCourseIds.value.toMutableSet()
        if (current.remove(courseId)) {
            val serialized = current.joinToString(",")
            prefs.edit().putString(KEY_PREFERRED_CONFLICT_COURSES, serialized).apply()
            _preferredConflictCourseIds.value = current
        }
    }

    fun setSessionPriority(week: Int, dayOfWeek: Int, startPeriod: Int, preferredCourseId: Long) {
        val key = "w${week}_d${dayOfWeek}_p${startPeriod}"
        val current = _sessionCoursePriorities.value.toMutableMap()
        current[key] = preferredCourseId
        prefs.edit().putString(KEY_SESSION_COURSE_PRIORITIES, serializeMap(current)).apply()
        _sessionCoursePriorities.value = current

        val curSet = _preferredConflictCourseIds.value.toMutableSet()
        curSet.add(preferredCourseId)
        prefs.edit().putString(KEY_PREFERRED_CONFLICT_COURSES, curSet.joinToString(",")).apply()
        _preferredConflictCourseIds.value = curSet
    }

    fun clearSessionPriority(week: Int, dayOfWeek: Int, startPeriod: Int) {
        val key = "w${week}_d${dayOfWeek}_p${startPeriod}"
        val current = _sessionCoursePriorities.value.toMutableMap()
        if (current.remove(key) != null) {
            prefs.edit().putString(KEY_SESSION_COURSE_PRIORITIES, serializeMap(current)).apply()
            _sessionCoursePriorities.value = current
        }
    }

    fun setTimeBlockPermanentPreference(dayOfWeek: Int, startPeriod: Int, endPeriod: Int, preferredCourseId: Long, otherCourseId: Long? = null) {
        val key = "d${dayOfWeek}_p${startPeriod}"
        val current = _timeBlockPermanentPreferences.value.toMutableMap()
        current[key] = preferredCourseId
        prefs.edit().putString(KEY_TIME_BLOCK_PERMANENT_PREFERENCES, serializeMap(current)).apply()
        _timeBlockPermanentPreferences.value = current

        setPreferredCourseForConflict(preferredCourseId, otherCourseId)
    }

    fun clearTimeBlockPermanentPreference(dayOfWeek: Int, startPeriod: Int, endPeriod: Int) {
        val key = "d${dayOfWeek}_p${startPeriod}"
        val current = _timeBlockPermanentPreferences.value.toMutableMap()
        if (current.remove(key) != null) {
            prefs.edit().putString(KEY_TIME_BLOCK_PERMANENT_PREFERENCES, serializeMap(current)).apply()
            _timeBlockPermanentPreferences.value = current
        }
    }

    fun getEffectiveConflictPreference(
        week: Int,
        dayOfWeek: Int,
        startPeriod: Int,
        course1Id: Long,
        course2Id: Long
    ): ConflictPreferenceStatus {
        val sessionKey = "w${week}_d${dayOfWeek}_p${startPeriod}"
        val sessionPref = _sessionCoursePriorities.value[sessionKey]
        if (sessionPref != null) {
            if (sessionPref == course1Id) return ConflictPreferenceStatus(course1Id, isPermanent = false, scopeDescription = "Current Session (Week $week)")
            if (sessionPref == course2Id) return ConflictPreferenceStatus(course2Id, isPermanent = false, scopeDescription = "Current Session (Week $week)")
        }

        val blockKey = "d${dayOfWeek}_p${startPeriod}"
        val blockPref = _timeBlockPermanentPreferences.value[blockKey]
        if (blockPref != null) {
            if (blockPref == course1Id) return ConflictPreferenceStatus(course1Id, isPermanent = true, scopeDescription = "Permanent (Time Block)")
            if (blockPref == course2Id) return ConflictPreferenceStatus(course2Id, isPermanent = true, scopeDescription = "Permanent (Time Block)")
        }

        if (_preferredConflictCourseIds.value.contains(course1Id) && !_preferredConflictCourseIds.value.contains(course2Id)) {
            return ConflictPreferenceStatus(course1Id, isPermanent = true, scopeDescription = "Permanent Preference")
        }
        if (_preferredConflictCourseIds.value.contains(course2Id) && !_preferredConflictCourseIds.value.contains(course1Id)) {
            return ConflictPreferenceStatus(course2Id, isPermanent = true, scopeDescription = "Permanent Preference")
        }

        return ConflictPreferenceStatus(null, isPermanent = false, scopeDescription = "")
    }

    private fun serializeMap(map: Map<String, Long>): String {
        return map.entries.joinToString(";") { "${it.key}=${it.value}" }
    }

    private fun deserializeMap(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                val k = parts[0].trim()
                val v = parts[1].trim().toLongOrNull()
                if (k.isNotEmpty() && v != null) k to v else null
            } else null
        }.toMap()
    }

    fun isCoursePreferred(courseId: Long): Boolean {
        return _preferredConflictCourseIds.value.contains(courseId)
    }

    fun setHideAuditedInConflict(hide: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_AUDITED_IN_CONFLICT, hide).apply()
        _hideAuditedInConflict.value = hide
    }

    private fun loadPreferredConflictCourseIds(): Set<Long> {
        val raw = prefs.getString(KEY_PREFERRED_CONFLICT_COURSES, null) ?: return emptySet()
        return raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
    }

    private fun loadThemeMode(): AppThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(name ?: AppThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    private fun loadAccentColor(): AppAccentColor {
        val id = prefs.getString(KEY_ACCENT_ID, AppAccents.PINK.id) // Defaults to Pink or Blue
        return AppAccents.fromId(id)
    }

    private val _courseCreditsMap = MutableStateFlow(loadCourseCredits())
    val courseCreditsMap: StateFlow<Map<String, Float>> = _courseCreditsMap.asStateFlow()

    fun getCourseCredits(courseName: String, fallback: Float = 3.0f): Float {
        return _courseCreditsMap.value[courseName] ?: fallback
    }

    fun setCourseCredit(courseName: String, credits: Float) {
        val current = _courseCreditsMap.value.toMutableMap()
        current[courseName] = credits
        _courseCreditsMap.value = current
        saveCourseCredits(current)
    }

    private fun loadCourseCredits(): Map<String, Float> {
        val json = prefs.getString(KEY_COURSE_CREDITS, null) ?: return emptyMap()
        return try {
            val map = mutableMapOf<String, Float>()
            val obj = org.json.JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getDouble(k).toFloat()
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveCourseCredits(map: Map<String, Float>) {
        try {
            val obj = org.json.JSONObject()
            map.forEach { (k, v) -> obj.put(k, v.toDouble()) }
            prefs.edit().putString(KEY_COURSE_CREDITS, obj.toString()).apply()
        } catch (_: Exception) {}
    }

    private val _courseReminderMinutes = MutableStateFlow(loadCourseReminderMinutes())
    val courseReminderMinutes: StateFlow<Map<String, Int>> = _courseReminderMinutes.asStateFlow()

    fun getCourseReminderMinutes(courseName: String): Int? {
        return _courseReminderMinutes.value[courseName]
    }

    fun setCourseReminderMinutes(courseName: String, minutes: Int?) {
        val current = _courseReminderMinutes.value.toMutableMap()
        if (minutes == null) {
            current.remove(courseName)
        } else {
            current[courseName] = minutes
        }
        _courseReminderMinutes.value = current
        saveCourseReminderMinutes(current)
    }

    private fun loadCourseReminderMinutes(): Map<String, Int> {
        val json = prefs.getString(KEY_COURSE_REMINDER_MINUTES, null) ?: return emptyMap()
        return try {
            val map = mutableMapOf<String, Int>()
            val obj = org.json.JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getInt(k)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveCourseReminderMinutes(map: Map<String, Int>) {
        try {
            val obj = org.json.JSONObject()
            map.forEach { (k, v) -> obj.put(k, v) }
            prefs.edit().putString(KEY_COURSE_REMINDER_MINUTES, obj.toString()).apply()
        } catch (_: Exception) {}
    }

    // --- Study Phone Lock Feature ---
    private val _focusLockEndTimeMillis = MutableStateFlow(prefs.getLong(KEY_FOCUS_LOCK_END_TIME, 0L))
    val focusLockEndTimeMillis: StateFlow<Long> = _focusLockEndTimeMillis.asStateFlow()

    private val _focusLockDurationSeconds = MutableStateFlow(prefs.getLong(KEY_FOCUS_LOCK_DURATION, 1500L))
    val focusLockDurationSeconds: StateFlow<Long> = _focusLockDurationSeconds.asStateFlow()

    private val _focusLockCourseName = MutableStateFlow(prefs.getString(KEY_FOCUS_LOCK_COURSE, "") ?: "")
    val focusLockCourseName: StateFlow<String> = _focusLockCourseName.asStateFlow()

    private val _focusWhitelistedPackages = MutableStateFlow(loadFocusWhitelistedPackages())
    val focusWhitelistedPackages: StateFlow<Set<String>> = _focusWhitelistedPackages.asStateFlow()

    fun startFocusLock(durationSeconds: Long, courseName: String = "") {
        val endTime = System.currentTimeMillis() + (durationSeconds * 1000L)
        prefs.edit()
            .putLong(KEY_FOCUS_LOCK_END_TIME, endTime)
            .putLong(KEY_FOCUS_LOCK_DURATION, durationSeconds)
            .putString(KEY_FOCUS_LOCK_COURSE, courseName)
            .apply()
        _focusLockEndTimeMillis.value = endTime
        _focusLockDurationSeconds.value = durationSeconds
        _focusLockCourseName.value = courseName
    }

    fun stopFocusLock() {
        prefs.edit()
            .putLong(KEY_FOCUS_LOCK_END_TIME, 0L)
            .apply()
        _focusLockEndTimeMillis.value = 0L
    }

    fun setFocusWhitelistedPackages(packages: Set<String>) {
        val stringSet = packages.toSet()
        prefs.edit().putStringSet(KEY_FOCUS_WHITELIST_PACKAGES, stringSet).apply()
        _focusWhitelistedPackages.value = stringSet
    }

    private fun loadFocusWhitelistedPackages(): Set<String> {
        val saved = prefs.getStringSet(KEY_FOCUS_WHITELIST_PACKAGES, null)
        if (saved != null && saved.isNotEmpty()) return saved
        return setOf(
            "com.aistudio.courseschedule.kxmpzq",
            "com.example",
            "com.android.calculator2",
            "com.google.android.calculator",
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.settings"
        )
    }

    companion object {
        private const val PREFS_NAME = "myschedule_user_preferences"
        private const val KEY_COURSE_CREDITS = "pref_course_credits"
        private const val KEY_COURSE_REMINDER_MINUTES = "pref_course_reminder_minutes"
        private const val KEY_FOCUS_LOCK_END_TIME = "pref_focus_lock_end_time"
        private const val KEY_FOCUS_LOCK_DURATION = "pref_focus_lock_duration"
        private const val KEY_FOCUS_LOCK_COURSE = "pref_focus_lock_course"
        private const val KEY_FOCUS_WHITELIST_PACKAGES = "pref_focus_whitelist_packages"
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_ACCENT_ID = "pref_accent_id"
        private const val KEY_ONBOARDING_COMPLETED = "pref_onboarding_completed_v1"
        private const val KEY_AUTO_DND = "pref_auto_dnd"
        private const val KEY_CLASS_REMINDER_15M = "pref_class_reminder_15m"
        private const val KEY_CLASS_REMINDER_MINUTES = "pref_class_reminder_minutes"
        private const val KEY_SLEEP_ALARM_ENABLED = "pref_sleep_alarm_enabled"
        private const val KEY_SLEEP_BEDTIME = "pref_sleep_bedtime"
        private const val KEY_SLEEP_WAKE_TIME = "pref_sleep_wake_time"
        private const val KEY_SLEEP_SMART_WAKE = "pref_sleep_smart_wake"
        private const val KEY_SLEEP_WAKE_ADVANCE = "pref_sleep_wake_advance"
        private const val KEY_SLEEP_TARGET_HOURS = "pref_sleep_target_hours"
        private const val KEY_SLEEP_ALARM_VIBRATE = "pref_sleep_alarm_vibrate"
        private const val KEY_SLEEP_ALARM_SOUND = "pref_sleep_alarm_sound"
        private const val KEY_APP_LANGUAGE = "pref_app_language"
        private const val KEY_BILINGUAL = "pref_bilingual_preferred"
        private const val KEY_GEMINI_API_KEY = "pref_gemini_api_key"
        private const val KEY_AI_BASE_URL = "pref_ai_base_url"
        private const val KEY_AI_MODEL_NAME = "pref_ai_model_name"
        private const val KEY_SECTION_TIMINGS = "pref_section_timings"
        private const val KEY_CALENDAR_IMAGE_URI = "pref_calendar_image_uri"
        private const val KEY_PREFERRED_CONFLICT_COURSES = "pref_preferred_conflict_courses"
        private const val KEY_SESSION_COURSE_PRIORITIES = "pref_session_course_priorities"
        private const val KEY_TIME_BLOCK_PERMANENT_PREFERENCES = "pref_time_block_permanent_preferences"
        private const val KEY_HIDE_AUDITED_IN_CONFLICT = "pref_hide_audited_in_conflict"
        private const val KEY_NIGHTLY_SUMMARY_ENABLED = "pref_nightly_summary_enabled"
        private const val KEY_NIGHTLY_SUMMARY_TIME = "pref_nightly_summary_time"

        val defaultSectionTimings = listOf(
            com.example.data.model.SectionTiming(1, "08:00", "08:45"),
            com.example.data.model.SectionTiming(2, "08:50", "09:35"),
            com.example.data.model.SectionTiming(3, "09:50", "10:35"),
            com.example.data.model.SectionTiming(4, "10:40", "11:25"),
            com.example.data.model.SectionTiming(5, "11:30", "12:15"),
            com.example.data.model.SectionTiming(6, "14:30", "15:15"),
            com.example.data.model.SectionTiming(7, "15:20", "16:05"),
            com.example.data.model.SectionTiming(8, "16:20", "17:05"),
            com.example.data.model.SectionTiming(9, "17:10", "17:55"),
            com.example.data.model.SectionTiming(10, "19:00", "19:45"),
            com.example.data.model.SectionTiming(11, "19:50", "20:35"),
            com.example.data.model.SectionTiming(12, "20:40", "21:25")
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
