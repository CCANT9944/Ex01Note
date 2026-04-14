package com.example.ex01.ui.screens

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*


import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color


@Composable
fun NoteGrid(
    notes: List<Note>,
    viewModel: NoteViewModel,
    onNoteClick: (Int) -> Unit,
    folderBounds: MutableMap<Int, androidx.compose.ui.geometry.Rect>,
    modifier: Modifier = Modifier,
    onNoteRename: (Note) -> Unit,
    onNotePreview: (Note) -> Unit = {},
    onNoteChangeStyle: (Note) -> Unit = {},
    onNoteDelete: (Note) -> Unit,
    onNoteMoveToFolder: (Note) -> Unit = {}
) {
    val context = LocalContext.current
    val collapsedNotesRepo = remember(context) { CollapsedNotesRepository(context) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notes, key = { it.id }) { note ->
            var itemOffset by remember { mutableStateOf(Offset.Zero) }
            var isDragging by remember { mutableStateOf(false) }
            var globalPosition by remember { mutableStateOf(Offset.Zero) }
            val noteCollapsed by collapsedNotesRepo.isCollapsedFlow(note.id).collectAsStateWithLifecycle(initialValue = true)

            Box(
                modifier = Modifier
                    .onGloballyPositioned { globalPosition = it.positionInWindow() }
                    .pointerInput(note.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { isDragging = true },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                itemOffset += dragAmount
                            },
                            onDragEnd = {
                                val dropPoint = globalPosition + itemOffset
                                folderBounds.forEach { (id, rect) ->
                                    if (rect.contains(dropPoint)) {
                                        viewModel.moveNoteToFolder(note.id, id)
                                    }
                                }
                                isDragging = false
                                itemOffset = Offset.Zero
                            },
                            onDragCancel = {
                                isDragging = false
                                itemOffset = Offset.Zero
                            }
                        )
                    }
                    .offset { IntOffset(itemOffset.x.roundToInt(), itemOffset.y.roundToInt()) }
            ) {
                NoteCard(
                    note = note,
                    viewModel = viewModel,
                    onClick = { if (!isDragging) onNoteClick(note.id) },
                    onMenuClick = {},
                    onMenuRename = { onNoteRename(note) },
                    onMenuExpand = { onNotePreview(note) },
                    onMenuChangeStyle = if (note.kind == NoteKinds.FREE_TEXT) null else ({ onNoteChangeStyle(note) }),
                    onMenuDelete = { onNoteDelete(note) },
                    onMenuMoveToFolder = { onNoteMoveToFolder(note) },
                    isCollapsed = noteCollapsed,
                    onMenuCollapse = { collapsedNotesRepo.setCollapsed(note.id, !noteCollapsed) },
                    modifier = Modifier
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun FolderGrid(
    folders: List<Folder>,
    viewModel: NoteViewModel,
    onFolderClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: (Folder) -> Unit = {},
    onRename: (Folder) -> Unit,
    onDelete: (Folder) -> Unit,
    onMoveToFolder: (Folder) -> Unit = {},
    onChangeColor: (Folder) -> Unit = {},
    folderColorRepo: FolderColorRepository? = null,
    folderBounds: MutableMap<Int, androidx.compose.ui.geometry.Rect>,
    columns: GridCells = GridCells.Fixed(2),
    showMenu: Boolean = true,
    folderIconTint: (Folder) -> Color = { Color.Unspecified }
) {
    val context = LocalContext.current
    val collapsedRepo = remember(context) { CollapsedFoldersRepository(context) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(folders) {
        folderBounds.keys.retainAll(folders.mapTo(mutableSetOf()) { it.id })
    }

    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(folders, key = { it.id }) { folder ->
            val isCollapsed by collapsedRepo.isCollapsedFlow(folder.id).collectAsStateWithLifecycle(initialValue = true)
            val repoTint = folderColorRepo?.let { repo ->
                val savedColor by repo.colorFlow(folder.id).collectAsStateWithLifecycle(initialValue = null)
                savedColor?.let(::Color)
            }
            val resolvedTint = (repoTint ?: folderIconTint(folder)).let { if (it == Color.Unspecified) MaterialTheme.colorScheme.primary else it }
            FolderCard(
                folder = folder,
                viewModel = viewModel,
                onClick = { onFolderClick(folder.id) },
                onMenuClick = { onMenuClick(folder) },
                onMenuRename = { onRename(folder) },
                onMenuDelete = { onDelete(folder) },
                onMenuMoveToFolder = { onMoveToFolder(folder) },
                onMenuChangeColor = { onChangeColor(folder) },
                isCollapsed = isCollapsed,
                onMenuCollapse = { scope.launch { collapsedRepo.setCollapsed(folder.id, !isCollapsed) } },
                showMenu = showMenu,
                iconTint = resolvedTint,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val windowPos = coordinates.positionInWindow()
                    folderBounds[folder.id] = androidx.compose.ui.geometry.Rect(
                        windowPos.x, windowPos.y,
                        windowPos.x + coordinates.size.width,
                        windowPos.y + coordinates.size.height
                    )
                }
            )
        }
    }
}
