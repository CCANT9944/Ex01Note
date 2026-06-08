package com.example.ex01

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ex01.ui.editor.snote.SNoteEditorState
import com.example.ex01.ui.editor.snote.SNoteViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashReproduceTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test
    fun testSwitchModeCrash() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = SNoteViewModel()
        val state = SNoteEditorState(viewModel, context)

        composeTestRule.setContent {
            com.example.ex01.ui.editor.snote.SNoteCanvas(
                viewModel = viewModel,
                state = state,
                focusRequester = androidx.compose.ui.focus.FocusRequester(),
                bringIntoViewRequester = androidx.compose.foundation.relocation.BringIntoViewRequester(),
                commitChanges = {},
                commitActiveText = {}
            )
        }

        composeTestRule.waitForIdle()

        // Switch to Text mode
        composeTestRule.runOnIdle {
            viewModel.isTextMode = true
            viewModel.activeTextInputPosition = Offset(50f, 100f)
            viewModel.activeTextValue = TextFieldValue("Test text")
        }
        composeTestRule.waitForIdle()

        // Switch to Pen mode (Text mode = false, activeTextInputPosition becomes null)
        composeTestRule.runOnIdle {
            viewModel.isTextMode = false
            viewModel.activeTextInputPosition = null
        }
        composeTestRule.waitForIdle()
    }
}
