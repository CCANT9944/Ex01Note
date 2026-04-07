package com.example.ex01

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
}

