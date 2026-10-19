package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExamEntity
import com.example.data.model.Task
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TaskTab {
    TASKS,
    EXAMS,
    REMINDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksExamsScreen(viewModel: ScheduleViewModel) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allExams by viewModel.allExams.collectAsStateWithLifecycle()
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(TaskTab.TASKS) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskCourse by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf("MEDIUM") }

    var newExamCourse by remember { mutableStateOf("") }
    var newExamRoom by remember { mutableStateOf("") }
    var newExamSeat by remember { mutableStateOf("") }
    var newExamStartTime by remember { mutableStateOf("09:00") }
    var newExamEndTime by remember { mutableStateOf("11:00") }

    val completedTasksCount = remember(allTasks) { allTasks.count { it.isCompleted } }
    val totalTasksCount = remember(allTasks) { allTasks.size }
    val completionFraction = remember(completedTasksCount, totalTasksCount) {
        if (totalTasksCount == 0) 0f else completedTasksCount.toFloat() / totalTasksCount.toFloat()
    }

    val nearestExam = remember(allExams) {
        val now = System.currentTimeMillis()
        allExams.filter { it.examDateMillis >= now - 86400000L }
            .minByOrNull { it.examDateMillis } ?: allExams.firstOrNull()
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Tasks & Exams".tr, fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        TaskTab.TASKS -> showAddTaskDialog = true
                        TaskTab.EXAMS -> showAddExamDialog = true
                        TaskTab.REMINDERS -> showAddReminderDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        when (selectedTab) {
                            TaskTab.TASKS -> "Add Task".tr
                            TaskTab.EXAMS -> "Add Exam".tr
                            TaskTab.REMINDERS -> "Add Alarm".tr
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. Primary Tab Selector
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == TaskTab.TASKS,
                    onClick = { selectedTab = TaskTab.TASKS },
                    text = { Text("Tasks (${allTasks.count { !it.isCompleted }})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == TaskTab.EXAMS,
                    onClick = { selectedTab = TaskTab.EXAMS },
                    text = { Text("Exams (${allExams.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == TaskTab.REMINDERS,
                    onClick = { selectedTab = TaskTab.REMINDERS },
                    text = { Text("Alarms (${allReminders.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            if (selectedTab == TaskTab.TASKS) {
                // 2. Task Completion Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Homework Completion", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("$completedTasksCount of $totalTasksCount completed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        LinearProgressIndicator(
                            progress = { completionFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }

                // 3. Task List
                if (allTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Text("No pending tasks!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Tap + Add Task to schedule homework or study items.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(allTasks, key = { it.id }) { task ->
                            val priorityColor = when (task.priority) {
                                "HIGH" -> MaterialTheme.colorScheme.error
                                "LOW" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.tertiary
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTask(task) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = task.isCompleted,
                                            onCheckedChange = { viewModel.toggleTask(task) }
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = task.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (task.courseName.isNotBlank()) {
                                                Text(
                                                    text = "Course: ${task.courseName}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = priorityColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = task.priority,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = priorityColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        IconButton(onClick = { viewModel.deleteTask(task) }) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // EXAMS TAB WITH LIVE COUNTDOWN HERO CARD
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Exam Countdown Hero Banner
                    nearestExam?.let { exam ->
                        val now = System.currentTimeMillis()
                        val diffMs = exam.examDateMillis - now
                        val daysLeft = (diffMs / 86400000L).coerceAtLeast(0)

                        val badgeText = when {
                            daysLeft == 0L -> "⚠️ EXAM TODAY"
                            daysLeft <= 3L -> "🔥 $daysLeft DAYS LEFT"
                            else -> "⚡ $daysLeft DAYS REMAINING"
                        }

                        val badgeColor = when {
                            daysLeft == 0L -> Color(0xFFEF4444)
                            daysLeft <= 3L -> Color(0xFFF59E0B)
                            else -> Color(0xFF0EA5E9)
                        }

                        val dateStr = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.ENGLISH)
                            .format(Date(exam.examDateMillis))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Unspecified,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(badgeColor, MaterialTheme.colorScheme.tertiary)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                badgeColor.copy(alpha = 0.25f),
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = badgeColor
                                        ) {
                                            Text(
                                                text = badgeText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Text(
                                            text = "COUNTDOWN",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Text(
                                        text = exam.courseName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.Event, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                                            Text(dateStr, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                                            Text("${exam.startTime} - ${exam.endTime}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    if (exam.classroom.isNotBlank() || exam.seatNumber.isNotBlank()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (exam.classroom.isNotBlank()) {
                                                Text(
                                                    text = "📍 ${exam.classroom}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (exam.seatNumber.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = "🪑 ${exam.seatNumber}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Other Scheduled Exams List
                    val otherExams = remember(allExams, nearestExam) {
                        allExams.filter { nearestExam == null || it.id != nearestExam.id }
                    }

                    if (otherExams.isEmpty() && nearestExam == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                Text("No upcoming exams scheduled", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Tap + Add Exam to record exam dates & locations.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(otherExams, key = { it.id }) { exam ->
                                val now = System.currentTimeMillis()
                                val diffMs = exam.examDateMillis - now
                                val daysLeft = (diffMs / 86400000L).coerceAtLeast(0)

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(14.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = exam.courseName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "$daysLeft Days",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("📍 ${exam.classroom.ifBlank { "TBD" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("⏰ ${exam.startTime} - ${exam.endTime}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                            }

                                            if (exam.seatNumber.isNotBlank()) {
                                                Text("🪑 ${exam.seatNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        IconButton(onClick = { viewModel.deleteExam(exam) }) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTab == TaskTab.REMINDERS) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Exact Alarms & Scheduled Alerts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Triggers exact alarms with notifications even when phone is locked or screen is off.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (allReminders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No active alarms or custom reminders",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Tap + Add Alarm below to set an alert for assignments or study sessions.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allReminders, key = { it.id }) { reminder ->
                            val triggerDate = SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault())
                                .format(Date(reminder.triggerTimeMillis))
                            val isPast = reminder.triggerTimeMillis < System.currentTimeMillis()

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (reminder.isEnabled && !isPast)
                                        MaterialTheme.colorScheme.surface
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (reminder.isEnabled && !isPast) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (reminder.isEnabled && !isPast) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isPast) Icons.Outlined.AlarmOff else Icons.Outlined.Alarm,
                                            contentDescription = null,
                                            tint = if (reminder.isEnabled && !isPast) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            reminder.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (reminder.isEnabled && !isPast) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (reminder.description.isNotBlank()) {
                                            Text(
                                                reminder.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "⏰ $triggerDate",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isPast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                            )
                                            if (reminder.repeatInterval != "NONE") {
                                                Text(
                                                    "🔄 ${reminder.repeatInterval}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }

                                    Switch(
                                        checked = reminder.isEnabled,
                                        onCheckedChange = { viewModel.toggleReminder(reminder) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )

                                    IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
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

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Homework / Task".tr, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        label = { Text("Course Name (Optional)".tr) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Priority:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                            FilterChip(
                                selected = newTaskPriority == p,
                                onClick = { newTaskPriority = p },
                                label = { Text(p, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            viewModel.addTask(newTaskTitle.trim(), newTaskCourse.trim(), newTaskPriority)
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

    if (showAddExamDialog) {
        AlertDialog(
            onDismissRequest = { showAddExamDialog = false },
            title = { Text("Add Exam Schedule".tr, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newExamCourse,
                        onValueChange = { newExamCourse = it },
                        label = { Text("Course Name".tr) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newExamRoom,
                        onValueChange = { newExamRoom = it },
                        label = { Text("Classroom Location".tr) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newExamSeat,
                        onValueChange = { newExamSeat = it },
                        label = { Text("Seat Number (Optional)".tr) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newExamStartTime,
                            onValueChange = { newExamStartTime = it },
                            label = { Text("Start Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newExamEndTime,
                            onValueChange = { newExamEndTime = it },
                            label = { Text("End Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newExamCourse.isNotBlank()) {
                            viewModel.addExam(
                                courseName = newExamCourse.trim(),
                                classroom = newExamRoom.trim(),
                                dateMillis = System.currentTimeMillis() + 10 * 86400000L,
                                sTime = newExamStartTime.trim(),
                                eTime = newExamEndTime.trim(),
                                seatNumber = newExamSeat.trim()
                            )
                            newExamCourse = ""
                            newExamRoom = ""
                            newExamSeat = ""
                            showAddExamDialog = false
                        }
                    }
                ) {
                    Text("Save".tr)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExamDialog = false }) {
                    Text("Cancel".tr)
                }
            }
        )
    }

    if (showAddReminderDialog) {
        var reminderTitle by remember { mutableStateOf("") }
        var reminderDesc by remember { mutableStateOf("") }
        var reminderOffsetDays by remember { mutableIntStateOf(0) }
        var reminderHour by remember { mutableIntStateOf(9) }
        var reminderMin by remember { mutableIntStateOf(0) }
        var reminderRepeat by remember { mutableStateOf("NONE") }

        AlertDialog(
            onDismissRequest = { showAddReminderDialog = false },
            title = { Text("Add Scheduled Alarm / Reminder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        label = { Text("Reminder Title") },
                        placeholder = { Text("e.g. Submit CS201 Lab Report") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reminderDesc,
                        onValueChange = { reminderDesc = it },
                        label = { Text("Notes / Classroom (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("When to alert:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "Today", 1 to "Tomorrow", 2 to "+2 Days", 7 to "+1 Week").forEach { (offset, label) ->
                            FilterChip(
                                selected = reminderOffsetDays == offset,
                                onClick = { reminderOffsetDays = offset },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Alarm Time:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(8 to 0, 9 to 0, 12 to 0, 14 to 30, 18 to 0, 21 to 0).forEach { (h, m) ->
                            val timeStr = String.format("%02d:%02d", h, m)
                            FilterChip(
                                selected = reminderHour == h && reminderMin == m,
                                onClick = {
                                    reminderHour = h
                                    reminderMin = m
                                },
                                label = { Text(timeStr, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Repeat Interval:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NONE" to "Once", "DAILY" to "Daily", "WEEKLY" to "Weekly").forEach { (rep, label) ->
                            FilterChip(
                                selected = reminderRepeat == rep,
                                onClick = { reminderRepeat = rep },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reminderTitle.isNotBlank()) {
                            val cal = java.util.Calendar.getInstance().apply {
                                add(java.util.Calendar.DAY_OF_YEAR, reminderOffsetDays)
                                set(java.util.Calendar.HOUR_OF_DAY, reminderHour)
                                set(java.util.Calendar.MINUTE, reminderMin)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                                if (reminderOffsetDays == 0 && timeInMillis <= System.currentTimeMillis()) {
                                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            viewModel.insertReminder(
                                title = reminderTitle.trim(),
                                description = reminderDesc.trim(),
                                triggerTimeMillis = cal.timeInMillis,
                                repeatInterval = reminderRepeat
                            )
                            showAddReminderDialog = false
                        }
                    }
                ) {
                    Text("Save Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
