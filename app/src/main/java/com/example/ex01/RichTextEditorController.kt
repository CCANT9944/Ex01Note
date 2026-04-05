package com.example.ex01

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

internal class RichTextEditorController(initialValue: TextFieldValue) {
    var value by mutableStateOf(initialValue)
        private set

    fun updateValue(nextValue: TextFieldValue) {
        value = nextValue
    }

    fun toggleBold() {
        value = toggleBoldFormatting(value)
    }

    fun toggleItalic() {
        value = toggleItalicFormatting(value)
    }

    fun toggleUnderline() {
        value = toggleUnderlineFormatting(value)
    }

    fun toggleStrikethrough() {
        value = toggleStrikethroughFormatting(value)
    }

    fun toggleBullet() {
        value = toggleBulletFormatting(value)
    }

    fun indent() {
        value = indentSelectedLines(value)
    }

    fun outdent() {
        value = outdentSelectedLines(value)
    }
}

@Composable
internal fun rememberRichTextEditorController(initialValue: TextFieldValue): RichTextEditorController {
    val controller = remember { RichTextEditorController(initialValue) }

    LaunchedEffect(initialValue) {
        controller.updateValue(initialValue)
    }

    return controller
}

