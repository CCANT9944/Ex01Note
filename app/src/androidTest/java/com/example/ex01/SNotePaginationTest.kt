package com.example.ex01

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ex01.ui.editor.snote.DrawingLine
import com.example.ex01.ui.editor.snote.SNoteCanvas
import com.example.ex01.ui.editor.snote.SNoteConfig
import com.example.ex01.ui.editor.snote.SNoteEditorState
import com.example.ex01.ui.editor.snote.SNoteViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SNotePaginationTest {
    @get:Rule
    val composeTestRule = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun testEnterKeyPaginationFlow() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = SNoteViewModel()
        val state = SNoteEditorState(viewModel, context)
        
        viewModel.pageCount = 1

        composeTestRule.setContent {
            SNoteCanvas(
                viewModel = viewModel,
                state = state,
                focusRequester = androidx.compose.ui.focus.FocusRequester(),
                bringIntoViewRequester = androidx.compose.foundation.relocation.BringIntoViewRequester(),
                commitChanges = {},
                commitActiveText = {
                    state.commitActiveText(onSerializedBodyChange = {})
                }
            )
        }

        // Wait for composition to determine pageHeightPx
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            state.pageHeightPx > 0f
        }

        val pageHeight = state.pageHeightPx
        val gap = 40f * state.currentDensity // SNoteConfig.PAGE_GAP_DP = 40
        val pageBottom = pageHeight - gap

        // 1. Start editing a text block on Page 1
        // Place it such that "Line 1" fits on Page 1, but adding many newlines guarantees crossing pageBottom
        val startY = pageBottom - 30f
        
        composeTestRule.runOnIdle {
            viewModel.isTextMode = true
            viewModel.activeTextInputPosition = Offset(50f, startY)
            viewModel.activeTextValue = TextFieldValue("Line 1", selection = TextRange(6))
        }

        // Wait for layout to settle
        composeTestRule.waitForIdle()

        // Verify that nothing is committed yet (since it fits on page 1)
        composeTestRule.runOnIdle {
            assertTrue("No lines should be committed yet", viewModel.drawingLines.isEmpty())
            assertEquals("Line 1", viewModel.activeTextValue.text)
            assertEquals(startY, viewModel.activeTextInputPosition?.y)
        }

        // 2. Press Enter to simulate typing a newline that pushes the layout to the next page.
        composeTestRule.onNodeWithText("Line 1").performTextInput("\n\n\n\n\n\n\nLine 2")

        // Wait until pagination triggers and drawingLines is populated
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.drawingLines.isNotEmpty()
        }

        // 3. Verify that the pagination engine triggered an autoJump and split the text block.
        composeTestRule.runOnIdle {
            println("DEBUG: drawingLines size: ${viewModel.drawingLines.size}")
            viewModel.drawingLines.forEachIndexed { index, line ->
                println("DEBUG: drawingLines[$index] text: '${line.text}' y: ${line.points.firstOrNull()?.y}")
            }
            val committedLine = viewModel.drawingLines.first()
            assertEquals("Line 1", committedLine.text)
            assertEquals(startY, committedLine.points.first().y)
            
            // Check active text block has jumped to the second page
            val activePos = viewModel.activeTextInputPosition
            assertTrue("Active editor should be active on page 2 (current pageHeight: $pageHeight, activePos: $activePos)", activePos != null && activePos.y >= pageHeight)
            assertEquals("Line 2", viewModel.activeTextValue.text)
        }
    }

    @Test
    fun testMultiEnterRowByRowPagination() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = SNoteViewModel()
        val state = SNoteEditorState(viewModel, context)
        
        viewModel.pageCount = 1

        composeTestRule.setContent {
            SNoteCanvas(
                viewModel = viewModel,
                state = state,
                focusRequester = androidx.compose.ui.focus.FocusRequester(),
                bringIntoViewRequester = androidx.compose.foundation.relocation.BringIntoViewRequester(),
                commitChanges = {},
                commitActiveText = {
                    state.commitActiveText(onSerializedBodyChange = {})
                }
            )
        }

        // Wait for composition to determine pageHeightPx
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            state.pageHeightPx > 0f
        }

        val pageHeight = state.pageHeightPx
        val gap = 40f * state.currentDensity
        val pageBottom = pageHeight - gap

        // Start editing a 4-line text block close to page bottom
        val rowHeight = SNoteConfig.getRowHeight(24f) // TEXT_LARGE = 24
        val startY = pageBottom - 2.5f * rowHeight
        
        composeTestRule.runOnIdle {
            viewModel.isTextMode = true
            viewModel.activeTextInputPosition = Offset(50f, startY)
            viewModel.activeTextValue = TextFieldValue("Line 1\nLine 2\nLine 3\nLine 4", selection = TextRange(0))
        }

        composeTestRule.waitForIdle()

        // Press Enter 4 times at the beginning of the text to push it row-by-row
        for (i in 1..4) {
            composeTestRule.onNode(hasSetTextAction()).performTextInput("\n")
            composeTestRule.waitForIdle()
        }

        // Wait until pagination processes
        composeTestRule.waitForIdle()

        // Verify that all committed lines end up on Page 2, not scattered across multiple pages
        composeTestRule.runOnIdle {
            println("DEBUG TEST: drawingLines size: ${viewModel.drawingLines.size}")
            viewModel.drawingLines.forEachIndexed { index, line ->
                println("DEBUG TEST: drawingLines[$index] text: '${line.text}' y: ${line.points.firstOrNull()?.y}")
            }
            
            // The committed lines should contain Line 1, Line 2, Line 3, Line 4 on Page 2
            val committed = viewModel.drawingLines.filter { it.text != null && it.points.isNotEmpty() }
            assertTrue("Should have committed lines", committed.isNotEmpty())
            
            committed.forEach { line ->
                val lineY = line.points.first().y
                val pageIndex = kotlin.math.floor(lineY / pageHeight).toInt()
                assertEquals("Line '${line.text}' at Y $lineY should be on Page 2 (index 1)", 1, pageIndex)
            }
        }
    }
}
