import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

if "showDndDialog" not in content:
    content = content.replace("val scope = rememberCoroutineScope()", "val scope = rememberCoroutineScope()\n    var showDndDialog by remember { androidx.compose.runtime.mutableStateOf(false) }")
    
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
    # Insert dialog near the top of DashboardScreen content
    content = content.replace("Scaffold(", dnd_dialog + "\n    Scaffold(")

    toggle_logic = """onToggleAuto = { isEnabled ->
                    if (isEnabled && !com.example.service.DndManager.checkDndPermission(context)) {
                        showDndDialog = true
                    } else {
                        viewModel.toggleAutoDnd(isEnabled)
                    }
                }"""
    content = re.sub(r'onToggleAuto = \{ viewModel\.toggleAutoDnd\(it\) \}', toggle_logic, content)

    with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
        f.write(content)
