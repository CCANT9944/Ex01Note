package com.example.ex01.ui.editor.snote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SNoteBackgroundLayer(
    state: SNoteEditorState,
    pageCount: Int,
    eraserColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(state.pageHeightDp * pageCount)
    ) {
        drawRect(color = eraserColor, size = size)
    }
}
