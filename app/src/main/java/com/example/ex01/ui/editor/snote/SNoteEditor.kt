package com.example.ex01.ui.editor.snote

// Trigger IDE analysis

import com.example.ex01.utils.*


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.layout
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SNoteEditor(
    serializedBody: String,
    onSerializedBodyChange: (String) -> Unit,
    viewModel: SNoteViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) = with(viewModel) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var currentTextSize by remember { mutableFloatStateOf(prefs.getFloat("text_size", TEXT_LARGE)) }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
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
    var currentCanvasWidthPx by remember { mutableFloatStateOf(0f) }
    var currentDensity = 1f
    var activeTextLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val staticTextLayouts = remember { mutableMapOf<DrawingLine, androidx.compose.ui.text.TextLayoutResult>() }
    var needsAutoCommitAfterPaste by remember { mutableStateOf(false) }
    var showLassoMenu by remember { mutableStateOf(false) }
    var showLassoColorPicker by remember { mutableStateOf(false) }
    var lassoMenuPosition by remember { mutableStateOf(Offset.Zero) }

    val coroutineScope = rememberCoroutineScope()

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
                if (l.text != null && l.points.isNotEmpty()) {
                    val startPt = l.points.first()
                    val tw = staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                    val th = staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
                    if (startPt.x < minX) minX = startPt.x
                    if (startPt.y < minY) minY = startPt.y
                    if (startPt.x + tw > maxX) maxX = startPt.x + tw
                    if (startPt.y + th > maxY) maxY = startPt.y + th
                } else {
                    val halfStroke = l.strokeWidth / 2f
                    l.points.forEach { pt ->
                        if (pt.x - halfStroke < minX) minX = pt.x - halfStroke
                        if (pt.y - halfStroke < minY) minY = pt.y - halfStroke
                        if (pt.x + halfStroke > maxX) maxX = pt.x + halfStroke
                        if (pt.y + halfStroke > maxY) maxY = pt.y + halfStroke
                    }
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
                if (l.text != null && l.points.isNotEmpty()) {
                    val startPt = l.points.first()
                    val tw = staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                    val th = staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
                    if (startPt.x < minX) minX = startPt.x
                    if (startPt.y < minY) minY = startPt.y
                    if (startPt.x + tw > maxX) maxX = startPt.x + tw
                    if (startPt.y + th > maxY) maxY = startPt.y + th
                } else {
                    val halfStroke = l.strokeWidth / 2f
                    l.points.forEach { pt ->
                        if (pt.x - halfStroke < minX) minX = pt.x - halfStroke
                        if (pt.y - halfStroke < minY) minY = pt.y - halfStroke
                        if (pt.x + halfStroke > maxX) maxX = pt.x + halfStroke
                        if (pt.y + halfStroke > maxY) maxY = pt.y + halfStroke
                    }
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
                // --- TEXT PAGINATION ALGORITHM ---
                val startX = activeTextInputPosition!!.x
                val startY = activeTextInputPosition!!.y
                val fullText = activeTextValue.text

                if (activeTextLayoutResult != null && pageHeightPx > 0f) {
                    val layRes = activeTextLayoutResult!!
                    var currentBlockStartIdx = 0
                    var currentBlockY = startY
                    var cumulativeGapOffset = 0f

                    for (i in 0 until layRes.lineCount) {
                        val virtualTop = startY + layRes.getLineTop(i) + cumulativeGapOffset
                        val virtualBottom = startY + layRes.getLineBottom(i) + cumulativeGapOffset
                        val targetPage = kotlin.math.floor(virtualTop / pageHeightPx).toInt()
                        val pageBottom = (targetPage + 1) * pageHeightPx - (24f * currentDensity)

                        if (virtualBottom > pageBottom) {
                            if (i > 0) {
                                val endIdx = layRes.getLineEnd(i - 1)
                                if (endIdx > currentBlockStartIdx) {
                                    drawingLines.add(
                                        DrawingLine(
                                            points = listOf(Offset(startX, currentBlockY)),
                                            color = chosenColor,
                                            strokeWidth = currentTextSize,
                                            text = fullText.substring(currentBlockStartIdx, endIdx).trimEnd('\n', '\r')
                                        )
                                    )
                                }
                                currentBlockStartIdx = layRes.getLineStart(i)
                            }
                            
                            val nxtPage = targetPage + 1
                            var newBlockY = nxtPage * pageHeightPx + (32f * currentDensity)
                            val rowHeight = TEXT_LARGE * 1.2f
                            newBlockY = kotlin.math.ceil(newBlockY / rowHeight) * rowHeight
                            
                            cumulativeGapOffset += (newBlockY - virtualTop)
                            currentBlockY = newBlockY
                        }
                    }
                    if (currentBlockStartIdx < fullText.length) {
                        drawingLines.add(
                            DrawingLine(
                                points = listOf(Offset(startX, currentBlockY)),
                                color = chosenColor,
                                strokeWidth = currentTextSize,
                                text = fullText.substring(currentBlockStartIdx, fullText.length)
                            )
                        )
                    }
                } else {
                    // Fallback block mapping if no precise text layout
                    drawingLines.add(
                        DrawingLine(
                            points = listOf(Offset(startX, startY)),
                            color = chosenColor,
                            strokeWidth = currentTextSize,
                            text = fullText
                        )
                    )
                }
                // ------------------------------------
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

    LaunchedEffect(triggerAddPage) {
        if (triggerAddPage) {
            commitActiveText()
            pageCount++
            triggerAddPage = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        SNoteToolbar(
            viewModel = viewModel,
            currentColorValue = currentColorValue,
            currentThickness = currentThickness,
            currentEraserThickness = currentEraserThickness,
            currentHighlighterThickness = currentHighlighterThickness,
            currentTextSize = currentTextSize,
            onThicknessChange = ::updatePenThickness,
            onEraserThicknessChange = ::updateEraserThickness,
            onHighlighterThicknessChange = ::updateHighlighterThickness,
            onTextSizeChange = ::updateTextSize,
            onColorChange = ::updatePenColor,
            commitActiveText = commitActiveText,
            commitChanges = commitChanges
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            shadowElevation = 0.dp,
            color = androidx.compose.ui.graphics.Color.Transparent
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableHeight = this.maxHeight
                val availableWidth = this.maxWidth
                val density = LocalDensity.current
                val availableWidthPx = with(density) { availableWidth.toPx() }
                currentCanvasWidthPx = availableWidthPx
                currentDensity = density.density

                LaunchedEffect(availableHeight) {
                    if (pageHeightDp == 0.dp) {
                        pageHeightDp = availableHeight
                        pageHeightPx = with(density) { availableHeight.toPx() }
                    }
                }

                val scrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (pageHeightDp > 0.dp) {
                        // Background & Dividers Layer
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pageHeightDp * pageCount)
                        ) {
                            drawRect(color = androidx.compose.ui.graphics.Color(0xFFE5E5E5), size = size)
                            val pageGap = 24.dp.toPx()
                            for (i in 0 until pageCount) {
                                val yStart = i * pageHeightPx
                                val rectHeight = pageHeightPx - pageGap
                                drawRoundRect(
                                    color = eraserColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, yStart),
                                    size = androidx.compose.ui.geometry.Size(size.width, rectHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                                )
                            }
                        }

                        // Drawing Storkes Layer
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pageHeightDp * pageCount)
                                .graphicsLayer(alpha = 0.99f) // Force offscreen layer to support true transparent erasing
                                .pointerInput(currentColorValue, currentThickness, currentEraserThickness, isEraserMode, isTextMode, isLassoMode) {
                                    awaitPointerEventScope {
                                        var textModeDownPos: Offset? = null
                                        var dragStartOffset = Offset.Zero
                                        var dragStartScale = 1f
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: continue

                                            // Yield touch to other UI components that consumed it first (like tapping the text cursor)
                                            if (change.isConsumed) {
                                                textModeDownPos = null
                                                continue
                                            }

                                            val isStylus = change.type == PointerType.Stylus
                                            val isStylusEraser = change.type == PointerType.Eraser

                                            if (!isStylus && !isStylusEraser && !isTextMode) continue

                                            if (!isTextMode) {
                                                // Consume the event so horizontal/vertical scroll doesn't intercept it while drawing
                                                change.consume()
                                            }

                                            if (change.pressed && !change.previousPressed) {
                                                // Prevent starting drawing or text inside the page gap
                                                if (pageHeightPx > 0f) {
                                                    val relY = change.position.y % pageHeightPx
                                                    val gapPx = 24f * currentDensity
                                                    if (relY > pageHeightPx - gapPx) {
                                                        continue
                                                    }
                                                }

                                                if (isTextMode) {
                                                    textModeDownPos = change.position
                                                }
                                                if (isLassoMode) {
                                                    // Handle lasso tool start
                                                    change.consume()
                                                    val tapPos = change.position
                                                    
                                                    // Determine if tap is inside the current selection bounding box or handle
                                                    // pre-compute text bounds for hit tests
                                                    val tBounds = selectedLines.filter { it.text != null }.associateWith { 
                                                        val tw = staticTextLayouts[it]?.size?.width?.toFloat() ?: (it.strokeWidth * 0.6f * it.text!!.length.toFloat())
                                                        val th = staticTextLayouts[it]?.size?.height?.toFloat() ?: (it.strokeWidth * 1.5f)
                                                        Pair(tw, th)
                                                    }
                                                    val draggingHandle = selectedLines.isNotEmpty() && isPointInScaleHandle(tapPos, selectedLines, selectionDragOffset, selectionScale, tBounds)
                                                    val hittingMenuHandle = selectedLines.isNotEmpty() && isPointInMenuHandle(tapPos, selectedLines, selectionDragOffset, selectionScale, tBounds)
                                                    val dragging = !draggingHandle && !hittingMenuHandle && selectedLines.isNotEmpty() && isPointInSelectionBounds(tapPos, selectedLines, selectionDragOffset, selectionScale, tBounds)

                                                    if (hittingMenuHandle) {
                                                        showLassoMenu = true
                                                        showLassoColorPicker = false
                                                        lassoMenuPosition = tapPos
                                                    } else if (draggingHandle) {
                                                        isScalingSelection = true
                                                        showLassoMenu = false
                                                        showLassoColorPicker = false
                                                        dragStartScale = selectionScale
                                                        dragStartOffset = selectionDragOffset
                                                    } else if (dragging) {
                                                        isDraggingSelection = true
                                                        showLassoMenu = false
                                                        showLassoColorPicker = false
                                                        dragStartOffset = selectionDragOffset
                                                        dragStartScale = selectionScale
                                                    } else {
                                                        showLassoMenu = false
                                                        showLassoColorPicker = false
                                                        // Tap outside selection -> clear previous selection and start new lasso
                                                        if (selectedLines.isNotEmpty()) {
                                                            commitLassoSelection()
                                                        }
                                                        lassoPath = listOf(tapPos)
                                                    }
                                                    } else if (!isTextMode && currentPath == null) {
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
                                                     val relY = change.position.y % pageHeightPx
                                                     val gapPx = 24f * currentDensity
                                                     if (pageHeightPx > 0f && relY > pageHeightPx - gapPx) {
                                                         drawingLines.add(currentProperties.copy(points = currentPath!!))
                                                         undoneLines.clear()
                                                         currentPath = null
                                                         commitChanges()
                                                     } else {
                                                         currentPath = currentPath!! + change.position
                                                     }
                                                 }
                                             } else if (!change.pressed && change.previousPressed) {
                                                if (isTextMode) {
                                                    val downPos = textModeDownPos
                                                    textModeDownPos = null
                                                    
                                                    if (downPos == null) {
                                                        // Pointer event was consumed (e.g. scroll intercepted it), abort tap processing
                                                        continue
                                                    }
                                                    
                                                    val dx = change.position.x - downPos.x
                                                    val dy = change.position.y - downPos.y
                                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                                    
                                                    // If dragged more than 10dp, treat it as a scroll/pan, NOT a text click
                                                    if (dist > with(density) { 10.dp.toPx() }) {
                                                        continue
                                                    }

                                                    // If parent scroll consumed the pointer, we won't get a clean tap here
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
                                                            val layRes = staticTextLayouts[l]
                                                            if (layRes != null) {
                                                                val py = l.points.first().y
                                                                if (tapPos.y >= py && tapPos.y <= py + layRes.size.height) {
                                                                    hitIndex = i
                                                                    break
                                                                }
                                                            } else {
                                                                val py = l.points.first().y
                                                                val startRow = kotlin.math.round(py / rowHeight).toInt()

                                                                val maxTextWidthPx = availableWidthPx - l.points.first().x - with(density) { 4.dp.toPx() }
                                                                val pEst = android.graphics.Paint().apply { textSize = l.strokeWidth }
                                                                var visualRows = 0
                                                                for (lineStr in l.text.split("\n")) {
                                                                    val w = pEst.measureText(lineStr)
                                                                    if (maxTextWidthPx > 0f) {
                                                                        visualRows += kotlin.math.max(1, kotlin.math.ceil((w / (maxTextWidthPx * 0.95f)).toDouble()).toInt())
                                                                    } else {
                                                                        visualRows += 1
                                                                    }
                                                                }
                                                                val endRow = startRow + visualRows

                                                                if (clickedRowIndex in startRow..endRow) {
                                                                    hitIndex = i
                                                                    break
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (hitIndex != -1) {
                                                        val hitLine = drawingLines.removeAt(hitIndex)
                                                        originalHitLine = hitLine
                                                        originalHitIndex = hitIndex
                                                        updatePenColor(hitLine.color.value.toLong())
                                                        activeTextInputPosition = hitLine.points.first()
                                                        val safeText = hitLine.text!! // Keep original newlines

                                                        val layRes = staticTextLayouts[hitLine]
                                                        var finalCharIdx = safeText.length
                                                        if (layRes != null) {
                                                            val localOffset = tapPos - hitLine.points.first()
                                                            finalCharIdx = layRes.getOffsetForPosition(localOffset)
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
                                                    if (isScalingSelection || isDraggingSelection) {
                                                        if (isScalingSelection) isScalingSelection = false
                                                        if (isDraggingSelection) isDraggingSelection = false

                                                        if (selectedLines.isNotEmpty() && pageHeightPx > 0f) {
                                                            var minY = Float.MAX_VALUE
                                                            var maxY = Float.MIN_VALUE
                                                            var minX = Float.MAX_VALUE
                                                            var maxX = Float.MIN_VALUE
                                                            selectedLines.forEach { l ->
                                                                if (!l.isEraser) {
                                                                    if (l.text != null && l.points.isNotEmpty()) {
                                                                        val startPt = l.points.first()
                                                                        val tw = staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                                                                        val th = staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
                                                                        if (startPt.x < minX) minX = startPt.x
                                                                        if (startPt.y < minY) minY = startPt.y
                                                                        if (startPt.x + tw > maxX) maxX = startPt.x + tw
                                                                        if (startPt.y + th > maxY) maxY = startPt.y + th
                                                                    } else {
                                                                        val halfStroke = l.strokeWidth / 2f
                                                                        l.points.forEach { pt ->
                                                                            if (pt.x - halfStroke < minX) minX = pt.x - halfStroke
                                                                            if (pt.y - halfStroke < minY) minY = pt.y - halfStroke
                                                                            if (pt.x + halfStroke > maxX) maxX = pt.x + halfStroke
                                                                            if (pt.y + halfStroke > maxY) maxY = pt.y + halfStroke
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            if (minY <= maxY) {
                                                                val cY = (minY + maxY) / 2f
                                                                val cX = (minX + maxX) / 2f
                                                                val topY = cY + (minY - cY) * selectionScale + selectionDragOffset.y
                                                                val bottomY = cY + (maxY - cY) * selectionScale + selectionDragOffset.y
                                                                val leftX = cX + (minX - cX) * selectionScale + selectionDragOffset.x
                                                                val rightX = cX + (maxX - cX) * selectionScale + selectionDragOffset.x
                                                                val gapPx = 24f * currentDensity
                                                                
                                                                val startPage = kotlin.math.floor(topY / pageHeightPx).toInt()
                                                                val endPage = kotlin.math.floor(bottomY / pageHeightPx).toInt()
                                                                
                                                                var overlapsGap = false
                                                                if (startPage != endPage) {
                                                                    overlapsGap = true // Crosses the boundary between pages
                                                                } else {
                                                                    val relativeBottom = bottomY % pageHeightPx
                                                                    if (relativeBottom > pageHeightPx - gapPx) {
                                                                        overlapsGap = true // Bottom touches the gap
                                                                    }
                                                                }
                                                                
                                                                if (overlapsGap || topY < 0f || leftX < 0f || rightX > currentCanvasWidthPx) {
                                                                    selectionDragOffset = dragStartOffset
                                                                    selectionScale = dragStartScale
                                                                }
                                                            }
                                                        }
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
                                                            val isSelected = if (l.text != null && l.points.isNotEmpty()) {
                                                                val startPt = l.points.first()
                                                                val tw = staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                                                                val th = staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
                                                                val cX = startPt.x + tw / 2f
                                                                val cY = startPt.y + th / 2f
                                                                val ptsToCheck = listOf(
                                                                    startPt,
                                                                    Offset(startPt.x + tw, startPt.y),
                                                                    Offset(startPt.x, startPt.y + th),
                                                                    Offset(startPt.x + tw, startPt.y + th),
                                                                    Offset(cX, cY)
                                                                )
                                                                ptsToCheck.any { p -> lassoRect.contains(p) && isPointInPolygon(p, capturedLassoPath) }
                                                            } else {
                                                                l.points.any { pt ->
                                                                    if (!lassoRect.contains(pt)) return@any false
                                                                    if (!isPointInPolygon(pt, capturedLassoPath)) return@any false
                                                                    if (l.text != null) return@any true
                                                                    var pointErased = false
                                                                    for (j in index + 1 until drawingLines.size) {
                                                                        val e = drawingLines[j]
                                                                        if (e.isEraser) {
                                                                            val rSq = e.strokeWidth * e.strokeWidth
                                                                            for (ept in e.points) {
                                                                                val dx = pt.x - ept.x
                                                                                val dy = pt.y - ept.y
                                                                                if (dx * dx + dy * dy <= rSq) {
                                                                                    pointErased = true
                                                                                    break
                                                                                }
                                                                            }
                                                                            if (pointErased) break
                                                                        }
                                                                    }
                                                                    !pointErased
                                                                }
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
                                        if (l.text != null && l.points.isNotEmpty()) {
                                            val startPt = l.points.first()
                                            val tw = staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                                            val th = staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
                                            if (startPt.x < minX) minX = startPt.x
                                            if (startPt.y < minY) minY = startPt.y
                                            if (startPt.x + tw > maxX) maxX = startPt.x + tw
                                            if (startPt.y + th > maxY) maxY = startPt.y + th
                                        } else {
                                            val halfStroke = l.strokeWidth / 2f
                                            l.points.forEach { pt ->
                                                if (pt.x - halfStroke < minX) minX = pt.x - halfStroke
                                                if (pt.y - halfStroke < minY) minY = pt.y - halfStroke
                                                if (pt.x + halfStroke > maxX) maxX = pt.x + halfStroke
                                                if (pt.y + halfStroke > maxY) maxY = pt.y + halfStroke
                                            }
                                        }
                                    }
                                    val cX = (minX + maxX) / 2f
                                    val cY = (minY + maxY) / 2f

                                    selectedLines.forEach { l ->
                                        val activeLineColor = if (l.color == Color.Unspecified || l.color == Color.Black || l.color == Color.White) strokeColor else l.color
                                        val finalColor = when {
                                            l.isEraser -> Color.Black
                                            l.isHighlighter -> activeLineColor.copy(alpha = 0.4f)
                                            else -> activeLineColor.copy(alpha = 0.7f)
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

                                    // Menu Handle
                                    drawCircle(
                                        color = primaryColor,
                                        radius = 6.dp.toPx(),
                                        center = Offset(sMaxX + boundsPadding, sMinY - boundsPadding)
                                    )
                                    val hr = 1.dp.toPx()
                                    val menuHandleCenter = Offset(sMaxX + boundsPadding, sMinY - boundsPadding)
                                    drawCircle(color = Color.White, radius = hr, center = menuHandleCenter.copy(y = menuHandleCenter.y - 3.dp.toPx()))
                                    drawCircle(color = Color.White, radius = hr, center = menuHandleCenter)
                                    drawCircle(color = Color.White, radius = hr, center = menuHandleCenter.copy(y = menuHandleCenter.y + 3.dp.toPx()))
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
                                    onTextLayout = { textLayoutResult ->
                                        staticTextLayouts[line] = textLayoutResult
                                        val bottomY = line.points.first().y + textLayoutResult.size.height
                                        if (pageHeightPx > 0) {
                                            val neededPages = kotlin.math.ceil((bottomY / pageHeightPx).toDouble()).toInt()
                                            if (neededPages > pageCount) {
                                                pageCount = neededPages
                                            }
                                        }
                                    },
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

                        // Render Selected Text dynamically while dragging/scaling
                        if (selectedLines.isNotEmpty()) {
                            var minX = Float.MAX_VALUE
                            var minY = Float.MAX_VALUE
                            var maxX = Float.MIN_VALUE
                            var maxY = Float.MIN_VALUE
                            selectedLines.forEach { l ->
                                if (l.isEraser) return@forEach
                                if (l.text != null && l.points.isNotEmpty()) {
                                    val startPt = l.points.first()
                                    val tw = staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                                    val th = staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
                                    if (startPt.x < minX) minX = startPt.x
                                    if (startPt.y < minY) minY = startPt.y
                                    if (startPt.x + tw > maxX) maxX = startPt.x + tw
                                    if (startPt.y + th > maxY) maxY = startPt.y + th
                                } else {
                                    val halfStroke = l.strokeWidth / 2f
                                    l.points.forEach { pt ->
                                        if (pt.x - halfStroke < minX) minX = pt.x - halfStroke
                                        if (pt.y - halfStroke < minY) minY = pt.y - halfStroke
                                        if (pt.x + halfStroke > maxX) maxX = pt.x + halfStroke
                                        if (pt.y + halfStroke > maxY) maxY = pt.y + halfStroke
                                    }
                                }
                            }
                            val cX = if (minX < maxX) (minX + maxX) / 2f else 0f
                            val cY = if (minY < maxY) (minY + maxY) / 2f else 0f

                            selectedLines.forEach { line ->
                                if (line.text != null && line.points.isNotEmpty()) {
                                    val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
                                    val pt = line.points.first()
                                    val pX = cX + (pt.x - cX) * selectionScale + selectionDragOffset.x
                                    val pY = cY + (pt.y - cY) * selectionScale + selectionDragOffset.y
                                    
                                    val xPosDp = with(LocalDensity.current) { pX.coerceAtLeast(0f).toDp() }
                                    val maxTextWidth = (availableWidth - xPosDp - 4.dp).coerceAtLeast(10.dp)
                                    
                                    Text(
                                        text = line.text,
                                        modifier = Modifier
                                            .offset { IntOffset(kotlin.math.round(pX).toInt(), kotlin.math.round(pY).toInt()) }
                                            .widthIn(max = maxTextWidth),
                                        style = androidx.compose.ui.text.TextStyle(
                                            color = activeLineColor.copy(alpha = 0.7f),
                                            fontSize = with(LocalDensity.current) { (line.strokeWidth * selectionScale).coerceAtLeast(1f).toSp() },
                                            lineHeight = with(LocalDensity.current) { (TEXT_LARGE * 1.2f * selectionScale).coerceAtLeast(1f).toSp() },
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
                        }

                        // Lasso Context Menu
                        if (showLassoMenu && selectedLines.isNotEmpty()) {
                            val xPosDp = with(LocalDensity.current) { lassoMenuPosition.x.toDp() }
                            val yPosDp = with(LocalDensity.current) { lassoMenuPosition.y.toDp() }
                            Box(modifier = Modifier.offset(x = xPosDp, y = yPosDp)) {
                                DropdownMenu(
                                    expanded = showLassoMenu,
                                    onDismissRequest = { showLassoMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Colour") },
                                        leadingIcon = { Icon(Icons.Default.Create, contentDescription = "Colour") },
                                        onClick = {
                                            showLassoMenu = false
                                            showLassoColorPicker = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                                        onClick = {
                                            drawingLines.removeAll(selectedLines)
                                            selectedLines.clear()
                                            showLassoMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Lasso Color Picker
                        if (showLassoColorPicker && selectedLines.isNotEmpty()) {
                            val xPosDp = with(LocalDensity.current) { lassoMenuPosition.x.toDp() }
                            val yPosDp = with(LocalDensity.current) { lassoMenuPosition.y.toDp() }
                            Box(modifier = Modifier.offset(x = xPosDp, y = yPosDp)) {
                                DropdownMenu(
                                    expanded = showLassoColorPicker,
                                    onDismissRequest = { showLassoColorPicker = false },
                                    modifier = Modifier.width(64.dp)
                                ) {
                                    val penCols = listOf(Color.Unspecified) + ALLOWED_PEN_COLORS
                                    penCols.forEach { c ->
                                        DropdownMenuItem(
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            text = {
                                                Box(modifier = Modifier.fillMaxWidth().height(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                                    Box(modifier = Modifier
                                                        .size(24.dp)
                                                        .background(if (c == Color.Unspecified) strokeColor else c, shape = CircleShape)
                                                        .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                val newSelection = selectedLines.map { l -> l.copy(color = c) }
                                                selectedLines.clear()
                                                selectedLines.addAll(newSelection)
                                                if (c != Color.Unspecified) {
                                                    updatePenColor(c.value.toLong())
                                                } else {
                                                    updatePenColor(Color.Unspecified.value.toLong())
                                                }
                                                showLassoColorPicker = false
                                                commitChanges()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Inline Text Tool Layer
                        if (activeTextInputPosition != null) {
                            val cVal = Color(currentColorValue.toULong())
                            val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else strokeColor
                            val xPosDp = with(LocalDensity.current) { activeTextInputPosition!!.x.toDp() }
                            val yPosDp = with(LocalDensity.current) { activeTextInputPosition!!.y.toDp() }
                            val maxTextWidth = availableWidth - xPosDp - 4.dp

                            Column {
                                Spacer(modifier = Modifier.height(yPosDp))
                                Row {
                                    Spacer(modifier = Modifier.width(xPosDp))
                                    BasicTextField(
                                        value = activeTextValue,
                                        onValueChange = {
                                            if (it.text.length - activeTextValue.text.length > 50) {
                                                needsAutoCommitAfterPaste = true
                                            }
                                            activeTextValue = it
                                        },
                                        onTextLayout = { textLayoutResult ->
                                            activeTextLayoutResult = textLayoutResult
                                            val bottomY = activeTextInputPosition!!.y + textLayoutResult.size.height
                                            if (pageHeightPx > 0) {
                                                val neededPages = kotlin.math.ceil((bottomY / pageHeightPx).toDouble()).toInt()
                                                if (neededPages > pageCount) {
                                                    pageCount = neededPages
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .bringIntoViewRequester(bringIntoViewRequester)
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
                                }
                            }

                            LaunchedEffect(activeTextInputPosition, activeTextLayoutResult, availableHeight) {
                                // Trigger scroll whenever position is initially clicked and layout is ready
                                if (activeTextLayoutResult != null) {
                                    kotlinx.coroutines.delay(50) // Brief delay to let IME padding settle
                                    try {
                                        val cursorOffset = activeTextValue.selection.end.coerceIn(0, activeTextLayoutResult!!.layoutInput.text.text.length)
                                        val cursorRect = activeTextLayoutResult!!.getCursorRect(cursorOffset)
                                        val absoluteCursorTop = activeTextInputPosition!!.y + cursorRect.top - 60f
                                        val absoluteCursorBottom = activeTextInputPosition!!.y + cursorRect.bottom + 140f
                                        val viewportTop = scrollState.value.toFloat()
                                        val currentViewportHeight = with(density) { availableHeight.toPx() }
                                        val viewportBottom = viewportTop + currentViewportHeight

                                        if (absoluteCursorBottom > viewportBottom) {
                                            scrollState.animateScrollTo((absoluteCursorBottom - currentViewportHeight).toInt())
                                        } else if (absoluteCursorTop < viewportTop) {
                                            scrollState.animateScrollTo(absoluteCursorTop.toInt())
                                        }
                                    } catch (e: Exception) {}
                                }
                            }

                            LaunchedEffect(activeTextValue.text, activeTextValue.selection, availableHeight) {
                                kotlinx.coroutines.delay(10)
                                if (needsAutoCommitAfterPaste && activeTextLayoutResult != null) {
                                    needsAutoCommitAfterPaste = false
                                    commitActiveText()
                                }
                                activeTextLayoutResult?.let {
                                    try {
                                        val cursorOffset = activeTextValue.selection.end.coerceIn(0, it.layoutInput.text.text.length)
                                        val cursorRect = it.getCursorRect(cursorOffset)
                                        val absoluteCursorTop = activeTextInputPosition!!.y + cursorRect.top - 60f
                                        val absoluteCursorBottom = activeTextInputPosition!!.y + cursorRect.bottom + 140f
                                        val viewportTop = scrollState.value.toFloat()
                                        val currentViewportHeight = with(density) { availableHeight.toPx() }
                                        val viewportBottom = viewportTop + currentViewportHeight

                                        if (absoluteCursorBottom > viewportBottom) {
                                            scrollState.animateScrollTo((absoluteCursorBottom - currentViewportHeight).toInt())
                                        } else if (absoluteCursorTop < viewportTop) {
                                            scrollState.animateScrollTo(absoluteCursorTop.toInt())
                                        }
                                    } catch (e: Exception) {}
                                }
                                kotlinx.coroutines.delay(300)
                                commitChanges()
                            }

                            LaunchedEffect(activeTextInputPosition) {
                                kotlinx.coroutines.delay(100)
                                try {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        // Overlay page numbers
                        for (i in 0 until pageCount) {
                            val bottomY = pageHeightDp * (i + 1) - 34.dp
                            Text(
                                text = "Page ${i + 1}",
                                color = androidx.compose.ui.graphics.Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier
                                    .offset(x = availableWidth - 80.dp, y = bottomY)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                } // End if (pageHeightDp > 0.dp)
                } // End Box(verticalScroll)
            } // End BoxWithConstraints
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
