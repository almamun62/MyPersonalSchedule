package com.example.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SectionTiming
import com.example.domain.model.Course
import com.example.ui.components.QuickModifyClassDialog
import com.example.ui.components.SectionTimingsDialog
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import com.example.ui.viewmodel.ScheduleViewMode

private fun parseTimeToMinutes(timeStr: String): Int {
    val clean = timeStr.trim()
    val parts = clean.split(":")
    if (parts.size >= 2) {
        val h = parts[0].trim().toIntOrNull() ?: 0
        val m = parts[1].trim().toIntOrNull() ?: 0
        return h * 60 + m
    }
    return 0
}

fun Course.getStartPeriod(timings: List<SectionTiming>): Int {
    if (startPeriod in 1..12) return startPeriod
    val startMin = parseTimeToMinutes(startTime)
    if (startMin == 0) return 1
    val match = timings.indexOfFirst {
        val sMin = parseTimeToMinutes(it.startTime)
        val eMin = parseTimeToMinutes(it.endTime)
        startMin in sMin..eMin
    }
    if (match != -1) return match + 1
    val byStart = timings.indexOfFirst { parseTimeToMinutes(it.startTime) >= startMin }
    if (byStart != -1) return byStart + 1
    return 1
}

fun Course.getEndPeriod(timings: List<SectionTiming>): Int {
    if (endPeriod in startPeriod..12) return endPeriod
    val endMin = parseTimeToMinutes(endTime)
    if (endMin == 0) return getStartPeriod(timings)
    val match = timings.indexOfLast {
        val sMin = parseTimeToMinutes(it.startTime)
        val eMin = parseTimeToMinutes(it.endTime)
        endMin in sMin..eMin
    }
    if (match != -1) return match + 1
    val byEnd = timings.indexOfLast { parseTimeToMinutes(it.endTime) <= endMin }
    if (byEnd != -1) return (byEnd + 1).coerceAtLeast(getStartPeriod(timings))
    return getStartPeriod(timings).coerceAtLeast(1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: ScheduleViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToCourses: (() -> Unit)? = null,
    onOpenAcademicCalendar: (() -> Unit)? = null
) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val selectedWeek by viewModel.selectedWeek.collectAsStateWithLifecycle()
    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val timings by viewModel.userPreferencesManager.sectionTimings.collectAsStateWithLifecycle()
    val scheduleViewMode by viewModel.scheduleViewMode.collectAsStateWithLifecycle()

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showSectionTimingsDialog by remember { mutableStateOf(false) }
    var courseToQuickEdit by remember { mutableStateOf<Course?>(null) }
    var initialAddPeriod by remember { mutableIntStateOf(1) }
    var weekDropdownExpanded by remember { mutableStateOf(false) }

    val dayCourses = remember(allCourses, selectedDay) {
        allCourses.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startPeriod }
    }

    val dayNamesShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayDates = listOf("28", "29", "30", "1", "2", "3", "4")
    val selectedDayName = dayNamesShort.getOrNull(selectedDay - 1) ?: "Sun"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        val isWeekGrid = scheduleViewMode == ScheduleViewMode.WEEK_GRID || (isTablet && scheduleViewMode != ScheduleViewMode.DAY)

        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { weekDropdownExpanded = true }
                            ) {
                                Text(
                                    text = if (isWeekGrid) "Week $selectedWeek" else "Week $selectedWeek, $selectedDayName",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Week",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Fall 2026",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            DropdownMenu(
                                expanded = weekDropdownExpanded,
                                onDismissRequest = { weekDropdownExpanded = false }
                            ) {
                                for (w in 1..20) {
                                    DropdownMenuItem(
                                        text = { Text("Week $w") },
                                        onClick = {
                                            viewModel.setSelectedWeek(w)
                                            weekDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { onOpenAcademicCalendar?.invoke() }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Academic Calendar", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onNavigateToImport) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = "Import", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = { showSectionTimingsDialog = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        initialAddPeriod = 1
                        showAddCourseDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Course".tr, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ==========================================
                // SIMPLE VIEW-TYPE TOGGLE (Day vs. Week)
                // ==========================================
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 6.dp)
                ) {
                    SegmentedButton(
                        selected = !isWeekGrid,
                        onClick = { viewModel.setScheduleViewMode(ScheduleViewMode.DAY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Day View", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                    SegmentedButton(
                        selected = isWeekGrid,
                        onClick = { viewModel.setScheduleViewMode(ScheduleViewMode.WEEK_GRID) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Week View", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                if (isWeekGrid) {
                    // ==========================================
                    // WEEK GRID VIEW (Mon - Sun side-by-side)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (isTablet) 16.dp else 8.dp, vertical = 4.dp)
                    ) {
                        // Week Day Column Headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Time",
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            dayNamesShort.forEachIndexed { idx, dayName ->
                                val dayNum = idx + 1
                                val isToday = dayNum == selectedDay
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 12 Periods Row Grid
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (p in 1..12) {
                                val timing = timings.getOrNull(p - 1)
                                val sTime = timing?.startTime ?: "08:00"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (isTablet) 60.dp else 48.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Clean time marker on the left
                                    Text(
                                        text = sTime,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    for (d in 1..7) {
                                        val courseInSlot = allCourses.find {
                                            it.dayOfWeek == d && p in it.getStartPeriod(timings)..it.getEndPeriod(timings)
                                        }

                                        // Subtle empty cell or course block (NO period labels in empty slots)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .padding(horizontal = 1.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (courseInSlot != null) {
                                                        try {
                                                            Color(android.graphics.Color.parseColor(courseInSlot.colorHex))
                                                        } catch (e: Exception) {
                                                            MaterialTheme.colorScheme.primary
                                                        }
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                                    }
                                                )
                                                .clickable {
                                                    if (courseInSlot != null) {
                                                        courseToQuickEdit = courseInSlot
                                                    } else {
                                                        viewModel.setSelectedDay(d)
                                                        initialAddPeriod = p
                                                        showAddCourseDialog = true
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (courseInSlot != null) {
                                                Text(
                                                    text = courseInSlot.name,
                                                    fontSize = if (isTablet) 11.sp else 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 11.sp,
                                                    modifier = Modifier.padding(2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Clearance so FAB never covers the last class
                            Spacer(modifier = Modifier.height(88.dp))
                        }
                    }
                } else {
                    // ==========================================
                    // DAY VIEW (Clean timeline, subtle indicators)
                    // ==========================================

                    // Compact Date Selector Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (d in 1..7) {
                            val isSelected = d == selectedDay
                            val dayName = dayNamesShort[d - 1]
                            val dateNum = dayDates[d - 1]
                            val hasCourses = allCourses.any { it.dayOfWeek == d }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clickable { viewModel.setSelectedDay(d) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dayName,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )

                                    Text(
                                        text = dateNum,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    hasCourses -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Day Timeline (Clean time on left, subtle empty indicators on right)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        // Left Column: Times
                        Column(modifier = Modifier.width(46.dp)) {
                            for (period in 1..12) {
                                val timing = timings.getOrNull(period - 1)
                                val sTime = timing?.startTime ?: "08:00"

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = sTime,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right Column: Course Cards & Subtle Empty Slots
                        Column(modifier = Modifier.weight(1f)) {
                            var currPeriod = 1
                            while (currPeriod <= 12) {
                                val startingCourses = dayCourses.filter { it.getStartPeriod(timings) == currPeriod }

                                if (startingCourses.isNotEmpty()) {
                                    var maxEndP = currPeriod
                                    startingCourses.forEach { course ->
                                        val startP = course.getStartPeriod(timings)
                                        val endP = course.getEndPeriod(timings).coerceAtLeast(startP)
                                        maxEndP = maxOf(maxEndP, endP)
                                        val span = (endP - startP + 1).coerceAtLeast(1)
                                        val cardHeight = (58 * span).dp

                                        val cardColor = try {
                                            Color(android.graphics.Color.parseColor(course.colorHex))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }

                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = cardColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(cardHeight)
                                                .padding(vertical = 2.dp)
                                                .clickable { courseToQuickEdit = course }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = course.name,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 14.sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        if (course.isRetake) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFF59E0B)
                                                            ) {
                                                                Text(
                                                                    text = "Retake",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = Color.Black,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (course.classroom.isNotBlank()) {
                                                        Text(
                                                            text = "📍 ${course.classroom}",
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "${course.startTime} - ${course.endTime}",
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }

                                    currPeriod = maxEndP + 1
                                } else {
                                    val targetP = currPeriod

                                    // Subtle indicator for empty time slot:
                                    // NO period labels ("+ Period X" removed completely)
                                    // Only a quiet, clean surface with a subtle faint indicator line
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .padding(vertical = 2.dp)
                                            .clickable {
                                                initialAddPeriod = targetP
                                                showAddCourseDialog = true
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Subtle indicator line / quiet plus sign
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add class",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    currPeriod++
                                }
                            }

                            // Clearance so FAB never covers the last class
                            Spacer(modifier = Modifier.height(88.dp))
                        }
                    }
                }
            }
        }

        if (showAddCourseDialog) {
            AddEditCourseDialog(
                initialCourse = null,
                existingCourses = allCourses,
                defaultSemester = "Fall 2026",
                initialDay = selectedDay,
                initialStartPeriod = initialAddPeriod,
                onDismiss = { showAddCourseDialog = false },
                onSave = { newCourse ->
                    viewModel.addCourse(newCourse)
                    showAddCourseDialog = false
                },
                onSaveMultiple = { newCourses ->
                    newCourses.forEach { viewModel.addCourse(it) }
                    showAddCourseDialog = false
                }
            )
        }

        if (showSectionTimingsDialog) {
            SectionTimingsDialog(
                currentTimings = timings,
                onDismiss = { showSectionTimingsDialog = false },
                onSave = { updated ->
                    viewModel.userPreferencesManager.setSectionTimings(updated)
                    showSectionTimingsDialog = false
                }
            )
        }

        courseToQuickEdit?.let { course ->
            QuickModifyClassDialog(
                course = course.toEntity(),
                onDismiss = { courseToQuickEdit = null },
                onSave = { newClassroom, newStartP, newEndP, newStartT, newEndT, _ ->
                    val updated = course.copy(
                        classroom = newClassroom,
                        startPeriod = newStartP,
                        endPeriod = newEndP,
                        startTime = newStartT,
                        endTime = newEndT
                    )
                    viewModel.updateCourse(updated)
                    courseToQuickEdit = null
                }
            )
        }
    }
}
