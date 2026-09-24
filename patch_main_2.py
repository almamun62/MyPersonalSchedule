import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """            com.example.ui.components.SmartPill(
        isDndActive = state.isDndActive,
        currentWeek = state.currentAcademicWeek,
        modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
    )"""

replacement = """            com.example.ui.components.SmartPill(
                isDndActive = state.isDndActive,
                currentWeek = state.currentAcademicWeek,
                ongoingCourseName = ongoingCourse
            )
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Target not found")
