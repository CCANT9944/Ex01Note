package com.example.ex01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.view.WindowCompat
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ex01.ui.theme.Ex01Theme
import com.example.ex01.ui.theme.ThemeMode
import com.example.ex01.ui.theme.ThemeSettingsRepository
import com.example.ex01.NoteWritingToolbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = remember { NoteDatabase.getDatabase(context) }
            val folderColorRepo = remember { FolderColorRepository(context) }
            val themeSettingsRepository = remember { ThemeSettingsRepository(context) }
            val lastOpenNoteRepository = remember { LastOpenNoteRepository(context) }
            val themeMode by themeSettingsRepository.themeModeFlow().collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
            val savedLastOpenNoteId = remember { lastOpenNoteRepository.lastOpenNoteId() }
            var startupRoute by remember { mutableStateOf<String?>(if (savedLastOpenNoteId == null) "list" else null) }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = themeMode == ThemeMode.LIGHT
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = themeMode == ThemeMode.LIGHT
            }
            val viewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(database.noteDao())
            )

            val folders by viewModel.folders.collectAsStateWithLifecycle(initialValue = emptyList())

            val navController = rememberNavController()
            LaunchedEffect(savedLastOpenNoteId) {
                val restoredNoteId = savedLastOpenNoteId ?: return@LaunchedEffect
                val restoredNote = withContext(Dispatchers.IO) {
                    viewModel.getNote(restoredNoteId).firstOrNull()
                }
                startupRoute = if (restoredNote != null) {
                    "edit/$restoredNoteId"
                } else {
                    lastOpenNoteRepository.clearLastOpenNoteId()
                    "list"
                }
            }

            Ex01Theme(themeMode = themeMode) {
                if (startupRoute == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val openNote: (Int) -> Unit = { noteId ->
                        lastOpenNoteRepository.setLastOpenNoteId(noteId)
                        navController.navigate("edit/$noteId") { launchSingleTop = true }
                    }

                    NavHost(navController = navController, startDestination = startupRoute!!) {
                        composable("list") {
                            MainScreen(
                                viewModel = viewModel,
                                allFolders = folders,
                                folderColorRepo = folderColorRepo,
                                themeSettingsRepository = themeSettingsRepository,
                                onNoteClick = openNote,
                                onFolderClick = { folderId -> navController.navigate("folder/$folderId") }
                            )
                        }
                        composable("edit/{noteId}", arguments = listOf(navArgument("noteId") { type = NavType.IntType })) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
                            NoteEditScreen(
                                noteId = noteId,
                                viewModel = viewModel,
                                onBack = {
                                    lastOpenNoteRepository.clearLastOpenNoteId()
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("folder/{folderId}",
                            arguments = listOf(navArgument("folderId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val folderId = backStackEntry.arguments?.getInt("folderId") ?: -1
                            val folderName = folders.firstOrNull { it.id == folderId }?.name ?: "Folder"
                            FolderDetailScreen(
                                folderId = folderId,
                                folderName = folderName,
                                viewModel = viewModel,
                                allFolders = folders,
                                folderColorRepo = folderColorRepo,
                                onFolderClick = { childFolderId -> navController.navigate("folder/$childFolderId") },
                                onNoteClick = openNote,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NoteViewModel,
    allFolders: List<Folder>,
    folderColorRepo: FolderColorRepository,
    themeSettingsRepository: ThemeSettingsRepository,
    onNoteClick: (Int) -> Unit,
    onFolderClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val lastOpenNoteRepository = remember(context) { LastOpenNoteRepository(context) }
    val currentThemeMode by themeSettingsRepository.themeModeFlow().collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
    var showCreateChooser by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToRename by remember { mutableStateOf<Note?>(null) }
    var noteToChangeStyle by remember { mutableStateOf<Note?>(null) }
    var notePreview by remember { mutableStateOf<Note?>(null) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToMove by remember { mutableStateOf<Folder?>(null) }
    var folderToColor by remember { mutableStateOf<Folder?>(null) }
    var folderToActions by remember { mutableStateOf<Folder?>(null) }

    val folders by viewModel.rootFolders.collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by viewModel.unassignedNotes.collectAsStateWithLifecycle(initialValue = emptyList())
    val displayLists = notes.filter { it.kind == NoteKinds.CHECKLIST }.sortedBy { it.id }
    val displayNotes = notes.filter { it.kind == NoteKinds.FREE_TEXT }.sortedBy { it.id }

    val folderBounds = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    val collapsedFoldersRepo = remember(context) { CollapsedFoldersRepository(context) }
    val collapsedNotesRepo = remember(context) { CollapsedNotesRepository(context) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(folders) {
        folderBounds.keys.retainAll(folders.mapTo(mutableSetOf()) { it.id })
    }
    LaunchedEffect(notePreview?.id, notes) {
        val previewId = notePreview?.id ?: return@LaunchedEffect
        if (notes.none { it.id == previewId }) {
            notePreview = null
        }
    }

    Scaffold(
        topBar = { 
            HomeTopAppBar(onSettingsClick = { showSettingsDialog = true })
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showCreateChooser = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Create")
                }

                DropdownMenu(
                    expanded = showCreateChooser,
                    onDismissRequest = { showCreateChooser = false },

                    modifier = Modifier.widthIn(min = 96.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Note") },
                        onClick = {
                            showCreateChooser = false
                            showNoteDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("List") },
                        onClick = {
                            showCreateChooser = false
                            showListDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Folder") },
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
            if (folders.isEmpty() && displayLists.isEmpty() && displayNotes.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyStateCard(
                        title = "No items yet",
                        message = "Tap the + button to create your first folder, list, or note.",
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            } else {
                gridItems(folders, key = { it.id }) { folder ->
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

                if (folders.isNotEmpty() && (displayLists.isNotEmpty() || displayNotes.isNotEmpty())) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                gridItems(
                    items = displayLists,
                    key = { it.id },
                    span = { GridItemSpan(2) }
                ) { note ->
                    var itemOffset by remember(note.id) { mutableStateOf(Offset.Zero) }
                    var isDragging by remember(note.id) { mutableStateOf(false) }
                    var globalPosition by remember(note.id) { mutableStateOf(Offset.Zero) }

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
                            .zIndex(if (isDragging) 10f else 1f)
                    ) {
                        val noteCollapsed by collapsedNotesRepo.isCollapsedFlow(note.id).collectAsStateWithLifecycle(initialValue = true)
                        NoteCard(
                            note = note,
                            viewModel = viewModel,
                            onClick = { if (!isDragging) onNoteClick(note.id) },
                            onMenuClick = {},
                            onMenuRename = { noteToRename = note },
                            onMenuExpand = { notePreview = note },
                            onMenuChangeStyle = { noteToChangeStyle = note },
                            onMenuDelete = { noteToDelete = note },
                            onMenuMoveToFolder = { noteToMove = note },
                            isCollapsed = noteCollapsed,
                            onMenuCollapse = { scope.launch { collapsedNotesRepo.setCollapsed(note.id, !noteCollapsed) } },
                            modifier = Modifier.background(
                                if (isDragging) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                        )
                    }
                }

                gridItems(
                    items = displayNotes,
                    key = { it.id },
                    span = { GridItemSpan(2) }
                ) { note ->
                    var itemOffset by remember(note.id) { mutableStateOf(Offset.Zero) }
                    var isDragging by remember(note.id) { mutableStateOf(false) }
                    var globalPosition by remember(note.id) { mutableStateOf(Offset.Zero) }

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
                            .zIndex(if (isDragging) 10f else 1f)
                    ) {
                        val noteCollapsed by collapsedNotesRepo.isCollapsedFlow(note.id).collectAsStateWithLifecycle(initialValue = true)
                        NoteCard(
                            note = note,
                            viewModel = viewModel,
                            onClick = { if (!isDragging) onNoteClick(note.id) },
                            onMenuClick = {},
                            onMenuRename = { noteToRename = note },
                            onMenuExpand = { notePreview = note },
                            onMenuChangeStyle = null,
                            onMenuDelete = { noteToDelete = note },
                            onMenuMoveToFolder = { noteToMove = note },
                            isCollapsed = noteCollapsed,
                            onMenuCollapse = { scope.launch { collapsedNotesRepo.setCollapsed(note.id, !noteCollapsed) } },
                            modifier = Modifier.background(
                                if (isDragging) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                        )
                    }
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


        CreateItemDialogs(
            showFolderDialog = showFolderDialog,
            folderDialogTitle = "New Folder",
            onCreateFolder = { name -> viewModel.addFolder(name) },
            onDismissFolderDialog = { showFolderDialog = false },
            showNoteDialog = showNoteDialog,
            noteDialogTitle = "New Note",
            onCreateNote = { title -> viewModel.addNote(title, kind = NoteKinds.FREE_TEXT) },
            onDismissNoteDialog = { showNoteDialog = false },
            showListDialog = showListDialog,
            listDialogTitle = "New List",
            onCreateList = { listTitle, listStyle ->
                viewModel.addNote(
                    listTitle,
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
                message = "Are you sure you want to delete the folder \"${folder.name}\"?\n\nThe notes inside this folder will also be deleted.",
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
                title = "Delete Note",
                message = "Are you sure you want to delete the note \"${note.title}\"?",
                onConfirm = {
                    if (lastOpenNoteRepository.lastOpenNoteId() == note.id) {
                        lastOpenNoteRepository.clearLastOpenNoteId()
                    }
                    viewModel.deleteNote(note)
                    noteToDelete = null
                },
                onDismissRequest = { noteToDelete = null },
                confirmButtonColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

        if (showSettingsDialog) {
            AppSettingsDialog(
                currentThemeMode = currentThemeMode,
                onThemeModeSelected = themeSettingsRepository::setThemeMode,
                onDismissRequest = { showSettingsDialog = false }
            )
        }
    }
}

// FolderRow implementation moved to NoteUi.kt
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
    val lastOpenNoteRepository = remember(context) { LastOpenNoteRepository(context) }
    val currentFolder = remember(allFolders, folderId) { allFolders.firstOrNull { it.id == folderId } }
    val folderPath = remember(allFolders, folderId, folderName) {
        currentFolder?.let { buildFolderBreadcrumb(allFolders, it) } ?: listOf("Home", folderName)
    }
    val childFolders by viewModel.getFoldersByParent(folderId).collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by viewModel.getNotesByFolder(folderId).collectAsStateWithLifecycle(initialValue = emptyList())
    val folderBounds = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    val collapsedFoldersRepo = remember(context) { CollapsedFoldersRepository(context) }
    val scope = rememberCoroutineScope()
    var showCreateChooser by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToRename by remember { mutableStateOf<Note?>(null) }
    var noteToChangeStyle by remember { mutableStateOf<Note?>(null) }
    var notePreview by remember { mutableStateOf<Note?>(null) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }
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
                    modifier = Modifier.widthIn(min = 96.dp)
                ) {
                    DropdownMenuItem(text = { Text("Note") }, onClick = {
                        showCreateChooser = false
                        showNoteDialog = true
                    })
                    DropdownMenuItem(text = { Text("List") }, onClick = {
                        showCreateChooser = false
                        showListDialog = true
                    })
                    DropdownMenuItem(text = { Text("Folder") }, onClick = {
                        showCreateChooser = false
                        showFolderDialog = true
                    })
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (childFolders.isNotEmpty()) {
                SectionHeader(
                    title = "Folders",
                    subtitle = if (childFolders.size == 1) "1 folder" else "${childFolders.size} folders"
                )

                FolderGrid(
                    folders = childFolders,
                    viewModel = viewModel,
                    onFolderClick = onFolderClick,
                    onMenuClick = { folderToActions = it },
                    onRename = { folderToRename = it },
                    onDelete = { folderToDelete = it },
                    onMoveToFolder = { folderToMove = it },
                    folderBounds = folderBounds,
                    columns = GridCells.Fixed(4),
                    onChangeColor = { folderToColor = it },
                    folderColorRepo = folderColorRepo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            if (notes.isNotEmpty()) {
                SectionHeader(
                    title = "Items",
                    subtitle = if (notes.size == 1) "1 item" else "${notes.size} items"
                )

                NoteGrid(
                    notes = notes,
                    viewModel = viewModel,
                    onNoteClick = onNoteClick,
                    folderBounds = folderBounds,
                    onNoteRename = { noteToRename = it },
                    onNotePreview = { notePreview = it },
                    onNoteChangeStyle = { noteToChangeStyle = it },
                    onNoteDelete = { noteToDelete = it },
                    onNoteMoveToFolder = { noteToMove = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
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

        CreateItemDialogs(
            showFolderDialog = showFolderDialog,
            folderDialogTitle = "New Subfolder",
            onCreateFolder = { name -> viewModel.addFolder(name, parentFolderId = folderId) },
            onDismissFolderDialog = { showFolderDialog = false },
            showNoteDialog = showNoteDialog,
            noteDialogTitle = "New Note in Folder",
            onCreateNote = { title -> viewModel.addNote(title, folderId, kind = NoteKinds.FREE_TEXT) },
            onDismissNoteDialog = { showNoteDialog = false },
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
                    if (lastOpenNoteRepository.lastOpenNoteId() == note.id) {
                        lastOpenNoteRepository.clearLastOpenNoteId()
                    }
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderActionsDialog(
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
                TextButton(onClick = onExpandCollapse) { Text(if (isCollapsed) "Expand" else "Collapse") }
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
private fun FolderColorDialog(
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
private fun TextInputDialog(
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
private fun CreateItemDialogs(
    showFolderDialog: Boolean,
    folderDialogTitle: String,
    onCreateFolder: (String) -> Unit,
    onDismissFolderDialog: () -> Unit,
    showNoteDialog: Boolean,
    noteDialogTitle: String,
    onCreateNote: (String) -> Unit,
    onDismissNoteDialog: () -> Unit,
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
private fun CreateListDialog(
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
private fun ChangeListStyleDialog(
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
private fun NotePreviewDialog(
    note: Note,
    viewModel: NoteViewModel,
    onEdit: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val items by viewModel.getItems(note.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val previewItems = remember(items) { items.sortedByDescending { it.id } }
    val bodyStyleNote = note.kind == NoteKinds.FREE_TEXT && note.listStyle == NoteListStyles.CHECKLIST

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

                if (bodyStyleNote) {
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
                        if (note.body.isBlank()) {
                            Text(
                                text = "No text yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                                            val renderedBody by produceState<AnnotatedString?>(initialValue = null, note.body) {
                                                value = withContext(Dispatchers.Default) {
                                                    renderRichTextMarkup(note.body)
                                                }
                                            }
                            Text(
                                                text = renderedBody ?: AnnotatedString(note.body),
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
private fun ListStyleOptionRow(
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
private fun ConfirmDialog(
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

private data class FolderTreeEntry(
    val folder: Folder,
    val depth: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToFolderDialog(
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

private fun buildFolderTreeRows(
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

private fun collectFolderDescendantIds(
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

private fun buildFolderBreadcrumb(
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

