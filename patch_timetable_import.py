import re

with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

if "import androidx.compose.runtime.collectAsState" not in content:
    content = content.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.collectAsState")
    with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
        f.write(content)
    print("Added import")
else:
    print("Import already exists")
