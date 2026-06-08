package com.example.ex01.ui.editor.snote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ex01.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SNoteEditor(
    serializedBody: String,
    onSerializedBodyChange: (String) -> Unit,
    viewModel: SNoteViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) = with(viewModel) {
    val context = LocalContext.current
    val state = rememberSNoteEditorState(viewModel)

    val focusRequester = state.focusRequester
    val bringIntoViewRequester = state.bringIntoViewRequester

    var pageHeightPx by state::pageHeightPx
    var pageCount by viewModel::pageCount
    var initialLoadDone by viewModel::initialLoadDone

    var showClearWarning by viewModel::showClearWarning
    val drawingLines = viewModel.drawingLines

    var currentColorValue by state::currentColorValue
    var currentThickness by state::currentThickness
    var currentEraserThickness by state::currentEraserThickness
    var currentHighlighterThickness by state::currentHighlighterThickness
    var currentTextSize by state::currentTextSize

    fun updatePenThickness(t: Float) = state.updatePenThickness(t)
    fun updateEraserThickness(t: Float) = state.updateEraserThickness(t)
    fun updateTextSize(t: Float) = state.updateTextSize(t)
    fun updateHighlighterThickness(t: Float) = state.updateHighlighterThickness(t)
    fun updatePenColor(c: Long) = state.updatePenColor(c)

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
        state.commitChanges(onSerializedBodyChange)
    }

    val currentCommitChanges by rememberUpdatedState(commitChanges)
    DisposableEffect(Unit) {
        onDispose {
            currentCommitChanges()
        }
    }

    fun commitActiveText(autoJump: Boolean = false, forcedLayout: androidx.compose.ui.text.TextLayoutResult? = null) {
        state.commitActiveText(autoJump, forcedLayout, onSerializedBodyChange)
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
            commitActiveText = { commitActiveText(false) },
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
            SNoteCanvas(
                viewModel = viewModel,
                state = state,
                focusRequester = focusRequester,
                bringIntoViewRequester = bringIntoViewRequester,
                commitChanges = commitChanges,
                commitActiveText = { autoJump -> commitActiveText(autoJump) }
            )
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
