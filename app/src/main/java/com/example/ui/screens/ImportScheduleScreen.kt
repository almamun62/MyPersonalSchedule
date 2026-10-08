package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WeekRule
import com.example.domain.CourseConflictDetector
import com.example.domain.model.Course
import com.example.domain.model.ImportedCourse
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ImportViewModel
import com.example.ui.viewmodel.ScheduleViewModel

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScheduleScreen(
    importViewModel: ImportViewModel,
    scheduleViewModel: ScheduleViewModel,
    onFinishImport: () -> Unit
) {
    val context = LocalContext.current
    val parsedCourses by importViewModel.parsedCourses.collectAsStateWithLifecycle()
    val existingCourses by scheduleViewModel.filteredCourses.collectAsStateWithLifecycle()
    val statusMessage by importViewModel.statusMessage.collectAsStateWithLifecycle()
    val isWarning by importViewModel.isWarning.collectAsStateWithLifecycle()
    val replaceExisting by importViewModel.replaceExisting.collectAsStateWithLifecycle()

    var showManualAddDialog by remember { mutableStateOf(false) }
    var editingCourseIndex by remember { mutableStateOf<Int?>(null) }
    var showPasteTextSection by remember { mutableStateOf(false) }
    var pasteTextContent by remember { mutableStateOf("") }

    // Upload launcher for CSV, XLSX, and PDF
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            importViewModel.parseFileFromUri(it)
        }
    }

    // Downloadable CSV template saver
    val csvTemplateSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outStream ->
                    outStream.write(importViewModel.getSampleCsvTemplate().toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "CSV template downloaded successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to download template: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Check conflicts for each course
    fun checkConflict(course: ImportedCourse): String? {
        val matchExisting = existingCourses.find { existing ->
            existing.dayOfWeek == course.dayOfWeek &&
                    CourseConflictDetector.doWeekRulesOverlap(existing.weekRule, course.weekRule) &&
                    (course.startPeriod <= existing.endPeriod && existing.startPeriod <= course.endPeriod)
        }
        if (matchExisting != null) {
            return "Conflicts with existing: ${matchExisting.name} (Period ${matchExisting.startPeriod}-${matchExisting.endPeriod})"
        }

        val matchOtherParsed = parsedCourses.find { other ->
            other != course &&
                    other.isSelected &&
                    other.dayOfWeek == course.dayOfWeek &&
                    CourseConflictDetector.doWeekRulesOverlap(other.weekRule, course.weekRule) &&
                    (course.startPeriod <= other.endPeriod && other.startPeriod <= course.endPeriod)
        }
        if (matchOtherParsed != null) {
            return "Overlaps with parsed course: ${matchOtherParsed.name}"
        }

        return null
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Course Import".tr, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onFinishImport) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Offline Security Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% Offline — All files parsed locally on your device without network calls.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Status / Error Message
            statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isWarning) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                    border = BorderStroke(1.dp, if (isWarning) Color(0xFFF59E0B) else Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (isWarning) Icons.Outlined.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isWarning) Color(0xFFD97706) else Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isWarning) Color(0xFF92400E) else Color(0xFF065F46)
                            )
                        }
                        IconButton(
                            onClick = { importViewModel.clearStatusMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // 3. Two Paths Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose Import Path",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Path A: Upload File
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "text/comma-separated-values",
                                        "text/csv",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "application/vnd.ms-excel",
                                        "application/pdf",
                                        "text/plain"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { csvTemplateSaver.launch("schedule_template.csv") },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV Template", fontSize = 11.sp)
                        }
                    }

                    // Format badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("📄 CSV", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("📊 Excel (XLSX)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("📕 PDF (Text)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Path B: Manual Add
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showManualAddDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manual Add Class", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { showPasteTextSection = !showPasteTextSection }
                        ) {
                            Text(if (showPasteTextSection) "Hide Paste" else "Paste Text", fontSize = 11.sp)
                        }
                    }

                    // Collapsible text paste input
                    AnimatedVisibility(visible = showPasteTextSection) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = pasteTextContent,
                                onValueChange = { pasteTextContent = it },
                                placeholder = { Text("Paste CSV, timetable table, or syllabus text here...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        if (pasteTextContent.isNotBlank()) {
                                            if (pasteTextContent.contains(",")) {
                                                importViewModel.parseCsvContent(pasteTextContent)
                                            } else {
                                                importViewModel.parseFreeText(pasteTextContent)
                                            }
                                        }
                                    },
                                    enabled = pasteTextContent.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Parse Pasted Text", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Editable Preview List (ALWAYS shown before saving)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Courses Preview (${parsedCourses.count { it.isSelected }}/${parsedCourses.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                if (parsedCourses.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                val allSel = parsedCourses.all { it.isSelected }
                                importViewModel.selectAll(!allSel)
                            }
                        ) {
                            Text(if (parsedCourses.all { it.isSelected }) "Deselect All" else "Select All", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { importViewModel.clearParsedCourses() }
                        ) {
                            Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (parsedCourses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FormatListBulleted,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No courses added yet",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Upload a CSV/XLSX/PDF file or tap 'Manual Add Class' above.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(parsedCourses) { index, course ->
                        val conflictReason = remember(course, existingCourses, parsedCourses) {
                            checkConflict(course)
                        }
                        val hasMissingName = course.name.isBlank()
                        val hasMissingDay = course.dayOfWeek !in 1..7
                        val hasInvalidPeriod = course.startPeriod <= 0 || course.startPeriod > course.endPeriod

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (conflictReason != null || hasMissingName || hasMissingDay || hasInvalidPeriod) {
                                    Color(0xFFFFFBEB)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (conflictReason != null || hasMissingName || hasMissingDay || hasInvalidPeriod) {
                                    Color(0xFFF59E0B)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
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
                                        onCheckedChange = { importViewModel.toggleCourseSelection(index) }
                                    )

                                    // Color badge
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                try {
                                                    Color(android.graphics.Color.parseColor(course.colorHex))
                                                } catch (e: Exception) {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = course.name.ifBlank { "⚠️ [Unnamed Course]" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val dayLabel = DAY_NAMES.getOrNull(course.dayOfWeek - 1) ?: "Day ${course.dayOfWeek}"
                                        val periodStr = "Sec ${course.startPeriod}-${course.endPeriod}"
                                        val roomStr = if (course.classroom.isNotBlank()) " • ${course.classroom}" else ""
                                        val ruleStr = when (course.weekRule) {
                                            WeekRule.ODD -> " • Odd"
                                            WeekRule.EVEN -> " • Even"
                                            WeekRule.ALL -> ""
                                            else -> ""
                                        }

                                        Text(
                                            text = "$dayLabel $periodStr$roomStr$ruleStr",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Edit Row Button
                                    IconButton(
                                        onClick = { editingCourseIndex = index },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                    }

                                    // Delete Row Button
                                    IconButton(
                                        onClick = { importViewModel.removeParsedCourse(course) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Validation warnings
                                if (hasMissingName) {
                                    Text(
                                        text = "⚠️ Missing Course Name (tap edit to fix)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                                    )
                                }
                                if (hasMissingDay) {
                                    Text(
                                        text = "⚠️ Missing / Invalid Day of Week",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                                    )
                                }
                                if (hasInvalidPeriod) {
                                    Text(
                                        text = "⚠️ Invalid Period timing (start > end)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                                    )
                                }
                                if (conflictReason != null) {
                                    Text(
                                        text = "⚠️ $conflictReason",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(start = 36.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Save & Commit Controls
            if (parsedCourses.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Save Mode:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = replaceExisting,
                                    onClick = { importViewModel.setReplaceExisting(true) },
                                    label = { Text("Replace All", fontSize = 11.sp) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = !replaceExisting,
                                    onClick = { importViewModel.setReplaceExisting(false) },
                                    label = { Text("Merge", fontSize = 11.sp) }
                                )
                            }
                        }

                        val selectedCourses = parsedCourses.filter { it.isSelected }
                        Button(
                            onClick = {
                                val toSave = selectedCourses.map { it.toCourse() }
                                scheduleViewModel.importCourses(toSave, replaceExisting = replaceExisting)
                                Toast.makeText(context, "Saved ${toSave.size} courses to timetable!", Toast.LENGTH_SHORT).show()
                                onFinishImport()
                            },
                            enabled = selectedCourses.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save ${selectedCourses.size} Courses to Timetable",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Manual Add Dialog (simplified AddEditCourseDialog)
    if (showManualAddDialog) {
        AddEditCourseDialog(
            existingCourses = existingCourses,
            onDismiss = { showManualAddDialog = false },
            onSave = { newCourse ->
                val imported = ImportedCourse(
                    name = newCourse.name,
                    code = newCourse.code,
                    instructor = newCourse.instructor,
                    classroom = newCourse.classroom,
                    dayOfWeek = newCourse.dayOfWeek,
                    startPeriod = newCourse.startPeriod,
                    endPeriod = newCourse.endPeriod,
                    startTime = newCourse.startTime,
                    endTime = newCourse.endTime,
                    colorHex = newCourse.colorHex,
                    credits = newCourse.credits,
                    isRetake = newCourse.isRetake,
                    weekRule = newCourse.weekRule,
                    isSelected = true
                )
                importViewModel.addManualCourse(imported)
                showManualAddDialog = false
            }
        )
    }

    // Row Edit Dialog
    editingCourseIndex?.let { idx ->
        if (idx in parsedCourses.indices) {
            val ic = parsedCourses[idx]
            val courseModel = ic.toCourse()
            AddEditCourseDialog(
                initialCourse = courseModel,
                existingCourses = existingCourses,
                onDismiss = { editingCourseIndex = null },
                onSave = { updatedCourse ->
                    val updatedImported = ic.copy(
                        name = updatedCourse.name,
                        code = updatedCourse.code,
                        instructor = updatedCourse.instructor,
                        classroom = updatedCourse.classroom,
                        dayOfWeek = updatedCourse.dayOfWeek,
                        startPeriod = updatedCourse.startPeriod,
                        endPeriod = updatedCourse.endPeriod,
                        startTime = updatedCourse.startTime,
                        endTime = updatedCourse.endTime,
                        colorHex = updatedCourse.colorHex,
                        credits = updatedCourse.credits,
                        isRetake = updatedCourse.isRetake,
                        weekRule = updatedCourse.weekRule
                    )
                    importViewModel.updateParsedCourse(idx, updatedImported)
                    editingCourseIndex = null
                }
            )
        }
    }
}
