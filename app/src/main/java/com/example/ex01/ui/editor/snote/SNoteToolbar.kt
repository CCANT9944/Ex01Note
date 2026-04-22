package com.example.ex01.ui.editor.snote
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
@Composable
fun SNoteToolbar(
    viewModel: SNoteViewModel,
    currentColorValue: Long,
    currentThickness: Float,
    currentEraserThickness: Float,
    currentHighlighterThickness: Float,
    currentTextSize: Float,
    onThicknessChange: (Float) -> Unit,
    onEraserThicknessChange: (Float) -> Unit,
    onHighlighterThicknessChange: (Float) -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onColorChange: (Long) -> Unit,
    commitActiveText: () -> Unit,
    commitChanges: () -> Unit
) = with(viewModel) {
    val strokeColor = MaterialTheme.colorScheme.onSurface
    val eraserColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    val toolbarBgColor = Color.DarkGray
    val activeModeColor = Color(0xFF4CAF50) // Material Green
    val iconColor = Color.White
    val dividerColor = Color.White.copy(alpha = 0.3f)

        Surface(
            modifier = Modifier.fillMaxWidth().zIndex(1f),
            color = toolbarBgColor,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Scrollable Tools Group
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = {
                                commitActiveText()
                                if (!isEraserMode && !isHighlighterMode && !isTextMode && !isLassoMode) {
                                    showThicknessMenu = true
                                } else {
                                    isEraserMode = false
                                    isHighlighterMode = false
                                    isTextMode = false
                                    isLassoMode = false
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (!isEraserMode && !isHighlighterMode && !isTextMode && !isLassoMode) activeModeColor else Color.Transparent,
                                contentColor = iconColor
                            )
                        ) {
                            Icon(Icons.Default.Create, contentDescription = "Pen", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showThicknessMenu,
                            onDismissRequest = { showThicknessMenu = false },
                            modifier = Modifier.width(48.dp) // Force tight width constraint
                        ) {
                            listOf(PEN_THIN, PEN_MEDIUM, PEN_THICK).forEach { thickness ->
                                DropdownMenuItem(
                                    modifier = if (currentThickness == thickness) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
                                    contentPadding = PaddingValues(horizontal = 4.dp), // Tiny padding
                                    text = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.size(32.dp, 24.dp)) {
                                                drawLine(
                                                    color = if (currentThickness == thickness) primaryColor else strokeColor,
                                                    start = Offset(0f, size.height / 2),
                                                    end = Offset(size.width, size.height / 2),
                                                    strokeWidth = thickness,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    },
                                    onClick = { onThicknessChange(thickness); showThicknessMenu = false }
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(
                            onClick = {
                                commitActiveText()
                                if (isEraserMode) {
                                    showEraserThicknessMenu = true
                                } else {
                                    isEraserMode = true
                                    isHighlighterMode = false
                                    isTextMode = false
                                    isLassoMode = false
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isEraserMode) activeModeColor else Color.Transparent,
                                contentColor = iconColor
                            )
                        ) {
                            Icon(EraserIcon, contentDescription = "Eraser", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showEraserThicknessMenu,
                            onDismissRequest = { showEraserThicknessMenu = false },
                            modifier = Modifier.width(48.dp) // Force tight width constraint
                        ) {
                            val eraserVisualColor = strokeColor.copy(alpha = 0.3f)
                            listOf(ERASER_THIN, ERASER_MEDIUM, ERASER_THICK).forEach { thickness ->
                                DropdownMenuItem(
                                    modifier = if (currentEraserThickness == thickness) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
                                    contentPadding = PaddingValues(horizontal = 4.dp), // Tiny padding
                                    text = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.size(32.dp, 24.dp)) {
                                                drawLine(
                                                    color = if (currentEraserThickness == thickness) primaryColor.copy(alpha = 0.5f) else eraserVisualColor,
                                                    start = Offset(0f, size.height / 2),
                                                    end = Offset(size.width, size.height / 2),
                                                    strokeWidth = thickness,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    },
                                    onClick = { onEraserThicknessChange(thickness); showEraserThicknessMenu = false }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = {
                                commitActiveText()
                                if (isHighlighterMode) {
                                    showHighlighterThicknessMenu = true
                                } else {
                                    isHighlighterMode = true
                                    isEraserMode = false
                                    isTextMode = false
                                    isLassoMode = false
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isHighlighterMode) activeModeColor else Color.Transparent,
                                contentColor = iconColor
                            )
                        ) {
                            Icon(HighlighterIcon, contentDescription = "Highlighter", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showHighlighterThicknessMenu,
                            onDismissRequest = { showHighlighterThicknessMenu = false },
                            modifier = Modifier.width(48.dp) // Force tight width constraint
                        ) {
                            listOf(HIGHLIGHTER_THIN, HIGHLIGHTER_MEDIUM, HIGHLIGHTER_THICK).forEach { thickness ->
                                DropdownMenuItem(
                                    modifier = if (currentHighlighterThickness == thickness) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
                                    contentPadding = PaddingValues(horizontal = 4.dp), // Tiny padding
                                    text = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.size(32.dp, 24.dp)) {
                                                val hColor = (if (currentColorValue == Color.Unspecified.value.toLong()) strokeColor else Color(currentColorValue.toULong())).copy(alpha = 0.4f)
                                                drawLine(
                                                    color = hColor,
                                                    start = Offset(0f, size.height / 2),
                                                    end = Offset(size.width, size.height / 2),
                                                    strokeWidth = thickness,
                                                    cap = StrokeCap.Square
                                                )
                                            }
                                        }
                                    },
                                    onClick = { onHighlighterThicknessChange(thickness); showHighlighterThicknessMenu = false }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = {
                                if (!isTextMode) {
                                    commitActiveText()
                                    isTextMode = true
                                    isHighlighterMode = false
                                    isEraserMode = false
                                    isLassoMode = false
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isTextMode) activeModeColor else Color.Transparent,
                                contentColor = iconColor
                            )
                        ) {
                            Icon(TextIcon, contentDescription = "Text", modifier = Modifier.size(20.dp))
                        }
                    }

                    Box {
                        IconButton(
                            onClick = {
                                commitActiveText()
                                isLassoMode = true
                                isEraserMode = false
                                isHighlighterMode = false
                                isTextMode = false
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isLassoMode) activeModeColor else Color.Transparent,
                                contentColor = iconColor
                            )
                        ) {
                            Icon(LassoIcon, contentDescription = "Lasso", modifier = Modifier.size(20.dp))
                        }
                    }

                    Box {
                        IconButton(onClick = { showColorMenu = true }, modifier = Modifier.size(38.dp)) {
                            Box(modifier = Modifier
                                .size(20.dp)
                                .background(if (currentColorValue == Color.Unspecified.value.toLong()) strokeColor else Color(currentColorValue.toULong()), shape = CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            )
                        }
                        DropdownMenu(
                            expanded = showColorMenu,
                            onDismissRequest = { showColorMenu = false },
                            modifier = Modifier.width(48.dp)
                        ) {
                            val penColors = listOf(Color.Unspecified) + ALLOWED_PEN_COLORS
                            penColors.forEach { c ->
                                DropdownMenuItem(
                                    modifier = if (currentColorValue == c.value.toLong()) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    text = {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                            Box(modifier = Modifier
                                                .size(24.dp)
                                                .background(if (c == Color.Unspecified) strokeColor else c, shape = CircleShape)
                                                .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onColorChange(c.value.toLong())
                                        showColorMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(20.dp)
                            .width(1.dp)
                            .background(dividerColor)
                    )

                    IconButton(
                        onClick = {
                            if (activeTextInputPosition != null) {
                                // Cancel text editing and restore original hit line
                                activeTextInputPosition = null
                                activeTextValue = TextFieldValue("")
                                if (originalHitLine != null) {
                                    val index = if (originalHitIndex in 0..drawingLines.size) originalHitIndex else drawingLines.size
                                    drawingLines.add(index, originalHitLine!!)
                                    originalHitLine = null
                                    originalHitIndex = -1
                                }
                                commitChanges()
                            } else {
                                undo()
                                commitChanges()
                            }
                        },
                        modifier = Modifier.size(38.dp),
                        enabled = undoStack.isNotEmpty() || activeTextInputPosition != null,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = iconColor,
                            disabledContentColor = iconColor.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = {
                            commitActiveText()
                            redo()
                            commitChanges()
                        },
                        modifier = Modifier.size(38.dp),
                        enabled = redoStack.isNotEmpty(),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = iconColor,
                            disabledContentColor = iconColor.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(20.dp))
                    }
                } // Close Scrollable Row

                // Fixed right-side actions
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(20.dp)
                            .width(1.dp)
                            .background(dividerColor)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    TextButton(
                        onClick = {
                            commitActiveText()
                            if (drawingLines.isNotEmpty()) {
                                showClearWarning = true
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear All", fontSize = 14.sp)
                    }
                }
            } // Close Outer Row
        } // Close Surface Toolbar

}
