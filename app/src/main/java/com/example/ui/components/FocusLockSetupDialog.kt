package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.tr

@Composable
fun FocusLockSetupDialog(
    initialCourseName: String = "",
    availableCourses: List<String> = emptyList(),
    whitelistedAppsCount: Int,
    onOpenWhitelistPicker: () -> Unit,
    onDismiss: () -> Unit,
    onStartLock: (durationSeconds: Long, courseName: String) -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(45) }
    var seconds by remember { mutableIntStateOf(0) }
    var selectedCourse by remember { mutableStateOf(initialCourseName) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val totalSeconds = (hours * 3600L) + (minutes * 60L) + seconds

    val presets = listOf(
        Triple(0, 25, "25m Pomodoro"),
        Triple(0, 45, "45m Deep Study"),
        Triple(1, 0, "1h Lecture Focus"),
        Triple(1, 30, "1.5h Exam Cram"),
        Triple(2, 0, "2h Heavy Focus")
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Strict Study Lock Notice".tr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                val timeStr = buildString {
                    if (hours > 0) append("${hours}h ")
                    if (minutes > 0) append("${minutes}m ")
                    if (seconds > 0 || (hours == 0 && minutes == 0)) append("${seconds}s")
                }.trim()
                Text(
                    text = "Are you sure you want to lock your phone for $timeStr? Once locked, there is NO WAY to unlock early until the timer finishes (emergency 5s hold required to exit).".tr,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onStartLock(totalSeconds, selectedCourse)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Lock Phone".tr)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel".tr)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "Lock Phone for Study".tr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Strict Warning Banner
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Warning: Once locked, no early unlock until time finishes!".tr,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Large Time Display XX:XX:XX with Vertical Steppers
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LOCK DURATION (HH:MM:SS)".tr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Compact Vertical Steppers for Hours, Minutes, Seconds
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeStepperColumn(
                                label = "Hours".tr,
                                value = hours,
                                onDecrement = { if (hours > 0) hours-- },
                                onIncrement = { if (hours < 12) hours++ }
                            )

                            Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            TimeStepperColumn(
                                label = "Minutes".tr,
                                value = minutes,
                                onDecrement = { if (minutes > 0) minutes = (minutes - 5).coerceAtLeast(0) },
                                onIncrement = { if (minutes < 55) minutes = (minutes + 5).coerceAtMost(55) }
                            )

                            Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            TimeStepperColumn(
                                label = "Seconds".tr,
                                value = seconds,
                                onDecrement = { if (seconds > 0) seconds = (seconds - 15).coerceAtLeast(0) },
                                onIncrement = { if (seconds < 45) seconds = (seconds + 15).coerceAtMost(45) }
                            )
                        }
                    }
                }

                // Quick Presets
                Text(
                    text = "Quick Presets:".tr,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(3).forEach { (h, m, label) ->
                        val isSelected = hours == h && minutes == m && seconds == 0
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                hours = h
                                minutes = m
                                seconds = 0
                            },
                            label = { Text("${if (h > 0) "${h}h " else ""}${m}m", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Allowed Apps Selector button
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWhitelistPicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Whitelisted Study Apps".tr,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = "$whitelistedAppsCount ${"apps allowed during lock".tr}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Course Selection (optional)
                if (availableCourses.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Course Being Studied (Optional):".tr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableCourses.take(2).forEach { course ->
                                FilterChip(
                                    selected = selectedCourse == course,
                                    onClick = { selectedCourse = if (selectedCourse == course) "" else course },
                                    label = { Text(course, maxLines = 1, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (totalSeconds > 0) {
                        showConfirmDialog = true
                    }
                },
                enabled = totalSeconds > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lock Phone Now".tr)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel".tr)
            }
        }
    )
}

@Composable
private fun TimeStepperColumn(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase", modifier = Modifier.size(20.dp))
        }

        Text(
            text = String.format("%02d", value),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        IconButton(
            onClick = onDecrement,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease", modifier = Modifier.size(20.dp))
        }

        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
