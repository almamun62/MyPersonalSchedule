with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

target = "            Card(\n                shape = RoundedCornerShape(16.dp),"
replacement = "            Card(\n                modifier = Modifier.fillMaxWidth(),\n                shape = RoundedCornerShape(16.dp),"

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)

print("Settings replaced")
