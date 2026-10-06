package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Course
import com.example.domain.HyperIslandManager
import com.example.domain.HyperIslandState
import com.example.domain.HyperIslandStatus
import com.example.domain.CalculatorMemoryManager
import com.example.ui.components.AcademicCalendarDialog
import com.example.ui.components.NoteTakingCanvasDialog
import com.example.ui.components.OfflineCalculatorDialog
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class BentoWidgetId(val title: String) {
    HYPER_ISLAND("HyperIsland Live Status"),
    NEXT_CLASS("Next Class Hero"),
    TODAY_TIMELINE("Today's Schedule"),
    TASKS_PREVIEW("Tasks & Exams"),
    SEMESTER_PROGRESS("Semester Progress"),
    FOCUS_TIMER("Study Focus"),
    QUICK_ACTIONS("Quick Actions")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ScheduleViewModel,
    onNavigateToTimetable: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onOpenAcademicCalendar: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val timings by viewModel.userPreferencesManager.sectionTimings.collectAsStateWithLifecycle()

    val widgetOrderList by viewModel.userPreferencesManager.dashboardWidgetOrder.collectAsStateWithLifecycle()
    val hiddenWidgetsSet by viewModel.userPreferencesManager.dashboardHiddenWidgets.collectAsStateWithLifecycle()

    var isEditMode by remember { mutableStateOf(false) }
    var isIslandExpanded by remember { mutableStateOf(false) }

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showNoteCanvasDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showFocusModal by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showCalendarModal by remember { mutableStateOf(false) }

    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskCourse by remember { mutableStateOf("") }

    val actualDate = remember { LocalDate.now() }
    val todayDayOfWeek = remember { actualDate.dayOfWeek.value } // 1 = Mon, 7 = Sun
    val formattedToday = remember { actualDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) }

    val todayCourses = remember(allCourses, todayDayOfWeek) {
        allCourses.filter { it.dayOfWeek == todayDayOfWeek }.sortedBy { it.startPeriod }
    }

    val pendingTasks = remember(allTasks) { allTasks.filter { !it.isCompleted } }

    // Live HyperIsland Calculation
    val hyperStatus = remember(todayCourses, timings) {
        HyperIslandManager.calculateStatus(todayCourses, timings, LocalTime.now())
    }

    // Start Foreground Service with RemoteViews for live countdowns
    LaunchedEffect(Unit) {
        try {
            com.example.service.HyperIslandLiveService.startService(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sync notification with live status
    LaunchedEffect(hyperStatus) {
        HyperIslandManager.updateLiveNotification(context, hyperStatus)
    }

    val activeWidgets = remember(widgetOrderList, hiddenWidgetsSet) {
        widgetOrderList
            .mapNotNull { idStr ->
                try { BentoWidgetId.valueOf(idStr) } catch (e: Exception) { null }
            }
            .filter { !hiddenWidgetsSet.contains(it.name) }
    }

    fun moveWidget(index: Int, up: Boolean) {
        val current = widgetOrderList.toMutableList()
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in 0 until current.size) {
            val item = current.removeAt(index)
            current.add(targetIndex, item)
            viewModel.userPreferencesManager.setDashboardWidgetOrder(current)
        }
    }

    fun hideWidget(id: BentoWidgetId) {
        val currentHidden = hiddenWidgetsSet.toMutableSet()
        currentHidden.add(id.name)
        viewModel.userPreferencesManager.setDashboardHiddenWidgets(currentHidden)
    }

    fun restoreWidget(id: BentoWidgetId) {
        val currentHidden = hiddenWidgetsSet.toMutableSet()
        currentHidden.remove(id.name)
        viewModel.userPreferencesManager.setDashboardHiddenWidgets(currentHidden)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            val isTablet = maxWidth >= 600.dp

            if (isTablet) {
                // ==========================================
                // TABLET SMART ZONES:
                // Left Zone (55%): The Today's Timeline
                // Right Zone (45%): Control Center & Bento Widgets
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Zone: Daily Timeline Visualizer
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Today's Schedule Timeline",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formattedToday,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = onNavigateToTimetable) {
                                    Text("Full Week Grid →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (todayCourses.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.CheckCircleOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "No classes scheduled today!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Take time to study, work on assignments, or relax.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(onClick = { showAddCourseDialog = true }) {
                                            Text("Add a Class")
                                        }
                                    }
                                }
                            } else {
                                todayCourses.forEach { course ->
                                    TodayTimelineCourseCard(
                                        course = course,
                                        onTakeNote = { showNoteCanvasDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    // Right Zone: Control Center & Bento Grid
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
                            // Top HyperIsland status pill
                            HyperIslandPill(
                                status = hyperStatus,
                                isExpanded = isIslandExpanded,
                                onToggleExpand = { isIslandExpanded = !isIslandExpanded },
                                onTakeNote = { showNoteCanvasDialog = true },
                                onLaunchFocus = { showFocusModal = true },
                                onOpenCalculator = { showCalculatorDialog = true }
                            )

                            // Quick Action Launcher Tiles
                            BentoQuickActionsWidget(
                                onAddClass = { showAddCourseDialog = true },
                                onTakeNote = { showNoteCanvasDialog = true },
                                onAddTask = { showAddTaskDialog = true },
                                onOpenCalendar = { showCalendarModal = true },
                                onOpenCalculator = { showCalculatorDialog = true }
                            )

                            // Next Class Card
                            BentoNextClassWidget(
                                status = hyperStatus,
                                onTakeNote = { showNoteCanvasDialog = true }
                            )

                            // Tasks & Exams
                            BentoTasksWidget(
                                pendingTasks = pendingTasks,
                                onAddTask = { showAddTaskDialog = true },
                                onNavigateToTasks = onNavigateToTasks
                            )

                            // Semester Progress
                            BentoSemesterProgressWidget(
                                onOpenCalendar = { showCalendarModal = true }
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // PHONE BENTO BOX DASHBOARD (REORDERABLE GRID)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Edit Mode Header / Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dashboard".tr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isEditMode) {
                                TextButton(
                                    onClick = {
                                        viewModel.userPreferencesManager.resetDashboardWidgets()
                                    }
                                ) {
                                    Text("Reset Layout", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { isEditMode = false },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                IconButton(onClick = { isEditMode = true }) {
                                    Icon(
                                        Icons.Outlined.DashboardCustomize,
                                        contentDescription = "Customize Dashboard",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Render Reorderable Bento Widgets
                    activeWidgets.forEachIndexed { index, widgetId ->
                        BentoWidgetContainer(
                            widgetId = widgetId,
                            isEditMode = isEditMode,
                            index = index,
                            totalCount = activeWidgets.size,
                            onMoveUp = { moveWidget(index, up = true) },
                            onMoveDown = { moveWidget(index, up = false) },
                            onHide = { hideWidget(widgetId) }
                        ) {
                            when (widgetId) {
                                BentoWidgetId.HYPER_ISLAND -> {
                                    HyperIslandPill(
                                        status = hyperStatus,
                                        isExpanded = isIslandExpanded,
                                        onToggleExpand = { isIslandExpanded = !isIslandExpanded },
                                        onTakeNote = { showNoteCanvasDialog = true },
                                        onLaunchFocus = { showFocusModal = true },
                                        onOpenCalculator = { showCalculatorDialog = true }
                                    )
                                }
                                BentoWidgetId.NEXT_CLASS -> {
                                    BentoNextClassWidget(
                                        status = hyperStatus,
                                        onTakeNote = { showNoteCanvasDialog = true }
                                    )
                                }
                                BentoWidgetId.TODAY_TIMELINE -> {
                                    BentoTodayScheduleWidget(
                                        todayCourses = todayCourses,
                                        formattedToday = formattedToday,
                                        onNavigateToTimetable = onNavigateToTimetable,
                                        onAddCourse = { showAddCourseDialog = true },
                                        onTakeNote = { showNoteCanvasDialog = true }
                                    )
                                }
                                BentoWidgetId.TASKS_PREVIEW -> {
                                    BentoTasksWidget(
                                        pendingTasks = pendingTasks,
                                        onAddTask = { showAddTaskDialog = true },
                                        onNavigateToTasks = onNavigateToTasks
                                    )
                                }
                                BentoWidgetId.SEMESTER_PROGRESS -> {
                                    BentoSemesterProgressWidget(
                                        onOpenCalendar = { showCalendarModal = true }
                                    )
                                }
                                BentoWidgetId.FOCUS_TIMER -> {
                                    BentoFocusTimerWidget(
                                        onLaunchFocus = { showFocusModal = true }
                                    )
                                }
                                BentoWidgetId.QUICK_ACTIONS -> {
                                    BentoQuickActionsWidget(
                                        onAddClass = { showAddCourseDialog = true },
                                        onTakeNote = { showNoteCanvasDialog = true },
                                        onAddTask = { showAddTaskDialog = true },
                                        onOpenCalendar = { showCalendarModal = true },
                                        onOpenCalculator = { showCalculatorDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    // If in Edit Mode and widgets are hidden, show restore list
                    if (isEditMode && hiddenWidgetsSet.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Hidden Widgets (Tap + to restore):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                hiddenWidgetsSet.forEach { hiddenName ->
                                    val hiddenId = try { BentoWidgetId.valueOf(hiddenName) } catch (e: Exception) { null }
                                    if (hiddenId != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(hiddenId.title, fontSize = 13.sp)
                                            IconButton(onClick = { restoreWidget(hiddenId) }) {
                                                Icon(Icons.Default.Add, contentDescription = "Restore")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Dialogs
    if (showAddCourseDialog) {
        AddEditCourseDialog(
            initialCourse = null,
            existingCourses = allCourses,
            defaultSemester = "Fall 2026",
            initialDay = todayDayOfWeek,
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

    if (showNoteCanvasDialog) {
        NoteTakingCanvasDialog(
            viewModel = viewModel,
            onDismiss = { showNoteCanvasDialog = false }
        )
    }

    if (showCalculatorDialog) {
        OfflineCalculatorDialog(
            viewModel = viewModel,
            onDismiss = { showCalculatorDialog = false }
        )
    }

    if (showCalendarModal) {
        AcademicCalendarDialog(
            viewModel = viewModel,
            onDismiss = { showCalendarModal = false }
        )
    }

    if (showFocusModal) {
        com.example.ui.components.StudyFocusDialog(
            viewModel = viewModel,
            onDismiss = { showFocusModal = false }
        )
    }

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Task".tr, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Title".tr) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTaskCourse,
                        onValueChange = { newTaskCourse = it },
                        label = { Text("Course Name".tr) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.addTask(newTaskTitle.trim(), newTaskCourse.trim())
                            newTaskTitle = ""
                            newTaskCourse = ""
                            showAddTaskDialog = false
                        }
                    }
                ) {
                    Text("Save".tr)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel".tr)
                }
            }
        )
    }
}

/**
 * Edit Mode Wrapper for Bento Widgets
 */
@Composable
private fun BentoWidgetContainer(
    widgetId: BentoWidgetId,
    isEditMode: Boolean,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onHide: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEditMode) {
                    Modifier
                        .scale(1.01f)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(2.dp)
                } else Modifier
            )
    ) {
        content()

        if (isEditMode) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (index > 0) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < totalCount - 1) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onHide, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Hide Widget", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * HyperOS HyperIsland Component
 * Three Glanceable States:
 * 1. Idle (No Class Soon): Subtle pill with next class time or free day
 * 2. Active (In Class): Countdown pill "Math • 45m left • Room 302"
 * 3. Urgent (Class Starting < 15m): Pulse alert pill "Starts in 10m • Room 302"
 */
@Composable
fun HyperIslandPill(
    status: HyperIslandStatus,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTakeNote: () -> Unit,
    onLaunchFocus: () -> Unit,
    onOpenCalculator: () -> Unit = {}
) {
    val pillBgColor = when (status.state) {
        HyperIslandState.ACTIVE -> Color(0xFF064E3B) // Dark Emerald
        HyperIslandState.URGENT -> Color(0xFF78350F) // Dark Amber Alert
        HyperIslandState.IDLE -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusDotColor = when (status.state) {
        HyperIslandState.ACTIVE -> Color(0xFF10B981)
        HyperIslandState.URGENT -> Color(0xFFF59E0B)
        HyperIslandState.IDLE -> MaterialTheme.colorScheme.primary
    }

    val calcTicker by CalculatorMemoryManager.lastResultTicker.collectAsStateWithLifecycle()

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = pillBgColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pill Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Text(
                        text = status.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = status.timeBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (status.subtitle.isNotBlank()) {
                Text(
                    text = status.subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // HyperIsland Mini-Calc Ticker
            if (!calcTicker.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCalculator() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🧮", fontSize = 12.sp)
                            Text(
                                text = "Ticker: $calcTicker",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text("Tap to Open", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            // Progress bar if in class
            if (status.state == HyperIslandState.ACTIVE && status.progress > 0f) {
                LinearProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF10B981),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }

            // Expanded Floating Drawer with Quick Actions
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTakeNote,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Take Note", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onOpenCalculator,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Calc", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onLaunchFocus,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Focus (25m)", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bento Widget: Next Class Hero Card
 */
@Composable
private fun BentoNextClassWidget(
    status: HyperIslandStatus,
    onTakeNote: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEXT UPCOMING LECTURE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                if (status.room.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "📍 ${status.room}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = if (status.courseName.isNotBlank()) status.courseName else "No Classes Pending",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (status.course != null) {
                    TextButton(onClick = onTakeNote) {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Class Memo", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Bento Widget: Today's Schedule Timeline
 */
@Composable
private fun BentoTodayScheduleWidget(
    todayCourses: List<Course>,
    formattedToday: String,
    onNavigateToTimetable: () -> Unit,
    onAddCourse: () -> Unit,
    onTakeNote: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedToday,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onNavigateToTimetable) {
                    Text("Week Grid →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (todayCourses.isEmpty()) {
                Text(
                    text = "No classes for today. Tap to add a course.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onAddCourse, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Add Class")
                }
            } else {
                todayCourses.forEach { course ->
                    TodayTimelineCourseCard(course = course, onTakeNote = onTakeNote)
                }
            }
        }
    }
}

@Composable
fun TodayTimelineCourseCard(
    course: Course,
    onTakeNote: () -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(course.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = course.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = "${course.startTime} - ${course.endTime} • Periods ${course.startPeriod}-${course.endPeriod}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                if (course.classroom.isNotBlank()) {
                    Text(
                        text = "📍 ${course.classroom}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }

            IconButton(onClick = onTakeNote) {
                Icon(Icons.Outlined.EditNote, contentDescription = "Take Note", tint = Color.White)
            }
        }
    }
}

/**
 * Bento Widget: Tasks & Exams
 */
@Composable
private fun BentoTasksWidget(
    pendingTasks: List<com.example.data.model.TaskEntity>,
    onAddTask: () -> Unit,
    onNavigateToTasks: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tasks & Assignments",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${pendingTasks.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                TextButton(onClick = onAddTask) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp)
                }
            }

            if (pendingTasks.isEmpty()) {
                Text(
                    text = "All homework & assignments completed! 🎉",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                pendingTasks.take(3).forEach { task ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = task.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bento Widget: Semester Progress
 */
@Composable
private fun BentoSemesterProgressWidget(
    onOpenCalendar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fall 2026 Semester",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Week 5 / 20",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { 0.25f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("25% Term Completed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                TextButton(onClick = onOpenCalendar) {
                    Text("Calendar →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Bento Widget: Study Focus Timer
 */
@Composable
private fun BentoFocusTimerWidget(
    onLaunchFocus: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text("Pomodoro Study Focus", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("25 min focus interval", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = onLaunchFocus,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Start Focus", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Bento Widget: Quick Action Launcher Tiles
 */
@Composable
private fun BentoQuickActionsWidget(
    onAddClass: () -> Unit,
    onTakeNote: () -> Unit,
    onAddTask: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCalculator: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        QuickActionChip(
            icon = Icons.Default.Add,
            label = "Add Class",
            color = Color(0xFF2563EB),
            onClick = onAddClass,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            icon = Icons.Outlined.EditNote,
            label = "Notes",
            color = Color(0xFF10B981),
            onClick = onTakeNote,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            icon = Icons.Outlined.Calculate,
            label = "Calc",
            color = Color(0xFF3B82F6),
            onClick = onOpenCalculator,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            icon = Icons.Outlined.CheckCircle,
            label = "Tasks",
            color = Color(0xFFF59E0B),
            onClick = onAddTask,
            modifier = Modifier.weight(1f)
        )
        QuickActionChip(
            icon = Icons.Outlined.CalendarMonth,
            label = "Calendar",
            color = Color(0xFF8B5CF6),
            onClick = onOpenCalendar,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
            .height(54.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
