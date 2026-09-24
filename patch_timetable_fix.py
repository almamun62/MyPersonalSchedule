import re

with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'r') as f:
    content = f.read()

target = """    val conflictAlert by viewModel.courseConflictAlert.collectAsStateWithLifecycle()
    if (conflictAlert != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissConflictAlert() },
            title = { Text("Scheduling Conflict".tr) },
            text = { Text("A scheduling conflict was detected between ${conflictAlert!!.course1.name} and ${conflictAlert!!.course2.name}. Please review your timetable.".tr) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissConflictAlert() }) {
                    Text("OK".tr)
                }
            }
        )
    }"""

replacement = """    val conflictAlert by viewModel.courseConflictAlert.collectAsState()
    val currentConflict = conflictAlert
    if (currentConflict != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissConflictAlert() },
            title = { Text("Scheduling Conflict".tr) },
            text = { Text("A scheduling conflict was detected between ${currentConflict.course1.name} and ${currentConflict.course2.name}. Please review your timetable.".tr) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissConflictAlert() }) {
                    Text("OK".tr)
                }
            }
        )
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/screens/TimetableScreen.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Not found")
