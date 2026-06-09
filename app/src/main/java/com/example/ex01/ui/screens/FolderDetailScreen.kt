package com.example.ex01
import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.editor.snote.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
private fun DraggableNoteItem(
    note: Note,
    viewModel: NoteViewModel,
    folderBounds: MutableMap<Int, androidx.compose.ui.geometry.Rect>,
    collapsedNotesRepo: CollapsedNotesRepository,
    onNoteClick: (Int) -> Unit,
    onMenuClick: () -> Unit,
    onMenuRename: () -> Unit,
    onMenuExpand: () -> Unit,
    onMenuChangeStyle: (() -> Unit)?,
    onMenuDelete: () -> Unit,
    onMenuMoveToFolder: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var itemOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var globalPosition by remember { mutableStateOf(Offset.Zero) }
    var pointerWindowPos by remember { mutableStateOf(Offset.Zero) }
    val noteCollapsed by collapsedNotesRepo.isCollapsedFlow(note.id).collectAsStateWithLifecycle(initialValue = true)

    Box(
        modifier = Modifier
            .onGloballyPositioned { globalPosition = it.positionInWindow() }
            .pointerInput(note.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        isDragging = true
                        pointerWindowPos = globalPosition + localOffset
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        itemOffset += dragAmount
                        pointerWindowPos += dragAmount
                    },
                    onDragEnd = {
                        folderBounds.forEach { (id, rect) ->
                            if (rect.contains(pointerWindowPos)) {
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
            .zIndex(if (isDragging) 10f else 1f)
    ) {
        NoteCard(
            note = note,
            viewModel = viewModel,
            onClick = { if (!isDragging) onNoteClick(note.id) },
            onMenuClick = onMenuClick,
            onMenuRename = onMenuRename,
            onMenuExpand = onMenuExpand,
            onMenuChangeStyle = onMenuChangeStyle,
            onMenuDelete = onMenuDelete,
            onMenuMoveToFolder = onMenuMoveToFolder,
            isCollapsed = noteCollapsed,
            onMenuCollapse = { scope.launch { collapsedNotesRepo.setCollapsed(note.id, !noteCollapsed) } },
            modifier = Modifier.background(
                if (isDragging) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: Int,
    folderName: String,
    viewModel: NoteViewModel,
    allFolders: List<Folder>,
    folderColorRepo: FolderColorRepository,
    onFolderClick: (Int) -> Unit,
    onNoteClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentFolder = remember(allFolders, folderId) { allFolders.firstOrNull { it.id == folderId } }
    val folderPath = remember(allFolders, folderId, folderName) {
        currentFolder?.let { buildFolderBreadcrumb(allFolders, it) } ?: listOf("Home", folderName)
    }
    val childFolders by viewModel.getFoldersByParent(folderId).collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by viewModel.getNotesByFolder(folderId).collectAsStateWithLifecycle(initialValue = emptyList())
    val folderBounds = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    val collapsedFoldersRepo = remember(context) { CollapsedFoldersRepository(context) }
    val collapsedNotesRepo = remember(context) { CollapsedNotesRepository(context) }
    val scope = rememberCoroutineScope()
    var showCreateChooser by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showSNoteDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToRename by remember { mutableStateOf<Note?>(null) }
    var noteToChangeStyle by remember { mutableStateOf<Note?>(null) }
    var notePreview by remember { mutableStateOf<Note?>(null) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }
    var noteToActions by remember { mutableStateOf<Note?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToMove by remember { mutableStateOf<Folder?>(null) }
    var folderToColor by remember { mutableStateOf<Folder?>(null) }
    var folderToActions by remember { mutableStateOf<Folder?>(null) }
    LaunchedEffect(notePreview?.id, notes) {
        val previewId = notePreview?.id ?: return@LaunchedEffect
        if (notes.none { it.id == previewId }) {
            notePreview = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = folderPath.joinToString("/"),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = currentFolder != null) {
                            folderToRename = currentFolder
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showCreateChooser = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Create")
                }

                DropdownMenu(
                    expanded = showCreateChooser,
                    onDismissRequest = { showCreateChooser = false },
                    modifier = Modifier.widthIn(min = 160.dp),
                    offset = androidx.compose.ui.unit.DpOffset(x = (-16).dp, y = (-8).dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Note") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        onClick = {
                            showCreateChooser = false
                            showNoteDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("S-Note") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showCreateChooser = false
                            showSNoteDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("List") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null) },
                        onClick = {
                            showCreateChooser = false
                            showListDialog = true
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                    DropdownMenuItem(
                        text = { Text("Folder") },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        onClick = {
                            showCreateChooser = false
                            showFolderDialog = true
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (childFolders.isEmpty() && notes.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyStateCard(
                        title = "Folder is empty",
                        message = "Tap the + button to create a note or subfolder.",
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            } else {
                if (childFolders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(
                            title = "Folders",
                            subtitle = if (childFolders.size == 1) "1 folder" else "${childFolders.size} folders",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                gridItems(childFolders, key = { "folder_${it.id}" }) { folder ->
                    val isCollapsed by collapsedFoldersRepo.isCollapsedFlow(folder.id).collectAsStateWithLifecycle(initialValue = true)
                    val folderColorValue by folderColorRepo.colorFlow(folder.id).collectAsStateWithLifecycle(initialValue = null)
                    FolderCard(
                        folder = folder,
                        viewModel = viewModel,
                        onClick = { onFolderClick(folder.id) },
                        onMenuClick = { folderToActions = folder },
                        onMenuRename = { folderToRename = folder },
                        onMenuDelete = { folderToDelete = folder },
                        onMenuMoveToFolder = { folderToMove = folder },
                        onMenuChangeColor = { folderToColor = folder },
                        isCollapsed = isCollapsed,
                        onMenuCollapse = { scope.launch { collapsedFoldersRepo.setCollapsed(folder.id, !isCollapsed) } },
                        iconTint = folderColorValue?.let(::Color) ?: MaterialTheme.colorScheme.primary,
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

                if (childFolders.isNotEmpty() && notes.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                val displayLists = notes.filter { it.kind == NoteKinds.CHECKLIST }.sortedBy { it.id }
                val displayNotes = notes.filter { it.kind == NoteKinds.FREE_TEXT || it.kind == NoteKinds.SNOTE }.sortedBy { it.id }

                gridItems(displayLists, key = { "list_${it.id}" }, span = { GridItemSpan(2) }) { note ->
                    DraggableNoteItem(
                        note = note,
                        viewModel = viewModel,
                        folderBounds = folderBounds,
                        collapsedNotesRepo = collapsedNotesRepo,
                        onNoteClick = onNoteClick,
                        onMenuClick = { noteToActions = note },
                        onMenuRename = { noteToRename = note },
                        onMenuExpand = { notePreview = note },
                        onMenuChangeStyle = { noteToChangeStyle = note },
                        onMenuDelete = { noteToDelete = note },
                        onMenuMoveToFolder = { noteToMove = note },
                    )
                }

                gridItems(displayNotes, key = { "note_${it.id}" }, span = { GridItemSpan(2) }) { note ->
                    DraggableNoteItem(
                        note = note,
                        viewModel = viewModel,
                        folderBounds = folderBounds,
                        collapsedNotesRepo = collapsedNotesRepo,
                        onNoteClick = onNoteClick,
                        onMenuClick = { noteToActions = note },
                        onMenuRename = { noteToRename = note },
                        onMenuExpand = { notePreview = note },
                        onMenuChangeStyle = null,
                        onMenuDelete = { noteToDelete = note },
                        onMenuMoveToFolder = { noteToMove = note },
                    )
                }
            }
        }

        folderToColor?.let { folder ->
            val currentColor = folderColorRepo.colorFlow(folder.id).collectAsStateWithLifecycle(initialValue = null).value
            FolderColorDialog(
                folderName = folder.name,
                currentColor = currentColor,
                onColorSelected = { selected ->
                    folderColorRepo.setColor(folder.id, selected)
                    folderToColor = null
                },
                onDismissRequest = { folderToColor = null }
            )
        }

        folderToActions?.let { folder ->
            val actionsCollapsed by collapsedFoldersRepo.isCollapsedFlow(folder.id).collectAsStateWithLifecycle(initialValue = true)
            FolderActionsDialog(
                folder = folder,
                isCollapsed = actionsCollapsed,
                onExpandCollapse = {
                    scope.launch { collapsedFoldersRepo.setCollapsed(folder.id, !actionsCollapsed) }
                    folderToActions = null
                },
                onRename = {
                    folderToRename = folder
                    folderToActions = null
                },
                onMoveToFolder = {
                    folderToMove = folder
                    folderToActions = null
                },
                onChangeColor = {
                    folderToColor = folder
                    folderToActions = null
                },
                onDelete = {
                    folderToDelete = folder
                    folderToActions = null
                },
                onDismissRequest = { folderToActions = null }
            )
        }

        noteToActions?.let { note ->
            NoteActionsDialog(
                note = note,
                onRename = {
                    noteToRename = note
                    noteToActions = null
                },
                onChangeStyle = if (note.kind != NoteKinds.CHECKLIST) null else { {
                    noteToChangeStyle = note
                    noteToActions = null
                } },
                onMoveToFolder = {
                    noteToMove = note
                    noteToActions = null
                },
                onDelete = {
                    noteToDelete = note
                    noteToActions = null
                },
                onDismissRequest = { noteToActions = null }
            )
        }

        CreateItemDialogs(
            showFolderDialog = showFolderDialog,
            folderDialogTitle = "New Subfolder",
            onCreateFolder = { name -> viewModel.addFolder(name, parentFolderId = folderId) },
            onDismissFolderDialog = { showFolderDialog = false },
            showNoteDialog = showNoteDialog,
            noteDialogTitle = "New Note in Folder",
            onCreateNote = { title -> viewModel.addNote(title, folderId, kind = NoteKinds.FREE_TEXT) },
            onDismissNoteDialog = { showNoteDialog = false },
            showSNoteDialog = showSNoteDialog,
            sNoteDialogTitle = "New S-Note in Folder",
            onCreateSNote = { title -> viewModel.addNote(title, folderId, kind = NoteKinds.SNOTE) },
            onDismissSNoteDialog = { showSNoteDialog = false },
            showListDialog = showListDialog,
            listDialogTitle = "New List in Folder",
            onCreateList = { listTitle, listStyle ->
                viewModel.addNote(
                    listTitle,
                    folderId,
                    kind = NoteKinds.CHECKLIST,
                    listStyle = listStyle
                )
            },
            onDismissListDialog = { showListDialog = false }
        )

        folderToRename?.let { folder ->
            var newName by remember { mutableStateOf(folder.name) }
            TextInputDialog(
                title = "Rename Folder",
                value = newName,
                onValueChange = { newName = it },
                confirmText = "Rename",
                onConfirm = {
                    if (newName.isNotBlank() && newName != folder.name) {
                        viewModel.updateFolder(folder.copy(name = newName))
                    }
                    folderToRename = null
                },
                onDismissRequest = { folderToRename = null }
            )
        }

        folderToDelete?.let { folder ->
            ConfirmDialog(
                title = "Delete Folder",
                message = "Are you sure you want to delete the folder \"${folder.name}\"?\n\nThe folders and notes inside it will also be deleted.",
                onConfirm = {
                    viewModel.deleteFolder(folder)
                    folderToDelete = null
                },
                onDismissRequest = { folderToDelete = null },
                confirmButtonColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            )
        }

        noteToDelete?.let { note ->
            ConfirmDialog(
                title = "Delete List",
                message = "Are you sure you want to delete the list \"${note.title}\" from this folder?",
                onConfirm = {
                    viewModel.deleteNote(note)
                    noteToDelete = null
                },
                onDismissRequest = { noteToDelete = null }
            )
        }

        noteToRename?.let { note ->
            var newTitle by remember { mutableStateOf(note.title) }
            TextInputDialog(
                title = "Rename Note",
                value = newTitle,
                onValueChange = { newTitle = it },
                confirmText = "Rename",
                onConfirm = {
                    if (newTitle.isNotBlank() && newTitle != note.title) {
                        viewModel.updateNote(note.copy(title = newTitle))
                    }
                    noteToRename = null
                },
                onDismissRequest = { noteToRename = null }
            )
        }

        noteToChangeStyle?.let { note ->
            ChangeListStyleDialog(
                title = "Change List Style",
                currentStyle = note.listStyle,
                onConfirm = { style ->
                    viewModel.updateNoteListStyle(note, style)
                    noteToChangeStyle = null
                },
                onDismissRequest = { noteToChangeStyle = null }
            )
        }

        noteToMove?.let { note ->
            MoveToFolderDialog(
                itemLabel = note.title,
                folders = allFolders,
                excludedFolderIds = emptySet(),
                onFolderSelected = { folder ->
                    viewModel.moveNoteToFolder(note.id, folder?.id)
                    noteToMove = null
                },
                onDismissRequest = { noteToMove = null }
            )
        }

        notePreview?.let { note ->
            NotePreviewDialog(
                note = note,
                viewModel = viewModel,
                onEdit = {
                    notePreview = null
                    onNoteClick(note.id)
                },
                onDismissRequest = { notePreview = null }
            )
        }

        folderToMove?.let { folder ->
            val excludedIds = remember(allFolders, folder.id) { collectFolderDescendantIds(allFolders, folder.id) + folder.id }
            MoveToFolderDialog(
                itemLabel = folder.name,
                folders = allFolders,
                excludedFolderIds = excludedIds,
                onFolderSelected = { target ->
                    viewModel.updateFolder(folder.copy(parentFolderId = target?.id))
                    folderToMove = null
                },
                onDismissRequest = { folderToMove = null }
            )
        }
    }
}
