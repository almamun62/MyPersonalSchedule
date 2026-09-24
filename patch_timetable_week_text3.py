import re
with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

target = 'text = "(Current)",'
replacement = 'text = "(${\\"Current\\".tr})",'

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
        f.write(content)
    print("Patched Timetable Week Text 3")
else:
    print("Target not found")
