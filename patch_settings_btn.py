import re
with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'Text(if (geminiKey.isNullOrEmpty()) "Set Up" else "Edit Settings")',
    'Text(if (geminiKey.isNullOrEmpty()) "Set Up".tr else "Edit Settings".tr)'
)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
