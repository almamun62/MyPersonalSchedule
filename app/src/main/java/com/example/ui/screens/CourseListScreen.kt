package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Course
import com.example.ui.theme.AppIcons
import com.example.ui.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    viewModel: ScheduleViewModel
) {
    val filteredCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var courseToEdit by remember { mutableStateOf<Course?>(null) }
    var notebookCourse by remember { mutableStateOf<com.example.data.model.CourseEntity?>(null) }

    val totalCredits = filteredCourses.distinctBy { it.code }.sumOf { it.credits }
    val uniqueClassesCount = filteredCourses.distinctBy { it.code }.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Enrolled Courses", fontWeight = FontWeight.Bold)
                        Text(
                            "$uniqueClassesCount courses • $totalCredits Total Credits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("course_list_add_fab")
            ) {
                Icon(AppIcons.Add, contentDescription = "Add Course")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by code, title, or instructor...") },
                leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("course_search_input")
            )

            if (filteredCourses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isNotBlank()) "No courses match \"$searchQuery\"" else "No courses in $selectedSemester",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCourses, key = { it.id }) { course ->
                        CourseRowItem(
                            course = course,
                            onOpenNotebook = { notebookCourse = course.toEntity() },
                            onEdit = { courseToEdit = course },
                            onDelete = { viewModel.deleteCourse(course) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditCourseDialog(
            defaultSemester = selectedSemester,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.addCourse(it)
                showAddDialog = false
            }
        )
    }

    courseToEdit?.let { course ->
        AddEditCourseDialog(
            initialCourse = course,
            defaultSemester = course.semester,
            onDismiss = { courseToEdit = null },
            onSave = {
                viewModel.updateCourse(it)
                courseToEdit = null
            }
        )
    }

    notebookCourse?.let { courseEntity ->
        com.example.ui.components.ClassNotebookDialog(
            course = courseEntity,
            viewModel = viewModel,
            onDismiss = { notebookCourse = null }
        )
    }
}

@Composable
fun CourseRowItem(
    course: Course,
    onOpenNotebook: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val courseColor = try {
        Color(android.graphics.Color.parseColor(course.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(courseColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        course.code,
                        fontWeight = FontWeight.Bold,
                        color = courseColor,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            "${course.dayShortName} ${course.startTime}-${course.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    course.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                if (course.classroom.isNotBlank() || course.instructor.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (course.classroom.isNotBlank()) {
                            Text("📍 ${course.classroom}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (course.instructor.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(AppIcons.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(course.instructor, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenNotebook,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(AppIcons.Edit, contentDescription = "Open Class Notebook", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                val context = androidx.compose.ui.platform.LocalContext.current
                IconButton(
                    onClick = {
                        val entity = course.toEntity()
                        com.example.util.CalendarExportHelper.exportCourseToCalendar(context, entity)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(AppIcons.Share, contentDescription = "Add to Calendar", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(AppIcons.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(AppIcons.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                }
            }
        }
    }
}
