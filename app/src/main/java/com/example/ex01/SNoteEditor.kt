package com.example.ex01

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class DrawingLine(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f,
    val isEraser: Boolean = false
) {
    fun toPath(): Path {
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
        }
        return path
    }
}

fun serializeDrawing(lines: List<DrawingLine>): String {
    val jsonArray = JSONArray()
    for (line in lines) {
        val pointArray = JSONArray()
        for (point in line.points) {
            val pObj = JSONObject()
            pObj.put("x", point.x)
            pObj.put("y", point.y)
            pointArray.put(pObj)
        }
        val lineObj = JSONObject()
        lineObj.put("points", pointArray)
        lineObj.put("color", line.color.value.toLong())
        lineObj.put("stroke", line.strokeWidth.toDouble()) // Float cast
        lineObj.put("isEraser", line.isEraser)
        jsonArray.put(lineObj)
    }
    return jsonArray.toString()
}

fun deserializeDrawing(json: String): List<DrawingLine> {
    if (json.isBlank()) return emptyList()
    val lines = mutableListOf<DrawingLine>()
    try {
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val lineObj = jsonArray.getJSONObject(i)
            val pointArray = lineObj.getJSONArray("points")
            val points = mutableListOf<Offset>()
            for (p in 0 until pointArray.length()) {
                val pObj = pointArray.getJSONObject(p)
                points.add(Offset(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
            }
            val color = Color(lineObj.optLong("color", Color.Black.value.toLong()).toULong())
            val stroke = lineObj.optDouble("stroke", 5.0).toFloat()
            val isEraser = lineObj.optBoolean("isEraser", false)
            
            // Heuristic for old lines without isEraser flag: old eraser lines either matched the default M3 surface colors or were much thicker (min eraser thickness is 20f, max pen is 12f).
            val inferredEraser = isEraser || (!lineObj.has("isEraser") && (stroke >= 20f || color == Color(0xFFFFFBFE) || color == Color(0xFF1C1B1F) || color == Color(0xFF141218))) 
            
            lines.add(DrawingLine(points, color, stroke, inferredEraser))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return lines
}

private val EraserIcon: ImageVector
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

@Suppress("SpellCheckingInspection")
@Composable
fun SNoteEditor(
    serializedBody: String,
    onSerializedBodyChange: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("snote_settings", Context.MODE_PRIVATE) }

    val drawingLines = remember { mutableStateListOf<DrawingLine>() }
    var currentPath by remember { mutableStateOf<List<Offset>?>(null) }
    var currentProperties by remember { mutableStateOf(DrawingLine(emptyList())) }
    var isEraserMode by remember { mutableStateOf(false) }
    var currentThickness by remember { mutableFloatStateOf(prefs.getFloat("pen_thickness", 6f)) }
    var showThicknessMenu by remember { mutableStateOf(false) }
    var currentEraserThickness by remember { mutableFloatStateOf(prefs.getFloat("eraser_thickness", 40f)) }
    var showEraserThicknessMenu by remember { mutableStateOf(false) }

    fun updatePenThickness(t: Float) {
        currentThickness = t
        prefs.edit { putFloat("pen_thickness", t) }
    }

    fun updateEraserThickness(t: Float) {
        currentEraserThickness = t
        prefs.edit { putFloat("eraser_thickness", t) }
    }

    var pageCount by remember { mutableIntStateOf(1) }
    var pageHeightPx by remember { mutableFloatStateOf(0f) }
    var pageHeightDp by remember { mutableStateOf(0.dp) }

    LaunchedEffect(serializedBody, pageHeightPx) {
        if (drawingLines.isEmpty() && serializedBody.isNotBlank() && currentPath == null && pageHeightPx > 0f) {
            val lines = deserializeDrawing(serializedBody)
            drawingLines.addAll(lines)
            val maxY = lines.flatMap { it.points }.maxOfOrNull { it.y } ?: 0f
            if (maxY > 0) {
                val neededPages = kotlin.math.ceil((maxY / pageHeightPx).toDouble()).toInt()
                if (neededPages > pageCount) pageCount = neededPages
            }
        }
    }

    val commitChanges = {
        onSerializedBodyChange(serializeDrawing(drawingLines))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                IconButton(
                    onClick = {
                        if (!isEraserMode) {
                            showThicknessMenu = true
                        } else {
                            isEraserMode = false
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (!isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Icon(Icons.Default.Create, contentDescription = "Pen")
                }
                DropdownMenu(
                    expanded = showThicknessMenu,
                    onDismissRequest = { showThicknessMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Thin") }, onClick = { updatePenThickness(3f); showThicknessMenu = false })
                    DropdownMenuItem(text = { Text("Medium") }, onClick = { updatePenThickness(6f); showThicknessMenu = false })
                    DropdownMenuItem(text = { Text("Thick") }, onClick = { updatePenThickness(12f); showThicknessMenu = false })
                }
            }
            Box {
                IconButton(
                    onClick = {
                        if (isEraserMode) {
                            showEraserThicknessMenu = true
                        } else {
                            isEraserMode = true
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Icon(EraserIcon, contentDescription = "Eraser")
                }
                DropdownMenu(
                    expanded = showEraserThicknessMenu,
                    onDismissRequest = { showEraserThicknessMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Thin") }, onClick = { updateEraserThickness(20f); showEraserThicknessMenu = false })
                    DropdownMenuItem(text = { Text("Medium") }, onClick = { updateEraserThickness(40f); showEraserThicknessMenu = false })
                    DropdownMenuItem(text = { Text("Thick") }, onClick = { updateEraserThickness(80f); showEraserThicknessMenu = false })
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = {
                drawingLines.clear()
                commitChanges()
            }) {
                Text("Clear All")
            }
        }

        val strokeColor = MaterialTheme.colorScheme.onSurface
        val eraserColor = MaterialTheme.colorScheme.surface

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(8.dp),
            color = eraserColor
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableHeight = this.maxHeight
                val density = LocalDensity.current

                LaunchedEffect(availableHeight) {
                    if (pageHeightDp == 0.dp) {
                        pageHeightDp = availableHeight
                        pageHeightPx = with(density) { availableHeight.toPx() }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (pageHeightDp > 0.dp) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pageHeightDp * pageCount)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: continue
                                            val isStylus = change.type == PointerType.Stylus
                                            val isStylusEraser = change.type == PointerType.Eraser

                                            if (!isStylus && !isStylusEraser) continue

                                            // Consume the event so horizontal/vertical scroll doesn't intercept it while drawing
                                            change.consume()

                                            if (change.pressed) {
                                                if (currentPath == null) {
                                                    currentPath = listOf(change.position)
                                                    val actualEraserMode = isStylusEraser || isEraserMode
                                                    currentProperties = DrawingLine(
                                                        points = currentPath!!,
                                                        color = if (actualEraserMode) eraserColor else strokeColor,
                                                        strokeWidth = if (actualEraserMode) currentEraserThickness else currentThickness,
                                                        isEraser = actualEraserMode
                                                     )
                                                 } else {
                                                     currentPath = currentPath!! + change.position
                                                 }
                                            } else {
                                                if (currentPath != null) {
                                                    drawingLines.add(currentProperties.copy(points = currentPath!!))
                                                    currentPath = null
                                                    commitChanges()
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            // Fill whole background first
                            drawRect(color = eraserColor, size = size)

                            // Draw thick visual dividers to make pages look distinct
                            for (i in 1 until pageCount) {
                                val y = i * pageHeightPx
                                drawRect(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    topLeft = Offset(0f, y - 8.dp.toPx()),
                                    size = Size(size.width, 16.dp.toPx())
                                )
                                // Add a slight shadow/borderline effect
                                drawLine(
                                    color = Color.DarkGray,
                                    start = Offset(0f, y - 8.dp.toPx()),
                                    end = Offset(size.width, y - 8.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.DarkGray,
                                    start = Offset(0f, y + 8.dp.toPx()),
                                    end = Offset(size.width, y + 8.dp.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Draw page numbers at the bottom right
                            val textSizePx = 14.dp.toPx()
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = textSizePx
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                            for (i in 0 until pageCount) {
                                val pageBottomY = (i + 1) * pageHeightPx
                                drawContext.canvas.nativeCanvas.drawText(
                                    "Page ${i + 1}",
                                    size.width - 16.dp.toPx(),
                                    pageBottomY - 24.dp.toPx() // Slightly above the bottom edge
                                    , textPaint
                                )
                            }

                            // Draw saved lines
                            drawingLines.forEach { line ->
                                drawPath(
                                    path = line.toPath(),
                                    color = if (line.isEraser) eraserColor else strokeColor,
                                    style = Stroke(
                                        width = line.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // Draw active line
                            currentPath?.let { pathOffsets ->
                                val activeLine = currentProperties.copy(points = pathOffsets)
                                drawPath(
                                    path = activeLine.toPath(),
                                    color = if (activeLine.isEraser) eraserColor else strokeColor,
                                    style = Stroke(
                                        width = activeLine.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }

                FilledIconButton(
                    onClick = { pageCount++ },
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomStart)
                        .padding(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Page")
                }
            }
        }
    }
}
