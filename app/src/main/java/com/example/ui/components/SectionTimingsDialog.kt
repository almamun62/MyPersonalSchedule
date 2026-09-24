package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPreferencesManager
import com.example.data.model.SectionTiming
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.tr

@Composable
fun SectionTimingsDialog(
    currentTimings: List<SectionTiming>,
    onDismiss: () -> Unit,
    onSave: (List<SectionTiming>) -> Unit
) {
    var editedTimings by remember { mutableStateOf(currentTimings) }

    val swpuTimings = UserPreferencesManager.defaultSectionTimings

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Class Period Schedule".tr,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Southwest Petroleum University • Fall 2026-2027",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Preset Banner / Reset button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SWPU Fall 2026-2027 Preset".tr,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Standard 12 Periods (45m each)".tr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        OutlinedButton(
                            onClick = { editedTimings = swpuTimings },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset".tr, fontSize = 12.sp)
                        }
                    }
                }

                // 1. Morning (Periods 1 - 5)
                PeriodGroupCard(
                    title = "上午 (Morning)",
                    subtitle = "08:00 - 12:15",
                    icon = Icons.Default.WbSunny,
                    accentColor = Color(0xFFE65100),
                    containerColor = Color(0xFFFFF3E0).copy(alpha = 0.6f),
                    timings = editedTimings.filter { it.section in 1..5 },
                    onTimingChanged = { updatedTiming ->
                        editedTimings = editedTimings.map { if (it.section == updatedTiming.section) updatedTiming else it }
                    },
                    breakNote = "Intermission: 09:35-09:50 (15 min) • Lunch: 12:15-14:30"
                )

                // 2. Afternoon (Periods 6 - 9)
                PeriodGroupCard(
                    title = "下午 (Afternoon)",
                    subtitle = "14:30 - 17:55",
                    icon = Icons.Default.WbTwilight,
                    accentColor = Color(0xFF1565C0),
                    containerColor = Color(0xFFE3F2FD).copy(alpha = 0.6f),
                    timings = editedTimings.filter { it.section in 6..9 },
                    onTimingChanged = { updatedTiming ->
                        editedTimings = editedTimings.map { if (it.section == updatedTiming.section) updatedTiming else it }
                    },
                    breakNote = "Break: 16:05-16:20 (15 min) • Dinner: 17:55-19:00"
                )

                // 3. Evening (Periods 10 - 12)
                PeriodGroupCard(
                    title = "晚上 (Evening)",
                    subtitle = "19:00 - 21:25",
                    icon = Icons.Default.NightlightRound,
                    accentColor = Color(0xFF4A148C),
                    containerColor = Color(0xFFF3E5F5).copy(alpha = 0.6f),
                    timings = editedTimings.filter { it.section in 10..12 },
                    onTimingChanged = { updatedTiming ->
                        editedTimings = editedTimings.map { if (it.section == updatedTiming.section) updatedTiming else it }
                    },
                    breakNote = "5 min break between periods"
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(editedTimings) }) {
                Text("Save".tr)
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
private fun PeriodGroupCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    timings: List<SectionTiming>,
    onTimingChanged: (SectionTiming) -> Unit,
    breakNote: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = accentColor.copy(alpha = 0.15f))

            timings.forEach { timing ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.width(68.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val currentLang = LocalAppLanguage.current
                            val isEnglish = currentLang == "en" || !currentLang.startsWith("zh")
                            val primaryText = if (isEnglish) "Period ${timing.section}" else "第${timing.section}节"
                            val secondaryText = if (isEnglish) "P${timing.section}" else "Period ${timing.section}"
                            Text(
                                text = primaryText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = secondaryText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = timing.startTime,
                        onValueChange = { newTime ->
                            onTimingChanged(timing.copy(startTime = newTime))
                        },
                        label = { Text("Start".tr, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = timing.endTime,
                        onValueChange = { newTime ->
                            onTimingChanged(timing.copy(endTime = newTime))
                        },
                        label = { Text("End".tr, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Text(
                text = "ℹ️ $breakNote",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

