import re
with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('if (LocalAppLanguage.current == "zh") "第$weekNum周"', 'if (com.example.ui.theme.LocalAppLanguage.current == "zh") "第${weekNum}周"')
content = content.replace('LocalAppLanguage.current', 'com.example.ui.theme.LocalAppLanguage.current')

with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
    f.write(content)
print("Patched TimetableScreen errors")
