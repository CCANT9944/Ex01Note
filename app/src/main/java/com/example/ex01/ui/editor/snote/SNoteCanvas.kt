package com.example.ex01.ui.editor.snote
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import com.example.ex01.utils.*
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.text.input.TextFieldValue

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.example.ex01.utils.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.clipToBounds

@Composable
fun SNoteCanvas(
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    focusRequester: FocusRequester,
    bringIntoViewRequester: androidx.compose.foundation.relocation.BringIntoViewRequester,
    commitChanges: () -> Unit,
    commitActiveText: () -> Unit
) {
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val strokeColor = MaterialTheme.colorScheme.onSurface
    val eraserColor = MaterialTheme.colorScheme.surface
    val isHighlighterMode = viewModel.isHighlighterMode

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableHeight = this.maxHeight
                val availableWidth = this.maxWidth
                val density = LocalDensity.current
                val availableWidthPx = with(density) { availableWidth.toPx() }
                state.currentCanvasWidthPx = availableWidthPx
                state.currentDensity = density.density

                LaunchedEffect(availableHeight) {
                    if (state.pageHeightDp == 0.dp) {
                        state.pageHeightDp = availableHeight
                        state.pageHeightPx = with(density) { availableHeight.toPx() }
                    }
                }

                val scrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (state.pageHeightDp > 0.dp) {
                        // Background & Dividers Layer
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(state.pageHeightDp * viewModel.pageCount)
                        ) {
                            drawRect(color = androidx.compose.ui.graphics.Color(0xFFE5E5E5), size = size)
                            val pageGap = 24.dp.toPx()
                            for (i in 0 until viewModel.pageCount) {
                                val yStart = i * state.pageHeightPx
                                val rectHeight = state.pageHeightPx - pageGap
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
                                .height(state.pageHeightDp * viewModel.pageCount)
                                .graphicsLayer(alpha = 0.99f) // Force offscreen layer to support true transparent erasing
                                .pointerInput(state.currentColorValue, state.currentThickness, state.currentEraserThickness, viewModel.isEraserMode, viewModel.isTextMode, viewModel.isLassoMode) {
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

                                            if (!isStylus && !isStylusEraser && !viewModel.isTextMode) continue

                                            if (!viewModel.isTextMode) {
                                                // Consume the event so horizontal/vertical scroll doesn't intercept it while drawing
                                                change.consume()
                                            }

                                            if (change.pressed && !change.previousPressed) {
                                                // Prevent starting drawing or text inside the page gap
                                                if (state.pageHeightPx > 0f) {
                                                    val relY = change.position.y % state.pageHeightPx
                                                    val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                                    if (relY > state.pageHeightPx - gapPx) {
                                                        continue
                                                    }
                                                }

                                                if (viewModel.isTextMode) {
                                                    textModeDownPos = change.position
                                                }
                                                if (viewModel.isLassoMode) {
                                                    // Handle lasso tool start
                                                    change.consume()
                                                    val tapPos = change.position
                                                    
                                                    // Determine if tap is inside the current selection bounding box or handle
                                                    // pre-compute text bounds for hit tests
                                                    val tBounds = viewModel.selectedLines.filter { it.text != null }.associateWith { 
                                                        val tw = state.staticTextLayouts[it]?.size?.width?.toFloat() ?: (it.strokeWidth * 0.6f * it.text!!.length.toFloat())
                                                        val th = state.staticTextLayouts[it]?.size?.height?.toFloat() ?: (it.strokeWidth * 1.5f)
                                                        Pair(tw, th)
                                                    }
                                                    val draggingHandle = viewModel.selectedLines.isNotEmpty() && isPointInScaleHandle(tapPos, viewModel.selectedLines, viewModel.selectionDragOffset, viewModel.selectionScale, tBounds)
                                                    val hittingMenuHandle = viewModel.selectedLines.isNotEmpty() && isPointInMenuHandle(tapPos, viewModel.selectedLines, viewModel.selectionDragOffset, viewModel.selectionScale, tBounds)
                                                    val dragging = !draggingHandle && !hittingMenuHandle && viewModel.selectedLines.isNotEmpty() && isPointInSelectionBounds(tapPos, viewModel.selectedLines, viewModel.selectionDragOffset, viewModel.selectionScale, tBounds)

                                                    if (hittingMenuHandle) {
                                                        state.showLassoMenu = true
                                                        state.showLassoColorPicker = false
                                                        state.lassoMenuPosition = tapPos
                                                    } else if (draggingHandle) {
                                                        if (viewModel.preLassoState == null) {
                                                            viewModel.preLassoState = viewModel.drawingLines.toList() + viewModel.selectedLines.toList()
                                                        }
                                                        viewModel.isScalingSelection = true
                                                        state.showLassoMenu = false
                                                        state.showLassoColorPicker = false
                                                        dragStartScale = viewModel.selectionScale
                                                        dragStartOffset = viewModel.selectionDragOffset
                                                    } else if (dragging) {
                                                        if (viewModel.preLassoState == null) {
                                                            viewModel.preLassoState = viewModel.drawingLines.toList() + viewModel.selectedLines.toList()
                                                        }
                                                        viewModel.isDraggingSelection = true
                                                        state.showLassoMenu = false
                                                        state.showLassoColorPicker = false
                                                        dragStartOffset = viewModel.selectionDragOffset
                                                        dragStartScale = viewModel.selectionScale
                                                    } else {
                                                        state.showLassoMenu = false
                                                        state.showLassoColorPicker = false
                                                        // Tap outside selection -> clear previous selection and start new lasso
                                                        if (viewModel.selectedLines.isNotEmpty()) {
                                                            state.commitLassoSelection { commitChanges() }
                                                        }
                                                        viewModel.lassoPath = listOf(tapPos)
                                                    }
                                                    } else if (!viewModel.isTextMode && viewModel.currentPath == null) {
                                                    commitActiveText()
                                                    viewModel.currentPath = listOf(change.position)
                                                    val actualEraserMode = isStylusEraser || viewModel.isEraserMode
                                                    val cVal = Color(state.currentColorValue.toULong())
                                                    val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
                                                    viewModel.currentProperties = DrawingLine(
                                                        points = viewModel.currentPath!!,
                                                        color = if (actualEraserMode) Color.Unspecified else chosenColor,
                                                        strokeWidth = if (actualEraserMode) state.currentEraserThickness else if (isHighlighterMode) state.currentHighlighterThickness else state.currentThickness,
                                                        isEraser = actualEraserMode,
                                                        isHighlighter = isHighlighterMode
                                                     )
                                                 }
                                            } else if (change.pressed && change.previousPressed) {
                                                 if (viewModel.isLassoMode) {
                                                     change.consume()
                                                     if (viewModel.isScalingSelection) {
                                                         val dx = change.position.x - change.previousPosition.x
                                                         val dy = change.position.y - change.previousPosition.y
                                                         // Base scale change off total drag distance out/in
                                                         val scaleDelta = (dx + dy) / 400f
                                                         viewModel.selectionScale = kotlin.math.max(0.1f, viewModel.selectionScale + scaleDelta)
                                                     } else if (viewModel.isDraggingSelection) {
                                                         val dragAmount = change.position - change.previousPosition
                                                         viewModel.selectionDragOffset += dragAmount
                                                     } else if (viewModel.lassoPath != null) {
                                                         viewModel.lassoPath = viewModel.lassoPath!! + change.position
                                                     }
                                                 } else if (!viewModel.isTextMode && viewModel.currentPath != null) {
                                                     val relY = change.position.y % state.pageHeightPx
                                                     val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                                     if (state.pageHeightPx > 0f && relY > state.pageHeightPx - gapPx) {
                                                         viewModel.pushUndoState()
                                                         viewModel.drawingLines.add(viewModel.currentProperties.copy(points = viewModel.currentPath!!))
                                                         viewModel.currentPath = null
                                                         commitChanges()
                                                     } else {
                                                         viewModel.currentPath = viewModel.currentPath!! + change.position
                                                     }
                                                 }
                                             } else if (!change.pressed && change.previousPressed) {
                                                if (viewModel.isTextMode) {
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

                                                    val rowHeight = SNoteConfig.getRowHeight(TEXT_LARGE)
                                                    
                                                    // Check if tap position is in a page gap - if so, reject text placement
                                                    if (state.pageHeightPx > 0f) {
                                                        val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                                        val pageIndex = kotlin.math.floor(tapPos.y / state.pageHeightPx).toInt()
                                                        val pageStart = pageIndex * state.pageHeightPx
                                                        val contentEnd = pageStart + state.pageHeightPx - gapPx
                                                        val isInGap = tapPos.y >= contentEnd && tapPos.y < pageStart + state.pageHeightPx
                                                        if (isInGap) {
                                                            continue // Reject text placement in gap
                                                        }
                                                    }
                                                    
                                                    val targetY = SNoteConfig.snapYToRow(tapPos.y, state.pageHeightPx, rowHeight, state.currentDensity)
                                                    val clickedRowIndex = (tapPos.y / rowHeight).toInt()

                                                    var hitIndex = -1
                                                    for (i in viewModel.drawingLines.indices.reversed()) {
                                                        val l = viewModel.drawingLines[i]
                                                        if (l.text != null && l.points.isNotEmpty()) {
                                                            val layRes = state.staticTextLayouts[l]
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
                                                        viewModel.preEditTextState = viewModel.drawingLines.toList()
                                                        val hitLine = viewModel.drawingLines.removeAt(hitIndex)
                                                        viewModel.originalHitLine = hitLine
                                                        viewModel.originalHitIndex = hitIndex
                                                        state.updatePenColor(hitLine.color.value.toLong())
                                                        viewModel.activeTextInputPosition = hitLine.points.first()
                                                        val safeText = hitLine.text!! // Keep original newlines

                                                        val layRes = state.staticTextLayouts[hitLine]
                                                        var finalCharIdx = safeText.length
                                                        if (layRes != null) {
                                                            val localOffset = tapPos - hitLine.points.first()
                                                            finalCharIdx = layRes.getOffsetForPosition(localOffset)
                                                        }

                                                        viewModel.activeTextValue = TextFieldValue(safeText, selection = androidx.compose.ui.text.TextRange(finalCharIdx))
                                                        state.currentTextSize = hitLine.strokeWidth
                                                        commitChanges()
                                                    } else {
                                                        val defaultIndent = with(density) { 16.dp.toPx() }
                                                        // Final safety check: ensure targetY is never in a gap
                                                        val finalY = if (state.pageHeightPx > 0f) {
                                                            val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                                            val pageIdx = kotlin.math.floor(targetY / state.pageHeightPx).toInt()
                                                            val pageStart = pageIdx * state.pageHeightPx
                                                            val contentEnd = pageStart + state.pageHeightPx - gapPx
                                                            if (targetY >= contentEnd && targetY < pageStart + state.pageHeightPx) {
                                                                // In gap - snap to previous valid position
                                                                contentEnd - rowHeight
                                                            } else {
                                                                targetY
                                                            }
                                                        } else {
                                                            targetY
                                                        }
                                                        viewModel.activeTextInputPosition = Offset(defaultIndent, finalY)
                                                        viewModel.activeTextValue = TextFieldValue("")
                                                        commitChanges()
                                                    }
                                                } else if (viewModel.isLassoMode) {
                                                    if (viewModel.isScalingSelection || viewModel.isDraggingSelection) {
                                                        if (viewModel.isScalingSelection) viewModel.isScalingSelection = false
                                                        if (viewModel.isDraggingSelection) viewModel.isDraggingSelection = false

                                                        if (viewModel.selectedLines.isNotEmpty() && state.pageHeightPx > 0f) {
                                                            var minY = Float.MAX_VALUE
                                                            var maxY = Float.MIN_VALUE
                                                            var minX = Float.MAX_VALUE
                                                            var maxX = Float.MIN_VALUE
                                                            viewModel.selectedLines.forEach { l ->
                                                                if (!l.isEraser) {
                                                                    if (l.text != null && l.points.isNotEmpty()) {
                                                                        val startPt = l.points.first()
                                                                        val tw = state.staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                                                                        val th = state.staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
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
                                                                val topY = cY + (minY - cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
                                                                val bottomY = cY + (maxY - cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
                                                                val leftX = cX + (minX - cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
                                                                val rightX = cX + (maxX - cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
                                                                val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                                                
                                                                val startPage = kotlin.math.floor(topY / state.pageHeightPx).toInt()
                                                                val endPage = kotlin.math.floor(bottomY / state.pageHeightPx).toInt()
                                                                
                                                                var overlapsGap = false
                                                                if (startPage != endPage) {
                                                                    overlapsGap = true // Crosses the boundary between pages
                                                                } else {
                                                                    val relativeBottom = bottomY % state.pageHeightPx
                                                                    if (relativeBottom > state.pageHeightPx - gapPx) {
                                                                        overlapsGap = true // Bottom touches the gap
                                                                    }
                                                                }
                                                                
                                                                if (overlapsGap || topY < 0f || leftX < 0f || rightX > state.currentCanvasWidthPx) {
                                                                    viewModel.selectionDragOffset = dragStartOffset
                                                                    viewModel.selectionScale = dragStartScale
                                                                } else if (viewModel.selectionDragOffset != androidx.compose.ui.geometry.Offset.Zero || viewModel.selectionScale != 1f) {
                                                                    if (viewModel.preLassoState != null) {
                                                                        viewModel.pushUndoState(viewModel.preLassoState!!)
                                                                    }
                                                                    val finalizedLines = viewModel.selectedLines.map { l ->
                                                                        l.copy(
                                                                            points = l.points.map { p ->
                                                                                androidx.compose.ui.geometry.Offset(
                                                                                    cX + (p.x - cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x,
                                                                                    cY + (p.y - cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
                                                                                )
                                                                            },
                                                                            strokeWidth = l.strokeWidth * viewModel.selectionScale
                                                                        )
                                                                    }
                                                                    viewModel.selectedLines.clear()
                                                                    viewModel.selectedLines.addAll(finalizedLines)
                                                                    viewModel.selectionDragOffset = androidx.compose.ui.geometry.Offset.Zero
                                                                    viewModel.selectionScale = 1f
                                                                }
                                                                viewModel.preLassoState = null
                                                                commitChanges()
                                                            }
                                                        }
                                                    } else if (viewModel.lassoPath != null) {
                                                        // Lasso drawing finished, calculate enclosed lines
                                                        val capturedLassoPath = viewModel.lassoPath!!
                                                        viewModel.lassoPath = null
                                                        
                                                        val newSelection = mutableListOf<DrawingLine>()
                                                        val remainingLines = mutableListOf<DrawingLine>()
                                                        
                                                        // Basic AABB intersection logic for simplicity (can evolve to Polygon hit-test later)
                                                        val minX = capturedLassoPath.minOfOrNull { it.x } ?: 0f
                                                        val maxX = capturedLassoPath.maxOfOrNull { it.x } ?: 0f
                                                        val minY = capturedLassoPath.minOfOrNull { it.y } ?: 0f
                                                        val maxY = capturedLassoPath.maxOfOrNull { it.y } ?: 0f
                                                        val lassoRect = androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY)
                                                        
                                                        for ((index, l) in viewModel.drawingLines.withIndex()) {
                                                            if (l.points.isEmpty()) continue

                                                            if (l.isEraser) {
                                                                remainingLines.add(l)
                                                                continue
                                                            }

                                                            // Fast precise geometry intersection that mathematically ignores ANY points visually "covered" by subsequent erasers
                                                            val isSelected = if (l.text != null && l.points.isNotEmpty()) {
                                                                val startPt = l.points.first()
                                                                val tw = state.staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                                                                val th = state.staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
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
                                                                    for (j in index + 1 until viewModel.drawingLines.size) {
                                                                        val e = viewModel.drawingLines[j]
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
                                                            viewModel.drawingLines.clear()
                                                            viewModel.drawingLines.addAll(remainingLines)
                                                            viewModel.selectedLines.addAll(newSelection)
                                                            viewModel.selectionDragOffset = Offset.Zero
                                                            viewModel.selectionScale = 1f
                                                        }
                                                    }
                                                } else if (!viewModel.isTextMode && viewModel.currentPath != null) {
                                                    viewModel.pushUndoState()
                                                    viewModel.drawingLines.add(viewModel.currentProperties.copy(points = viewModel.currentPath!!))
                                                    viewModel.currentPath = null
                                                    commitChanges()
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            // Draw saved lines
                            viewModel.drawingLines.forEach { line ->
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
                            viewModel.currentPath?.let { pathOffsets ->
                                val activeLine = viewModel.currentProperties.copy(points = pathOffsets)
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
                            viewModel.lassoPath?.let { lPath ->
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
                            if (viewModel.selectedLines.isNotEmpty()) {
                                // Draw bounding box
                                    var minX = Float.MAX_VALUE
                                    var minY = Float.MAX_VALUE
                                    var maxX = Float.MIN_VALUE
                                    var maxY = Float.MIN_VALUE

                                    viewModel.selectedLines.forEach { l ->
                                        if (l.isEraser) return@forEach
                                        if (l.text != null && l.points.isNotEmpty()) {
                                            val startPt = l.points.first()
                                            val tw = state.staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                                            val th = state.staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
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

                                    viewModel.selectedLines.forEach { l ->
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
                                            val pX = cX + (pt.x - cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
                                            val pY = cY + (pt.y - cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y

                                            if (idx == 0) offsetPath.moveTo(pX, pY) else offsetPath.lineTo(pX, pY)
                                        }
                                        
                                        drawPath(
                                            path = offsetPath,
                                            color = finalColor,
                                            style = Stroke(
                                                width = l.strokeWidth * viewModel.selectionScale,
                                                cap = if (l.isHighlighter) StrokeCap.Square else StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            ),
                                            blendMode = finalBlendMode
                                        )
                                    }
                                
                                // Dashed selection bounds
                                if (minX < maxX && minY < maxY) {
                                    val sMinX = cX + (minX - cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
                                    val sMinY = cY + (minY - cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
                                    val sMaxX = cX + (maxX - cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
                                    val sMaxY = cY + (maxY - cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y

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
                         viewModel.drawingLines.forEach { line ->
                             if (line.text != null && line.points.isNotEmpty()) {
                                 val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
                                 val xPosDp = with(LocalDensity.current) { line.points.first().x.toDp() }
                                 val maxTextWidth = availableWidth - xPosDp - 4.dp
                                 val y = line.points.first().y
                                 val pageIndex = kotlin.math.floor(y / state.pageHeightPx).toInt()
                                 val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                 val contentEnd = (pageIndex + 1) * state.pageHeightPx - gapPx
                                 val maxHeightPx = kotlin.math.max(0f, contentEnd - y)
                                 val maxHeightDp = with(LocalDensity.current) { maxHeightPx.toDp() }
                                 Box(
                                     modifier = Modifier
                                         .offset {
                                             IntOffset(kotlin.math.round(line.points.first().x).toInt(), kotlin.math.round(line.points.first().y).toInt())
                                         }
                                         .widthIn(max = maxTextWidth)
                                         .heightIn(max = maxHeightDp)
                                         .clipToBounds()
                                 ) {
                                     Text(
                                         text = line.text,
                                         onTextLayout = { textLayoutResult: androidx.compose.ui.text.TextLayoutResult ->
                                             state.staticTextLayouts[line] = textLayoutResult
                                             val bottomY = line.points.first().y + textLayoutResult.size.height
                                             if (state.pageHeightPx > 0) {
                                                 val neededPages = kotlin.math.ceil((bottomY / state.pageHeightPx).toDouble()).toInt()
                                                 if (neededPages > viewModel.pageCount) {
                                                     viewModel.pageCount = neededPages
                                                 }
                                             }
                                         },
                                         modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                         style = androidx.compose.ui.text.TextStyle(
                                             color = activeLineColor,
                                             fontSize = with(LocalDensity.current) { line.strokeWidth.toSp() },
                                             lineHeight = with(LocalDensity.current) { (SNoteConfig.getRowHeight(TEXT_LARGE)).toSp() },
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

                        // Render Selected Text dynamically while dragging/scaling
                        if (viewModel.selectedLines.isNotEmpty()) {
                            var minX = Float.MAX_VALUE
                            var minY = Float.MAX_VALUE
                            var maxX = Float.MIN_VALUE
                            var maxY = Float.MIN_VALUE
                            viewModel.selectedLines.forEach { l ->
                                if (l.isEraser) return@forEach
                                if (l.text != null && l.points.isNotEmpty()) {
                                    val startPt = l.points.first()
                                    val tw = state.staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
                                    val th = state.staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
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

                            viewModel.selectedLines.forEach { line ->
                                 if (line.text != null && line.points.isNotEmpty()) {
                                     val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
                                     val pt = line.points.first()
                                     val pX = cX + (pt.x - cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
                                     val pY = cY + (pt.y - cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y

                                     val xPosDp = with(LocalDensity.current) { pX.coerceAtLeast(0f).toDp() }
                                     val maxTextWidth = (availableWidth - xPosDp - 4.dp).coerceAtLeast(10.dp)
                                     val y = pY
                                     val pageIndex = kotlin.math.floor(y / state.pageHeightPx).toInt()
                                     val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                     val contentEnd = (pageIndex + 1) * state.pageHeightPx - gapPx
                                     val maxHeightPx = kotlin.math.max(0f, contentEnd - y)
                                     val maxHeightDp = with(LocalDensity.current) { maxHeightPx.toDp() }

                                     Box(
                                         modifier = Modifier
                                             .offset { IntOffset(kotlin.math.round(pX).toInt(), kotlin.math.round(pY).toInt()) }
                                             .widthIn(max = maxTextWidth)
                                             .heightIn(max = maxHeightDp)
                                             .clipToBounds()
                                     ) {
                                         Text(
                                             text = line.text,
                                             modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                             style = androidx.compose.ui.text.TextStyle(
                                                 color = activeLineColor.copy(alpha = 0.7f),
                                                 fontSize = with(LocalDensity.current) { (line.strokeWidth * viewModel.selectionScale).coerceAtLeast(1f).toSp() },
                                                 lineHeight = with(LocalDensity.current) { (SNoteConfig.getRowHeight(TEXT_LARGE) * viewModel.selectionScale).coerceAtLeast(1f).toSp() },
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
                        }

                        // Lasso Context Menu
                        if (state.showLassoMenu && viewModel.selectedLines.isNotEmpty()) {
                            val xPosDp = with(LocalDensity.current) { state.lassoMenuPosition.x.toDp() }
                            val yPosDp = with(LocalDensity.current) { state.lassoMenuPosition.y.toDp() }
                            Box(modifier = Modifier.offset(x = xPosDp, y = yPosDp)) {
                                DropdownMenu(
                                    expanded = state.showLassoMenu,
                                    onDismissRequest = { state.showLassoMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Colour") },
                                        leadingIcon = { Icon(Icons.Default.Create, contentDescription = "Colour") },
                                        onClick = {
                                            state.showLassoMenu = false
                                            state.showLassoColorPicker = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                                        onClick = {
                                            viewModel.drawingLines.removeAll(viewModel.selectedLines)
                                            viewModel.selectedLines.clear()
                                            state.showLassoMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Lasso Color Picker
                        if (state.showLassoColorPicker && viewModel.selectedLines.isNotEmpty()) {
                            val xPosDp = with(LocalDensity.current) { state.lassoMenuPosition.x.toDp() }
                            val yPosDp = with(LocalDensity.current) { state.lassoMenuPosition.y.toDp() }
                            Box(modifier = Modifier.offset(x = xPosDp, y = yPosDp)) {
                                DropdownMenu(
                                    expanded = state.showLassoColorPicker,
                                    onDismissRequest = { state.showLassoColorPicker = false },
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
                                                viewModel.pushUndoState(viewModel.drawingLines.toList() + viewModel.selectedLines.toList())
                                                val newSelection = viewModel.selectedLines.map { l -> l.copy(color = c) }
                                                viewModel.selectedLines.clear()
                                                viewModel.selectedLines.addAll(newSelection)
                                                if (c != Color.Unspecified) {
                                                    state.updatePenColor(c.value.toLong())
                                                } else {
                                                    state.updatePenColor(Color.Unspecified.value.toLong())
                                                }
                                                state.showLassoColorPicker = false
                                                state.commitLassoSelection { commitChanges() }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Inline Text Tool Layer
                         if (viewModel.activeTextInputPosition != null) {
                             val cVal = Color(state.currentColorValue.toULong())
                             val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else strokeColor
                             val xPosDp = with(LocalDensity.current) { viewModel.activeTextInputPosition!!.x.toDp() }
                             val yPosDp = with(LocalDensity.current) { viewModel.activeTextInputPosition!!.y.toDp() }
                             val maxTextWidth = availableWidth - xPosDp - 4.dp
                             val y = viewModel.activeTextInputPosition!!.y
                             val pageIndex = kotlin.math.floor(y / state.pageHeightPx).toInt()
                             val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                             val contentEnd = (pageIndex + 1) * state.pageHeightPx - gapPx
                             val maxHeightPx = kotlin.math.max(0f, contentEnd - y)
                             val maxHeightDp = with(LocalDensity.current) { maxHeightPx.toDp() }

                             Box(
                                 modifier = Modifier
                                     .offset { IntOffset(kotlin.math.round(viewModel.activeTextInputPosition!!.x).toInt(), kotlin.math.round(viewModel.activeTextInputPosition!!.y).toInt()) }
                                     .widthIn(max = maxTextWidth)
                                     .heightIn(max = maxHeightDp)
                                     .clipToBounds()
                             ) {
                                  BasicTextField(
                                      value = viewModel.activeTextValue,
                                      onValueChange = { newValue ->
                                          // Enforce strict gap boundary - truncate text if it would overflow
                                          var finalValue = newValue

                                          if (state.pageHeightPx > 0f) {
                                              val y = viewModel.activeTextInputPosition!!.y
                                              val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                              val pageIndex = kotlin.math.floor(y / state.pageHeightPx).toInt()
                                              val contentEnd = (pageIndex + 1) * state.pageHeightPx - gapPx
                                              val maxAllowedHeightPx = kotlin.math.max(0f, contentEnd - y)

                                              // Hard limit: if text would extend beyond available height, reject addition
                                              if (newValue.text.length > viewModel.activeTextValue.text.length && state.activeTextLayoutResult != null) {
                                                  try {
                                                      val currentHeight = state.activeTextLayoutResult!!.size.height
                                                      val rowHeight = SNoteConfig.getRowHeight(state.currentTextSize)

                                                      // If current height + one more row exceeds boundary, prevent addition
                                                      if (currentHeight + rowHeight > maxAllowedHeightPx) {
                                                          return@BasicTextField
                                                      }
                                                  } catch (e: Exception) {}
                                              }
                                          }

                                          if (finalValue.text.length - viewModel.activeTextValue.text.length > 50) {
                                              state.needsAutoCommitAfterPaste = true
                                          }
                                          viewModel.activeTextValue = finalValue
                                      },
                                     onTextLayout = { textLayoutResult ->
                                          state.activeTextLayoutResult = textLayoutResult
                                          val bottomY = viewModel.activeTextInputPosition!!.y + textLayoutResult.size.height

                                          // CRITICAL: Enforce maximum height constraint to prevent text entering gap
                                          val rowHeight = SNoteConfig.getRowHeight(state.currentTextSize)
                                          val maxAllowedHeight = maxHeightPx

                                          // If text height tries to exceed limit, auto-commit immediately
                                          if (textLayoutResult.size.height > maxAllowedHeight) {
                                              if (viewModel.activeTextValue.text.isNotEmpty()) {
                                                  commitActiveText()
                                              }
                                          }

                                          // Secondary check: if text extends into next page's gap, auto-commit
                                          if (state.pageHeightPx > 0f) {
                                              val pageIdx = kotlin.math.floor(bottomY / state.pageHeightPx).toInt()
                                              val pageStart = pageIdx * state.pageHeightPx
                                              val contentEnd2 = pageStart + state.pageHeightPx - gapPx
                                              if (bottomY > contentEnd2) {
                                                  // Text extends into next page's gap - auto-commit this text
                                                  if (viewModel.activeTextValue.text.isNotEmpty()) {
                                                      commitActiveText()
                                                  }
                                              }
                                          }

                                          if (state.pageHeightPx > 0) {
                                              val neededPages = kotlin.math.ceil((bottomY / state.pageHeightPx).toDouble()).toInt()
                                              if (neededPages > viewModel.pageCount) {
                                                  viewModel.pageCount = neededPages
                                              }
                                          }
                                     },
                                      modifier = Modifier
                                          .bringIntoViewRequester(bringIntoViewRequester)
                                          .fillMaxWidth()
                                          .heightIn(max = maxHeightDp)
                                          .focusRequester(focusRequester)
                                          .background(Color.Transparent)
                                          .clipToBounds(),
                                     keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                         autoCorrectEnabled = false
                                     ),
                                     textStyle = androidx.compose.ui.text.TextStyle(
                                         color = chosenColor,
                                         fontSize = with(LocalDensity.current) { state.currentTextSize.toSp() },
                                         lineHeight = with(LocalDensity.current) { (SNoteConfig.getRowHeight(TEXT_LARGE)).toSp() },
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

                             LaunchedEffect(viewModel.activeTextInputPosition, state.activeTextLayoutResult, availableHeight) {
                                 // Trigger scroll whenever position is initially clicked and layout is ready
                                 if (state.activeTextLayoutResult != null) {
                                     kotlinx.coroutines.delay(50) // Brief delay to let IME padding settle
                                     try {
                                         val cursorOffset = viewModel.activeTextValue.selection.end.coerceIn(0, state.activeTextLayoutResult!!.layoutInput.text.text.length)
                                         val cursorRect = state.activeTextLayoutResult!!.getCursorRect(cursorOffset)
                                         val absoluteCursorTop = viewModel.activeTextInputPosition!!.y + cursorRect.top - 60f
                                         val absoluteCursorBottom = viewModel.activeTextInputPosition!!.y + cursorRect.bottom + 140f
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

                             LaunchedEffect(viewModel.activeTextValue.text, viewModel.activeTextValue.selection, availableHeight) {
                                 kotlinx.coroutines.delay(10)
                                 if (state.needsAutoCommitAfterPaste && state.activeTextLayoutResult != null) {
                                     state.needsAutoCommitAfterPaste = false
                                     commitActiveText()
                                 }
                                 state.activeTextLayoutResult?.let {
                                     try {
                                         val cursorOffset = viewModel.activeTextValue.selection.end.coerceIn(0, it.layoutInput.text.text.length)
                                         val cursorRect = it.getCursorRect(cursorOffset)
                                         val absoluteCursorTop = viewModel.activeTextInputPosition!!.y + cursorRect.top - 60f
                                         val absoluteCursorBottom = viewModel.activeTextInputPosition!!.y + cursorRect.bottom + 140f
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

                               // Aggressive line-count enforcement - prevent text from exceeding page gap boundary
                                LaunchedEffect(viewModel.activeTextValue.text, state.activeTextLayoutResult) {
                                    if (state.activeTextLayoutResult != null && viewModel.activeTextInputPosition != null && state.pageHeightPx > 0f) {
                                        try {
                                            val layout = state.activeTextLayoutResult!!
                                            val y = viewModel.activeTextInputPosition!!.y
                                            val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                            val pageIndex = kotlin.math.floor(y / state.pageHeightPx).toInt()
                                            val contentEnd = (pageIndex + 1) * state.pageHeightPx - gapPx
                                            val maxHeightPx = kotlin.math.max(0f, contentEnd - y)

                                            val textBottomY = y + layout.size.height

                                            // If text height exceeds boundary, truncate to fit
                                            if (layout.size.height > maxHeightPx && viewModel.activeTextValue.text.isNotEmpty()) {
                                                // Calculate max lines that fit
                                                val rowHeight = SNoteConfig.getRowHeight(state.currentTextSize)
                                                val maxLines = kotlin.math.max(1, (maxHeightPx / rowHeight).toInt())

                                                // If layout has more lines than can fit, truncate
                                                if (layout.lineCount > maxLines) {
                                                    val endOfLastFittingLine = layout.getLineEnd(maxLines - 1)
                                                    val truncatedText = viewModel.activeTextValue.text.substring(0, endOfLastFittingLine)
                                                    viewModel.activeTextValue = viewModel.activeTextValue.copy(text = truncatedText)
                                                    commitActiveText()
                                                }
                                            }
                                        } catch (e: Exception) {}
                                    }
                                }

                               // Aggressive real-time cursor position constraint - prevent cursor from entering gap
                               LaunchedEffect(viewModel.activeTextValue.selection, state.activeTextLayoutResult) {
                                   if (state.activeTextLayoutResult != null && viewModel.activeTextInputPosition != null && state.pageHeightPx > 0f) {
                                       try {
                                           val layout = state.activeTextLayoutResult!!
                                           val cursorOffset = viewModel.activeTextValue.selection.end.coerceIn(0, layout.layoutInput.text.text.length)
                                           val cursorRect = layout.getCursorRect(cursorOffset)
                                           val cursorBottomY = viewModel.activeTextInputPosition!!.y + cursorRect.bottom
                                           val cursorLineIndex = layout.getLineForOffset(cursorOffset)

                                           // Calculate gap boundaries
                                           val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                           val pageIndex = kotlin.math.floor(cursorBottomY / state.pageHeightPx).toInt()
                                           val pageStart = pageIndex * state.pageHeightPx
                                           val contentEnd = pageStart + state.pageHeightPx - gapPx

                                           // If cursor would be in gap, move to last valid position
                                           if (cursorBottomY > contentEnd) {
                                               // Find the line at the content boundary
                                               val maxValidY = contentEnd - viewModel.activeTextInputPosition!!.y
                                               val maxValidLine = (maxValidY / SNoteConfig.getRowHeight(state.currentTextSize)).toInt()
                                                   .coerceAtMost(layout.lineCount - 1)
                                                   .coerceAtLeast(0)

                                               val safeOffset = if (maxValidLine >= 0 && maxValidLine < layout.lineCount) {
                                                   layout.getLineEnd(maxValidLine).coerceAtMost(layout.layoutInput.text.text.length)
                                               } else {
                                                   layout.layoutInput.text.text.length
                                               }

                                               viewModel.activeTextValue = viewModel.activeTextValue.copy(
                                                   selection = androidx.compose.ui.text.TextRange(safeOffset)
                                               )
                                           }
                                       } catch (e: Exception) {
                                           // Silently ignore errors
                                       }
                                   }
                               }

                               // Monitor text height and auto-commit if approaching gap
                               LaunchedEffect(viewModel.activeTextValue.text) {
                                   if (state.activeTextLayoutResult != null && state.pageHeightPx > 0f) {
                                       kotlinx.coroutines.delay(50)
                                       try {
                                           val layout = state.activeTextLayoutResult!!
                                           val bottomY = viewModel.activeTextInputPosition!!.y + layout.size.height

                                           val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
                                           val pageIndex = kotlin.math.floor(bottomY / state.pageHeightPx).toInt()
                                           val pageStart = pageIndex * state.pageHeightPx
                                           val contentEnd = pageStart + state.pageHeightPx - gapPx
                                           val rowHeight = SNoteConfig.getRowHeight(state.currentTextSize)

                                           // If text bottom is approaching or crossing into gap, auto-commit
                                           if (bottomY > contentEnd - rowHeight * 0.1f) {
                                               if (viewModel.activeTextValue.text.isNotEmpty()) {
                                                   commitActiveText()
                                               }
                                           }
                                       } catch (e: Exception) {}
                                   }
                               }
                        }

                        // Overlay page numbers
                        for (i in 0 until viewModel.pageCount) {
                            val bottomY = state.pageHeightDp * (i + 1) - 34.dp
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
                } // End if (state.pageHeightDp > 0.dp)
                } // End Box(verticalScroll)
            }
}
