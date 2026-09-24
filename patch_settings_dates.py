import re
with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'Text("Term Name: ${state.activeSemester?.name ?: "Fall 2026 Term"}", style = MaterialTheme.typography.bodyMedium)',
    'Text("${"Term Name: ".tr}${state.activeSemester?.name ?: "Fall 2026 Term".tr}", style = MaterialTheme.typography.bodyMedium)'
)
content = content.replace(
    'Text("Semester Start Date: $semStartDate", style = MaterialTheme.typography.bodyMedium)',
    'Text("${"Semester Start Date: ".tr}$semStartDate", style = MaterialTheme.typography.bodyMedium)'
)
content = content.replace(
    'Text("${state.classReminderMinutes} mins")',
    'Text("${state.classReminderMinutes} ${"mins".tr}")'
)
content = content.replace(
    'text = { Text("$mins mins") },',
    'text = { Text("$mins ${"mins".tr}") },'
)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
