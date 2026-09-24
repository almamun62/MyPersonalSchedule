import re

with open('app/src/main/java/com/example/service/ClassReminderReceiver.kt', 'r') as f:
    content = f.read()

# Add DndAutomationScheduler
content = content.replace("ClassReminderScheduler.scheduleUpcomingAlarms(context)", """ClassReminderScheduler.scheduleUpcomingAlarms(context)
                        com.example.service.DndAutomationScheduler.scheduleDndAlarms(context)""")

with open('app/src/main/java/com/example/service/ClassReminderReceiver.kt', 'w') as f:
    f.write(content)
