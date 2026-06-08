package com.example.ex01.ui.editor.snote

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester

class SNoteEditorState(
    val viewModel: SNoteViewModel,
    val context: Context
) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var currentTextSize by mutableFloatStateOf(prefs.getFloat("text_size", TEXT_LARGE))
    var currentHighlighterThickness by mutableFloatStateOf(prefs.getFloat("highlighter_thickness", HIGHLIGHTER_MEDIUM))
    var currentThickness by mutableFloatStateOf(prefs.getFloat("pen_thickness", PEN_MEDIUM))
    var currentEraserThickness by mutableFloatStateOf(prefs.getFloat("eraser_thickness", ERASER_MEDIUM))
    var currentColorValue by mutableLongStateOf(prefs.getLong("pen_color", Color.Unspecified.value.toLong()))

    var pageHeightPx by mutableFloatStateOf(0f)
    var pageHeightDp by mutableStateOf(0.dp)
    var currentCanvasWidthPx by mutableFloatStateOf(0f)
    var currentDensity by mutableFloatStateOf(1f)
    var activeTextLayoutResult by mutableStateOf<TextLayoutResult?>(null)
    val staticTextLayouts = mutableMapOf<DrawingLine, TextLayoutResult>()
    var needsAutoCommitAfterPaste by mutableStateOf(false)
    var showLassoMenu by mutableStateOf(false)
    var showLassoColorPicker by mutableStateOf(false)
    var lassoMenuPosition by mutableStateOf(Offset.Zero)

    val focusRequester = FocusRequester()
    val bringIntoViewRequester = BringIntoViewRequester()
    var activeTextOriginalHeight by mutableFloatStateOf(0f)


    fun updatePenThickness(t: Float) {
        currentThickness = t
        prefs.edit().putFloat("pen_thickness", t).apply()
    }

    fun updateEraserThickness(t: Float) {
        currentEraserThickness = t
        prefs.edit().putFloat("eraser_thickness", t).apply()
    }

    fun updateTextSize(t: Float) {
        currentTextSize = t
        prefs.edit().putFloat("text_size", t).apply()
    }

    fun updateHighlighterThickness(t: Float) {
        currentHighlighterThickness = t
        prefs.edit().putFloat("highlighter_thickness", t).apply()
    }

    fun updatePenColor(c: Long) {
        currentColorValue = c
        prefs.edit().putLong("pen_color", c).apply()
    }

    fun mergeAdjacentTextBlocks() {
        if (viewModel.drawingLines.isEmpty() || pageHeightPx <= 0f) return
        val textLines = viewModel.drawingLines.filter { it.text != null && it.points.isNotEmpty() }
        if (textLines.size < 2) return
        
        val sorted = textLines.sortedBy { it.points.first().y }
        val toRemove = mutableSetOf<DrawingLine>()
        val replacements = mutableMapOf<DrawingLine, DrawingLine>()
        
        var i = 0
        while (i < sorted.size) {
            var current = sorted[i]
            var j = i + 1
            while (j < sorted.size) {
                val next = sorted[j]
                
                val currentBottom = current.points.first().y + (staticTextLayouts[current]?.size?.height?.toFloat() ?: SNoteConfig.getRowHeight(current.strokeWidth))
                val nextTop = next.points.first().y
                
                val sameX = kotlin.math.abs(current.points.first().x - next.points.first().x) < 2f
                val sameSize = kotlin.math.abs(current.strokeWidth - next.strokeWidth) < 0.1f
                val sameColor = current.color == next.color
                val samePage = kotlin.math.floor(currentBottom / pageHeightPx) == kotlin.math.floor(nextTop / pageHeightPx)
                val adjacent = kotlin.math.abs(nextTop - currentBottom) < 5f
                
                if (sameX && sameSize && sameColor && samePage && adjacent) {
                    current = current.copy(text = current.text + "\n" + next.text)
                    toRemove.add(next)
                    j++
                } else {
                    break
                }
            }
            if (j > i + 1) {
                replacements[sorted[i]] = current
            }
            i = j
        }
        
        if (toRemove.isNotEmpty() || replacements.isNotEmpty()) {
            val newList = viewModel.drawingLines.mapNotNull { line ->
                if (toRemove.contains(line)) null
                else replacements[line] ?: line
            }
            viewModel.drawingLines.clear()
            viewModel.drawingLines.addAll(newList)
        }
    }

    fun pushContentBelow(startY: Float, deltaY: Float, excludeLines: Set<DrawingLine> = emptySet(), baseline: List<DrawingLine>? = null): List<DrawingLine> {
        val source = baseline ?: viewModel.drawingLines.toList()
        if (deltaY < 1f) return source
        
        val updatedLines = source.map { line ->
            if (excludeLines.contains(line)) return@map line
            val firstPt = line.points.firstOrNull() ?: return@map line
            
            val yThreshold = startY - 5f
            val shouldPush = if (line.text != null) {
                firstPt.y >= yThreshold
            } else {
                val minY = line.points.minOf { it.y }
                val maxY = line.points.maxOf { it.y }
                val centerY = (minY + maxY) / 2f
                centerY >= yThreshold || minY >= yThreshold
            }
            
            if (shouldPush) {
                val shiftedPoints = line.points.map { pt -> pt.copy(y = pt.y + deltaY) }
                line.copy(points = shiftedPoints)
            } else {
                line
            }
        }
        
        if (baseline == null) {
            viewModel.drawingLines.clear()
            viewModel.drawingLines.addAll(updatedLines)
        }
        return updatedLines
    }

    fun commitChanges(onSerializedBodyChange: (String) -> Unit = {}) {
        val linesToSave = viewModel.drawingLines.toList().toMutableList()
        if (viewModel.selectedLines.isNotEmpty()) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            viewModel.selectedLines.forEach { l ->
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

            val finalizedLines = viewModel.selectedLines.map { l ->
                l.copy(
                    points = l.points.map { p ->
                        Offset(
                            cX + (p.x - cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x,
                            cY + (p.y - cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
                        )
                    },
                    strokeWidth = l.strokeWidth * viewModel.selectionScale
                )
            }
            linesToSave.addAll(finalizedLines)
        }
        if (viewModel.activeTextInputPosition != null && viewModel.activeTextValue.text.isNotEmpty()) {
            val cVal = Color(currentColorValue.toULong())
            val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
            linesToSave.add(
                DrawingLine(
                    points = listOf(viewModel.activeTextInputPosition!!),
                    color = chosenColor,
                    strokeWidth = currentTextSize,
                    text = viewModel.activeTextValue.text
                )
            )
        }
        onSerializedBodyChange(serializeDrawing(linesToSave))
    }

    fun commitLassoSelection(onSerializedBodyChange: (String) -> Unit = {}) {
        if (viewModel.selectedLines.isNotEmpty()) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            viewModel.selectedLines.forEach { l ->
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

            val finalizedLines = viewModel.selectedLines.map { l ->
                l.copy(
                    points = l.points.map { p ->
                        Offset(
                            cX + (p.x - cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x,
                            cY + (p.y - cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y
                        )
                    },
                    strokeWidth = l.strokeWidth * viewModel.selectionScale
                )
            }


            viewModel.drawingLines.addAll(finalizedLines)
            viewModel.selectedLines.clear()
            viewModel.selectionDragOffset = Offset.Zero
            viewModel.selectionScale = 1f
            commitChanges(onSerializedBodyChange)
        }
    }


    fun commitActiveText(autoJump: Boolean = false, forcedLayout: androidx.compose.ui.text.TextLayoutResult? = null, onSerializedBodyChange: (String) -> Unit = {}) {
        commitLassoSelection(onSerializedBodyChange)
        if (viewModel.activeTextInputPosition != null) {
            var currentBlockStartIdx = 0
            
            // 1. CAPTURE STABLE BASELINE ONCE PER SESSION
            if (viewModel.preEditTextState == null) {
                viewModel.preEditTextState = viewModel.drawingLines.toList()
                viewModel.layoutBaselineState = viewModel.drawingLines.toList()
            }
            val undoBaseline = viewModel.preEditTextState!!
            val layoutBaseline = viewModel.layoutBaselineState!!
            
            val textChanged = (viewModel.originalHitLine == null && viewModel.activeTextValue.text.isNotBlank()) ||
                              (viewModel.originalHitLine != null && viewModel.activeTextValue.text != viewModel.originalHitLine!!.text)

            if (textChanged) {
                // Use the CLEAN baseline for undo
                viewModel.pushUndoState(undoBaseline)
            }

            var lastBlockY = viewModel.activeTextInputPosition!!.y
            var lastBlockText = ""

            if (!textChanged && viewModel.originalHitLine != null) {
                viewModel.drawingLines.clear()
                viewModel.drawingLines.addAll(undoBaseline)
            } else if (viewModel.activeTextValue.text.isNotBlank()) {
                val cVal = Color(currentColorValue.toULong())
                val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
                // --- TEXT PROCESSING ALGORITHM ---
                val startX = viewModel.activeTextInputPosition!!.x
                val startY = viewModel.activeTextInputPosition!!.y
                val fullText = viewModel.activeTextValue.text

                val layRes = forcedLayout ?: activeTextLayoutResult
                if (layRes != null) {
                    val finalBlockHeight = layRes.size.height.toFloat()
                    val newBottomY = startY + finalBlockHeight
                    
                    val sessionTopY = viewModel.activeTextInputPosition!!.y
                    val originalBottomY = sessionTopY + activeTextOriginalHeight
                    
                    val deltaY = newBottomY - originalBottomY
                    
                    val pushedBaseline = if (deltaY > 1f) {
                        pushContentBelow(originalBottomY, deltaY, baseline = layoutBaseline)
                    } else {
                        layoutBaseline
                    }
                    
                    viewModel.drawingLines.clear()
                    viewModel.drawingLines.addAll(pushedBaseline)
                    
                    val cleanText = fullText.replace("\r", "").trimEnd('\n')
                    viewModel.drawingLines.add(
                        DrawingLine(listOf(Offset(startX, startY)), chosenColor, currentTextSize, text = cleanText)
                    )
                } else {
                    val cleanText = fullText.replace("\r", "")
                    viewModel.drawingLines.add(
                        DrawingLine(
                            points = listOf(Offset(startX, startY)),
                            color = chosenColor,
                            strokeWidth = currentTextSize,
                            text = cleanText
                        )
                    )
                }
                // ------------------------------------
            }


            if (!autoJump) {
                viewModel.preEditTextState = null
                viewModel.layoutBaselineState = null
                viewModel.originalHitLine = null
                viewModel.originalHitIndex = -1
                viewModel.activeTextInputPosition = null
                viewModel.activeTextValue = TextFieldValue("")
                activeTextLayoutResult = null
                activeTextOriginalHeight = 0f
            } else if ((forcedLayout ?: activeTextLayoutResult) == null) {
                // MID-EDIT Fallback
                viewModel.activeTextInputPosition = Offset(viewModel.activeTextInputPosition!!.x, lastBlockY)
                val originalSelection = viewModel.activeTextValue.selection.end
                val relSelection = (originalSelection - currentBlockStartIdx).coerceIn(0, lastBlockText.length)
                viewModel.activeTextValue = TextFieldValue(lastBlockText, selection = androidx.compose.ui.text.TextRange(relSelection))
            }
            mergeAdjacentTextBlocks()
            commitChanges(onSerializedBodyChange)
        }
    }




}

@Composable
fun rememberSNoteEditorState(
    viewModel: SNoteViewModel,
    context: Context = LocalContext.current
): SNoteEditorState {
    return remember(viewModel, context) {
        SNoteEditorState(viewModel, context)
    }
}

