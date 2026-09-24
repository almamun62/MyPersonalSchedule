import re
with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

target = 'text = "W$weekNum",'
replacement = 'text = if (LocalAppLanguage.current == "zh") "第$weekNum周" else "W$weekNum",'

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
        f.write(content)
    print("Patched Timetable Week Text 2")
else:
    print("Target not found")
