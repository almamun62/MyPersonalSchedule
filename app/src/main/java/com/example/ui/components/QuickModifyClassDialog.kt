package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.data.local.UserPreferencesManager
import com.example.ui.theme.tr

@Composable
fun QuickModifyClassDialog(
    course: CourseEntity,
    onDismiss: () -> Unit,
    onSave: (
        newClassroom: String,
        newStartPeriod: Int,
        newEndPeriod: Int,
        newStartTime: String,
        newEndTime: String,
        isPermanent: Boolean
    ) -> Unit
) {
    var classroom by remember { mutableStateOf(course.classroom) }
    var startPeriod by remember { mutableIntStateOf(course.startPeriod) }
    var endPeriod by remember { mutableIntStateOf(course.endPeriod) }
    var startTime by remember { mutableStateOf(course.startTime) }
    var endTime by remember { mutableStateOf(course.endTime) }
    var isPermanent by remember { mutableStateOf(true) }

    val periodPresets = listOf(
        Pair(1, 2) to "08:00 - 09:35 (Periods 1-2)",
        Pair(3, 4) to "09:50 - 11:25 (Periods 3-4)",
        Pair(5, 6) to "13:30 - 15:05 (Periods 5-6)",
        Pair(7, 8) to "15:20 - 16:55 (Periods 7-8)",
        Pair(9, 10) to "18:30 - 20:05 (Periods 9-10)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Sudden Classroom / Time Change".tr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Update the room or lecture time if your teacher made a sudden schedule adjustment.".tr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Classroom Input
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { Text("New Classroom / Location".tr) },
                    placeholder = { Text("e.g. Teaching Bldg 3-402 or Online".tr) },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick location chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Online Meeting".tr, "Lab /机房".tr, "Auditorium".tr).forEach { loc ->
                        AssistChip(
                            onClick = { classroom = loc },
                            label = { Text(loc, fontSize = 11.sp) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 2. Class Time / Period Selector
                Text(
                    text = "Select New Time Period:".tr,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    periodPresets.forEach { (periods, label) ->
                        val isSelected = startPeriod == periods.first && endPeriod == periods.second
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                startPeriod = periods.first
                                endPeriod = periods.second
                                val times = label.split("(")[0].trim().split("-")
                                if (times.size == 2) {
                                    startTime = times[0].trim()
                                    endTime = times[1].trim()
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Custom time row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time".tr, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time".tr, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Scope selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPermanent = !isPermanent }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = isPermanent,
                        onCheckedChange = { isPermanent = it }
                    )
                    Column {
                        Text(
                            text = if (isPermanent) "Update Schedule for All Remaining Weeks".tr else "Temporary Adjustment for Today Only".tr,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isPermanent) "Updates course database & future notifications".tr else "Only today's reminders and alert card".tr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(classroom.trim(), startPeriod, endPeriod, startTime.trim(), endTime.trim(), isPermanent)
                },
                enabled = classroom.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()
            ) {
                Text("Apply Change".tr)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel".tr)
            }
        }
    )
}
