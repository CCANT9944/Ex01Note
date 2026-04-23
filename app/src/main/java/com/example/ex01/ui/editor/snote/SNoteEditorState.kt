package com.example.ex01.ui.editor.snote

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

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

    fun commitChanges(onSerializedBodyChange: (String) -> Unit) {
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

    fun commitLassoSelection(onSerializedBodyChange: (String) -> Unit) {
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
            if (viewModel.selectionDragOffset != Offset.Zero || viewModel.selectionScale != 1f) {
                val preLState = viewModel.preLassoState
                if (preLState != null) {
                    viewModel.pushUndoState(preLState)
                }
            }
            viewModel.preLassoState = null

            viewModel.drawingLines.addAll(finalizedLines)
            viewModel.selectedLines.clear()
            viewModel.selectionDragOffset = Offset.Zero
            viewModel.selectionScale = 1f
            commitChanges(onSerializedBodyChange)
        }
    }

    fun commitActiveText(onSerializedBodyChange: (String) -> Unit) {
        commitLassoSelection(onSerializedBodyChange)
        if (viewModel.activeTextInputPosition != null) {
            val savedState = viewModel.preEditTextState ?: viewModel.drawingLines.toList()
            val textChanged = (viewModel.originalHitLine == null && viewModel.activeTextValue.text.isNotBlank()) ||
                              (viewModel.originalHitLine != null && viewModel.activeTextValue.text != viewModel.originalHitLine!!.text)

            if (textChanged) {
                viewModel.pushUndoState(savedState)
            }

            if (!textChanged && viewModel.originalHitLine != null) {
                val index = if (viewModel.originalHitIndex in 0..viewModel.drawingLines.size) viewModel.originalHitIndex else viewModel.drawingLines.size
                viewModel.drawingLines.add(index, viewModel.originalHitLine!!)
            } else if (viewModel.activeTextValue.text.isNotBlank()) {
                val cVal = Color(currentColorValue.toULong())
                val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
                // --- TEXT PAGINATION ALGORITHM ---
                val startX = viewModel.activeTextInputPosition!!.x
                val startY = viewModel.activeTextInputPosition!!.y
                val fullText = viewModel.activeTextValue.text

                if (activeTextLayoutResult != null && pageHeightPx > 0f) {
                    val layRes = activeTextLayoutResult!!
                    var currentBlockStartIdx = 0
                    var currentBlockY = startY
                    var cumulativeGapOffset = 0f

                    for (i in 0 until layRes.lineCount) {
                        val virtualTop = startY + layRes.getLineTop(i) + cumulativeGapOffset
                        val virtualBottom = startY + layRes.getLineBottom(i) + cumulativeGapOffset
                        val targetPage = kotlin.math.floor((virtualTop / pageHeightPx).toDouble()).toInt()
                        val pageBottom = (targetPage + 1) * pageHeightPx - (SNoteConfig.PAGE_GAP_DP * currentDensity)

                        if (virtualBottom > pageBottom) {
                            if (i > 0) {
                                val endIdx = layRes.getLineEnd(i - 1)
                                if (endIdx > currentBlockStartIdx) {
                                    viewModel.drawingLines.add(
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
                            val rowHeight = SNoteConfig.getRowHeight(TEXT_LARGE)
                            newBlockY = kotlin.math.ceil((newBlockY / rowHeight).toDouble()).toFloat() * rowHeight

                            cumulativeGapOffset += (newBlockY - virtualTop)
                            currentBlockY = newBlockY
                        }
                    }
                    if (currentBlockStartIdx < fullText.length) {
                        viewModel.drawingLines.add(
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
                    viewModel.drawingLines.add(
                        DrawingLine(
                            points = listOf(Offset(startX, startY)),
                            color = chosenColor,
                            strokeWidth = currentTextSize,
                            text = fullText
                        )
                    )
                }
                // ------------------------------------
            }

            viewModel.preEditTextState = null
            viewModel.originalHitLine = null
            viewModel.originalHitIndex = -1
            viewModel.activeTextInputPosition = null
            viewModel.activeTextValue = TextFieldValue("")
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

