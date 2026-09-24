import re

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'r') as f:
    content = f.read()

# Remove the inline DND toggling since the AlarmManager receiver handles it now
content = content.replace("""            if (state.isAutoDndEnabled && state.isDndPermissionGranted && !state.isDndActive) {
                DndManager.setDnd(context, enable = true)
            }""", "")

content = content.replace("""                // Restore DND if we had auto-enabled it
                if (state.isAutoDndEnabled && state.isDndPermissionGranted && state.isDndActive) {
                    DndManager.setDnd(context, enable = false)
                }""", "")

with open('app/src/main/java/com/example/ui/viewmodel/ScheduleViewModel.kt', 'w') as f:
    f.write(content)
