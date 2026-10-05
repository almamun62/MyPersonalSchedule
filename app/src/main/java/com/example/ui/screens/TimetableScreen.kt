package com.example.ui.screens

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
    onNavigateToCourses: (() -> Unit)? = null
) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val selectedWeek by viewModel.selectedWeek.collectAsStateWithLifecycle()
    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val timings by viewModel.userPreferencesManager.sectionTimings.collectAsStateWithLifecycle()

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showSectionTimingsDialog by remember { mutableStateOf(false) }
    var courseToQuickEdit by remember { mutableStateOf<Course?>(null) }
    var initialAddPeriod by remember { mutableIntStateOf(1) }
    var weekDropdownExpanded by remember { mutableStateOf(false) }

    val dayCourses = remember(allCourses, selectedDay) {
        allCourses.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startPeriod }
    }

    val dayNamesShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayDates = listOf("28", "29", "30", "1", "2", "3", "4") // Sample academic week dates as in screenshot
    val selectedDayName = dayNamesShort.getOrNull(selectedDay - 1) ?: "Sun"

    Scaffold(
        containerColor = Color(0xFF0B0F17),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F17)),
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { weekDropdownExpanded = true }
                        ) {
                            Text(
                                text = "Week $selectedWeek, $selectedDayName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Week",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "10/4/26",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
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
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.White)
                    }
                    IconButton(onClick = {
                        initialAddPeriod = 1
                        showAddCourseDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToImport) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = "Import", tint = Color.White)
                    }
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = { showSectionTimingsDialog = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF243348),
                    onClick = onNavigateToImport,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = "Import", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }

                Button(
                    onClick = {
                        initialAddPeriod = 1
                        showAddCourseDialog = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8C7FA)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0F1E33), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Course", color = Color(0xFF0F1E33), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Date Strip (Exact match to Screenshot 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leftmost "Oct" icon card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF182230),
                    modifier = Modifier
                        .width(44.dp)
                        .height(64.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Oct", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8EADC9))
                        Spacer(modifier = Modifier.height(2.dp))
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                }

                // 7 Day Cards (Mon 28 .. Sun 4)
                for (d in 1..7) {
                    val isSelected = d == selectedDay
                    val dayName = dayNamesShort[d - 1]
                    val dateNum = dayDates[d - 1]
                    val hasCourses = allCourses.any { it.dayOfWeek == d }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFA8C7FA) else Color(0xFF182230),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable { viewModel.setSelectedDay(d) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                color = if (isSelected) Color(0xFF0F1E33) else Color(0xFF94A3B8),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )

                            Text(
                                text = dateNum,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F1E33) else Color.White
                            )

                            // Dot below
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> Color(0xFF0F1E33)
                                            hasCourses -> Color(0xFFA8C7FA)
                                            else -> Color.Transparent
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timetable Periods Grid (Exact match to Screenshot 2)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                // Left Column: Periods & Intermission Badges
                Column(modifier = Modifier.width(56.dp)) {
                    for (period in 1..12) {
                        val timing = timings.getOrNull(period - 1)
                        val sTime = timing?.startTime ?: "08:00"
                        val eTime = timing?.endTime ?: "08:45"

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$period", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Text(sTime, fontSize = 9.sp, color = Color(0xFF94A3B8))
                                Text(eTime, fontSize = 9.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        // Intermission pills after periods 2, 5, 7
                        if (period == 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2C1E38)
                                ) {
                                    Text("15m", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE4C7FA), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        } else if (period == 5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2C1E38)
                                ) {
                                    Text("135m", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE4C7FA), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        } else if (period == 7) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2C1E38)
                                ) {
                                    Text("15m", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE4C7FA), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Column: Course Cards, Empty Slots, and Break Bars
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
                                val cardHeight = (64 * span).dp

                                val cardColor = try {
                                    Color(android.graphics.Color.parseColor(course.colorHex))
                                } catch (e: Exception) {
                                    Color(0xFF2563EB)
                                }

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(cardHeight)
                                        .padding(vertical = 2.dp)
                                        .clickable { courseToQuickEdit = course }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = course.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (course.classroom.isNotBlank()) {
                                                Text(
                                                    text = "📍 ${course.classroom}",
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Periods $startP-$endP (${course.startTime}-${course.endTime})",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Render intermission bars if passed period 2, 5, 7
                            if (maxEndP == 2) {
                                Surface(
                                    color = Color(0xFF2C1E38),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F2652)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .padding(vertical = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Coffee, contentDescription = null, tint = Color(0xFFE4C7FA), modifier = Modifier.size(14.dp))
                                            Text("Break Time • 15m", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE4C7FA))
                                        }
                                        Text("09:35 - 09:50", fontSize = 10.sp, color = Color(0xFFE4C7FA).copy(alpha = 0.8f))
                                    }
                                }
                            } else if (maxEndP == 5) {
                                Surface(
                                    color = Color(0xFF2C1E38),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F2652)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .padding(vertical = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Coffee, contentDescription = null, tint = Color(0xFFE4C7FA), modifier = Modifier.size(14.dp))
                                            Text("Lunch Break • 2h 15m", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE4C7FA))
                                        }
                                        Text("12:15 - 14:30", fontSize = 10.sp, color = Color(0xFFE4C7FA).copy(alpha = 0.8f))
                                    }
                                }
                            } else if (maxEndP == 7) {
                                Surface(
                                    color = Color(0xFF2C1E38),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F2652)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .padding(vertical = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Coffee, contentDescription = null, tint = Color(0xFFE4C7FA), modifier = Modifier.size(14.dp))
                                            Text("Break Time • 15m", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE4C7FA))
                                        }
                                        Text("16:05 - 16:20", fontSize = 10.sp, color = Color(0xFFE4C7FA).copy(alpha = 0.8f))
                                    }
                                }
                            }

                            currPeriod = maxEndP + 1
                        } else {
                            val emptyP = currPeriod
                            val isCovered = dayCourses.any {
                                val sP = it.getStartPeriod(timings)
                                val eP = it.getEndPeriod(timings)
                                sP <= emptyP && eP >= emptyP
                            }

                            if (!isCovered) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF131B26))
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF1D2838),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            initialAddPeriod = emptyP
                                            showAddCourseDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ Period $emptyP",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF6B7280),
                                        fontWeight = FontWeight.Normal
                                    )
                                }

                                if (emptyP == 2) {
                                    Surface(
                                        color = Color(0xFF2C1E38),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F2652)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .padding(vertical = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Coffee, contentDescription = null, tint = Color(0xFFE4C7FA), modifier = Modifier.size(14.dp))
                                                Text("Break Time • 15m", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE4C7FA))
                                            }
                                            Text("09:35 - 09:50", fontSize = 10.sp, color = Color(0xFFE4C7FA).copy(alpha = 0.8f))
                                        }
                                    }
                                } else if (emptyP == 5) {
                                    Surface(
                                        color = Color(0xFF2C1E38),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F2652)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .padding(vertical = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Coffee, contentDescription = null, tint = Color(0xFFE4C7FA), modifier = Modifier.size(14.dp))
                                                Text("Lunch Break • 2h 15m", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE4C7FA))
                                            }
                                            Text("12:15 - 14:30", fontSize = 10.sp, color = Color(0xFFE4C7FA).copy(alpha = 0.8f))
                                        }
                                    }
                                } else if (emptyP == 7) {
                                    Surface(
                                        color = Color(0xFF2C1E38),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F2652)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .padding(vertical = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Coffee, contentDescription = null, tint = Color(0xFFE4C7FA), modifier = Modifier.size(14.dp))
                                                Text("Break Time • 15m", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE4C7FA))
                                            }
                                            Text("16:05 - 16:20", fontSize = 10.sp, color = Color(0xFFE4C7FA).copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }

                            currPeriod++
                        }
                    }
                }
            }
        }
    }

    if (showAddCourseDialog) {
        AddEditCourseDialog(
            initialDay = selectedDay,
            initialStartPeriod = initialAddPeriod,
            onDismiss = { showAddCourseDialog = false },
            onSave = { course ->
                viewModel.addCourse(course)
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
            onSave = { newRoom, sP, eP, sT, eT, _ ->
                val updated = course.copy(
                    classroom = newRoom,
                    startPeriod = sP,
                    endPeriod = eP,
                    startTime = sT,
                    endTime = eT
                )
                viewModel.updateCourse(updated)
                courseToQuickEdit = null
            }
        )
    }
}
