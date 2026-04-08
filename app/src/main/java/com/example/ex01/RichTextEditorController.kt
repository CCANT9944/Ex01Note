package com.example.ex01

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
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

        // Intercept single-backspace or forward-delete over an invisible formatting marker.
        // Instead of deleting the marker, just move the cursor inside the span appropriately.
        var interceptedNext = nextValue
        if (value.text.length - nextValue.text.length == 1) {
            var deletedOffset = -1
            for (i in 0 until nextValue.text.length) {
                if (value.text[i] != nextValue.text[i]) {
                    deletedOffset = i
                    break
                }
            }
            if (deletedOffset == -1) deletedOffset = nextValue.text.length

            if (deletedOffset >= 0 && deletedOffset < value.text.length) {
                val deletedChar = value.text[deletedOffset]
                if (isFormattingMarker(deletedChar)) {
                    val expectedRemaining = value.text.substring(0, deletedOffset) +
                                            value.text.substring(deletedOffset + 1)
                    if (nextValue.text == expectedRemaining) {
                        // The user deleted EXACTLY the invisible marker.
                        // We restore the marker and just move the cursor to where it would be.
                        // If it was a backspace, the cursor in nextValue is probably deletedOffset.
                        interceptedNext = TextFieldValue(
                            text = value.text,
                            selection = TextRange(deletedOffset),
                            composition = nextValue.composition
                        )
                    }
                }
            }
        }

        val sanitized = sanitizeRichTextTyping(interceptedNext)
        if (sanitized == value) return
        if (sanitized.text != value.text) {
            recordTypingUndoBoundary()
            lastTextEditAtMillis = nowMillis()
        } else {
            lastTextEditAtMillis = null
        }
        value = sanitized
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

    private fun isFormattingMarker(char: Char): Boolean {
        return char == BOLD_OPEN_MARKER || char == BOLD_CLOSE_MARKER ||
               char == ITALIC_OPEN_MARKER || char == ITALIC_CLOSE_MARKER ||
               char == UNDERLINE_OPEN_MARKER || char == UNDERLINE_CLOSE_MARKER ||
               char == STRIKETHROUGH_OPEN_MARKER || char == STRIKETHROUGH_CLOSE_MARKER ||
               char == BULLET_OPEN_MARKER || char == BULLET_CLOSE_MARKER
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
