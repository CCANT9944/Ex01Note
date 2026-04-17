package com.example.ex01.ui.screens

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.editor.snote.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NoteEditScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val note by viewModel.getNote(noteId).collectAsStateWithLifecycle(initialValue = null)
    val items by viewModel.getItems(noteId).collectAsStateWithLifecycle(initialValue = emptyList())
    val showBodyEditor = note?.kind == NoteKinds.FREE_TEXT

    var isTransitionFinished by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isTransitionFinished = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Fallback timer just in case ON_RESUME doesn't fire right (e.g. from Widget launch edge-cases)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400) // Match the 400ms enter transition animation exactly
        isTransitionFinished = true
    }

    var noteResolved by remember(noteId) { mutableStateOf(false) }
    var draftInitialized by rememberSaveable(noteId) { mutableStateOf(false) }
    var noteTitle by rememberSaveable(noteId) { mutableStateOf("") }
    var serializedPagesBody by rememberSaveable(noteId) { mutableStateOf("") }
    var selectedPageIndex by rememberSaveable(noteId) { mutableIntStateOf(0) }
    val pageControllers = remember(noteId) { mutableMapOf<Int, RichTextEditorController>() }
    var newItemText by rememberSaveable(noteId) { mutableStateOf("") }

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

    val saveNote = {
        if (!isSaving && note != null) {
            val currentNote = note!!
            val updatedNote = currentNote.copy(
                title = noteTitle,
                body = if (currentNote.kind == NoteKinds.FREE_TEXT || currentNote.kind == NoteKinds.SNOTE) serializedPagesBody else currentNote.body
            )

            // Smart Render Culling: Only save and trigger flow emissions if the note ACTUALLY changed.
            // This prevents massive recomposition spikes on the Home screen during the exit animation.
            if (updatedNote != currentNote) {
                isSaving = true
                @Suppress("OPT_IN_USAGE")
                GlobalScope.launch(Dispatchers.IO) {
                    viewModel.updateNoteSync(updatedNote)
                    try {
                        viewModel.triggerWidgetUpdate()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val handleBack: () -> Unit = {
        // Just trigger standard back navigation immediately.
        // The save and database work will safely execute on the background lifecycle stop.
        onBack()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Use ON_STOP to flush data to DB. Works without lagging the exit animation over on ON_PAUSE!
            if (event == Lifecycle.Event.ON_STOP) {
                saveNote()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            saveNote()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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

            if (!isTransitionFinished && (note?.kind == NoteKinds.SNOTE || showBodyEditor)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (note?.kind == NoteKinds.SNOTE) {
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
