package com.example.ex01.ui.editor.snote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ex01.utils.*

@Composable
fun SNoteDrawingLayer(
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    availableWidthPx: Float,
    primaryColor: Color,
    strokeColor: Color,
    commitChanges: () -> Unit,
    commitActiveText: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(state.pageHeightDp * viewModel.pageCount)
            .graphicsLayer(alpha = 0.99f)
    ) {
        // 1. Static Canvas Layer (caches saved drawing strokes)
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            drawSavedLines(viewModel, state, strokeColor)
        }

        // 2. Dynamic Interaction Overlay (handles pointer input and active drawings/selections)
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(
                    state.currentColorValue, state.currentThickness, state.currentEraserThickness,
                    viewModel.isEraserMode, viewModel.isTextMode, viewModel.isLassoMode
                ) {
                    awaitPointerEventScope {
                        var textModeDownPos: Offset? = null
                        var dragStartOffset = Offset.Zero
                        var dragStartScale = 1f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue

                            if (change.isConsumed) {
                                textModeDownPos = null
                                continue
                            }

                            val isStylus = change.type == PointerType.Stylus
                            val isStylusEraser = change.type == PointerType.Eraser

                            if (!isStylus && !isStylusEraser && !viewModel.isTextMode) continue

                            if (!viewModel.isTextMode) change.consume()

                            // ── POINTER DOWN ──────────────────────────────────────────────────
                            if (change.pressed && !change.previousPressed) {
                                if (state.pageHeightPx > 0f) {
                                    // Gaps removed
                                }

                                if (viewModel.isTextMode) {
                                    textModeDownPos = change.position
                                } else if (viewModel.isLassoMode) {
                                    change.consume()
                                    val tapPos = change.position
                                    val tBounds = viewModel.selectedLines
                                        .filter { it.text != null }
                                        .associateWith { l ->
                                            val tw = state.staticTextLayouts[l]?.size?.width?.toFloat()
                                                ?: (l.strokeWidth * 0.6f * l.text!!.length.toFloat())
                                            val th = state.staticTextLayouts[l]?.size?.height?.toFloat()
                                                ?: (l.strokeWidth * 1.5f)
                                            Pair(tw, th)
                                        }
                                    val draggingHandle = viewModel.selectedLines.isNotEmpty() &&
                                        isPointInScaleHandle(tapPos, viewModel.selectedLines, viewModel.selectionDragOffset, viewModel.selectionScale, tBounds)
                                    val hittingMenuHandle = viewModel.selectedLines.isNotEmpty() &&
                                        isPointInMenuHandle(tapPos, viewModel.selectedLines, viewModel.selectionDragOffset, viewModel.selectionScale, tBounds)
                                    val dragging = !draggingHandle && !hittingMenuHandle &&
                                        viewModel.selectedLines.isNotEmpty() &&
                                        isPointInSelectionBounds(tapPos, viewModel.selectedLines, viewModel.selectionDragOffset, viewModel.selectionScale, tBounds)

                                    when {
                                        hittingMenuHandle -> {
                                            state.showLassoMenu = true
                                            state.showLassoColorPicker = false
                                            state.lassoMenuPosition = tapPos
                                        }
                                        draggingHandle -> {
                                            if (viewModel.preLassoState == null)
                                                viewModel.preLassoState = viewModel.drawingLines.toList() + viewModel.selectedLines.toList()
                                            viewModel.isScalingSelection = true
                                            state.showLassoMenu = false
                                            state.showLassoColorPicker = false
                                            dragStartScale = viewModel.selectionScale
                                            dragStartOffset = viewModel.selectionDragOffset
                                        }
                                        dragging -> {
                                            if (viewModel.preLassoState == null)
                                                viewModel.preLassoState = viewModel.drawingLines.toList() + viewModel.selectedLines.toList()
                                            viewModel.isDraggingSelection = true
                                            state.showLassoMenu = false
                                            state.showLassoColorPicker = false
                                            dragStartOffset = viewModel.selectionDragOffset
                                            dragStartScale = viewModel.selectionScale
                                        }
                                        else -> {
                                            state.showLassoMenu = false
                                            state.showLassoColorPicker = false
                                            if (viewModel.selectedLines.isNotEmpty())
                                                state.commitLassoSelection { commitChanges() }
                                            viewModel.lassoPath = listOf(tapPos)
                                        }
                                    }
                                } else if (viewModel.currentPath == null) {
                                    commitActiveText(false)
                                    viewModel.currentPath = listOf(change.position)
                                    val path = Path()
                                    path.moveTo(change.position.x, change.position.y)
                                    viewModel.activePathObject = path
                                    val actualEraserMode = isStylusEraser || viewModel.isEraserMode
                                    val cVal = Color(state.currentColorValue.toULong())
                                    val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
                                    viewModel.currentProperties = DrawingLine(
                                        points = viewModel.currentPath!!,
                                        color = if (actualEraserMode) Color.Unspecified else chosenColor,
                                        strokeWidth = when {
                                            actualEraserMode -> state.currentEraserThickness
                                            viewModel.isHighlighterMode -> state.currentHighlighterThickness
                                            else -> state.currentThickness
                                        },
                                        isEraser = actualEraserMode,
                                        isHighlighter = viewModel.isHighlighterMode
                                    )
                                }

                            // ── POINTER MOVE ──────────────────────────────────────────────────
                            } else if (change.pressed && change.previousPressed) {
                                if (viewModel.isLassoMode) {
                                    change.consume()
                                    when {
                                        viewModel.isScalingSelection -> {
                                            val dx = change.position.x - change.previousPosition.x
                                            val dy = change.position.y - change.previousPosition.y
                                            viewModel.selectionScale = kotlin.math.max(0.1f, viewModel.selectionScale + (dx + dy) / 400f)
                                        }
                                        viewModel.isDraggingSelection -> {
                                            viewModel.selectionDragOffset += change.position - change.previousPosition
                                        }
                                        viewModel.lassoPath != null -> {
                                            viewModel.lassoPath = viewModel.lassoPath!! + change.position
                                        }
                                    }
                                } else if (!viewModel.isTextMode && viewModel.currentPath != null) {
                                    val relY = change.position.y % state.pageHeightPx
                                    viewModel.currentPath = viewModel.currentPath!! + change.position
                                    viewModel.activePathObject?.lineTo(change.position.x, change.position.y)
                                }

                            // ── POINTER UP ────────────────────────────────────────────────────
                            } else if (!change.pressed && change.previousPressed) {
                                if (viewModel.isTextMode) {
                                    val downPos = textModeDownPos
                                    textModeDownPos = null
                                    if (downPos == null) continue
                                    val dx = change.position.x - downPos.x
                                    val dy = change.position.y - downPos.y
                                    if (kotlin.math.sqrt(dx * dx + dy * dy) > 10.dp.toPx()) continue
                                    change.consume()
                                    handleTextTap(
                                        tapPos = change.position,
                                        viewModel = viewModel,
                                        state = state,
                                        availableWidthPx = availableWidthPx,
                                        commitChanges = commitChanges
                                    )

                                } else if (viewModel.isLassoMode) {
                                    if (viewModel.isScalingSelection || viewModel.isDraggingSelection) {
                                        viewModel.isScalingSelection = false
                                        viewModel.isDraggingSelection = false
                                        finalizeLassoDrag(
                                            viewModel = viewModel,
                                            state = state,
                                            dragStartOffset = dragStartOffset,
                                            dragStartScale = dragStartScale,
                                            commitChanges = commitChanges
                                        )
                                    } else if (viewModel.lassoPath != null) {
                                        finalizeLassoSelection(viewModel = viewModel, state = state)
                                    }

                                } else if (!viewModel.isTextMode && viewModel.currentPath != null) {
                                    viewModel.pushUndoState()
                                    val completedLine = viewModel.currentProperties.copy(points = viewModel.currentPath!!)
                                    completedLine._cachedPath = viewModel.activePathObject
                                    viewModel.drawingLines.add(completedLine)
                                    viewModel.currentPath = null
                                    viewModel.activePathObject = null
                                    commitChanges()
                                }
                            }
                        }
                    }
                }
        ) {
            drawActiveLine(viewModel, strokeColor)
            drawLassoPath(viewModel.lassoPath, primaryColor)
            drawSelection(viewModel, state, strokeColor, primaryColor)
        }
    }
}

// ── Canvas draw helpers ───────────────────────────────────────────────────────

private fun DrawScope.drawSavedLines(viewModel: SNoteViewModel, state: SNoteEditorState, strokeColor: Color) {
    val activePos = viewModel.activeTextInputPosition
    val layRes = state.activeTextLayoutResult
    val liveDeltaY = if (activePos != null && layRes != null) {
        val sessionTopY = activePos.y
        val originalBottomY = sessionTopY + state.activeTextOriginalHeight
        val newBottomY = sessionTopY + layRes.size.height.toFloat()
        newBottomY - originalBottomY
    } else 0f
    val originalBottomY = if (activePos != null) activePos.y + state.activeTextOriginalHeight else 0f
    val pushThreshold = originalBottomY - 5f

    viewModel.drawingLines.forEach { line ->
        if (line.text != null) return@forEach
        
        var shiftY = 0f
        if (liveDeltaY > 1f) {
            val minY = line.points.minOf { it.y }
            val maxY = line.points.maxOf { it.y }
            val centerY = (minY + maxY) / 2f
            if (centerY >= pushThreshold || minY >= pushThreshold) {
                shiftY = liveDeltaY
            }
        }

        if (shiftY > 0f) {
            translate(top = shiftY) {
                drawSNotePath(line.toPath(), line, line.strokeWidth, strokeColor)
            }
        } else {
            drawSNotePath(line.toPath(), line, line.strokeWidth, strokeColor)
        }
    }
}

private fun DrawScope.drawActiveLine(viewModel: SNoteViewModel, strokeColor: Color) {
    val activePath = viewModel.activePathObject
    val currentPath = viewModel.currentPath // Read state to register invalidation dependency on gesture updates
    if (activePath != null && currentPath != null) {
        val active = viewModel.currentProperties
        drawSNotePath(activePath, active, active.strokeWidth, strokeColor)
    }
}

private fun DrawScope.drawLassoPath(lassoPath: List<Offset>?, primaryColor: Color) {
    lassoPath ?: return
    val p = Path()
    lassoPath.forEachIndexed { i, pt ->
        if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
    }
    drawPath(
        path = p,
        color = primaryColor,
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    )
}

private fun DrawScope.drawSelection(
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    strokeColor: Color,
    primaryColor: Color
) {
    if (viewModel.selectedLines.isEmpty()) return

    val tBounds = viewModel.selectedLines
        .filter { it.text != null && it.points.isNotEmpty() }
        .associateWith { l ->
            Pair(
                state.staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text!!.length.toFloat()),
                state.staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
            )
        }
    val bounds = computeSelectionBounds(viewModel.selectedLines, tBounds) ?: return

    // Draw stroke lines with transform applied
    viewModel.selectedLines.forEach { l ->
        if (l.text != null) return@forEach
        val offsetPath = Path()
        l.points.forEachIndexed { idx, pt ->
            val pX = bounds.cX + (pt.x - bounds.cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
            val pY = bounds.cY + (pt.y - bounds.cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
            if (idx == 0) offsetPath.moveTo(pX, pY) else offsetPath.lineTo(pX, pY)
        }
        drawSNotePath(offsetPath, l, l.strokeWidth * viewModel.selectionScale, strokeColor, alpha = 0.7f)
    }

    // Dashed bounding box + handles
    if (bounds.minX < bounds.maxX && bounds.minY < bounds.maxY) {
        val pad = 16f
        val sMinX = bounds.cX + (bounds.minX - bounds.cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
        val sMinY = bounds.cY + (bounds.minY - bounds.cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
        val sMaxX = bounds.cX + (bounds.maxX - bounds.cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
        val sMaxY = bounds.cY + (bounds.maxY - bounds.cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y

        drawRect(
            color = primaryColor,
            topLeft = Offset(sMinX - pad, sMinY - pad),
            size = Size(sMaxX - sMinX + pad * 2, sMaxY - sMinY + pad * 2),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )

        // Scale handle (bottom-right)
        drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(sMaxX + pad, sMaxY + pad))
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(sMaxX + pad, sMaxY + pad))

        // Menu handle (top-right) — three dots
        drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(sMaxX + pad, sMinY - pad))
        val hr = 1.dp.toPx()
        val mc = Offset(sMaxX + pad, sMinY - pad)
        drawCircle(color = Color.White, radius = hr, center = mc.copy(y = mc.y - 3.dp.toPx()))
        drawCircle(color = Color.White, radius = hr, center = mc)
        drawCircle(color = Color.White, radius = hr, center = mc.copy(y = mc.y + 3.dp.toPx()))
    }
}

private fun DrawScope.drawSNotePath(
    path: Path,
    line: DrawingLine,
    strokeWidth: Float,
    strokeColor: Color,
    alpha: Float = 1f
) {
    val activeColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
    val finalColor = when {
        line.isEraser -> Color.Black
        line.isHighlighter -> activeColor.copy(alpha = 0.4f)
        else -> activeColor.copy(alpha = alpha)
    }
    val blendMode = when {
        line.isEraser -> BlendMode.Clear
        line.isHighlighter -> BlendMode.Multiply
        else -> BlendMode.SrcOver
    }
    drawPath(
        path = path,
        color = finalColor,
        style = Stroke(
            width = strokeWidth,
            cap = if (line.isHighlighter) StrokeCap.Square else StrokeCap.Round,
            join = StrokeJoin.Round
        ),
        blendMode = blendMode
    )
}

// ── Gesture logic helpers ─────────────────────────────────────────────────────

private fun handleTextTap(
    tapPos: Offset,
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    availableWidthPx: Float,
    commitChanges: () -> Unit
) {
    commitActiveText(false, viewModel, state, commitChanges)

    val rowHeight = SNoteConfig.getRowHeight(TEXT_LARGE)

    // Reject tap in page gap removed

    val targetY = SNoteConfig.snapYToRow(tapPos.y, state.pageHeightPx, rowHeight, state.currentDensity)
    val clickedRowIndex = (tapPos.y / rowHeight).toInt()

    var hitIndex = -1
    for (i in viewModel.drawingLines.indices.reversed()) {
        val l = viewModel.drawingLines[i]
        if (l.text != null && l.points.isNotEmpty()) {
            val layRes = state.staticTextLayouts[l]
            val py = l.points.first().y
            if (layRes != null) {
                if (tapPos.y >= py && tapPos.y <= py + layRes.size.height) { hitIndex = i; break }
            } else {
                val startRow = kotlin.math.round(py / rowHeight).toInt()
                val maxTextWidthPx = availableWidthPx - l.points.first().x - 4f * state.currentDensity
                val pEst = android.graphics.Paint().apply { textSize = l.strokeWidth }
                var visualRows = 0
                for (lineStr in l.text.split("\n")) {
                    val w = pEst.measureText(lineStr)
                    visualRows += if (maxTextWidthPx > 0f)
                        kotlin.math.max(1, kotlin.math.ceil((w / (maxTextWidthPx * 0.95f)).toDouble()).toInt())
                    else 1
                }
                val endRow = startRow + kotlin.math.max(0, visualRows - 1)
                if (clickedRowIndex in startRow..endRow) { hitIndex = i; break }
            }
        }
    }

    if (hitIndex != -1) {
        viewModel.preEditTextState = viewModel.drawingLines.toList()
        val hitLine = viewModel.drawingLines.removeAt(hitIndex)
        viewModel.layoutBaselineState = viewModel.drawingLines.toList()
        viewModel.originalHitLine = hitLine
        viewModel.originalHitIndex = hitIndex
        state.updatePenColor(hitLine.color.value.toLong())
        viewModel.activeTextInputPosition = hitLine.points.first()
        state.activeTextLayoutResult = null
        val safeText = hitLine.text!!
        val layRes = state.staticTextLayouts[hitLine]
        state.activeTextOriginalHeight = layRes?.size?.height?.toFloat() ?: (hitLine.strokeWidth * 1.5f)
        val finalCharIdx = if (layRes != null) {
            layRes.getOffsetForPosition(tapPos - hitLine.points.first())
        } else safeText.length
        viewModel.activeTextValue = androidx.compose.ui.text.input.TextFieldValue(
            safeText,
            selection = androidx.compose.ui.text.TextRange(finalCharIdx)
        )
        state.currentTextSize = hitLine.strokeWidth
        commitChanges()
    } else {
        val defaultIndent = 16f * state.currentDensity
        
        var extrapolatedY = targetY
        var minDistance = Float.MAX_VALUE

        for (l in viewModel.drawingLines) {
            if (l.text != null && l.points.isNotEmpty()) {
                val layRes = state.staticTextLayouts[l]
                if (layRes != null) {
                    val blockTop = l.points.first().y
                    val blockBottom = blockTop + layRes.size.height
                    if (tapPos.y >= blockBottom) {
                        val dist = tapPos.y - blockBottom
                        if (dist < minDistance && dist < rowHeight * 3f) {
                            minDistance = dist
                            val lineCount = l.text.split("\n").size.coerceAtLeast(1)
                            val actualRowHeight = layRes.size.height.toFloat() / lineCount
                            val rowsBelow = kotlin.math.round(dist / actualRowHeight)
                            extrapolatedY = blockBottom + rowsBelow * actualRowHeight
                        }
                    }
                }
            }
        }
        
        val finalY = if (minDistance < Float.MAX_VALUE) extrapolatedY else targetY
        
        viewModel.activeTextInputPosition = Offset(defaultIndent, finalY)
        viewModel.activeTextValue = androidx.compose.ui.text.input.TextFieldValue("")
        state.activeTextLayoutResult = null
        state.activeTextOriginalHeight = 0f
        commitChanges()
    }
}

private fun commitActiveText(autoJump: Boolean, viewModel: SNoteViewModel, state: SNoteEditorState, commitChanges: () -> Unit) {
    // Thin shim — real logic lives in SNoteEditorState; this avoids capturing the full lambda in the pointer handler
    if (viewModel.activeTextInputPosition != null) {
        state.commitActiveText(autoJump, onSerializedBodyChange = { commitChanges() })
    }
}

private fun finalizeLassoDrag(
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    dragStartOffset: Offset,
    dragStartScale: Float,
    commitChanges: () -> Unit
) {
    if (viewModel.selectedLines.isEmpty() || state.pageHeightPx <= 0f) return

    val tBounds = viewModel.selectedLines
        .filter { it.text != null && it.points.isNotEmpty() }
        .associateWith { l ->
            Pair(
                state.staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text!!.length.toFloat()),
                state.staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
            )
        }
    val b = computeSelectionBounds(viewModel.selectedLines, tBounds) ?: return
    if (b.minY > b.maxY) return

    val gapPx = SNoteConfig.PAGE_GAP_DP * state.currentDensity
    val topY = b.cY + (b.minY - b.cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
    val bottomY = b.cY + (b.maxY - b.cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
    val leftX = b.cX + (b.minX - b.cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
    val rightX = b.cX + (b.maxX - b.cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x

    val overlapsGap = false

    if (topY < 0f || leftX < 0f || rightX > state.currentCanvasWidthPx) {
        viewModel.selectionDragOffset = dragStartOffset
        viewModel.selectionScale = dragStartScale
    } else if (viewModel.selectionDragOffset != Offset.Zero || viewModel.selectionScale != 1f) {
        if (viewModel.preLassoState != null) viewModel.pushUndoState(viewModel.preLassoState!!)
        val finalizedLines = viewModel.selectedLines.map { l ->
            l.copy(
                points = l.points.map { p ->
                    Offset(
                        b.cX + (p.x - b.cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x,
                        b.cY + (p.y - b.cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
                    )
                },
                strokeWidth = l.strokeWidth * viewModel.selectionScale
            )
        }
        viewModel.selectedLines.clear()
        viewModel.selectedLines.addAll(finalizedLines)
        viewModel.selectionDragOffset = Offset.Zero
        viewModel.selectionScale = 1f
    }
    viewModel.preLassoState = null
    commitChanges()
}

private fun finalizeLassoSelection(viewModel: SNoteViewModel, state: SNoteEditorState) {
    val capturedPath = viewModel.lassoPath ?: return
    viewModel.lassoPath = null

    val minX = capturedPath.minOfOrNull { it.x } ?: 0f
    val maxX = capturedPath.maxOfOrNull { it.x } ?: 0f
    val minY = capturedPath.minOfOrNull { it.y } ?: 0f
    val maxY = capturedPath.maxOfOrNull { it.y } ?: 0f
    val lassoRect = Rect(minX, minY, maxX, maxY)

    val newSelection = mutableListOf<DrawingLine>()
    val remaining = mutableListOf<DrawingLine>()

    for ((index, l) in viewModel.drawingLines.withIndex()) {
        if (l.points.isEmpty() || l.isEraser || l.text != null) {
            remaining.add(l)
            continue
        }
        val lMinX = l.points.minOf { it.x }
        val lMaxX = l.points.maxOf { it.x }
        val lMinY = l.points.minOf { it.y }
        val lMaxY = l.points.maxOf { it.y }
        if (lMinX > maxX || lMaxX < minX || lMinY > maxY || lMaxY < minY) {
            remaining.add(l)
            continue
        }
        val isSelected = l.points.any { pt ->
            if (!lassoRect.contains(pt)) return@any false
            if (!isPointInPolygon(pt, capturedPath)) return@any false
            var pointErased = false
            for (j in index + 1 until viewModel.drawingLines.size) {
                val e = viewModel.drawingLines[j]
                if (e.isEraser) {
                    val rSq = e.strokeWidth * e.strokeWidth
                    for (ept in e.points) {
                        val dx = pt.x - ept.x
                        val dy = pt.y - ept.y
                        if (dx * dx + dy * dy <= rSq) { pointErased = true; break }
                    }
                    if (pointErased) break
                }
            }
            !pointErased
        }
        if (isSelected) newSelection.add(l) else remaining.add(l)
    }

    if (newSelection.isNotEmpty()) {
        viewModel.drawingLines.clear()
        viewModel.drawingLines.addAll(remaining)
        viewModel.selectedLines.addAll(newSelection)
        viewModel.selectionDragOffset = Offset.Zero
        viewModel.selectionScale = 1f
    }
}
