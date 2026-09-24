package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.ui.components.ClassNotebookDialog
import com.example.ui.components.FormattedNoteView
import com.example.ui.components.FullNoteEditorDialog
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleUiState
import com.example.ui.viewmodel.ScheduleViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    state: ScheduleUiState,
    viewModel: ScheduleViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf<Long?>(null) }
    var activeCourseForNotebook by remember { mutableStateOf<CourseEntity?>(null) }
    var noteForFullEditor by remember { mutableStateOf<NoteEntity?>(null) }
    var showNewNoteEditor by remember { mutableStateOf(false) }

    val allCourses = state.courses

    // Filter notes across courses
    val filteredNotes = state.allNotes.filter { note ->
        val matchesCourse = selectedCourseId == null || note.courseId == selectedCourseId
        val matchesSearch = searchQuery.isBlank() || 
                            note.content.contains(searchQuery, ignoreCase = true) ||
                            note.tags.contains(searchQuery, ignoreCase = true)
        matchesCourse && matchesSearch
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Search Area
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back".tr
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Course Notes Archive".tr,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100% Offline Study Hub for Class Notes".tr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search course notes, tags, or materials...".tr) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear".tr)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Course Filter Chips
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCourseId == null,
                                onClick = { selectedCourseId = null },
                                label = { Text("All Courses (${state.allNotes.size})".tr) }
                            )
                        }
                        items(allCourses) { course ->
                            val courseNotesCount = state.allNotes.count { it.courseId == course.id }
                            FilterChip(
                                selected = selectedCourseId == course.id,
                                onClick = { selectedCourseId = course.id },
                                label = { Text("${course.name} ($courseNotesCount)") }
                            )
                        }
                    }
                }
            }

            // Notes List
            if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notes found in archive".tr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '+ Write Note' below to compose a new full-screen class note.".tr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        val course = allCourses.find { it.id == note.courseId }
                        GlobalNoteCard(
                            note = note,
                            courseName = course?.name ?: "General Notes".tr,
                            onEditNote = {
                                noteForFullEditor = note
                            },
                            onOpenNotebook = {
                                if (course != null) {
                                    activeCourseForNotebook = course
                                } else if (allCourses.isNotEmpty()) {
                                    activeCourseForNotebook = allCourses.first()
                                }
                            },
                            onDelete = { viewModel.deleteNote(note) }
                        )
                    }
                }
            }
        }

        // Action FABs
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            ExtendedFloatingActionButton(
                onClick = { showNewNoteEditor = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Write Note".tr, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    }

    // New Note Full Screen Editor
    if (showNewNoteEditor) {
        FullNoteEditorDialog(
            note = null,
            courses = allCourses,
            initialCourseId = selectedCourseId,
            viewModel = viewModel,
            onDismiss = { showNewNoteEditor = false }
        )
    }

    // Edit Existing Note Full Screen Editor
    noteForFullEditor?.let { note ->
        FullNoteEditorDialog(
            note = note,
            courses = allCourses,
            initialCourseId = note.courseId,
            viewModel = viewModel,
            onDismiss = { noteForFullEditor = null }
        )
    }

    // Active Course Study Hub Notebook
    activeCourseForNotebook?.let { course ->
        ClassNotebookDialog(
            course = course,
            viewModel = viewModel,
            onDismiss = { activeCourseForNotebook = null }
        )
    }
}

@Composable
fun GlobalNoteCard(
    note: NoteEntity,
    courseName: String,
    onEditNote: () -> Unit,
    onOpenNotebook: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm").withZone(ZoneId.systemDefault())
    val timeString = formatter.format(Instant.ofEpochMilli(note.timestampMillis))

    val (typeLabel, typeIcon) = when (note.type) {
        NoteType.DRAWING -> Pair("Handwriting / Stylus".tr, Icons.Default.Gesture)
        NoteType.FILE -> Pair("PDF / Document".tr, Icons.Default.PictureAsPdf)
        NoteType.IMAGE -> Pair("Photo / Attachment".tr, Icons.Default.Image)
        NoteType.VOICE -> Pair("Voice Note".tr, Icons.Default.Mic)
        NoteType.TEXT -> Pair("Formatted Class Note".tr, Icons.Default.Notes)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditNote)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(typeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = courseName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditNote, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Note".tr, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete".tr, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FormattedNoteView(
                content = note.content,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFDCFCE7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = androidx.compose.ui.graphics.Color(0xFF16A34A)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Saved in Room DB",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = androidx.compose.ui.graphics.Color(0xFF15803D)
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onOpenNotebook,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Course Hub".tr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            if (note.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(end = 2.dp)
                        ) {
                            Text(
                                text = if (tag.startsWith("#")) tag else "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
