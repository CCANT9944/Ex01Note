package com.example.ex01.ui.dialogs

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderActionsDialog(
    folder: Folder,
    isCollapsed: Boolean,
    onExpandCollapse: () -> Unit,
    onRename: () -> Unit,
    onMoveToFolder: () -> Unit,
    onChangeColor: () -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(folder.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRename) { Text("Rename") }
                TextButton(onClick = onMoveToFolder) { Text("Move to folder") }
                TextButton(onClick = onChangeColor) { Text("Change icon color") }
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
fun FolderColorDialog(
    folderName: String,
    currentColor: Long?,
    onColorSelected: (Long?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val palette = listOf<Long?>(
        null,
        0xFFE57373,
        0xFFBA68C8,
        0xFF64B5F6,
        0xFF4DB6AC,
        0xFFFFB74D,
        0xFFA1887F,
        0xFF90A4AE
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Folder icon color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Choose a color for $folderName")
                palette.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { colorValue ->
                            val selected = colorValue == currentColor
                            val swatchColor = colorValue?.let(::Color) ?: MaterialTheme.colorScheme.surfaceVariant
                            Surface(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        onColorSelected(colorValue)
                                        onDismissRequest()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = swatchColor,
                                border = BorderStroke(
                                    2.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    if (colorValue == null) {
                                        Text("Default", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { TextField(value = value, onValueChange = onValueChange) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } }
    )
}

@Composable
fun CreateItemDialogs(
    showFolderDialog: Boolean,
    folderDialogTitle: String,
    onCreateFolder: (String) -> Unit,
    onDismissFolderDialog: () -> Unit,
    showNoteDialog: Boolean,
    noteDialogTitle: String,
    onCreateNote: (String) -> Unit,
    onDismissNoteDialog: () -> Unit,
    showSNoteDialog: Boolean,
    sNoteDialogTitle: String,
    onCreateSNote: (String) -> Unit,
    onDismissSNoteDialog: () -> Unit,
    showListDialog: Boolean,
    listDialogTitle: String,
    onCreateList: (String, String) -> Unit,
    onDismissListDialog: () -> Unit,
) {
    if (showFolderDialog) {
        var name by remember { mutableStateOf("") }
        TextInputDialog(
            title = folderDialogTitle,
            value = name,
            onValueChange = { name = it },
            confirmText = "Create",
            onConfirm = {
                if (name.isNotBlank()) {
                    onCreateFolder(name)
                    onDismissFolderDialog()
                }
            },
            onDismissRequest = onDismissFolderDialog
        )
    }

    if (showNoteDialog) {
        var title by remember { mutableStateOf("") }
        TextInputDialog(
            title = noteDialogTitle,
            value = title,
            onValueChange = { title = it },
            confirmText = "Create",
            onConfirm = {
                if (title.isNotBlank()) {
                    onCreateNote(title)
                    onDismissNoteDialog()
                }
            },
            onDismissRequest = onDismissNoteDialog
        )
    }

    if (showSNoteDialog) {
        var title by remember { mutableStateOf("") }
        TextInputDialog(
            title = sNoteDialogTitle,
            value = title,
            onValueChange = { title = it },
            confirmText = "Create",
            onConfirm = {
                if (title.isNotBlank()) {
                    onCreateSNote(title)
                    onDismissSNoteDialog()
                }
            },
            onDismissRequest = onDismissSNoteDialog
        )
    }

    if (showListDialog) {
        CreateListDialog(
            title = listDialogTitle,
            onConfirm = { listTitle, listStyle ->
                onCreateList(listTitle, listStyle)
                onDismissListDialog()
            },
            onDismissRequest = onDismissListDialog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListDialog(
    title: String,
    onConfirm: (String, String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var listTitle by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(NoteListStyles.CHECKLIST) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = listTitle,
                    onValueChange = { listTitle = it },
                    placeholder = { Text("List title") },
                    singleLine = true
                )

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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedTitle = listTitle.trim()
                    if (trimmedTitle.isNotBlank()) {
                        onConfirm(trimmedTitle, selectedStyle)
                    }
                }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } }
    )
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

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    confirmButtonColors: ButtonColors = ButtonDefaults.buttonColors()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = confirmButtonColors
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } }
    )
}

data class FolderTreeEntry(
    val folder: Folder,
    val depth: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderDialog(
    itemLabel: String,
    folders: List<Folder>,
    excludedFolderIds: Set<Int>,
    onFolderSelected: (Folder?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val treeRows = remember(folders, excludedFolderIds) {
        buildFolderTreeRows(folders, excludedFolderIds)
    }
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val containerColor = if (isLightTheme) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    }
    val rowColor = if (isLightTheme) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val rowBorderColor = if (isLightTheme) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Move \"$itemLabel\" to folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = containerColor,
                    border = BorderStroke(1.dp, rowBorderColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFolderSelected(null) }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Main screen",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (treeRows.isEmpty()) {
                    Text("No subfolders available.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(treeRows, key = { it.folder.id }) { row ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = (row.depth * 10).dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isLightTheme) rowColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (row.depth == 0) 0.42f else 0.3f),
                                border = BorderStroke(1.dp, rowBorderColor)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFolderSelected(row.folder) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    FolderLabelStack(
                                        name = row.folder.name,
                                        modifier = Modifier.fillMaxWidth(),
                                        iconSize = if (row.depth == 0) 16.dp else 12.dp,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        textColor = MaterialTheme.colorScheme.onSurface,
                                        iconAlpha = if (row.depth == 0) 0.95f else 0.7f,
                                        horizontalAlignment = Alignment.Start,
                                        spacing = 2.dp,
                                        fontWeight = if (row.depth == 0) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Cancel") } }
    )
}

fun buildFolderTreeRows(
    folders: List<Folder>,
    excludedFolderIds: Set<Int>
): List<FolderTreeEntry> {
    val childrenByParent = folders
        .filterNot { it.id in excludedFolderIds }
        .groupBy { it.parentFolderId }
        .mapValues { entry -> entry.value.sortedBy { it.name.lowercase() } }

    val rows = mutableListOf<FolderTreeEntry>()

    fun visit(parentId: Int?, depth: Int) {
        childrenByParent[parentId].orEmpty().forEach { folder ->
            rows += FolderTreeEntry(folder = folder, depth = depth)
            visit(folder.id, depth + 1)
        }
    }

    visit(null, 0)
    return rows
}

fun collectFolderDescendantIds(
    folders: List<Folder>,
    folderId: Int
): Set<Int> {
    val childrenByParent = folders.groupBy { it.parentFolderId }
    val descendantIds = mutableSetOf<Int>()

    fun visit(parentId: Int) {
        childrenByParent[parentId].orEmpty().forEach { child ->
            if (descendantIds.add(child.id)) {
                visit(child.id)
            }
        }
    }

    visit(folderId)
    return descendantIds
}

fun buildFolderBreadcrumb(
    folders: List<Folder>,
    folder: Folder
): List<String> {
    val folderById = folders.associateBy { it.id }
    val path = mutableListOf<String>()
    val visited = mutableSetOf<Int>()

    var current: Folder? = folder
    while (current != null && visited.add(current.id)) {
        path += current.name
        current = current.parentFolderId?.let(folderById::get)
    }

    path.reverse()
    return listOf("Home") + path
}

