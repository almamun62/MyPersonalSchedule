package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HolidayOverrideEntity
import com.example.data.model.HolidayOverrideType
import com.example.ui.components.AddHolidayDialog
import com.example.ui.viewmodel.ScheduleUiState
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun SettingsScreen(
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddHolidayDialog by remember { mutableStateOf(false) }
    var totalWeeksText by remember(state.activeSemester) {
        mutableStateOf((state.activeSemester?.totalWeeks ?: 16).toString())
    }

    if (showAddHolidayDialog) {
        AddHolidayDialog(
            onDismiss = { showAddHolidayDialog = false },
            onConfirm = { override ->
                viewModel.addHolidayOverride(override)
                showAddHolidayDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // 1. Zero Network & Privacy Guarantee
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "100% Offline-First Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Zero network calls, analytics, or remote tracking. All course schedules, exams, and tasks reside strictly in your device's encrypted Room SQLite database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 2. Semester Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Academic Semester Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val semStartDate = state.activeSemester?.startDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    } ?: "2026-09-01"

                    Text("Term Name: ${state.activeSemester?.name ?: "Fall 2026 Term"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Semester Start Date: $semStartDate", style = MaterialTheme.typography.bodyMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = totalWeeksText,
                            onValueChange = { totalWeeksText = it },
                            label = { Text("Total Weeks in Term") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val weeks = totalWeeksText.toIntOrNull() ?: 16
                                state.activeSemester?.let { sem ->
                                    viewModel.updateSemester(sem.startDateMillis, weeks)
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        // 3. Automation & DND Settings
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Do Not Disturb & Automation Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notification Policy Access", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (state.isDndPermissionGranted) "Granted • Able to control DND filter" else "Not granted • Tap to open system settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isDndPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        if (!state.isDndPermissionGranted) {
                            Button(onClick = { viewModel.requestDndPermission(context) }) {
                                Text("Grant")
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-DND on Class Start", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Automatically silences distractions and keeps DND active through consecutive lectures",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isAutoDndEnabled,
                            onCheckedChange = { viewModel.toggleAutoDnd(it) }
                        )
                    }

                    HorizontalDivider()

                    Text("Manual Diagnostic & Test Actions", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.testStickyNotification(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Sticky Alert", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.testNagNotification(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Nag Loop", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 4. Chinese Holiday & Weekend Make-up Day (调休) Overrides
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Holiday & Make-up Engine (调休)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auto-pauses on holidays or substitutes weekend schedules",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = { showAddHolidayDialog = true },
                    modifier = Modifier.testTag("add_holiday_override_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Override")
                }
            }
        }

        if (state.holidayOverrides.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text("No holiday overrides set. Tap + to add holidays or weekend make-up days.")
                    }
                }
            }
        } else {
            items(state.holidayOverrides, key = { it.id }) { override ->
                HolidayOverrideRow(
                    override = override,
                    onDelete = { viewModel.deleteHolidayOverride(override) }
                )
            }
        }
    }
}

@Composable
private fun HolidayOverrideRow(
    override: HolidayOverrideEntity,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (override.type == HolidayOverrideType.HOLIDAY) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (override.type == HolidayOverrideType.HOLIDAY) Icons.Default.BeachAccess else Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = if (override.type == HolidayOverrideType.HOLIDAY) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = override.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Date: ${override.dateString} • ${if (override.type == HolidayOverrideType.HOLIDAY) "Classes Paused" else "Make-up Day (Follows day ${override.targetDayOfWeek})"} ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
