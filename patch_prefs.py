import re

with open('app/src/main/java/com/example/data/local/UserPreferencesManager.kt', 'r') as f:
    content = f.read()

if "KEY_CLASS_REMINDER_MINUTES" not in content:
    content = content.replace(
        "private const val KEY_CLASS_REMINDER_15M = \"pref_class_reminder_15m\"",
        "private const val KEY_CLASS_REMINDER_15M = \"pref_class_reminder_15m\"\n        private const val KEY_CLASS_REMINDER_MINUTES = \"pref_class_reminder_minutes\""
    )

    minutes_prop = """
    private val _classReminderMinutes = MutableStateFlow(prefs.getInt(KEY_CLASS_REMINDER_MINUTES, 15))
    val classReminderMinutes: StateFlow<Int> = _classReminderMinutes.asStateFlow()

    fun setClassReminderMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_CLASS_REMINDER_MINUTES, minutes).apply()
        _classReminderMinutes.value = minutes
    }
"""
    content = content.replace("fun setClassReminder15mEnabled", minutes_prop + "    fun setClassReminder15mEnabled")

    with open('app/src/main/java/com/example/data/local/UserPreferencesManager.kt', 'w') as f:
        f.write(content)
