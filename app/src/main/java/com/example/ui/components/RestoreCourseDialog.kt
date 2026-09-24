package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferencesManager
import com.example.data.model.CourseEntity
import com.example.data.model.SectionTiming
import com.example.ui.theme.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreCourseDialog(
    course: CourseEntity,
    onDismiss: () -> Unit,
    onRestore: (dayOfWeek: Int, startPeriod: Int, endPeriod: Int, startTime: String, endTime: String) -> Unit
) {
    var selectedDay by remember { mutableStateOf(1) }
    var startPeriod by remember { mutableStateOf(1) }
    var endPeriod by remember { mutableStateOf(2) }

    val sectionTimings: List<SectionTiming> = remember { UserPreferencesManager.defaultSectionTimings }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var dayExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Course to Timetable".tr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Assign a timetable slot to \"${course.name}\" to resume normal attendance tracking and notifications.".tr,
                    style = MaterialTheme.typography.bodySmall
                )

                // Day Selection
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = it }
                ) {
                    OutlinedTextField(
                        value = daysOfWeek[selectedDay - 1],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day of Week".tr) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        daysOfWeek.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedDay = index + 1
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }

                // Period Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startPeriod.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull()
                            if (v != null && v in 1..12) {
                                startPeriod = v
                                if (endPeriod < v) endPeriod = v
                            }
                        },
                        label = { Text("Start (1-12)".tr) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endPeriod.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull()
                            if (v != null && v in startPeriod..12) {
                                endPeriod = v
                            }
                        },
                        label = { Text("End (1-12)".tr) },
                        modifier = Modifier.weight(1f)
                    )
                }

                val startTime = sectionTimings.getOrNull(startPeriod - 1)?.startTime ?: "08:00"
                val endTime = sectionTimings.getOrNull(endPeriod - 1)?.endTime ?: "09:35"
                Text(
                    text = "Time: $startTime - $endTime",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startTime = sectionTimings.getOrNull(startPeriod - 1)?.startTime ?: "08:00"
                    val endTime = sectionTimings.getOrNull(endPeriod - 1)?.endTime ?: "09:35"
                    onRestore(selectedDay, startPeriod, endPeriod, startTime, endTime)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Restore Class".tr)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel".tr)
            }
        }
    )
}
