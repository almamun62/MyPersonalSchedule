import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

dialog_code = """        com.example.ui.components.SmartPill(
        isDndActive = state.isDndActive,
        currentWeek = state.currentAcademicWeek,
        modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
    )

    val conflictAlert by viewModel.courseConflictAlert.collectAsState()
    if (conflictAlert != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissConflictAlert() },
            title = { androidx.compose.material3.Text(text = "Schedule Conflict Detected") },
            text = { androidx.compose.material3.Text(text = conflictAlert!!.summary + "\\n\\n" + conflictAlert!!.recommendation) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissConflictAlert() }) {
                    androidx.compose.material3.Text("OK")
                }
            }
        )
    }"""

content = content.replace("""        com.example.ui.components.SmartPill(
        isDndActive = state.isDndActive,
        currentWeek = state.currentAcademicWeek,
        modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
    )""", dialog_code)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

