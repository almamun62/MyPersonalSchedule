package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserPreferencesManager
import com.example.domain.PinyinEngine
import com.example.domain.model.Course
import com.example.ui.components.RubyText
import com.example.ui.viewmodel.ScheduleViewModel

/**
 * Chaoxing Add/Edit Schedule Screen matching Screenshot 1.
 * Features:
 * - Course Name (with live Pinyin)
 * - Teacher (with live Pinyin)
 * - Class Week picker (e.g. "The 8Week")
 * - Class Time day picker (e.g. "Monday")
 * - Course Begin... session picker (e.g. "Session 3")
 * - Session Number duration picker (e.g. "3" sessions)
 * - Take place at classroom (with live Pinyin)
 * - Delete button at bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaoxingAddScheduleDialog(
    initialCourse: Course? = null,
    defaultDayOfWeek: Int = 1,
    defaultStartPeriod: Int = 1,
    defaultWeek: Int = 8,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var courseName by remember { mutableStateOf(initialCourse?.name ?: "") }
    var teacherName by remember { mutableStateOf(initialCourse?.instructor ?: "") }
    var selectedWeek by remember { mutableIntStateOf(defaultWeek) }
    var selectedDayOfWeek by remember { mutableIntStateOf(initialCourse?.dayOfWeek ?: defaultDayOfWeek) }
    var courseBeginSession by remember { mutableIntStateOf(initialCourse?.startPeriod ?: defaultStartPeriod) }
    var sessionNumber by remember {
        val span = if (initialCourse != null) (initialCourse.endPeriod - initialCourse.startPeriod + 1).coerceAtLeast(1) else 2
        mutableIntStateOf(span)
    }
    var classroom by remember { mutableStateOf(initialCourse?.classroom ?: viewModel.userPreferencesManager.lastUsedRoom.value) }

    // Picker state sheets
    var showWeekPicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showBeginSessionPicker by remember { mutableStateOf(false) }
    var showSessionNumberPicker by remember { mutableStateOf(false) }
    var showCourseNameEditDialog by remember { mutableStateOf(false) }
    var showTeacherEditDialog by remember { mutableStateOf(false) }
    var showClassroomEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val daysList = listOf(
        1 to "Monday",
        2 to "Tuesday",
        3 to "Wednesday",
        4 to "Thursday",
        5 to "Friday",
        6 to "Saturday",
        7 to "Sunday"
    )

    fun calculateTimes(begin: Int, span: Int): Pair<String, String> {
        val end = (begin + span - 1).coerceAtMost(12)
        val timings = UserPreferencesManager.defaultSectionTimings
        val startStr = timings.find { it.section == begin }?.startTime ?: "08:00"
        val endStr = timings.find { it.section == end }?.endTime ?: "09:50"
        return startStr to endStr
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    title = {
                        Text(
                            text = if (initialCourse == null) "Add schedule" else "Edit schedule",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (courseName.isNotBlank()) {
                                    val (sTime, eTime) = calculateTimes(courseBeginSession, sessionNumber)
                                    val endPeriodCalc = (courseBeginSession + sessionNumber - 1).coerceAtMost(12)
                                    val defaultColor = initialCourse?.colorHex ?: "#60A5FA"

                                    viewModel.userPreferencesManager.setLastUsedCourseMetadata(
                                        room = classroom,
                                        semester = "Fall 2026"
                                    )

                                    if (initialCourse == null) {
                                        val newCourse = com.example.domain.model.Course(
                                            id = 0L,
                                            name = courseName.trim(),
                                            code = "",
                                            classroom = classroom.trim(),
                                            instructor = teacherName.trim(),
                                            dayOfWeek = selectedDayOfWeek,
                                            startPeriod = courseBeginSession,
                                            endPeriod = endPeriodCalc,
                                            startTime = sTime,
                                            endTime = eTime,
                                            weekRule = com.example.data.model.WeekRule.ALL,
                                            colorHex = defaultColor,
                                            semester = "Fall 2026",
                                            notes = ""
                                        )
                                        viewModel.addCourse(newCourse)
                                    } else {
                                        viewModel.updateCourse(
                                            initialCourse.copy(
                                                name = courseName.trim(),
                                                classroom = classroom.trim(),
                                                instructor = teacherName.trim(),
                                                dayOfWeek = selectedDayOfWeek,
                                                startPeriod = courseBeginSession,
                                                endPeriod = endPeriodCalc,
                                                startTime = sTime,
                                                endTime = eTime
                                            )
                                        )
                                    }
                                    onSaved()
                                }
                            }
                        ) {
                            Text(
                                text = "Finish",
                                color = Color(0xFF0284C7),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = Color(0xFFFAFAFA)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Course Row
                        ChaoxingFormRow(
                            label = "Course",
                            valueContent = {
                                if (courseName.isBlank()) {
                                    Text("Enter Course Name", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                                } else {
                                    RubyText(
                                        text = courseName,
                                        showPinyin = true,
                                        charSize = 15.sp,
                                        pinyinSize = 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            hasChevron = true,
                            onClick = { showCourseNameEditDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 2. Teacher Row
                        ChaoxingFormRow(
                            label = "Teacher",
                            valueContent = {
                                if (teacherName.isBlank()) {
                                    Text("Enter Teacher", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                                } else {
                                    RubyText(
                                        text = teacherName,
                                        showPinyin = true,
                                        charSize = 15.sp,
                                        pinyinSize = 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            hasChevron = true,
                            onClick = { showTeacherEditDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 3. Class Week Row
                        ChaoxingFormRow(
                            label = "Class Week",
                            valueContent = {
                                Text("The ${selectedWeek}Week", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            },
                            hasDropdown = true,
                            onClick = { showWeekPicker = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 4. Class Time Row
                        ChaoxingFormRow(
                            label = "Class Time",
                            valueContent = {
                                val dayName = daysList.find { it.first == selectedDayOfWeek }?.second ?: "Monday"
                                Text(dayName, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            },
                            hasDropdown = true,
                            onClick = { showDayPicker = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 5. Course Begin... Row
                        ChaoxingFormRow(
                            label = "Course Begin...",
                            valueContent = {
                                Text("Session $courseBeginSession", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            },
                            hasDropdown = true,
                            onClick = { showBeginSessionPicker = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 6. Session Number Row
                        ChaoxingFormRow(
                            label = "Session Number",
                            valueContent = {
                                Text("$sessionNumber", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            },
                            hasDropdown = true,
                            onClick = { showSessionNumberPicker = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        // 7. Take place at Row
                        ChaoxingFormRow(
                            label = "Take place at",
                            valueContent = {
                                if (classroom.isBlank()) {
                                    Text("Enter Classroom", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                                } else {
                                    RubyText(
                                        text = classroom,
                                        showPinyin = true,
                                        charSize = 15.sp,
                                        pinyinSize = 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            hasChevron = true,
                            onClick = { showClassroomEditDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Delete Button (Screenshot 1 bottom button)
                if (initialCourse != null) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD))
                    ) {
                        Text(
                            text = "Delete",
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Modal Pickers
    if (showWeekPicker) {
        AlertDialog(
            onDismissRequest = { showWeekPicker = false },
            title = { Text("Select Class Week", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    (1..16).forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedWeek = week
                                    showWeekPicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("The ${week}Week", fontSize = 15.sp)
                            if (selectedWeek == week) {
                                RadioButton(selected = true, onClick = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showDayPicker) {
        AlertDialog(
            onDismissRequest = { showDayPicker = false },
            title = { Text("Select Class Day", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    daysList.forEach { (dNum, dLabel) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDayOfWeek = dNum
                                    showDayPicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(dLabel, fontSize = 15.sp)
                            if (selectedDayOfWeek == dNum) {
                                RadioButton(selected = true, onClick = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showBeginSessionPicker) {
        AlertDialog(
            onDismissRequest = { showBeginSessionPicker = false },
            title = { Text("Course Begin Session", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    (1..12).forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    courseBeginSession = s
                                    showBeginSessionPicker = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Session $s", fontSize = 15.sp)
                            if (courseBeginSession == s) {
                                RadioButton(selected = true, onClick = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showSessionNumberPicker) {
        AlertDialog(
            onDismissRequest = { showSessionNumberPicker = false },
            title = { Text("Session Number (Duration)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    (1..5).forEach { span ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sessionNumber = span
                                    showSessionNumberPicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$span Session${if (span > 1) "s" else ""}", fontSize = 15.sp)
                            if (sessionNumber == span) {
                                RadioButton(selected = true, onClick = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Text inputs with live Pinyin preview
    if (showCourseNameEditDialog) {
        var tempName by remember { mutableStateOf(courseName) }
        AlertDialog(
            onDismissRequest = { showCourseNameEditDialog = false },
            title = { Text("Course Name", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("e.g. 计算机组成原理(国际学生)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tempName.isNotBlank()) {
                        Text("Live Pinyin Preview:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RubyText(text = tempName, showPinyin = true)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    courseName = tempName
                    showCourseNameEditDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showCourseNameEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTeacherEditDialog) {
        var tempTeacher by remember { mutableStateOf(teacherName) }
        AlertDialog(
            onDismissRequest = { showTeacherEditDialog = false },
            title = { Text("Teacher Name", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempTeacher,
                        onValueChange = { tempTeacher = it },
                        label = { Text("e.g. 徐媛媛") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tempTeacher.isNotBlank()) {
                        Text("Live Pinyin Preview:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RubyText(text = tempTeacher, showPinyin = true)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    teacherName = tempTeacher
                    showTeacherEditDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showTeacherEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClassroomEditDialog) {
        var tempRoom by remember { mutableStateOf(classroom) }
        AlertDialog(
            onDismissRequest = { showClassroomEditDialog = false },
            title = { Text("Take place at (Classroom)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempRoom,
                        onValueChange = { tempRoom = it },
                        label = { Text("e.g. 明理楼B105") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tempRoom.isNotBlank()) {
                        Text("Live Pinyin Preview:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RubyText(text = tempRoom, showPinyin = true)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    classroom = tempRoom
                    showClassroomEditDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showClassroomEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirmDialog && initialCourse != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Course", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${initialCourse.name} from your schedule?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCourse(initialCourse)
                        showDeleteConfirmDialog = false
                        onSaved()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ChaoxingFormRow(
    label: String,
    valueContent: @Composable () -> Unit,
    hasChevron: Boolean = false,
    hasDropdown: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 100.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(1f)
        ) {
            valueContent()
            Spacer(modifier = Modifier.width(6.dp))
            if (hasChevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            } else if (hasDropdown) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
