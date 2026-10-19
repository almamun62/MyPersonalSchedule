package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.UserPreferencesManager
import com.example.domain.model.Course
import com.example.ui.components.CompactRubyScheduleText
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.LocalDate

/**
 * Chaoxing-style weekly timetable grid matching Screenshots 3 & 4.
 * Features:
 * - Week selector dropdown (Week 8 ∨)
 * - Hamburger quick-sheet menu
 * - Weekday date pills (Monday 10-19 highlighted)
 * - 12 Session ruler with period times
 * - Continuous multi-session course cards with ruby Pinyin annotations
 * - Interactive course inspection & slot tap-to-add
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChaoxingScheduleScreen(
    viewModel: ScheduleViewModel,
    onNavigateToDashboard: (() -> Unit)? = null,
    onNavigateToTasks: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val isPinyinEnabled by viewModel.userPreferencesManager.isPinyinEnabled.collectAsStateWithLifecycle()
    var currentWeek by remember { mutableIntStateOf(8) }

    // Dialog & sheet states
    var showWeekSelectorSheet by remember { mutableStateOf(false) }
    var showQuickMenuDrawer by remember { mutableStateOf(false) }
    var viewingCourseDetails by remember { mutableStateOf<Course?>(null) }
    var editingCourseTarget by remember { mutableStateOf<Course?>(null) }
    var addingSlotTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (dayOfWeek, session)

    // Calculate dates for current week
    val today = remember { LocalDate.now() }
    val baseMonday = remember(today) {
        today.minusDays((today.dayOfWeek.value - 1).toLong())
    }

    val weekDays = remember(baseMonday, currentWeek) {
        val weekOffset = (currentWeek - 8) * 7L
        (0..6).map { offset ->
            val d = baseMonday.plusDays(offset + weekOffset)
            val dayName = when (offset + 1) {
                1 -> "Monday"
                2 -> "Tuesday"
                3 -> "Wednesday"
                4 -> "Thursday"
                5 -> "Friday"
                6 -> "Saturday"
                7 -> "Sunday"
                else -> ""
            }
            val dateStr = String.format("%02d-%02d", d.monthValue, d.dayOfMonth)
            Triple(offset + 1, dayName, dateStr)
        }
    }

    val sessions = remember { UserPreferencesManager.defaultSectionTimings }
    val sessionHeight = 78.dp

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { showQuickMenuDrawer = true }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showWeekSelectorSheet = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Week $currentWeek",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Week",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { addingSlotTarget = Pair(1, 1) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Course",
                            tint = Color(0xFF0284C7)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Weekday Header Row (Screenshot 3 & 4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Empty corner box for time ruler alignment
                Box(modifier = Modifier.width(46.dp))

                weekDays.forEach { (dayNum, dayName, dateStr) ->
                    val isToday = dayNum == today.dayOfWeek.value && currentWeek == 8

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isToday) {
                            // Active Day Highlight Pill (Screenshot 3: Monday 10-19 in soft blue pill)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE0F2FE))
                                    .border(1.dp, Color(0xFF7DD3FC), RoundedCornerShape(8.dp))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = dateStr,
                                        fontSize = 10.sp,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = dayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                            Text(
                                text = dateStr,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Main Timetable Body (Vertical Scroll)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left Time Ruler Column
                    Column(modifier = Modifier.width(46.dp)) {
                        sessions.forEach { s ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(sessionHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${s.section}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = s.startTime,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = s.endTime,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    // 7-Day Course Columns Grid
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(sessionHeight * 12)
                    ) {
                        (1..7).forEach { dayNum ->
                            val dayCourses = remember(allCourses, dayNum) {
                                allCourses.filter { it.dayOfWeek == dayNum }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(
                                        width = 0.5.dp,
                                        color = Color(0xFFF1F5F9)
                                    )
                            ) {
                                // Background session slot dividers & tap to add
                                Column(modifier = Modifier.fillMaxSize()) {
                                    (1..12).forEach { sessionNum ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(sessionHeight)
                                                .border(
                                                    width = 0.5.dp,
                                                    color = Color(0xFFF8FAFC)
                                                )
                                                .clickable {
                                                    addingSlotTarget = Pair(dayNum, sessionNum)
                                                }
                                        )
                                    }
                                }

                                // Course Blocks Spanning Exact Sessions
                                dayCourses.forEach { course ->
                                    val startIdx = (course.startPeriod - 1).coerceAtLeast(0)
                                    val spanCount = (course.endPeriod - course.startPeriod + 1).coerceAtLeast(1)
                                    val topOffset = sessionHeight * startIdx
                                    val cardHeight = (sessionHeight * spanCount) - 3.dp

                                    val cardColor = remember(course.colorHex) {
                                        try {
                                            val hex = course.colorHex.removePrefix("#")
                                            Color(android.graphics.Color.parseColor("#$hex"))
                                        } catch (e: Exception) {
                                            Color(0xFF60A5FA)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset(y = topOffset)
                                            .height(cardHeight)
                                            .padding(horizontal = 1.dp, vertical = 1.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(cardColor)
                                            .clickable { viewingCourseDetails = course }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Top,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Course Name with Compact Ruby Pinyin
                                            CompactRubyScheduleText(
                                                text = course.name,
                                                showPinyin = isPinyinEnabled,
                                                textColor = Color.White
                                            )

                                            if (course.classroom.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                CompactRubyScheduleText(
                                                    text = "@${course.classroom}",
                                                    showPinyin = isPinyinEnabled,
                                                    textColor = Color.White.copy(alpha = 0.9f)
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

    // Week Selector Modal Sheet
    if (showWeekSelectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWeekSelectorSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Semester Week",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 4
                ) {
                    (1..16).forEach { week ->
                        val isSelected = week == currentWeek
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF0284C7)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    currentWeek = week
                                    showWeekSelectorSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Week $week",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Quick Navigation Sheet (Hamburger Menu ≡)
    if (showQuickMenuDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showQuickMenuDrawer = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chaoxing Timetable Menu", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    FilterChip(
                        selected = isPinyinEnabled,
                        onClick = { viewModel.userPreferencesManager.setPinyinEnabled(!isPinyinEnabled) },
                        label = { Text(if (isPinyinEnabled) "Pinyin ON" else "Pinyin OFF", fontSize = 11.sp) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Navigation Items
                onNavigateToDashboard?.let { navDash ->
                    ChaoxingMenuItem(
                        icon = Icons.Outlined.Dashboard,
                        title = "Dashboard & Widgets Overview",
                        subtitle = "Daily timeline, next class badge, study timer",
                        onClick = {
                            showQuickMenuDrawer = false
                            navDash()
                        }
                    )
                }

                onNavigateToTasks?.let { navTasks ->
                    ChaoxingMenuItem(
                        icon = Icons.Outlined.Assignment,
                        title = "Homework, Exams & Reminders",
                        subtitle = "Assignment countdowns, exam room assignments, alarms",
                        onClick = {
                            showQuickMenuDrawer = false
                            navTasks()
                        }
                    )
                }

                onNavigateToSettings?.let { navSettings ->
                    ChaoxingMenuItem(
                        icon = Icons.Outlined.Settings,
                        title = "App Settings & Offline Backup",
                        subtitle = "Themes, SHA-256 backup, bell timetable",
                        onClick = {
                            showQuickMenuDrawer = false
                            navSettings()
                        }
                    )
                }

                ChaoxingMenuItem(
                    icon = Icons.Outlined.Restore,
                    title = "Reload Preset Chaoxing Timetable",
                    subtitle = "Reset to CS courses shown in screenshots",
                    onClick = {
                        showQuickMenuDrawer = false
                        viewModel.loadComputerSciencePreset()
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Course Details Dialog (Screenshot 2)
    viewingCourseDetails?.let { course ->
        val dateForDay = weekDays.find { it.first == course.dayOfWeek }?.third ?: "10-19"
        ChaoxingCourseDetailsDialog(
            course = course,
            dateString = dateForDay,
            showPinyin = isPinyinEnabled,
            onDismiss = { viewingCourseDetails = null },
            onEdit = {
                viewingCourseDetails = null
                editingCourseTarget = course
            }
        )
    }

    // Add / Edit Schedule Dialog (Screenshot 1)
    if (editingCourseTarget != null || addingSlotTarget != null) {
        val courseToEdit = editingCourseTarget
        val slot = addingSlotTarget

        ChaoxingAddScheduleDialog(
            initialCourse = courseToEdit,
            defaultDayOfWeek = slot?.first ?: 1,
            defaultStartPeriod = slot?.second ?: 1,
            defaultWeek = currentWeek,
            viewModel = viewModel,
            onDismiss = {
                editingCourseTarget = null
                addingSlotTarget = null
            },
            onSaved = {
                editingCourseTarget = null
                addingSlotTarget = null
            }
        )
    }
}

@Composable
fun ChaoxingMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE0F2FE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
