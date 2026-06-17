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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.navArgument

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
