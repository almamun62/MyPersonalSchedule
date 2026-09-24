import re
with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

target = 'text = "${activeCoursesInWeek.size} classes scheduled this week",'
replacement = 'text = "${activeCoursesInWeek.size} ${"classes scheduled this week".tr}",'

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
        f.write(content)
    print("Patched Timetable Week Text 4")
else:
    print("Target not found")
