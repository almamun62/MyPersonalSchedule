package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.CourseEntity
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import com.example.util.FileStorageHelper
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class RoomSaveState {
    IDLE,
    SAVING,
    SAVED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullNoteEditorDialog(
    note: NoteEntity? = null,
    courses: List<CourseEntity>,
    initialCourseId: Long? = null,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Course selection
    var selectedCourseId by remember { 
        mutableStateOf(note?.courseId ?: initialCourseId ?: courses.firstOrNull()?.id ?: 0L) 
    }
    
    // Parse title & body from content
    val (initialTitle, initialBody) = remember(note) {
        if (note != null) {
            val lines = note.content.lines()
            if (lines.isNotEmpty() && lines.first().startsWith("# ")) {
                Pair(lines.first().removePrefix("# ").trim(), lines.drop(1).joinToString("\n").trim())
            } else if (lines.isNotEmpty() && lines.first().length < 60 && !lines.first().contains("\n")) {
                Pair(lines.first().trim(), lines.drop(1).joinToString("\n").trim())
            } else {
                Pair("", note.content)
            }
        } else {
            Pair("", "")
        }
    }

    var titleText by remember { mutableStateOf(initialTitle) }
    var editorTextFieldValue by remember { 
        mutableStateOf(TextFieldValue(initialBody, TextRange(initialBody.length))) 
    }
    var tagsText by remember { mutableStateOf(note?.tags ?: "") }
    var isPinned by remember { mutableStateOf(note?.tags?.contains("#Pinned") == true) }

    // Media states
    var showDrawingCanvas by remember { mutableStateOf(false) }
    var showInlineCanvasPad by remember { mutableStateOf(false) }
    var showAudioRecorder by remember { mutableStateOf(false) }

    // Room Save State
    var roomSaveState by remember { mutableStateOf(RoomSaveState.IDLE) }
    var lastSavedTimestamp by remember { mutableStateOf<Long?>(note?.timestampMillis) }

    // Camera state
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Media pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.copyImageToInternalStorage(context, uri)
            if (savedPath != null) {
                val currentText = editorTextFieldValue.text
                val appendText = if (currentText.isBlank()) "🖼️ Attachment: $savedPath" else "$currentText\n\n🖼️ Attachment: $savedPath"
                editorTextFieldValue = TextFieldValue(appendText, TextRange(appendText.length))
                Toast.makeText(context, "Image attached to note!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var fileName = "Document.pdf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: "Document.pdf"
                }
            }
            val savedPath = FileStorageHelper.copyFileToInternalStorage(context, uri, fileName)
            if (savedPath != null) {
                val currentText = editorTextFieldValue.text
                val appendText = if (currentText.isBlank()) "📄 File: $savedPath" else "$currentText\n\n📄 File: $savedPath"
                editorTextFieldValue = TextFieldValue(appendText, TextRange(appendText.length))
                Toast.makeText(context, "Document attached to note!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoFile != null && currentPhotoFile!!.exists()) {
            val savedPath = currentPhotoFile!!.absolutePath
            val currentText = editorTextFieldValue.text
            val appendText = if (currentText.isBlank()) "📷 Photo: $savedPath" else "$currentText\n\n📷 Photo: $savedPath"
            editorTextFieldValue = TextFieldValue(appendText, TextRange(appendText.length))
            Toast.makeText(context, "Camera photo attached!", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraDirectly() {
        try {
            val imgDir = File(context.filesDir, "notebook_images")
            if (!imgDir.exists()) imgDir.mkdirs()
            val file = File(imgDir, "cam_${System.currentTimeMillis()}.jpg")
            currentPhotoFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to launch camera", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchCameraDirectly()
    }

    fun triggerCameraCapture() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            launchCameraDirectly()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Formatting helper
    fun applyFormatting(prefix: String, suffix: String) {
        val text = editorTextFieldValue.text
        val selection = editorTextFieldValue.selection
        if (selection.collapsed) {
            val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.start)
            val newCursorPos = selection.start + prefix.length
            editorTextFieldValue = TextFieldValue(newText, TextRange(newCursorPos))
        } else {
            val selectedText = text.substring(selection.start, selection.end)
            val newText = text.substring(0, selection.start) + prefix + selectedText + suffix + text.substring(selection.end)
            editorTextFieldValue = TextFieldValue(newText, TextRange(selection.start + prefix.length + selectedText.length + suffix.length))
        }
    }

    // Save Logic
    fun saveNoteToRoom() {
        var targetCourseId = selectedCourseId
        if (targetCourseId == 0L && courses.isNotEmpty()) {
            targetCourseId = courses.first().id
        }

        val fullContent = buildString {
            if (titleText.isNotBlank()) {
                append("# ").append(titleText.trim()).append("\n\n")
            }
            append(editorTextFieldValue.text.trim())
        }

        if (fullContent.isBlank()) return

        val updatedTags = buildString {
            append(tagsText.replace("#Pinned", "").trim())
            if (isPinned) {
                if (isNotEmpty()) append(", ")
                append("#Pinned")
            }
        }

        if (note != null) {
            viewModel.updateNote(
                note.copy(
                    courseId = targetCourseId,
                    content = fullContent,
                    tags = updatedTags,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        } else {
            viewModel.addNote(
                NoteEntity(
                    courseId = targetCourseId,
                    content = fullContent,
                    type = NoteType.TEXT,
                    tags = updatedTags,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        }
        roomSaveState = RoomSaveState.SAVED
        lastSavedTimestamp = System.currentTimeMillis()
    }

    // Auto-save debounced effect
    LaunchedEffect(titleText, editorTextFieldValue.text, tagsText, isPinned, selectedCourseId) {
        if (titleText.isNotBlank() || editorTextFieldValue.text.isNotBlank()) {
            roomSaveState = RoomSaveState.SAVING
            delay(800L)
            saveNoteToRoom()
        }
    }

    // Reset saved indicator after 3 seconds
    LaunchedEffect(roomSaveState) {
        if (roomSaveState == RoomSaveState.SAVED) {
            delay(3000L)
            roomSaveState = RoomSaveState.IDLE
        }
    }

    // Word count & reading time calculation
    val textToCount = "${titleText} ${editorTextFieldValue.text}"
    val wordCount = remember(textToCount) {
        textToCount.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    }
    val readingTimeMin = remember(wordCount) {
        maxOf(1, (wordCount / 200))
    }

    Dialog(
        onDismissRequest = {
            saveNoteToRoom()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (note == null) "New Class Note".tr else "Edit Class Note".tr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (roomSaveState) {
                                        RoomSaveState.SAVING -> Icons.Default.Sync
                                        RoomSaveState.SAVED -> Icons.Default.CheckCircle
                                        RoomSaveState.IDLE -> Icons.Default.Storage
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (roomSaveState == RoomSaveState.SAVED) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (roomSaveState) {
                                        RoomSaveState.SAVING -> "Saving to Room DB...".tr
                                        RoomSaveState.SAVED -> "Saved to Room DB".tr
                                        RoomSaveState.IDLE -> if (lastSavedTimestamp != null) "Room DB Active".tr else "100% Offline Room DB".tr
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (roomSaveState == RoomSaveState.SAVED) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            saveNoteToRoom()
                            onDismiss()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back".tr)
                        }
                    },
                    actions = {
                        // Pin Toggle
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Note".tr,
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (note != null) {
                            IconButton(onClick = {
                                viewModel.deleteNote(note)
                                onDismiss()
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete".tr, tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Button(
                            onClick = {
                                saveNoteToRoom()
                                onDismiss()
                            },
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done".tr, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Course Selector Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Course:".tr, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        var courseMenuExpanded by remember { mutableStateOf(false) }
                        val selectedCourse = courses.find { it.id == selectedCourseId }

                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { courseMenuExpanded = true },
                                label = { Text(selectedCourse?.name ?: "Select Course".tr, fontWeight = FontWeight.Bold) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                            )

                            DropdownMenu(
                                expanded = courseMenuExpanded,
                                onDismissRequest = { courseMenuExpanded = false }
                            ) {
                                courses.forEach { course ->
                                    DropdownMenuItem(
                                        text = { Text(course.name, fontWeight = if (course.id == selectedCourseId) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedCourseId = course.id
                                            courseMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Note Area Scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Title Field
                    TextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                "Note Title (e.g. Chapter 3: Data Structures)".tr, 
                                style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            ) 
                        },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tags & Category Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = tagsText,
                            onValueChange = { tagsText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Tags / Category (e.g. #Lecture, #Exam, #Readings)".tr, style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Template Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Templates:".tr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                AssistChip(
                                    onClick = {
                                        if (titleText.isBlank()) titleText = "Cornell Study Note"
                                        editorTextFieldValue = TextFieldValue("📌 Key Concepts:\n- \n\n📝 Lecture Notes:\n- \n\n❓ Review Questions:\n- \n\n💡 Summary & Takeaways:\n- ")
                                        tagsText = "#Cornell"
                                    },
                                    label = { Text("🎓 Cornell", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        if (titleText.isBlank()) titleText = "Lecture Summary"
                                        editorTextFieldValue = TextFieldValue("🎯 Objectives:\n- \n\n📖 Key Formulas & Definitions:\n- \n\n⚡ Important Examples:\n- \n\n❓ Follow-up Questions:\n- ")
                                        tagsText = "#Lecture"
                                    },
                                    label = { Text("📘 Lecture", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        if (titleText.isBlank()) titleText = "Exam Review Sheet"
                                        editorTextFieldValue = TextFieldValue("🎯 High-Yield Topics:\n- \n\n⚠️ Common Mistakes to Avoid:\n- \n\n💡 Core Formulas:\n- \n\n📝 Practice Questions:\n- ")
                                        tagsText = "#ExamPrep"
                                    },
                                    label = { Text("🎯 Exam Prep", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        if (titleText.isBlank()) titleText = "Lab & Experiment Notes"
                                        editorTextFieldValue = TextFieldValue("🔬 Experiment Objective:\n- \n\n🧪 Procedure & Steps:\n- \n\n📊 Observations & Data:\n- \n\n✅ Results & Conclusion:\n- ")
                                        tagsText = "#Lab"
                                    },
                                    label = { Text("🧪 Lab Report", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        if (titleText.isBlank()) titleText = "Study Checklist"
                                        editorTextFieldValue = TextFieldValue("- [ ] Read Chapter Slides\n- [ ] Complete Homework Problem 1-5\n- [ ] Practice Quiz Questions\n- [ ] Review Lecture Recording")
                                        tagsText = "#Checklist"
                                    },
                                    label = { Text("📋 Checklist", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Formatting Toolbar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { applyFormatting("**", "**") }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { applyFormatting("*", "*") }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { applyFormatting("# ", "") }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Title, contentDescription = "Header 1", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { applyFormatting("## ", "") }, modifier = Modifier.size(36.dp)) {
                                Text("H2", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(onClick = { applyFormatting("\n- ", "") }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { applyFormatting("\n1. ", "") }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { applyFormatting("\n- [ ] ", "") }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Checklist, contentDescription = "Checkbox List", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { applyFormatting("\n> ", "") }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.FormatQuote, contentDescription = "Quote", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main Content Editor Field
                    OutlinedTextField(
                        value = editorTextFieldValue,
                        onValueChange = { editorTextFieldValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 320.dp),
                        placeholder = { Text("Start typing your class notes, formula derivations, or summaries here...".tr) },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Inline Drawing Canvas Pad
                    AnimatedVisibility(
                        visible = showInlineCanvasPad,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            InlineCanvasPad(
                                onSaveDrawing = { savedPath ->
                                    showInlineCanvasPad = false
                                    val currentText = editorTextFieldValue.text
                                    val appendText = if (currentText.isBlank()) "✍️ Stylus Sketch: $savedPath" else "$currentText\n\n✍️ Stylus Sketch: $savedPath"
                                    editorTextFieldValue = TextFieldValue(appendText, TextRange(appendText.length))
                                    Toast.makeText(context, "Inline canvas drawing inserted into note!", Toast.LENGTH_SHORT).show()
                                },
                                onClose = { showInlineCanvasPad = false }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Media Attachments Bar & Live Word Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { triggerCameraCapture() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Camera Photo".tr, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.Collections, contentDescription = "Gallery Image".tr, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            IconButton(
                                onClick = { showAudioRecorder = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Note".tr, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            IconButton(
                                onClick = { showInlineCanvasPad = !showInlineCanvasPad },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (showInlineCanvasPad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                                    .size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Gesture,
                                    contentDescription = "Touch & Stylus Canvas".tr,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (showInlineCanvasPad) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            IconButton(
                                onClick = { showDrawingCanvas = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.OpenInFull, contentDescription = "Full Infinite Canvas".tr, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            IconButton(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/*")
                                    )
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Attach Document".tr, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        // Word count & reading time
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$wordCount words • $readingTimeMin min read",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Drawing Canvas Dialog
    if (showDrawingCanvas) {
        DrawingCanvasDialog(
            onSave = { savedPath ->
                showDrawingCanvas = false
                val currentText = editorTextFieldValue.text
                val appendText = if (currentText.isBlank()) "✍️ Stylus Sketch: $savedPath" else "$currentText\n\n✍️ Stylus Sketch: $savedPath"
                editorTextFieldValue = TextFieldValue(appendText, TextRange(appendText.length))
                Toast.makeText(context, "Handwriting drawing attached!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDrawingCanvas = false }
        )
    }

    // Audio Recorder Dialog
    if (showAudioRecorder) {
        AudioRecorderDialog(
            onSaveAudio = { filePath, title ->
                showAudioRecorder = false
                val currentText = editorTextFieldValue.text
                val appendText = if (currentText.isBlank()) "🎤 Voice Note ($title): $filePath" else "$currentText\n\n🎤 Voice Note ($title): $filePath"
                editorTextFieldValue = TextFieldValue(appendText, TextRange(appendText.length))
                Toast.makeText(context, "Voice recording attached!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAudioRecorder = false }
        )
    }
}
