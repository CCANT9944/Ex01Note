package com.example.ex01

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Modifier

@RunWith(AndroidJUnit4::class)
class SanityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBasicTextFieldLayout() {
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize()) {
                BasicTextField(
                    value = "Line 1",
                    onValueChange = {},
                    onTextLayout = {
                        android.util.Log.d("SanityTest", "onTextLayout fired! size=${it.size}")
                    }
                )
            }
        }
        
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().printToLog("SanityTestRoot")
        composeTestRule.onNodeWithText("Line 1").assertExists()
    }
}
