import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

setter = """
    fun setClassReminderMinutes(minutes: Int) {
        _classReminderMinutes.value = minutes
        rescheduleClassRemindersInternal()
    }
"""

content = content.replace("fun toggleClassReminder15m", setter + "\n    fun toggleClassReminder15m")

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
