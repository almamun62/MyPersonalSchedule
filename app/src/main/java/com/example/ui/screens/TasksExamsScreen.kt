package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamEntity
import com.example.data.model.TaskEntity
import com.example.ui.components.AddTaskDialog
import com.example.ui.viewmodel.ScheduleUiState
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TasksExamsScreen(
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Tasks, 1 = Exams
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }

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

    if (showAddExamDialog) {
        AddExamDialog(
            semesterId = state.activeSemester?.id ?: 1,
            courses = state.courses.map { it.name }.distinct(),
            onDismiss = { showAddExamDialog = false },
            onConfirm = { exam ->
                viewModel.addExam(exam)
                showAddExamDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddTaskDialog = true else showAddExamDialog = true
                },
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("fab_add_task_or_exam")
            ) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Default.AddTask else Icons.Default.PostAdd,
                    contentDescription = "Add"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Tab Selector: Tasks vs Exams
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tasks & HW")
                            Badge { Text("${state.pendingTasks.size}") }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Exams")
                            if (state.exams.isNotEmpty()) {
                                Badge { Text("${state.exams.size}") }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Tasks List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    if (state.tasks.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.AssignmentTurnedIn,
                                message = "No tasks yet! Tap the + button or use the post-class homework prompt after any lecture."
                            )
                        }
                    } else {
                        // Pending Tasks
                        if (state.pendingTasks.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Pending (${state.pendingTasks.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(state.pendingTasks, key = { it.id }) { task ->
                                TaskItemRow(
                                    task = task,
                                    onToggle = { viewModel.toggleTaskCompleted(task.id, true) },
                                    onDelete = { viewModel.deleteTask(task) }
                                )
                            }
                        }

                        // Completed Tasks
                        val completedTasks = state.tasks.filter { it.isCompleted }
                        if (completedTasks.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Completed (${completedTasks.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(completedTasks, key = { it.id }) { task ->
                                TaskItemRow(
                                    task = task,
                                    onToggle = { viewModel.toggleTaskCompleted(task.id, false) },
                                    onDelete = { viewModel.deleteTask(task) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Exams List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    if (state.exams.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.EventNote,
                                message = "No upcoming exams recorded. Add your midterms and finals to see live day countdowns!"
                            )
                        }
                    } else {
                        items(state.exams, key = { it.id }) { exam ->
                            ExamCard(exam = exam, onDelete = { viewModel.deleteExam(exam) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItemRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.courseName.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = task.courseName,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Priority tag
                    val pColor = when (task.priority) {
                        "HIGH" -> MaterialTheme.colorScheme.error
                        "MEDIUM" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Text(
                        text = task.priority,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = pColor
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ExamCard(
    exam: ExamEntity,
    onDelete: () -> Unit
) {
    val examDate = Instant.ofEpochMilli(exam.examDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val daysLeft = ChronoUnit.DAYS.between(today, examDate)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Days Left Countdown Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    daysLeft <= 3 -> MaterialTheme.colorScheme.errorContainer
                    daysLeft <= 7 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(60.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$daysLeft",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = when {
                            daysLeft <= 3 -> MaterialTheme.colorScheme.onErrorContainer
                            daysLeft <= 7 -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                    Text(
                        text = if (daysLeft == 1L) "day left" else "days left",
                        fontSize = 9.sp,
                        color = when {
                            daysLeft <= 3 -> MaterialTheme.colorScheme.onErrorContainer
                            daysLeft <= 7 -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = exam.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Date: ${examDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Time: ${exam.startTime} - ${exam.endTime} • Room: ${exam.classroom}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exam.seatNumber.isNotEmpty()) {
                    Text("Seat: ${exam.seatNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                if (exam.notes.isNotEmpty()) {
                    Text("Note: ${exam.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AddExamDialog(
    semesterId: Long,
    courses: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (ExamEntity) -> Unit
) {
    var courseName by remember { mutableStateOf(courses.firstOrNull() ?: "") }
    var classroom by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf(LocalDate.now().plusDays(14).toString()) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("11:00") }
    var seatNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exam Countdown") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Course Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Room *") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = seatNumber,
                    onValueChange = { seatNumber = it },
                    label = { Text("Seat Number (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Allowed Materials / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courseName.isNotBlank() && classroom.isNotBlank()) {
                        val epochMillis = try {
                            LocalDate.parse(dateString).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } catch (e: Exception) {
                            System.currentTimeMillis() + 86400000L * 14
                        }
                        onConfirm(
                            ExamEntity(
                                semesterId = semesterId,
                                courseName = courseName.trim(),
                                classroom = classroom.trim(),
                                examDateMillis = epochMillis,
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                seatNumber = seatNumber.trim(),
                                notes = notes.trim()
                            )
                        )
                    }
                },
                enabled = courseName.isNotBlank() && classroom.isNotBlank()
            ) {
                Text("Add Exam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
