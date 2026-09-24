import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Add a state for showing DND dialog
if "showDndDialog" not in content:
    content = content.replace("var expanded by remember { mutableStateOf(false) }", "var expanded by remember { mutableStateOf(false) }\n    var showDndDialog by remember { mutableStateOf(false) }")

    dnd_dialog = """
    if (showDndDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDndDialog = false },
            title = { Text("Permission Required".tr) },
            text = { Text("To automatically silence your phone during class hours, you must grant the 'Do Not Disturb' (Notification Policy) access in your device settings.".tr) },
            confirmButton = {
                TextButton(onClick = {
                    showDndDialog = false
                    com.example.service.DndManager.requestDndPermission(context)
                }) {
                    Text("Open Settings".tr)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDndDialog = false }) {
                    Text("Cancel".tr)
                }
            }
        )
    }
"""
    content = content.replace("val scope = rememberCoroutineScope()", "val scope = rememberCoroutineScope()\n" + dnd_dialog)

    switch_logic = """Switch(
                            checked = state.isAutoDndEnabled,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled && !com.example.service.DndManager.checkDndPermission(context)) {
                                    showDndDialog = true
                                } else {
                                    viewModel.toggleAutoDnd(isEnabled)
                                }
                            }
                        )"""
    content = re.sub(r'Switch\([\s\S]*?onCheckedChange = \{ viewModel\.toggleAutoDnd\(it\) \}\n\s*\)', switch_logic, content)

    with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
        f.write(content)
