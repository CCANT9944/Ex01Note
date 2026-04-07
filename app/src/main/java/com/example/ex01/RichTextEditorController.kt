package com.example.ex01

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

internal class RichTextEditorController(
    initialValue: TextFieldValue,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private companion object {
        const val MAX_UNDO_STEPS = 20
        const val TYPING_BURST_WINDOW_MS = 650L
    }

    var value by mutableStateOf(initialValue)
        private set

    var canUndo by mutableStateOf(false)
        private set

    private val undoStack = ArrayDeque<TextFieldValue>()
    private var lastTextEditAtMillis: Long? = null

    fun updateValue(nextValue: TextFieldValue) {
        if (nextValue == value) return
        if (nextValue.text != value.text) {
            recordTypingUndoBoundary()
            lastTextEditAtMillis = nowMillis()
        } else {
            lastTextEditAtMillis = null
        }
        value = nextValue
        canUndo = undoStack.isNotEmpty()
    }

    fun replaceValue(nextValue: TextFieldValue) {
        if (nextValue == value) return
        undoStack.clear()
        value = nextValue
        canUndo = false
        lastTextEditAtMillis = null
    }

    fun undo(): Boolean {
        val previousValue = undoStack.removeLastOrNull() ?: return false
        value = previousValue
        canUndo = undoStack.isNotEmpty()
        lastTextEditAtMillis = null
        return true
    }

    fun toggleBold() {
        applyAtomicChange(toggleBoldFormatting(value))
    }

    fun toggleItalic() {
        applyAtomicChange(toggleItalicFormatting(value))
    }

    fun toggleUnderline() {
        applyAtomicChange(toggleUnderlineFormatting(value))
    }

    fun toggleStrikethrough() {
        applyAtomicChange(toggleStrikethroughFormatting(value))
    }

    fun toggleBullet() {
        applyAtomicChange(toggleBulletFormatting(value))
    }

    fun indent() {
        applyAtomicChange(indentSelectedLines(value))
    }

    fun outdent() {
        applyAtomicChange(outdentSelectedLines(value))
    }

    private fun applyAtomicChange(nextValue: TextFieldValue) {
        if (nextValue == value) return
        pushUndoSnapshot(value)
        value = nextValue
        canUndo = undoStack.isNotEmpty()
        lastTextEditAtMillis = null
    }

    private fun recordTypingUndoBoundary() {
        val now = nowMillis()
        val withinTypingBurst = lastTextEditAtMillis?.let { now - it <= TYPING_BURST_WINDOW_MS } == true
        if (!withinTypingBurst) {
            pushUndoSnapshot(value)
        }
    }

    private fun pushUndoSnapshot(snapshot: TextFieldValue) {
        if (undoStack.isNotEmpty() && undoStack.last() == snapshot) return
        undoStack.addLast(snapshot)
        while (undoStack.size > MAX_UNDO_STEPS) {
            undoStack.removeFirst()
        }
    }
}

@Composable
internal fun rememberRichTextEditorController(initialValue: TextFieldValue): RichTextEditorController {
    val controller = remember { RichTextEditorController(initialValue) }

    LaunchedEffect(initialValue) {
        controller.replaceValue(initialValue)
    }

    return controller
}

