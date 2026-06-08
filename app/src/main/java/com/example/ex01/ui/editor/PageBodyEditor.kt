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
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PageBodyEditor(
    serializedPagesBody: String,
    selectedPageIndex: Int,
    pageControllers: MutableMap<Int, RichTextEditorController>,
    onSelectedPageIndexChange: (Int) -> Unit,
    onSerializedPagesBodyChange: (String) -> Unit,
) {
    val activeController = pageControllers.getOrPut(0) {
        RichTextEditorController(
            TextFieldValue(serializedPagesBody, selection = TextRange(serializedPagesBody.length))
        )
    }

    LaunchedEffect(serializedPagesBody) {
        if (activeController.value.text != serializedPagesBody) {
            activeController.replaceValue(
                TextFieldValue(
                    serializedPagesBody,
                    selection = activeController.value.selection
                )
            )
        }
    }

    fun commit() {
        val newBody = activeController.value.text
        if (newBody != serializedPagesBody) {
            onSerializedPagesBodyChange(newBody)
        }
    }

    DisposableEffect(Unit) {
        onDispose { commit() }
    }

    LaunchedEffect(activeController.value.text) {
        kotlinx.coroutines.delay(400)
        commit()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoteWritingToolbar(
            value = activeController.value,
            canUndo = activeController.canUndo,
            onUndoClick = {
                if (activeController.undo()) commit()
            },
            onBoldClick = {
                activeController.toggleBold()
                commit()
            },
            onItalicClick = {
                activeController.toggleItalic()
                commit()
            },
            onUnderlineClick = {
                activeController.toggleUnderline()
                commit()
            },
            onStrikethroughClick = {
                activeController.toggleStrikethrough()
                commit()
            },
            onBulletClick = {
                activeController.toggleBullet()
                commit()
            },
            onIndentClick = {
                activeController.indent()
                commit()
            },
            onOutdentClick = {
                activeController.outdent()
                commit()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            val scrollState = androidx.compose.foundation.rememberScrollState()
            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val view = androidx.compose.ui.platform.LocalView.current
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            val textLayoutResult = remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
            val density = androidx.compose.ui.platform.LocalDensity.current

            var viewportHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val insets = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets)
                            val isKeyboardOpen = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())
                            
                            val absoluteTapY = offset.y + scrollState.value
                            val doInsert = { shrinkedViewHeight: Int, capturedTapY: Float ->
                                val layout = textLayoutResult.value
                                var newText = activeController.value.text
                                
                                if (layout != null) {
                                    val textHeight = layout.size.height
                                    val maxVisibleY = shrinkedViewHeight + scrollState.value - with(density) { 60.dp.toPx() }
                                    val targetY = maxVisibleY
                                    
                                    if (targetY > textHeight) {
                                        val lineCount = layout.lineCount
                                        val lastLineHeight = if (lineCount > 0) {
                                            layout.getLineBottom(lineCount - 1) - layout.getLineTop(lineCount - 1)
                                        } else {
                                            with(density) { 24.dp.toPx() }
                                        }
                                        
                                        if (lastLineHeight > 0) {
                                            val gap = targetY - textHeight
                                            val newLinesCount = (gap / lastLineHeight).toInt()
                                            if (newLinesCount > 0) {
                                                newText += "\n".repeat(newLinesCount)
                                            }
                                        }
                                    }
                                }
                                
                                activeController.updateValue(
                                    activeController.value.copy(
                                        text = newText,
                                        selection = TextRange(newText.length)
                                    )
                                )
                            }
                            
                            if (!isKeyboardOpen) {
                                focusManager.clearFocus()
                                focusRequester.requestFocus()
                                keyboardController?.show()
                                coroutineScope.launch {
                                    val initialViewportHeight = viewportHeightPx
                                    val initialIme = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets).getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
                                    var iterations = 0
                                    
                                    // 1. Wait up to 3 seconds for IME to start opening
                                    var ime = initialIme
                                    while (ime <= initialIme + 10 && iterations < 60) {
                                        kotlinx.coroutines.delay(50)
                                        iterations++
                                        ime = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets).getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
                                    }
                                    
                                    if (ime > initialIme + 10) {
                                        // 2. IME started opening! Wait for it to stabilize
                                        var currentIme = ime
                                        var stableCount = 0
                                        while (stableCount < 3) {
                                            kotlinx.coroutines.delay(50)
                                            val newIme = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets).getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
                                            if (newIme == currentIme) {
                                                stableCount++
                                            } else {
                                                currentIme = newIme
                                                stableCount = 0
                                            }
                                        }
                                        
                                        // 3. Wait up to 1 second for Compose to update viewportHeightPx
                                        val startWait = System.currentTimeMillis()
                                        while (viewportHeightPx >= initialViewportHeight - 50 && System.currentTimeMillis() - startWait < 1000) {
                                            kotlinx.coroutines.delay(50)
                                        }
                                        kotlinx.coroutines.delay(100)
                                    }
                                    
                                    doInsert(viewportHeightPx, absoluteTapY)
                                }
                            } else {
                                doInsert(viewportHeightPx, absoluteTapY)
                            }
                        }
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize().onSizeChanged { viewportHeightPx = it.height }) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        RichTextBodyEditor(
                            focusRequester = focusRequester,
                            value = activeController.value,
                            onValueChange = { next ->
                                activeController.updateValue(next)
                            },
                            onFocus = {
                                onSelectedPageIndexChange(0)
                            },
                            onBackspaceAtStart = {},
                            modifier = Modifier.fillMaxWidth(),
                            textLayoutResult = textLayoutResult
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
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
    modifier: Modifier = Modifier,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    textLayoutResult: androidx.compose.runtime.MutableState<androidx.compose.ui.text.TextLayoutResult?>
) {
    var isFocused by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val cachedVisualTransformation = remember { richTextVisualTransformation() }
    val imeBottom = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)

    LaunchedEffect(isFocused, value.selection, imeBottom) {
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
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
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
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp)) {
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
