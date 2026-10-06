package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NoteEntity
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ScheduleViewModel
import org.json.JSONArray
import org.json.JSONObject

data class DrawnPathState(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val pointsList: List<Pair<Float, Float>> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteTakingCanvasDialog(
    viewModel: ScheduleViewModel,
    initialCourseId: Long = 0,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Canvas Draw, 1 = Text Note, 2 = Saved Notes
    var noteTitle by remember { mutableStateOf("") }
    var noteTextContent by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableLongStateOf(initialCourseId) }

    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()

    // Canvas State
    val paths = remember { mutableStateListOf<DrawnPathState>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    val strokeColors = listOf(
        Color(0xFF0F172A), // Black
        Color(0xFF2563EB), // Blue
        Color(0xFFEF4444), // Red
        Color(0xFF10B981), // Emerald
        Color(0xFF8B5CF6), // Purple
        Color(0xFFF59E0B), // Amber
        Color(0xFFFFFFFF)  // White
    )
    var currentColor by remember { mutableStateOf(strokeColors[1]) }
    var currentStrokeWidth by remember { mutableFloatStateOf(8f) }
    var isEraserMode by remember { mutableStateOf(false) }

    val selectedCourseName = remember(allCourses, selectedCourseId) {
        if (selectedCourseId == 0L) "General Note"
        else allCourses.find { it.id == selectedCourseId }?.name ?: "General Note"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Course & Lecture Notes".tr,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Freehand Canvas • Text Memos • Local DB".tr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tab Switcher
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Icon(Icons.Outlined.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Draw", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Memo", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Saved (${allNotes.size})", fontSize = 12.sp)
                    }
                }

                if (activeTab != 2) {
                    // Title & Course Selector Row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            label = { Text("Note Title *") },
                            placeholder = { Text("e.g. Lecture 4: Data Structures & Graphs") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Linked Course:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    FilterChip(
                                        selected = selectedCourseId == 0L,
                                        onClick = { selectedCourseId = 0L },
                                        label = { Text("General", fontSize = 11.sp) }
                                    )
                                }
                                items(allCourses) { c ->
                                    FilterChip(
                                        selected = selectedCourseId == c.id,
                                        onClick = { selectedCourseId = c.id },
                                        label = { Text(c.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        0 -> { // Freehand Canvas Draw Tab
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Tool Palette Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Palette colors
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(strokeColors) { color ->
                                            val isSelected = currentColor == color && !isEraserMode
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        currentColor = color
                                                        isEraserMode = false
                                                    }
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = { isEraserMode = !isEraserMode },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.AutoFixHigh,
                                                contentDescription = "Eraser",
                                                tint = if (isEraserMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1) },
                                            enabled = paths.isNotEmpty(),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Undo")
                                        }
                                        IconButton(
                                            onClick = { paths.clear() },
                                            enabled = paths.isNotEmpty(),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                // Stroke Width Selector
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Thickness:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    listOf(3f to "Fine", 8f to "Medium", 16f to "Thick").forEach { (w, label) ->
                                        FilterChip(
                                            selected = currentStrokeWidth == w,
                                            onClick = { currentStrokeWidth = w },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                }

                                // Drawing Canvas View
                                val canvasBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                val strokeColorActive = if (isEraserMode) canvasBg else currentColor

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(canvasBg)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                ) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(currentColor, currentStrokeWidth, isEraserMode) {
                                                detectDragGestures(
                                                    onDragStart = { offset ->
                                                        val p = Path()
                                                        p.moveTo(offset.x, offset.y)
                                                        currentPath = p
                                                        currentPoints.clear()
                                                        currentPoints.add(Pair(offset.x, offset.y))
                                                    },
                                                    onDrag = { change, _ ->
                                                        val point = change.position
                                                        currentPath?.lineTo(point.x, point.y)
                                                        currentPoints.add(Pair(point.x, point.y))
                                                        change.consume()
                                                    },
                                                    onDragEnd = {
                                                        currentPath?.let { p ->
                                                            paths.add(
                                                                DrawnPathState(
                                                                    path = p,
                                                                    color = strokeColorActive,
                                                                    strokeWidth = currentStrokeWidth,
                                                                    pointsList = currentPoints.toList()
                                                                )
                                                            )
                                                        }
                                                        currentPath = null
                                                        currentPoints.clear()
                                                    }
                                                )
                                            }
                                    ) {
                                        // Draw existing saved paths
                                        paths.forEach { drawnPathState ->
                                            drawPath(
                                                path = drawnPathState.path,
                                                color = drawnPathState.color,
                                                style = Stroke(
                                                    width = drawnPathState.strokeWidth,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }

                                        // Draw currently active dragging path
                                        currentPath?.let { p ->
                                            drawPath(
                                                path = p,
                                                color = strokeColorActive,
                                                style = Stroke(
                                                    width = currentStrokeWidth,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }

                                    if (paths.isEmpty() && currentPath == null) {
                                        Text(
                                            text = "✍️ Touch & Draw Notes Here",
                                            modifier = Modifier.align(Alignment.Center),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        1 -> { // Text Note Tab
                            OutlinedTextField(
                                value = noteTextContent,
                                onValueChange = { noteTextContent = it },
                                placeholder = { Text("Write detailed lecture summary, formulas, key definitions, or exam topics...") },
                                modifier = Modifier
                                    .fillMaxSize(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        2 -> { // Saved Notes Tab
                            if (allNotes.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Notes Saved Yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Draw or write notes to save them locally in Room DB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(allNotes) { note ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(14.dp)
                                                    .fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(note.title.ifBlank { "Untitled Note" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        if (note.drawingDataJson != null) {
                                                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                                                Text("🎨 Drawing", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    if (note.content.isNotBlank()) {
                                                        Text(note.content, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }

                                                IconButton(onClick = { viewModel.deleteNote(note) }) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete Note", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Bar
                if (activeTab != 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val jsonPoints = if (paths.isNotEmpty()) {
                                    val arr = JSONArray()
                                    paths.forEach { stroke ->
                                        val obj = JSONObject()
                                        obj.put("color", stroke.color.toArgb())
                                        obj.put("strokeWidth", stroke.strokeWidth)
                                        val pts = JSONArray()
                                        stroke.pointsList.forEach { (x, y) ->
                                            val ptObj = JSONObject()
                                            ptObj.put("x", x)
                                            ptObj.put("y", y)
                                            pts.put(ptObj)
                                        }
                                        obj.put("points", pts)
                                        arr.put(obj)
                                    }
                                    arr.toString()
                                } else null

                                val titleToSave = noteTitle.ifBlank { "Lecture Note (${selectedCourseName})" }
                                viewModel.insertNote(
                                    title = titleToSave,
                                    content = noteTextContent,
                                    courseId = selectedCourseId,
                                    drawingJson = jsonPoints
                                )

                                // Switch to saved notes list
                                activeTab = 2
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = noteTitle.isNotBlank() || noteTextContent.isNotBlank() || paths.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Note to Local DB".tr, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
