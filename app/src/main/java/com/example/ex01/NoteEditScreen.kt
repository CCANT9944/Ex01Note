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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.appwidget.AppWidgetManager
import androidx.glance.appwidget.updateAll
import com.example.ex01.widget.NotesWidget
import com.example.ex01.widget.NotesWidgetReceiver
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.ComponentName

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
    var newItemText by rememberSaveable(noteId) { mutableStateOf("") }

    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

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

    val handleBack: () -> Unit = {
        if (!isSaving && note != null) {
            isSaving = true
            scope.launch {
                val currentNote = note!!
                val updatedNote = currentNote.copy(
                    title = noteTitle,
                    body = if (currentNote.kind == NoteKinds.FREE_TEXT || currentNote.kind == NoteKinds.SNOTE) serializedPagesBody else currentNote.body
                )
                val appContext = context.applicationContext
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    viewModel.updateNoteSync(updatedNote)
                    try {
                        viewModel.triggerWidgetUpdate()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                onBack()
            }
        } else if (note == null) {
            onBack()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (!isSaving) {
                    val currentNote = note ?: return@LifecycleEventObserver
                    val updatedNote = currentNote.copy(
                        title = noteTitle,
                        body = if (currentNote.kind == NoteKinds.FREE_TEXT || currentNote.kind == NoteKinds.SNOTE) serializedPagesBody else currentNote.body
                    )
                    @Suppress("OPT_IN_USAGE")
                    GlobalScope.launch(Dispatchers.IO) {
                        viewModel.updateNoteSync(updatedNote)
                        try {
                            viewModel.triggerWidgetUpdate()
                        } catch (e: Exception) {}
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = note != null) {
        handleBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteResolved) noteTitle.ifBlank { "Untitled note" } else "Loading note",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
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

            if (note?.kind == NoteKinds.SNOTE) {
                SNoteEditor(
                    serializedBody = serializedPagesBody,
                    onSerializedBodyChange = { serializedPagesBody = it }
                )
            } else if (showBodyEditor) {
                PageBodyEditor(
                    serializedPagesBody = serializedPagesBody,
                    onSerializedPagesBodyChange = { serializedPagesBody = it },
                    selectedPageIndex = selectedPageIndex,
                    onSelectedPageIndexChange = { selectedPageIndex = it },
                    pageControllers = pageControllers
                )
            } else {
                ChecklistEditor(
                    listStyle = note?.listStyle ?: NoteListStyles.CHECKLIST,
                    items = items,
                    newItemText = newItemText,
                    onNewItemTextChange = { newItemText = it },
                    onAddItem = {
                        val trimmedText = newItemText.trim()
                        if (trimmedText.isNotEmpty()) {
                            @Suppress("OPT_IN_USAGE")
                            GlobalScope.launch(Dispatchers.IO) {
                                viewModel.addItemSync(noteId, trimmedText)
                                viewModel.triggerWidgetUpdate()
                            }
                            newItemText = ""
                            true
                        } else {
                            false
                        }
                    },
                    onToggleItem = { item, checked -> 
                        @Suppress("OPT_IN_USAGE")
                        GlobalScope.launch(Dispatchers.IO) {
                            viewModel.updateItemSync(item.copy(isChecked = checked))
                            viewModel.triggerWidgetUpdate()
                        }
                    },
                    onEditItem = { item, nextText -> 
                        @Suppress("OPT_IN_USAGE")
                        GlobalScope.launch(Dispatchers.IO) {
                            viewModel.updateItemSync(item.copy(text = nextText))
                            viewModel.triggerWidgetUpdate()
                        }
                    },
                    onDeleteItem = { item -> 
                        @Suppress("OPT_IN_USAGE")
                        GlobalScope.launch(Dispatchers.IO) {
                            viewModel.deleteItemSync(item)
                            viewModel.triggerWidgetUpdate()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}
