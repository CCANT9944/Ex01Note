package com.example.ex01.ui.screens

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val deletedFolders by viewModel.deletedFolders.collectAsStateWithLifecycle(initialValue = emptyList())
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle(initialValue = emptyList())
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash Bin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (deletedFolders.isNotEmpty() || deletedNotes.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Empty Trash")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (deletedFolders.isEmpty() && deletedNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Trash is empty", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (deletedFolders.isNotEmpty()) {
                        item {
                            Text("Folders", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        }
                        items(deletedFolders) { folder ->
                            TrashItemRow(
                                title = folder.name,
                                type = "Folder",
                                onRestore = { viewModel.restoreFolder(folder) },
                                onDelete = { viewModel.permanentlyDeleteFolder(folder) }
                            )
                        }
                    }

                    if (deletedNotes.isNotEmpty()) {
                        item {
                            Text("Items", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        }
                        items(deletedNotes) { note ->
                            TrashItemRow(
                                title = note.title,
                                type = if (note.kind == NoteKinds.CHECKLIST) "List" else "Note",
                                onRestore = { viewModel.restoreNote(note) },
                                onDelete = { viewModel.permanentlyDeleteNote(note) }
                            )
                        }
                    }
                }
            }
        }

        if (showEmptyTrashConfirm) {
            AlertDialog(
                onDismissRequest = { showEmptyTrashConfirm = false },
                title = { Text("Empty Trash") },
                text = { Text("Are you sure you want to permanently delete all items in the trash?") },
                confirmButton = {
                    Button(
                        onClick = {
                            deletedFolders.forEach { viewModel.permanentlyDeleteFolder(it) }
                            deletedNotes.forEach { viewModel.permanentlyDeleteNote(it) }
                            showEmptyTrashConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Empty") }
                },
                dismissButton = { TextButton(onClick = { showEmptyTrashConfirm = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun TrashItemRow(
    title: String,
    type: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(text = type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row {
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, contentDescription = "Restore")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

