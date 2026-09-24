import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("fun toggle15MinuteReminder", "fun toggleClassReminder15m")

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
