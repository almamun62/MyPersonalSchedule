package com.example.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.ui.theme.tr

@Composable
fun CourseAlertSettingsDialog(
    globalAdvanceMinutes: Int,
    courses: List<CourseEntity>,
    courseSpecificAlerts: Map<String, Int>,
    onDismiss: () -> Unit,
    onSaveGlobalMinutes: (Int) -> Unit,
    onSetCourseAlert: (courseName: String, minutes: Int?) -> Unit
) {
    var selectedGlobalMinutes by remember { mutableIntStateOf(globalAdvanceMinutes) }
    val uniqueCourses = remember(courses) { courses.distinctBy { it.name } }
    val presetMinutes = listOf(5, 10, 15, 20, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Class Start Alert Settings".tr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize advance reminder time globally & per course".tr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Global Reminder Timing
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Default Global Alert".tr,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$selectedGlobalMinutes ${"min before".tr}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Preset chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetMinutes.take(4).forEach { min ->
                                FilterChip(
                                    selected = selectedGlobalMinutes == min,
                                    onClick = { selectedGlobalMinutes = min },
                                    label = { Text("${min}m", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetMinutes.drop(4).forEach { min ->
                                FilterChip(
                                    selected = selectedGlobalMinutes == min,
                                    onClick = { selectedGlobalMinutes = min },
                                    label = { Text("${min}m", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Course-Wise Custom Alerts
                Text(
                    text = "Course-Wise Alert Lead Times:".tr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (uniqueCourses.isEmpty()) {
                    Text(
                        text = "No enrolled courses yet.".tr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uniqueCourses, key = { it.name }) { course ->
                            val currentCourseAlert = courseSpecificAlerts[course.name]
                            var expanded by remember { mutableStateOf(false) }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expanded = !expanded },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = course.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.5.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = if (currentCourseAlert == null) {
                                                    "${"Default".tr} (${selectedGlobalMinutes}m)"
                                                } else if (currentCourseAlert <= 0) {
                                                    "Muted (No alert)".tr
                                                } else {
                                                    "$currentCourseAlert ${"min before".tr}"
                                                },
                                                fontSize = 11.5.sp,
                                                color = if (currentCourseAlert != null && currentCourseAlert <= 0)
                                                    MaterialTheme.colorScheme.error
                                                else
                                                    MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Icon(
                                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (expanded) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Choices: Default, 5m, 10m, 15m, 20m, 30m, 45m, Mute
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = currentCourseAlert == null,
                                                onClick = {
                                                    onSetCourseAlert(course.name, null)
                                                    expanded = false
                                                },
                                                label = { Text("Default".tr, fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            listOf(10, 20, 30).forEach { m ->
                                                FilterChip(
                                                    selected = currentCourseAlert == m,
                                                    onClick = {
                                                        onSetCourseAlert(course.name, m)
                                                        expanded = false
                                                    },
                                                    label = { Text("${m}m", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(45, 60).forEach { m ->
                                                FilterChip(
                                                    selected = currentCourseAlert == m,
                                                    onClick = {
                                                        onSetCourseAlert(course.name, m)
                                                        expanded = false
                                                    },
                                                    label = { Text("${m}m", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            FilterChip(
                                                selected = currentCourseAlert != null && currentCourseAlert <= 0,
                                                onClick = {
                                                    onSetCourseAlert(course.name, 0)
                                                    expanded = false
                                                },
                                                label = { Text("Mute".tr, fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveGlobalMinutes(selectedGlobalMinutes)
                    onDismiss()
                }
            ) {
                Text("Save & Apply".tr)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel".tr)
            }
        }
    )
}
