package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import com.example.ui.theme.tr
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserPreferencesManager
import com.example.data.model.CourseEntity
import com.example.domain.CourseConflict
import com.example.domain.CourseConflictDetector
import com.example.domain.ScheduleTranslationEngine
import com.example.util.PdfScheduleHelper
import com.example.util.ScheduleImportHelper
import com.example.util.ScheduleOcrHelper
import com.example.ui.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch

@Composable
fun SpreadsheetImportDialog(
    onDismiss: () -> Unit,
    viewModel: ScheduleViewModel
) {
    val activeSemId = viewModel.uiState.collectAsState().value.activeSemester?.id ?: 1L
    SpreadsheetImportDialog(
        semesterId = activeSemId,
        onDismiss = onDismiss,
        onImport = { courses ->
            courses.forEach { viewModel.addCourse(it) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetImportDialog(
    semesterId: Long,
    onDismiss: () -> Unit,
    onImport: (List<CourseEntity>) -> Unit
) {
    SpreadsheetImportDialog(
        semesterId = semesterId,
        onDismiss = onDismiss,
        onImportWithMode = { courses, _ -> onImport(courses) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetImportDialog(
    semesterId: Long,
    onDismiss: () -> Unit,
    onImportWithMode: (List<CourseEntity>, Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val userPrefs = remember { UserPreferencesManager(context) }
    val savedApiKey by userPrefs.geminiApiKey.collectAsState(initial = "")
    val effectiveApiKey = (savedApiKey ?: "").ifBlank { com.example.BuildConfig.GEMINI_API_KEY ?: "" }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Excel/PDF/CSV", "Photo OCR", "Paste / Edit", "DIY Formats", "Preset")

    // State
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var rawParsedCourses by remember { mutableStateOf<List<CourseEntity>>(emptyList()) }
    var autoTranslateToEnglish by remember { mutableStateOf(true) }
    var translationMode by remember { mutableStateOf(ScheduleTranslationEngine.TranslationMode.ENGLISH_ONLY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rowCount by remember { mutableStateOf(0) }
    var isFileLoading by remember { mutableStateOf(false) }
    var replaceExisting by remember { mutableStateOf(true) }

    // OCR / Image State
    var ocrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }
    var ocrStatusMessage by remember { mutableStateOf<String?>(null) }
    var showApiKeyInput by remember { mutableStateOf(false) }
    var apiKeyInputText by remember { mutableStateOf("") }
    var lastSelectedPdfUri by remember { mutableStateOf<Uri?>(null) }

    // Paste text state
    var rawCsvText by remember { mutableStateOf("") }

    // Dynamic translation based on user's selected mode
    val parsedCourses = remember(rawParsedCourses, autoTranslateToEnglish, translationMode) {
        if (autoTranslateToEnglish) {
            ScheduleTranslationEngine.translateCourses(rawParsedCourses, translationMode).translatedCourses
        } else {
            rawParsedCourses
        }
    }

    val translationStats = remember(rawParsedCourses, autoTranslateToEnglish, translationMode) {
        if (autoTranslateToEnglish) {
            ScheduleTranslationEngine.translateCourses(rawParsedCourses, translationMode)
        } else {
            null
        }
    }

    val hasChineseContent = remember(rawParsedCourses) {
        rawParsedCourses.any {
            ScheduleTranslationEngine.containsChinese(it.name) ||
            ScheduleTranslationEngine.containsChinese(it.classroom) ||
            ScheduleTranslationEngine.containsChinese(it.instructor)
        }
    }

    // Calculate conflicts dynamically whenever parsedCourses changes
    val conflicts = remember(parsedCourses) {
        CourseConflictDetector.detectConflicts(parsedCourses, totalWeeks = 18)
    }

    val conflictingCourseNames = remember(conflicts) {
        conflicts.flatMap { listOf(it.course1.name, it.course2.name) }.toSet()
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                ocrBitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                ocrStatusMessage = "Photo selected. Tap 'Scan Timetable Image' to analyze with AI Vision."
                errorMessage = null
            } catch (e: Exception) {
                ocrStatusMessage = "Failed to load image: ${e.localizedMessage}"
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            ocrBitmap = bitmap
            ocrStatusMessage = "Photo captured. Tap 'Scan Timetable Image' to analyze with AI Vision."
            errorMessage = null
        }
    }

    // Launcher for file picker (.xlsx, .pdf, .csv, .tsv, .txt)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isFileLoading = true
            try {
                val result = ScheduleImportHelper.parseUri(context, uri, semesterId, autoTranslateToEnglish = false)
                selectedFileName = result.fileName
                rawParsedCourses = result.courses
                rowCount = result.rowCount
                errorMessage = result.error
                if (result.fileName.endsWith(".pdf", ignoreCase = true)) {
                    lastSelectedPdfUri = uri
                }
            } catch (e: Exception) {
                errorMessage = "Error reading file: ${e.localizedMessage}"
            } finally {
                isFileLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(6.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0x66FFFFFF),
                            Color(0x33337DFF),
                            Color(0x11FFFFFF)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                ),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // 1. Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Import Schedule".tr,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Excel, CSV, or University Timetable format".tr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close".tr)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Tabs: Excel/PDF/CSV vs Photo OCR vs Paste/Edit vs DIY Formats vs Preset
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    edgePadding = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                when (index) {
                                    2 -> {
                                        if (rawCsvText.isBlank()) {
                                            rawCsvText = ScheduleImportHelper.MAMUN_SCHEDULE_RAW_TEXT
                                            rawParsedCourses = ScheduleImportHelper.parseTextToCourses(rawCsvText, semesterId, false)
                                        }
                                    }
                                    4 -> {
                                        // Load Fall 2026 Mamun preset directly
                                        val scheduled = ScheduleImportHelper.getMamunFall2026Schedule(semesterId, false)
                                        val unscheduled = ScheduleImportHelper.getMamunUnscheduledCourses(semesterId, false)
                                        rawParsedCourses = scheduled + unscheduled
                                        selectedFileName = "Mamun Fall 2026 Schedule"
                                        errorMessage = null
                                    }
                                }
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (index) {
                                            0 -> Icons.Default.CloudUpload
                                            1 -> Icons.Default.CameraAlt
                                            2 -> Icons.Default.EditNote
                                            3 -> Icons.Default.FormatListNumbered
                                            else -> Icons.Default.School
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        title.tr,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Tab Content Area
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> {
                            // TAB 0: UPLOAD FILE (Excel, PDF, CSV, TXT)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // File selection glass card
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = 1.5.dp,
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            fileLauncher.launch(
                                                arrayOf(
                                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                    "application/vnd.ms-excel",
                                                    "application/pdf",
                                                    "text/csv",
                                                    "text/comma-separated-values",
                                                    "text/tab-separated-values",
                                                    "text/plain",
                                                    "*/*"
                                                )
                                            )
                                        },
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (isFileLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Parsing timetable...".tr, style = MaterialTheme.typography.bodyMedium)
                                        } else if (selectedFileName != null && parsedCourses.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = selectedFileName ?: "",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${parsedCourses.size} courses detected • Tap to change file",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.UploadFile,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(38.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Tap to Select Excel, PDF, CSV, or Text File".tr,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Supports .xlsx, .pdf, .csv, matrix schedules and exported university files".tr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // PDF-Specific Assist Card
                                if (selectedFileName?.endsWith(".pdf", ignoreCase = true) == true) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureAsPdf,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "PDF Schedule Parsing",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            if (rawParsedCourses.isNotEmpty()) {
                                                Text(
                                                    text = "Extracted ${rawParsedCourses.size} courses from embedded PDF text.",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            } else {
                                                Text(
                                                    text = "No selectable text found in this PDF (it appears to be a scanned timetable). Tap below to extract using Gemini Vision AI!",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (lastSelectedPdfUri != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            isOcrRunning = true
                                                            ocrStatusMessage = "Rendering PDF page to image..."
                                                            selectedTabIndex = 1 // Switch to OCR tab
                                                            val bitmap = PdfScheduleHelper.renderPdfFirstPage(context, lastSelectedPdfUri!!)
                                                            if (bitmap != null) {
                                                                ocrBitmap = bitmap
                                                                ocrStatusMessage = "Analyzing PDF page image with Gemini Vision..."
                                                                val ocrResult = ScheduleOcrHelper.recognizeScheduleFromBitmap(
                                                                    bitmap = bitmap,
                                                                    apiKey = effectiveApiKey,
                                                                    semesterId = semesterId
                                                                )
                                                                if (ocrResult.success && ocrResult.courses.isNotEmpty()) {
                                                                    rawParsedCourses = ocrResult.courses
                                                                    selectedFileName = "PDF Vision OCR (${ocrResult.courses.size} courses)"
                                                                    ocrStatusMessage = "Recognized ${ocrResult.courses.size} courses from PDF!"
                                                                } else {
                                                                    ocrStatusMessage = ocrResult.errorMessage ?: "Could not extract timetable courses from PDF image."
                                                                }
                                                            } else {
                                                                ocrStatusMessage = "Failed to render PDF page into image."
                                                            }
                                                            isOcrRunning = false
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Scan PDF with Gemini Vision AI")
                                                }
                                            }
                                        }
                                    }
                                }

                                // Error banner if any
                                if (errorMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = errorMessage ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                // Quick Presets and Templates Bar
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Quick Actions & Templates".tr,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val scheduled = ScheduleImportHelper.getMamunFall2026Schedule(semesterId, false)
                                                    val unscheduled = ScheduleImportHelper.getMamunUnscheduledCourses(semesterId, false)
                                                    rawParsedCourses = scheduled + unscheduled
                                                    selectedFileName = "Mamun Fall 2026 Schedule"
                                                    errorMessage = null
                                                },
                                                modifier = Modifier.weight(1.2f),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Load My Schedule".tr, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    ScheduleImportHelper.shareCsvTemplate(context)
                                                },
                                                modifier = Modifier.weight(0.9f),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("CSV Template".tr, fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    rawParsedCourses = ScheduleImportHelper.parseTextToCourses(
                                                        ScheduleImportHelper.SAMPLE_CSV_TEMPLATE,
                                                        semesterId,
                                                        false
                                                    )
                                                    selectedFileName = "Standard University Sample.csv"
                                                    errorMessage = null
                                                },
                                                modifier = Modifier.weight(0.8f),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Text("Try Sample".tr, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                // Conflict Warning Card & Preview Area
                                ConflictAndPreviewSection(
                                    parsedCourses = parsedCourses,
                                    conflicts = conflicts,
                                    conflictingCourseNames = conflictingCourseNames,
                                    autoTranslateToEnglish = autoTranslateToEnglish,
                                    onToggleAutoTranslate = { autoTranslateToEnglish = it },
                                    translationMode = translationMode,
                                    onSelectTranslationMode = { translationMode = it },
                                    translationStats = translationStats,
                                    hasChineseContent = hasChineseContent
                                )
                            }
                        }

                        1 -> {
                            // TAB 1: PHOTO OCR (Camera / Gallery -> Gemini Vision AI)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Photo OCR Header Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Photo OCR Schedule Scanner",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Snap a photo of your classroom schedule, paper timetable, or school portal screenshot. Gemini Vision AI will automatically detect courses, days, periods, and rooms.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Action buttons: Pick from Gallery or Camera
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select Photo", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            cameraLauncher.launch(null)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Take Photo", fontSize = 12.sp)
                                    }
                                }

                                // API Key config bar
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (effectiveApiKey.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Key,
                                                    contentDescription = null,
                                                    tint = if (effectiveApiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = if (effectiveApiKey.isNotBlank()) "Vision AI: Ready (Gemini)" else "Gemini API Key Required",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            TextButton(
                                                onClick = { showApiKeyInput = !showApiKeyInput },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(if (showApiKeyInput) "Close" else "Change Key", fontSize = 11.sp)
                                            }
                                        }

                                        if (showApiKeyInput || effectiveApiKey.isBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = apiKeyInputText,
                                                    onValueChange = { apiKeyInputText = it },
                                                    placeholder = { Text("Paste Gemini API Key", fontSize = 11.sp) },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                                )
                                                Button(
                                                    onClick = {
                                                        if (apiKeyInputText.isNotBlank()) {
                                                            coroutineScope.launch {
                                                                userPrefs.setGeminiApiKey(apiKeyInputText.trim())
                                                                showApiKeyInput = false
                                                                Toast.makeText(context, "API Key Saved", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Save", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Image Preview & AI Trigger
                                if (ocrBitmap != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Image(
                                                bitmap = ocrBitmap!!.asImageBitmap(),
                                                contentDescription = "Selected schedule image",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isOcrRunning = true
                                                        ocrStatusMessage = "Analyzing image with Gemini Vision AI..."
                                                        try {
                                                            val ocrResult = ScheduleOcrHelper.recognizeScheduleFromBitmap(
                                                                bitmap = ocrBitmap!!,
                                                                apiKey = effectiveApiKey,
                                                                semesterId = semesterId
                                                            )
                                                            if (ocrResult.success && ocrResult.courses.isNotEmpty()) {
                                                                rawParsedCourses = ocrResult.courses
                                                                selectedFileName = "AI Photo OCR (${ocrResult.courses.size} courses)"
                                                                ocrStatusMessage = "Successfully recognized ${ocrResult.courses.size} courses from image!"
                                                            } else {
                                                                ocrStatusMessage = ocrResult.errorMessage ?: "Could not identify timetable courses in this image. Please ensure text is legible, or try Paste / DIY tab."
                                                            }
                                                        } catch (e: Exception) {
                                                            ocrStatusMessage = "OCR error: ${e.localizedMessage}"
                                                        } finally {
                                                            isOcrRunning = false
                                                        }
                                                    }
                                                },
                                                enabled = !isOcrRunning && effectiveApiKey.isNotBlank(),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                if (isOcrRunning) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Analyzing Schedule...", fontSize = 12.sp)
                                                } else {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Scan Timetable Image with AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Status message
                                if (ocrStatusMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = ocrStatusMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                // Conflict Warning Card & Preview Area
                                ConflictAndPreviewSection(
                                    parsedCourses = parsedCourses,
                                    conflicts = conflicts,
                                    conflictingCourseNames = conflictingCourseNames,
                                    autoTranslateToEnglish = autoTranslateToEnglish,
                                    onToggleAutoTranslate = { autoTranslateToEnglish = it },
                                    translationMode = translationMode,
                                    onSelectTranslationMode = { translationMode = it },
                                    translationStats = translationStats,
                                    hasChineseContent = hasChineseContent
                                )
                            }
                        }

                        2 -> {
                            // TAB 2: PASTE TEXT / IN-APP EDITOR
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Action Toolbar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            rawCsvText = ScheduleImportHelper.MAMUN_SCHEDULE_RAW_TEXT
                                            rawParsedCourses = ScheduleImportHelper.parseTextToCourses(rawCsvText, semesterId, false)
                                            selectedFileName = "Mamun Fall 2026 Timetable Text"
                                            errorMessage = null
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste My Schedule".tr, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val clipText = clipboardManager.getText()?.text ?: ""
                                            if (clipText.isNotBlank()) {
                                                rawCsvText = clipText
                                                rawParsedCourses = ScheduleImportHelper.parseTextToCourses(rawCsvText, semesterId, false)
                                                selectedFileName = "Pasted from Clipboard"
                                                errorMessage = if (rawParsedCourses.isEmpty()) "No courses recognized in text" else null
                                            } else {
                                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste".tr, fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            rawCsvText = ScheduleImportHelper.SAMPLE_CSV_TEMPLATE
                                            rawParsedCourses = ScheduleImportHelper.parseTextToCourses(rawCsvText, semesterId, false)
                                            selectedFileName = "Sample CSV Template"
                                            errorMessage = null
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Sample CSV".tr, fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            rawCsvText = ""
                                            rawParsedCourses = emptyList()
                                            selectedFileName = null
                                            errorMessage = null
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Clear".tr, fontSize = 11.sp)
                                    }
                                }

                                // Text Area
                                OutlinedTextField(
                                    value = rawCsvText,
                                    onValueChange = {
                                        rawCsvText = it
                                        rawParsedCourses = ScheduleImportHelper.parseTextToCourses(rawCsvText, semesterId, false)
                                        errorMessage = if (rawCsvText.isNotBlank() && rawParsedCourses.isEmpty()) {
                                            "Could not parse courses from text. Check format."
                                        } else null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.45f),
                                    placeholder = {
                                        Text("Paste schedule text e.g.:\n1619304040-计算机组成原理(国际学生)\n1-10周,星期1,第3节-第5节明理楼B105\n\nOr CSV lines.".tr)
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Conflict Warning Card & Preview Area
                                ConflictAndPreviewSection(
                                    parsedCourses = parsedCourses,
                                    conflicts = conflicts,
                                    conflictingCourseNames = conflictingCourseNames,
                                    autoTranslateToEnglish = autoTranslateToEnglish,
                                    onToggleAutoTranslate = { autoTranslateToEnglish = it },
                                    translationMode = translationMode,
                                    onSelectTranslationMode = { translationMode = it },
                                    translationStats = translationStats,
                                    hasChineseContent = hasChineseContent
                                )
                            }
                        }

                        3 -> {
                            // TAB 3: DIY FORMATS & TEMPLATE MAKER
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DesignServices,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Create Your Own Timetable",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Build your schedule file in Excel, Google Sheets, or Notepad, then import it here. All formats are automatically converted into your semester schedule.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Action Buttons: Share & Copy Templates
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Template Quick Actions",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { ScheduleImportHelper.shareCsvTemplate(context) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Share CSV File", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    ScheduleImportHelper.copyTemplateToClipboard(context)
                                                    Toast.makeText(context, "CSV template copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy CSV", fontSize = 11.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    ScheduleImportHelper.copyTsvToClipboard(context)
                                                    Toast.makeText(context, "Excel/Sheets TSV copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy TSV (Excel)", fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    rawCsvText = ScheduleImportHelper.SAMPLE_CSV_TEMPLATE
                                                    rawParsedCourses = ScheduleImportHelper.parseTextToCourses(rawCsvText, semesterId, false)
                                                    selectedFileName = "Custom DIY Schedule"
                                                    selectedTabIndex = 2 // Switch to Paste / Edit tab!
                                                    Toast.makeText(context, "Template loaded into Editor!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary
                                                )
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Customize in App", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                // Format Reference Guide
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                                        item {
                                            Text(
                                                text = "Standard CSV Header & Columns",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "1. Course Name (Required)\n2. Course Code\n3. Classroom (e.g. B105)\n4. Teacher / Instructor\n5. Day of Week (1=Mon, 2=Tue, ..., 7=Sun)\n6. Start Period (1 to 12)\n7. End Period (1 to 12)\n8. Start Time (HH:mm, e.g. 08:00)\n9. End Time (HH:mm, e.g. 09:35)\n10. Week Rule (ALL / ODD / EVEN / CUSTOM)\n11. Custom Weeks (e.g. 1-16 or 1-8,10-18)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 18.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Plain Text Shorthand Syntax",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Course Name\n1-16周,星期1,第1节-第2节明理楼B105\n\nOr in English:\nData Structures\nWeeks 1-16, Monday, Periods 3-4, Room A302",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            // TAB 4: MAMUN'S FALL 2026 PRESET
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Fall 2026 Class Schedule (Mamun)".tr,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = ScheduleImportHelper.MAMUN_SCHEDULE_INFO,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "8 Courses • 15 Weekly Class Blocks • Includes Practicum & Contest".tr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                ConflictAndPreviewSection(
                                    parsedCourses = parsedCourses,
                                    conflicts = conflicts,
                                    conflictingCourseNames = conflictingCourseNames,
                                    autoTranslateToEnglish = autoTranslateToEnglish,
                                    onToggleAutoTranslate = { autoTranslateToEnglish = it },
                                    translationMode = translationMode,
                                    onSelectTranslationMode = { translationMode = it },
                                    translationStats = translationStats,
                                    hasChineseContent = hasChineseContent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Replacement Mode & Import Actions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceExisting = !replaceExisting }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Replace Current Courses".tr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (replaceExisting) "Clear old schedule before adding" else "Keep old courses and append",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = replaceExisting,
                            onCheckedChange = { replaceExisting = it },
                            modifier = Modifier.testTag("toggle_replace_courses")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel".tr)
                    }

                    Button(
                        onClick = {
                            if (parsedCourses.isNotEmpty()) {
                                onImportWithMode(parsedCourses, replaceExisting)
                            }
                        },
                        enabled = parsedCourses.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_import_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (parsedCourses.isNotEmpty()) {
                                if (replaceExisting) "Replace (${parsedCourses.size} Courses)" else "Import (${parsedCourses.size} Courses)"
                            } else {
                                "No Courses"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictAndPreviewSection(
    parsedCourses: List<CourseEntity>,
    conflicts: List<CourseConflict>,
    conflictingCourseNames: Set<String>,
    autoTranslateToEnglish: Boolean,
    onToggleAutoTranslate: (Boolean) -> Unit,
    translationMode: ScheduleTranslationEngine.TranslationMode,
    onSelectTranslationMode: (ScheduleTranslationEngine.TranslationMode) -> Unit,
    translationStats: ScheduleTranslationEngine.TranslationResult?,
    hasChineseContent: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Translation Card for International Students
        if (hasChineseContent || parsedCourses.isNotEmpty()) {
            InternationalStudentTranslationCard(
                autoTranslateToEnglish = autoTranslateToEnglish,
                onToggleAutoTranslate = onToggleAutoTranslate,
                translationMode = translationMode,
                onSelectTranslationMode = onSelectTranslationMode,
                translationStats = translationStats,
                hasChineseContent = hasChineseContent
            )
        }

        // Conflict Warning Card if any conflicts found
        if (conflicts.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x22FF9500),
                border = BorderStroke(1.2.dp, Color(0x99FF9500)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Conflict".tr,
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Course Conflict Detected (${conflicts.size} overlapping slot)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9500)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    conflicts.forEach { conflict ->
                        Text(
                            text = "• ${conflict.dayName} Sec ${conflict.overlapStartPeriod}-${conflict.overlapEndPeriod} (${conflict.overlapStartTime}-${conflict.overlapEndTime}):\n  ${conflict.course1.name} & ${conflict.course2.name}\n  Overlapping: ${conflict.formattedWeeks}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Preview Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preview (${parsedCourses.size} courses)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            if (conflicts.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x33FF9500)
                ) {
                    Text(
                        text = "${conflicts.size} Conflicts Flagged",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9500),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (parsedCourses.isNotEmpty()) {
                Text(
                    text = "All Clear ✓".tr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (parsedCourses.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(parsedCourses) { course ->
                    val isConflicted = conflictingCourseNames.contains(course.name)
                    CoursePreviewItem(
                        course = course,
                        isConflicted = isConflicted
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No courses loaded yet. Select a file or preset above.".tr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InternationalStudentTranslationCard(
    autoTranslateToEnglish: Boolean,
    onToggleAutoTranslate: (Boolean) -> Unit,
    translationMode: ScheduleTranslationEngine.TranslationMode,
    onSelectTranslationMode: (ScheduleTranslationEngine.TranslationMode) -> Unit,
    translationStats: ScheduleTranslationEngine.TranslationResult?,
    hasChineseContent: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (hasChineseContent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        border = BorderStroke(
            1.dp,
            if (hasChineseContent) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("international_translation_card")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Translation".tr,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Auto-Translate to English".tr,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "INTL STUDENT".tr,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (hasChineseContent) {
                                "Chinese timetable detected • Auto-translating to English"
                            } else {
                                "Translates courses, campus buildings & labs into English"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = autoTranslateToEnglish,
                    onCheckedChange = onToggleAutoTranslate,
                    modifier = Modifier.testTag("toggle_auto_translate")
                )
            }

            if (autoTranslateToEnglish) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = translationMode == ScheduleTranslationEngine.TranslationMode.ENGLISH_ONLY,
                        onClick = { onSelectTranslationMode(ScheduleTranslationEngine.TranslationMode.ENGLISH_ONLY) },
                        label = { Text("English Only".tr, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = if (translationMode == ScheduleTranslationEngine.TranslationMode.ENGLISH_ONLY) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        } else null,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("chip_english_only")
                    )

                    FilterChip(
                        selected = translationMode == ScheduleTranslationEngine.TranslationMode.BILINGUAL,
                        onClick = { onSelectTranslationMode(ScheduleTranslationEngine.TranslationMode.BILINGUAL) },
                        label = { Text("Bilingual (EN + 中文)".tr, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = if (translationMode == ScheduleTranslationEngine.TranslationMode.BILINGUAL) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        } else null,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("chip_bilingual")
                    )
                }

                if (translationStats != null && (translationStats.translatedCourseCount > 0 || translationStats.translatedClassroomCount > 0)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Translated ${translationStats.translatedCourseCount} courses & ${translationStats.translatedClassroomCount} room/lab locations",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoursePreviewItem(
    course: CourseEntity,
    isConflicted: Boolean = false
) {
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayLabel = if (course.dayOfWeek in 1..7) dayNames[course.dayOfWeek - 1] else "TBD"
    val isUnscheduled = course.dayOfWeek == 0 || course.startPeriod == 0

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isConflicted -> Color(0x20FF9500)
            isUnscheduled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        border = if (isConflicted) {
            BorderStroke(1.2.dp, Color(0x99FF9500))
        } else {
            BorderStroke(0.5.dp, Color(0x22FFFFFF))
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(course.colorHex))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isConflicted) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFF9500)
                            ) {
                                Text(
                                    text = "Conflict".tr,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (course.classroom.isNotBlank()) {
                            Text(
                                text = course.classroom,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (course.instructor.isNotBlank()) {
                            Text(
                                text = "• ${course.instructor}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (course.customWeeks.isNotBlank()) {
                            Text(
                                text = "• Weeks ${course.customWeeks}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isUnscheduled) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Unscheduled".tr,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isConflicted) Color(0x44FF9500) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "$dayLabel • Sec ${course.startPeriod}-${course.endPeriod}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConflicted) Color(0xFFFF9500) else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${course.startTime}-${course.endTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
