package com.example.ex01

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteEditScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val note by viewModel.getNote(noteId).collectAsStateWithLifecycle(initialValue = null)
    val items by viewModel.getItems(noteId).collectAsStateWithLifecycle(initialValue = emptyList())
    val showBodyEditor = note?.kind == NoteKinds.FREE_TEXT

    var noteResolved by remember(noteId) { mutableStateOf(false) }
    var draftInitialized by rememberSaveable(noteId) { mutableStateOf(false) }
    var noteTitle by rememberSaveable(noteId) { mutableStateOf("") }
    var serializedPagesBody by rememberSaveable(noteId) { mutableStateOf("") }
    var selectedPageIndex by rememberSaveable(noteId) { mutableIntStateOf(0) }
    val pageControllers = remember(noteId) { mutableMapOf<Int, RichTextEditorController>() }
    var newItemText by rememberSaveable(noteId, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(note?.id, note?.title, note?.body, showBodyEditor) {
        val currentNote = note
        if (currentNote != null) {
            noteResolved = true
            if (!draftInitialized) {
                noteTitle = currentNote.title
                serializedPagesBody = currentNote.body
                selectedPageIndex = 0
                pageControllers.clear()
                draftInitialized = true
            }
        }
    }

    fun persistDraft() {
        val currentNote = note ?: return
        val updatedNote = currentNote.copy(
            title = noteTitle,
            body = if (showBodyEditor) serializedPagesBody else currentNote.body
        )
        viewModel.updateNote(updatedNote)
    }

    BackHandler(enabled = note != null) {
        persistDraft()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteResolved) noteTitle.ifBlank { "Untitled note" } else "Loading note…",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        persistDraft()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (note == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (noteResolved) {
                    Text(
                        text = "This note could not be loaded.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            verticalArrangement = Arrangement.Top
        ) {

            if (showBodyEditor) {
                PageBodyEditor(
                    serializedPagesBody = serializedPagesBody,
                    selectedPageIndex = selectedPageIndex,
                    pageControllers = pageControllers,
                    onSelectedPageIndexChange = { selectedPageIndex = it },
                    onSerializedPagesBodyChange = { serializedPagesBody = it }
                )
            } else {
                ChecklistEditor(
                    listStyle = note?.listStyle ?: NoteListStyles.CHECKLIST,
                    items = items,
                    newItemText = newItemText,
                    onNewItemTextChange = { newItemText = it },
                    onAddItem = {
                        val trimmedText = newItemText.text.trim()
                        if (trimmedText.isNotEmpty()) {
                            viewModel.addItem(noteId, trimmedText)
                            newItemText = TextFieldValue("")
                            true
                        } else {
                            false
                        }
                    },
                    onToggleItem = { item, checked -> viewModel.updateItem(item.copy(isChecked = checked)) },
                    onEditItem = { item, nextText -> viewModel.updateItem(item.copy(text = nextText)) },
                    onDeleteItem = { item -> viewModel.deleteItem(item) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageBodyEditor(
    serializedPagesBody: String,
    selectedPageIndex: Int,
    pageControllers: MutableMap<Int, RichTextEditorController>,
    onSelectedPageIndexChange: (Int) -> Unit,
    onSerializedPagesBodyChange: (String) -> Unit,
) {
    val pageBodies = splitNotePages(serializedPagesBody)
    val safePageIndex = selectedPageIndex.coerceIn(0, pageBodies.lastIndex)
    val pagerState = rememberPagerState(initialPage = safePageIndex, pageCount = { pageBodies.size })

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
                itemsIndexed(pageBodies) { index, pageBody ->
                    val selected = index == safePageIndex

                    if (selected) {
                        Button(onClick = { onSelectedPageIndexChange(index) }) {
                            Text("Page ${index + 1}")
                        }
                    } else {
                        OutlinedButton(onClick = { onSelectedPageIndexChange(index) }) {
                            Text("Page ${index + 1}")
                        }
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

            HorizontalPager(
                state = pagerState,
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

@Composable
private fun ChecklistEditor(
    listStyle: String,
    items: List<NoteItem>,
    newItemText: TextFieldValue,
    onNewItemTextChange: (TextFieldValue) -> Unit,
    onAddItem: () -> Boolean,
    onToggleItem: (NoteItem, Boolean) -> Unit,
    onEditItem: (NoteItem, String) -> Unit,
    onDeleteItem: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val addItemFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var addItemIsFocused by remember { mutableStateOf(false) }
    
    val imeInsets = WindowInsets.ime
    val imeBottomPx = imeInsets.getBottom(density)
    
    var revealChecklistEndRequested by remember { mutableStateOf(false) }
    var pendingAddedItemCount by remember { mutableIntStateOf(-1) }

    fun submitNewItem() {
        val added = onAddItem()
        if (added) {
            pendingAddedItemCount = items.size + 1
        }
        revealChecklistEndRequested = true
    }

    LaunchedEffect(imeBottomPx, addItemIsFocused, items.size, pendingAddedItemCount, revealChecklistEndRequested) {
        if (!revealChecklistEndRequested || !addItemIsFocused || imeBottomPx <= 0) {
            return@LaunchedEffect
        }

        if (pendingAddedItemCount != -1 && items.size < pendingAddedItemCount) {
            return@LaunchedEffect
        }

        if (items.isNotEmpty()) {
            withFrameNanos { }
            listState.animateScrollToItem(items.lastIndex)
        }

        pendingAddedItemCount = -1
        revealChecklistEndRequested = false
    }

    LaunchedEffect(imeBottomPx, addItemIsFocused) {
        if (addItemIsFocused && imeBottomPx > 0) {
            revealChecklistEndRequested = true
        }
    }

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    val rowBringIntoViewRequester = remember(item.id) { BringIntoViewRequester() }

                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var rowIsFocused by remember(item.id) { mutableStateOf(false) }

                        LaunchedEffect(rowIsFocused) {
                            if (rowIsFocused) {
                                withFrameNanos { }
                                rowBringIntoViewRequester.bringIntoView()
                            }
                        }

                        if (listStyle == NoteListStyles.CHECKLIST) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { onToggleItem(item, it) }
                            )
                        } else {
                            val leadingLabel = when (listStyle) {
                                NoteListStyles.BULLETED -> "•"
                                NoteListStyles.NUMBERED -> "${index + 1}."
                                else -> ""
                            }
                            Text(
                                text = leadingLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        
                        var textValue by remember(item.id) { mutableStateOf(TextFieldValue(item.text)) }
                        LaunchedEffect(item.text) {
                            if (textValue.text != item.text) {
                                textValue = textValue.copy(text = item.text)
                            }
                        }

                        TextField(
                            value = textValue,
                            onValueChange = { 
                                textValue = it
                                onEditItem(item, it.text)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .bringIntoViewRequester(rowBringIntoViewRequester)
                                .onFocusChanged { rowIsFocused = it.isFocused },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (listStyle == NoteListStyles.CHECKLIST && item.isChecked) TextDecoration.LineThrough else null,
                                color = if (listStyle == NoteListStyles.CHECKLIST && item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            )
                        )
                        IconButton(onClick = { onDeleteItem(item) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete item")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newItemText,
                    onValueChange = onNewItemTextChange,
                    placeholder = { Text("Add item") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(addItemFocusRequester)
                        .onFocusChanged { focusState ->
                            val isFocused = focusState.isFocused
                            if (isFocused && !addItemIsFocused) {
                                revealChecklistEndRequested = true
                            }
                            addItemIsFocused = isFocused
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitNewItem() }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    )
                )
                IconButton(onClick = { submitNewItem() }, modifier = Modifier.focusProperties { canFocus = false }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add item")
                }
            }
        }
    }
}

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
        bringIntoViewRequester.bringIntoView(cursorRect.inflate(bottomRevealMarginPx))
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
