package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Course
import com.example.domain.model.CourseConflict
import com.example.domain.model.ImportedCourse
import com.example.domain.parser.ScheduleParser
import com.example.ui.theme.AppIcons
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ConflictResolution
import com.example.ui.viewmodel.ImportTab
import com.example.ui.viewmodel.ImportViewModel
import com.example.ui.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScheduleScreen(
    importViewModel: ImportViewModel,
    scheduleViewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTimetable: () -> Unit
) {
    val context = LocalContext.current
    val currentTab by importViewModel.currentTab.collectAsStateWithLifecycle()
    val rawInputText by importViewModel.rawInputText.collectAsStateWithLifecycle()
    val targetSemester by importViewModel.targetSemester.collectAsStateWithLifecycle()
    val parsedCourses by importViewModel.parsedCourses.collectAsStateWithLifecycle()
    val detectedConflicts by importViewModel.detectedConflicts.collectAsStateWithLifecycle()
    val conflictResolution by importViewModel.conflictResolution.collectAsStateWithLifecycle()
    val isProcessing by importViewModel.isProcessing.collectAsStateWithLifecycle()
    val statusMessage by importViewModel.statusMessage.collectAsStateWithLifecycle()
    val isSuccess by importViewModel.isSuccess.collectAsStateWithLifecycle()
    val editingCourse by importViewModel.editingCourse.collectAsStateWithLifecycle()

    val existingCourses by scheduleViewModel.allCourses.collectAsStateWithLifecycle()

    LaunchedEffect(existingCourses, targetSemester) {
        importViewModel.updateExistingScheduleCourses(existingCourses)
    }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var importedCount by remember { mutableStateOf(0) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            importViewModel.scanScheduleImage(bitmap)
        }
    }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                capturedBitmap = bitmap
                importViewModel.scanScheduleImage(bitmap)
            } catch (e: Exception) {
                // Ignore decoding error
            }
        }
    }

    // Document / CSV File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            importViewModel.parseFileUri(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Import Schedule",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Fast multi-source timetable reader",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("import_back_button")
                    ) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { importViewModel.clearAll() },
                        modifier = Modifier.testTag("import_clear_button")
                    ) {
                        Icon(AppIcons.Delete, contentDescription = "Reset", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card: 1-Click Fast Import for Fall 2026 Schedule
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("hero_quick_import_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "1-Click Fast Import",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "RECOMMENDED",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Fall 2026 Schedule (Mamun) • 7 Courses, 14 Class Sessions",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scheduleViewModel.loadPresetMamunSchedule(bilingual = true, replaceExisting = true)
                                    importedCount = 14
                                    showSuccessDialog = true
                                },
                                modifier = Modifier.weight(1.3f).testTag("direct_import_mamun_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Directly", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    importViewModel.loadMamunDirectly(bilingual = true)
                                },
                                modifier = Modifier.weight(1f).testTag("preview_mamun_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview First", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Step 1: Input Source Selector Tabs
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        TabRow(
                            selectedTabIndex = currentTab.ordinal,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Tab(
                                selected = currentTab == ImportTab.QUICK_PASTE,
                                onClick = { importViewModel.setTab(ImportTab.QUICK_PASTE) },
                                text = { Text("Paste Text", fontSize = 12.sp) },
                                icon = { Icon(AppIcons.Paste, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = currentTab == ImportTab.FILE_CSV,
                                onClick = { importViewModel.setTab(ImportTab.FILE_CSV) },
                                text = { Text("CSV / File", fontSize = 12.sp) },
                                icon = { Icon(AppIcons.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = currentTab == ImportTab.CAMERA_OCR,
                                onClick = { importViewModel.setTab(ImportTab.CAMERA_OCR) },
                                text = { Text("AI Scan", fontSize = 12.sp) },
                                icon = { Icon(AppIcons.Camera, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = currentTab == ImportTab.PRESETS,
                                onClick = { importViewModel.setTab(ImportTab.PRESETS) },
                                text = { Text("Presets", fontSize = 12.sp) },
                                icon = { Icon(AppIcons.Courses, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }

                        // Tab Contents
                        Box(modifier = Modifier.padding(16.dp)) {
                            when (currentTab) {
                                ImportTab.QUICK_PASTE -> {
                                    QuickPasteView(
                                        rawText = rawInputText,
                                        onTextChanged = { importViewModel.setRawInputText(it) },
                                        onPasteFromClipboard = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = clipboard.primaryClip
                                            if (clip != null && clip.itemCount > 0) {
                                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                                importViewModel.setRawInputText(text)
                                            }
                                        },
                                        onLoadSample = { importViewModel.loadSampleCsvTemplate() },
                                        onLoadMamunSample = {
                                            importViewModel.setRawInputText(com.example.util.ScheduleImportHelper.MAMUN_SCHEDULE_RAW_TEXT)
                                            importViewModel.parseInputText()
                                        }
                                    )
                                }
                                ImportTab.FILE_CSV -> {
                                    FileUploadView(
                                        onPickFile = { filePickerLauncher.launch("*/*") },
                                        onLoadSampleCsv = { importViewModel.loadSampleCsvTemplate() },
                                        onLoadMamunSchedule = {
                                            importViewModel.loadMamunDirectly(bilingual = true)
                                        }
                                    )
                                }
                                ImportTab.CAMERA_OCR -> {
                                    CameraScanView(
                                        capturedBitmap = capturedBitmap,
                                        isProcessing = isProcessing,
                                        onCapturePhoto = { cameraLauncher.launch(null) },
                                        onPickGallery = { galleryLauncher.launch("image/*") },
                                        onRescan = {
                                            capturedBitmap?.let { importViewModel.scanScheduleImage(it) }
                                        }
                                    )
                                }
                                ImportTab.PRESETS -> {
                                    PresetsView(
                                        onSelectPreset = { presetName ->
                                            importViewModel.loadPreset(presetName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Parse Action Button (if in text or file mode)
            if (currentTab == ImportTab.QUICK_PASTE || currentTab == ImportTab.FILE_CSV) {
                item {
                    Button(
                        onClick = { importViewModel.parseInputText() },
                        enabled = !isProcessing && rawInputText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("parse_schedule_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Parsing Schedule...")
                        } else {
                            Icon(AppIcons.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze & Detect Courses")
                        }
                    }
                }
            }

            // Status message
            if (statusMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                AppIcons.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Step 2 & 3: Parsed Results & Conflict Resolution Section
            if (parsedCourses.isNotEmpty()) {
                // Summary Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            label = "Detected",
                            value = "${parsedCourses.size} classes",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "Conflicts",
                            value = "${detectedConflicts.size} overlap",
                            color = if (detectedConflicts.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        val totalCredits = parsedCourses.filter { it.isSelected }.sumOf { it.credits }
                        StatChip(
                            label = "Credits",
                            value = "$totalCredits hrs",
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Target Semester and Conflict Resolution Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Target Semester & Import Destination",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )

                            OutlinedTextField(
                                value = targetSemester,
                                onValueChange = { importViewModel.setTargetSemester(it) },
                                label = { Text("Semester Name") },
                                placeholder = { Text("e.g. Fall 2025") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("import_target_semester_input")
                            )

                            // Conflict Resolution Mode if conflicts exist
                            if (detectedConflicts.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                AppIcons.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Schedule Conflicts Detected (${detectedConflicts.size})",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Text(
                                            "Choose how to handle conflicting classes:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ResolutionChoiceChip(
                                                selected = conflictResolution == ConflictResolution.KEEP_ALL,
                                                title = "Keep All (Allow Overlaps)",
                                                subtitle = "Import all selected courses regardless of overlapping hours",
                                                onClick = { importViewModel.setConflictResolution(ConflictResolution.KEEP_ALL) }
                                            )

                                            ResolutionChoiceChip(
                                                selected = conflictResolution == ConflictResolution.SKIP_CONFLICTS,
                                                title = "Skip Conflicting Courses",
                                                subtitle = "Only import non-conflicting classes safely",
                                                onClick = { importViewModel.setConflictResolution(ConflictResolution.SKIP_CONFLICTS) }
                                            )

                                            ResolutionChoiceChip(
                                                selected = conflictResolution == ConflictResolution.REPLACE_SEMESTER,
                                                title = "Replace Semester Schedule",
                                                subtitle = "Wipe existing $targetSemester courses and replace with this import",
                                                onClick = { importViewModel.setConflictResolution(ConflictResolution.REPLACE_SEMESTER) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selection Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Courses to Import (${parsedCourses.count { it.isSelected }} of ${parsedCourses.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row {
                            TextButton(onClick = { importViewModel.selectAll(true) }) {
                                Text("Select All")
                            }
                            TextButton(onClick = { importViewModel.selectAll(false) }) {
                                Text("Deselect All")
                            }
                        }
                    }
                }

                // Parsed Courses List
                items(parsedCourses, key = { it.tempId }) { course ->
                    ParsedCourseCard(
                        course = course,
                        onToggle = { importViewModel.toggleCourseSelection(course.tempId) },
                        onEdit = { importViewModel.setEditingCourse(course) },
                        onDelete = { importViewModel.removeParsedCourse(course.tempId) }
                    )
                }

                // Final Confirm Import Button
                item {
                    val selectedCount = parsedCourses.count { it.isSelected }
                    Button(
                        onClick = {
                            importViewModel.executeImport(
                                repository = scheduleViewModel.repository,
                                onSuccess = { count ->
                                    importedCount = count
                                    showSuccessDialog = true
                                }
                            )
                        },
                        enabled = selectedCount > 0 && !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 8.dp)
                            .testTag("confirm_import_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving to Database...")
                        } else {
                            Icon(AppIcons.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Confirm Import ($selectedCount Courses)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    // Inline Course Edit Dialog
    editingCourse?.let { course ->
        EditParsedCourseDialog(
            course = course,
            onDismiss = { importViewModel.setEditingCourse(null) },
            onSave = { updated ->
                importViewModel.saveEditedCourse(updated)
            }
        )
    }

    // Success Confirmation Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateToTimetable()
            },
            icon = {
                Icon(
                    AppIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Import Complete!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    "Successfully imported $importedCount class sessions into your \"$targetSemester\" timetable schedule. You can now view and manage them in your weekly calendar grid.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToTimetable()
                    },
                    modifier = Modifier.testTag("view_timetable_after_import_button")
                ) {
                    Text("View Timetable")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSuccessDialog = false }) {
                    Text("Import More")
                }
            }
        )
    }
}

@Composable
fun QuickPasteView(
    rawText: String,
    onTextChanged: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onLoadSample: () -> Unit,
    onLoadMamunSample: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Paste Schedule Text", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onPasteFromClipboard) {
                Icon(AppIcons.Paste, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paste", fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = onLoadMamunSample,
                label = { Text("⚡ Fall 2026 Raw Text", fontSize = 11.5.sp) }
            )
            SuggestionChip(
                onClick = onLoadSample,
                label = { Text("Standard CSV", fontSize = 11.5.sp) }
            )
            if (rawText.isNotBlank()) {
                SuggestionChip(
                    onClick = { onTextChanged("") },
                    label = { Text("Clear", fontSize = 11.5.sp) }
                )
            }
        }

        OutlinedTextField(
            value = rawText,
            onValueChange = onTextChanged,
            placeholder = {
                Text(
                    "Paste course list or portal table here...\n\nExample:\n1619304040 计算机组成原理 09:50-12:15 明理楼B105\nCS 101 Intro to CS MWF 09:00 - 10:15 Room 102",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            minLines = 6,
            maxLines = 10,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("paste_text_input")
        )
    }
}

@Composable
fun FileUploadView(
    onPickFile: () -> Unit,
    onLoadSampleCsv: () -> Unit,
    onLoadMamunSchedule: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    AppIcons.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            "Upload Schedule File",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            "Supports Excel (.xlsx, .xls), PDF timetables, CSV, TSV, and plain text files exported from school portals.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickFile,
                modifier = Modifier.weight(1.2f).testTag("select_csv_file_button")
            ) {
                Icon(AppIcons.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select File", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onLoadMamunSchedule,
                modifier = Modifier.weight(1.3f)
            ) {
                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Load Fall 2026", fontSize = 12.sp)
            }
        }

        TextButton(onClick = onLoadSampleCsv) {
            Text("Or load sample CSV template", fontSize = 12.sp)
        }
    }
}

@Composable
fun CameraScanView(
    capturedBitmap: Bitmap?,
    isProcessing: Boolean,
    onCapturePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (capturedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = capturedBitmap.asImageBitmap(),
                    contentDescription = "Captured Schedule Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        "Image Ready",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRescan,
                    enabled = !isProcessing,
                    modifier = Modifier.testTag("scan_image_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanning...")
                    } else {
                        Icon(AppIcons.Camera, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan with AI Vision")
                    }
                }

                OutlinedButton(onClick = onCapturePhoto) {
                    Text("Retake")
                }
            }
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        AppIcons.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                "Scan Paper or Whiteboard Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                "Take a clear photo of your paper syllabus, university portal printout, or whiteboard timetable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCapturePhoto,
                    modifier = Modifier.testTag("take_photo_button")
                ) {
                    Icon(AppIcons.Camera, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo")
                }

                OutlinedButton(
                    onClick = onPickGallery,
                    modifier = Modifier.testTag("pick_gallery_button")
                ) {
                    Icon(AppIcons.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Image")
                }
            }
        }
    }
}

@Composable
fun PresetsView(
    onSelectPreset: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Ready-to-Use Major Presets",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        ScheduleParser.PRESET_SCHEDULES.keys.forEach { presetName ->
            val isMamun = presetName.contains("Mamun")
            val isBilingual = presetName.contains("Bilingual")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMamun) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = if (isMamun) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPreset(presetName) }
                    .testTag("preset_$presetName")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                presetName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (isMamun && isBilingual) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "RECOMMENDED",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            if (isMamun) "Full Fall 2026 timetable with courses, rooms, and professors"
                            else "Tap to populate & review timetable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (isMamun) Icons.Default.CheckCircle else AppIcons.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ResolutionChoiceChip(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ParsedCourseCard(
    course: ImportedCourse,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val courseColor = try {
        Color(android.graphics.Color.parseColor(course.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (course.isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            if (course.hasConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = course.isSelected,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(courseColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = course.code.ifEmpty { "CLASS" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = courseColor
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = course.dayShortName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "${course.startTime} - ${course.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            AppIcons.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            AppIcons.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Conflict Warning banner if overlapping
            if (course.hasConflict) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            AppIcons.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = course.conflictDescription ?: "Time overlap detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Location & Instructor details row
            if (course.classroom.isNotBlank() || course.instructor.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.padding(start = 42.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (course.classroom.isNotBlank()) {
                        Text(
                            "📍 ${course.classroom}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    if (course.instructor.isNotBlank()) {
                        Text(
                            "👤 ${course.instructor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
