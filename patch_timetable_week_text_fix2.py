import re
with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('com.example.ui.theme.com.example.ui.theme.LocalAppLanguage', 'com.example.ui.theme.LocalAppLanguage')

with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
    f.write(content)
print("Patched TimetableScreen double package")
