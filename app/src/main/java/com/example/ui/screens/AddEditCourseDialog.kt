package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.UserPreferencesManager
import com.example.data.model.WeekRule
import com.example.domain.CourseConflictDetector
import com.example.domain.model.Course
import com.example.ui.theme.tr

private val DEFAULT_COLORS = listOf(
    "#2563EB", // Blue
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#8B5CF6", // Purple
    "#EC4899", // Pink
    "#06B6D4"  // Cyan
)

private val DAYS_OF_WEEK_LABELS = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCourseDialog(
    initialCourse: Course? = null,
    existingCourses: List<Course> = emptyList(),
    defaultSemester: String = "Fall 2026",
    initialDay: Int = 1,
    initialStartPeriod: Int = 1,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onSaveMultiple: ((List<Course>) -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferencesManager.getInstance(context) }
    val lastUsedRoomPref by prefs.lastUsedRoom.collectAsStateWithLifecycle()
    val lastUsedSemesterPref by prefs.lastUsedSemester.collectAsStateWithLifecycle()

    // Smart Defaults from preferences or initialCourse
    val defaultClassroom = initialCourse?.classroom ?: lastUsedRoomPref.ifBlank { "Room 101" }
    val defaultSemesterValue = initialCourse?.semester ?: lastUsedSemesterPref.ifBlank { defaultSemester }
    val defaultColor = initialCourse?.colorHex ?: DEFAULT_COLORS[existingCourses.size % DEFAULT_COLORS.size]

    // Primary Fields
    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var selectedDay by remember { mutableIntStateOf(initialCourse?.dayOfWeek ?: initialDay) }
    var startPeriod by remember { mutableIntStateOf(initialCourse?.startPeriod ?: initialStartPeriod) }
    var endPeriod by remember { mutableIntStateOf(initialCourse?.endPeriod ?: (initialStartPeriod + 1).coerceAtMost(12)) }
    var room by remember { mutableStateOf(defaultClassroom) }
    var weekRule by remember { mutableStateOf(initialCourse?.weekRule ?: WeekRule.ALL) }
    var weekRangeText by remember { mutableStateOf(if (initialCourse?.weekRule == WeekRule.ALL) "1-16" else "") }

    // Collapsible "More options"
    var showMoreOptions by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf(initialCourse?.code ?: "") }
    var instructor by remember { mutableStateOf(initialCourse?.instructor ?: "") }
    var credits by remember { mutableStateOf(initialCourse?.credits?.toString() ?: "3") }
    var colorHex by remember { mutableStateOf(defaultColor) }
    var semester by remember { mutableStateOf(defaultSemesterValue) }
    var notes by remember { mutableStateOf(initialCourse?.notes ?: "") }
    var isRetake by remember { mutableStateOf(initialCourse?.isRetake ?: false) }

    val defaultTiming = UserPreferencesManager.defaultSectionTimings
    val calculatedStartTime = defaultTiming.getOrNull(startPeriod - 1)?.startTime ?: "08:00"
    val calculatedEndTime = defaultTiming.getOrNull(endPeriod - 1)?.endTime ?: "09:35"

    // Live conflict detector
    val conflictingExistingCourses = remember(selectedDay, startPeriod, endPeriod, weekRule, existingCourses) {
        existingCourses.filter { existing ->
            if (initialCourse != null && existing.id == initialCourse.id) return@filter false
            existing.dayOfWeek == selectedDay &&
                    CourseConflictDetector.doWeekRulesOverlap(weekRule, existing.weekRule) &&
                    (startPeriod <= existing.endPeriod) && (existing.startPeriod <= endPeriod)
        }
    }

    fun handleSave() {
        if (name.isBlank()) return

        // Persist last-used room and semester
        prefs.setLastUsedCourseMetadata(room = room, semester = semester)

        val courseToSave = Course(
            id = initialCourse?.id ?: 0L,
            name = name.trim(),
            code = code.trim(),
            instructor = instructor.trim(),
            classroom = room.trim(),
            dayOfWeek = selectedDay,
            startPeriod = startPeriod,
            endPeriod = endPeriod.coerceAtLeast(startPeriod),
            startTime = calculatedStartTime,
            endTime = calculatedEndTime,
            weekRule = weekRule,
            colorHex = colorHex,
            semester = semester.trim(),
            credits = credits.toIntOrNull() ?: 3,
            isRetake = isRetake,
            notes = notes.trim()
        )
        onSave(courseToSave)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (initialCourse == null) "Add Course".tr else "Edit Course".tr,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close".tr)
                            }
                        },
                        actions = {
                            Button(
                                onClick = { handleSave() },
                                enabled = name.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save".tr, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Conflict warning if any
                    if (conflictingExistingCourses.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Overlaps with ${conflictingExistingCourses.joinToString { it.name }} at Period $startPeriod-$endPeriod",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    // 1. Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Course Name *".tr, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("e.g. Calculus I, Data Structures") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 2. Day of Week
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Day of Week *".tr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DAYS_OF_WEEK_LABELS.forEach { (dayNum, label) ->
                                val isSelected = selectedDay == dayNum
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedDay = dayNum },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }
                    }

                    // 3. Start-End (Period & Time)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Periods & Time *".tr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$calculatedStartTime ~ $calculatedEndTime",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start period selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start Period", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (startPeriod > 1) { startPeriod--; if (endPeriod < startPeriod) endPeriod = startPeriod } },
                                        enabled = startPeriod > 1
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "Section $startPeriod",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { if (startPeriod < 12) { startPeriod++; if (endPeriod < startPeriod) endPeriod = startPeriod } },
                                        enabled = startPeriod < 12
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                    }
                                }
                            }

                            // End period selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text("End Period", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (endPeriod > startPeriod) endPeriod-- },
                                        enabled = endPeriod > startPeriod
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "Section $endPeriod",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { if (endPeriod < 12) endPeriod++ },
                                        enabled = endPeriod < 12
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                    }
                                }
                            }
                        }
                    }

                    // 4. Room (Pre-filled with last-used room)
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room / Location".tr) },
                        placeholder = { Text("e.g. Science Bldg 302, Lab A") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 5. Weeks (All / Odd / Even / Range)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Weeks Rotation *".tr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = weekRule == WeekRule.ALL && weekRangeText.isBlank(),
                                onClick = { weekRule = WeekRule.ALL; weekRangeText = "" },
                                label = { Text("All Weeks".tr, fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = weekRule == WeekRule.ODD,
                                onClick = { weekRule = WeekRule.ODD; weekRangeText = "" },
                                label = { Text("Odd (单周)".tr, fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = weekRule == WeekRule.EVEN,
                                onClick = { weekRule = WeekRule.EVEN; weekRangeText = "" },
                                label = { Text("Even (双周)".tr, fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = weekRangeText.isNotBlank(),
                                onClick = {
                                    weekRule = WeekRule.ALL
                                    weekRangeText = if (weekRangeText.isBlank()) "1-16" else ""
                                },
                                label = { Text("Range".tr, fontSize = 12.sp) }
                            )
                        }

                        if (weekRangeText.isNotBlank()) {
                            OutlinedTextField(
                                value = weekRangeText,
                                onValueChange = { weekRangeText = it },
                                label = { Text("Week Range (e.g. 1-16, 1-8)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Collapsible "More options"
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMoreOptions = !showMoreOptions }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "More Options".tr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    if (showMoreOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = showMoreOptions) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Code & Instructor
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = code,
                                            onValueChange = { code = it },
                                            label = { Text("Course Code".tr) },
                                            placeholder = { Text("CS101") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        OutlinedTextField(
                                            value = instructor,
                                            onValueChange = { instructor = it },
                                            label = { Text("Teacher".tr) },
                                            placeholder = { Text("Prof. Turing") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    // Credits & Semester (Pre-filled with last-used semester)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = credits,
                                            onValueChange = { credits = it },
                                            label = { Text("Credits".tr) },
                                            singleLine = true,
                                            modifier = Modifier.weight(0.8f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        OutlinedTextField(
                                            value = semester,
                                            onValueChange = { semester = it },
                                            label = { Text("Semester".tr) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.2f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    // Color Picker
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Badge Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            DEFAULT_COLORS.forEach { hex ->
                                                val c = Color(android.graphics.Color.parseColor(hex))
                                                val isSelected = colorHex.equals(hex, ignoreCase = true)
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(c)
                                                        .border(
                                                            width = if (isSelected) 3.dp else 1.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                            shape = CircleShape
                                                        )
                                                        .clickable { colorHex = hex }
                                                )
                                            }
                                        }
                                    }

                                    // Retake & Notes
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = isRetake,
                                            onCheckedChange = { isRetake = it }
                                        )
                                        Text("Is Retake Course (重修)", fontSize = 13.sp)
                                    }

                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text("Notes & Syllabus".tr) },
                                        placeholder = { Text("Exam dates, grading policy...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
