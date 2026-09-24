import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

if "classReminderMinutes" not in content:
    content = content.replace(
        "val isClassReminder15mEnabled: Boolean = true,",
        "val isClassReminder15mEnabled: Boolean = true,\n    val classReminderMinutes: Int = 15,"
    )
    
    content = content.replace(
        "private val _isClassReminder15mEnabled = MutableStateFlow(true)",
        "private val _isClassReminder15mEnabled = MutableStateFlow(true)\n    private val _classReminderMinutes = MutableStateFlow(15)"
    )
    
    content = content.replace(
        "_isClassReminder15mEnabled,",
        "_isClassReminder15mEnabled,\n                _classReminderMinutes,"
    )

    # In combine block
    content = re.sub(
        r'(is15mEnabled) = values',
        r'\1 = values[6] as Boolean\n                val reminderMins = values[7] as Int',
        content
    )
    
    # Actually wait, `values` index might be tricky. Let's see the combine statement.
