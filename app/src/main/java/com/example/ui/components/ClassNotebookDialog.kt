package com.example.ui.components

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.tr
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
    
    var showFullEditorForNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showNewFullEditor by remember { mutableStateOf(false) }

    var showDrawingCanvas by remember { mutableStateOf(false) }
    var showAudioRecorder by remember { mutableStateOf(false) }

    // In-App Reader / Viewer Dialog States
    var activeDocumentForReader by remember { mutableStateOf<Pair<String, String>?>(null) }
    var activeImageForViewer by remember { mutableStateOf<Pair<String, String>?>(null) }

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

    // Direct Camera Picture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && currentPhotoFile != null && currentPhotoFile!!.exists()) {
                viewModel.addNote(
                    NoteEntity(
                        courseId = course.id,
                        content = "📷 Photo: ${currentPhotoFile!!.absolutePath}",
                        tags = "#Photo",
                        type = NoteType.IMAGE
                    )
                )
                Toast.makeText(context, "Camera photo saved to notebook!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Unable to launch camera", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) launchCameraDirectly()
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
                            content = "🖼️ Attachment: $localPath",
                            tags = "#Image",
                            type = NoteType.IMAGE
                        )
                    )
                    Toast.makeText(context, "Image saved to notebook!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Document / PDF Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                var fileName = "CourseMaterial.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "CourseMaterial.pdf"
                    }
                }
                val localPath = com.example.util.FileStorageHelper.copyFileToInternalStorage(context, uri, fileName)
                if (localPath != null) {
                    viewModel.addNote(
                        NoteEntity(
                            courseId = course.id,
                            content = "$localPath|$fileName",
                            tags = "#Material",
                            type = NoteType.FILE
                        )
                    )
                    Toast.makeText(context, "Material imported!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

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
                        content = "✍️ Stylus Sketch: $path",
                        tags = "#Drawing",
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
                        tags = if (title.isBlank()) "#Voice" else "#Voice, $title",
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
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // App Bar
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "${course.name} Study Hub".tr,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF16A34A)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${notes.size} items in 100% offline Room DB".tr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF16A34A)
                                    )
                                }
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
                                placeholder = { Text("Search inside ${course.name} notes...".tr, style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (inHubSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { inHubSearchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear".tr, modifier = Modifier.size(18.dp))
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
                                        label = { Text(category.tr) },
                                        leadingIcon = when (category) {
                                            "Pinned" -> { { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(14.dp)) } }
                                            "Voice" -> { { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp)) } }
                                            "Handwriting" -> { { Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(14.dp)) } }
                                            else -> null
                                        }
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
                        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
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
                                        text = if (inHubSearchQuery.isNotBlank()) "No notes match search query.".tr else "No notes or materials found in this category.".tr,
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
                                        showFullEditorForNote = note
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
                }

                // Bottom Action Toolbar (Quick Media + Write Note Button)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { triggerCameraCapture() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Camera Photo".tr, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Collections, contentDescription = "Gallery Photo".tr, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { showAudioRecorder = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = "Record Voice Note".tr, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { showDrawingCanvas = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Draw, contentDescription = "Stylus Drawing".tr, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                filePickerLauncher.launch(
                                    arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/*")
                                )
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Upload Material".tr, modifier = Modifier.size(20.dp))
                            }
                        }

                        Button(
                            onClick = { showNewFullEditor = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Write Note".tr, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // New Note Full Screen Editor
    if (showNewFullEditor) {
        FullNoteEditorDialog(
            note = null,
            courses = listOf(course),
            initialCourseId = course.id,
            viewModel = viewModel,
            onDismiss = { showNewFullEditor = false }
        )
    }

    // Edit Existing Note Full Screen Editor
    showFullEditorForNote?.let { note ->
        FullNoteEditorDialog(
            note = note,
            courses = listOf(course),
            initialCourseId = note.courseId,
            viewModel = viewModel,
            onDismiss = { showFullEditorForNote = null }
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val icon = when (note.type) {
                        NoteType.TEXT -> Icons.AutoMirrored.Filled.Notes
                        NoteType.IMAGE -> Icons.Default.Image
                        NoteType.VOICE -> Icons.Default.Mic
                        NoteType.DRAWING -> Icons.Default.Gesture
                        NoteType.FILE -> Icons.Default.PictureAsPdf
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = Color(0xFF16A34A)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Room DB",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPinToggle, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note".tr,
                            modifier = Modifier.size(16.dp),
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit".tr, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete".tr, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            when (note.type) {
                NoteType.IMAGE -> {
                    val rawPath = note.content.removePrefix("📷 Photo: ").removePrefix("🖼️ Attachment: ").trim()
                    AsyncImage(
                        model = File(rawPath),
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onViewImage(rawPath, note.tags.ifBlank { "Image Note" }) },
                        contentScale = ContentScale.Crop
                    )
                }
                NoteType.VOICE -> {
                    AudioNotePlayer(filePath = note.content)
                }
                NoteType.DRAWING -> {
                    val rawPath = note.content.removePrefix("✍️ Stylus Sketch: ").trim()
                    AsyncImage(
                        model = File(rawPath),
                        contentDescription = "Handwritten Canvas",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { onViewImage(rawPath, note.tags.ifBlank { "Handwritten Canvas" }) },
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
                                Text(text = "In-App Reader Only".tr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Button(
                            onClick = { onViewDocument(filePath, fileName) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read In-App".tr)
                        }
                    }
                }
                NoteType.TEXT -> {
                    FormattedNoteView(
                        content = note.content,
                        maxLines = 6,
                        onViewDocument = onViewDocument,
                        onViewImage = onViewImage
                    )
                }
            }

            if (note.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
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
                val file = File(filePath)
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
                    contentDescription = if (isPlaying) "Pause Voice Note".tr else "Play Voice Note".tr,
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
                        text = "Voice Recording".tr,
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
