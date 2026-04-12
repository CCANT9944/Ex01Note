package com.example.ex01

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.zIndex
import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "snote_settings"
private const val PEN_THIN = 3f
private const val PEN_MEDIUM = 6f
private const val PEN_THICK = 12f

private const val ERASER_THIN = 20f
private const val ERASER_MEDIUM = 40f
private const val ERASER_THICK = 80f

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
            val rawColor = Color(lineObj.optLong("color", Color.Unspecified.value.toLong()).toULong())
            val stroke = lineObj.optDouble("stroke", 5.0).toFloat()
            val isEraser = lineObj.optBoolean("isEraser", false)
            
            // Heuristic for old lines without isEraser flag: old eraser lines either matched the default M3 surface colors or were much thicker (min eraser thickness is 20f, max pen is 12f).
            val inferredEraser = isEraser || (!lineObj.has("isEraser") && (stroke >= 20f || rawColor == Color(0xFFFFFBFE) || rawColor == Color(0xFF1C1B1F) || rawColor == Color(0xFF141218)))

            // For older drawings that hardcoded "black" or "white" or specific theme surface colors, fallback to Unspecified so it dynamically adapts.
            val finalColor = if (!inferredEraser && rawColor != Color.Red && rawColor != Color.Blue && rawColor != Color.Green && rawColor != Color(0xFFA52A2A)) {
                Color.Unspecified
            } else {
                rawColor
            }

            lines.add(DrawingLine(points, finalColor, stroke, inferredEraser))
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

@Composable
fun SNoteEditor(
    serializedBody: String,
    onSerializedBodyChange: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val drawingLines = remember { mutableStateListOf<DrawingLine>() }
    val undoneLines = remember { mutableStateListOf<DrawingLine>() }
    var currentPath by remember { mutableStateOf<List<Offset>?>(null) }
    var currentProperties by remember { mutableStateOf(DrawingLine(emptyList())) }
    var isEraserMode by remember { mutableStateOf(false) }
    var currentThickness by remember { mutableFloatStateOf(prefs.getFloat("pen_thickness", PEN_MEDIUM)) }
    var showThicknessMenu by remember { mutableStateOf(false) }
    var currentEraserThickness by remember { mutableFloatStateOf(prefs.getFloat("eraser_thickness", ERASER_MEDIUM)) }
    var showEraserThicknessMenu by remember { mutableStateOf(false) }
    var currentColorValue by remember { mutableLongStateOf(prefs.getLong("pen_color", Color.Unspecified.value.toLong())) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showClearWarning by remember { mutableStateOf(false) }

    fun updatePenThickness(t: Float) {
        currentThickness = t
        prefs.edit { putFloat("pen_thickness", t) }
    }

    fun updateEraserThickness(t: Float) {
        currentEraserThickness = t
        prefs.edit { putFloat("eraser_thickness", t) }
    }

    fun updatePenColor(c: Long) {
        currentColorValue = c
        prefs.edit { putLong("pen_color", c) }
    }

    var pageCount by remember { mutableIntStateOf(1) }
    var pageHeightPx by remember { mutableFloatStateOf(0f) }
    var pageHeightDp by remember { mutableStateOf(0.dp) }

    LaunchedEffect(serializedBody, pageHeightPx) {
        if (drawingLines.isEmpty() && serializedBody.isNotBlank() && currentPath == null && pageHeightPx > 0f) {
            val lines = withContext(Dispatchers.Default) {
                deserializeDrawing(serializedBody)
            }
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

    val strokeColor = MaterialTheme.colorScheme.onSurface
    val eraserColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth().zIndex(1f),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Scrollable Tools Group
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                            onDismissRequest = { showThicknessMenu = false },
                            modifier = Modifier.width(48.dp) // Force tight width constraint
                        ) {
                            listOf(PEN_THIN, PEN_MEDIUM, PEN_THICK).forEach { thickness ->
                                DropdownMenuItem(
                                    modifier = if (currentThickness == thickness) Modifier.background(MaterialTheme.colorScheme.surfaceVariant) else Modifier,
                                    contentPadding = PaddingValues(horizontal = 4.dp), // Tiny padding
                                    text = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.size(32.dp, 24.dp)) {
                                                drawLine(
                                                    color = if (currentThickness == thickness) primaryColor else strokeColor,
                                                    start = Offset(0f, size.height / 2),
                                                    end = Offset(size.width, size.height / 2),
                                                    strokeWidth = thickness,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    },
                                    onClick = { updatePenThickness(thickness); showThicknessMenu = false }
                                )
                            }
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
                            onDismissRequest = { showEraserThicknessMenu = false },
                            modifier = Modifier.width(48.dp) // Force tight width constraint
                        ) {
                            val eraserVisualColor = strokeColor.copy(alpha = 0.3f)
                            listOf(ERASER_THIN, ERASER_MEDIUM, ERASER_THICK).forEach { thickness ->
                                DropdownMenuItem(
                                    modifier = if (currentEraserThickness == thickness) Modifier.background(MaterialTheme.colorScheme.surfaceVariant) else Modifier,
                                    contentPadding = PaddingValues(horizontal = 4.dp), // Tiny padding
                                    text = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.size(32.dp, 24.dp)) {
                                                drawLine(
                                                    color = if (currentEraserThickness == thickness) primaryColor.copy(alpha = 0.5f) else eraserVisualColor,
                                                    start = Offset(0f, size.height / 2),
                                                    end = Offset(size.width, size.height / 2),
                                                    strokeWidth = thickness,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    },
                                    onClick = { updateEraserThickness(thickness); showEraserThicknessMenu = false }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { showColorMenu = true }) {
                            Box(modifier = Modifier
                                .size(24.dp)
                                .background(if (currentColorValue == Color.Unspecified.value.toLong()) strokeColor else Color(currentColorValue.toULong()), shape = CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            )
                        }
                        DropdownMenu(
                            expanded = showColorMenu,
                            onDismissRequest = { showColorMenu = false },
                            modifier = Modifier.width(48.dp)
                        ) {
                            val penColors = listOf(
                                Color.Unspecified,
                                Color.Red,
                                Color.Blue,
                                Color.Green,
                                Color(0xFFA52A2A)
                            )
                            penColors.forEach { c ->
                                DropdownMenuItem(
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    text = {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                            Box(modifier = Modifier
                                                .size(24.dp)
                                                .background(if (c == Color.Unspecified) strokeColor else c, shape = CircleShape)
                                                .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            )
                                        }
                                    },
                                    onClick = {
                                        updatePenColor(c.value.toLong())
                                        showColorMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(24.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )

                    IconButton(
                        onClick = {
                            if (drawingLines.isNotEmpty()) {
                                undoneLines.add(drawingLines.removeAt(drawingLines.size - 1))
                                commitChanges()
                            }
                        },
                        enabled = drawingLines.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }

                    IconButton(
                        onClick = {
                            if (undoneLines.isNotEmpty()) {
                                drawingLines.add(undoneLines.removeAt(undoneLines.size - 1))
                                commitChanges()
                            }
                        },
                        enabled = undoneLines.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                } // Close Scrollable Row

                // Fixed right-side actions
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(24.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            if (drawingLines.isNotEmpty()) {
                                showClearWarning = true
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear All")
                    }
                }
            } // Close Outer Row
        } // Close Surface Toolbar

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
                        // Background & Dividers Layer
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pageHeightDp * pageCount)
                        ) {
                            // Fill whole background first
                            drawRect(color = eraserColor, size = size)
                            drawPageLayout(pageCount, pageHeightPx)
                        }

                        // Drawing Storkes Layer
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pageHeightDp * pageCount)
                                .graphicsLayer(alpha = 0.99f) // Force offscreen layer to support true transparent erasing
                                .pointerInput(currentColorValue, currentThickness, currentEraserThickness, isEraserMode) {
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
                                                    val cVal = Color(currentColorValue.toULong())
                                                    val chosenColor = if (cVal == Color.Red || cVal == Color.Blue || cVal == Color.Green || cVal == Color(0xFFA52A2A)) cVal else Color.Unspecified
                                                    currentProperties = DrawingLine(
                                                        points = currentPath!!,
                                                        color = if (actualEraserMode) Color.Unspecified else chosenColor,
                                                        strokeWidth = if (actualEraserMode) currentEraserThickness else currentThickness,
                                                        isEraser = actualEraserMode
                                                     )
                                                 } else {
                                                     currentPath = currentPath!! + change.position
                                                 }
                                            } else {
                                                if (currentPath != null) {
                                                    drawingLines.add(currentProperties.copy(points = currentPath!!))
                                                    undoneLines.clear()
                                                    currentPath = null
                                                    commitChanges()
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            // Draw saved lines
                            drawingLines.forEach { line ->
                                val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
                                drawPath(
                                    path = line.toPath(),
                                    color = if (line.isEraser) Color.Black else activeLineColor, // Color ignored for Clear blend mode
                                    style = Stroke(
                                        width = line.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    ),
                                    blendMode = if (line.isEraser) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver
                                )
                            }

                            // Draw active line
                            currentPath?.let { pathOffsets ->
                                val activeLine = currentProperties.copy(points = pathOffsets)
                                val activeLineColor = if (activeLine.color == Color.Unspecified || activeLine.color == Color.Black || activeLine.color == Color.White) strokeColor else activeLine.color
                                drawPath(
                                    path = activeLine.toPath(),
                                    color = if (activeLine.isEraser) Color.Black else activeLineColor,
                                    style = Stroke(
                                        width = activeLine.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    ),
                                    blendMode = if (activeLine.isEraser) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver
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

        if (showClearWarning) {
            AlertDialog(
                onDismissRequest = { showClearWarning = false },
                title = { Text("Clear All") },
                text = { Text("Are you sure you want to clear the entire drawing? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            drawingLines.clear()
                            undoneLines.clear()
                            commitChanges()
                            showClearWarning = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearWarning = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPageLayout(
    pageCount: Int,
    pageHeightPx: Float
) {
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
            pageBottomY - 24.dp.toPx(), // Slightly above the bottom edge
            textPaint
        )
    }
}
