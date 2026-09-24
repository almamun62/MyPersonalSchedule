package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.tr
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class BrushStyle {
    FOUNTAIN_PEN,
    HIGHLIGHTER,
    CALLIGRAPHY,
    GLOW_CHALK,
    CRAYON,
    ERASER,
    HAND_PAN
}

enum class ShapeType {
    NONE,
    LINE,
    ARROW,
    RECTANGLE,
    FILLED_RECTANGLE,
    CIRCLE,
    FILLED_CIRCLE,
    TRIANGLE,
    COORDINATE_AXIS
}

enum class CanvasPaperType {
    LINED, GRID, DOT_GRID, BLANK, DARK_CANVAS
}

data class StrokePoint(val x: Float, val y: Float)

data class DrawnStroke(
    val points: List<StrokePoint>,
    val color: Color,
    val strokeWidth: Float,
    val brushStyle: BrushStyle,
    val shapeType: ShapeType = ShapeType.NONE,
    val alpha: Float = 1.0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Immersive Full Screen Mode
    var isFullScreenImmersive by remember { mutableStateOf(false) }

    // Infinite Canvas Transform State
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var scaleZoom by remember { mutableFloatStateOf(1.0f) }

    // Drawing History
    var strokes by remember { mutableStateOf(listOf<DrawnStroke>()) }
    var redoStrokes by remember { mutableStateOf(listOf<DrawnStroke>()) }

    // Gesture / Drag Points
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var shapeStartPoint by remember { mutableStateOf<StrokePoint?>(null) }
    var shapeCurrentPoint by remember { mutableStateOf<StrokePoint?>(null) }

    // Active Tool Configuration
    var activeBrush by remember { mutableStateOf(BrushStyle.FOUNTAIN_PEN) }
    var activeShape by remember { mutableStateOf(ShapeType.NONE) }
    var selectedColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var penThickness by remember { mutableFloatStateOf(6f) }
    var eraserThickness by remember { mutableFloatStateOf(28f) }
    var paperType by remember { mutableStateOf(CanvasPaperType.LINED) }
    var palmRejectionOnlyStylus by remember { mutableStateOf(false) }

    // Custom Color Picker Dialog
    var showColorPickerDialog by remember { mutableStateOf(false) }

    // Extended Color Palettes
    val classicInks = listOf(
        Color(0xFF0F172A), // Dark Slate
        Color(0xFF1D4ED8), // Royal Blue
        Color(0xFFDC2626), // Crimson Red
        Color(0xFF15803D), // Forest Green
        Color(0xFF7E22CE), // Deep Purple
        Color(0xFFD97706), // Amber Gold
        Color(0xFF059669), // Emerald
        Color(0xFFDB2777)  // Magenta
    )

    val neonChalks = listOf(
        Color(0xFFFFFFFF), // Chalk White
        Color(0xFF38BDF8), // Electric Cyan
        Color(0xFF4ADE80), // Neon Lime
        Color(0xFFF43F5E), // Hot Pink
        Color(0xFFFACC15), // Sunlight Yellow
        Color(0xFFC084FC), // Lavender Neon
        Color(0xFFFB923C), // Sunset Orange
        Color(0xFF2DD4BF)  // Neon Mint
    )

    val activeColors = if (paperType == CanvasPaperType.DARK_CANVAS) neonChalks else classicInks

    // Auto-adjust default color when switching paper modes
    LaunchedEffect(paperType) {
        if (paperType == CanvasPaperType.DARK_CANVAS && selectedColor == Color(0xFF0F172A)) {
            selectedColor = Color.White
        } else if (paperType != CanvasPaperType.DARK_CANVAS && selectedColor == Color.White) {
            selectedColor = Color(0xFF0F172A)
        }
    }

    // Helper: Build Path from list of points
    fun createPathFromPoints(pts: List<StrokePoint>): Path {
        val path = Path()
        if (pts.isEmpty()) return path
        path.moveTo(pts.first().x, pts.first().y)
        for (i in 1 until pts.size) {
            path.lineTo(pts[i].x, pts[i].y)
        }
        return path
    }

    // Helper: Build Geometric Shape Path
    fun createShapePath(shape: ShapeType, start: StrokePoint, end: StrokePoint): Path {
        val path = Path()
        val minX = minOf(start.x, end.x)
        val maxX = maxOf(start.x, end.x)
        val minY = minOf(start.y, end.y)
        val maxY = maxOf(start.y, end.y)

        when (shape) {
            ShapeType.LINE -> {
                path.moveTo(start.x, start.y)
                path.lineTo(end.x, end.y)
            }
            ShapeType.ARROW -> {
                path.moveTo(start.x, start.y)
                path.lineTo(end.x, end.y)
                // Arrowhead calculation
                val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
                val arrowLength = 28f
                val arrowAngle = Math.toRadians(30.0)

                val x1 = end.x - arrowLength * cos(angle - arrowAngle).toFloat()
                val y1 = end.y - arrowLength * sin(angle - arrowAngle).toFloat()
                val x2 = end.x - arrowLength * cos(angle + arrowAngle).toFloat()
                val y2 = end.y - arrowLength * sin(angle + arrowAngle).toFloat()

                path.moveTo(end.x, end.y)
                path.lineTo(x1, y1)
                path.moveTo(end.x, end.y)
                path.lineTo(x2, y2)
            }
            ShapeType.RECTANGLE, ShapeType.FILLED_RECTANGLE -> {
                path.addRect(Rect(minX, minY, maxX, maxY))
            }
            ShapeType.CIRCLE, ShapeType.FILLED_CIRCLE -> {
                path.addOval(Rect(minX, minY, maxX, maxY))
            }
            ShapeType.TRIANGLE -> {
                val topX = (minX + maxX) / 2f
                path.moveTo(topX, minY)
                path.lineTo(maxX, maxY)
                path.lineTo(minX, maxY)
                path.close()
            }
            ShapeType.COORDINATE_AXIS -> {
                val originX = minX + 40f
                val originY = maxY - 40f

                // X Axis
                path.moveTo(originX, originY)
                path.lineTo(maxX, originY)
                path.lineTo(maxX - 16f, originY - 12f)
                path.moveTo(maxX, originY)
                path.lineTo(maxX - 16f, originY + 12f)

                // Y Axis
                path.moveTo(originX, originY)
                path.lineTo(originX, minY)
                path.lineTo(originX - 12f, minY + 16f)
                path.moveTo(originX, originY)
                path.lineTo(originX + 12f, minY + 16f)
            }
            else -> {}
        }
        return path
    }

    // Bounding Box Calculation for PNG Export
    fun calculateExportBoundingBox(): RectF {
        if (strokes.isEmpty()) return RectF(0f, 0f, 1080f, 1920f)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        strokes.forEach { stroke ->
            stroke.points.forEach { pt ->
                if (pt.x < minX) minX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
            }
        }

        val padding = 90f
        val calculatedWidth = (maxX - minX + padding * 2).coerceAtLeast(800f)
        val calculatedHeight = (maxY - minY + padding * 2).coerceAtLeast(800f)

        return RectF(
            minX - padding,
            minY - padding,
            minX - padding + calculatedWidth,
            minY - padding + calculatedHeight
        )
    }

    // High Resolution Image Bitmap Export
    fun exportCanvasToImage() {
        try {
            val imagesDir = File(context.filesDir, "notebook_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val fileName = "sketch_${UUID.randomUUID()}.png"
            val file = File(imagesDir, fileName)

            val bounds = calculateExportBoundingBox()
            val width = bounds.width().toInt().coerceIn(600, 2400)
            val height = bounds.height().toInt().coerceIn(600, 3200)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val bgColor = if (paperType == CanvasPaperType.DARK_CANVAS) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.WHITE
            canvas.drawColor(bgColor)

            val originX = -bounds.left
            val originY = -bounds.top

            // Draw Paper Grid Lines
            val linePaint = Paint().apply {
                color = if (paperType == CanvasPaperType.DARK_CANVAS) android.graphics.Color.parseColor("#1E293B") else android.graphics.Color.parseColor("#E2E8F0")
                strokeWidth = 2f
            }

            if (paperType == CanvasPaperType.LINED) {
                var y = (originY % 60f)
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
                    y += 60f
                }
            } else if (paperType == CanvasPaperType.GRID) {
                var pos = 0f
                while (pos < maxOf(width, height)) {
                    if (pos < height) canvas.drawLine(0f, pos, width.toFloat(), pos, linePaint)
                    if (pos < width) canvas.drawLine(pos, 0f, pos, height.toFloat(), linePaint)
                    pos += 60f
                }
            }

            // Draw All Strokes & Shapes
            strokes.forEach { stroke ->
                val strokePath = android.graphics.Path()

                if (stroke.shapeType != ShapeType.NONE && stroke.points.size >= 2) {
                    val s = stroke.points.first()
                    val e = stroke.points.last()
                    val p = createShapePath(stroke.shapeType, StrokePoint(s.x + originX, s.y + originY), StrokePoint(e.x + originX, e.y + originY))
                    strokePath.addPath(p.asAndroidPath())
                } else if (stroke.points.isNotEmpty()) {
                    strokePath.moveTo(stroke.points.first().x + originX, stroke.points.first().y + originY)
                    for (i in 1 until stroke.points.size) {
                        strokePath.lineTo(stroke.points[i].x + originX, stroke.points[i].y + originY)
                    }
                }

                val isFilledShape = stroke.shapeType == ShapeType.FILLED_RECTANGLE || stroke.shapeType == ShapeType.FILLED_CIRCLE
                val paint = Paint().apply {
                    color = if (stroke.brushStyle == BrushStyle.ERASER) bgColor else stroke.color.toArgb()
                    style = if (isFilledShape) Paint.Style.FILL else Paint.Style.STROKE
                    strokeWidth = stroke.strokeWidth * 1.5f
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                    if (stroke.brushStyle == BrushStyle.HIGHLIGHTER) {
                        alpha = 90
                    }
                }
                canvas.drawPath(strokePath, paint)
            }

            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

            onSave(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top App Bar (Hidden in Immersive Fullscreen)
                    AnimatedVisibility(visible = !isFullScreenImmersive) {
                        TopAppBar(
                            title = {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Full Screen Canvas & Studio".tr,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "${(scaleZoom * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Brushes • Geometric Shapes • Stylus & Touch Canvas".tr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                            val last = strokes.last()
                                            strokes = strokes.dropLast(1)
                                            redoStrokes = redoStrokes + last
                                        }
                                    },
                                    enabled = strokes.isNotEmpty()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo".tr)
                                }

                                IconButton(
                                    onClick = {
                                        if (redoStrokes.isNotEmpty()) {
                                            val lastRedo = redoStrokes.last()
                                            redoStrokes = redoStrokes.dropLast(1)
                                            strokes = strokes + lastRedo
                                        }
                                    },
                                    enabled = redoStrokes.isNotEmpty()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo".tr)
                                }

                                IconButton(
                                    onClick = {
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                        scaleZoom = 1.0f
                                    }
                                ) {
                                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Center View".tr)
                                }

                                IconButton(onClick = { isFullScreenImmersive = true }) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = "Immersive Fullscreen".tr)
                                }

                                IconButton(onClick = {
                                    strokes = emptyList()
                                    redoStrokes = emptyList()
                                }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Canvas".tr)
                                }

                                Button(
                                    onClick = { exportCanvasToImage() },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.padding(end = 8.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Note".tr, fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    }

                    // Main Tool Control Studio Bar (Hidden in Immersive Fullscreen)
                    AnimatedVisibility(visible = !isFullScreenImmersive) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                // Row 1: Brush Styles & Tools Selection
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        FilterChip(
                                            selected = activeBrush == BrushStyle.FOUNTAIN_PEN && activeShape == ShapeType.NONE,
                                            onClick = {
                                                activeBrush = BrushStyle.FOUNTAIN_PEN
                                                activeShape = ShapeType.NONE
                                            },
                                            label = { Text("Fountain Pen".tr) },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeBrush == BrushStyle.HIGHLIGHTER && activeShape == ShapeType.NONE,
                                            onClick = {
                                                activeBrush = BrushStyle.HIGHLIGHTER
                                                activeShape = ShapeType.NONE
                                            },
                                            label = { Text("Highlighter".tr) },
                                            leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeBrush == BrushStyle.CALLIGRAPHY && activeShape == ShapeType.NONE,
                                            onClick = {
                                                activeBrush = BrushStyle.CALLIGRAPHY
                                                activeShape = ShapeType.NONE
                                            },
                                            label = { Text("Calligraphy".tr) },
                                            leadingIcon = { Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeBrush == BrushStyle.GLOW_CHALK && activeShape == ShapeType.NONE,
                                            onClick = {
                                                activeBrush = BrushStyle.GLOW_CHALK
                                                activeShape = ShapeType.NONE
                                            },
                                            label = { Text("Glow Chalk".tr) },
                                            leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeBrush == BrushStyle.CRAYON && activeShape == ShapeType.NONE,
                                            onClick = {
                                                activeBrush = BrushStyle.CRAYON
                                                activeShape = ShapeType.NONE
                                            },
                                            label = { Text("Crayon".tr) },
                                            leadingIcon = { Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeBrush == BrushStyle.ERASER,
                                            onClick = {
                                                activeBrush = BrushStyle.ERASER
                                                activeShape = ShapeType.NONE
                                            },
                                            label = { Text("Eraser".tr) },
                                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeBrush == BrushStyle.HAND_PAN,
                                            onClick = {
                                                activeBrush = BrushStyle.HAND_PAN
                                                activeShape = ShapeType.NONE
                                            },
                                            label = { Text("Pan Hand".tr) },
                                            leadingIcon = { Icon(Icons.Default.PanTool, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Row 2: Geometric Shapes Bar
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        Text(
                                            text = "Shapes:".tr,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeShape == ShapeType.LINE,
                                            onClick = { activeShape = if (activeShape == ShapeType.LINE) ShapeType.NONE else ShapeType.LINE },
                                            label = { Text("Line".tr) },
                                            leadingIcon = { Icon(Icons.Default.HorizontalRule, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeShape == ShapeType.ARROW,
                                            onClick = { activeShape = if (activeShape == ShapeType.ARROW) ShapeType.NONE else ShapeType.ARROW },
                                            label = { Text("Arrow".tr) },
                                            leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeShape == ShapeType.RECTANGLE,
                                            onClick = { activeShape = if (activeShape == ShapeType.RECTANGLE) ShapeType.NONE else ShapeType.RECTANGLE },
                                            label = { Text("Rectangle".tr) },
                                            leadingIcon = { Icon(Icons.Default.CropSquare, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeShape == ShapeType.FILLED_RECTANGLE,
                                            onClick = { activeShape = if (activeShape == ShapeType.FILLED_RECTANGLE) ShapeType.NONE else ShapeType.FILLED_RECTANGLE },
                                            label = { Text("Fill Box".tr) },
                                            leadingIcon = { Icon(Icons.Default.Square, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeShape == ShapeType.CIRCLE,
                                            onClick = { activeShape = if (activeShape == ShapeType.CIRCLE) ShapeType.NONE else ShapeType.CIRCLE },
                                            label = { Text("Circle".tr) },
                                            leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeShape == ShapeType.FILLED_CIRCLE,
                                            onClick = { activeShape = if (activeShape == ShapeType.FILLED_CIRCLE) ShapeType.NONE else ShapeType.FILLED_CIRCLE },
                                            label = { Text("Fill Circle".tr) },
                                            leadingIcon = { Icon(Icons.Default.Circle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeShape == ShapeType.TRIANGLE,
                                            onClick = { activeShape = if (activeShape == ShapeType.TRIANGLE) ShapeType.NONE else ShapeType.TRIANGLE },
                                            label = { Text("Triangle".tr) },
                                            leadingIcon = { Icon(Icons.Default.ChangeHistory, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }

                                    item {
                                        FilterChip(
                                            selected = activeShape == ShapeType.COORDINATE_AXIS,
                                            onClick = { activeShape = if (activeShape == ShapeType.COORDINATE_AXIS) ShapeType.NONE else ShapeType.COORDINATE_AXIS },
                                            label = { Text("X-Y Axes".tr) },
                                            leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Row 3: Colors & Thickness Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Colors Swatches + Picker
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(activeColors) { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .clickable { selectedColor = color }
                                                    .then(
                                                        if (selectedColor == color) {
                                                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                        } else Modifier
                                                    )
                                            )
                                        }

                                        item {
                                            IconButton(
                                                onClick = { showColorPickerDialog = true },
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                            ) {
                                                Icon(
                                                    Icons.Default.Palette,
                                                    contentDescription = "Custom Color Picker".tr,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }

                                    // Thickness Slider & Palm Rejection / Paper Style
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Slider(
                                            value = penThickness,
                                            onValueChange = { penThickness = it },
                                            valueRange = 2f..36f,
                                            modifier = Modifier.width(120.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                paperType = when (paperType) {
                                                    CanvasPaperType.LINED -> CanvasPaperType.GRID
                                                    CanvasPaperType.GRID -> CanvasPaperType.DOT_GRID
                                                    CanvasPaperType.DOT_GRID -> CanvasPaperType.BLANK
                                                    CanvasPaperType.BLANK -> CanvasPaperType.DARK_CANVAS
                                                    CanvasPaperType.DARK_CANVAS -> CanvasPaperType.LINED
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = when (paperType) {
                                                    CanvasPaperType.LINED -> Icons.Default.ViewAgenda
                                                    CanvasPaperType.GRID -> Icons.Default.GridOn
                                                    CanvasPaperType.DOT_GRID -> Icons.Default.Grain
                                                    CanvasPaperType.BLANK -> Icons.Default.CropSquare
                                                    CanvasPaperType.DARK_CANVAS -> Icons.Default.DarkMode
                                                },
                                                contentDescription = "Paper Style".tr,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Main Drawing Surface
                    val canvasBgColor = if (paperType == CanvasPaperType.DARK_CANVAS) Color(0xFF0F172A) else Color.White

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(canvasBgColor)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scaleZoom = (scaleZoom * zoom).coerceIn(0.25f, 5.0f)
                                    panOffsetX += pan.x
                                    panOffsetY += pan.y
                                }
                            }
                            .pointerInput(activeBrush, activeShape, selectedColor, penThickness, eraserThickness, scaleZoom, panOffsetX, panOffsetY, palmRejectionOnlyStylus) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (activeBrush == BrushStyle.HAND_PAN) return@detectDragGestures

                                        val canvasX = (offset.x - panOffsetX) / scaleZoom
                                        val canvasY = (offset.y - panOffsetY) / scaleZoom
                                        val pt = StrokePoint(canvasX, canvasY)

                                        if (activeShape != ShapeType.NONE) {
                                            shapeStartPoint = pt
                                            shapeCurrentPoint = pt
                                        } else {
                                            currentPoints = listOf(pt)
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (activeBrush == BrushStyle.HAND_PAN) {
                                            panOffsetX += dragAmount.x
                                            panOffsetY += dragAmount.y
                                            change.consume()
                                            return@detectDragGestures
                                        }

                                        if (palmRejectionOnlyStylus && change.type != PointerType.Stylus) {
                                            panOffsetX += dragAmount.x
                                            panOffsetY += dragAmount.y
                                            change.consume()
                                            return@detectDragGestures
                                        }

                                        change.consume()
                                        val canvasX = (change.position.x - panOffsetX) / scaleZoom
                                        val canvasY = (change.position.y - panOffsetY) / scaleZoom
                                        val pt = StrokePoint(canvasX, canvasY)

                                        if (activeShape != ShapeType.NONE) {
                                            shapeCurrentPoint = pt
                                        } else {
                                            currentPoints = currentPoints + pt
                                        }
                                    },
                                    onDragEnd = {
                                        if (activeShape != ShapeType.NONE && shapeStartPoint != null && shapeCurrentPoint != null) {
                                            val newStroke = DrawnStroke(
                                                points = listOf(shapeStartPoint!!, shapeCurrentPoint!!),
                                                color = selectedColor,
                                                strokeWidth = penThickness,
                                                brushStyle = activeBrush,
                                                shapeType = activeShape,
                                                alpha = 1.0f
                                            )
                                            strokes = strokes + newStroke
                                            redoStrokes = emptyList()
                                            shapeStartPoint = null
                                            shapeCurrentPoint = null
                                        } else if (currentPoints.isNotEmpty()) {
                                            val strokeWidth = when (activeBrush) {
                                                BrushStyle.HIGHLIGHTER -> penThickness * 2.5f
                                                BrushStyle.ERASER -> eraserThickness
                                                else -> penThickness
                                            }

                                            val newStroke = DrawnStroke(
                                                points = currentPoints,
                                                color = if (activeBrush == BrushStyle.ERASER) canvasBgColor else selectedColor,
                                                strokeWidth = strokeWidth,
                                                brushStyle = activeBrush,
                                                shapeType = ShapeType.NONE,
                                                alpha = if (activeBrush == BrushStyle.HIGHLIGHTER) 0.35f else 1.0f
                                            )

                                            strokes = strokes + newStroke
                                            redoStrokes = emptyList()
                                            currentPoints = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            withTransform({
                                translate(panOffsetX, panOffsetY)
                                scale(scaleZoom, scaleZoom, pivot = Offset.Zero)
                            }) {
                                val gridLineColor = if (paperType == CanvasPaperType.DARK_CANVAS) Color(0xFF1E293B) else Color(0xFFE2E8F0)

                                when (paperType) {
                                    CanvasPaperType.LINED -> {
                                        var y = -2000f
                                        while (y < 6000f) {
                                            drawLine(color = gridLineColor, start = Offset(-2000f, y), end = Offset(6000f, y), strokeWidth = 1.5f)
                                            y += 60f
                                        }
                                    }
                                    CanvasPaperType.GRID -> {
                                        var pos = -2000f
                                        while (pos < 6000f) {
                                            drawLine(color = gridLineColor, start = Offset(-2000f, pos), end = Offset(6000f, pos), strokeWidth = 1.2f)
                                            drawLine(color = gridLineColor, start = Offset(pos, -2000f), end = Offset(pos, 6000f), strokeWidth = 1.2f)
                                            pos += 60f
                                        }
                                    }
                                    CanvasPaperType.DOT_GRID -> {
                                        var x = -2000f
                                        while (x < 6000f) {
                                            var y = -2000f
                                            while (y < 6000f) {
                                                drawCircle(color = gridLineColor, radius = 2f, center = Offset(x, y))
                                                y += 60f
                                            }
                                            x += 60f
                                        }
                                    }
                                    else -> {}
                                }

                                // Render Saved Strokes & Shapes
                                strokes.forEach { stroke ->
                                    val isFilled = stroke.shapeType == ShapeType.FILLED_RECTANGLE || stroke.shapeType == ShapeType.FILLED_CIRCLE
                                    val path = if (stroke.shapeType != ShapeType.NONE && stroke.points.size >= 2) {
                                        createShapePath(stroke.shapeType, stroke.points.first(), stroke.points.last())
                                    } else {
                                        createPathFromPoints(stroke.points)
                                    }

                                    val capStyle = when (stroke.brushStyle) {
                                        BrushStyle.CALLIGRAPHY, BrushStyle.HIGHLIGHTER -> StrokeCap.Square
                                        else -> StrokeCap.Round
                                    }

                                    val joinStyle = when (stroke.brushStyle) {
                                        BrushStyle.CALLIGRAPHY -> StrokeJoin.Miter
                                        else -> StrokeJoin.Round
                                    }

                                    val pathEffect = when (stroke.brushStyle) {
                                        BrushStyle.CRAYON -> PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                        else -> null
                                    }

                                    drawPath(
                                        path = path,
                                        color = if (stroke.brushStyle == BrushStyle.HIGHLIGHTER) stroke.color.copy(alpha = 0.35f) else stroke.color,
                                        style = if (isFilled) {
                                            androidx.compose.ui.graphics.drawscope.Fill
                                        } else {
                                            Stroke(
                                                width = stroke.strokeWidth,
                                                cap = capStyle,
                                                join = joinStyle,
                                                pathEffect = pathEffect
                                            )
                                        }
                                    )
                                }

                                // Render Current Freehand Drawing Stroke
                                if (currentPoints.isNotEmpty()) {
                                    val activePath = createPathFromPoints(currentPoints)
                                    val strokeWidth = when (activeBrush) {
                                        BrushStyle.HIGHLIGHTER -> penThickness * 2.5f
                                        BrushStyle.ERASER -> eraserThickness
                                        else -> penThickness
                                    }
                                    drawPath(
                                        path = activePath,
                                        color = if (activeBrush == BrushStyle.ERASER) canvasBgColor else if (activeBrush == BrushStyle.HIGHLIGHTER) selectedColor.copy(alpha = 0.35f) else selectedColor,
                                        style = Stroke(
                                            width = strokeWidth,
                                            cap = if (activeBrush == BrushStyle.CALLIGRAPHY) StrokeCap.Square else StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }

                                // Render Live Geometric Shape Preview while Dragging
                                if (activeShape != ShapeType.NONE && shapeStartPoint != null && shapeCurrentPoint != null) {
                                    val previewPath = createShapePath(activeShape, shapeStartPoint!!, shapeCurrentPoint!!)
                                    val isFilled = activeShape == ShapeType.FILLED_RECTANGLE || activeShape == ShapeType.FILLED_CIRCLE

                                    drawPath(
                                        path = previewPath,
                                        color = selectedColor,
                                        style = if (isFilled) {
                                            androidx.compose.ui.graphics.drawscope.Fill
                                        } else {
                                            Stroke(
                                                width = penThickness,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Floating Immersive Overlay Controls
                        if (isFullScreenImmersive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        tonalElevation = 6.dp
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            IconButton(onClick = { isFullScreenImmersive = false }) {
                                                Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Immersive Mode".tr)
                                            }
                                            IconButton(onClick = { exportCanvasToImage() }) {
                                                Icon(Icons.Default.Check, contentDescription = "Save Note".tr, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Floating Zoom Controls
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { scaleZoom = (scaleZoom * 0.8f).coerceAtLeast(0.25f) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out".tr, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "${(scaleZoom * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(onClick = { scaleZoom = (scaleZoom * 1.25f).coerceAtMost(5.0f) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "Zoom In".tr, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Color Spectrum Picker Dialog
    if (showColorPickerDialog) {
        CustomColorPickerDialog(
            initialColor = selectedColor,
            onColorSelected = {
                selectedColor = it
                showColorPickerDialog = false
            },
            onDismiss = { showColorPickerDialog = false }
        )
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    val currentColor = Color(red, green, blue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Ink Color Picker".tr, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Color Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )

                Column {
                    Text("Red (${(red * 255).toInt()})", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                    Slider(value = red, onValueChange = { red = it }, valueRange = 0f..1f)
                }

                Column {
                    Text("Green (${(green * 255).toInt()})", style = MaterialTheme.typography.labelSmall, color = Color.Green)
                    Slider(value = green, onValueChange = { green = it }, valueRange = 0f..1f)
                }

                Column {
                    Text("Blue (${(blue * 255).toInt()})", style = MaterialTheme.typography.labelSmall, color = Color.Blue)
                    Slider(value = blue, onValueChange = { blue = it }, valueRange = 0f..1f)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(currentColor) }) {
                Text("Select Color".tr)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel".tr)
            }
        }
    )
}
