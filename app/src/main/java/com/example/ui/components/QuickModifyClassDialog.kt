package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
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
        Pair(3, 5) to "09:50 - 12:15 (Periods 3-5)",
        Pair(6, 7) to "14:30 - 16:05 (Periods 6-7 • After Lunch)",
        Pair(8, 9) to "16:20 - 17:55 (Periods 8-9)",
        Pair(10, 12) to "19:00 - 21:25 (Periods 10-12 • Evening)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Quick Room / Time Change".tr,
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { Text("New Classroom".tr) },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Select Time Period:".tr, fontWeight = FontWeight.SemiBold)

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
                            label = { Text(label, fontSize = 11.5.sp) },
                            modifier = Modifier.fillMaxWidth()
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
                enabled = classroom.isNotBlank()
            ) {
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
