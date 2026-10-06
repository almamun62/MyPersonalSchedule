package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CourseMaterialEntity
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseMaterialLibraryDialog(
    viewModel: ScheduleViewModel,
    initialCourseName: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val allMaterials by viewModel.allCourseMaterials.collectAsStateWithLifecycle()

    var selectedCourseFilter by remember { mutableStateOf(initialCourseName.ifBlank { "All Courses" }) }
    var readingMaterial by remember { mutableStateOf<CourseMaterialEntity?>(null) }
    var showAddMaterialDialog by remember { mutableStateOf(false) }

    // Seed default starter materials if library is completely empty
    LaunchedEffect(allMaterials) {
        if (allMaterials.isEmpty()) {
            viewModel.insertCourseMaterial(
                CourseMaterialEntity(
                    courseName = "Calculus I",
                    title = "Key Formulas & Derivative Rules",
                    fileName = "calculus_formulas.txt",
                    fileType = "NOTES",
                    content = """
                    CALCULUS I: ESSENTIAL FORMULA SHEET
                    ====================================
                    1. Limits & Continuity:
                       lim_{x->0} (sin x)/x = 1
                       lim_{x->0} (1 - cos x)/x = 0
                       lim_{x->inf} (1 + 1/x)^x = e
                    
                    2. Derivative Rules:
                       Power Rule: d/dx [x^n] = n * x^(n-1)
                       Product Rule: (uv)' = u'v + uv'
                       Quotient Rule: (u/v)' = (u'v - uv') / v^2
                       Chain Rule: (f(g(x)))' = f'(g(x)) * g'(x)
                    
                    3. Trigonometric Derivatives:
                       d/dx [sin x] = cos x
                       d/dx [cos x] = -sin x
                       d/dx [tan x] = sec^2 x
                       d/dx [sec x] = sec x * tan x
                    
                    4. Integration by Parts:
                       integral(u dv) = uv - integral(v du)
                    """.trimIndent(),
                    fileSizeBytes = 650
                )
            )

            viewModel.insertCourseMaterial(
                CourseMaterialEntity(
                    courseName = "Computer Architecture",
                    title = "RISC-V Instruction Set Cheat Sheet",
                    fileName = "riscv_instructions.txt",
                    fileType = "NOTES",
                    content = """
                    RISC-V ARCHITECTURE REFERENCE GUIDE
                    ====================================
                    Register Conventions:
                    - x0 (zero): Hardwired constant 0
                    - x1 (ra): Return address
                    - x2 (sp): Stack pointer
                    - x10-x17 (a0-a7): Function arguments / return values
                    - x5-x7, x28-x31 (t0-t6): Temporary registers
                    - x8-x9, x18-x27 (s0-s11): Saved registers
                    
                    Common Instructions:
                    - ADD rd, rs1, rs2 (rd = rs1 + rs2)
                    - SUB rd, rs1, rs2 (rd = rs1 - rs2)
                    - LW rd, offset(rs1) (Load 32-bit word from memory)
                    - SW rs2, offset(rs1) (Store 32-bit word to memory)
                    - BEQ rs1, rs2, label (Branch if equal)
                    - JAL rd, label (Jump and link)
                    """.trimIndent(),
                    fileSizeBytes = 720
                )
            )
        }
    }

    val filteredMaterials = remember(allMaterials, selectedCourseFilter) {
        if (selectedCourseFilter == "All Courses" || selectedCourseFilter.isBlank()) {
            allMaterials
        } else {
            allMaterials.filter { it.courseName.equals(selectedCourseFilter, ignoreCase = true) }
        }
    }

    // Document Picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "document.txt"

                viewModel.insertCourseMaterial(
                    CourseMaterialEntity(
                        courseName = if (selectedCourseFilter != "All Courses") selectedCourseFilter else "General",
                        title = fileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' '),
                        fileName = fileName,
                        fileType = if (fileName.endsWith(".pdf", ignoreCase = true)) "PDF" else "DOCUMENT",
                        content = content,
                        fileSizeBytes = content.length.toLong()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                modifier = Modifier.statusBarsPadding(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                        title = {
                            Column {
                                Text(
                                    text = if (readingMaterial != null) readingMaterial!!.title else "Course Material Library",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (readingMaterial != null) "In-App Focus Reader • ${readingMaterial!!.courseName}" else "Offline study texts, notes & reference sheets",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (readingMaterial != null) {
                                    readingMaterial = null
                                } else {
                                    onDismiss()
                                }
                            }) {
                                Icon(
                                    if (readingMaterial != null) Icons.Default.ArrowBack else Icons.Default.Close,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            if (readingMaterial == null) {
                                IconButton(onClick = { showAddMaterialDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Material")
                                }
                                IconButton(onClick = { filePickerLauncher.launch(arrayOf("text/*", "application/pdf")) }) {
                                    Icon(Icons.Outlined.UploadFile, contentDescription = "Import File")
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                if (readingMaterial != null) {
                    // ==========================================
                    // IN-APP FULLSCREEN MATERIAL READER
                    // (Readable directly inside pinned Focus Mode!)
                    // ==========================================
                    InAppMaterialReader(
                        material = readingMaterial!!,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                } else {
                    // ==========================================
                    // MATERIAL LIBRARY BROWSER
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Course Filter Chips
                        val coursesList = remember(allCourses) {
                            listOf("All Courses") + allCourses.map { it.name }.distinct()
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(coursesList) { cName ->
                                FilterChip(
                                    selected = selectedCourseFilter == cName,
                                    onClick = { selectedCourseFilter = cName },
                                    label = { Text(cName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }

                        // Library Materials List
                        if (filteredMaterials.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "No study materials added yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Add reference notes or upload text files to read during Focus Mode.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(onClick = { showAddMaterialDialog = true }) {
                                        Text("Create Study Note")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredMaterials, key = { it.id }) { item ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { readingMaterial = item }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(14.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Description,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(
                                                        text = item.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${item.courseName} • ${item.fileType}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { viewModel.deleteCourseMaterial(item) }) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Icon(
                                                    Icons.Outlined.ChevronRight,
                                                    contentDescription = "Open",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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

    // Add Material Dialog
    if (showAddMaterialDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }
        var newCourseName by remember { mutableStateOf(if (selectedCourseFilter != "All Courses") selectedCourseFilter else "Calculus I") }

        AlertDialog(
            onDismissRequest = { showAddMaterialDialog = false },
            title = { Text("Add Course Material Note", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        placeholder = { Text("e.g. Chapter 3 Summary, Formula Sheet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newCourseName,
                        onValueChange = { newCourseName = it },
                        label = { Text("Course Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Content / Notes") },
                        placeholder = { Text("Paste summary notes, definitions, formulas...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.insertCourseMaterial(
                                CourseMaterialEntity(
                                    courseName = newCourseName.trim(),
                                    title = newTitle.trim(),
                                    fileName = "${newTitle.trim().replace(' ', '_')}.txt",
                                    fileType = "NOTES",
                                    content = newContent.trim(),
                                    fileSizeBytes = newContent.length.toLong()
                                )
                            )
                            showAddMaterialDialog = false
                        }
                    }
                ) {
                    Text("Save to Library")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMaterialDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Distraction-Free In-App Material Reader
 */
@Composable
private fun InAppMaterialReader(
    material: CourseMaterialEntity,
    modifier: Modifier = Modifier
) {
    var fontSizeSp by remember { mutableFloatStateOf(14f) }

    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Reader toolbar (font size adjuster)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reading: ${material.courseName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(10f) }) {
                        Text("A-", fontWeight = FontWeight.Bold)
                    }
                    Text("${fontSizeSp.toInt()}sp", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(24f) }) {
                        Text("A+", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Reading Content Paper
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Text(
                    text = material.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSizeSp + 4).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = material.content.ifBlank { "No text content available in this document." },
                    fontSize = fontSizeSp.sp,
                    lineHeight = (fontSizeSp * 1.5).sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
