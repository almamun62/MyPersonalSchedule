import re
with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# Replace totalWeeks = 18 with totalWeeks = 20
content = content.replace("totalWeeks < 18", "totalWeeks < 20")
content = content.replace("totalWeeks = 18", "totalWeeks = 20")

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
