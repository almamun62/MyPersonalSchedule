package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.domain.ScheduleEngine
import com.example.ui.components.AddCourseDialog
import com.example.ui.viewmodel.ScheduleUiState
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCourseForDetails by remember { mutableStateOf<CourseEntity?>(null) }
    var showCsvImportDialog by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val totalWeeks = state.activeSemester?.totalWeeks ?: 16
    val currentWeek = state.currentAcademicWeek
    val selectedWeek = state.selectedScheduleWeek
    val totalPeriods = 12

    // Filter courses active in selectedWeek
    val activeCoursesInWeek = remember(state.courses, selectedWeek, totalWeeks) {
        state.courses.filter { course ->
            ScheduleEngine.isCourseActiveInWeek(
                rule = course.weekRule,
                customWeeks = course.customWeeks,
                currentWeek = selectedWeek,
                totalWeeks = totalWeeks
            )
        }
    }

    if (showAddDialog) {
        AddCourseDialog(
            semesterId = state.activeSemester?.id ?: 1,
            onDismiss = { showAddDialog = false },
            onConfirm = { course ->
                viewModel.addCourse(course)
                showAddDialog = false
            }
        )
    }

    if (selectedCourseForDetails != null) {
        val course = selectedCourseForDetails!!
        AlertDialog(
            onDismissRequest = { selectedCourseForDetails = null },
            title = { Text(course.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Code: ${course.code}")
                    Text("Classroom: ${course.classroom}")
                    Text("Instructor: ${course.instructor}")
                    Text("Time: ${course.startTime} - ${course.endTime} (Section ${course.startPeriod}-${course.endPeriod})")
                    Text("Week Rule: ${course.weekRule.name} ${if (course.customWeeks.isNotEmpty()) "(${course.customWeeks})" else ""}")
                    Text("Auto-DND: ${if (course.dndEnabled) "Enabled" else "Disabled"}")
                    if (course.notes.isNotEmpty()) {
                        Text("Notes: ${course.notes}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCourseForDetails = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCourse(course)
                        selectedCourseForDetails = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Course")
                }
            }
        )
    }

    if (showCsvImportDialog) {
        CsvImportDialog(
            semesterId = state.activeSemester?.id ?: 1,
            onDismiss = { showCsvImportDialog = false },
            onImport = { newCourses ->
                newCourses.forEach { viewModel.addCourse(it) }
                showCsvImportDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("fab_add_course"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Course")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Premium HyperOS Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Week $selectedWeek",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (selectedWeek == currentWeek) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Current",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "${activeCoursesInWeek.size} classes this week",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledIconButton(
                    onClick = { showCsvImportDialog = true },
                    modifier = Modifier.testTag("quick_csv_import_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Quick CSV Import")
                }
            }

            // Week Selector Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(totalWeeks) { index ->
                    val weekNum = index + 1
                    val isSelected = weekNum == selectedWeek
                    val isCurrent = weekNum == currentWeek

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        contentColor = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        shadowElevation = if (isSelected) 8.dp else 0.dp,
                        modifier = Modifier
                            .clickable { viewModel.setSelectedScheduleWeek(weekNum) }
                            .widthIn(min = 60.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "W$weekNum",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Timetable Grid: 7 Day Columns + Period Rows
            val scrollStateHorizontal = rememberScrollState()
            val scrollStateVertical = rememberScrollState()
            val configuration = LocalConfiguration.current
            
            // Layout calculations
            val timeColumnWidth = 40.dp
            // Calculate day width so all 7 days fit horizontally without scrolling, but ensure it's not too small.
            // Screen width - timeColumn - padding
            val screenWidth = configuration.screenWidthDp.dp
            val availableWidth = screenWidth - timeColumnWidth - 16.dp
            val calculatedDayWidth = availableWidth / 7
            val dayWidth = if (calculatedDayWidth < 45.dp) 45.dp else calculatedDayWidth
            val periodHeight = 70.dp

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Grid Header (Days)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                            .horizontalScroll(scrollStateHorizontal)
                    ) {
                        // Top-left corner (Month placeholder)
                        Box(
                            modifier = Modifier
                                .width(timeColumnWidth)
                                .align(Alignment.CenterVertically),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = LocalDate.now().month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Day headers
                        daysOfWeek.forEachIndexed { index, day ->
                            val currentLocalDate = LocalDate.now()
                            // Just a simple calculation for display dates relative to the current week
                            // In a real app, calculate actual dates based on semester start date.
                            val dayOffset = (index + 1) - currentLocalDate.dayOfWeek.value
                            val dateToShow = currentLocalDate.plusDays(dayOffset.toLong())
                            val isToday = dayOffset == 0

                            Column(
                                modifier = Modifier.width(dayWidth),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${dateToShow.dayOfMonth}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                    // Scrollable Grid Area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                            .verticalScroll(scrollStateVertical)
                    ) {
                        Row(modifier = Modifier.horizontalScroll(scrollStateHorizontal)) {
                            
                            // Left Column (Period Numbers & Times)
                            Column(
                                modifier = Modifier.width(timeColumnWidth)
                            ) {
                                val periodStartTimes = listOf(
                                    "08:00", "08:50", "09:50", "10:40", 
                                    "13:30", "14:20", "15:20", "16:10", 
                                    "18:30", "19:20", "20:10", "21:00"
                                )
                                for (i in 1..totalPeriods) {
                                    Box(
                                        modifier = Modifier
                                            .height(periodHeight)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$i",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = periodStartTimes.getOrElse(i - 1) { "" },
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // The Grid (7 Days)
                            Box(
                                modifier = Modifier
                                    .width(dayWidth * 7)
                                    .height(periodHeight * totalPeriods)
                            ) {
                                // Draw horizontal grid lines
                                for (i in 1..totalPeriods) {
                                    val yOffset = periodHeight * (i - 1)
                                    Box(
                                        modifier = Modifier
                                            .offset(y = yOffset)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    )
                                    
                                    // Add subtle dividers for morning/afternoon/evening
                                    if (i == 5 || i == 9) {
                                        Box(
                                            modifier = Modifier
                                                .offset(y = yOffset)
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                        )
                                    }
                                }

                                // Draw vertical grid lines
                                for (i in 1..7) {
                                    val xOffset = dayWidth * i
                                    Box(
                                        modifier = Modifier
                                            .offset(x = xOffset)
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    )
                                }

                                // Render Courses Absolutely
                                activeCoursesInWeek.forEach { course ->
                                    val dayIndex = (course.dayOfWeek - 1).coerceIn(0, 6)
                                    // Ensure periods are within valid bounds (1 to totalPeriods)
                                    val startPeriod = course.startPeriod.coerceIn(1, totalPeriods)
                                    val endPeriod = course.endPeriod.coerceIn(startPeriod, totalPeriods)
                                    
                                    val span = endPeriod - startPeriod + 1
                                    
                                    val xOffset = dayWidth * dayIndex
                                    val yOffset = periodHeight * (startPeriod - 1)
                                    val courseHeight = periodHeight * span

                                    Box(
                                        modifier = Modifier
                                            .offset(x = xOffset, y = yOffset)
                                            .width(dayWidth)
                                            .height(courseHeight)
                                            .padding(2.dp) // Gap between courses
                                    ) {
                                        val cColor = Color(course.colorHex)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            cColor.copy(alpha = 0.5f),
                                                            cColor.copy(alpha = 0.2f)
                                                        )
                                                    )
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            cColor.copy(alpha = 0.8f),
                                                            cColor.copy(alpha = 0.3f)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedCourseForDetails = course }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(6.dp)
                                            ) {
                                                Text(
                                                    text = course.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = if (span == 1) 2 else 4,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 14.sp
                                                )
                                                if (span > 1) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Room,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(10.dp),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = course.classroom,
                                                            fontSize = 9.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = "@${course.classroom}",
                                                        fontSize = 9.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
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
    }
}

@Composable
fun CsvImportDialog(
    semesterId: Long,
    onDismiss: () -> Unit,
    onImport: (List<CourseEntity>) -> Unit
) {
    var rawText by remember {
        mutableStateOf(
            """
            Computer Graphics,CS-440,Hall C-102,Dr. Morris,3,3,4,10:00,11:40,ALL,#2563EB
            Mobile App Architecture,CS-462,Lab 105,Prof. Bell,4,1,2,08:00,09:40,ALL,#0D9488
            AI Ethics Seminar,PHIL-205,Auditorium 2,Dr. Miller,5,9,11,18:30,21:00,ODD,#7C3AED
            """.trimIndent()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CSV Schedule Import", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Format: Name, Code, Room, Instructor, Day(1-7), StartPeriod(1-12), EndPeriod, StartTime, EndTime, WeekRule(ALL/ODD/EVEN), ColorHex",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = mutableListOf<CourseEntity>()
                    rawText.lines().forEach { line ->
                        val tokens = line.split(",").map { it.trim() }
                        if (tokens.size >= 9) {
                            try {
                                parsed.add(
                                    CourseEntity(
                                        semesterId = semesterId,
                                        name = tokens[0],
                                        code = tokens[1],
                                        classroom = tokens[2],
                                        instructor = tokens[3],
                                        dayOfWeek = tokens[4].toIntOrNull() ?: 1,
                                        startPeriod = tokens[5].toIntOrNull() ?: 1,
                                        endPeriod = tokens[6].toIntOrNull() ?: 2,
                                        startTime = tokens[7],
                                        endTime = tokens[8],
                                        weekRule = com.example.data.model.WeekRule.valueOf(tokens[9].uppercase()),
                                        colorHex = if (tokens.size > 10) android.graphics.Color.parseColor(tokens[10]).toLong() else 0xFF4F46E5
                                    )
                                )
                            } catch (e: Exception) {
                                // Skip invalid lines
                            }
                        }
                    }
                    onImport(parsed)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

