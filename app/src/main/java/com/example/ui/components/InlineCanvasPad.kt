package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.widget.Toast
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.tr
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun InlineCanvasPad(
    modifier: Modifier = Modifier,
    onSaveDrawing: (filePath: String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var strokes by remember { mutableStateOf(listOf<DrawnStroke>()) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

    var selectedColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    var isEraserMode by remember { mutableStateOf(false) }

    val inkColors = listOf(
        Color(0xFF0F172A), // Dark Ink
        Color(0xFF1D4ED8), // Royal Blue
        Color(0xFFDC2626), // Crimson Red
        Color(0xFF15803D), // Forest Green
        Color(0xFF7E22CE), // Purple
        Color(0xFFF59E0B)  // Gold
    )

    fun createPathFromPoints(pts: List<StrokePoint>): Path {
        val path = Path()
        if (pts.isEmpty()) return path
        path.moveTo(pts.first().x, pts.first().y)
        for (i in 1 until pts.size) {
            path.lineTo(pts[i].x, pts[i].y)
        }
        return path
    }

    fun saveCanvasToBitmap() {
        if (strokes.isEmpty() && currentPoints.isEmpty()) {
            Toast.makeText(context, "Canvas is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val imgDir = File(context.filesDir, "notebook_images")
            if (!imgDir.exists()) imgDir.mkdirs()
            val file = File(imgDir, "canvas_${UUID.randomUUID()}.png")

            val width = 1080
            val height = 720
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            // Draw Ruled Lines
            val linePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#E2E8F0")
                strokeWidth = 2f
            }
            var y = 80f
            while (y < height) {
                canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
                y += 60f
            }

            // Draw strokes
            strokes.forEach { stroke ->
                val strokePath = android.graphics.Path()
                if (stroke.points.isNotEmpty()) {
                    strokePath.moveTo(stroke.points.first().x, stroke.points.first().y)
                    for (i in 1 until stroke.points.size) {
                        strokePath.lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }

                val paint = Paint().apply {
                    color = if (stroke.brushStyle == BrushStyle.ERASER) android.graphics.Color.WHITE else stroke.color.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = stroke.strokeWidth * 1.5f
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }
                canvas.drawPath(strokePath, paint)
            }

            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

            onSaveDrawing(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving drawing: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Touch & Stylus Note Canvas".tr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) strokes = strokes.dropLast(1)
                        },
                        enabled = strokes.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo".tr, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { strokes = emptyList() },
                        enabled = strokes.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear".tr, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Canvas".tr, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tool Palette Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ink Colors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    inkColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
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

                // Eraser & Thickness
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isEraserMode,
                        onClick = { isEraserMode = !isEraserMode },
                        label = { Text("Eraser".tr, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )

                    IconButton(
                        onClick = {
                            strokeWidth = when (strokeWidth) {
                                4f -> 8f
                                8f -> 16f
                                else -> 4f
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.LineWeight, contentDescription = "Thickness".tr, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Drawing Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .pointerInput(selectedColor, strokeWidth, isEraserMode) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(StrokePoint(offset.x, offset.y))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + StrokePoint(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    val newStroke = DrawnStroke(
                                        points = currentPoints,
                                        color = if (isEraserMode) Color.White else selectedColor,
                                        strokeWidth = if (isEraserMode) strokeWidth * 2.5f else strokeWidth,
                                        brushStyle = if (isEraserMode) BrushStyle.ERASER else BrushStyle.FOUNTAIN_PEN
                                    )
                                    strokes = strokes + newStroke
                                    currentPoints = emptyList()
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Background lines
                    var y = 60f
                    while (y < h) {
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.5f
                        )
                        y += 50f
                    }

                    // Render Strokes
                    strokes.forEach { stroke ->
                        val p = createPathFromPoints(stroke.points)
                        drawPath(
                            path = p,
                            color = stroke.color,
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Render current stroke
                    if (currentPoints.isNotEmpty()) {
                        val activeP = createPathFromPoints(currentPoints)
                        drawPath(
                            path = activeP,
                            color = if (isEraserMode) Color.White else selectedColor,
                            style = Stroke(
                                width = if (isEraserMode) strokeWidth * 2.5f else strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attach / Save Button
            Button(
                onClick = { saveCanvasToBitmap() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Insert Drawing into Note & Save to Room DB".tr, fontWeight = FontWeight.Bold)
            }
        }
    }
}
