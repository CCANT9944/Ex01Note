package com.example.ex01.ui.editor.snote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ex01.utils.computeSelectionBounds

@Composable
fun SNoteStaticTextLayer(
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    availableWidth: Dp,
    strokeColor: Color
) {
    val density = LocalDensity.current
    viewModel.drawingLines.forEach { line ->
        if (line.text == null || line.points.isEmpty()) return@forEach
        val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
        val xPosDp = with(density) { line.points.first().x.toDp() }
        val activePos = viewModel.activeTextInputPosition
        val layRes = state.activeTextLayoutResult
        val liveDeltaY = if (activePos != null && layRes != null) {
            val sessionTopY = activePos.y
            val originalBottomY = sessionTopY + state.activeTextOriginalHeight
            val newBottomY = sessionTopY + layRes.size.height.toFloat()
            newBottomY - originalBottomY
        } else 0f
        val originalBottomY = if (activePos != null) activePos.y + state.activeTextOriginalHeight else 0f
        val pushThreshold = originalBottomY - 5f

        var y = line.points.first().y
        if (liveDeltaY > 1f && y >= pushThreshold) {
            y += liveDeltaY
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        kotlin.math.round(line.points.first().x).toInt(),
                        kotlin.math.round(y).toInt()
                    )
                }
                .widthIn(max = availableWidth - xPosDp - 4.dp)
                .clipToBounds()
        ) {
            Text(
                text = line.text,
                onTextLayout = { result ->
                    state.staticTextLayouts[line] = result
                    val bottomY = y + result.size.height
                    if (state.pageHeightPx > 0) {
                        val neededPages = kotlin.math.ceil((bottomY / state.pageHeightPx).toDouble()).toInt()
                        if (neededPages > viewModel.pageCount) viewModel.pageCount = neededPages
                    }
                },
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                style = TextStyle(
                    color = activeLineColor,
                    fontSize = with(density) { line.strokeWidth.toSp() },
                    lineHeight = with(density) { SNoteConfig.getRowHeight(TEXT_LARGE).toSp() },
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
fun SNoteSelectedTextLayer(
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    availableWidth: Dp,
    strokeColor: Color
) {
    if (viewModel.selectedLines.none { it.text != null }) return
    val density = LocalDensity.current

    val tBounds = viewModel.selectedLines
        .filter { it.text != null && it.points.isNotEmpty() }
        .associateWith { l ->
            Pair(
                state.staticTextLayouts[l]?.size?.width?.toFloat() ?: (l.strokeWidth * 0.6f * l.text!!.length.toFloat()),
                state.staticTextLayouts[l]?.size?.height?.toFloat() ?: (l.strokeWidth * 1.5f)
            )
        }
    val bounds = computeSelectionBounds(viewModel.selectedLines, tBounds) ?: return

    viewModel.selectedLines.forEach { line ->
        if (line.text == null || line.points.isEmpty()) return@forEach
        val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
        val pt = line.points.first()
        val pX = bounds.cX + (pt.x - bounds.cX) * viewModel.selectionScale + viewModel.selectionDragOffset.x
        val pY = bounds.cY + (pt.y - bounds.cY) * viewModel.selectionScale + viewModel.selectionDragOffset.y

        val xPosDp = with(density) { pX.coerceAtLeast(0f).toDp() }

        Box(
            modifier = Modifier
                .offset { IntOffset(kotlin.math.round(pX).toInt(), kotlin.math.round(pY).toInt()) }
                .widthIn(max = (availableWidth - xPosDp - 4.dp).coerceAtLeast(10.dp))
                .clipToBounds()
        ) {
            Text(
                text = line.text,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                style = TextStyle(
                    color = activeLineColor.copy(alpha = 0.7f),
                    fontSize = with(density) { (line.strokeWidth * viewModel.selectionScale).coerceAtLeast(1f).toSp() },
                    lineHeight = with(density) { (SNoteConfig.getRowHeight(TEXT_LARGE) * viewModel.selectionScale).coerceAtLeast(1f).toSp() },
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}
