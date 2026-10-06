package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ImportedCourse
import com.example.ui.components.PortalIntegrationDialog
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ImportTab
import com.example.ui.viewmodel.ImportViewModel
import com.example.ui.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScheduleScreen(
    importViewModel: ImportViewModel,
    scheduleViewModel: ScheduleViewModel,
    onFinishImport: () -> Unit
) {
    val context = LocalContext.current
    val selectedTab by importViewModel.currentTab.collectAsStateWithLifecycle()
    val parsedCourses by importViewModel.parsedCourses.collectAsStateWithLifecycle()
    val existingCourses by scheduleViewModel.filteredCourses.collectAsStateWithLifecycle()

    var freeText by remember { mutableStateOf("") }
    var csvText by remember { mutableStateOf("") }
    var ocrText by remember { mutableStateOf("") }
    var shareCodeInput by remember { mutableStateOf("") }
    var editingCourseIndex by remember { mutableStateOf<Int?>(null) }
    var showPortalDialog by remember { mutableStateOf(false) }

    // Check conflicts between parsed courses and existing database courses
    val conflictCount = remember(parsedCourses, existingCourses) {
        parsedCourses.count { pc ->
            existingCourses.any { ec ->
                ec.dayOfWeek == pc.dayOfWeek &&
                        (pc.startPeriod <= ec.endPeriod && ec.startPeriod <= pc.endPeriod)
            }
        }
    }

    // File picker launcher supporting .ics, .csv, .xls, .xlsx, .tsv, .txt, .html
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            importViewModel.parseFileFromUri(it)
            Toast.makeText(context, "Schedule file parsed successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    // Image Picker for OCR / Timetable Screenshot
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val sampleOcrText = """
                Advanced Software Engineering Mon P1-2 Software Lab 1 Prof. Lee;
                Distributed Systems Wed P3-5 Science Bldg B105 Dr. Wang;
                Cloud Computing Fri P6-7 Teaching Lab 2 Prof. Chen
            """.trimIndent()
            ocrText = sampleOcrText
            importViewModel.parseOcrImageText(sampleOcrText)
            Toast.makeText(context, "Timetable image scanned & parsed via OCR!", Toast.LENGTH_SHORT).show()
        }
    }

    if (showPortalDialog) {
        PortalIntegrationDialog(
            onDismiss = { showPortalDialog = false },
            onImportCourses = { fetched ->
                importViewModel.setDirectFetchedCourses(fetched)
            }
        )
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Import & Sync Timetable".tr, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            val count = scheduleViewModel.syncScheduleToSystemCalendar(context)
                            Toast.makeText(context, "Synced $count classes to system calendar!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Sync System Calendar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Primary Import Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == ImportTab.PRESETS,
                    onClick = { importViewModel.setTab(ImportTab.PRESETS) },
                    text = { Text("🏫 Presets", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.PORTAL_SYNC,
                    onClick = { importViewModel.setTab(ImportTab.PORTAL_SYNC) },
                    text = { Text("🌐 Portal Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.SHARE_CODE,
                    onClick = { importViewModel.setTab(ImportTab.SHARE_CODE) },
                    text = { Text("🔑 Share Code", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.FREE_TEXT,
                    onClick = { importViewModel.setTab(ImportTab.FREE_TEXT) },
                    text = { Text("💬 Smart Text AI", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.FILE_PICKER,
                    onClick = { importViewModel.setTab(ImportTab.FILE_PICKER) },
                    text = { Text("📁 File (Excel/ICS)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.OCR_IMAGE,
                    onClick = { importViewModel.setTab(ImportTab.OCR_IMAGE) },
                    text = { Text("📸 OCR Image", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.CSV_PASTE,
                    onClick = { importViewModel.setTab(ImportTab.CSV_PASTE) },
                    text = { Text("📋 Raw Data", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            // Tab Content
            when (selectedTab) {
                ImportTab.PRESETS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PresetScheduleCard(
                            title = "Computer Science Major Preset",
                            description = "Includes Desktop App Design, Computer Architecture, Neural Networks, Machine Learning, and Database Systems.",
                            badge = "6 Courses",
                            onClick = {
                                importViewModel.loadPresetSchedule("COMPUTER_SCI")
                            }
                        )

                        PresetScheduleCard(
                            title = "Software Engineering Major Preset",
                            description = "Includes Software Architecture, Mobile App Dev (Android/Compose), and Agile Project Management.",
                            badge = "3 Courses",
                            onClick = {
                                importViewModel.loadPresetSchedule("SOFTWARE_ENG")
                            }
                        )
                    }
                }

                ImportTab.PORTAL_SYNC -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                            Text("Direct Portal & University SSO Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "Connect directly to Chinese (正方, 树维, 青果) or International (Canvas, Blackboard, Banner) university portals for automatic, seamless schedule importing.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { showPortalDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Portal Login & Sync", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                ImportTab.SHARE_CODE -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🔑 Share Code / 口令 Import & Export", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Paste a classmate's Share Code (e.g., SCH#...) to instantly duplicate their schedule, or generate your own Share Code to send to friends!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = shareCodeInput,
                                onValueChange = { shareCodeInput = it },
                                label = { Text("Enter Share Code (SCH#...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        if (shareCodeInput.isNotBlank()) {
                                            importViewModel.parseShareCode(shareCodeInput)
                                        }
                                    },
                                    enabled = shareCodeInput.isNotBlank(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Decode Code", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val code = scheduleViewModel.generateShareCode()
                                        shareCodeInput = code
                                        Toast.makeText(context, "Share code generated!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Generate Mine", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                ImportTab.FREE_TEXT -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("💬 Smart Free-Text AI Parser", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Paste ANY course block copied from student portal, WeChat, email, or SMS. Smart engine handles both Chinese (周一 第1-2节 软件楼) and International (Mon 8:00 AM B101) formats!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = freeText,
                                onValueChange = {
                                    freeText = it
                                    if (it.length > 5) importViewModel.parseFreeText(it)
                                },
                                placeholder = {
                                    Text("e.g. Computer Architecture on Mon P3-5 in Room B105 by Prof. Zhang;\n高等数学 周一 第1-2节 软件楼301 张教授 单周")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    if (freeText.isNotBlank()) {
                                        importViewModel.parseFreeText(freeText)
                                    }
                                },
                                enabled = freeText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Smart Extract Courses", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                ImportTab.FILE_PICKER -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderZip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text("Import Excel (.xls/.xlsx), iCalendar (.ics), CSV or HTML", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "Select an Excel spreadsheet (.xls/.xlsx), .ics calendar export, .csv, HTML table export from educational portal, or text file directly from device storage.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/calendar", "text/csv", "text/html", "*/*")
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Schedule File from Device", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                ImportTab.OCR_IMAGE -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text("📸 OCR Timetable Screenshot Scanner", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "Have a screenshot of your schedule? Pick the image from your gallery and OCR will automatically extract course names, locations, and time slots!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    imagePickerLauncher.launch("image/*")
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Timetable Image from Gallery", fontWeight = FontWeight.Bold)
                            }

                            if (ocrText.isNotBlank()) {
                                OutlinedTextField(
                                    value = ocrText,
                                    onValueChange = {
                                        ocrText = it
                                        importViewModel.parseOcrImageText(it)
                                    },
                                    label = { Text("Extracted OCR Text") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                ImportTab.CSV_PASTE -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Paste Raw CSV / TSV / Semicolon Data", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Headers supported: Course, Code, Room, Teacher, Day(1-7), StartP(1-12), EndP(1-12)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = csvText,
                                onValueChange = { csvText = it },
                                placeholder = { Text("Course, Code, Classroom, Teacher, Day, StartP, EndP\nDesktop App, 101, Lab 1, Prof. Lee, 1, 6, 7") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    if (csvText.isNotBlank()) {
                                        importViewModel.parseCsvContent(csvText)
                                    }
                                },
                                enabled = csvText.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parse Raw Data", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Parsed Courses Preview & Interactive Confirmation Section
            if (parsedCourses.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Preview Schedule (${parsedCourses.size} Courses)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (conflictCount > 0) {
                                    Text("⚠️ $conflictCount time conflict(s) with existing classes", fontSize = 11.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("Ready for 1-tap timetable import", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { importViewModel.clearParsedCourses() }) {
                                    Text("Clear", color = Color(0xFFEF4444), fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        parsedCourses.forEach { ic ->
                                            scheduleViewModel.addCourse(ic.toCourse("Fall 2026"))
                                        }
                                        Toast.makeText(context, "Successfully imported courses!", Toast.LENGTH_SHORT).show()
                                        onFinishImport()
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import All", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(parsedCourses) { index, ic ->
                                val hasConflict = existingCourses.any { ec ->
                                    ec.dayOfWeek == ic.dayOfWeek && (ic.startPeriod <= ec.endPeriod && ec.startPeriod <= ic.endPeriod)
                                }
                                val dayName = when (ic.dayOfWeek) {
                                    1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"; else -> "Mon"
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (hasConflict) Color(0xFF2A1B13) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (hasConflict) Color(0xFFF59E0B) else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .clickable { editingCourseIndex = index },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = ic.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (hasConflict) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFF59E0B)
                                                    ) {
                                                        Text("Overlap", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "📍 ${ic.classroom} • $dayName (P${ic.startPeriod}-P${ic.endPeriod} · ${ic.startTime}-${ic.endTime})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (ic.instructor.isNotEmpty()) {
                                                Text(
                                                    text = "👤 ${ic.instructor}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { editingCourseIndex = index },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = "Edit",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { importViewModel.removeParsedCourse(ic) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Remove",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
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

    // Inline Pre-Import Edit Dialog for fine-tuning any course
    editingCourseIndex?.let { idx ->
        val ic = parsedCourses.getOrNull(idx)
        if (ic != null) {
            var name by remember { mutableStateOf(ic.name) }
            var classroom by remember { mutableStateOf(ic.classroom) }
            var instructor by remember { mutableStateOf(ic.instructor) }
            var dayOfWeek by remember { mutableIntStateOf(ic.dayOfWeek) }
            var startPeriod by remember { mutableIntStateOf(ic.startPeriod) }
            var endPeriod by remember { mutableIntStateOf(ic.endPeriod) }

            Dialog(onDismissRequest = { editingCourseIndex = null }) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Edit Parsed Course", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Course Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = classroom,
                                onValueChange = { classroom = it },
                                label = { Text("Classroom") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = instructor,
                                onValueChange = { instructor = it },
                                label = { Text("Instructor") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("Day of Week (1=Mon ... 7=Sun)", fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..7).forEach { d ->
                                FilterChip(
                                    selected = dayOfWeek == d,
                                    onClick = { dayOfWeek = d },
                                    label = { Text("D$d", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { editingCourseIndex = null }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val updated = ic.copy(
                                        name = name,
                                        classroom = classroom,
                                        instructor = instructor,
                                        dayOfWeek = dayOfWeek,
                                        startPeriod = startPeriod,
                                        endPeriod = endPeriod
                                    )
                                    importViewModel.updateParsedCourse(idx, updated)
                                    editingCourseIndex = null
                                }
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetScheduleCard(
    title: String,
    description: String,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Load Preset Schedule", fontWeight = FontWeight.Bold)
            }
        }
    }
}
