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
class Bullet4Test {
    @Test
    fun testNewlineInsideBullet() {
        val initialText = "12\n" + BULLET_OPEN_MARKER + "Item 1" + "\n" + "Item 2" + BULLET_CLOSE_MARKER + "\n34"
        val initialValue = TextFieldValue(initialText, TextRange(initialText.indexOf("Item 2")))
        val controller = RichTextEditorController(initialValue)
        println("Is bullet active initially: " + controller.value.text.contains(BULLET_OPEN_MARKER))
        controller.toggleBullet()
        val t = controller.value.text
        println("After toggle text: " + t.replace(BULLET_OPEN_MARKER.toString(), "[B]").replace(BULLET_CLOSE_MARKER.toString(), "[/B]").replace("\n", "\\n"))
    }
}
