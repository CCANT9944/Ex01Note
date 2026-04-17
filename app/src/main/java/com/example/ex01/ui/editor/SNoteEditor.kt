package com.example.ex01.ui.editor


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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun SNoteEditor(
    serializedBody: String,
    onSerializedBodyChange: (String) -> Unit,
    viewModel: SNoteViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) = with(viewModel) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var currentTextSize by remember { mutableFloatStateOf(prefs.getFloat("text_size", TEXT_MEDIUM)) }
    val focusRequester = remember { FocusRequester() }
    var currentHighlighterThickness by remember { mutableFloatStateOf(prefs.getFloat("highlighter_thickness", HIGHLIGHTER_MEDIUM)) }
    var currentThickness by remember { mutableFloatStateOf(prefs.getFloat("pen_thickness", PEN_MEDIUM)) }
    var currentEraserThickness by remember { mutableFloatStateOf(prefs.getFloat("eraser_thickness", ERASER_MEDIUM)) }
    var currentColorValue by remember { mutableLongStateOf(prefs.getLong("pen_color", Color.Unspecified.value.toLong())) }
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

    var pageHeightPx by remember { mutableFloatStateOf(0f) }
    var pageHeightDp by remember { mutableStateOf(0.dp) }



    LaunchedEffect(serializedBody, pageHeightPx) {
        if (!initialLoadDone && pageHeightPx > 0f) {
            initialLoadDone = true
            if (serializedBody.isNotBlank()) {
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
    }

    val commitChanges = {
        val linesToSave = drawingLines.toList().toMutableList()
        if (selectedLines.isNotEmpty()) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            selectedLines.forEach { l ->
                if (l.isEraser) return@forEach
                l.points.forEach { pt ->
                    if (pt.x < minX) minX = pt.x
                    if (pt.y < minY) minY = pt.y
                    if (pt.x > maxX) maxX = pt.x
                    if (pt.y > maxY) maxY = pt.y
                }
            }
            val cX = (minX + maxX) / 2f
            val cY = (minY + maxY) / 2f

            val finalizedLines = selectedLines.map { l ->
                l.copy(
                    points = l.points.map { p ->
                        Offset(
                            cX + (p.x - cX) * selectionScale + selectionDragOffset.x,
                            cY + (p.y - cY) * selectionScale + selectionDragOffset.y
                        )
                    },
                    strokeWidth = l.strokeWidth * selectionScale
                )
            }
            linesToSave.addAll(finalizedLines)
        }
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

    val commitLassoSelection = {
        if (selectedLines.isNotEmpty()) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            selectedLines.forEach { l ->
                if (l.isEraser) return@forEach
                l.points.forEach { pt ->
                    if (pt.x < minX) minX = pt.x
                    if (pt.y < minY) minY = pt.y
                    if (pt.x > maxX) maxX = pt.x
                    if (pt.y > maxY) maxY = pt.y
                }
            }
            val cX = (minX + maxX) / 2f
            val cY = (minY + maxY) / 2f

            val finalizedLines = selectedLines.map { l ->
                l.copy(
                    points = l.points.map { p ->
                        Offset(
                            cX + (p.x - cX) * selectionScale + selectionDragOffset.x,
                            cY + (p.y - cY) * selectionScale + selectionDragOffset.y
                        )
                    },
                    strokeWidth = l.strokeWidth * selectionScale
                )
            }
            drawingLines.addAll(finalizedLines)
            selectedLines.clear()
            selectionDragOffset = Offset.Zero
            selectionScale = 1f
            commitChanges()
        }
    }

    val currentCommitChanges by rememberUpdatedState(commitChanges)
    DisposableEffect(Unit) {
        onDispose {
            currentCommitChanges()
        }
    }

    val commitActiveText = {
        commitLassoSelection()
        if (activeTextInputPosition != null) {
            if (activeTextValue.text.isNotBlank()) {
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
            originalHitLine = null
            originalHitIndex = -1
            activeTextInputPosition = null
            activeTextValue = TextFieldValue("")
            commitChanges()
        }
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
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Scrollable Tools Group
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = {
                                commitActiveText()
                                if (!isEraserMode && !isHighlighterMode && !isTextMode && !isLassoMode) {
                                    showThicknessMenu = true
                                } else {
                                    isEraserMode = false
                                    isHighlighterMode = false
                                    isTextMode = false
                                    isLassoMode = false
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (!isEraserMode && !isHighlighterMode && !isTextMode && !isLassoMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Create, contentDescription = "Pen", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showThicknessMenu,
                            onDismissRequest = { showThicknessMenu = false },
                            modifier = Modifier.width(48.dp) // Force tight width constraint
                        ) {
                            listOf(PEN_THIN, PEN_MEDIUM, PEN_THICK).forEach { thickness ->
                                DropdownMenuItem(
                                    modifier = if (currentThickness == thickness) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
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
                                    isLassoMode = false
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(EraserIcon, contentDescription = "Eraser", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showEraserThicknessMenu,
                            onDismissRequest = { showEraserThicknessMenu = false },
                            modifier = Modifier.width(48.dp) // Force tight width constraint
                        ) {
                            val eraserVisualColor = strokeColor.copy(alpha = 0.3f)
                            listOf(ERASER_THIN, ERASER_MEDIUM, ERASER_THICK).forEach { thickness ->
                                DropdownMenuItem(
                                    modifier = if (currentEraserThickness == thickness) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
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
                                    isLassoMode = false
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isHighlighterMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(HighlighterIcon, contentDescription = "Highlighter", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showHighlighterThicknessMenu,
                            onDismissRequest = { showHighlighterThicknessMenu = false },
                            modifier = Modifier.width(48.dp) // Force tight width constraint
                        ) {
                            listOf(HIGHLIGHTER_THIN, HIGHLIGHTER_MEDIUM, HIGHLIGHTER_THICK).forEach { thickness ->
                                DropdownMenuItem(
                                    modifier = if (currentHighlighterThickness == thickness) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
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
                                if (isTextMode) {
                                    showTextSizeMenu = true
                                } else {
                                    commitActiveText()
                                    isTextMode = true
                                    isHighlighterMode = false
                                    isEraserMode = false
                                    isLassoMode = false
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isTextMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(TextIcon, contentDescription = "Text", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showTextSizeMenu,
                            onDismissRequest = { showTextSizeMenu = false },
                            modifier = Modifier.width(60.dp)
                        ) {
                            listOf("40" to TEXT_MEDIUM, "64" to TEXT_LARGE).forEach { (label, size) ->
                                DropdownMenuItem(
                                    modifier = if (currentTextSize == size) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
                                    text = {
                                        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    onClick = {
                                        updateTextSize(size)
                                        showTextSizeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = {
                                commitActiveText()
                                isLassoMode = true
                                isEraserMode = false
                                isHighlighterMode = false
                                isTextMode = false
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isLassoMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(LassoIcon, contentDescription = "Lasso", modifier = Modifier.size(20.dp))
                        }
                    }

                    Box {
                        IconButton(onClick = { showColorMenu = true }, modifier = Modifier.size(38.dp)) {
                            Box(modifier = Modifier
                                .size(20.dp)
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
                                    modifier = if (currentColorValue == c.value.toLong()) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
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
                            .height(20.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )

                    IconButton(
                        onClick = {
                            if (activeTextInputPosition != null) {
                                // Cancel text editing and restore original hit line
                                activeTextInputPosition = null
                                activeTextValue = TextFieldValue("")
                                if (originalHitLine != null) {
                                    val index = if (originalHitIndex in 0..drawingLines.size) originalHitIndex else drawingLines.size
                                    drawingLines.add(index, originalHitLine!!)
                                    originalHitLine = null
                                    originalHitIndex = -1
                                }
                                commitChanges()
                            } else if (drawingLines.isNotEmpty()) {
                                undoneLines.add(drawingLines.removeAt(drawingLines.size - 1))
                                commitChanges()
                            }
                        },
                        modifier = Modifier.size(38.dp),
                        enabled = drawingLines.isNotEmpty() || activeTextInputPosition != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            commitActiveText()
                            if (undoneLines.isNotEmpty()) {
                                drawingLines.add(undoneLines.removeAt(undoneLines.size - 1))
                                commitChanges()
                            }
                        },
                        modifier = Modifier.size(38.dp),
                        enabled = undoneLines.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(20.dp))
                    }
                } // Close Scrollable Row

                // Fixed right-side actions
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(20.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    TextButton(
                        onClick = {
                            commitActiveText()
                            if (drawingLines.isNotEmpty()) {
                                showClearWarning = true
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear All", fontSize = 14.sp)
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
                val availableWidth = this.maxWidth
                val density = LocalDensity.current
                val availableWidthPx = with(density) { availableWidth.toPx() }

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
                                .pointerInput(currentColorValue, currentThickness, currentEraserThickness, isEraserMode, isTextMode, isLassoMode) {
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

                                                    commitActiveText()

                                                    val rowHeight = TEXT_LARGE * 1.2f
                                                    val clickedRowIndex = (tapPos.y / rowHeight).toInt()
                                                    val targetY = clickedRowIndex * rowHeight

                                                    var hitIndex = -1
                                                    for (i in drawingLines.indices.reversed()) {
                                                        val l = drawingLines[i]
                                                        if (l.text != null && l.points.isNotEmpty()) {
                                                            val py = l.points.first().y
                                                            val startRow = kotlin.math.round(py / rowHeight).toInt()

                                                            val maxTextWidthPx = availableWidthPx - l.points.first().x - with(density) { 4.dp.toPx() }
                                                            val pEst = android.graphics.Paint().apply { textSize = l.strokeWidth }
                                                            var visualRows = 0
                                                            for (lineStr in l.text.split("\n")) {
                                                                val w = pEst.measureText(lineStr)
                                                                if (maxTextWidthPx > 0f) {
                                                                    visualRows += kotlin.math.max(1, kotlin.math.ceil((w / maxTextWidthPx).toDouble()).toInt())
                                                                } else {
                                                                    visualRows += 1
                                                                }
                                                            }
                                                            val endRow = startRow + visualRows - 1

                                                            if (clickedRowIndex in startRow..endRow) {
                                                                hitIndex = i
                                                                break
                                                            }
                                                        }
                                                    }

                                                    if (hitIndex != -1) {
                                                        val hitLine = drawingLines.removeAt(hitIndex)
                                                        originalHitLine = hitLine
                                                        originalHitIndex = hitIndex
                                                        updatePenColor(hitLine.color.value.toLong())
                                                        activeTextInputPosition = hitLine.points.first()
                                                        val py = hitLine.points.first().y
                                                        val startRow = kotlin.math.round(py / rowHeight).toInt()
                                                        val safeText = hitLine.text!! // Keep original newlines

                                                        val p = android.graphics.Paint().apply { textSize = hitLine.strokeWidth }
                                                        var finalCharIdx = safeText.length

                                                        val maxTextWidthPx = availableWidthPx - hitLine.points.first().x - with(density) { 4.dp.toPx() }
                                                        val lines = safeText.split("\n")
                                                        var currentVisualRowIdx = startRow
                                                        var charIdxOff = 0

                                                        // Fallback logic for hit tapping
                                                        for (lineStr in lines) {
                                                            var w = p.measureText(lineStr)
                                                            val rowsForThisLine = if (maxTextWidthPx > 0f) kotlin.math.max(1, kotlin.math.ceil((w / maxTextWidthPx).toDouble()).toInt()) else 1
                                                            val endVisualRowForThisLine = currentVisualRowIdx + rowsForThisLine - 1

                                                            if (clickedRowIndex in currentVisualRowIdx..endVisualRowForThisLine) {
                                                                if (clickedRowIndex == endVisualRowForThisLine && lineStr.isNotEmpty()) {
                                                                    // Roughly tapped on the last visual chunk of this hard line or a wrapped line.
                                                                    val widths = FloatArray(lineStr.length)
                                                                    p.getTextWidths(lineStr, widths)
                                                                    val tapDX = tapPos.x - hitLine.points.first().x

                                                                    if (rowsForThisLine > 1) {
                                                                        // Simplest fallback for soft-wrapped lines: place cursor at end
                                                                        // to prevent jumping to the very beginning due to wrap offset mismatch.
                                                                        finalCharIdx = charIdxOff + lineStr.length
                                                                    } else {
                                                                        var curX = 0f
                                                                        var charIdxInLine = lineStr.length
                                                                        for (i in widths.indices) {
                                                                            if (tapDX < curX + widths[i] / 2f) {
                                                                                charIdxInLine = i
                                                                                break
                                                                            }
                                                                            curX += widths[i]
                                                                        }
                                                                        if (tapDX < 0) charIdxInLine = 0
                                                                        else if (tapDX > curX + 20f) charIdxInLine = lineStr.length

                                                                        finalCharIdx = charIdxOff + charIdxInLine
                                                                    }
                                                                } else {
                                                                    // Tapped somewhere on a completely wrapped block.
                                                                    // Safest interaction model is placing at the end of the full line.
                                                                    finalCharIdx = charIdxOff + lineStr.length
                                                                }
                                                                break
                                                            }
                                                            currentVisualRowIdx += rowsForThisLine
                                                            charIdxOff += lineStr.length + 1 // +1 for '\n'
                                                        }

                                                        activeTextValue = TextFieldValue(safeText, selection = androidx.compose.ui.text.TextRange(finalCharIdx))
                                                        currentTextSize = hitLine.strokeWidth
                                                        commitChanges()
                                                    } else {
                                                        val defaultIndent = with(density) { 16.dp.toPx() }
                                                        activeTextInputPosition = Offset(defaultIndent, targetY)
                                                        activeTextValue = TextFieldValue("")
                                                        commitChanges()
                                                    }
                                                } else if (isLassoMode) {
                                                    // Handle lasso tool start
                                                    change.consume()
                                                    val tapPos = change.position
                                                    
                                                    // Determine if tap is inside the current selection bounding box or handle
                                                    val draggingHandle = selectedLines.isNotEmpty() && isPointInScaleHandle(tapPos, selectedLines, selectionDragOffset, selectionScale)
                                                    val dragging = !draggingHandle && selectedLines.isNotEmpty() && isPointInSelectionBounds(tapPos, selectedLines, selectionDragOffset, selectionScale)

                                                    if (draggingHandle) {
                                                        isScalingSelection = true
                                                    } else if (dragging) {
                                                        isDraggingSelection = true
                                                    } else {
                                                        // Tap outside selection -> clear previous selection and start new lasso
                                                        if (selectedLines.isNotEmpty()) {
                                                            commitLassoSelection()
                                                        }
                                                        lassoPath = listOf(tapPos)
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
                                                 if (isLassoMode) {
                                                     change.consume()
                                                     if (isScalingSelection) {
                                                         val dx = change.position.x - change.previousPosition.x
                                                         val dy = change.position.y - change.previousPosition.y
                                                         // Base scale change off total drag distance out/in
                                                         val scaleDelta = (dx + dy) / 400f
                                                         selectionScale = kotlin.math.max(0.1f, selectionScale + scaleDelta)
                                                     } else if (isDraggingSelection) {
                                                         val dragAmount = change.position - change.previousPosition
                                                         selectionDragOffset += dragAmount
                                                     } else if (lassoPath != null) {
                                                         lassoPath = lassoPath!! + change.position
                                                     }
                                                 } else if (!isTextMode && currentPath != null) {
                                                     currentPath = currentPath!! + change.position
                                                 }
                                            } else if (!change.pressed && change.previousPressed) {
                                                if (isLassoMode) {
                                                    if (isScalingSelection) {
                                                        isScalingSelection = false
                                                    } else if (isDraggingSelection) {
                                                        isDraggingSelection = false
                                                    } else if (lassoPath != null) {
                                                        // Lasso drawing finished, calculate enclosed lines
                                                        val capturedLassoPath = lassoPath!!
                                                        lassoPath = null
                                                        
                                                        val newSelection = mutableListOf<DrawingLine>()
                                                        val remainingLines = mutableListOf<DrawingLine>()
                                                        
                                                        // Basic AABB intersection logic for simplicity (can evolve to Polygon hit-test later)
                                                        val minX = capturedLassoPath.minOfOrNull { it.x } ?: 0f
                                                        val maxX = capturedLassoPath.maxOfOrNull { it.x } ?: 0f
                                                        val minY = capturedLassoPath.minOfOrNull { it.y } ?: 0f
                                                        val maxY = capturedLassoPath.maxOfOrNull { it.y } ?: 0f
                                                        val lassoRect = androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY)
                                                        
                                                        for ((index, l) in drawingLines.withIndex()) {
                                                            if (l.points.isEmpty()) continue

                                                            if (l.isEraser) {
                                                                remainingLines.add(l)
                                                                continue
                                                            }

                                                            // Fast precise geometry intersection that mathematically ignores ANY points visually "covered" by subsequent erasers
                                                            val isSelected = l.points.any { pt ->
                                                                if (!lassoRect.contains(pt)) return@any false
                                                                if (!isPointInPolygon(pt, capturedLassoPath)) return@any false

                                                                if (l.text != null) return@any true // Text elements bypass Canvas composite erasers natively

                                                                var pointErased = false
                                                                for (j in index + 1 until drawingLines.size) {
                                                                    val e = drawingLines[j]
                                                                    if (e.isEraser) {
                                                                        val rSq = e.strokeWidth * e.strokeWidth // Broad radius bounds to buffer interpolation
                                                                        for (ept in e.points) {
                                                                            val dx = pt.x - ept.x
                                                                            val dy = pt.y - ept.y
                                                                            if (dx * dx + dy * dy <= rSq) {
                                                                                pointErased = true
                                                                                break
                                                                            }
                                                                        }
                                                                    }
                                                                    if (pointErased) break
                                                                }
                                                                !pointErased
                                                            }

                                                            if (isSelected) {
                                                                newSelection.add(l)
                                                            } else {
                                                                remainingLines.add(l)
                                                            }
                                                        }


                                                        if (newSelection.isNotEmpty()) {
                                                            drawingLines.clear()
                                                            drawingLines.addAll(remainingLines)
                                                            selectedLines.addAll(newSelection)
                                                            selectionDragOffset = Offset.Zero
                                                            selectionScale = 1f
                                                        }
                                                    }
                                                } else if (!isTextMode && currentPath != null) {
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

                            // Draw lasso path
                            lassoPath?.let { lPath ->
                                val p = Path()
                                lPath.forEachIndexed { index, point ->
                                    if (index == 0) p.moveTo(point.x, point.y) else p.lineTo(point.x, point.y)
                                }
                                drawPath(
                                    path = p,
                                    color = primaryColor,
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                )
                            }
                            
                            // Draw selected lines (dragged)
                            if (selectedLines.isNotEmpty()) {
                                // Draw bounding box
                                    var minX = Float.MAX_VALUE
                                    var minY = Float.MAX_VALUE
                                    var maxX = Float.MIN_VALUE
                                    var maxY = Float.MIN_VALUE

                                    selectedLines.forEach { l ->
                                        if (l.isEraser) return@forEach
                                        l.points.forEach { pt ->
                                            if (pt.x < minX) minX = pt.x
                                            if (pt.y < minY) minY = pt.y
                                            if (pt.x > maxX) maxX = pt.x
                                            if (pt.y > maxY) maxY = pt.y
                                        }
                                    }
                                    val cX = (minX + maxX) / 2f
                                    val cY = (minY + maxY) / 2f

                                    selectedLines.forEach { l ->
                                        val finalColor = when {
                                            l.isEraser -> Color.Black
                                            l.isHighlighter -> l.color.copy(alpha = 0.4f)
                                            else -> l.color.copy(alpha = 0.7f)
                                        }

                                        val finalBlendMode = when {
                                            l.isEraser -> androidx.compose.ui.graphics.BlendMode.Clear
                                            l.isHighlighter -> androidx.compose.ui.graphics.BlendMode.Multiply
                                            else -> androidx.compose.ui.graphics.BlendMode.SrcOver
                                        }
                                        
                                        val offsetPath = Path()
                                        l.points.forEachIndexed { idx, pt ->
                                            val pX = cX + (pt.x - cX) * selectionScale + selectionDragOffset.x
                                            val pY = cY + (pt.y - cY) * selectionScale + selectionDragOffset.y

                                            if (idx == 0) offsetPath.moveTo(pX, pY) else offsetPath.lineTo(pX, pY)
                                        }
                                        
                                        drawPath(
                                            path = offsetPath,
                                            color = finalColor,
                                            style = Stroke(
                                                width = l.strokeWidth * selectionScale,
                                                cap = if (l.isHighlighter) StrokeCap.Square else StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            ),
                                            blendMode = finalBlendMode
                                        )
                                    }
                                
                                // Dashed selection bounds
                                if (minX < maxX && minY < maxY) {
                                    val sMinX = cX + (minX - cX) * selectionScale + selectionDragOffset.x
                                    val sMinY = cY + (minY - cY) * selectionScale + selectionDragOffset.y
                                    val sMaxX = cX + (maxX - cX) * selectionScale + selectionDragOffset.x
                                    val sMaxY = cY + (maxY - cY) * selectionScale + selectionDragOffset.y

                                    val boundsPadding = 16f
                                    drawRect(
                                        color = primaryColor,
                                        topLeft = Offset(sMinX - boundsPadding, sMinY - boundsPadding),
                                        size = Size(sMaxX - sMinX + boundsPadding * 2, sMaxY - sMinY + boundsPadding * 2),
                                        style = Stroke(
                                            width = 1.dp.toPx(),
                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    )

                                    // Scale Handle
                                    drawCircle(
                                        color = primaryColor,
                                        radius = 6.dp.toPx(),
                                        center = Offset(sMaxX + boundsPadding, sMaxY + boundsPadding)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 4.dp.toPx(),
                                        center = Offset(sMaxX + boundsPadding, sMaxY + boundsPadding)
                                    )
                                }
                            }
                        }

                        // Render Static Text natively via Compose to fix multi-line metrics and Android's Canvas jump disparities
                        drawingLines.forEach { line ->
                            if (line.text != null && line.points.isNotEmpty()) {
                                val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
                                val xPosDp = with(LocalDensity.current) { line.points.first().x.toDp() }
                                val maxTextWidth = availableWidth - xPosDp - 4.dp
                                Text(
                                    text = line.text,
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(kotlin.math.round(line.points.first().x).toInt(), kotlin.math.round(line.points.first().y).toInt())
                                        }
                                        .widthIn(max = maxTextWidth),
                                    style = androidx.compose.ui.text.TextStyle(
                                        color = activeLineColor,
                                        fontSize = with(LocalDensity.current) { line.strokeWidth.toSp() },
                                        lineHeight = with(LocalDensity.current) { (TEXT_LARGE * 1.2f).toSp() },
                                        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                                            alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                                            trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None
                                        ),
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    )
                                )
                            }
                        }

                        // Inline Text Tool Layer
                        if (activeTextInputPosition != null) {
                            val cVal = Color(currentColorValue.toULong())
                            val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else strokeColor
                            val xPosDp = with(LocalDensity.current) { activeTextInputPosition!!.x.toDp() }
                            val maxTextWidth = availableWidth - xPosDp - 4.dp
                            BasicTextField(
                                value = activeTextValue,
                                onValueChange = {
                                    activeTextValue = it
                                },
                                modifier = Modifier
                                    .offset { IntOffset(kotlin.math.round(activeTextInputPosition!!.x).toInt(), kotlin.math.round(activeTextInputPosition!!.y).toInt()) }
                                    .widthIn(max = maxTextWidth)
                                    .focusRequester(focusRequester)
                                    .background(Color.Transparent),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    autoCorrectEnabled = false
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = chosenColor,
                                    fontSize = with(LocalDensity.current) { currentTextSize.toSp() },
                                    lineHeight = with(LocalDensity.current) { (TEXT_LARGE * 1.2f).toSp() },
                                    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                                        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                                        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None
                                    ),
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(strokeColor)
                            )
                            LaunchedEffect(activeTextValue.text) {
                                kotlinx.coroutines.delay(400)
                                commitChanges()
                            }
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

// Helper to determine if tap is inside the selection bounds
private fun isPointInSelectionBounds(
    point: Offset,
    selectedLines: List<DrawingLine>,
    dragOffset: Offset,
    scale: Float
): Boolean {
    if (selectedLines.isEmpty()) return false
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    selectedLines.forEach { l ->
        if (l.isEraser) return@forEach
        l.points.forEach { pt ->
            if (pt.x < minX) minX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.x > maxX) maxX = pt.x
            if (pt.y > maxY) maxY = pt.y
        }
    }
    
    val cX = (minX + maxX) / 2f
    val cY = (minY + maxY) / 2f

    val sMinX = cX + (minX - cX) * scale + dragOffset.x
    val sMinY = cY + (minY - cY) * scale + dragOffset.y
    val sMaxX = cX + (maxX - cX) * scale + dragOffset.x
    val sMaxY = cY + (maxY - cY) * scale + dragOffset.y

    val boundsPadding = 24f
    return point.x in (sMinX - boundsPadding)..(sMaxX + boundsPadding) &&
           point.y in (sMinY - boundsPadding)..(sMaxY + boundsPadding)
}

private fun isPointInScaleHandle(
    point: Offset,
    selectedLines: List<DrawingLine>,
    dragOffset: Offset,
    scale: Float
): Boolean {
    if (selectedLines.isEmpty()) return false
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    selectedLines.forEach { l ->
        if (l.isEraser) return@forEach
        l.points.forEach { pt ->
            if (pt.x < minX) minX = pt.x
            if (pt.y < minY) minY = pt.y
            if (pt.x > maxX) maxX = pt.x
            if (pt.y > maxY) maxY = pt.y
        }
    }
    val cX = (minX + maxX) / 2f
    val cY = (minY + maxY) / 2f
    
    val sMaxX = cX + (maxX - cX) * scale + dragOffset.x
    val sMaxY = cY + (maxY - cY) * scale + dragOffset.y

    val boundsPadding = 16f
    val handleX = sMaxX + boundsPadding
    val handleY = sMaxY + boundsPadding
    val hitRadius = 60f // generous hit area for touch
    
    val dx = point.x - handleX
    val dy = point.y - handleY
    return (dx * dx + dy * dy) <= hitRadius * hitRadius
}

private fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var isInside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]
        if ((pi.y > point.y) != (pj.y > point.y) &&
            point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
        ) {
            isInside = !isInside
        }
        j = i
    }
    return isInside
}
