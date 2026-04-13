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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.zIndex
import android.content.Context
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
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

private const val HIGHLIGHTER_THIN = 15f
private const val HIGHLIGHTER_MEDIUM = 30f
private const val HIGHLIGHTER_THICK = 50f

private const val TEXT_SMALL = 24f
private const val TEXT_MEDIUM = 40f
private const val TEXT_LARGE = 64f

private val ALLOWED_PEN_COLORS = listOf(
    Color.Red,
    Color.Blue,
    Color.Green,
    Color(0xFFA52A2A), // Brown
    Color(0xFFFF9999), // Light Red
    Color(0xFF99FF99), // Light Green
    Color(0xFF99CCFF), // Light Blue
    Color.Yellow
)

data class DrawingLine(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val text: String? = null
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
        lineObj.put("isHighlighter", line.isHighlighter)
        if (line.text != null) {
            lineObj.put("text", line.text)
        }
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
            val isHighlighter = lineObj.optBoolean("isHighlighter", false)
            val text = if (lineObj.has("text")) lineObj.getString("text") else null

            // Heuristic for old lines without isEraser flag: old eraser lines either matched the default M3 surface colors or were much thicker (min eraser thickness is 20f, max pen is 12f).
            val inferredEraser = isEraser || (!lineObj.has("isEraser") && (stroke >= 20f || rawColor == Color(0xFFFFFBFE) || rawColor == Color(0xFF1C1B1F) || rawColor == Color(0xFF141218)))

            // For older drawings that hardcoded "black" or "white" or specific theme surface colors, fallback to Unspecified so it dynamically adapts.
            val finalColor = if (!inferredEraser && rawColor !in ALLOWED_PEN_COLORS) {
                Color.Unspecified
            } else {
                rawColor
            }

            lines.add(DrawingLine(points, finalColor, stroke, inferredEraser, isHighlighter, text))
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

private val TextIcon: ImageVector
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
    var isHighlighterMode by remember { mutableStateOf(false) }
    var isTextMode by remember { mutableStateOf(false) }
    var currentTextSize by remember { mutableFloatStateOf(prefs.getFloat("text_size", TEXT_MEDIUM)) }
    var showTextSizeMenu by remember { mutableStateOf(false) }
    var activeTextInputPosition by remember { mutableStateOf<Offset?>(null) }
    var activeTextValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    var currentHighlighterThickness by remember { mutableFloatStateOf(prefs.getFloat("highlighter_thickness", HIGHLIGHTER_MEDIUM)) }
    var showHighlighterThicknessMenu by remember { mutableStateOf(false) }
    var currentThickness by remember { mutableFloatStateOf(prefs.getFloat("pen_thickness", PEN_MEDIUM)) }
    var showThicknessMenu by remember { mutableStateOf(false) }
    var currentEraserThickness by remember { mutableFloatStateOf(prefs.getFloat("eraser_thickness", ERASER_MEDIUM)) }
    var showEraserThicknessMenu by remember { mutableStateOf(false) }
    var currentColorValue by remember { mutableLongStateOf(prefs.getLong("pen_color", Color.Unspecified.value.toLong())) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showClearWarning by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun updatePenThickness(t: Float) {
        currentThickness = t
        prefs.edit { putFloat("pen_thickness", t) }
    }

    fun updateEraserThickness(t: Float) {
        currentEraserThickness = t
        prefs.edit { putFloat("eraser_thickness", t) }
    }

    fun updateTextSize(t: Float) {
        currentTextSize = t
        prefs.edit { putFloat("text_size", t) }
    }

    fun updateHighlighterThickness(t: Float) {
        currentHighlighterThickness = t
        prefs.edit { putFloat("highlighter_thickness", t) }
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
        val linesToSave = drawingLines.toList().toMutableList()
        if (activeTextInputPosition != null && activeTextValue.text.isNotEmpty()) {
            val cVal = Color(currentColorValue.toULong())
            val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
            linesToSave.add(
                DrawingLine(
                    points = listOf(activeTextInputPosition!!),
                    color = chosenColor,
                    strokeWidth = currentTextSize,
                    text = activeTextValue.text
                )
            )
        }
        onSerializedBodyChange(serializeDrawing(linesToSave))
    }

    val commitActiveText = {
        if (activeTextInputPosition != null && activeTextValue.text.isNotBlank()) {
            val cVal = Color(currentColorValue.toULong())
            val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
            drawingLines.add(
                DrawingLine(
                    points = listOf(activeTextInputPosition!!),
                    color = chosenColor,
                    strokeWidth = currentTextSize,
                    text = activeTextValue.text
                )
            )
            undoneLines.clear()
        }
        activeTextInputPosition = null
        activeTextValue = TextFieldValue("")
        commitChanges()
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
                                commitActiveText()
                                if (!isEraserMode && !isHighlighterMode && !isTextMode) {
                                    showThicknessMenu = true
                                } else {
                                    isEraserMode = false
                                    isHighlighterMode = false
                                    isTextMode = false
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (!isEraserMode && !isHighlighterMode && !isTextMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
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
                                commitActiveText()
                                if (isEraserMode) {
                                    showEraserThicknessMenu = true
                                } else {
                                    isEraserMode = true
                                    isHighlighterMode = false
                                    isTextMode = false
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
                        IconButton(
                            onClick = {
                                commitActiveText()
                                if (isHighlighterMode) {
                                    showHighlighterThicknessMenu = true
                                } else {
                                    isHighlighterMode = true
                                    isEraserMode = false
                                    isTextMode = false
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isHighlighterMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Highlighter")
                        }
                        DropdownMenu(
                            expanded = showHighlighterThicknessMenu,
                            onDismissRequest = { showHighlighterThicknessMenu = false },
                            modifier = Modifier.width(48.dp) // Force tight width constraint
                        ) {
                            listOf(HIGHLIGHTER_THIN, HIGHLIGHTER_MEDIUM, HIGHLIGHTER_THICK).forEach { thickness ->
                                DropdownMenuItem(
                                    modifier = if (currentHighlighterThickness == thickness) Modifier.background(MaterialTheme.colorScheme.surfaceVariant) else Modifier,
                                    contentPadding = PaddingValues(horizontal = 4.dp), // Tiny padding
                                    text = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.size(32.dp, 24.dp)) {
                                                val hColor = (if (currentColorValue == Color.Unspecified.value.toLong()) strokeColor else Color(currentColorValue.toULong())).copy(alpha = 0.4f)
                                                drawLine(
                                                    color = hColor,
                                                    start = Offset(0f, size.height / 2),
                                                    end = Offset(size.width, size.height / 2),
                                                    strokeWidth = thickness,
                                                    cap = StrokeCap.Square
                                                )
                                            }
                                        }
                                    },
                                    onClick = { updateHighlighterThickness(thickness); showHighlighterThicknessMenu = false }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = {
                                commitActiveText()
                                if (isTextMode) {
                                    showTextSizeMenu = true
                                } else {
                                    isTextMode = true
                                    isHighlighterMode = false
                                    isEraserMode = false
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isTextMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(TextIcon, contentDescription = "Text")
                        }
                        DropdownMenu(
                            expanded = showTextSizeMenu,
                            onDismissRequest = { showTextSizeMenu = false },
                            modifier = Modifier.width(60.dp)
                        ) {
                            listOf("S" to TEXT_SMALL, "M" to TEXT_MEDIUM, "L" to TEXT_LARGE).forEach { (label, size) ->
                                DropdownMenuItem(
                                    modifier = if (currentTextSize == size) Modifier.background(MaterialTheme.colorScheme.surfaceVariant) else Modifier,
                                    text = {
                                        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    onClick = { updateTextSize(size); showTextSizeMenu = false }
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
                            val penColors = listOf(Color.Unspecified) + ALLOWED_PEN_COLORS
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
                            commitActiveText()
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
                            commitActiveText()
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
                            commitActiveText()
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
                val maxWidthPx = constraints.maxWidth.toFloat()

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
                                .pointerInput(currentColorValue, currentThickness, currentEraserThickness, isEraserMode, isTextMode) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: continue

                                            // Yield touch to other UI components that consumed it first (like tapping the text cursor)
                                            if (change.isConsumed) continue

                                            val isStylus = change.type == PointerType.Stylus
                                            val isStylusEraser = change.type == PointerType.Eraser

                                            if (!isStylus && !isStylusEraser && !isTextMode) continue

                                            if (!isTextMode) {
                                                // Consume the event so horizontal/vertical scroll doesn't intercept it while drawing
                                                change.consume()
                                            }

                                            if (change.pressed && !change.previousPressed) {
                                                if (isTextMode) {
                                                    change.consume()
                                                    val tapPos = change.position
                                                    var hitIndex = -1
                                                    for (i in drawingLines.indices.reversed()) {
                                                        val l = drawingLines[i]
                                                        if (l.text != null && l.points.isNotEmpty()) {
                                                            val px = l.points.first().x
                                                            val py = l.points.first().y
                                                            val maxTargetW = (maxWidthPx - px - with(density) { 16.dp.toPx() }).coerceAtLeast(10f)
                                                            val p = android.graphics.Paint().apply { textSize = l.strokeWidth }
                                                            
                                                            val linesText = l.text.split("\n")
                                                            var estimatedLines = 0
                                                            var w = 0f
                                                            for (t in linesText) {
                                                                val tw = p.measureText(t)
                                                                w = kotlin.math.max(w, kotlin.math.min(tw, maxTargetW))
                                                                estimatedLines += kotlin.math.ceil(tw / maxTargetW).toInt().coerceAtLeast(1)
                                                            }
                                                            val h = l.strokeWidth * estimatedLines * 1.2f

                                                            // Provide a generous touch target for the text box
                                                            if (tapPos.x in (px - 50f)..(px + w + 70f) && tapPos.y in (py - 50f)..(py + h + 80f)) {
                                                                hitIndex = i
                                                                break
                                                            }
                                                        }
                                                    }

                                                    commitActiveText()
                                                    if (hitIndex != -1) {
                                                        val hitLine = drawingLines.removeAt(hitIndex)
                                                        updatePenColor(hitLine.color.value.toLong())
                                                        activeTextInputPosition = hitLine.points.first()
                                                        val safeText = hitLine.text!! // Keep original newlines
                                                        val p = android.graphics.Paint().apply { textSize = hitLine.strokeWidth }
                                                        val widths = FloatArray(safeText.length)
                                                        p.getTextWidths(safeText, widths)
                                                        val tapDX = tapPos.x - hitLine.points.first().x
                                                        var curX = 0f
                                                        var charIdx = safeText.length
                                                        for (charI in widths.indices) {
                                                            if (safeText[charI] == '\n') continue // skip measurement for newline char
                                                            if (tapDX < curX + widths[charI] / 2f) {
                                                                charIdx = charI
                                                                break
                                                            }
                                                            curX += widths[charI]
                                                        }
                                                        activeTextValue = TextFieldValue(safeText, selection = androidx.compose.ui.text.TextRange(charIdx))
                                                        currentTextSize = hitLine.strokeWidth
                                                        commitChanges()
                                                    } else {
                                                        var targetY = tapPos.y - currentTextSize / 2f
                                                        for (l in drawingLines) {
                                                            if (l.text != null && l.points.isNotEmpty()) {
                                                                val py = l.points.first().y
                                                                if (kotlin.math.abs(targetY - py) < l.strokeWidth) {
                                                                    targetY = py
                                                                    break
                                                                }
                                                            }
                                                        }
                                                        activeTextInputPosition = Offset(tapPos.x, targetY)
                                                        activeTextValue = TextFieldValue("")
                                                        commitChanges()
                                                    }
                                                } else if (currentPath == null) {
                                                    commitActiveText()
                                                    currentPath = listOf(change.position)
                                                    val actualEraserMode = isStylusEraser || isEraserMode
                                                    val cVal = Color(currentColorValue.toULong())
                                                    val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
                                                    currentProperties = DrawingLine(
                                                        points = currentPath!!,
                                                        color = if (actualEraserMode) Color.Unspecified else chosenColor,
                                                        strokeWidth = if (actualEraserMode) currentEraserThickness else if (isHighlighterMode) currentHighlighterThickness else currentThickness,
                                                        isEraser = actualEraserMode,
                                                        isHighlighter = isHighlighterMode
                                                     )
                                                 }
                                            } else if (change.pressed && change.previousPressed) {
                                                 if (!isTextMode && currentPath != null) {
                                                     currentPath = currentPath!! + change.position
                                                 }
                                            } else if (!change.pressed && change.previousPressed) {
                                                if (!isTextMode && currentPath != null) {
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
                                if (line.text != null && line.points.isNotEmpty()) {
                                    return@forEach // Rendered natively via Jetpack Compose Text overlay instead of raw Canvas
                                }

                                val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
                                val finalColor = when {
                                    line.isEraser -> Color.Black
                                    line.isHighlighter -> activeLineColor.copy(alpha = 0.4f)
                                    else -> activeLineColor
                                }
                                val finalBlendMode = when {
                                    line.isEraser -> androidx.compose.ui.graphics.BlendMode.Clear
                                    line.isHighlighter -> androidx.compose.ui.graphics.BlendMode.Multiply
                                    else -> androidx.compose.ui.graphics.BlendMode.SrcOver
                                }
                                drawPath(
                                    path = line.toPath(),
                                    color = finalColor, // Color ignored for Clear blend mode
                                    style = Stroke(
                                        width = line.strokeWidth,
                                        cap = if (line.isHighlighter) StrokeCap.Square else StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    ),
                                    blendMode = finalBlendMode
                                )
                            }

                            // Draw active line
                            currentPath?.let { pathOffsets ->
                                val activeLine = currentProperties.copy(points = pathOffsets)
                                val activeLineColor = if (activeLine.color == Color.Unspecified || activeLine.color == Color.Black || activeLine.color == Color.White) strokeColor else activeLine.color
                                val finalColor = when {
                                    activeLine.isEraser -> Color.Black
                                    activeLine.isHighlighter -> activeLineColor.copy(alpha = 0.4f)
                                    else -> activeLineColor
                                }
                                val finalBlendMode = when {
                                    activeLine.isEraser -> androidx.compose.ui.graphics.BlendMode.Clear
                                    activeLine.isHighlighter -> androidx.compose.ui.graphics.BlendMode.Multiply
                                    else -> androidx.compose.ui.graphics.BlendMode.SrcOver
                                }
                                drawPath(
                                    path = activeLine.toPath(),
                                    color = finalColor,
                                    style = Stroke(
                                        width = activeLine.strokeWidth,
                                        cap = if (activeLine.isHighlighter) StrokeCap.Square else StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    ),
                                    blendMode = finalBlendMode
                                )
                            }
                        }

                        // Render Static Text natively via Compose to fix multi-line metrics and Android's Canvas jump disparities
                        drawingLines.forEach { line ->
                            if (line.text != null && line.points.isNotEmpty()) {
                                val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
                                Text(
                                    text = line.text,
                                    style = androidx.compose.ui.text.TextStyle(
                                        color = activeLineColor,
                                        fontSize = with(LocalDensity.current) { line.strokeWidth.toSp() }
                                    ),
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(line.points.first().x.toInt(), line.points.first().y.toInt())
                                        }
                                        .widthIn(max = with(LocalDensity.current) { (maxWidthPx - line.points.first().x).toDp() - 16.dp })
                                )
                            }
                        }

                        // Inline Text Tool Layer
                        if (activeTextInputPosition != null) {
                            val cVal = Color(currentColorValue.toULong())
                            val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else strokeColor
                            BasicTextField(
                                value = activeTextValue,
                                onValueChange = {
                                    activeTextValue = it
                                    commitChanges()
                                },
                                modifier = Modifier
                                    .offset { IntOffset(activeTextInputPosition!!.x.toInt(), activeTextInputPosition!!.y.toInt()) }
                                    .widthIn(max = with(density) { (maxWidthPx - activeTextInputPosition!!.x).toDp() - 16.dp })
                                    .focusRequester(focusRequester)
                                    .background(Color.Transparent),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = chosenColor,
                                    fontSize = with(LocalDensity.current) { currentTextSize.toSp() }
                                )
                            )
                            LaunchedEffect(activeTextInputPosition) {
                                kotlinx.coroutines.delay(50)
                                try {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

                FilledIconButton(
                    onClick = { 
                        commitActiveText()
                        pageCount++ 
                    },
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
