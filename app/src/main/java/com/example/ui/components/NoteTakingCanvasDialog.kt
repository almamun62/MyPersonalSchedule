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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

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
    var currentNoteId by remember { mutableLongStateOf(0L) }
    var isDirty by remember { mutableStateOf(false) }
    var lastSavedStatus by remember { mutableStateOf<String?>(null) }

    val allCourses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()

    // Canvas State
    val paths = remember { mutableStateListOf<DrawnPathState>() }
    val redoStack = remember { mutableStateListOf<DrawnPathState>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    val currentPoints = remember { mutableStateListOf<Pair<Float, Float>>() }

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

    // Helper to serialize strokes to JSON
    fun serializeStrokesToJson(): String? {
        if (paths.isEmpty()) return null
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
        return arr.toString()
    }

    // Save/Autosave note to Room Database
    fun saveNoteToRoom() {
        if (noteTitle.isBlank() && noteTextContent.isBlank() && paths.isEmpty()) return

        val titleToSave = noteTitle.ifBlank { "Note: $selectedCourseName" }
        val drawingJson = serializeStrokesToJson()

        viewModel.insertNote(
            title = titleToSave,
            content = noteTextContent,
            courseId = selectedCourseId,
            drawingJson = drawingJson
        )
        lastSavedStatus = "Saved"
    }

    // Autosave every 3 seconds if modified
    LaunchedEffect(isDirty) {
        if (isDirty) {
            delay(3000L)
            saveNoteToRoom()
            isDirty = false
        }
    }

    fun handleDismiss() {
        if (isDirty || paths.isNotEmpty() || noteTitle.isNotBlank() || noteTextContent.isNotBlank()) {
            saveNoteToRoom()
        }
        onDismiss()
    }

    // Eraser helper: removes strokes close to the touch position
    fun eraseStrokesNear(x: Float, y: Float, radius: Float = 35f) {
        val iterator = paths.iterator()
        var removedAny = false
        while (iterator.hasNext()) {
            val stroke = iterator.next()
            val intersects = stroke.pointsList.any { (px, py) ->
                hypot(px - x, py - y) <= radius
            }
            if (intersects) {
                iterator.remove()
                removedAny = true
            }
        }
        if (removedAny) {
            isDirty = true
        }
    }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Course & Lecture Notes".tr,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            lastSavedStatus?.let { status ->
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• $status",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Autosaved to Room • Linked to Course".tr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { handleDismiss() }) {
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
                    // Title & Linked Course Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = {
                                noteTitle = it
                                isDirty = true
                            },
                            label = { Text("Note Title") },
                            placeholder = { Text("e.g. Lecture 4: Tree Traversal & Recursion") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Course:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    FilterChip(
                                        selected = selectedCourseId == 0L,
                                        onClick = {
                                            selectedCourseId = 0L
                                            isDirty = true
                                        },
                                        label = { Text("General", fontSize = 11.sp) }
                                    )
                                }
                                items(allCourses) { c ->
                                    FilterChip(
                                        selected = selectedCourseId == c.id,
                                        onClick = {
                                            selectedCourseId = c.id
                                            isDirty = true
                                        },
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
                        0 -> { // Canvas Draw Tab
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Toolbar: Colors, Eraser, Undo, Redo, Clear
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Palette colors
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        items(strokeColors) { color ->
                                            val isSelected = currentColor == color && !isEraserMode
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
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

                                    // Action buttons: Eraser, Undo, Redo, Clear
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Eraser
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

                                        // Undo
                                        IconButton(
                                            onClick = {
                                                if (paths.isNotEmpty()) {
                                                    val popped = paths.removeAt(paths.size - 1)
                                                    redoStack.add(popped)
                                                    isDirty = true
                                                }
                                            },
                                            enabled = paths.isNotEmpty(),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                                        }

                                        // Redo
                                        IconButton(
                                            onClick = {
                                                if (redoStack.isNotEmpty()) {
                                                    val restored = redoStack.removeAt(redoStack.size - 1)
                                                    paths.add(restored)
                                                    isDirty = true
                                                }
                                            },
                                            enabled = redoStack.isNotEmpty(),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                                        }

                                        // Clear
                                        IconButton(
                                            onClick = {
                                                paths.clear()
                                                redoStack.clear()
                                                isDirty = true
                                            },
                                            enabled = paths.isNotEmpty(),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                // Thickness selection
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Width:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    listOf(3f to "Fine", 8f to "Medium", 16f to "Thick").forEach { (w, label) ->
                                        FilterChip(
                                            selected = currentStrokeWidth == w,
                                            onClick = { currentStrokeWidth = w },
                                            label = { Text(label, fontSize = 10.sp) }
                                        )
                                    }
                                    if (isEraserMode) {
                                        Text(
                                            text = "• Eraser Mode Active (touch stroke to erase)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Drawing Canvas Box
                                val canvasBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                                                        if (isEraserMode) {
                                                            eraseStrokesNear(offset.x, offset.y)
                                                        } else {
                                                            val p = Path()
                                                            p.moveTo(offset.x, offset.y)
                                                            currentPath = p
                                                            currentPoints.clear()
                                                            currentPoints.add(Pair(offset.x, offset.y))
                                                        }
                                                    },
                                                    onDrag = { change, _ ->
                                                        val point = change.position
                                                        if (isEraserMode) {
                                                            eraseStrokesNear(point.x, point.y)
                                                        } else {
                                                            currentPath?.lineTo(point.x, point.y)
                                                            currentPoints.add(Pair(point.x, point.y))
                                                        }
                                                        change.consume()
                                                    },
                                                    onDragEnd = {
                                                        if (!isEraserMode) {
                                                            currentPath?.let { p ->
                                                                paths.add(
                                                                    DrawnPathState(
                                                                        path = p,
                                                                        color = currentColor,
                                                                        strokeWidth = currentStrokeWidth,
                                                                        pointsList = currentPoints.toList()
                                                                    )
                                                                )
                                                                redoStack.clear()
                                                                isDirty = true
                                                            }
                                                            currentPath = null
                                                            currentPoints.clear()
                                                        }
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
                                                color = currentColor,
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

                        1 -> { // Text Memo Tab
                            OutlinedTextField(
                                value = noteTextContent,
                                onValueChange = {
                                    noteTextContent = it
                                    isDirty = true
                                },
                                placeholder = { Text("Write lecture notes, formulas, questions, or assignment reminders...") },
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        2 -> { // Saved Notes List Tab
                            if (allNotes.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No saved notes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(allNotes) { note ->
                                        val linkedName = remember(allCourses, note.courseId) {
                                            if (note.courseId == 0L) "General"
                                            else allCourses.find { it.id == note.courseId }?.name ?: "General"
                                        }

                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(note.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(
                                                            text = "Course: $linkedName",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.deleteNote(note) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (note.content.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = note.content,
                                                        fontSize = 12.sp,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis
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

                // Bottom Save Action Bar
                if (activeTab != 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDirty) "Auto-saving..." else "Autosave enabled",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                saveNoteToRoom()
                                activeTab = 2
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = noteTitle.isNotBlank() || noteTextContent.isNotBlank() || paths.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Now".tr, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
