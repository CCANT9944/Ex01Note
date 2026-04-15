package com.example.ex01.ui.editor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
val EraserIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Eraser",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(16.24f, 3.56f)
            lineTo(21.19f, 8.5f)
            curveTo(21.97f, 9.29f, 21.97f, 10.65f, 21.19f, 11.44f)
            lineTo(12.0f, 20.63f)
            curveTo(11.6f, 21.0f, 11.1f, 21.2f, 10.58f, 21.2f)
            horizontalLineTo(2.0f)
            verticalLineTo(19.2f)
            horizontalLineTo(8.38f)
            lineTo(16.24f, 3.56f)
            moveTo(11.17f, 17.0f)
            lineTo(19.08f, 9.08f)
            lineTo(15.65f, 5.65f)
            lineTo(7.74f, 13.57f)
            lineTo(11.17f, 17.0f)
            close()
        }
    }.build()

val TextIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Text",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(5f, 4f)
            verticalLineTo(7f)
            horizontalLineTo(10.5f)
            verticalLineTo(19f)
            horizontalLineTo(13.5f)
            verticalLineTo(7f)
            horizontalLineTo(19f)
            verticalLineTo(4f)
            close()
        }
    }.build()

val HighlighterIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Highlighter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(17.75f, 7.0f)
            lineTo(14.0f, 3.25f)
            lineTo(15.4f, 1.85f)
            curveTo(15.79f, 1.46f, 16.42f, 1.46f, 16.81f, 1.85f)
            lineTo(19.15f, 4.19f)
            curveTo(19.54f, 4.58f, 19.54f, 5.21f, 19.15f, 5.6f)
            lineTo(17.75f, 7.0f)
            moveTo(12.6f, 8.4f)
            lineTo(16.35f, 12.15f)
            lineTo(7.15f, 21.35f)
            lineTo(3.4f, 17.6f)
            lineTo(12.6f, 8.4f)
            moveTo(2.0f, 21.2f)
            horizontalLineTo(22.0f)
            verticalLineTo(23.2f)
            horizontalLineTo(2.0f)
            verticalLineTo(21.2f)
            close()
        }
    }.build()

val LassoIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Lasso",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(14f, 13f)
            curveTo(17f, 13f, 19f, 11f, 19f, 8f)
            curveTo(19f, 5f, 16f, 3f, 12f, 3f)
            curveTo(8f, 3f, 5f, 5f, 5f, 8f)
            curveTo(5f, 11f, 7f, 13f, 10f, 13f)
            lineTo(10f, 21f)
            horizontalLineTo(14f)
            lineTo(14f, 13f)
            close()
            moveTo(12f, 5f)
            curveTo(14f, 5f, 16f, 6.5f, 16f, 8f)
            curveTo(16f, 9.5f, 14f, 11f, 12f, 11f)
            curveTo(10f, 11f, 8f, 9.5f, 8f, 8f)
            curveTo(8f, 6.5f, 10f, 5f, 12f, 5f)
            close()
        }
    }.build()
