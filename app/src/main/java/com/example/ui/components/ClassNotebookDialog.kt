package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CourseEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassNotebookDialog(
    course: CourseEntity,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.getNotesForCourse(course.id).collectAsState(initial = emptyList())
    var draftText by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var editModeNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showDrawingCanvas by remember { mutableStateOf(false) }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Copy to internal storage so it survives app restarts
                val localPath = com.example.util.ImageStorageHelper.copyImageToInternalStorage(context, uri)
                if (localPath != null) {
                    viewModel.addNote(
                        NoteEntity(
                            courseId = course.id,
                            content = localPath,
                            type = NoteType.IMAGE
                        )
                    )
                    Toast.makeText(context, "Image saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    fun saveDraft() {
        if (draftText.isNotBlank()) {
            if (editModeNote != null) {
                viewModel.updateNote(editModeNote!!.copy(content = draftText, tags = tagsText.trim(), timestampMillis = System.currentTimeMillis()))
                editModeNote = null
            } else {
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = draftText.trim(),
                        tags = tagsText.trim(),
                        type = NoteType.TEXT
                    )
                )
            }
            draftText = ""
            tagsText = ""
        }
    }

    if (showDrawingCanvas) {
        DrawingCanvasDialog(
            onSave = { path ->
                showDrawingCanvas = false
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = path,
                        type = NoteType.DRAWING
                    )
                )
            },
            onDismiss = { showDrawingCanvas = false }
        )
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
                // App Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "${course.name} Notes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Auto-saving locally".tr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close".tr)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                // Notes List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (notes.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No notes yet.".tr,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        items(notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onEdit = {
                                    if (note.type == NoteType.TEXT) {
                                        editModeNote = note
                                        draftText = note.content
                                        tagsText = note.tags
                                    }
                                },
                                onDelete = { viewModel.deleteNote(note) }
                            )
                        }
                    }
                }

                // Editor Bottom Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        if (editModeNote != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Editing Note", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { 
                                    editModeNote = null
                                    draftText = ""
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Edit", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 150.dp),
                            placeholder = { Text("Write something...") },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (draftText.isNotBlank()) {
                                    IconButton(
                                        onClick = { saveDraft() },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FilledTonalIconButton(onClick = { /* Already active text mode */ }) {
                                Icon(Icons.Default.Keyboard, contentDescription = "Type".tr)
                            }
                            FilledTonalIconButton(onClick = {
                                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Image".tr)
                            }
                            FilledTonalIconButton(onClick = {
                                showDrawingCanvas = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Draw".tr)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: NoteEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dateString = remember(note.timestampMillis) { formatter.format(Date(note.timestampMillis)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when(note.type) {
                        NoteType.TEXT -> Icons.AutoMirrored.Filled.Notes
                        NoteType.IMAGE -> Icons.Default.Image
                        NoteType.VOICE -> Icons.Default.Mic
                        NoteType.DRAWING -> Icons.Default.Edit
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    if (note.type == NoteType.TEXT) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (note.type) {
                NoteType.IMAGE -> {
                    AsyncImage(
                        model = java.io.File(note.content),
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                NoteType.VOICE -> {
                    Text(note.content, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                NoteType.DRAWING -> {
                    AsyncImage(
                        model = java.io.File(note.content),
                        contentDescription = "Handwritten Canvas",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                }
                NoteType.TEXT -> {
                    Text(note.content, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
