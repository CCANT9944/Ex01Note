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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Edit
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.navArgument
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
            val themeMode by themeSettingsRepository.themeModeFlow().collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
            }
            val viewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(application, database.noteDao())
            )
            val folders by viewModel.folders.collectAsStateWithLifecycle(initialValue = emptyList<Folder>())

            val navController = rememberNavController()
            
            // Check if opened from Widget 
            var widgetNoteId by remember { mutableIntStateOf(intent?.getIntExtra("widget_note_id", -1) ?: -1) }

            DisposableEffect(Unit) {
                val listener = androidx.core.util.Consumer<android.content.Intent> { newIntent ->
                    val id = newIntent.getIntExtra("widget_note_id", -1)
                    if (id != -1) {
                        widgetNoteId = id
                    }
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            Ex01Theme(themeMode = themeMode) {
                val openNote: (Int, Boolean) -> Unit = { noteId, fromWidget ->
                    val route = if (fromWidget) "edit/$noteId?fromWidget=true" else "edit/$noteId"
                    navController.navigate(route) { launchSingleTop = true }
                }

                LaunchedEffect(widgetNoteId) {
                    if (widgetNoteId != -1) {
                        openNote(widgetNoteId, true)
                        widgetNoteId = -1
                        intent?.removeExtra("widget_note_id")
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "list",
                    enterTransition = { slideInHorizontally(animationSpec = tween(400), initialOffsetX = { it }) },
                    exitTransition = { slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { -it }) },
                    popEnterTransition = { slideInHorizontally(animationSpec = tween(400), initialOffsetX = { -it }) },
                    popExitTransition = { slideOutHorizontally(animationSpec = tween(400), targetOffsetX = { it }) }
                ) {
                    composable("list") {
                        MainScreen(
                            viewModel = viewModel,
                            allFolders = folders,
                            folderColorRepo = folderColorRepo,
                            themeSettingsRepository = themeSettingsRepository,
                            onNoteClick = { openNote(it, false) },
                            onFolderClick = { folderId -> navController.navigate("folder/$folderId") },
                            onOpenTrash = { navController.navigate("trash") }
                        )
                    }
                    composable("trash") {
                        TrashScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        "edit/{noteId}?fromWidget={fromWidget}",
                        arguments = listOf(
                            navArgument("noteId") { type = NavType.IntType },
                            navArgument("fromWidget") { type = NavType.BoolType; defaultValue = false }
                        ),
                        enterTransition = {
                            if (targetState.arguments?.getBoolean("fromWidget") == true) {
                                androidx.compose.animation.scaleIn(initialScale = 0.8f, animationSpec = tween(400)) + androidx.compose.animation.fadeIn(animationSpec = tween(400))
                            } else {
                                slideInHorizontally(animationSpec = tween(400), initialOffsetX = { it })
                            }
                        }
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
                        NoteEditScreen(
                            noteId = noteId,
                            viewModel = viewModel,
                            onBack = {
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
                            onNoteClick = { openNote(it, false) },
                            onBack = { navController.popBackStack() }
                        )
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
    onFolderClick: (Int) -> Unit,
    onOpenTrash: () -> Unit
) {
    val context = LocalContext.current
    val currentThemeMode by themeSettingsRepository.themeModeFlow().collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
    var showCreateChooser by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showSNoteDialog by remember { mutableStateOf(false) }
    var showListDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var noteToRename by remember { mutableStateOf<Note?>(null) }
    var noteToChangeStyle by remember { mutableStateOf<Note?>(null) }
    var notePreview by remember { mutableStateOf<Note?>(null) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }
    var noteToActions by remember { mutableStateOf<Note?>(null) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToMove by remember { mutableStateOf<Folder?>(null) }
    var folderToColor by remember { mutableStateOf<Folder?>(null) }
    var folderToActions by remember { mutableStateOf<Folder?>(null) }

    val folders by viewModel.rootFolders.collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by viewModel.unassignedNotes.collectAsStateWithLifecycle(initialValue = emptyList())
    val displayLists = notes.filter { it.kind == NoteKinds.CHECKLIST }.sortedBy { it.id }
    val displayNotes = notes.filter { it.kind == NoteKinds.FREE_TEXT || it.kind == NoteKinds.SNOTE }.sortedBy { it.id }

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
                            onMenuClick = { noteToActions = note },
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
                            onMenuClick = { noteToActions = note },
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

        noteToActions?.let { note ->
            NoteActionsDialog(
                note = note,
                onRename = {
                    noteToRename = note
                    noteToActions = null
                },
                onChangeStyle = if (note.kind == NoteKinds.FREE_TEXT) null else { {
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
            folderDialogTitle = "New Folder",
            onCreateFolder = { name -> viewModel.addFolder(name) },
            onDismissFolderDialog = { showFolderDialog = false },
            showNoteDialog = showNoteDialog,
            noteDialogTitle = "New Note",
            onCreateNote = { title -> viewModel.addNote(title, kind = NoteKinds.FREE_TEXT) },
            onDismissNoteDialog = { showNoteDialog = false },
            showSNoteDialog = showSNoteDialog,
            sNoteDialogTitle = "New S-Note",
            onCreateSNote = { title -> viewModel.addNote(title, kind = NoteKinds.SNOTE) },
            onDismissSNoteDialog = { showSNoteDialog = false },
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
                onDismissRequest = { showSettingsDialog = false },
                onOpenTrash = {
                    showSettingsDialog = false
                    onOpenTrash()
                }
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
                    onNoteActions = { noteToActions = it },
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

        noteToActions?.let { note ->
            NoteActionsDialog(
                note = note,
                onRename = {
                    noteToRename = note
                    noteToActions = null
                },
                onChangeStyle = if (note.kind == NoteKinds.FREE_TEXT) null else { {
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






