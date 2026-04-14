package com.example.ex01.ui.editor

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChecklistEditor(
    listStyle: String,
    items: List<NoteItem>,
    newItemText: String,
    onNewItemTextChange: (String) -> Unit,
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
            onNewItemTextChange("")
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

                    Row(
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
            Row(
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
