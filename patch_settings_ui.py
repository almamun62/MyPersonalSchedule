import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Replace "15-Minute Advance Reminder" with "Advance Class Reminder"
content = content.replace('"15-Minute Advance Reminder"', '"Advance Class Reminder"')
content = content.replace('state.isClassReminder15mEnabled', 'state.isClassReminder15mEnabled') # Keep the boolean for now
content = content.replace('viewModel.toggle15MinuteReminder(it)', 'viewModel.toggleClassReminder15m(it)') # Assuming it's toggleClassReminder15m or similar

# Add the minutes selector underneath the switch
minutes_ui = """
                    if (state.isClassReminder15mEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reminder Time (Minutes Before)".tr, style = MaterialTheme.typography.bodyMedium)
                            
                            var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                            androidx.compose.foundation.layout.Box {
                                androidx.compose.material3.OutlinedButton(onClick = { expanded = true }) {
                                    Text("${state.classReminderMinutes} mins")
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf(5, 10, 15, 30, 45, 60).forEach { mins ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text("$mins mins") },
                                            onClick = {
                                                viewModel.setClassReminderMinutes(mins)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
"""

content = re.sub(r'Switch\([\s\S]*?onCheckedChange = \{ viewModel\.toggleClassReminder15m\(it\) \}\n\s*\)', r'Switch(\n                            checked = state.isClassReminder15mEnabled,\n                            onCheckedChange = { viewModel.toggleClassReminder15m(it) }\n                        )', content)

# I need to insert `minutes_ui` right after the Row that contains the Switch.
content = re.sub(r'(Switch\([\s\S]*?\)\n\s*\})', r'\1' + minutes_ui, content)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
