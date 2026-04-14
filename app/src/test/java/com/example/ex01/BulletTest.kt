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
class BulletTest {
    private fun TextFieldValue.pp() = text.replace("\uE000", "[B]").replace("\uE001", "[/B]").replace("\n", "[LF]").replace("\uE008", "[BUL]").replace("\uE009", "[/BUL]")
    @Test
    fun testBullet() {
        val controller = RichTextEditorController(TextFieldValue("Item 1", selection = TextRange(6)))
        controller.toggleBullet()
        println("1. Bullet applied: ${controller.value.pp()} - cursor: ${controller.value.selection.start}")
        var s = controller.value.selection.start
        var t = controller.value.text
        controller.updateValue(TextFieldValue(t.substring(0, s) + "\n" + t.substring(s), selection = TextRange(s + 1)))
        println("2. Pressed enter: ${controller.value.pp()} - cursor: ${controller.value.selection.start}")
        controller.toggleBullet()
        println("3. Toggled bullet off: ${controller.value.pp()} - cursor: ${controller.value.selection.start}")
        s = controller.value.selection.start
        t = controller.value.text
        controller.updateValue(TextFieldValue(t.substring(0, s - 1) + t.substring(s), selection = TextRange(s - 1)))
        println("4. Backspaced: ${controller.value.pp()} - cursor: ${controller.value.selection.start}")
    }
}
