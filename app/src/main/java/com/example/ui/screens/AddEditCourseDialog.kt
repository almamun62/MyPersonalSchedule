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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    "#EC4899"  // Pink
)

class TimeSlotHolder(
    initialDayOfWeek: Int = 1,
    initialStartPeriod: Int = 1,
    initialEndPeriod: Int = 2,
    initialStartTime: String = "08:00",
    initialEndTime: String = "09:35",
    initialClassroom: String = "",
    initialInstructor: String = "",
    initialWeekRule: WeekRule = WeekRule.ALL,
    initialNotes: String = ""
) {
    val selectedDays = mutableStateListOf<Int>(initialDayOfWeek)
    var startPeriod by mutableIntStateOf(initialStartPeriod)
    var endPeriod by mutableIntStateOf(initialEndPeriod)
    var startTime by mutableStateOf(initialStartTime)
    var endTime by mutableStateOf(initialEndTime)
    var classroom by mutableStateOf(initialClassroom)
    var instructor by mutableStateOf(initialInstructor)
    var weekRule by mutableStateOf(initialWeekRule)
    var notes by mutableStateOf(initialNotes)
}

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
    // Smart Defaults from previous course entry
    val lastCourse = existingCourses.lastOrNull()
    val defaultClassroom = initialCourse?.classroom ?: lastCourse?.classroom ?: ""
    val defaultInstructor = initialCourse?.instructor ?: lastCourse?.instructor ?: ""
    val defaultColor = initialCourse?.colorHex ?: DEFAULT_COLORS[existingCourses.size % DEFAULT_COLORS.size]

    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var code by remember { mutableStateOf(initialCourse?.code ?: "") }
    var colorHex by remember { mutableStateOf(defaultColor) }
    var credits by remember { mutableStateOf(initialCourse?.credits?.toString() ?: "3") }
    var semester by remember { mutableStateOf(initialCourse?.semester ?: defaultSemester) }
    var isRetake by remember { mutableStateOf(initialCourse?.isRetake ?: false) }
    var showAdvancedOptions by remember { mutableStateOf(false) }

    val defaultTiming = UserPreferencesManager.defaultSectionTimings

    val slot = remember {
        TimeSlotHolder(
            initialDayOfWeek = initialCourse?.dayOfWeek ?: initialDay,
            initialStartPeriod = initialCourse?.startPeriod ?: initialStartPeriod,
            initialEndPeriod = initialCourse?.endPeriod ?: (initialStartPeriod + 1).coerceAtMost(12),
            initialStartTime = initialCourse?.startTime ?: defaultTiming.getOrNull(initialStartPeriod - 1)?.startTime ?: "08:00",
            initialEndTime = initialCourse?.endTime ?: defaultTiming.getOrNull(initialStartPeriod)?.endTime ?: "09:35",
            initialClassroom = defaultClassroom,
            initialInstructor = defaultInstructor,
            initialWeekRule = initialCourse?.weekRule ?: WeekRule.ALL,
            initialNotes = initialCourse?.notes ?: ""
        )
    }

    // Live conflict detector for current slot being configured
    val conflictingExistingCourses = remember(slot.selectedDays.toList(), slot.startPeriod, slot.endPeriod, slot.weekRule, existingCourses) {
        val days = if (slot.selectedDays.isEmpty()) listOf(initialDay) else slot.selectedDays.toList()
        existingCourses.filter { existing ->
            if (initialCourse != null && existing.id == initialCourse.id) return@filter false
            days.contains(existing.dayOfWeek) &&
                CourseConflictDetector.doWeekRulesOverlap(slot.weekRule, existing.weekRule) &&
                (slot.startPeriod <= existing.endPeriod) && (existing.startPeriod <= slot.endPeriod)
        }
    }

    fun handleSave() {
        if (name.isBlank()) return
        val generatedCourses = mutableListOf<Course>()
        val daysList = if (slot.selectedDays.isEmpty()) listOf(initialDay) else slot.selectedDays.toList()

        daysList.forEachIndexed { idx, dayNum ->
            generatedCourses.add(
                Course(
                    id = if (idx == 0 && initialCourse != null) initialCourse.id else 0,
                    name = name.trim(),
                    code = code.trim(),
                    instructor = slot.instructor.trim(),
                    classroom = slot.classroom.trim(),
                    dayOfWeek = dayNum,
                    startPeriod = slot.startPeriod,
                    endPeriod = slot.endPeriod,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    weekRule = slot.weekRule,
                    colorHex = colorHex,
                    semester = semester,
                    credits = credits.toIntOrNull() ?: 3,
                    isRetake = isRetake,
                    notes = slot.notes.trim()
                )
            )
        }

        if (generatedCourses.size > 1 && onSaveMultiple != null) {
            onSaveMultiple(generatedCourses)
        } else {
            generatedCourses.firstOrNull()?.let { onSave(it) }
        }
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
                modifier = Modifier.statusBarsPadding(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                        title = {
                            Text(
                                text = if (initialCourse == null) "Add Class".tr else "Edit Class".tr,
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
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val isTabletWide = maxWidth >= 600.dp

                    if (isTabletWide) {
                        // ==========================================
                        // TABLET 2-PANE MASTER-DETAIL LAYOUT
                        // Left: Interactive Grid (Hero)
                        // Right: Class Details (Name, Room, Teacher)
                        // ==========================================
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Pane: Big Interactive Schedule Grid
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "🗓️ Schedule Grid",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Tap a period to select. Tap another to set range.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    InteractiveScheduleGrid(
                                        slot = slot,
                                        defaultTiming = defaultTiming,
                                        activeColorHex = colorHex
                                    )
                                }
                            }

                            // Right Pane: Name, Room, Teacher, and Advanced Options
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Conflict Warning Box
                                    if (conflictingExistingCourses.isNotEmpty()) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                                Text(
                                                    text = "Overlaps with ${conflictingExistingCourses.joinToString { it.name }} at Period ${slot.startPeriod}-${slot.endPeriod}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                        }
                                    }

                                    // 1. Course Name (Hero Input)
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Course Name *".tr, fontWeight = FontWeight.Bold) },
                                        placeholder = { Text("e.g. Calculus, Physics") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    )

                                    // 2. Room & Teacher (Pre-filled)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = slot.classroom,
                                            onValueChange = { slot.classroom = it },
                                            label = { Text("Room".tr) },
                                            placeholder = { Text("Room 302") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = slot.instructor,
                                            onValueChange = { slot.instructor = it },
                                            label = { Text("Teacher".tr) },
                                            placeholder = { Text("Prof. Smith") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    // 3. Collapsible Advanced Options
                                    AdvancedOptionsSection(
                                        colorHex = colorHex,
                                        onColorChange = { colorHex = it },
                                        code = code,
                                        onCodeChange = { code = it },
                                        credits = credits,
                                        onCreditsChange = { credits = it },
                                        isRetake = isRetake,
                                        onRetakeChange = { isRetake = it },
                                        slot = slot,
                                        showAdvanced = showAdvancedOptions,
                                        onToggleAdvanced = { showAdvancedOptions = !showAdvancedOptions }
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    Button(
                                        onClick = { handleSave() },
                                        enabled = name.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Save Course".tr, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // ==========================================
                        // PHONE STREAMLINED 5-SECOND FLOW
                        // 1. Course Name (Top)
                        // 2. Room & Teacher (Pre-filled)
                        // 3. Interactive Grid (The Main Event)
                        // 4. Advanced Options (Collapsed by default)
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Conflict Warning Box
                            if (conflictingExistingCourses.isNotEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "Overlaps with ${conflictingExistingCourses.joinToString { it.name }} at Period ${slot.startPeriod}-${slot.endPeriod}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }

                            // 1. Course Name (Top, Big & Bold)
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Course Name *".tr, fontWeight = FontWeight.Bold) },
                                placeholder = { Text("e.g. Calculus, Physics") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            // 2. Room & Teacher (Pre-filled from previous entry)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = slot.classroom,
                                    onValueChange = { slot.classroom = it },
                                    label = { Text("Room".tr) },
                                    placeholder = { Text("Room 302") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = slot.instructor,
                                    onValueChange = { slot.instructor = it },
                                    label = { Text("Teacher".tr) },
                                    placeholder = { Text("Prof. Smith") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // 3. Interactive Grid (The Hero Event)
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
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
                                        Text(
                                            text = "🗓️ Schedule Grid",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Tap to set day & periods",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    InteractiveScheduleGrid(
                                        slot = slot,
                                        defaultTiming = defaultTiming,
                                        activeColorHex = colorHex
                                    )
                                }
                            }

                            // 4. Collapsible Advanced Options (Hidden by default)
                            AdvancedOptionsSection(
                                colorHex = colorHex,
                                onColorChange = { colorHex = it },
                                code = code,
                                onCodeChange = { code = it },
                                credits = credits,
                                onCreditsChange = { credits = it },
                                isRetake = isRetake,
                                onRetakeChange = { isRetake = it },
                                slot = slot,
                                showAdvanced = showAdvancedOptions,
                                onToggleAdvanced = { showAdvancedOptions = !showAdvancedOptions }
                            )

                            Button(
                                onClick = { handleSave() },
                                enabled = name.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Save Course".tr, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean, touch-friendly 7-Day x 12-Period Interactive Grid
 * Tapping a cell sets day and period.
 * Tapping another cell in the same day extends the period range (e.g. P1 to P3).
 * Tapping column header toggles multi-day selection (e.g. Mon, Wed, Fri).
 */
@Composable
private fun InteractiveScheduleGrid(
    slot: TimeSlotHolder,
    defaultTiming: List<com.example.data.model.SectionTiming>,
    activeColorHex: String
) {
    val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val activeColor = try {
        Color(android.graphics.Color.parseColor(activeColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    fun onCellClick(day: Int, period: Int) {
        if (!slot.selectedDays.contains(day)) {
            // Switch to this day and period
            slot.selectedDays.clear()
            slot.selectedDays.add(day)
            slot.startPeriod = period
            slot.endPeriod = period
        } else {
            // Already on this day
            if (slot.startPeriod == period && slot.endPeriod == period) {
                // already single period, keep
            } else if (period in slot.startPeriod..slot.endPeriod) {
                // Clicked inside existing range -> collapse to this single period
                slot.startPeriod = period
                slot.endPeriod = period
            } else if (period > slot.endPeriod) {
                // Extend end period (e.g. was P1-P2, clicked P3 -> P1-P3)
                slot.endPeriod = period
            } else if (period < slot.startPeriod) {
                // Extend start period (e.g. was P3-P4, clicked P2 -> P2-P4)
                slot.startPeriod = period
            }
        }

        // Auto-calculate start & end times silently from global period timings
        val sTiming = defaultTiming.getOrNull(slot.startPeriod - 1)
        val eTiming = defaultTiming.getOrNull(slot.endPeriod - 1)
        slot.startTime = sTiming?.startTime ?: "08:00"
        slot.endTime = eTiming?.endTime ?: "09:35"
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Day Headers Row (Tap to toggle multi-day)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(modifier = Modifier.width(32.dp))
            dayHeaders.forEachIndexed { idx, h ->
                val dayNum = idx + 1
                val isSelectedDay = slot.selectedDays.contains(dayNum)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelectedDay) activeColor.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable {
                            if (isSelectedDay && slot.selectedDays.size > 1) {
                                slot.selectedDays.remove(dayNum)
                            } else if (!isSelectedDay) {
                                slot.selectedDays.add(dayNum)
                            }
                        }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = h,
                        fontSize = 11.sp,
                        fontWeight = if (isSelectedDay) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelectedDay) activeColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Periods P1 to P12 Rows
        for (p in 1..12) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Period Label
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(30.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "P$p",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 7 Day Cells
                for (d in 1..7) {
                    val isSelectedCell = slot.selectedDays.contains(d) && p in slot.startPeriod..slot.endPeriod

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelectedCell) activeColor
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                            .clickable { onCellClick(d, p) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelectedCell) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Collapsible Advanced Options (Hidden by default to keep the main flow under 5 seconds)
 * Contains: 5-color palette, Credits/Code, Retake status, Custom times, Frequency
 */
@Composable
private fun AdvancedOptionsSection(
    colorHex: String,
    onColorChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    credits: String,
    onCreditsChange: (String) -> Unit,
    isRetake: Boolean,
    onRetakeChange: (Boolean) -> Unit,
    slot: TimeSlotHolder,
    showAdvanced: Boolean,
    onToggleAdvanced: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Toggle Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggleAdvanced() }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showAdvanced) "Hide Advanced Options" else "More Options (Color, Credits, Retake)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = showAdvanced) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Color Palette (Compact 5 default colors)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color Tag", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DEFAULT_COLORS.forEach { hex ->
                            val isSelected = colorHex.equals(hex, ignoreCase = true)
                            val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorChange(hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // 2. Credits & Course Code (Compact row)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = onCodeChange,
                        label = { Text("Code".tr) },
                        placeholder = { Text("CS101") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = credits,
                        onValueChange = onCreditsChange,
                        label = { Text("Credits".tr) },
                        placeholder = { Text("3") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // 3. Retake Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Course Retake", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Mark if retaking this class", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isRetake, onCheckedChange = onRetakeChange)
                }

                // 4. Custom Times (Optional manual override)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = slot.startTime,
                        onValueChange = { slot.startTime = it },
                        label = { Text("Start Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = slot.endTime,
                        onValueChange = { slot.endTime = it },
                        label = { Text("End Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // 5. Frequency
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val rules = listOf(
                        WeekRule.ALL to "Weekly",
                        WeekRule.ODD to "Odd",
                        WeekRule.EVEN to "Even"
                    )
                    rules.forEachIndexed { idx, (rule, label) ->
                        SegmentedButton(
                            selected = slot.weekRule == rule,
                            onClick = { slot.weekRule = rule },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = rules.size)
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
