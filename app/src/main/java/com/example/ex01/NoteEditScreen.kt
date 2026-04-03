package com.example.ex01
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val note by viewModel.getNote(noteId).collectAsStateWithLifecycle(initialValue = null)
    val items by viewModel.getItems(noteId).collectAsStateWithLifecycle(initialValue = emptyList())
    var noteResolved by remember(noteId) { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    val richTextController = rememberRichTextEditorController(TextFieldValue(""))
    var newItemText by remember { mutableStateOf(TextFieldValue("")) }
    val addItemFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var pendingAddItemReset by remember { mutableStateOf(false) }
    val showBodyEditor = note?.kind == NoteKinds.FREE_TEXT && note?.listStyle == NoteListStyles.CHECKLIST
    val submitItem = {
        val trimmedText = newItemText.text.trim()
        if (trimmedText.isNotEmpty()) {
            viewModel.addItem(noteId, trimmedText)
            pendingAddItemReset = true
        }
    }
    LaunchedEffect(pendingAddItemReset, noteId) {
        if (pendingAddItemReset) {
            withFrameNanos { }
            newItemText = TextFieldValue("", selection = TextRange(0))
            focusManager.clearFocus(force = true)
            addItemFocusRequester.requestFocus()
            keyboardController?.show()
            pendingAddItemReset = false
        }
    }
    LaunchedEffect(note?.id, note?.kind, note?.title, note?.body) {
        note?.let {
            noteResolved = true
            noteTitle = it.title
            val loadedBody = if (showBodyEditor) {
                withContext(Dispatchers.Default) {
                    collapseEmptyFormattingSpans(
                        normalizeRichTextMarkup(
                            TextFieldValue(it.body, selection = TextRange(it.body.length))
                        )
                    )
                }
            } else {
                TextFieldValue(it.body, selection = TextRange(it.body.length))
            }
            if (showBodyEditor) {
                richTextController.updateValue(loadedBody)
            }
        }
        if (showBodyEditor) {
            bodyFocusRequester.requestFocus()
        } else if (note != null) {
            addItemFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    LaunchedEffect(noteResolved, note) {
        if (noteResolved && note == null) {
            onBack()
        }
    }
    val saveAndBack: () -> Unit = {
        note?.let { currentNote ->
            scope.launch {
                val sanitizedBody = if (showBodyEditor) {
                    withContext(Dispatchers.Default) {
                        collapseEmptyFormattingSpans(
                            normalizeRichTextMarkup(richTextController.value),
                            preserveCollapsedSelectionSpan = false
                        ).text
                    }
                } else {
                    currentNote.body
                }
                if (noteTitle != currentNote.title) {
                    viewModel.updateNote(
                        currentNote.copy(
                            title = noteTitle,
                            body = sanitizedBody
                        )
                    )
                } else if (showBodyEditor && sanitizedBody != currentNote.body) {
                    viewModel.updateNote(currentNote.copy(body = sanitizedBody))
                }
                onBack()
            }
        } ?: onBack()
        Unit
    }
    BackHandler { saveAndBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = noteTitle.ifBlank { note?.title ?: "Untitled note" },
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = saveAndBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
                .imePadding()
        ) {
            if (showBodyEditor) {
                NoteWritingToolbar(
                    value = richTextController.value,
                    onBoldClick = richTextController::toggleBold,
                    onItalicClick = richTextController::toggleItalic,
                    onUnderlineClick = richTextController::toggleUnderline,
                    onStrikethroughClick = richTextController::toggleStrikethrough,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            TextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (showBodyEditor) {
                RichTextBodyEditor(
                    value = richTextController.value,
                    onValueChange = { next -> richTextController.updateValue(next) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(bodyFocusRequester)
                )
            } else {
                val displayItems = items.sortedByDescending { it.id }
                val isChecklist = note?.listStyle == NoteListStyles.CHECKLIST
                val isBulleted = note?.listStyle == NoteListStyles.BULLETED
                val isNumbered = note?.listStyle == NoteListStyles.NUMBERED
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = submitItem) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add item"
                        )
                    }
                    BasicTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { submitItem() }
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(addItemFocusRequester),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (newItemText.text.isBlank()) {
                                    Text(
                                        text = "Add item",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (displayItems.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(displayItems, key = { _, item -> item.id }) { index, item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                if (isChecklist) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { checked ->
                                            viewModel.updateItem(item.copy(isChecked = checked))
                                        }
                                    )
                                } else {
                                    val leadingLabel = when {
                                        isBulleted -> "•"
                                        isNumbered -> "${index + 1}."
                                        else -> null
                                    }
                                    if (leadingLabel != null) {
                                        Text(
                                            text = leadingLabel,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.widthIn(min = 18.dp).padding(end = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = item.text,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (isChecklist && item.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                    color = if (isChecklist && item.isChecked) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                IconButton(onClick = { viewModel.deleteItem(item) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Item")
                                }
                            }
                        }
                    }
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
    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = richTextVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
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
    }
}
