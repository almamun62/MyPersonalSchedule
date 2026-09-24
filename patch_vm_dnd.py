import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# Make rescheduleClassRemindersInternal call DndAutomationScheduler.scheduleDndAlarms(context)
replacement = """    private fun rescheduleClassRemindersInternal() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val scheduled = ClassReminderScheduler.scheduleUpcomingAlarms(context)
            _scheduledRemindersCount.value = scheduled.size
            com.example.service.DndAutomationScheduler.scheduleDndAlarms(context)
        }
    }"""

# Find existing rescheduleClassRemindersInternal
match = re.search(r'    private fun rescheduleClassRemindersInternal\(\) \{\n.*?\}', content, re.DOTALL)
if match:
    content = content.replace(match.group(0), replacement)
else:
    # Attempt a more flexible match
    match = re.search(r'    private fun rescheduleClassRemindersInternal\(\) \{[\s\S]*?\}[\s\S]*?\}', content)
    if match:
        content = content.replace(match.group(0), replacement)


with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
