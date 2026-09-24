package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CourseEntity
import com.example.data.model.TaskEntity
import com.example.domain.WeeklySummaryEngine
import com.example.ui.components.AddTaskDialog
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleUiState
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Filter mode for Task Tracker.
 */
enum class TaskFilter {
    ALL,
    PENDING,
    COMPLETED
}

/**
 * Priority filter for Task Tracker.
 */
enum class PriorityFilter(val label: String) {
    ALL("All Priorities"),
    HIGH("High Priority"),
    MEDIUM("Medium Priority"),
    LOW("Low Priority")
}

/**
 * Task Tracker screen that displays user tasks from the Room database
 * in a LazyColumn with support for marking items as complete and deleting them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerScreen(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TaskTrackerScreen(
        state = state,
        viewModel = viewModel,
        modifier = modifier,
        onNavigateBack = onNavigateBack
    )
}

/**
 * Overloaded TaskTrackerScreen taking [ScheduleUiState] and [ScheduleViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerScreen(
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }

    if (showAddTaskDialog) {
        AddTaskDialog(
            courses = state.courses,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, course, priority ->
                viewModel.addTask(title, course, priority)
                showAddTaskDialog = false
            }
        )
    }

    TaskTrackerContent(
        tasks = state.tasks,
        courses = state.courses,
        onToggleComplete = { taskId, isCompleted ->
            viewModel.toggleTaskCompleted(taskId, isCompleted)
        },
        onDeleteTask = { task ->
            viewModel.deleteTask(task)
        },
        onAddTaskClick = { showAddTaskDialog = true },
        modifier = modifier,
        onNavigateBack = onNavigateBack
    )
}

/**
 * Core Composable rendering the Task Tracker UI with LazyColumn, filters,
 * completion toggling, and deletion controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerContent(
    tasks: List<TaskEntity>,
    courses: List<CourseEntity> = emptyList(),
    onToggleComplete: (taskId: Long, isCompleted: Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddTaskClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    showTopBar: Boolean = true,
    showFab: Boolean = true
) {
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var selectedPriorityFilter by remember { mutableStateOf(PriorityFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(!showTopBar) }

    val totalCount = tasks.size
    val completedCount = tasks.count { it.isCompleted }
    val pendingCount = totalCount - completedCount
    val completionPercent = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()) else 0f

    // Filter tasks based on status, priority, and search query
    val filteredTasks = remember(tasks, selectedFilter, selectedPriorityFilter, searchQuery) {
        tasks.filter { task ->
            val matchesStatus = when (selectedFilter) {
                TaskFilter.ALL -> true
                TaskFilter.PENDING -> !task.isCompleted
                TaskFilter.COMPLETED -> task.isCompleted
            }
            val matchesPriority = when (selectedPriorityFilter) {
                PriorityFilter.ALL -> true
                PriorityFilter.HIGH -> task.priority.equals("HIGH", ignoreCase = true)
                PriorityFilter.MEDIUM -> task.priority.equals("MEDIUM", ignoreCase = true)
                PriorityFilter.LOW -> task.priority.equals("LOW", ignoreCase = true)
            }
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                task.title.contains(searchQuery, ignoreCase = true) ||
                        task.courseName.contains(searchQuery, ignoreCase = true)
            }
            matchesStatus && matchesPriority && matchesQuery
        }
    }

    val pendingFiltered = filteredTasks.filter { !it.isCompleted }
    val completedFiltered = filteredTasks.filter { it.isCompleted }

    val content = @Composable { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar (Expandable or inline)
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by title or course...".tr) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear".tr)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("input_task_search")
                    )
                }
            }

            // Summary Progress Card
            TaskTrackerProgressHeader(
                totalCount = totalCount,
                pendingCount = pendingCount,
                completedCount = completedCount,
                progress = completionPercent
            )

            // Filter Chips Bar
            TaskTrackerFilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                selectedPriority = selectedPriorityFilter,
                onPrioritySelected = { selectedPriorityFilter = it },
                totalCount = totalCount,
                pendingCount = pendingCount,
                completedCount = completedCount
            )

            Spacer(modifier = Modifier.height(8.dp))

            // LazyColumn displaying tasks from Room database
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("task_tracker_lazy_column"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = if (showFab) 88.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredTasks.isEmpty()) {
                    item {
                        TaskTrackerEmptyState(
                            hasTasksOverall = tasks.isNotEmpty(),
                            filter = selectedFilter,
                            onAddTask = onAddTaskClick
                        )
                    }
                } else {
                    // Pending Tasks Section
                    if (selectedFilter != TaskFilter.COMPLETED && pendingFiltered.isNotEmpty()) {
                        item(key = "header_pending") {
                            SectionHeader(
                                title = "Pending Tasks".tr,
                                count = pendingFiltered.size,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(
                            items = pendingFiltered,
                            key = { it.id }
                        ) { task ->
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { onToggleComplete(task.id, true) },
                                onDelete = { onDeleteTask(task) }
                            )
                        }
                    }

                    // Completed Tasks Section
                    if (selectedFilter != TaskFilter.PENDING && completedFiltered.isNotEmpty()) {
                        item(key = "header_completed") {
                            Spacer(modifier = Modifier.height(12.dp))
                            SectionHeader(
                                title = "Completed Tasks".tr,
                                count = completedFiltered.size,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        items(
                            items = completedFiltered,
                            key = { it.id }
                        ) { task ->
                            TaskItemCard(
                                task = task,
                                onToggleComplete = { onToggleComplete(task.id, false) },
                                onDelete = { onDeleteTask(task) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTopBar) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .testTag("task_tracker_screen"),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Task Tracker".tr,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (pendingCount == 0 && totalCount > 0) {
                                    "All caught up! 🎉".tr
                                } else {
                                    "$pendingCount pending · $completedCount completed".tr
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("btn_back")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back".tr
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) searchQuery = ""
                            },
                            modifier = Modifier.testTag("btn_toggle_search")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search tasks".tr
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (showFab) {
                    FloatingActionButton(
                        onClick = onAddTaskClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("fab_add_task")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task".tr
                        )
                    }
                }
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("task_tracker_screen")
        ) {
            content(PaddingValues(0.dp))
        }
    }
}

/**
 * Overview card showing total, pending, and completed tasks along with a progress indicator.
 */
@Composable
private fun TaskTrackerProgressHeader(
    totalCount: Int,
    pendingCount: Int,
    completedCount: Int,
    progress: Float
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
                Column {
                    Text(
                        text = "Semester Tasks Overview".tr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (totalCount == 0) {
                            "No tasks created in database yet".tr
                        } else {
                            "${(progress * 100).toInt()}% completed (${completedCount} of ${totalCount})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$pendingCount left",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill(label = "Total".tr, value = "$totalCount", color = MaterialTheme.colorScheme.onSurface)
                StatPill(label = "Pending".tr, value = "$pendingCount", color = MaterialTheme.colorScheme.primary)
                StatPill(label = "Completed".tr, value = "$completedCount", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Filter chips for switching between All, Pending, and Completed, plus priority.
 */
@Composable
private fun TaskTrackerFilterRow(
    selectedFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit,
    selectedPriority: PriorityFilter,
    onPrioritySelected: (PriorityFilter) -> Unit,
    totalCount: Int,
    pendingCount: Int,
    completedCount: Int
) {
    var priorityMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedFilter == TaskFilter.ALL,
            onClick = { onFilterSelected(TaskFilter.ALL) },
            label = { Text("All ($totalCount)") },
            leadingIcon = if (selectedFilter == TaskFilter.ALL) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            modifier = Modifier.testTag("filter_chip_all")
        )

        FilterChip(
            selected = selectedFilter == TaskFilter.PENDING,
            onClick = { onFilterSelected(TaskFilter.PENDING) },
            label = { Text("Pending ($pendingCount)") },
            leadingIcon = if (selectedFilter == TaskFilter.PENDING) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            modifier = Modifier.testTag("filter_chip_pending")
        )

        FilterChip(
            selected = selectedFilter == TaskFilter.COMPLETED,
            onClick = { onFilterSelected(TaskFilter.COMPLETED) },
            label = { Text("Completed ($completedCount)") },
            leadingIcon = if (selectedFilter == TaskFilter.COMPLETED) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            modifier = Modifier.testTag("filter_chip_completed")
        )

        // Priority Filter Dropdown Chip
        Box {
            FilterChip(
                selected = selectedPriority != PriorityFilter.ALL,
                onClick = { priorityMenuExpanded = true },
                label = { Text(selectedPriority.label) },
                trailingIcon = {
                    Icon(
                        imageVector = if (priorityMenuExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("filter_chip_priority")
            )

            DropdownMenu(
                expanded = priorityMenuExpanded,
                onDismissRequest = { priorityMenuExpanded = false }
            ) {
                PriorityFilter.values().forEach { priority ->
                    DropdownMenuItem(
                        text = { Text(priority.label) },
                        onClick = {
                            onPrioritySelected(priority)
                            priorityMenuExpanded = false
                        },
                        leadingIcon = if (selectedPriority == priority) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        }
    }
}

/**
 * Section Header for grouped lists.
 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Modern Task Item Card with Checkbox, Title, Badges, and Delete Button.
 */
@Composable
fun TaskItemCard(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (task.isCompleted) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Checkbox for Marking Task as Complete (Minimum 48dp touch target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.testTag("task_checkbox_${task.id}")
                )
            }

            // Task Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title.trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata Chips: Course, Priority, Due Date, Study Hours
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Associated Course / Category
                    if (task.courseName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = task.courseName,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }

                    // Priority Tag
                    val (priorityBg, priorityFg) = when (task.priority.uppercase()) {
                        "HIGH" -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                        "MEDIUM" -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = priorityBg
                    ) {
                        Text(
                            text = task.priority.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Due Date Tag
                    if (task.dueDateMillis > 0) {
                        val dueText = formatRelativeDueDate(task.dueDateMillis)
                        val isOverdue = task.dueDateMillis < System.currentTimeMillis() && !task.isCompleted
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dueText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Estimated Study Hours Tag
                    val taskHours = WeeklySummaryEngine.calculateTaskHours(task)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x1810B981)
                    ) {
                        Text(
                            text = "${taskHours}h study",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Delete Action Button (Minimum 48dp touch target)
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("task_delete_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete task: ${task.title}".tr,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty state card shown when the list of tasks is empty.
 */
@Composable
private fun TaskTrackerEmptyState(
    hasTasksOverall: Boolean,
    filter: TaskFilter,
    onAddTask: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        !hasTasksOverall -> Icons.Default.AddTask
                        filter == TaskFilter.COMPLETED -> Icons.Default.TaskAlt
                        else -> Icons.Default.DoneAll
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = when {
                    !hasTasksOverall -> "No Tasks Yet".tr
                    filter == TaskFilter.COMPLETED -> "No Completed Tasks".tr
                    filter == TaskFilter.PENDING -> "No Pending Tasks".tr
                    else -> "No Tasks Match Filters".tr
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = when {
                    !hasTasksOverall -> "Keep track of homework, exam prep, and daily campus to-dos by tapping the + button below.".tr
                    filter == TaskFilter.COMPLETED -> "Check off pending tasks as you complete them to see your progress here.".tr
                    filter == TaskFilter.PENDING -> "Great job! All your tasks are completed. Tap + to add new goals.".tr
                    else -> "Try adjusting your search query or priority filters.".tr
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Button(
                onClick = onAddTask,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_empty_state_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add First Task".tr)
            }
        }
    }
}

/**
 * Helper to compute human-friendly relative due date string.
 */
private fun formatRelativeDueDate(dueDateMillis: Long): String {
    val dueLocalDate = Instant.ofEpochMilli(dueDateMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    val daysUntil = ChronoUnit.DAYS.between(today, dueLocalDate)

    return when {
        daysUntil < -1 -> "Overdue by ${-daysUntil}d"
        daysUntil == -1L -> "Overdue (Yesterday)"
        daysUntil == 0L -> "Due Today"
        daysUntil == 1L -> "Due Tomorrow"
        daysUntil in 2..7 -> "Due in ${daysUntil}d"
        else -> DateTimeFormatter.ofPattern("MMM dd").format(dueLocalDate)
    }
}
