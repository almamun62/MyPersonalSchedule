package com.example.ui.components

import androidx.compose.foundation.background
import com.example.ui.theme.tr
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.CoursePalette
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(
    semesterId: Long,
    initialDayOfWeek: Int = 1,
    initialStartPeriod: Int = 1,
    initialEndPeriod: Int = 2,
    onDismiss: () -> Unit,
    onConfirm: (CourseEntity) -> Unit
) {
    CourseEditorDialog(
        semesterId = semesterId,
        courseToEdit = null,
        initialDayOfWeek = initialDayOfWeek,
        initialStartPeriod = initialStartPeriod,
        initialEndPeriod = initialEndPeriod,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CourseEditorDialog(
    semesterId: Long,
    courseToEdit: CourseEntity? = null,
    initialDayOfWeek: Int = 1,
    initialStartPeriod: Int = 1,
    initialEndPeriod: Int = 2,
    onDismiss: () -> Unit,
    onConfirm: (CourseEntity) -> Unit,
    onConfirmAndAddAnother: ((CourseEntity) -> Unit)? = null,
    onDelete: ((CourseEntity) -> Unit)? = null
) {
    val isEditing = courseToEdit != null
    var name by remember(courseToEdit) { mutableStateOf(courseToEdit?.name ?: "") }
    var code by remember(courseToEdit) { mutableStateOf(courseToEdit?.code ?: "") }
    var classroom by remember(courseToEdit) { mutableStateOf(courseToEdit?.classroom ?: "") }
    var instructor by remember(courseToEdit) { mutableStateOf(courseToEdit?.instructor ?: "") }
    var notes by remember(courseToEdit) { mutableStateOf(courseToEdit?.notes ?: "") }
    var selectedDay by remember(courseToEdit) {
        mutableStateOf(courseToEdit?.dayOfWeek ?: initialDayOfWeek.coerceIn(1, 7))
    }
    var startPeriod by remember(courseToEdit) {
        mutableStateOf(courseToEdit?.startPeriod ?: initialStartPeriod.coerceIn(1, 12))
    }
    var endPeriod by remember(courseToEdit) {
        mutableStateOf(courseToEdit?.endPeriod ?: initialEndPeriod.coerceIn(startPeriod, 12))
    }
    var startTime by remember(courseToEdit) {
        val defaultStart = when (startPeriod) {
            1 -> "08:00"; 3 -> "09:50"; 6 -> "14:30"; 8 -> "16:20"; 10 -> "19:00"
            else -> String.format("%02d:00", 7 + startPeriod)
        }
        mutableStateOf(courseToEdit?.startTime?.ifBlank { defaultStart } ?: defaultStart)
    }
    var endTime by remember(courseToEdit) {
        val defaultEnd = when (endPeriod) {
            2 -> "09:35"; 4 -> "11:25"; 5 -> "12:15"; 7 -> "16:05"; 9 -> "17:55"; 12 -> "21:25"
            else -> String.format("%02d:45", 7 + endPeriod)
        }
        mutableStateOf(courseToEdit?.endTime?.ifBlank { defaultEnd } ?: defaultEnd)
    }
    var weekRule by remember(courseToEdit) {
        mutableStateOf(courseToEdit?.weekRule ?: WeekRule.ALL)
    }
    var customWeeks by remember(courseToEdit) {
        mutableStateOf(courseToEdit?.customWeeks ?: "1-18")
    }
    var selectedColor by remember(courseToEdit) {
        mutableStateOf(courseToEdit?.colorHex ?: CoursePalette[0].value.toLong())
    }
    var dndEnabled by remember(courseToEdit) {
        mutableStateOf(courseToEdit?.dndEnabled ?: true)
    }

    // Interactive week set calculation
    var activeWeeksSet by remember(customWeeks) {
        val set = mutableSetOf<Int>()
        if (customWeeks.isNotBlank()) {
            customWeeks.split(",").forEach { part ->
                val trimmed = part.trim()
                if (trimmed.contains("-")) {
                    val bounds = trimmed.split("-")
                    val start = bounds.getOrNull(0)?.toIntOrNull() ?: 1
                    val end = bounds.getOrNull(1)?.toIntOrNull() ?: start
                    for (w in start..end) {
                        if (w in 1..20) set.add(w)
                    }
                } else {
                    val w = trimmed.toIntOrNull()
                    if (w != null && w in 1..20) set.add(w)
                }
            }
        }
        if (set.isEmpty()) {
            for (w in 1..18) set.add(w)
        }
        mutableStateOf(set)
    }

    fun syncWeeksFromSet(newSet: Set<Int>) {
        activeWeeksSet = newSet.toMutableSet()
        if (newSet.isEmpty()) {
            customWeeks = ""
            return
        }
        val sorted = newSet.sorted()
        val ranges = mutableListOf<String>()
        var start = sorted.first()
        var prev = start
        for (i in 1 until sorted.size) {
            val curr = sorted[i]
            if (curr == prev + 1) {
                prev = curr
            } else {
                ranges.add(if (start == prev) "$start" else "$start-$prev")
                start = curr
                prev = curr
            }
        }
        ranges.add(if (start == prev) "$start" else "$start-$prev")
        customWeeks = ranges.joinToString(",")
    }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val subjectSuggestions = listOf(
        "高等数学", "大学英语", "数据结构", "计算机体系",
        "大学物理", "机器学习", "操作系统", "体育"
    )

    val periodPresets = listOf(
        Triple("1-2 (08:00-09:35)", 1 to 2, "08:00" to "09:35"),
        Triple("3-4 (09:50-11:25)", 3 to 4, "09:50" to "11:25"),
        Triple("3-5 (09:50-12:15)", 3 to 5, "09:50" to "12:15"),
        Triple("6-7 (14:30-16:05)", 6 to 7, "14:30" to "16:05"),
        Triple("8-9 (16:20-17:55)", 8 to 9, "16:20" to "17:55"),
        Triple("10-11 (19:00-20:35)", 10 to 11, "19:00" to "20:35"),
        Triple("10-12 (19:00-21:25)", 10 to 12, "19:00" to "21:25")
    )

    fun createEntity(): CourseEntity {
        return CourseEntity(
            id = courseToEdit?.id ?: 0,
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
            customWeeks = if (weekRule == WeekRule.CUSTOM) customWeeks.trim() else "",
            colorHex = selectedColor,
            dndEnabled = dndEnabled,
            notes = notes.trim()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(if (isEditing) "Edit Course Schedule".tr else "Add Course Schedule".tr)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick subject suggestions
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Popular Subjects (Tap to fill)".tr, style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjectSuggestions.forEach { subj ->
                            AssistChip(
                                onClick = {
                                    name = subj
                                    if (classroom.isBlank()) classroom = "B105"
                                },
                                label = { Text(subj, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Course Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Name *".tr) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Code and Room
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code (e.g. CS401)".tr) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Room *".tr) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Instructor
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("Instructor".tr) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Day of the Week
                Text("Day of the Week".tr, style = MaterialTheme.typography.labelMedium)
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
                            label = { Text(day, fontSize = 11.sp) }
                        )
                    }
                }

                // Period Presets
                Text("Section Presets".tr, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    periodPresets.forEach { (label, periods, times) ->
                        val isCurrent = startPeriod == periods.first && endPeriod == periods.second
                        FilterChip(
                            selected = isCurrent,
                            onClick = {
                                startPeriod = periods.first
                                endPeriod = periods.second
                                startTime = times.first
                                endTime = times.second
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Manual Period Start / End
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startPeriod.toString(),
                        onValueChange = {
                            val p = it.toIntOrNull() ?: startPeriod
                            startPeriod = p.coerceIn(1, 12)
                            if (endPeriod < startPeriod) endPeriod = startPeriod
                            com.example.data.local.UserPreferencesManager.defaultSectionTimings.getOrNull(startPeriod - 1)?.let { timing ->
                                startTime = timing.startTime
                            }
                        },
                        label = { Text("Start Period (1-12)".tr) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endPeriod.toString(),
                        onValueChange = {
                            val p = it.toIntOrNull() ?: endPeriod
                            endPeriod = p.coerceIn(startPeriod, 12)
                            com.example.data.local.UserPreferencesManager.defaultSectionTimings.getOrNull(endPeriod - 1)?.let { timing ->
                                endTime = timing.endTime
                            }
                        },
                        label = { Text("End Period (1-12)".tr) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Clock times
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time".tr) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time".tr) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Week Rule Filter
                Text("Week Schedule Pattern".tr, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WeekRule.values().forEach { rule ->
                        FilterChip(
                            selected = weekRule == rule,
                            onClick = { weekRule = rule },
                            label = {
                                Text(
                                    when (rule) {
                                        WeekRule.ALL -> "All (1-18)"
                                        WeekRule.ODD -> "Odd (单周)"
                                        WeekRule.EVEN -> "Even (双周)"
                                        WeekRule.CUSTOM -> "Custom Weeks"
                                    },
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                // Interactive Custom Weeks Selector
                if (weekRule == WeekRule.CUSTOM) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active Weeks: $customWeeks",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Quick week presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AssistChip(
                                    onClick = { syncWeeksFromSet((1..18).toSet()) },
                                    label = { Text("1-18", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = { syncWeeksFromSet((1..8).toSet()) },
                                    label = { Text("Half 1 (1-8)", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = { syncWeeksFromSet((9..16).toSet()) },
                                    label = { Text("Half 2 (9-16)", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = { syncWeeksFromSet((1..18 step 2).toSet()) },
                                    label = { Text("Odd Weeks", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = { syncWeeksFromSet((2..18 step 2).toSet()) },
                                    label = { Text("Even Weeks", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = { syncWeeksFromSet(emptySet()) },
                                    label = { Text("Clear", fontSize = 10.sp) }
                                )
                            }

                            // Interactive 1..18 week buttons
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (w in 1..18) {
                                    val isSelected = w in activeWeeksSet
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val newSet = activeWeeksSet.toMutableSet()
                                            if (isSelected) newSet.remove(w) else newSet.add(w)
                                            syncWeeksFromSet(newSet)
                                        },
                                        label = { Text("W$w", fontSize = 10.sp) },
                                        modifier = Modifier.height(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Color Tag
                Text("Color Tag".tr, style = MaterialTheme.typography.labelMedium)
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

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Class Notes / Exam / Textbook".tr) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Auto-DND
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-DND during class".tr)
                    Switch(checked = dndEnabled, onCheckedChange = { dndEnabled = it })
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isEditing && onConfirmAndAddAnother != null) {
                    OutlinedButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirmAndAddAnother(createEntity())
                                // Keep course name, code, room, instructor for next slot!
                                selectedDay = (selectedDay % 7) + 1
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("+ Another Slot".tr)
                    }
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(createEntity())
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(if (isEditing) "Save Changes".tr else "Save Course".tr)
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(
                        onClick = {
                            courseToEdit?.let { onDelete(it) }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete".tr)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel".tr)
                }
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
    var selectedDuration by remember { mutableStateOf("1.5h") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCourseName.isNotEmpty()) "Post-Class Task" else "Add Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Homework Title *".tr) },
                    modifier = Modifier.fillMaxWidth()
                )

                val defaultCategories = listOf("General", "Gym", "Part-time Gig", "Cleaning", "Swimming", "Life")
                
                Text("Category / Associated Course".tr, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    defaultCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCourse == category || (category == "General" && selectedCourse.isEmpty()),
                            onClick = { selectedCourse = if (category == "General") "" else category },
                            label = { Text(category) }
                        )
                    }
                    if (courses.isNotEmpty()) {
                        courses.map { it.name }.distinct().filter { it !in defaultCategories }.forEach { cName ->
                            FilterChip(
                                selected = selectedCourse == cName,
                                onClick = { selectedCourse = cName },
                                label = { Text(cName) }
                            )
                        }
                    }
                }

                Text("Priority".tr, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                Text("Estimated Study Time (For Workload Chart)".tr, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("0.5h", "1h", "1.5h", "2h", "3h", "4h").forEach { dur ->
                        FilterChip(
                            selected = selectedDuration == dur,
                            onClick = {
                                selectedDuration = if (selectedDuration == dur) "" else dur
                            },
                            label = { Text(dur) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val cleanTitle = title.trim()
                        val finalTitle = if (selectedDuration.isNotEmpty() && !cleanTitle.contains("h", ignoreCase = true)) {
                            "$cleanTitle ($selectedDuration)"
                        } else {
                            cleanTitle
                        }
                        onConfirm(finalTitle, selectedCourse, priority)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add Task".tr)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel".tr) }
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
        title = { Text("Add Holiday / Make-up (调休)".tr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Mid-Autumn Break)".tr) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (YYYY-MM-DD)".tr) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == HolidayOverrideType.HOLIDAY,
                        onClick = { type = HolidayOverrideType.HOLIDAY },
                        label = { Text("Holiday (No Classes)".tr) }
                    )
                    FilterChip(
                        selected = type == HolidayOverrideType.MAKE_UP,
                        onClick = { type = HolidayOverrideType.MAKE_UP },
                        label = { Text("Weekend Make-up (调休)".tr) }
                    )
                }

                if (type == HolidayOverrideType.MAKE_UP) {
                    Text("Timetable to Follow:".tr, style = MaterialTheme.typography.labelMedium)
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
                Text("Add".tr)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel".tr) }
        }
    )
}
