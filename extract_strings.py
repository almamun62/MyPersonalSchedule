import os, re

files = [
    "app/src/main/java/com/example/ui/screens/ChatScreen.kt",
    "app/src/main/java/com/example/ui/screens/DashboardScreen.kt",
    "app/src/main/java/com/example/ui/screens/SettingsScreen.kt",
    "app/src/main/java/com/example/ui/screens/TasksExamsScreen.kt",
    "app/src/main/java/com/example/ui/screens/TimetableScreen.kt",
    "app/src/main/java/com/example/ui/screens/UsageScreen.kt",
    "app/src/main/java/com/example/ui/components/BootstartOnboardingDialog.kt",
    "app/src/main/java/com/example/ui/components/ClassNotebookDialog.kt",
    "app/src/main/java/com/example/ui/components/HyperIsland.kt",
    "app/src/main/java/com/example/ui/components/ScheduleDialogs.kt",
    "app/src/main/java/com/example/ui/components/SpreadsheetImportDialog.kt",
    "app/src/main/java/com/example/ui/components/TutorialDialog.kt",
    "app/src/main/java/com/example/MainActivity.kt"
]

strings = set()
for f in files:
    try:
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
            # Match Text(text = "...") or Text("...") or label = { Text("...") }
            matches = re.findall(r'Text\s*\(\s*(?:text\s*=\s*)?"([^"\$]+)"', content)
            strings.update(matches)
            
            # Match OutlinedTextField(label = { Text("...") }
            # Match contentDescription = "..."
            matches_desc = re.findall(r'contentDescription\s*=\s*"([^"\$]+)"', content)
            strings.update(matches_desc)
            
            # generic strings that are likely UI text (camel case or spaces)
    except FileNotFoundError:
        pass

for s in sorted(list(strings)):
    if len(s) > 1 and not s.startswith("nav_") and not s.startswith("http"):
        print(s)
