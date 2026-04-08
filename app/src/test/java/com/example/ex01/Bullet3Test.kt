package com.example.ex01
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Test
class Bullet3Test {
    private fun TextFieldValue.pp() = text.replace("\uE000", "[B]").replace("\uE001", "[/B]").replace("\n", "[LF]").replace("\uE008", "[BUL]").replace("\uE009", "[/BUL]")
    @Test
    fun testRewriteBullet() {
        val controller = RichTextEditorController(TextFieldValue("Item 1\nItem 2\nItem 3", selection = TextRange(8)))
        // Cursor at "Ite|m 2"
        // Emulate the proposed toggleBulletFormatting logic
        val raw = controller.value.text
        // 1. toggle on
        val newVal = toggleFormatting(TextFieldValue(raw, selection = TextRange(7, 13)), FormattingMarkerPair('\uE008', '\uE009', "bullet"))
        controller.updateValue(newVal)
        println("1. Bullet line 2: ${controller.value.pp()} - isBullet: ${richTextFormattingState(controller.value).bulletActive}")
        // 2. press enter
        var s = controller.value.selection.start
        var t = controller.value.text
        controller.updateValue(TextFieldValue(t.substring(0, s) + "\n" + t.substring(s), selection = TextRange(s + 1)))
        // Emulate sanitize
        controller.updateValue(sanitizeRichTextTyping(controller.value))
        println("2. Enter inside line 2: ${controller.value.pp()} - isBullet: ${richTextFormattingState(controller.value).bulletActive}")
    }
}
