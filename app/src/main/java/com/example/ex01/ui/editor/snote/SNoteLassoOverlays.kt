package com.example.ex01.ui.editor.snote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun SNoteLassoOverlays(
    viewModel: SNoteViewModel,
    state: SNoteEditorState,
    strokeColor: Color,
    commitChanges: () -> Unit
) {
    if (state.showLassoMenu && viewModel.selectedLines.isNotEmpty()) {
        val xDp = with(LocalDensity.current) { state.lassoMenuPosition.x.toDp() }
        val yDp = with(LocalDensity.current) { state.lassoMenuPosition.y.toDp() }
        Box(modifier = Modifier.offset(x = xDp, y = yDp)) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { state.showLassoMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Colour") },
                    leadingIcon = { Icon(Icons.Default.Create, contentDescription = "Colour") },
                    onClick = {
                        state.showLassoMenu = false
                        state.showLassoColorPicker = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                    onClick = {
                        viewModel.drawingLines.removeAll(viewModel.selectedLines)
                        viewModel.selectedLines.clear()
                        state.showLassoMenu = false
                    }
                )
            }
        }
    }

    if (state.showLassoColorPicker && viewModel.selectedLines.isNotEmpty()) {
        val xDp = with(LocalDensity.current) { state.lassoMenuPosition.x.toDp() }
        val yDp = with(LocalDensity.current) { state.lassoMenuPosition.y.toDp() }
        Box(modifier = Modifier.offset(x = xDp, y = yDp)) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { state.showLassoColorPicker = false },
                modifier = Modifier.width(64.dp)
            ) {
                val penCols = listOf(Color.Unspecified) + ALLOWED_PEN_COLORS
                penCols.forEach { c ->
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        text = {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = if (c == Color.Unspecified) strokeColor else c,
                                            shape = CircleShape
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                )
                            }
                        },
                        onClick = {
                            viewModel.pushUndoState(
                                viewModel.drawingLines.toList() + viewModel.selectedLines.toList()
                            )
                            val newSelection = viewModel.selectedLines.map { l -> l.copy(color = c) }
                            viewModel.selectedLines.clear()
                            viewModel.selectedLines.addAll(newSelection)
                            state.updatePenColor(
                                if (c != Color.Unspecified) c.value.toLong()
                                else Color.Unspecified.value.toLong()
                            )
                            state.showLassoColorPicker = false
                            state.commitLassoSelection { commitChanges() }
                        }
                    )
                }
            }
        }
    }
}
