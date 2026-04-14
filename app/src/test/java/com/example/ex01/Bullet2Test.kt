package com.example.ex01

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Test
class Bullet2Test {
    private fun TextFieldValue.pp() = text.replace("\uE000", "[B]").replace("\uE001", "[/B]").replace("\n", "[LF]").replace("\uE008", "[BUL]").replace("\uE009", "[/BUL]")
    @Test
    fun testBulletEnter() {
        val controller = RichTextEditorController(TextFieldValue("Item 1", selection = TextRange(6)))
        controller.toggleBullet()
        var s = controller.value.selection.start
        var t = controller.value.text
        controller.updateValue(TextFieldValue(t.substring(0, s) + "\n" + t.substring(s), selection = TextRange(s + 1)))
        println("1. After enter: ${controller.value.pp()} - isBullet: ${richTextFormattingState(controller.value).bulletActive}")
        controller.toggleBullet()
        println("2. After toggle off: ${controller.value.pp()} - isBullet: ${richTextFormattingState(controller.value).bulletActive}")
        controller.updateValue(TextFieldValue(controller.value.text.substring(0, controller.value.selection.start - 1) + controller.value.text.substring(controller.value.selection.start), selection = TextRange(controller.value.selection.start - 1)))
        println("3. After backspace: ${controller.value.pp()}")
        // Emulate empty row backspace
        val emptyLine = RichTextEditorController(TextFieldValue("\uE008\uE009\n\uE008\uE009", selection = TextRange(5)))
        emptyLine.updateValue(TextFieldValue("\uE008\uE009\uE008\uE009", selection = TextRange(4)))
        println("4. Backspacing empty bullet: ${emptyLine.value.pp()}")
    }
}
