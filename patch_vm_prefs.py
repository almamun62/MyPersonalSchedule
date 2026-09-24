import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# Add prefs initialization inside the ViewModel class
prefs_init = """    private val prefs = com.example.data.local.UserPreferencesManager.getInstance(application)
    
    private val _isClassReminder15mEnabled = MutableStateFlow(prefs.isClassReminder15mEnabled.value)
    private val _classReminderMinutes = MutableStateFlow(prefs.classReminderMinutes.value)
"""

content = re.sub(r'    private val _isClassReminder15mEnabled = MutableStateFlow\(true\)\n    private val _classReminderMinutes = MutableStateFlow\(15\)', prefs_init, content)

setter_patch = """
    fun setClassReminderMinutes(minutes: Int) {
        prefs.setClassReminderMinutes(minutes)
        _classReminderMinutes.value = minutes
        rescheduleClassRemindersInternal()
    }

    fun toggleClassReminder15m(enabled: Boolean) {
        prefs.setClassReminder15mEnabled(enabled)
        _isClassReminder15mEnabled.value = enabled
        if (enabled) {
            rescheduleClassRemindersInternal()
        }
    }
"""

content = re.sub(r'    fun setClassReminderMinutes\(minutes: Int\) \{[\s\S]*?    \}', '', content)
content = re.sub(r'    fun toggleClassReminder15m\(enabled: Boolean\) \{[\s\S]*?    \}', setter_patch, content)

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
