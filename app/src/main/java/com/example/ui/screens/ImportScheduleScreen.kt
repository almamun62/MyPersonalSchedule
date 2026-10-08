package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WeekRule
import com.example.domain.model.ImportedCourse
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
    val statusMessage by importViewModel.statusMessage.collectAsStateWithLifecycle()
    val replaceExisting by importViewModel.replaceExisting.collectAsStateWithLifecycle()

    var freeText by remember { mutableStateOf("") }
    var shareCodeInput by remember { mutableStateOf("") }
    var editingCourseIndex by remember { mutableStateOf<Int?>(null) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickAddDay by remember { mutableIntStateOf(1) }
    var quickAddPeriod by remember { mutableIntStateOf(1) }

    // Check conflicts between parsed courses and existing database courses
    val conflictCount = remember(parsedCourses, existingCourses) {
        parsedCourses.filter { it.isSelected }.count { pc ->
            existingCourses.any { ec ->
                ec.dayOfWeek == pc.dayOfWeek &&
                        com.example.domain.CourseConflictDetector.doWeekRulesOverlap(ec.weekRule, pc.weekRule) &&
                        (pc.startPeriod <= ec.endPeriod && ec.startPeriod <= pc.endPeriod)
            }
        }
    }

    // Universal File picker launcher supporting .xlsx, .xls, .ics, .json, .csv, .tsv, .html
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            importViewModel.parseFileFromUri(it)
        }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Offline security badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                        text = "100% Offline Processing — No cloud uploads, your data stays entirely on your device.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab Selector
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == ImportTab.FILE_PICKER,
                    onClick = { importViewModel.setTab(ImportTab.FILE_PICKER) },
                    text = { Text("📁 File Import", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.FREE_TEXT,
                    onClick = { importViewModel.setTab(ImportTab.FREE_TEXT) },
                    text = { Text("💬 Smart Text", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.VISUAL_GRID,
                    onClick = { importViewModel.setTab(ImportTab.VISUAL_GRID) },
                    text = { Text("⚡ Quick Grid", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.SHARE_CODE,
                    onClick = { importViewModel.setTab(ImportTab.SHARE_CODE) },
                    text = { Text("🔑 Code & JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == ImportTab.PRESETS,
                    onClick = { importViewModel.setTab(ImportTab.PRESETS) },
                    text = { Text("🏫 Presets", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            // Status message banner
            statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (msg.contains("Success") || msg.contains("Loaded") || msg.contains("recognized") || msg.contains("Added")) {
                        Color(0xFF10B981).copy(alpha = 0.15f)
                    } else {
                        Color(0xFFF59E0B).copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (msg.contains("Success") || msg.contains("Loaded") || msg.contains("recognized") || msg.contains("Added")) Color(0xFF10B981) else Color(0xFFF59E0B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { importViewModel.clearStatusMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                ImportTab.FILE_PICKER -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Offline File Importer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Supports Excel, iCalendar, JSON, CSV, and HTML tables", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Formats grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FormatBadge("📊 Excel", ".xlsx / .xls")
                                FormatBadge("📅 iCal", ".ics calendar")
                                FormatBadge("🌐 Web", ".html table")
                                FormatBadge("⚙️ JSON", "WakeUp / App")
                            }

                            Text(
                                text = "• Excel: Automatically parses both 2D Timetable Grids and Course Lists\n" +
                                       "• iCalendar: RFC 5545 events with recurring rules and locations\n" +
                                       "• Web HTML: Save your educational portal timetable page offline as HTML\n" +
                                       "• CSV / TSV: Comma, tab, or semicolon separated timetables",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            Button(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf(
                                        "application/vnd.ms-excel",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "text/calendar",
                                        "application/json",
                                        "text/csv",
                                        "text/html",
                                        "text/plain",
                                        "*/*"
                                    ))
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Schedule File from Device", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                ImportTab.FREE_TEXT -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("💬 Smart Text & Table Paste", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Paste timetable rows, copied syllabus text, or cells copied from Excel/Sheets. Engine automatically extracts Course Name, Day, Periods, Room, Teacher, and Week Rules.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Quick sample insertion buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssistChip(
                                    onClick = {
                                        freeText = "高等数学(上) 周一 第1-2节 软件楼101 张伟教授 单周\n大学物理 周三 第3-4节 理科楼B204 李明老师\n数据结构与算法 周五 第6-7节 实验楼302 王强"
                                        importViewModel.parseFreeText(freeText)
                                    },
                                    label = { Text("Sample Chinese Portal", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = {
                                        freeText = "Linear Algebra, Mon P1-2, Room 301, Dr. Euler\nData Analysis, Wed P3-5, Lab B, Prof. Turing\nOperating Systems, Fri P6-7, Hall 102, Dr. Ritchie"
                                        importViewModel.parseFreeText(freeText)
                                    },
                                    label = { Text("Sample English List", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = {
                                        freeText = "Time\tMonday\tTuesday\tWednesday\tThursday\tFriday\n1-2\tMath @ 101\tPhysics @ 201\tMath @ 101\tEnglish @ 102\tPE @ Gym\n3-4\tCS @ Lab 1\t\tCS @ Lab 1\tHistory @ 105\t"
                                        importViewModel.parseFreeText(freeText)
                                    },
                                    label = { Text("Sample Matrix Table", fontSize = 10.sp) }
                                )
                            }

                            OutlinedTextField(
                                value = freeText,
                                onValueChange = {
                                    freeText = it
                                    if (it.length > 10) importViewModel.parseFreeText(it)
                                },
                                placeholder = {
                                    Text("Paste schedule text or spreadsheet cells here...\ne.g. Operating Systems Mon P1-2 Room 101 Prof. Smith")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (freeText.isNotBlank()) {
                                            importViewModel.parseFreeText(freeText)
                                        }
                                    },
                                    enabled = freeText.isNotBlank(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Extract Courses", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { freeText = "" },
                                    enabled = freeText.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }

                ImportTab.VISUAL_GRID -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("⚡ Quick Grid Timetable Filler", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Tap any cell to place a course directly into your timetable", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        quickAddDay = 1
                                        quickAddPeriod = 1
                                        showQuickAddDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Slot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Interactive 7 Days x 12 Periods Compact Table
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                // Day Headers
                                Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                    Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                                        Text("P", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    days.forEachIndexed { dIdx, dName ->
                                        Box(
                                            modifier = Modifier
                                                .width(48.dp)
                                                .padding(horizontal = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(dName, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Periods 1 to 12
                                (1..12).forEach { period ->
                                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(26.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("$period", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        (1..7).forEach { day ->
                                            val existingInParsed = parsedCourses.firstOrNull {
                                                it.dayOfWeek == day && period in it.startPeriod..it.endPeriod
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .width(48.dp)
                                                    .height(26.dp)
                                                    .padding(1.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (existingInParsed != null) {
                                                            MaterialTheme.colorScheme.primaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        }
                                                    )
                                                    .clickable {
                                                        quickAddDay = day
                                                        quickAddPeriod = period
                                                        showQuickAddDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (existingInParsed != null) {
                                                    Text(
                                                        text = existingInParsed.name.take(4),
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                } else {
                                                    Text("+", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ImportTab.SHARE_CODE -> {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🔑 Share Code & JSON Import / Export", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "Exchange timetables with classmates 100% offline via compact Share Codes (SCH#...) or open JSON format (compatible with WakeUp 课程表 and ClassIsland).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = shareCodeInput,
                                onValueChange = { shareCodeInput = it },
                                label = { Text("Enter Share Code (SCH#...) or Paste JSON") },
                                singleLine = false,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        if (shareCodeInput.isNotBlank()) {
                                            val trimmed = shareCodeInput.trim()
                                            if (trimmed.startsWith("SCH#")) {
                                                importViewModel.parseShareCode(trimmed)
                                            } else {
                                                importViewModel.parseJsonContent(trimmed)
                                            }
                                        }
                                    },
                                    enabled = shareCodeInput.isNotBlank(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Decode & Preview", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val code = scheduleViewModel.generateShareCode()
                                        shareCodeInput = code
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Share Code", code))
                                        Toast.makeText(context, "Share Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Generate Mine", fontWeight = FontWeight.Bold)
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    val json = scheduleViewModel.exportScheduleAsJson()
                                    shareCodeInput = json
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Timetable JSON", json))
                                    Toast.makeText(context, "Full Timetable JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Full Timetable as JSON", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                ImportTab.PRESETS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            }

            // Parsed Courses Preview & Interactive Confirmation Section
            if (parsedCourses.isNotEmpty()) {
                val selectedCount = parsedCourses.count { it.isSelected }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Preview Schedule ($selectedCount/${parsedCourses.size} Selected)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (conflictCount > 0) {
                                    Text("⚠️ $conflictCount time conflict(s) with existing classes", fontSize = 11.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("Ready to import offline", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { importViewModel.clearParsedCourses() }) {
                                    Text("Clear", color = Color(0xFFEF4444), fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val toImport = parsedCourses.filter { it.isSelected }.map { it.toCourse("Fall 2026") }
                                        if (toImport.isEmpty()) {
                                            Toast.makeText(context, "No courses selected!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        scheduleViewModel.importCourses(toImport, replaceExisting = replaceExisting)
                                        val modeMsg = if (replaceExisting) "Replaced schedule with" else "Added"
                                        Toast.makeText(context, "$modeMsg ${toImport.size} courses!", Toast.LENGTH_SHORT).show()
                                        onFinishImport()
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import ($selectedCount)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Mode Selector: Replace vs Merge & Select All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = replaceExisting,
                                    onClick = { importViewModel.setReplaceExisting(true) },
                                    label = { Text("Replace Old", fontSize = 10.sp) },
                                    leadingIcon = if (replaceExisting) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = !replaceExisting,
                                    onClick = { importViewModel.setReplaceExisting(false) },
                                    label = { Text("Merge / Append", fontSize = 10.sp) },
                                    leadingIcon = if (!replaceExisting) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) } } else null
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { importViewModel.selectAll(true) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Select All", fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = { importViewModel.selectAll(false) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("None", fontSize = 11.sp)
                                }
                            }
                        }

                        // Courses List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(parsedCourses) { index, ic ->
                                val hasConflict = existingCourses.any { ec ->
                                    ec.dayOfWeek == ic.dayOfWeek &&
                                            com.example.domain.CourseConflictDetector.doWeekRulesOverlap(ec.weekRule, ic.weekRule) &&
                                            (ic.startPeriod <= ec.endPeriod && ec.startPeriod <= ic.endPeriod)
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
                                            .padding(10.dp)
                                            .clickable { importViewModel.toggleCourseSelection(index) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = ic.isSelected,
                                            onCheckedChange = { importViewModel.toggleCourseSelection(index) },
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = ic.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
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
                                                if (ic.weekRule != WeekRule.ALL) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                    ) {
                                                        Text(
                                                            text = if (ic.weekRule == WeekRule.ODD) "Odd Wk" else "Even Wk",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "📍 ${ic.classroom} • $dayName P${ic.startPeriod}-P${ic.endPeriod} (${ic.startTime}-${ic.endTime})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (ic.instructor.isNotEmpty()) {
                                                Text(
                                                    text = "👤 ${ic.instructor}",
                                                    fontSize = 10.sp,
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
                                                    modifier = Modifier.size(16.dp)
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
                                                    modifier = Modifier.size(16.dp)
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

    // Quick Add Slot Dialog for Visual Grid
    if (showQuickAddDialog) {
        var name by remember { mutableStateOf("") }
        var classroom by remember { mutableStateOf("") }
        var instructor by remember { mutableStateOf("") }
        var day by remember { mutableIntStateOf(quickAddDay) }
        var startP by remember { mutableIntStateOf(quickAddPeriod) }
        var endP by remember { mutableIntStateOf((quickAddPeriod + 1).coerceAtMost(12)) }
        var weekRule by remember { mutableStateOf(WeekRule.ALL) }

        Dialog(onDismissRequest = { showQuickAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Place Course on Timetable", fontWeight = FontWeight.Bold, fontSize = 16.sp)

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
                            label = { Text("Classroom / Lab") },
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

                    Text("Day of Week", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val dNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        dNames.forEachIndexed { idx, dName ->
                            FilterChip(
                                selected = day == idx + 1,
                                onClick = { day = idx + 1 },
                                label = { Text(dName, fontSize = 10.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Period", fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (startP > 1) startP-- }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text("P$startP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { if (startP < 12) { startP++; if (endP < startP) endP = startP } }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("End Period", fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (endP > startP) endP-- }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text("P$endP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { if (endP < 12) endP++ }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Text("Week Rule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = weekRule == WeekRule.ALL,
                            onClick = { weekRule = WeekRule.ALL },
                            label = { Text("All Weeks", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = weekRule == WeekRule.ODD,
                            onClick = { weekRule = WeekRule.ODD },
                            label = { Text("Odd (单周)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = weekRule == WeekRule.EVEN,
                            onClick = { weekRule = WeekRule.EVEN },
                            label = { Text("Even (双周)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showQuickAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Please enter course name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val (sTime, eTime) = com.example.domain.parser.ScheduleParser.defaultTimesForPeriods(startP, endP)
                                val newCourse = ImportedCourse(
                                    name = name.trim(),
                                    classroom = classroom.trim().ifBlank { "Classroom" },
                                    instructor = instructor.trim(),
                                    dayOfWeek = day,
                                    startPeriod = startP,
                                    endPeriod = endP,
                                    startTime = sTime,
                                    endTime = eTime,
                                    weekRule = weekRule
                                )
                                importViewModel.addManualCourse(newCourse)
                                showQuickAddDialog = false
                            }
                        ) {
                            Text("Add Course")
                        }
                    }
                }
            }
        }
    }

    // Inline Pre-Import Edit Dialog for fine-tuning any parsed course
    editingCourseIndex?.let { idx ->
        val ic = parsedCourses.getOrNull(idx)
        if (ic != null) {
            var name by remember { mutableStateOf(ic.name) }
            var classroom by remember { mutableStateOf(ic.classroom) }
            var instructor by remember { mutableStateOf(ic.instructor) }
            var dayOfWeek by remember { mutableIntStateOf(ic.dayOfWeek) }
            var startPeriod by remember { mutableIntStateOf(ic.startPeriod) }
            var endPeriod by remember { mutableIntStateOf(ic.endPeriod) }
            var weekRule by remember { mutableStateOf(ic.weekRule) }

            Dialog(onDismissRequest = { editingCourseIndex = null }) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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

                        Text("Day of Week", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val dNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            dNames.forEachIndexed { dIdx, dName ->
                                FilterChip(
                                    selected = dayOfWeek == dIdx + 1,
                                    onClick = { dayOfWeek = dIdx + 1 },
                                    label = { Text(dName, fontSize = 10.sp) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start Period", fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (startPeriod > 1) startPeriod-- }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Text("P$startPeriod", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { if (startPeriod < 12) { startPeriod++; if (endPeriod < startPeriod) endPeriod = startPeriod } }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("End Period", fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (endPeriod > startPeriod) endPeriod-- }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Text("P$endPeriod", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { if (endPeriod < 12) endPeriod++ }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Text("Week Rule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = weekRule == WeekRule.ALL,
                                onClick = { weekRule = WeekRule.ALL },
                                label = { Text("All", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = weekRule == WeekRule.ODD,
                                onClick = { weekRule = WeekRule.ODD },
                                label = { Text("Odd (单)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = weekRule == WeekRule.EVEN,
                                onClick = { weekRule = WeekRule.EVEN },
                                label = { Text("Even (双)", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
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
                                    val (sTime, eTime) = com.example.domain.parser.ScheduleParser.defaultTimesForPeriods(startPeriod, endPeriod)
                                    val updated = ic.copy(
                                        name = name.trim(),
                                        classroom = classroom.trim(),
                                        instructor = instructor.trim(),
                                        dayOfWeek = dayOfWeek,
                                        startPeriod = startPeriod,
                                        endPeriod = endPeriod,
                                        startTime = sTime,
                                        endTime = eTime,
                                        weekRule = weekRule
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
fun FormatBadge(title: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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

            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
