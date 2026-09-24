with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# Let's just fix the function rescheduleClassRemindersInternal directly.
correct_func = """    private fun rescheduleClassRemindersInternal() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<android.app.Application>()
            val scheduled = com.example.service.ClassReminderScheduler.scheduleUpcomingAlarms(context)
            _scheduledRemindersCount.value = scheduled.size
            com.example.service.DndAutomationScheduler.scheduleDndAlarms(context)
        }
    }"""

# Replace from `private fun rescheduleClassRemindersInternal` to the first `    fun addCourse`
import re
content = re.sub(r'    private fun rescheduleClassRemindersInternal\(\) \{.*?    fun test15MinuteReminder', correct_func + "\n\n    fun test15MinuteReminder", content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)

