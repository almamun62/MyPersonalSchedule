package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.data.model.HolidayOverrideType
import com.example.data.model.TaskEntity
import com.example.domain.CourseClassStatus
import com.example.domain.CourseWithStatus
import com.example.ui.components.AddTaskDialog
import com.example.ui.viewmodel.ScheduleUiState
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    onNavigateToTimetable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskDialogInitialCourse by remember { mutableStateOf("") }

    if (showAddTaskDialog) {
        AddTaskDialog(
            initialCourseName = taskDialogInitialCourse,
            courses = state.courses,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, course, priority ->
                viewModel.addTask(title, course, priority)
                showAddTaskDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // 1. HEADER: Semester Progress Bar & Upcoming Holiday Banner
        item {
            SemesterProgressCard(
                semesterName = state.activeSemester?.name ?: "Academic Term",
                currentWeek = state.currentAcademicWeek,
                totalWeeks = state.activeSemester?.totalWeeks ?: 16,
                progressPercent = state.semesterProgressPercent,
                upcomingHoliday = state.upcomingHoliday
            )
        }

        // 2. DND & LIVE AUTOMATION STATUS BAR
        item {
            DndAutomationBar(
                isDndPermissionGranted = state.isDndPermissionGranted,
                isDndActive = state.isDndActive,
                isAutoDndEnabled = state.isAutoDndEnabled,
                onRequestPermission = { viewModel.requestDndPermission(context) },
                onToggleAuto = { viewModel.toggleAutoDnd(it) },
                onTestSticky = { viewModel.testStickyNotification(context) },
                onTestNag = { viewModel.testNagNotification(context) }
            )
        }

        // 3. PENDING TASKS CHIP LIST
        item {
            PendingTasksSection(
                pendingTasks = state.pendingTasks,
                onCompleteTask = { id -> viewModel.toggleTaskCompleted(id, true) },
                onAddNewTask = {
                    taskDialogInitialCourse = ""
                    showAddTaskDialog = true
                }
            )
        }

        // 4. 'TODAY' TIMELINE with countdown needle & status styling
        item {
            TodayTimelineSection(
                resolution = state.todayResolution,
                coursesWithStatus = state.todayCoursesWithStatus,
                onAddCourseTask = { courseName ->
                    taskDialogInitialCourse = courseName
                    showAddTaskDialog = true
                },
                onViewFullTimetable = onNavigateToTimetable
            )
        }

        // 5. 'TOMORROW' CARD: Preview or Holiday alert
        item {
            TomorrowPreviewCard(
                resolution = state.tomorrowResolution,
                onViewSchedule = onNavigateToTimetable
            )
        }
    }
}

@Composable
private fun SemesterProgressCard(
    semesterName: String,
    currentWeek: Int,
    totalWeeks: Int,
    progressPercent: Float,
    upcomingHoliday: com.example.data.model.HolidayOverrideEntity?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("semester_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = semesterName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Academic Week $currentWeek of $totalWeeks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "${(progressPercent * 100).toInt()}% Done",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Animated progress indicator
            val animatedProgress by animateFloatAsState(
                targetValue = progressPercent,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "semesterProgress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )

            // Upcoming Holiday Banner
            if (upcomingHoliday != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (upcomingHoliday.type == HolidayOverrideType.HOLIDAY) Icons.Default.BeachAccess else Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${upcomingHoliday.name} (${upcomingHoliday.dateString})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DndAutomationBar(
    isDndPermissionGranted: Boolean,
    isDndActive: Boolean,
    isAutoDndEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onToggleAuto: (Boolean) -> Unit,
    onTestSticky: () -> Unit,
    onTestNag: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dnd_automation_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDndActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Icon(
                        imageVector = if (isDndActive) Icons.Default.DoNotDisturbOn else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (isDndActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = if (isDndActive) "DND Active (Priority Mode)" else "Class Automation Engine",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDndPermissionGranted) "Mutes notifications during lectures" else "Permission required for policy access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isDndPermissionGranted) {
                    FilledTonalButton(
                        onClick = onRequestPermission,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("grant_dnd_button")
                    ) {
                        Text("Grant Access", fontSize = 12.sp)
                    }
                } else {
                    Switch(
                        checked = isAutoDndEnabled,
                        onCheckedChange = onToggleAuto,
                        modifier = Modifier.testTag("auto_dnd_switch")
                    )
                }
            }

            // Test native sticky & 5-minute nag notification buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestSticky,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Sticky Alert", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onTestNag,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("5-Min Nag Loop", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PendingTasksSection(
    pendingTasks: List<TaskEntity>,
    onCompleteTask: (Long) -> Unit,
    onAddNewTask: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Pending Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (pendingTasks.isNotEmpty()) {
                    Badge { Text("${pendingTasks.size}") }
                }
            }

            TextButton(
                onClick = onAddNewTask,
                modifier = Modifier.testTag("add_task_header_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Task")
            }
        }

        if (pendingTasks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("All tasks completed! Ready for upcoming lectures.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(pendingTasks) { task ->
                    TaskChip(task = task, onComplete = { onCompleteTask(task.id) })
                }
            }
        }
    }
}

@Composable
private fun TaskChip(
    task: TaskEntity,
    onComplete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .widthIn(min = 160.dp, max = 240.dp)
            .clickable { onComplete() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { onComplete() },
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.courseName.isNotEmpty()) {
                    Text(
                        text = task.courseName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayTimelineSection(
    resolution: com.example.domain.DayResolutionResult?,
    coursesWithStatus: List<CourseWithStatus>,
    onAddCourseTask: (String) -> Unit,
    onViewFullTimetable: () -> Unit
) {
    val todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Today's Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = todayFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onViewFullTimetable) {
                Text("Full Week Grid")
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        // Holiday auto-pause notice if today is holiday
        if (resolution?.isHoliday == true) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Celebration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = resolution.holidayName ?: "Official Holiday",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "All classes auto-paused per academic calendar.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else if (resolution?.isMakeUpDay == true) {
            // Weekend Make-up Day notice
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        text = resolution.makeUpDescription ?: "Weekend Make-up Day (调休)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        if (coursesWithStatus.isEmpty() && resolution?.isHoliday != true) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("No scheduled classes today! Perfect time for self-study or lab prep.")
                }
            }
        } else {
            coursesWithStatus.forEach { item ->
                TodayCourseTimelineItem(
                    item = item,
                    onAddPostClassTask = { onAddCourseTask(item.course.name) }
                )
            }
        }
    }
}

@Composable
private fun TodayCourseTimelineItem(
    item: CourseWithStatus,
    onAddPostClassTask: () -> Unit
) {
    val isCompleted = item.status == CourseClassStatus.COMPLETED
    val isActive = item.status == CourseClassStatus.ACTIVE
    val isNextUp = item.status == CourseClassStatus.NEXT_UP

    val cardBg = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        isNextUp -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }

    val contentAlpha = if (isCompleted) 0.45f else 1f
    val courseColor = Color(item.course.colorHex)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = if (isActive || isNextUp) {
            androidx.compose.foundation.BorderStroke(1.5.dp, if (isActive) MaterialTheme.colorScheme.primary else courseColor)
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_item_${item.course.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) Color.Gray else courseColor)
                    )
                    Text(
                        text = "${item.course.startTime} - ${item.course.endTime}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                }

                // Live needle or badge
                when {
                    isActive -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = "LIVE: ${item.minutesRemaining}m left",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    isNextUp -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "Next Up: in ${item.minutesUntilStart}m",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    isCompleted -> {
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    else -> {
                        Text(
                            text = "In ${item.minutesUntilStart / 60}h ${item.minutesUntilStart % 60}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Room: ${item.course.classroom}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                        )
                        if (item.course.instructor.isNotEmpty()) {
                            Text(
                                text = "• ${item.course.instructor}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                            )
                        }
                    }
                }

                // Quick post-class task button
                IconButton(
                    onClick = onAddPostClassTask,
                    modifier = Modifier.testTag("post_class_task_${item.course.id}")
                ) {
                    Icon(
                        Icons.Default.PostAdd,
                        contentDescription = "Add Homework Task",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                    )
                }
            }
        }
    }
}

@Composable
private fun TomorrowPreviewCard(
    resolution: com.example.domain.DayResolutionResult?,
    onViewSchedule: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
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
                Text(
                    text = "Tomorrow's Preview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewSchedule) {
                    Text("Details", fontSize = 12.sp)
                }
            }

            if (resolution?.isHoliday == true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "Holiday tomorrow: ${resolution.holidayName}. Classes are cancelled!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val count = resolution?.activeCourses?.size ?: 0
                if (count == 0) {
                    Text("No scheduled classes tomorrow. Enjoy your free time!", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        text = "$count course${if (count > 1) "s" else ""} scheduled tomorrow.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    resolution?.activeCourses?.take(3)?.forEach { c ->
                        Text(
                            text = "• ${c.startTime} ${c.name} (${c.classroom})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
