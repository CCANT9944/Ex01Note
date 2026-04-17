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

