import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("val ticker = values[9] as Long", "val ticker = values[10] as Long")

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)

