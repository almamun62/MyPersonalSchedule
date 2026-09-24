package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.CourseEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.ui.viewmodel.ScheduleViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassNotebookDialog(
    course: CourseEntity,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.getNotesForCourse(course.id).collectAsState(initial = emptyList())
    
    var editorTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var tagsText by remember { mutableStateOf("") }
    var editModeNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showDrawingCanvas by remember { mutableStateOf(false) }
    var showAudioRecorder by remember { mutableStateOf(false) }

    // In-App Reader / Viewer Dialog States
    var activeDocumentForReader by remember { mutableStateOf<Pair<String, String>?>(null) } // filePath to fileName
    var activeImageForViewer by remember { mutableStateOf<Pair<String, String>?>(null) } // filePath to title

    // Camera state
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }

    // Filter & Search states
    var inHubSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "Notes", "Handwriting", "Materials", "Images", "Voice", "Pinned")

    val filteredNotes = remember(notes, inHubSearchQuery, selectedCategoryFilter) {
        val q = inHubSearchQuery.lowercase().trim()
        notes.filter { note ->
            val matchesQuery = q.isEmpty() || note.content.lowercase().contains(q) || note.tags.lowercase().contains(q)
            val matchesCategory = when (selectedCategoryFilter) {
                "Notes" -> note.type == NoteType.TEXT
                "Handwriting" -> note.type == NoteType.DRAWING
                "Materials" -> note.type == NoteType.FILE
                "Images" -> note.type == NoteType.IMAGE
                "Voice" -> note.type == NoteType.VOICE
                "Pinned" -> note.tags.contains("#Pinned")
                else -> true
            }
            matchesQuery && matchesCategory
        }.sortedWith(compareByDescending<NoteEntity> { it.tags.contains("#Pinned") }.thenByDescending { it.timestampMillis })
    }

    // Gallery Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val localPath = com.example.util.ImageStorageHelper.copyImageToInternalStorage(context, uri)
                if (localPath != null) {
                    viewModel.addNote(
                        NoteEntity(
                            courseId = course.id,
                            content = localPath,
                            tags = tagsText.ifBlank { "Photo" },
                            type = NoteType.IMAGE
                        )
                    )
                    Toast.makeText(context, "Image saved to app notebook!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Direct Camera Picture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && currentPhotoFile != null && currentPhotoFile!!.exists()) {
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = currentPhotoFile!!.absolutePath,
                        tags = tagsText.ifBlank { "Camera Photo" },
                        type = NoteType.IMAGE
                    )
                )
                Toast.makeText(context, "Camera photo saved to app notebook!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Camera capture cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun launchCameraDirectly() {
        try {
            val imgDir = File(context.filesDir, "notebook_images")
            if (!imgDir.exists()) imgDir.mkdirs()
            val file = File(imgDir, "cam_${System.currentTimeMillis()}.jpg")
            currentPhotoFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to launch camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                launchCameraDirectly()
            } else {
                Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun triggerCameraCapture() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            launchCameraDirectly()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Document / PDF Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                var fileName = "CourseMaterial.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex) ?: "CourseMaterial.pdf"
                        }
                    }
                }
                val localPath = com.example.util.FileStorageHelper.copyFileToInternalStorage(context, uri, fileName)
                if (localPath != null) {
                    viewModel.addNote(
                        NoteEntity(
                            courseId = course.id,
                            content = "$localPath|$fileName",
                            tags = tagsText.ifBlank { "Material" },
                            type = NoteType.FILE
                        )
                    )
                    Toast.makeText(context, "Material imported for in-app reading!", Toast.LENGTH_SHORT).show()
                    tagsText = ""
                } else {
                    Toast.makeText(context, "Failed to upload file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    fun applyFormatting(prefix: String, suffix: String = prefix) {
        val selectedText = editorTextFieldValue.getSelectedText().text
        val text = editorTextFieldValue.text
        val selection = editorTextFieldValue.selection

        if (selectedText.isNotEmpty()) {
            val newText = text.replaceRange(selection.min, selection.max, "$prefix$selectedText$suffix")
            val newSelectionRange = TextRange(selection.min + prefix.length, selection.max + prefix.length)
            editorTextFieldValue = TextFieldValue(newText, newSelectionRange)
        } else {
            val insertion = "$prefix$suffix"
            val newText = text.replaceRange(selection.min, selection.max, insertion)
            val newCursor = selection.min + prefix.length
            editorTextFieldValue = TextFieldValue(newText, TextRange(newCursor))
        }
    }

    fun saveDraft() {
        val draftText = editorTextFieldValue.text.trim()
        if (draftText.isNotBlank()) {
            if (editModeNote != null) {
                viewModel.updateNote(editModeNote!!.copy(content = draftText, tags = tagsText.trim(), timestampMillis = System.currentTimeMillis()))
                editModeNote = null
            } else {
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = draftText,
                        tags = tagsText.trim().ifBlank { "Lecture Note" },
                        type = NoteType.TEXT
                    )
                )
            }
            editorTextFieldValue = TextFieldValue("")
            tagsText = ""
        }
    }

    fun togglePin(note: NoteEntity) {
        val isPinned = note.tags.contains("#Pinned")
        val newTags = if (isPinned) {
            note.tags.replace("#Pinned", "").trim()
        } else {
            if (note.tags.isBlank()) "#Pinned" else "${note.tags} #Pinned"
        }
        viewModel.updateNote(note.copy(tags = newTags))
    }

    if (showDrawingCanvas) {
        DrawingCanvasDialog(
            onSave = { path ->
                showDrawingCanvas = false
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = path,
                        tags = tagsText.ifBlank { "Drawing" },
                        type = NoteType.DRAWING
                    )
                )
            },
            onDismiss = { showDrawingCanvas = false }
        )
    }

    if (showAudioRecorder) {
        AudioRecorderDialog(
            onSaveAudio = { path, title ->
                showAudioRecorder = false
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = path,
                        tags = if (title.isBlank()) "Voice Note" else title,
                        type = NoteType.VOICE
                    )
                )
            },
            onDismiss = { showAudioRecorder = false }
        )
    }

    if (activeDocumentForReader != null) {
        InAppDocumentReaderDialog(
            filePath = activeDocumentForReader!!.first,
            fileName = activeDocumentForReader!!.second,
            onDismiss = { activeDocumentForReader = null }
        )
    }

    if (activeImageForViewer != null) {
        InAppImageViewerDialog(
            filePath = activeImageForViewer!!.first,
            title = activeImageForViewer!!.second,
            onDismiss = { activeImageForViewer = null }
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
                                text = "${course.name} Study Hub",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${notes.size} entries saved 100% in-app offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                // Search Bar & Filter Tabs
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = inHubSearchQuery,
                            onValueChange = { inHubSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search inside ${course.name} notes...", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (inHubSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { inHubSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategoryFilter == category,
                                    onClick = { selectedCategoryFilter = category },
                                    label = { Text(category) },
                                    leadingIcon = if (category == "Pinned") {
                                        { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else if (category == "Voice") {
                                        { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Notes List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredNotes.isEmpty()) {
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
                                    text = if (inHubSearchQuery.isNotBlank()) "No notes match your search query." else "No notes or materials found in this category.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onEdit = {
                                    if (note.type == NoteType.TEXT) {
                                        editModeNote = note
                                        editorTextFieldValue = TextFieldValue(note.content, TextRange(note.content.length))
                                        tagsText = note.tags
                                    }
                                },
                                onDelete = { viewModel.deleteNote(note) },
                                onPinToggle = { togglePin(note) },
                                onViewDocument = { path, name ->
                                    activeDocumentForReader = Pair(path, name)
                                },
                                onViewImage = { path, title ->
                                    activeImageForViewer = Pair(path, title)
                                }
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
                                Text("Editing In-App Note", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { 
                                    editModeNote = null
                                    editorTextFieldValue = TextFieldValue("")
                                    tagsText = ""
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Edit", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = tagsText,
                            onValueChange = { tagsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tag / Category (e.g. #Lecture, #Exam, #Readings)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        // Formatting Toolbar & Quick Templates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { applyFormatting("**", "**") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { applyFormatting("*", "*") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { applyFormatting("\n- ", "") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { applyFormatting("\n1. ", "") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { applyFormatting("\n# ", "") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Title, contentDescription = "Header", modifier = Modifier.size(18.dp))
                                }
                            }

                            // Templates
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    AssistChip(
                                        onClick = {
                                            editorTextFieldValue = TextFieldValue("📌 Key Concepts:\n- \n\n📝 Lecture Summary:\n- \n\n❓ Questions:\n- ")
                                            tagsText = "#Cornell"
                                        },
                                        label = { Text("Cornell", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = {
                                            editorTextFieldValue = TextFieldValue("🎯 Topic:\n\n💡 Formulas / Definitions:\n- \n\n⚠️ Exam Questions:\n- ")
                                            tagsText = "#ExamPrep"
                                        },
                                        label = { Text("Exam Prep", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        
                        OutlinedTextField(
                            value = editorTextFieldValue,
                            onValueChange = { editorTextFieldValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 70.dp, max = 140.dp),
                            placeholder = { Text("Write in-app class note or summary...") },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            trailingIcon = {
                                if (editorTextFieldValue.text.isNotBlank()) {
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

                        // Action Bar: Camera, Gallery, Audio, Handwriting, PDF Upload
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FilledTonalIconButton(onClick = { triggerCameraCapture() }) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Camera Photo")
                            }
                            FilledTonalIconButton(onClick = {
                                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.Collections, contentDescription = "Gallery Photo")
                            }
                            FilledTonalIconButton(onClick = { showAudioRecorder = true }) {
                                Icon(Icons.Default.Mic, contentDescription = "Record Audio Note")
                            }
                            FilledTonalIconButton(onClick = { showDrawingCanvas = true }) {
                                Icon(Icons.Default.Draw, contentDescription = "Stylus Handwriting")
                            }
                            FilledTonalIconButton(onClick = {
                                filePickerLauncher.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/*", "image/*"))
                            }) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Upload Material")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPinToggle: () -> Unit,
    onViewDocument: (filePath: String, fileName: String) -> Unit,
    onViewImage: (filePath: String, title: String) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dateString = remember(note.timestampMillis) { formatter.format(Date(note.timestampMillis)) }
    val isPinned = note.tags.contains("#Pinned")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val icon = when(note.type) {
                        NoteType.TEXT -> Icons.AutoMirrored.Filled.Notes
                        NoteType.IMAGE -> Icons.Default.Image
                        NoteType.VOICE -> Icons.Default.Mic
                        NoteType.DRAWING -> Icons.Default.Edit
                        NoteType.FILE -> Icons.Default.PictureAsPdf
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (note.tags.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val displayTags = note.tags.replace("#Pinned", "").trim()
                        if (displayTags.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = displayTags,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPinToggle, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            modifier = Modifier.size(16.dp),
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    if (note.type == NoteType.TEXT) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
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
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onViewImage(note.content, note.tags.ifBlank { "Image Note" }) },
                        contentScale = ContentScale.Crop
                    )
                }
                NoteType.VOICE -> {
                    AudioNotePlayer(filePath = note.content)
                }
                NoteType.DRAWING -> {
                    AsyncImage(
                        model = java.io.File(note.content),
                        contentDescription = "Handwritten Canvas",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { onViewImage(note.content, note.tags.ifBlank { "Handwritten Canvas" }) },
                        contentScale = ContentScale.Fit
                    )
                }
                NoteType.FILE -> {
                    val parts = note.content.split("|")
                    val filePath = parts.getOrNull(0) ?: ""
                    val fileName = parts.getOrNull(1) ?: "Course Material"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = "In-App Reader Only", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Button(
                            onClick = { onViewDocument(filePath, fileName) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read In-App")
                        }
                    }
                }
                NoteType.TEXT -> {
                    Column {
                        FormattedNoteText(text = note.content)
                        val wordCount = remember(note.content) { note.content.trim().split("\\s+".toRegex()).size }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$wordCount words",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioNotePlayer(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var totalDurationMs by remember { mutableStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                try {
                    currentPositionMs = mediaPlayer?.currentPosition ?: 0
                } catch (e: Exception) {
                    isPlaying = false
                }
                delay(200L)
            }
        }
    }

    DisposableEffect(filePath) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }
    }

    fun togglePlayPause() {
        try {
            if (mediaPlayer == null) {
                val file = java.io.File(filePath)
                if (!file.exists()) {
                    Toast.makeText(context, "Audio file not found on device", Toast.LENGTH_SHORT).show()
                    return
                }
                val mp = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        isPlaying = false
                        currentPositionMs = 0
                    }
                }
                totalDurationMs = mp.duration
                mediaPlayer = mp
            }

            if (isPlaying) {
                mediaPlayer?.pause()
                isPlaying = false
            } else {
                mediaPlayer?.start()
                isPlaying = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Playback error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            isPlaying = false
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { togglePlayPause() },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Voice Note" else "Play Voice Note",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Voice Recording",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    val curSec = currentPositionMs / 1000
                    val totSec = if (totalDurationMs > 0) totalDurationMs / 1000 else 0
                    Text(
                        text = String.format("%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totSec / 60, totSec % 60),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val progress = if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun FormattedNoteText(text: String, modifier: Modifier = Modifier) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            val lines = text.split("\n")
            lines.forEachIndexed { index, line ->
                if (line.startsWith("# ")) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))) {
                        append(line.removePrefix("# "))
                    }
                } else {
                    var cursor = 0
                    val regex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*)")
                    val matches = regex.findAll(line)
                    for (match in matches) {
                        val matchStart = match.range.first
                        if (matchStart > cursor) {
                            append(line.substring(cursor, matchStart))
                        }
                        val matchedStr = match.value
                        if (matchedStr.startsWith("**") && matchedStr.endsWith("**") && matchedStr.length >= 4) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(matchedStr.substring(2, matchedStr.length - 2))
                            }
                        } else if (matchedStr.startsWith("*") && matchedStr.endsWith("*") && matchedStr.length >= 2) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(matchedStr.substring(1, matchedStr.length - 1))
                            }
                        } else {
                            append(matchedStr)
                        }
                        cursor = match.range.last + 1
                    }
                    if (cursor < line.length) {
                        append(line.substring(cursor))
                    }
                }
                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }
    Text(text = annotatedString, style = MaterialTheme.typography.bodyLarge, modifier = modifier)
}
