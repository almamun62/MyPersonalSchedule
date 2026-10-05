package com.example.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class PaperStyle {
    BLANK,
    LINED,
    GRID,
    DOT_GRID,
    ISOMETRIC,
    DARK_OLED
}

@Serializable
enum class BrushStyle {
    PEN,
    FOUNTAIN_PEN,
    GEL_PEN,
    HIGHLIGHTER,
    CALLIGRAPHY,
    CRAYON,
    ERASER,
    LASSO
}

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class ShapeType {
    FREEHAND,
    LINE,
    RECTANGLE,
    CIRCLE,
    ELLIPSE,
    TRIANGLE,
    ARROW,
    COORDINATE_AXIS
}

@Serializable
data class VectorStroke(
    val id: String = java.util.UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val colorHex: String = "#000000",
    val baseWidthDp: Float = 3.0f,
    val brushStyle: BrushStyle = BrushStyle.PEN,
    val shapeType: ShapeType = ShapeType.FREEHAND,
    val layerId: String = "layer_1",
    val opacity: Float = 1.0f
)

@Serializable
data class LayerModel(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f
)
