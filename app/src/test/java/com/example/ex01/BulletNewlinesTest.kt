package com.example.ex01

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class BulletNewlinesTest {

    @Test
    fun testNewlineInBulletSplitsIt() {
        // [BULLET_OPEN]hello\nworld[BULLET_CLOSE]
        val raw = "${BULLET_OPEN_MARKER}hello\nworld${BULLET_CLOSE_MARKER}"
        val initial = TextFieldValue(raw, selection = TextRange(0))
        val sanitized = sanitizeRichTextTyping(initial)

        val expected = "${BULLET_OPEN_MARKER}hello${BULLET_CLOSE_MARKER}\n${BULLET_OPEN_MARKER}world${BULLET_CLOSE_MARKER}"
        assertEquals(expected, sanitized.text)
    }

    @Test
    fun testNewlineBeforeBulletDoesNotSplit() {
        val raw = "\n${BULLET_OPEN_MARKER}world${BULLET_CLOSE_MARKER}"
        val initial = TextFieldValue(raw, selection = TextRange(0))
        val sanitized = sanitizeRichTextTyping(initial)
        assertEquals(raw, sanitized.text)
    }

}

