package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserPreferencesManager
import com.example.domain.model.Course
import com.example.domain.model.ImportedCourse
import com.example.ui.theme.tr

private val COLOR_CHOICES = listOf(
    "#5B9BF3", // Soft Blue
    "#4F46E5", // Indigo
    "#0EA5E9", // Sky Blue
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#EC4899", // Pink
    "#8B5CF6", // Purple
    "#06B6D4", // Cyan
    "#F97316", // Orange
    "#84CC16", // Lime
    "#14B8A6", // Teal
    "#EF4444"  // Rose
)

@Composable
private fun getDayDisplayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Monday".tr
        2 -> "Tuesday".tr
        3 -> "Wednesday".tr
        4 -> "Thursday".tr
        5 -> "Friday".tr
        6 -> "Saturday".tr
        7 -> "Sunday".tr
        else -> "Monday".tr
    }
}

@Composable
private fun getDayShortName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Mon".tr
        2 -> "Tue".tr
        3 -> "Wed".tr
        4 -> "Thu".tr
        5 -> "Fri".tr
        6 -> "Sat".tr
        7 -> "Sun".tr
        else -> "Mon".tr
    }
}

data class TimeSlotState(
    var dayOfWeek: Int = 1,
    var startPeriod: Int = 1,
    var endPeriod: Int = 2,
    var startTime: String = "08:00",
    var endTime: String = "09:35",
    var weeksSummary: String = "Weeks 1-20 (Full Term)",
    var classroom: String = "",
    var instructor: String = "",
    var notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCourseDialog(
    initialCourse: Course? = null,
    defaultSemester: String = "Fall 2025",
    initialDay: Int = 1,
    initialStartPeriod: Int = 1,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onSaveMultiple: ((List<Course>) -> Unit)? = null,
    currentCourseReminderMinutes: Int? = null,
    onSaveCourseReminderMinutes: ((courseName: String, minutes: Int?) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var colorHex by remember { mutableStateOf(initialCourse?.colorHex ?: "#5B9BF3") }
    var credits by remember { mutableStateOf(initialCourse?.credits?.toString() ?: "3") }
    var semester by remember { mutableStateOf(initialCourse?.semester ?: defaultSemester) }
    var code by remember { mutableStateOf(initialCourse?.code ?: "") }
    var courseReminderMinutes by remember { mutableStateOf(currentCourseReminderMinutes) }

    val defaultTiming = UserPreferencesManager.defaultSectionTimings
    val initialSlots = remember {
        mutableStateListOf(
            TimeSlotState(
                dayOfWeek = initialCourse?.dayOfWeek ?: initialDay,
                startPeriod = initialStartPeriod,
                endPeriod = (initialStartPeriod + 1).coerceAtMost(12),
                startTime = initialCourse?.startTime ?: defaultTiming.getOrNull(initialStartPeriod - 1)?.startTime ?: "08:00",
                endTime = initialCourse?.endTime ?: defaultTiming.getOrNull(initialStartPeriod)?.endTime ?: "09:35",
                classroom = initialCourse?.classroom ?: "",
                instructor = initialCourse?.instructor ?: "",
                notes = initialCourse?.notes ?: ""
            )
        )
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var editingSlotIndexForDayPeriod by remember { mutableStateOf<Int?>(null) }
    var editingSlotIndexForWeeks by remember { mutableStateOf<Int?>(null) }
    var editingSlotIndexForCustomTime by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (initialCourse == null) "Add Course".tr else "Edit Course".tr,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_course")) {
                                Icon(Icons.Default.Close, contentDescription = "Close".tr)
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    if (name.isBlank()) return@TextButton

                                    val generatedCourses = initialSlots.mapIndexed { idx, slot ->
                                        Course(
                                            id = if (idx == 0 && initialCourse != null) initialCourse.id else 0,
                                            name = name.trim(),
                                            code = code.trim(),
                                            instructor = slot.instructor.trim(),
                                            classroom = slot.classroom.trim(),
                                            dayOfWeek = slot.dayOfWeek,
                                            startTime = slot.startTime,
                                            endTime = slot.endTime,
                                            colorHex = colorHex,
                                            semester = semester,
                                            credits = credits.toIntOrNull() ?: 3,
                                            notes = slot.notes.trim()
                                        )
                                    }

                                    if (generatedCourses.size > 1 && onSaveMultiple != null) {
                                        onSaveMultiple(generatedCourses)
                                    } else {
                                        generatedCourses.firstOrNull()?.let { onSave(it) }
                                    }

                                    if (onSaveCourseReminderMinutes != null && name.isNotBlank()) {
                                        onSaveCourseReminderMinutes(name.trim(), courseReminderMinutes)
                                    }
                                },
                                enabled = name.isNotBlank(),
                                modifier = Modifier.testTag("save_course_button")
                            ) {
                                Text(
                                    text = "Save".tr,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card 1: Course Basic Info (Name, Color, Credits)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Course Name Input
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                if (name.isEmpty()) {
                                    Text(
                                        text = "Course Name (e.g. Operating Systems)".tr,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                BasicTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    textStyle = TextStyle(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("course_name_input")
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Color Row with circle swatch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showColorPicker = !showColorPicker }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("Color".tr, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                }

                                val currentColor = try {
                                    Color(android.graphics.Color.parseColor(colorHex))
                                } catch (_: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(currentColor)
                                )
                            }

                            // Horizontal color picker row if open
                            if (showColorPicker) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    items(COLOR_CHOICES) { hex ->
                                        val isChosen = hex.equals(colorHex, ignoreCase = true)
                                        val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Blue }
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .border(
                                                    width = if (isChosen) 3.dp else 1.dp,
                                                    color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .padding(3.dp)
                                                .clip(CircleShape)
                                                .background(parsed)
                                                .clickable {
                                                    colorHex = hex
                                                    showColorPicker = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isChosen) {
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
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Credits Column
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.StarOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text("Credits".tr, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "$credits ${"Credits".tr}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // 1-4 common, plus 5, 6 for higher credits
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("1", "2", "3", "4", "5", "6").forEach { cr ->
                                        FilterChip(
                                            selected = credits == cr,
                                            onClick = { credits = cr },
                                            label = { Text(cr, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                // Custom / Higher Credits Stepper or Input
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "Higher Credit Value:".tr,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = credits,
                                        onValueChange = { input ->
                                            if (input.all { it.isDigit() } && input.length <= 2) {
                                                credits = input
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.width(64.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    )

                                    FilledTonalIconButton(
                                        onClick = {
                                            val curr = credits.toIntOrNull() ?: 3
                                            credits = (curr + 1).coerceAtMost(30).toString()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                    }

                                    FilledTonalIconButton(
                                        onClick = {
                                            val curr = credits.toIntOrNull() ?: 3
                                            credits = (curr - 1).coerceAtLeast(1).toString()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Course-Wise Before Class Alert
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text("Before-Class Alert".tr, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            Text(
                                                text = if (courseReminderMinutes == null) "Default (15m before)".tr
                                                       else if (courseReminderMinutes!! <= 0) "Muted (No alert)".tr
                                                       else "${courseReminderMinutes}m before class".tr,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = courseReminderMinutes == null,
                                        onClick = { courseReminderMinutes = null },
                                        label = { Text("Default".tr, fontSize = 11.sp) }
                                    )
                                    listOf(5, 10, 15, 20, 30).forEach { mins ->
                                        FilterChip(
                                            selected = courseReminderMinutes == mins,
                                            onClick = { courseReminderMinutes = mins },
                                            label = { Text("${mins}m", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card 2+: Time Slots
                    initialSlots.forEachIndexed { index, slot ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header: Time Slot index + Duplicate + Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${"Time Slot".tr} ${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Duplicate".tr,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            modifier = Modifier.clickable {
                                                initialSlots.add(slot.copy())
                                            }
                                        )

                                        if (initialSlots.size > 1) {
                                            Text(
                                                text = "Delete".tr,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                modifier = Modifier.clickable {
                                                    initialSlots.removeAt(index)
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Row 1: Weeks
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingSlotIndexForWeeks = index }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Weeks".tr, fontSize = 14.5.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(slot.weeksSummary.tr, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                // Row 2: Periods & Day
                                val dayName = getDayDisplayName(slot.dayOfWeek)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingSlotIndexForDayPeriod = index }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Periods".tr, fontSize = 14.5.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("$dayName • ${slot.startPeriod}-${slot.endPeriod}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                // Row 3: Time
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingSlotIndexForCustomTime = index }
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Time".tr, fontSize = 14.5.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${slot.startTime} - ${slot.endTime}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                // Row 4: Classroom
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Classroom".tr, fontSize = 14.5.sp)
                                    }

                                    Box(modifier = Modifier.width(160.dp), contentAlignment = Alignment.CenterEnd) {
                                        if (slot.classroom.isEmpty()) {
                                            Text("Optional".tr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        }
                                        BasicTextField(
                                            value = slot.classroom,
                                            onValueChange = { slot.classroom = it },
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                // Row 5: Teacher
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Teacher".tr, fontSize = 14.5.sp)
                                    }

                                    Box(modifier = Modifier.width(160.dp), contentAlignment = Alignment.CenterEnd) {
                                        if (slot.instructor.isEmpty()) {
                                            Text("Optional".tr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        }
                                        BasicTextField(
                                            value = slot.instructor,
                                            onValueChange = { slot.instructor = it },
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                // Row 6: Notes
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Notes".tr, fontSize = 14.5.sp)
                                    }

                                    Box(modifier = Modifier.width(160.dp), contentAlignment = Alignment.CenterEnd) {
                                        if (slot.notes.isEmpty()) {
                                            Text("Optional".tr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        }
                                        BasicTextField(
                                            value = slot.notes,
                                            onValueChange = { slot.notes = it },
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // "+ Add Time Slot" Button
                    OutlinedButton(
                        onClick = {
                            initialSlots.add(
                                TimeSlotState(
                                    dayOfWeek = ((initialSlots.lastOrNull()?.dayOfWeek ?: 1) % 7) + 1,
                                    startPeriod = 3,
                                    endPeriod = 4,
                                    startTime = defaultTiming.getOrNull(2)?.startTime ?: "09:50",
                                    endTime = defaultTiming.getOrNull(3)?.endTime ?: "11:25"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ ${"Add Time Slot".tr}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Modal Dialog: Day & Period Selector
    editingSlotIndexForDayPeriod?.let { slotIdx ->
        val currentSlot = initialSlots.getOrNull(slotIdx)
        if (currentSlot != null) {
            AlertDialog(
                onDismissRequest = { editingSlotIndexForDayPeriod = null },
                title = { Text("Select Day & Periods".tr, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Day of Week".tr, style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            (1..7).forEach { dInt ->
                                val isSelected = currentSlot.dayOfWeek == dInt
                                val shortName = getDayShortName(dInt)
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { currentSlot.dayOfWeek = dInt }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = shortName.takeLast(1),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Text("Period Range".tr, style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${"Start Period".tr}: ${currentSlot.startPeriod}", fontSize = 12.sp)
                                Slider(
                                    value = currentSlot.startPeriod.toFloat(),
                                    onValueChange = {
                                        val p = it.toInt()
                                        currentSlot.startPeriod = p
                                        if (currentSlot.endPeriod < p) currentSlot.endPeriod = p
                                        currentSlot.startTime = defaultTiming.getOrNull(p - 1)?.startTime ?: currentSlot.startTime
                                        currentSlot.endTime = defaultTiming.getOrNull(currentSlot.endPeriod - 1)?.endTime ?: currentSlot.endTime
                                    },
                                    valueRange = 1f..12f,
                                    steps = 10
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${"End Period".tr}: ${currentSlot.endPeriod}", fontSize = 12.sp)
                                Slider(
                                    value = currentSlot.endPeriod.toFloat(),
                                    onValueChange = {
                                        val p = it.toInt()
                                        currentSlot.endPeriod = p
                                        if (currentSlot.startPeriod > p) currentSlot.startPeriod = p
                                        currentSlot.startTime = defaultTiming.getOrNull(currentSlot.startPeriod - 1)?.startTime ?: currentSlot.startTime
                                        currentSlot.endTime = defaultTiming.getOrNull(p - 1)?.endTime ?: currentSlot.endTime
                                    },
                                    valueRange = 1f..12f,
                                    steps = 10
                                )
                            }
                        }

                        Text(
                            "${"Time".tr}: ${currentSlot.startTime} - ${currentSlot.endTime}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { editingSlotIndexForDayPeriod = null }) {
                        Text("Confirm".tr)
                    }
                }
            )
        }
    }

    // Modal Dialog: Week Range Selector
    editingSlotIndexForWeeks?.let { slotIdx ->
        val currentSlot = initialSlots.getOrNull(slotIdx)
        if (currentSlot != null) {
            AlertDialog(
                onDismissRequest = { editingSlotIndexForWeeks = null },
                title = { Text("Select Active Weeks".tr, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            "Weeks 1-20 (Full Term)",
                            "Weeks 1-18 (Standard Term)",
                            "Odd Weeks (1, 3, 5...)",
                            "Even Weeks (2, 4, 6...)",
                            "First Half (Weeks 1-8)",
                            "Second Half (Weeks 9-16)"
                        ).forEach { option ->
                            val isSelected = currentSlot.weeksSummary == option
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentSlot.weeksSummary = option
                                        editingSlotIndexForWeeks = null
                                    }
                            ) {
                                Text(
                                    text = option.tr,
                                    modifier = Modifier.padding(14.dp),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { editingSlotIndexForWeeks = null }) {
                        Text("Cancel".tr)
                    }
                }
            )
        }
    }

    // Modal Dialog: Custom Time
    editingSlotIndexForCustomTime?.let { slotIdx ->
        val currentSlot = initialSlots.getOrNull(slotIdx)
        if (currentSlot != null) {
            var sTime by remember { mutableStateOf(currentSlot.startTime) }
            var eTime by remember { mutableStateOf(currentSlot.endTime) }

            AlertDialog(
                onDismissRequest = { editingSlotIndexForCustomTime = null },
                title = { Text("Time".tr, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = sTime,
                            onValueChange = { sTime = it },
                            label = { Text("Start Time".tr) },
                            placeholder = { Text("08:00") }
                        )
                        OutlinedTextField(
                            value = eTime,
                            onValueChange = { eTime = it },
                            label = { Text("End Time".tr) },
                            placeholder = { Text("09:35") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        currentSlot.startTime = sTime
                        currentSlot.endTime = eTime
                        editingSlotIndexForCustomTime = null
                    }) {
                        Text("Save".tr)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingSlotIndexForCustomTime = null }) {
                        Text("Cancel".tr)
                    }
                }
            )
        }
    }
}

@Composable
fun EditParsedCourseDialog(
    course: ImportedCourse,
    onDismiss: () -> Unit,
    onSave: (ImportedCourse) -> Unit
) {
    var name by remember { mutableStateOf(course.name) }
    var code by remember { mutableStateOf(course.code) }
    var classroom by remember { mutableStateOf(course.classroom) }
    var instructor by remember { mutableStateOf(course.instructor) }
    var dayOfWeek by remember { mutableIntStateOf(course.dayOfWeek) }
    var startTime by remember { mutableStateOf(course.startTime) }
    var endTime by remember { mutableStateOf(course.endTime) }
    var colorHex by remember { mutableStateOf(course.colorHex) }
    var credits by remember { mutableIntStateOf(course.credits) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Course".tr, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Name".tr) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code".tr) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = credits.toString(),
                        onValueChange = { credits = it.toIntOrNull() ?: credits },
                        label = { Text("Credits".tr) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { Text("Classroom".tr) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("Instructor".tr) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time".tr) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time".tr) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = course.copy(
                        name = name.trim(),
                        code = code.trim(),
                        classroom = classroom.trim(),
                        instructor = instructor.trim(),
                        dayOfWeek = dayOfWeek,
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        colorHex = colorHex,
                        credits = credits
                    )
                    onSave(updated)
                },
                enabled = name.isNotBlank()
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
