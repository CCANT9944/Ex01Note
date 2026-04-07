package com.example.ex01

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PageBodyEditor(
    serializedPagesBody: String,
    selectedPageIndex: Int,
    pageControllers: MutableMap<Int, RichTextEditorController>,
    onSelectedPageIndexChange: (Int) -> Unit,
    onSerializedPagesBodyChange: (String) -> Unit,
) {
    val pageItems = splitNotePages(serializedPagesBody).mapIndexed { i, p -> parseNotePage(p, i) }
    val pageBodies = pageItems.map { it.body }
    val safePageIndex = selectedPageIndex.coerceIn(0, pageBodies.lastIndex)
    val pagerState = rememberPagerState(initialPage = safePageIndex, pageCount = { pageBodies.size })
    var editingPageIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(pageBodies.size) {
        if (selectedPageIndex > pageBodies.lastIndex) {
            onSelectedPageIndexChange(pageBodies.lastIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedPageIndex) {
            onSelectedPageIndexChange(pagerState.currentPage)
        }
    }

    LaunchedEffect(selectedPageIndex, pageBodies.size) {
        val targetPage = selectedPageIndex.coerceIn(0, pageBodies.lastIndex)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val activeController = pageControllers.getOrPut(safePageIndex) {
        RichTextEditorController(
            TextFieldValue(
                pageBodies[safePageIndex],
                selection = TextRange(pageBodies[safePageIndex].length)
            )
        )
    }

    fun TextRange.coerceToTextLength(textLength: Int): TextRange {
        return TextRange(
            start = start.coerceIn(0, textLength),
            end = end.coerceIn(0, textLength)
        )
    }

    LaunchedEffect(pageBodies[safePageIndex]) {
        val pageText = pageBodies[safePageIndex]
        if (activeController.value.text != pageText) {
            activeController.replaceValue(
                TextFieldValue(
                    pageText,
                    selection = activeController.value.selection.coerceToTextLength(pageText.length)
                )
            )
        }
    }

    fun commitActivePage() {
        onSerializedPagesBodyChange(
            replaceNotePage(serializedPagesBody, safePageIndex, activeController.value.text)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoteWritingToolbar(
            value = activeController.value,
            canUndo = activeController.canUndo,
            onUndoClick = {
                if (activeController.undo()) {
                    commitActivePage()
                }
            },
            onBoldClick = {
                activeController.toggleBold()
                commitActivePage()
            },
            onItalicClick = {
                activeController.toggleItalic()
                commitActivePage()
            },
            onUnderlineClick = {
                activeController.toggleUnderline()
                commitActivePage()
            },
            onStrikethroughClick = {
                activeController.toggleStrikethrough()
                commitActivePage()
            },
            onBulletClick = {
                activeController.toggleBullet()
                commitActivePage()
            },
            onIndentClick = {
                activeController.indent()
                commitActivePage()
            },
            onOutdentClick = {
                activeController.outdent()
                commitActivePage()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(pageItems) { index, page ->
                    val selected = index == safePageIndex

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.combinedClickable(
                            onClick = { onSelectedPageIndexChange(index) },
                            onLongClick = { editingPageIndex = index }
                        )
                    ) {
                        Text(
                            text = "${if (page.name.isBlank()) "Page" else page.name} (${index + 1})",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                item {
                    OutlinedButton(onClick = {
                        onSerializedPagesBodyChange(appendNotePage(serializedPagesBody))
                        onSelectedPageIndexChange(pageBodies.size)
                    }) {
                        Text("+ Add page")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (editingPageIndex != null) {
                val indexToEdit = editingPageIndex!!
                EditPageDialog(
                    currentName = pageItems[indexToEdit].name,
                    canDelete = pageItems.size > 1,
                    onRename = { newName ->
                        onSerializedPagesBodyChange(renameNotePage(serializedPagesBody, indexToEdit, newName))
                        editingPageIndex = null
                    },
                    onDelete = {
                        onSerializedPagesBodyChange(deleteNotePage(serializedPagesBody, indexToEdit))
                        if (selectedPageIndex > 0 && selectedPageIndex >= indexToEdit) {
                            onSelectedPageIndexChange(selectedPageIndex - 1)
                        }
                        editingPageIndex = null
                    },
                    onDismissRequest = { editingPageIndex = null }
                )
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val pageText = pageBodies[pageIndex]
                val pageController = pageControllers.getOrPut(pageIndex) {
                    RichTextEditorController(
                        TextFieldValue(pageText, selection = TextRange(pageText.length))
                    )
                }

                LaunchedEffect(pageText) {
                    if (pageController.value.text != pageText) {
                        pageController.replaceValue(
                            TextFieldValue(
                                pageText,
                                selection = pageController.value.selection.coerceToTextLength(pageText.length)
                            )
                        )
                    }
                }

                RichTextBodyEditor(
                    value = pageController.value,
                    onValueChange = { next ->
                        pageController.updateValue(next)
                        onSerializedPagesBodyChange(replaceNotePage(serializedPagesBody, pageIndex, next.text))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RichTextBodyEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomRevealMargin = 96.dp
    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var resumeRevealTick by remember { mutableIntStateOf(0) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val imeInsets = WindowInsets.ime
    val imeBottomPx = imeInsets.getBottom(density)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeRevealTick++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    suspend fun revealCursor() {
        val layoutResult = textLayoutResult ?: return
        val cursorOffset = value.selection.end.coerceIn(0, value.text.length)
        val transformedCursorOffset = richTextVisualTransformation()
            .filter(AnnotatedString(value.text))
            .offsetMapping
            .originalToTransformed(cursorOffset)
            .coerceIn(0, layoutResult.layoutInput.text.text.length)
        val cursorRect = layoutResult.getCursorRect(transformedCursorOffset)
        val bottomRevealMarginPx = with(density) { bottomRevealMargin.toPx() }
        val visibleHeightPx = (viewportHeightPx - imeBottomPx).coerceAtLeast(0)
        if (visibleHeightPx > 0) {
            val targetScroll = (cursorRect.bottom - visibleHeightPx + bottomRevealMarginPx).toInt()
                .coerceAtLeast(0)
            if (targetScroll != scrollState.value) {
                scrollState.animateScrollTo(targetScroll.coerceAtMost(scrollState.maxValue))
            }
        }
        val targetRect = androidx.compose.ui.geometry.Rect(
            left = cursorRect.left,
            top = cursorRect.top,
            right = cursorRect.right,
            bottom = cursorRect.bottom + bottomRevealMarginPx
        )
        bringIntoViewRequester.bringIntoView(targetRect)
    }

    LaunchedEffect(isFocused, value.selection, textLayoutResult) {
        if (!isFocused) return@LaunchedEffect
        withFrameNanos { }
        revealCursor()
    }

    LaunchedEffect(resumeRevealTick, isFocused, textLayoutResult, imeBottomPx) {
        if (!isFocused || imeBottomPx == 0) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        revealCursor()
    }

    Column(
        modifier = modifier
            .onSizeChanged { viewportHeightPx = it.height }
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = richTextVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged {
                    if (it.isFocused && !isFocused) {
                        resumeRevealTick++
                    }
                    isFocused = it.isFocused
                },
            onTextLayout = { textLayoutResult = it },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.text.isBlank()) {
                        Text(
                            text = "Write your note",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(
            modifier = Modifier.height(
                bottomRevealMargin + with(density) { imeBottomPx.toDp() }
            )
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
    onDismissRequest: () -> Unit
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
            text = { TextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
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
