import re
with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

target = 'text = "Week $selectedWeek",'
replacement = 'text = if (LocalAppLanguage.current == "zh") "第 $selectedWeek 周" else "Week $selectedWeek",'

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
        f.write(content)
    print("Patched Timetable Week Text")
else:
    print("Target not found")
