import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """                    ScheduleScreen.CHAT -> ChatScreen(scheduleViewModel = viewModel)
                    ScheduleScreen.USAGE -> UsageScreen(state = state)"""
replacement = """                    ScheduleScreen.CHAT -> ChatScreen(scheduleViewModel = viewModel)
                    ScheduleScreen.NOTES -> com.example.ui.screens.NotesScreen(state = state, viewModel = viewModel)
                    ScheduleScreen.USAGE -> UsageScreen(state = state)"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
print("Updated pager")
