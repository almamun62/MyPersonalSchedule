import re

dnd_dialog = """
    if (showDndDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDndDialog = false },
            title = { androidx.compose.material3.Text(com.example.ui.theme.tr("Permission Required")) },
            text = { androidx.compose.material3.Text(com.example.ui.theme.tr("To automatically silence your phone during class hours, you must grant the 'Do Not Disturb' (Notification Policy) access in your device settings.")) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDndDialog = false
                    com.example.service.DndManager.requestDndPermission(context)
                }) {
                    androidx.compose.material3.Text(com.example.ui.theme.tr("Open Settings"))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDndDialog = false }) {
                    androidx.compose.material3.Text(com.example.ui.theme.tr("Cancel"))
                }
            }
        )
    }
"""

def inject_ui(file_path):
    with open(file_path, 'r') as f:
        content = f.read()

    # Prepend the dialog before the Scaffold
    content = content.replace("    Scaffold(", dnd_dialog + "    Scaffold(")
    # Some might use Scaffold( directly
    if dnd_dialog not in content:
        content = content.replace("Scaffold(", dnd_dialog + "\n    Scaffold(")

    with open(file_path, 'w') as f:
        f.write(content)

inject_ui('app/src/main/java/com/example/ui/screens/DashboardScreen.kt')
inject_ui('app/src/main/java/com/example/ui/screens/SettingsScreen.kt')
