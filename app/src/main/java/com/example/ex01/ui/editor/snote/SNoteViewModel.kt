package com.example.ex01.ui.editor.snote
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
class SNoteViewModel : ViewModel() {
    val drawingLines = mutableStateListOf<DrawingLine>()
    val undoStack = mutableStateListOf<List<DrawingLine>>()
    val redoStack = mutableStateListOf<List<DrawingLine>>()
    var preEditTextState by mutableStateOf<List<DrawingLine>?>(null)
    var preLassoState by mutableStateOf<List<DrawingLine>?>(null)
    
    fun pushUndoState(state: List<DrawingLine> = drawingLines.toList() + selectedLines.toList()) {
        undoStack.add(state)
        redoStack.clear()
    }
    
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentState = drawingLines.toList() + selectedLines.toList()
            val prevState = undoStack.removeAt(undoStack.size - 1)
            // Add current text/lasso commits if needed, but easier to just cancel them
            cancelOngoingEdits()
            redoStack.add(currentState)
            drawingLines.clear()
            drawingLines.addAll(prevState)
        }
    }
    
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentState = drawingLines.toList() + selectedLines.toList()
            val nextState = redoStack.removeAt(redoStack.size - 1)
            cancelOngoingEdits()
            undoStack.add(currentState)
            drawingLines.clear()
            drawingLines.addAll(nextState)
        }
    }
    
    private fun cancelOngoingEdits() {
        isDraggingSelection = false
        isScalingSelection = false
        showTextSizeMenu = false
        activeTextInputPosition = null
        selectedLines.clear()
        selectionDragOffset = Offset.Zero
        selectionScale = 1f
        preEditTextState = null
        preLassoState = null
        currentPath = null
        lassoPath = null
    }

    var currentPath by mutableStateOf<List<Offset>?>(null)
    var currentProperties by mutableStateOf(DrawingLine(emptyList()))
    var isEraserMode by mutableStateOf(false)
    var isHighlighterMode by mutableStateOf(false)
    var isTextMode by mutableStateOf(false)
    var isLassoMode by mutableStateOf(false)
    var lassoPath by mutableStateOf<List<Offset>?>(null)
    val selectedLines = mutableStateListOf<DrawingLine>()
    var selectionDragOffset by mutableStateOf(Offset.Zero)
    var selectionScale by mutableFloatStateOf(1f)
    var isDraggingSelection by mutableStateOf(false)
    var isScalingSelection by mutableStateOf(false)
    var showTextSizeMenu by mutableStateOf(false)
    var activeTextInputPosition by mutableStateOf<Offset?>(null)
    var activeTextValue by mutableStateOf(TextFieldValue(""))
    var showHighlighterThicknessMenu by mutableStateOf(false)
    var showThicknessMenu by mutableStateOf(false)
    var showEraserThicknessMenu by mutableStateOf(false)
    var showColorMenu by mutableStateOf(false)
    var showClearWarning by mutableStateOf(false)
    var pageCount by mutableIntStateOf(1)
    var initialLoadDone by mutableStateOf(false)
    var originalHitLine by mutableStateOf<DrawingLine?>(null)
    var originalHitIndex by mutableIntStateOf(-1)

    var triggerAddPage by mutableStateOf(false)

    suspend fun getLassoBox(lines: List<DrawingLine>): Pair<Offset, Offset>? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        if (lines.isEmpty()) return@withContext null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (l in lines) {
            for (p in l.points) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
        }
        Pair(Offset(minX, minY), Offset(maxX, maxY))
    }

    suspend fun calculateTextVisualRows(text: String, density: Float, availableWidthPx: Float, strokeWidth: Float, startX: Float): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val maxTextWidthPx = availableWidthPx - startX - (4f * density)
        // using a basic proportion approximation to keep off main thread without passing Paint
        var visualRows = 0
        val charWidthEstimate = strokeWidth * 0.6f
        for (lineStr in text.split("\n")) {
            val w = lineStr.length * charWidthEstimate
            if (maxTextWidthPx > 0f) {
                 visualRows += kotlin.math.max(1, kotlin.math.ceil((w / (maxTextWidthPx * 0.95f)).toDouble()).toInt())
            } else {
                 visualRows += 1
            }
        }
        visualRows
    }
}
