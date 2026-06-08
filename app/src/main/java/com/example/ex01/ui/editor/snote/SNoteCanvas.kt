@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ex01.ui.editor.snote

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints

@Composable
fun SNoteCanvas(
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    focusRequester: FocusRequester,
    bringIntoViewRequester: androidx.compose.foundation.relocation.BringIntoViewRequester,
    commitChanges: () -> Unit,
    commitActiveText: (Boolean) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val strokeColor = MaterialTheme.colorScheme.onSurface
    val eraserColor = MaterialTheme.colorScheme.surface

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        val availableWidth = maxWidth
        val density = LocalDensity.current
        val availableWidthPx = with(density) { availableWidth.toPx() }
        state.currentCanvasWidthPx = availableWidthPx
        state.currentDensity = density.density

        LaunchedEffect(availableHeight) {
            if (state.pageHeightDp == 0.dp) {
                state.pageHeightDp = availableHeight
                state.pageHeightPx = with(density) { availableHeight.toPx() }
            }
        }

        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            if (state.pageHeightDp > 0.dp) {
                SNoteBackgroundLayer(state, viewModel.pageCount, eraserColor)

                SNoteDrawingLayer(
                    viewModel = viewModel,
                    state = state,
                    availableWidthPx = availableWidthPx,
                    primaryColor = primaryColor,
                    strokeColor = strokeColor,
                    commitChanges = commitChanges,
                    commitActiveText = commitActiveText
                )

                SNoteStaticTextLayer(viewModel, state, availableWidth, strokeColor)
                SNoteSelectedTextLayer(viewModel, state, availableWidth, strokeColor)

                SNoteLassoOverlays(viewModel, state, strokeColor, commitChanges)

                SNoteTextInputLayer(
                    viewModel = viewModel,
                    state = state,
                    focusRequester = focusRequester,
                    bringIntoViewRequester = bringIntoViewRequester,
                    commitChanges = commitChanges,
                    commitActiveText = commitActiveText,
                    scrollState = scrollState,
                    availableHeight = availableHeight,
                    availableWidth = availableWidth,
                    strokeColor = strokeColor
                )
            }
        }
    }
}
