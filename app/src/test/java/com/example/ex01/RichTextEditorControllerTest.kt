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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextEditorControllerTest {
    private class FakeClock(var nowMillis: Long = 0L) {
        fun now(): Long = nowMillis
    }

    @Test
    fun typingUpdates_areGroupedIntoSingleUndoStepWithinBurstWindow() {
        val clock = FakeClock()
        val initial = TextFieldValue("Hello", selection = TextRange(5))
        val controller = RichTextEditorController(initial, clock::now)

        controller.updateValue(TextFieldValue("Hello ", selection = TextRange(6)))
        clock.nowMillis += 120
        controller.updateValue(TextFieldValue("Hello w", selection = TextRange(7)))
        clock.nowMillis += 120
        controller.updateValue(TextFieldValue("Hello wo", selection = TextRange(8)))

        assertTrue(controller.canUndo)
        assertEquals(TextFieldValue("Hello wo", selection = TextRange(8)), controller.value)

        assertTrue(controller.undo())

        assertEquals(initial, controller.value)
        assertFalse(controller.canUndo)
        assertFalse(controller.undo())
    }

    @Test
    fun typingUpdates_afterPause_createAnotherUndoStep() {
        val clock = FakeClock()
        val initial = TextFieldValue("Hello", selection = TextRange(5))
        val controller = RichTextEditorController(initial, clock::now)

        controller.updateValue(TextFieldValue("Hello!", selection = TextRange(6)))
        clock.nowMillis += 900
        controller.updateValue(TextFieldValue("Hello!!", selection = TextRange(7)))

        assertTrue(controller.canUndo)

        assertTrue(controller.undo())
        assertEquals(TextFieldValue("Hello!", selection = TextRange(6)), controller.value)
        assertTrue(controller.canUndo)

        assertTrue(controller.undo())
        assertEquals(initial, controller.value)
        assertFalse(controller.canUndo)
    }

    @Test
    fun formattingActions_areUndoable() {
        val initial = TextFieldValue("Hello world", selection = TextRange(6, 11))
        val controller = RichTextEditorController(initial)

        controller.toggleBold()

        assertTrue(controller.canUndo)
        assertTrue(controller.value.text.contains("\uE000"))

        assertTrue(controller.undo())

        assertEquals(initial, controller.value)
        assertFalse(controller.canUndo)
    }

    @Test
    fun selectionOnlyUpdates_doNotCreateUndoHistory() {
        val controller = RichTextEditorController(TextFieldValue("Hello", selection = TextRange(5)))

        controller.updateValue(TextFieldValue("Hello", selection = TextRange(0)))

        assertEquals(TextFieldValue("Hello", selection = TextRange(0)), controller.value)
        assertFalse(controller.canUndo)
    }

    @Test
    fun replaceValue_clearsUndoHistory_withoutKeepingOldSnapshot() {
        val controller = RichTextEditorController(TextFieldValue("Hello", selection = TextRange(5)))

        controller.updateValue(TextFieldValue("Hello!", selection = TextRange(6)))
        controller.replaceValue(TextFieldValue("World", selection = TextRange(5)))

        assertEquals(TextFieldValue("World", selection = TextRange(5)), controller.value)
        assertFalse(controller.canUndo)
        assertFalse(controller.undo())
    }

    @Test
    fun typingPlainCharacter_skipsSanitization() {
        val initial = TextFieldValue("[b][/b]")
        val controller = RichTextEditorController(initial)

        // Type 'x' at the end. Since 'x' is plain text, needsSanitization should return false.
        val next = TextFieldValue("[b][/b]x", selection = TextRange(8))
        controller.updateValue(next)

        // If sanitization had run, "[b][/b]" would have been normalized and collapsed to "",
        // so the value would be "x". Since it was skipped, it should still be "[b][/b]x".
        assertEquals("[b][/b]x", controller.value.text)
    }

    @Test
    fun typingControlCharacter_runsSanitization() {
        val initial = TextFieldValue("[b][/b]")
        val controller = RichTextEditorController(initial)

        // Type '[' at the end. Since '[' is a control character, needsSanitization should return true.
        val next = TextFieldValue("[b][/b][", selection = TextRange(8))
        controller.updateValue(next)

        // Sanitization runs, collapsing "[b][/b]" to "", leaving only "[".
        assertEquals("[", controller.value.text)
    }

    @Test
    fun deletingCharacter_runsSanitization() {
        val initial = TextFieldValue("[b][/b]x")
        val controller = RichTextEditorController(initial)

        // Delete 'x' at the end. Since length decreases, needsSanitization should return true.
        val next = TextFieldValue("[b][/b]", selection = TextRange(7))
        controller.updateValue(next)

        // Sanitization runs, collapsing "[b][/b]" to "".
        assertEquals("", controller.value.text)
    }

    @Test
    fun replacingCharacter_runsSanitization() {
        // Here we replace 'x' with 'y' (same length modification)
        val initial = TextFieldValue("[b][/b]x")
        val controller = RichTextEditorController(initial)

        // Replace 'x' with 'y'.
        val next = TextFieldValue("[b][/b]y", selection = TextRange(8))
        controller.updateValue(next)

        // Sanitization runs because a character was replaced, collapsing "[b][/b]" to "", leaving "y".
        assertEquals("y", controller.value.text)
    }
}


