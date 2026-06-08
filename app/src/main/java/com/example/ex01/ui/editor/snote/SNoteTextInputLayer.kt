@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ex01.ui.editor.snote

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SNoteTextInputLayer(
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    commitChanges: () -> Unit,
    commitActiveText: (Boolean) -> Unit,
    scrollState: ScrollState,
    availableHeight: Dp,
    availableWidth: Dp,
    strokeColor: Color
) {
    val activePos = viewModel.activeTextInputPosition ?: return
    val density = LocalDensity.current

    val cVal = Color(state.currentColorValue.toULong())
    val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else strokeColor

    val xPosDp = with(density) { activePos.x.toDp() }

    Box(
        modifier = Modifier
            .offset { IntOffset(kotlin.math.round(activePos.x).toInt(), kotlin.math.round(activePos.y).toInt()) }
            .widthIn(max = availableWidth - xPosDp - 4.dp)
            .clipToBounds()
    ) {
        BasicTextField(
            value = viewModel.activeTextValue,
            onValueChange = { newValue ->
                val cleanText = newValue.text.replace("\r", "")
                val cleanValue = if (cleanText != newValue.text) newValue.copy(text = cleanText) else newValue
                if (cleanValue.text.length - viewModel.activeTextValue.text.length > 50) {
                    state.needsAutoCommitAfterPaste = true
                } else {
                    state.needsAutoCommitAfterPaste = false
                }
                viewModel.activeTextValue = cleanValue
            },
            onTextLayout = { result ->
                val currentPos = viewModel.activeTextInputPosition ?: return@BasicTextField
                try {
                    state.activeTextLayoutResult = result
                } catch (_: Exception) {}
                if (state.pageHeightPx > 0) {
                    val neededPages = kotlin.math.ceil(
                        ((currentPos.y + result.size.height) / state.pageHeightPx).toDouble()
                    ).toInt()
                    if (neededPages > viewModel.pageCount) viewModel.pageCount = neededPages
                }
            },
            modifier = Modifier
                .bringIntoViewRequester(bringIntoViewRequester)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .background(Color.Transparent)
                .clipToBounds(),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            textStyle = TextStyle(
                color = chosenColor,
                fontSize = with(density) { state.currentTextSize.toSp() },
                lineHeight = with(density) { SNoteConfig.getRowHeight(TEXT_LARGE).toSp() },
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                ),
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            cursorBrush = SolidColor(strokeColor)
        )
    }

    // Scroll to cursor when position or layout changes
    LaunchedEffect(viewModel.activeTextInputPosition, state.activeTextLayoutResult, availableHeight) {
        val pos = viewModel.activeTextInputPosition ?: return@LaunchedEffect
        val layout = state.activeTextLayoutResult ?: return@LaunchedEffect
        delay(50)
        scrollState.scrollToCursor(
            position = pos,
            selectionEnd = viewModel.activeTextValue.selection.end,
            layout = layout,
            availableHeightPx = with(density) { availableHeight.toPx() }
        )
    }

    // Scroll to cursor on text/selection change; also handle paste and periodic commit
    LaunchedEffect(viewModel.activeTextValue.text, viewModel.activeTextValue.selection, availableHeight) {
        delay(10)
        if (state.needsAutoCommitAfterPaste && state.activeTextLayoutResult != null) {
            state.needsAutoCommitAfterPaste = false
            commitActiveText(false)
        }
        val pos = viewModel.activeTextInputPosition
        val layout = state.activeTextLayoutResult
        if (pos != null && layout != null) {
            scrollState.scrollToCursor(
                position = pos,
                selectionEnd = viewModel.activeTextValue.selection.end,
                layout = layout,
                availableHeightPx = with(density) { availableHeight.toPx() }
            )
        }
        delay(300)
        commitChanges()
    }
}

private suspend fun ScrollState.scrollToCursor(
    position: androidx.compose.ui.geometry.Offset,
    selectionEnd: Int,
    layout: androidx.compose.ui.text.TextLayoutResult,
    availableHeightPx: Float
) {
    try {
        val cursorOffset = selectionEnd.coerceIn(0, layout.layoutInput.text.text.length)
        val cursorRect = layout.getCursorRect(cursorOffset)
        val absoluteTop = position.y + cursorRect.top - 60f
        val absoluteBottom = position.y + cursorRect.bottom + 140f
        val viewportTop = value.toFloat()
        val viewportBottom = viewportTop + availableHeightPx
        when {
            absoluteBottom > viewportBottom -> animateScrollTo((absoluteBottom - availableHeightPx).toInt())
            absoluteTop < viewportTop -> animateScrollTo(absoluteTop.toInt())
        }
    } catch (_: Exception) {}
}
