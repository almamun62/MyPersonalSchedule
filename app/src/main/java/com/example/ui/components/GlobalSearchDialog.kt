package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CourseEntity
import com.example.data.model.ExamEntity
import com.example.data.model.NoteEntity
import com.example.data.model.TaskEntity
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleUiState
import com.example.ui.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchDialog(
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    onCourseClick: (CourseEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val query = searchQuery.trim().lowercase()

    val filteredNotes = remember(query, state.allNotes) {
        if (query.isBlank()) emptyList()
        else state.allNotes.filter { it.content.lowercase().contains(query) || it.tags.lowercase().contains(query) }
    }

    val filteredTasks = remember(query, state.tasks) {
        if (query.isBlank()) emptyList()
        else state.tasks.filter { it.title.lowercase().contains(query) || it.courseName.lowercase().contains(query) }
    }

    val filteredExams = remember(query, state.exams) {
        if (query.isBlank()) emptyList()
        else state.exams.filter { it.courseName.lowercase().contains(query) || it.classroom.lowercase().contains(query) }
    }

    val filteredCourses = remember(query, state.courses) {
        if (query.isBlank()) emptyList()
        else state.courses.filter { it.name.lowercase().contains(query) || it.instructor.lowercase().contains(query) || it.location.lowercase().contains(query) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp),
                            placeholder = { Text("Search notes, tasks, exams, courses...".tr) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close".tr)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (query.isBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Type to search across all your course notes, tasks, exams & materials".tr,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        val totalResults = filteredNotes.size + filteredTasks.size + filteredExams.size + filteredCourses.size
                        if (totalResults == 0) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No matching academic data found for \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Courses
                            if (filteredCourses.isNotEmpty()) {
                                item {
                                    Text("Courses (${filteredCourses.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                items(filteredCourses, key = { "course_${it.id}" }) { course ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onCourseClick(course)
                                                onDismiss()
                                            },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(text = course.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            if (course.instructor.isNotBlank()) {
                                                Text(text = "Instructor: ${course.instructor}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            // Tasks
                            if (filteredTasks.isNotEmpty()) {
                                item {
                                    Text("Tasks & Assignments (${filteredTasks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                items(filteredTasks, key = { "task_${it.id}" }) { task ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (task.courseName.isNotBlank()) {
                                                    Text(text = task.courseName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Checkbox(
                                                checked = task.isCompleted,
                                                onCheckedChange = { viewModel.toggleTaskCompleted(task.id, !task.isCompleted) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Notes & Materials
                            if (filteredNotes.isNotEmpty()) {
                                item {
                                    Text("Course Notes & Materials (${filteredNotes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                items(filteredNotes, key = { "note_${it.id}" }) { note ->
                                    val course = state.courses.find { it.id == note.courseId }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            if (course != null) {
                                                Text(text = course.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                            Text(
                                                text = if (note.type == com.example.data.model.NoteType.FILE) {
                                                    note.content.split("|").getOrNull(1) ?: "Course Material File"
                                                } else {
                                                    note.content
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (note.tags.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "Tag: ${note.tags}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            // Exams
                            if (filteredExams.isNotEmpty()) {
                                item {
                                    Text("Exams (${filteredExams.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                items(filteredExams, key = { "exam_${it.id}" }) { exam ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(text = exam.courseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(text = "Location: ${exam.classroom}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
