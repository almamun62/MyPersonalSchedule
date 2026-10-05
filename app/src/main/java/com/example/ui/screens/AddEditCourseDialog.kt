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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserPreferencesManager
import com.example.domain.model.Course
import com.example.ui.theme.tr

private val COLOR_CHOICES = listOf(
    "#5B9BF3", "#4F46E5", "#0EA5E9", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6", "#EF4444"
)

class TimeSlotHolder(
    initialDayOfWeek: Int = 1,
    initialStartPeriod: Int = 1,
    initialEndPeriod: Int = 2,
    initialStartTime: String = "08:00",
    initialEndTime: String = "09:35",
    initialClassroom: String = "",
    initialInstructor: String = "",
    initialNotes: String = ""
) {
    var dayOfWeek by mutableIntStateOf(initialDayOfWeek)
    var startPeriod by mutableIntStateOf(initialStartPeriod)
    var endPeriod by mutableIntStateOf(initialEndPeriod)
    var startTime by mutableStateOf(initialStartTime)
    var endTime by mutableStateOf(initialEndTime)
    var classroom by mutableStateOf(initialClassroom)
    var instructor by mutableStateOf(initialInstructor)
    var notes by mutableStateOf(initialNotes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCourseDialog(
    initialCourse: Course? = null,
    defaultSemester: String = "Fall 2026",
    initialDay: Int = 1,
    initialStartPeriod: Int = 1,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onSaveMultiple: ((List<Course>) -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var code by remember { mutableStateOf(initialCourse?.code ?: "") }
    var colorHex by remember { mutableStateOf(initialCourse?.colorHex ?: "#5B9BF3") }
    var credits by remember { mutableStateOf(initialCourse?.credits?.toString() ?: "3") }
    var semester by remember { mutableStateOf(initialCourse?.semester ?: defaultSemester) }

    val defaultTiming = UserPreferencesManager.defaultSectionTimings
    val slotHolders = remember {
        mutableStateListOf(
            TimeSlotHolder(
                initialDayOfWeek = initialCourse?.dayOfWeek ?: initialDay,
                initialStartPeriod = initialCourse?.startPeriod ?: initialStartPeriod,
                initialEndPeriod = initialCourse?.endPeriod ?: (initialStartPeriod + 1).coerceAtMost(12),
                initialStartTime = initialCourse?.startTime ?: defaultTiming.getOrNull(initialStartPeriod - 1)?.startTime ?: "08:00",
                initialEndTime = initialCourse?.endTime ?: defaultTiming.getOrNull(initialStartPeriod)?.endTime ?: "09:35",
                initialClassroom = initialCourse?.classroom ?: "",
                initialInstructor = initialCourse?.instructor ?: "",
                initialNotes = initialCourse?.notes ?: ""
            )
        )
    }

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
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close".tr)
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    if (name.isBlank()) return@TextButton
                                    val generatedCourses = slotHolders.mapIndexed { idx, slot ->
                                        Course(
                                            id = if (idx == 0 && initialCourse != null) initialCourse.id else 0,
                                            name = name.trim(),
                                            code = code.trim(),
                                            instructor = slot.instructor.trim(),
                                            classroom = slot.classroom.trim(),
                                            dayOfWeek = slot.dayOfWeek,
                                            startPeriod = slot.startPeriod,
                                            endPeriod = slot.endPeriod,
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
                                },
                                enabled = name.isNotBlank()
                            ) {
                                Text("Save".tr, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
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
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    value = credits,
                                    onValueChange = { credits = it },
                                    label = { Text("Credits".tr) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text("Accent Color".tr, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(COLOR_CHOICES) { hex ->
                                    val isSelected = colorHex.equals(hex, ignoreCase = true)
                                    val colorVal = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(colorVal)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { colorHex = hex }
                                    )
                                }
                            }
                        }
                    }

                    slotHolders.forEachIndexed { index, slot ->
                        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Time Slot ${index + 1}", fontWeight = FontWeight.Bold)

                                Text("Day of Week", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (d in 1..7) {
                                        val dayLabel = when (d) {
                                            1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"; else -> "Mon"
                                        }
                                        FilterChip(
                                            selected = slot.dayOfWeek == d,
                                            onClick = { slot.dayOfWeek = d },
                                            label = { Text(dayLabel, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = slot.classroom,
                                        onValueChange = { slot.classroom = it },
                                        label = { Text("Classroom".tr) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = slot.instructor,
                                        onValueChange = { slot.instructor = it },
                                        label = { Text("Instructor".tr) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text("Period Range (1..12): P${slot.startPeriod} - P${slot.endPeriod}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    val presetPairs = listOf(
                                        Triple(1, 2, "Morning (P1-2)"),
                                        Triple(3, 5, "Mid-Day (P3-5)"),
                                        Triple(6, 7, "After Lunch (P6-7)"),
                                        Triple(8, 9, "Afternoon (P8-9)"),
                                        Triple(10, 12, "Evening (P10-12)")
                                    )
                                    presetPairs.forEachIndexed { pIdx, (sP, eP, pLabel) ->
                                        SegmentedButton(
                                            selected = slot.startPeriod == sP && slot.endPeriod == eP,
                                            onClick = {
                                                slot.startPeriod = sP
                                                slot.endPeriod = eP
                                                val tStart = defaultTiming.getOrNull(sP - 1)?.startTime ?: "08:00"
                                                val tEnd = defaultTiming.getOrNull(eP - 1)?.endTime ?: "09:35"
                                                slot.startTime = tStart
                                                slot.endTime = tEnd
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(index = pIdx, count = presetPairs.size)
                                        ) {
                                            Text(pLabel, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
