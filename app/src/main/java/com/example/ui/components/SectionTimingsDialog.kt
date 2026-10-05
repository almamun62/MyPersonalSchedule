package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPreferencesManager
import com.example.data.model.SectionTiming
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
                    text = "SWPU Timetable Preset • 12 Periods",
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
                OutlinedButton(
                    onClick = { editedTimings = swpuTimings },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset to Official SWPU Preset".tr)
                }

                editedTimings.forEach { timing ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.width(72.dp)
                        ) {
                            Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                                Text("P${timing.section}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = timing.startTime,
                            onValueChange = { newStart ->
                                editedTimings = editedTimings.map {
                                    if (it.section == timing.section) it.copy(startTime = newStart) else it
                                }
                            },
                            label = { Text("Start", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Text("-")

                        OutlinedTextField(
                            value = timing.endTime,
                            onValueChange = { newEnd ->
                                editedTimings = editedTimings.map {
                                    if (it.section == timing.section) it.copy(endTime = newEnd) else it
                                }
                            },
                            label = { Text("End", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
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
