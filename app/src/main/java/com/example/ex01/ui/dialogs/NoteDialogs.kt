package com.example.ex01.ui.dialogs

import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.window.*
import androidx.lifecycle.compose.*
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.theme.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.editor.snote.*
import kotlinx.coroutines.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteActionsDialog(
    note: Note,
    onRename: () -> Unit,
    onChangeStyle: (() -> Unit)?,
    onMoveToFolder: () -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(note.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRename) { Text("Rename") }
                if (onChangeStyle != null) {
                    TextButton(onClick = onChangeStyle) { Text("Change style") }
                }
                TextButton(onClick = onMoveToFolder) { Text("Move to folder") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePreviewDialog(
    note: Note,
    viewModel: NoteViewModel,
    onEdit: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val items by viewModel.getItems(note.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val previewItems = remember(items) { items.sortedBy { it.id } }
    val showBodyPreview = note.kind == NoteKinds.FREE_TEXT

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = onEdit) {
                        Text("Edit")
                    }

                    TextButton(onClick = onDismissRequest) {
                        Text("Close")
                    }
                }

                if (showBodyPreview) {
                    Text(
                        text = "Note preview",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val previewBody = notePageBody(note.body, 0).trim()
                        if (previewBody.isBlank()) {
                            Text(
                                text = "No text yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                                            val renderedBody by produceState<AnnotatedString?>(initialValue = null, previewBody) {
                                                value = withContext(Dispatchers.Default) {
                                                    renderRichTextMarkup(previewBody)
                                                }
                                            }
                            Text(
                                                text = renderedBody ?: AnnotatedString(previewBody),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = when (note.listStyle) {
                            NoteListStyles.BULLETED -> "Bulleted preview"
                            NoteListStyles.NUMBERED -> "Numbered preview"
                            else -> "Checklist preview"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (previewItems.isEmpty()) {
                        Text(
                            text = "No items yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 560.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(previewItems, key = { _, item -> item.id }) { index, item ->
                                NoteItemRow(
                                    item = item,
                                    onCheckedChange = null,
                                    onDeleteClick = null,
                                    showDeleteButton = false,
                                    showCheckbox = note.listStyle == NoteListStyles.CHECKLIST,
                                    leadingLabel = when (note.listStyle) {
                                        NoteListStyles.BULLETED -> "•"
                                        NoteListStyles.NUMBERED -> "${index + 1}."
                                        else -> null
                                    },
                                    compact = false,
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeListStyleDialog(
    title: String,
    currentStyle: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var selectedStyle by remember(currentStyle) { mutableStateOf(currentStyle) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "List style", style = MaterialTheme.typography.labelLarge)

                ListStyleOptionRow(
                    label = "Checklist",
                    description = "Tick boxes",
                    selected = selectedStyle == NoteListStyles.CHECKLIST,
                    onClick = { selectedStyle = NoteListStyles.CHECKLIST }
                )
                ListStyleOptionRow(
                    label = "Bulleted",
                    description = "• Item one",
                    selected = selectedStyle == NoteListStyles.BULLETED,
                    onClick = { selectedStyle = NoteListStyles.BULLETED }
                )
                ListStyleOptionRow(
                    label = "Numbered",
                    description = "1. Item one",
                    selected = selectedStyle == NoteListStyles.NUMBERED,
                    onClick = { selectedStyle = NoteListStyles.NUMBERED }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedStyle) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } }
    )
}

@Composable
fun ListStyleOptionRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

