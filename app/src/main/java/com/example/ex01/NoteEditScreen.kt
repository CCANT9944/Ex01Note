package com.example.ex01

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteEditScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val note by viewModel.getNote(noteId).collectAsStateWithLifecycle(initialValue = null)
    val items by viewModel.getItems(noteId).collectAsStateWithLifecycle(initialValue = emptyList())
    val showBodyEditor = note?.kind == NoteKinds.FREE_TEXT && note?.listStyle == NoteListStyles.CHECKLIST

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
                .padding(top = 0.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ChecklistEditor(
                        items = items,
                        newItemText = newItemText,
                        onNewItemTextChange = { newItemText = it },
                        onAddItem = {
                            val trimmedText = newItemText.text.trim()
                            if (trimmedText.isNotEmpty()) {
                                viewModel.addItem(noteId, trimmedText)
                                newItemText = TextFieldValue("")
                            }
                        },
                        onToggleItem = { item, checked -> viewModel.updateItem(item.copy(isChecked = checked)) },
                        onEditItem = { item, nextText -> viewModel.updateItem(item.copy(text = nextText)) },
                        onDeleteItem = { item -> viewModel.deleteItem(item) }
                    )
                }
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
    items: List<NoteItem>,
    newItemText: TextFieldValue,
    onNewItemTextChange: (TextFieldValue) -> Unit,
    onAddItem: () -> Unit,
    onToggleItem: (NoteItem, Boolean) -> Unit,
    onEditItem: (NoteItem, String) -> Unit,
    onDeleteItem: (NoteItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { onToggleItem(item, it) }
                    )
                    TextField(
                        value = item.text,
                        onValueChange = { onEditItem(item, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
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

        Spacer(modifier = Modifier.height(12.dp))

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newItemText,
                onValueChange = onNewItemTextChange,
                placeholder = { Text("Add item") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.height(0.dp))
            IconButton(onClick = onAddItem) {
                Icon(Icons.Filled.Add, contentDescription = "Add item")
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
    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var resumeRevealTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val imeBottomPx = 1
    val imeBottomPadding = 24.dp

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeRevealTick++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isFocused, value.selection, textLayoutResult, imeBottomPx) {
        if (!isFocused || imeBottomPx == 0) return@LaunchedEffect
        val layoutResult = textLayoutResult ?: return@LaunchedEffect
        val cursorOffset = value.selection.end.coerceIn(0, value.text.length)
        val transformedCursorOffset = richTextVisualTransformation()
            .filter(AnnotatedString(value.text))
            .offsetMapping
            .originalToTransformed(cursorOffset)
            .coerceIn(0, layoutResult.layoutInput.text.text.length)
        bringIntoViewRequester.bringIntoView(layoutResult.getCursorRect(transformedCursorOffset))
    }

    LaunchedEffect(resumeRevealTick, isFocused, textLayoutResult, imeBottomPx) {
        if (!isFocused || imeBottomPx == 0) return@LaunchedEffect
        val layoutResult = textLayoutResult ?: return@LaunchedEffect
        withFrameNanos { }
        val cursorOffset = value.selection.end.coerceIn(0, value.text.length)
        val transformedCursorOffset = richTextVisualTransformation()
            .filter(AnnotatedString(value.text))
            .offsetMapping
            .originalToTransformed(cursorOffset)
            .coerceIn(0, layoutResult.layoutInput.text.text.length)
        bringIntoViewRequester.bringIntoView(layoutResult.getCursorRect(transformedCursorOffset))
    }

    Column(modifier = modifier.verticalScroll(scrollState)) {
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
        Spacer(modifier = Modifier.height(imeBottomPadding))
    }
}
