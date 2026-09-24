import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

bad_str = """                            Text("Auto-DND on Class Start".tr, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Automatically silences distractions and keeps DND active through consecutive lectures".tr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isClassReminder15mEnabled,
                            onCheckedChange = { viewModel.toggleClassReminder15m(it) }
                        )
                    }"""

good_str = """                            Text("Auto-DND on Class Start".tr, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Automatically silences distractions and keeps DND active through consecutive lectures".tr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isAutoDndEnabled,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled && !com.example.service.DndManager.checkDndPermission(context)) {
                                    showDndDialog = true
                                } else {
                                    viewModel.toggleAutoDnd(isEnabled)
                                }
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Advance Class Reminder".tr, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (state.isClassReminder15mEnabled) "Armed (${state.scheduledRemindersCount} alarms scheduled in system)" else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isClassReminder15mEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = state.isClassReminder15mEnabled,
                            onCheckedChange = { viewModel.toggleClassReminder15m(it) }
                        )
                    }"""

content = content.replace(bad_str, good_str)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
