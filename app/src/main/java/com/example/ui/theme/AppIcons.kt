package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val Add = Icons.Default.Add
    val Check = Icons.Default.Check
    val Delete = Icons.Default.Delete
    val Edit = Icons.Default.Edit
    val Info = Icons.Default.Info
    val Person = Icons.Default.Person
    val Search = Icons.Default.Search
    val Settings = Icons.Default.Settings
    val Share = Icons.Default.Share
    val Warning = Icons.Default.Warning
    val ArrowBack = Icons.AutoMirrored.Filled.ArrowBack

    // Custom vector paths for schedule, file, camera, import, export
    val Schedule: ImageVector = ImageVector.Builder(
        name = "Schedule",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            curveTo(2.0f, 17.52f, 6.47f, 22.0f, 11.99f, 22.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            curveTo(22.0f, 6.48f, 17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveTo(7.58f, 20.0f, 4.0f, 16.42f, 4.0f, 12.0f)
            curveTo(4.0f, 7.58f, 7.58f, 4.0f, 12.0f, 4.0f)
            curveTo(16.42f, 4.0f, 20.0f, 7.58f, 20.0f, 12.0f)
            curveTo(20.0f, 16.42f, 16.42f, 20.0f, 12.0f, 20.0f)
            close()
            moveTo(12.5f, 7.0f)
            horizontalLineTo(11.0f)
            verticalLineTo(13.0f)
            lineTo(16.25f, 16.15f)
            lineTo(17.0f, 14.92f)
            lineTo(12.5f, 12.25f)
            verticalLineTo(7.0f)
            close()
        }
    }.build()

    val Courses: ImageVector = ImageVector.Builder(
        name = "Courses",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(14.0f, 2.0f)
            horizontalLineTo(6.0f)
            curveTo(4.9f, 2.0f, 4.01f, 2.9f, 4.01f, 4.0f)
            lineTo(4.0f, 20.0f)
            curveTo(4.0f, 21.1f, 4.89f, 22.0f, 5.99f, 22.0f)
            horizontalLineTo(18.0f)
            curveTo(19.1f, 22.0f, 20.0f, 21.1f, 20.0f, 20.0f)
            verticalLineTo(8.0f)
            lineTo(14.0f, 2.0f)
            close()
            moveTo(16.0f, 18.0f)
            horizontalLineTo(8.0f)
            verticalLineTo(16.0f)
            horizontalLineTo(16.0f)
            verticalLineTo(18.0f)
            close()
            moveTo(16.0f, 14.0f)
            horizontalLineTo(8.0f)
            verticalLineTo(12.0f)
            horizontalLineTo(16.0f)
            verticalLineTo(14.0f)
            close()
            moveTo(13.0f, 9.0f)
            verticalLineTo(3.5f)
            lineTo(18.5f, 9.0f)
            horizontalLineTo(13.0f)
            close()
        }
    }.build()

    val FileDownload: ImageVector = ImageVector.Builder(
        name = "FileDownload",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.0f, 9.0f)
            horizontalLineTo(15.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(9.0f)
            verticalLineTo(9.0f)
            horizontalLineTo(5.0f)
            lineTo(12.0f, 16.0f)
            lineTo(19.0f, 9.0f)
            close()
            moveTo(5.0f, 18.0f)
            verticalLineTo(20.0f)
            horizontalLineTo(19.0f)
            verticalLineTo(18.0f)
            horizontalLineTo(5.0f)
            close()
        }
    }.build()

    val FileUpload: ImageVector = ImageVector.Builder(
        name = "FileUpload",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(9.0f, 16.0f)
            horizontalLineTo(15.0f)
            verticalLineTo(10.0f)
            horizontalLineTo(19.0f)
            lineTo(12.0f, 3.0f)
            lineTo(5.0f, 10.0f)
            horizontalLineTo(9.0f)
            verticalLineTo(16.0f)
            close()
            moveTo(5.0f, 18.0f)
            verticalLineTo(20.0f)
            horizontalLineTo(19.0f)
            verticalLineTo(18.0f)
            horizontalLineTo(5.0f)
            close()
        }
    }.build()

    val Camera: ImageVector = ImageVector.Builder(
        name = "Camera",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(9.0f, 2.0f)
            lineTo(7.17f, 4.0f)
            horizontalLineTo(4.0f)
            curveTo(2.9f, 4.0f, 2.0f, 4.9f, 2.0f, 6.0f)
            verticalLineTo(18.0f)
            curveTo(2.0f, 19.1f, 2.9f, 20.0f, 4.0f, 20.0f)
            horizontalLineTo(20.0f)
            curveTo(21.1f, 20.0f, 22.0f, 19.1f, 22.0f, 18.0f)
            verticalLineTo(6.0f)
            curveTo(22.0f, 4.9f, 21.1f, 4.0f, 20.0f, 4.0f)
            horizontalLineTo(16.83f)
            lineTo(15.0f, 2.0f)
            horizontalLineTo(9.0f)
            close()
            moveTo(12.0f, 17.0f)
            curveTo(9.24f, 17.0f, 7.0f, 14.76f, 7.0f, 12.0f)
            curveTo(7.0f, 9.24f, 9.24f, 7.0f, 12.0f, 7.0f)
            curveTo(14.76f, 7.0f, 17.0f, 9.24f, 17.0f, 12.0f)
            curveTo(17.0f, 14.76f, 14.76f, 17.0f, 12.0f, 17.0f)
            close()
        }
    }.build()

    val Copy: ImageVector = ImageVector.Builder(
        name = "Copy",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(16.0f, 1.0f)
            horizontalLineTo(4.0f)
            curveTo(2.9f, 1.0f, 2.0f, 1.9f, 2.0f, 3.0f)
            verticalLineTo(17.0f)
            horizontalLineTo(4.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(16.0f)
            verticalLineTo(1.0f)
            close()
            moveTo(19.0f, 5.0f)
            horizontalLineTo(8.0f)
            curveTo(6.9f, 5.0f, 6.0f, 5.9f, 6.0f, 7.0f)
            verticalLineTo(21.0f)
            curveTo(6.0f, 22.1f, 6.9f, 23.0f, 8.0f, 23.0f)
            horizontalLineTo(19.0f)
            curveTo(20.1f, 23.0f, 21.0f, 22.1f, 21.0f, 21.0f)
            verticalLineTo(7.0f)
            curveTo(21.0f, 5.9f, 20.1f, 5.0f, 19.0f, 5.0f)
            close()
            moveTo(19.0f, 21.0f)
            horizontalLineTo(8.0f)
            verticalLineTo(7.0f)
            horizontalLineTo(19.0f)
            verticalLineTo(21.0f)
            close()
        }
    }.build()

    val Paste: ImageVector = ImageVector.Builder(
        name = "Paste",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.0f, 2.0f)
            horizontalLineTo(14.82f)
            curveTo(14.4f, 0.84f, 13.3f, 0.0f, 12.0f, 0.0f)
            curveTo(10.7f, 0.0f, 9.6f, 0.84f, 9.18f, 2.0f)
            horizontalLineTo(5.0f)
            curveTo(3.9f, 2.0f, 3.0f, 2.9f, 3.0f, 4.0f)
            verticalLineTo(20.0f)
            curveTo(3.0f, 21.1f, 3.9f, 22.0f, 5.0f, 22.0f)
            horizontalLineTo(19.0f)
            curveTo(20.1f, 22.0f, 21.0f, 21.1f, 21.0f, 20.0f)
            verticalLineTo(4.0f)
            curveTo(21.0f, 2.9f, 20.1f, 2.0f, 19.0f, 2.0f)
            close()
            moveTo(12.0f, 2.0f)
            curveTo(12.55f, 2.0f, 13.0f, 2.45f, 13.0f, 3.0f)
            curveTo(13.0f, 3.55f, 12.55f, 4.0f, 12.0f, 4.0f)
            curveTo(11.45f, 4.0f, 11.0f, 3.55f, 11.0f, 3.0f)
            curveTo(11.0f, 2.45f, 11.45f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(19.0f, 20.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(4.0f)
            horizontalLineTo(7.0f)
            verticalLineTo(6.0f)
            horizontalLineTo(17.0f)
            verticalLineTo(4.0f)
            horizontalLineTo(19.0f)
            verticalLineTo(20.0f)
            close()
        }
    }.build()

    val Image: ImageVector = ImageVector.Builder(
        name = "Image",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(21.0f, 19.0f)
            verticalLineTo(5.0f)
            curveTo(21.0f, 3.9f, 20.1f, 3.0f, 19.0f, 3.0f)
            horizontalLineTo(5.0f)
            curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
            verticalLineTo(19.0f)
            curveTo(3.0f, 20.1f, 3.9f, 21.0f, 5.0f, 21.0f)
            horizontalLineTo(19.0f)
            curveTo(20.1f, 21.0f, 21.0f, 20.1f, 21.0f, 19.0f)
            close()
            moveTo(8.5f, 13.5f)
            lineTo(11.0f, 16.51f)
            lineTo(14.5f, 12.0f)
            lineTo(19.0f, 18.0f)
            horizontalLineTo(5.0f)
            lineTo(8.5f, 13.5f)
            close()
        }
    }.build()
}
