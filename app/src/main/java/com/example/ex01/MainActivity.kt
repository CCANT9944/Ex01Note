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
                val isLight = themeMode != ThemeMode.DARK
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = isLight
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = isLight
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




