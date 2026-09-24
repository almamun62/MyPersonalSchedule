package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.tr
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class DrawnStroke(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

enum class CanvasPaperType {
    BLANK, LINED, GRID
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var strokes by remember { mutableStateOf(listOf<DrawnStroke>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(8f) }
    var isEraserMode by remember { mutableStateOf(false) }
    var paperType by remember { mutableStateOf(CanvasPaperType.LINED) }

    val colorList = listOf(
        Color(0xFF0F172A), // Dark Navy/Black
        Color(0xFF1D4ED8), // Blue Pen
        Color(0xFFDC2626), // Red Pen
        Color(0xFF15803D), // Green Pen
        Color(0xFF7E22CE), // Purple
        Color(0xFFF59E0B)  // Gold/Highlighter
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
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
                            Text("Handwriting & Stylus Canvas".tr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Draw diagrams, write notes, or solve equations".tr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close".tr)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (strokes.isNotEmpty()) {
                                    strokes = strokes.dropLast(1)
                                }
                            },
                            enabled = strokes.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo".tr)
                        }
                        IconButton(onClick = { strokes = emptyList() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All".tr)
                        }
                        Button(
                            onClick = {
                                try {
                                    val imagesDir = File(context.filesDir, "notebook_images")
                                    if (!imagesDir.exists()) imagesDir.mkdirs()
                                    val fileName = "handwriting_${UUID.randomUUID()}.png"
                                    val file = File(imagesDir, fileName)
                                    
                                    val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(bitmap)
                                    canvas.drawColor(android.graphics.Color.WHITE)
                                    
                                    // Draw background lines if LINED or GRID
                                    val linePaint = Paint().apply {
                                        color = android.graphics.Color.parseColor("#E2E8F0")
                                        strokeWidth = 2f
                                    }
                                    if (paperType == CanvasPaperType.LINED) {
                                        var y = 120f
                                        while (y < 1920f) {
                                            canvas.drawLine(0f, y, 1080f, y, linePaint)
                                            y += 60f
                                        }
                                    } else if (paperType == CanvasPaperType.GRID) {
                                        var pos = 60f
                                        while (pos < 1920f) {
                                            canvas.drawLine(0f, pos, 1080f, pos, linePaint)
                                            if (pos < 1080f) {
                                                canvas.drawLine(pos, 0f, pos, 1920f, linePaint)
                                            }
                                            pos += 60f
                                        }
                                    }

                                    strokes.forEach { stroke ->
                                        val paint = Paint().apply {
                                            color = if (stroke.isEraser) android.graphics.Color.WHITE else stroke.color.toArgb()
                                            style = Paint.Style.STROKE
                                            strokeWidth = stroke.strokeWidth * 2f // Scale up for bitmap
                                            strokeJoin = Paint.Join.ROUND
                                            strokeCap = Paint.Cap.ROUND
                                            isAntiAlias = true
                                        }
                                        canvas.drawPath(stroke.path.asAndroidPath(), paint)
                                    }

                                    val out = FileOutputStream(file)
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    out.flush()
                                    out.close()
                                    onSave(file.absolutePath)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save".tr)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                // Toolbar Controls
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pen Colors
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colorList.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable {
                                            selectedColor = color
                                            isEraserMode = false
                                        }
                                        .then(
                                            if (!isEraserMode && selectedColor == color) {
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            } else Modifier
                                        )
                                )
                            }
                        }

                        // Tools & Eraser
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = isEraserMode,
                                onClick = { isEraserMode = !isEraserMode },
                                label = { Text("Eraser".tr) },
                                leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )

                            // Thickness Selector
                            IconButton(
                                onClick = {
                                    strokeWidth = when(strokeWidth) {
                                        4f -> 10f
                                        10f -> 20f
                                        else -> 4f
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LineWeight,
                                    contentDescription = "Stroke Thickness",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Paper Background
                            IconButton(
                                onClick = {
                                    paperType = when(paperType) {
                                        CanvasPaperType.BLANK -> CanvasPaperType.LINED
                                        CanvasPaperType.LINED -> CanvasPaperType.GRID
                                        CanvasPaperType.GRID -> CanvasPaperType.BLANK
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = when(paperType) {
                                        CanvasPaperType.BLANK -> Icons.Default.CropSquare
                                        CanvasPaperType.LINED -> Icons.Default.ViewAgenda
                                        CanvasPaperType.GRID -> Icons.Default.GridOn
                                    },
                                    contentDescription = "Paper Style"
                                )
                            }
                        }
                    }
                }

                // Drawing Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .pointerInput(selectedColor, strokeWidth, isEraserMode) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = newPath
                                    val stroke = DrawnStroke(
                                        path = newPath,
                                        color = if (isEraserMode) Color.White else selectedColor,
                                        strokeWidth = if (isEraserMode) strokeWidth * 2.5f else strokeWidth,
                                        isEraser = isEraserMode
                                    )
                                    strokes = strokes + stroke
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                    strokes = strokes.toList() // Force recomposition
                                },
                                onDragEnd = {
                                    currentPath = null
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Draw Notebook Paper Background lines
                        if (paperType == CanvasPaperType.LINED) {
                            var y = 120f
                            while (y < canvasHeight) {
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(0f, y),
                                    end = Offset(canvasWidth, y),
                                    strokeWidth = 2f
                                )
                                y += 60f
                            }
                        } else if (paperType == CanvasPaperType.GRID) {
                            var pos = 60f
                            while (pos < canvasHeight) {
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(0f, pos),
                                    end = Offset(canvasWidth, pos),
                                    strokeWidth = 1.5f
                                )
                                if (pos < canvasWidth) {
                                    drawLine(
                                        color = Color(0xFFE2E8F0),
                                        start = Offset(pos, 0f),
                                        end = Offset(pos, canvasHeight),
                                        strokeWidth = 1.5f
                                    )
                                }
                                pos += 60f
                            }
                        }

                        // Draw Strokes
                        strokes.forEach { stroke ->
                            drawPath(
                                path = stroke.path,
                                color = stroke.color,
                                style = Stroke(
                                    width = stroke.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
