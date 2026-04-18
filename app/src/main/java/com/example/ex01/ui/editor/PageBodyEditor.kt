package com.example.ex01.ui.editor

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalFocusManager

import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PageBodyEditor(
    serializedPagesBody: String,
    selectedPageIndex: Int,
    pageControllers: MutableMap<Int, RichTextEditorController>,
    onSelectedPageIndexChange: (Int) -> Unit,
    onSerializedPagesBodyChange: (String) -> Unit,
) {
    var lastParsedBody by remember { mutableStateOf(serializedPagesBody) }
    var blocks by remember { mutableStateOf(serializedPagesBody.split("\n\n").toMutableList()) }

    if (serializedPagesBody != lastParsedBody) {
        lastParsedBody = serializedPagesBody
        blocks = serializedPagesBody.split("\n\n").toMutableList()
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var editingPageIndex by remember { mutableStateOf<Int?>(null) }
    val focusManager = LocalFocusManager.current

    val activeIndex = remember { mutableIntStateOf(selectedPageIndex.coerceIn(0, maxOf(0, blocks.lastIndex))) }

    val activeController = pageControllers.getOrPut(activeIndex.intValue) {
        val txt = blocks.getOrNull(activeIndex.intValue) ?: ""
        RichTextEditorController(
            TextFieldValue(txt, selection = TextRange(txt.length))
        )
    }

    fun commitBlocks() {
        val newBody = blocks.joinToString("\n\n")
        lastParsedBody = newBody
        onSerializedPagesBodyChange(newBody)
    }

    DisposableEffect(Unit) {
        onDispose { commitBlocks() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoteWritingToolbar(
            value = activeController.value,
            canUndo = activeController.canUndo,
            onUndoClick = {
                if (activeController.undo()) commitBlocks()
            },
            onBoldClick = {
                activeController.toggleBold()
                commitBlocks()
            },
            onItalicClick = {
                activeController.toggleItalic()
                commitBlocks()
            },
            onUnderlineClick = {
                activeController.toggleUnderline()
                commitBlocks()
            },
            onStrikethroughClick = {
                activeController.toggleStrikethrough()
                commitBlocks()
            },
            onBulletClick = {
                activeController.toggleBullet()
                commitBlocks()
            },
            onIndentClick = {
                activeController.indent()
                commitBlocks()
            },
            onOutdentClick = {
                activeController.outdent()
                commitBlocks()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()

            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress) {
                    focusManager.clearFocus()
                }
            }

            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(blocks) { index, blockText ->
                    val blockController = pageControllers.getOrPut(index) {
                        RichTextEditorController(
                            TextFieldValue(blockText, selection = TextRange(blockText.length))
                        )
                    }

                    LaunchedEffect(blockText) {
                        if (blockController.value.text != blockText) {
                            blockController.replaceValue(
                                TextFieldValue(
                                    blockText,
                                    selection = blockController.value.selection
                                )
                            )
                        }
                    }

                    LaunchedEffect(blockController.value.text) {
                        kotlinx.coroutines.delay(400)
                        if (blockController.value.text != blockText) {
                            blocks[index] = blockController.value.text
                            commitBlocks()
                        }
                    }

                    RichTextBodyEditor(
                        value = blockController.value,
                        onValueChange = { next ->
                            if (next.text.contains("\n\n")) {
                                val parts = next.text.split("\n\n", limit = 2)
                                blockController.updateValue(TextFieldValue(parts[0], TextRange(parts[0].length)))
                                blocks[index] = parts[0]
                                blocks.add(index + 1, parts[1])
                                // shifts subsequent controllers
                                for (i in blocks.size - 1 downTo index + 1) {
                                    val old = pageControllers[i - 1]
                                    if (old != null && i > index + 1) {
                                        pageControllers[i] = old
                                    }
                                }
                                pageControllers[index + 1] = RichTextEditorController(TextFieldValue(parts[1], TextRange(0)))
                                setOf(index, index + 1).forEach { activeIndex.intValue = it }
                                commitBlocks()
                            } else {
                                blockController.updateValue(next)
                                blocks[index] = next.text
                                // Instant sync not needed due to delay above
                            }
                        },
                        onFocus = {
                            activeIndex.intValue = index
                            onSelectedPageIndexChange(index)
                        },
                        onBackspaceAtStart = {
                            if (index > 0) {
                                val prevText = blocks[index - 1]
                                val currentText = blockController.value.text
                                val newIndex = index - 1
                                blocks[newIndex] = prevText + currentText
                                blocks.removeAt(index)
                                
                                val prevController = pageControllers[newIndex]
                                prevController?.updateValue(TextFieldValue(blocks[newIndex], selection = TextRange(prevText.length)))
                                pageControllers.remove(blocks.size) // remove last
                                
                                activeIndex.intValue = newIndex
                                commitBlocks()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .windowInsetsBottomHeight(WindowInsets.ime)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RichTextBodyEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onFocus: () -> Unit,
    onBackspaceAtStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val cachedVisualTransformation = remember { richTextVisualTransformation() }

    LaunchedEffect(isFocused, value.selection) {
        if (!isFocused) return@LaunchedEffect
        val layoutResult = textLayoutResult.value ?: return@LaunchedEffect
        val cursorOffset = value.selection.end.coerceIn(0, value.text.length)

        val transformedCursorOffset = cachedVisualTransformation
            .filter(AnnotatedString(value.text))
            .offsetMapping
            .originalToTransformed(cursorOffset)
            .coerceIn(0, layoutResult.layoutInput.text.text.length)

        val cursorRect = layoutResult.getCursorRect(transformedCursorOffset)
        bringIntoViewRequester.bringIntoView(cursorRect)
    }

    Box(
        modifier = modifier
            .padding(vertical = 8.dp)
    ) {
        BasicTextField(
                value = value,
                onValueChange = onValueChange,
                visualTransformation = cachedVisualTransformation,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                        if (it.isFocused) onFocus()
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Backspace &&
                            keyEvent.type == KeyEventType.KeyDown) {
                            if (value.selection.start == 0 && value.selection.end == 0) {
                                onBackspaceAtStart()
                                return@onKeyEvent true
                            }
                        }
                        false
                    },
                onTextLayout = { result ->
                    textLayoutResult.value = result
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.text.isBlank()) {
                            Text(
                                text = "...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPageDialog(
    currentName: String,
    canDelete: Boolean,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
    canSplit: Boolean = false,
    onSplit: () -> Unit = {}
) {
    var newName by remember { mutableStateOf(currentName) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Page") },
            text = { Text("Are you sure you want to delete this page?") },
            confirmButton = {
                Button(
                    onClick = onDelete,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Page options") },
            text = { 
                Column {
                    TextField(value = newName, onValueChange = { newName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (canSplit) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onSplit, modifier = Modifier.fillMaxWidth()) {
                            Text("Split into multiple pages")
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { if (newName.isNotBlank()) onRename(newName) }) { Text("Rename") } },
            dismissButton = {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canDelete) {
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }
                }
            }
        )
    }
}
