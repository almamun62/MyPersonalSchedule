package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.theme.CoursePalette
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(
    semesterId: Long,
    onDismiss: () -> Unit,
    onConfirm: (CourseEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf(1) } // Monday = 1
    var startPeriod by remember { mutableStateOf(1) }
    var endPeriod by remember { mutableStateOf(2) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:40") }
    var weekRule by remember { mutableStateOf(WeekRule.ALL) }
    var customWeeks by remember { mutableStateOf("1-8,10-16") }
    var selectedColor by remember { mutableStateOf(CoursePalette[0].value.toLong()) }
    var dndEnabled by remember { mutableStateOf(true) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Course") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code (e.g. CS401)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Room *") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("Instructor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Day of the Week", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { index, day ->
                        val dayNum = index + 1
                        val isSelected = selectedDay == dayNum
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDay = dayNum },
                            label = { Text(day) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startPeriod.toString(),
                        onValueChange = { startPeriod = it.toIntOrNull() ?: startPeriod },
                        label = { Text("Start Period (1-12)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endPeriod.toString(),
                        onValueChange = { endPeriod = it.toIntOrNull() ?: endPeriod },
                        label = { Text("End Period (1-12)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Week Rule (Academic Filtering)", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WeekRule.values().forEach { rule ->
                        FilterChip(
                            selected = weekRule == rule,
                            onClick = { weekRule = rule },
                            label = { Text(rule.name) }
                        )
                    }
                }

                if (weekRule == WeekRule.CUSTOM) {
                    OutlinedTextField(
                        value = customWeeks,
                        onValueChange = { customWeeks = it },
                        label = { Text("Custom Weeks (e.g. 1-8,10-16)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Color Tag", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoursePalette.forEach { color ->
                        val isPicked = selectedColor == color.value.toLong()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isPicked) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color.value.toLong() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPicked) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-DND during class")
                    Switch(checked = dndEnabled, onCheckedChange = { dndEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            CourseEntity(
                                semesterId = semesterId,
                                name = name.trim(),
                                code = code.trim(),
                                classroom = classroom.trim().ifEmpty { "TBA" },
                                instructor = instructor.trim(),
                                dayOfWeek = selectedDay,
                                startPeriod = startPeriod,
                                endPeriod = endPeriod,
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                weekRule = weekRule,
                                customWeeks = customWeeks.trim(),
                                colorHex = selectedColor,
                                dndEnabled = dndEnabled
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddTaskDialog(
    initialCourseName: String = "",
    courses: List<CourseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, courseName: String, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf(initialCourseName) }
    var priority by remember { mutableStateOf("MEDIUM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCourseName.isNotEmpty()) "Post-Class Task" else "Add Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Homework Title *") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (courses.isNotEmpty()) {
                    Text("Associated Course", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCourse.isEmpty(),
                            onClick = { selectedCourse = "" },
                            label = { Text("General") }
                        )
                        courses.map { it.name }.distinct().forEach { cName ->
                            FilterChip(
                                selected = selectedCourse == cName,
                                onClick = { selectedCourse = cName },
                                label = { Text(cName) }
                            )
                        }
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), selectedCourse, priority)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddHolidayDialog(
    onDismiss: () -> Unit,
    onConfirm: (HolidayOverrideEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf(LocalDate.now().toString()) }
    var type by remember { mutableStateOf(HolidayOverrideType.HOLIDAY) }
    var targetDayOfWeek by remember { mutableStateOf(1) } // 1 = Monday

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Holiday / Make-up (调休)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Mid-Autumn Break)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == HolidayOverrideType.HOLIDAY,
                        onClick = { type = HolidayOverrideType.HOLIDAY },
                        label = { Text("Holiday (No Classes)") }
                    )
                    FilterChip(
                        selected = type == HolidayOverrideType.MAKE_UP,
                        onClick = { type = HolidayOverrideType.MAKE_UP },
                        label = { Text("Weekend Make-up (调休)") }
                    )
                }

                if (type == HolidayOverrideType.MAKE_UP) {
                    Text("Timetable to Follow:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1 to "Monday",
                            2 to "Tuesday",
                            3 to "Wednesday",
                            4 to "Thursday",
                            5 to "Friday"
                        ).forEach { (dayNum, dayName) ->
                            FilterChip(
                                selected = targetDayOfWeek == dayNum,
                                onClick = { targetDayOfWeek = dayNum },
                                label = { Text(dayName) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dateString.isNotBlank()) {
                        onConfirm(
                            HolidayOverrideEntity(
                                name = name.trim(),
                                dateString = dateString.trim(),
                                type = type,
                                targetDayOfWeek = if (type == HolidayOverrideType.MAKE_UP) targetDayOfWeek else null
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && dateString.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
