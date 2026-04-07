package com.example.ex01

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ex01.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsDialog(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDismissRequest: () -> Unit,
    onOpenTrash: () -> Unit,
) {
    val effectiveDarkTheme = currentThemeMode == ThemeMode.DARK
    val nextThemeMode = if (effectiveDarkTheme) ThemeMode.LIGHT else ThemeMode.DARK
    val toggleLabel = if (effectiveDarkTheme) "Theme / Light" else "Theme / Dark"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val hasOpenedOnce = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        drawerState.open()
        hasOpenedOnce.value = true
    }

    LaunchedEffect(drawerState.currentValue) {
        if (hasOpenedOnce.value && drawerState.currentValue == DrawerValue.Closed) {
            onDismissRequest()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ex01",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 16.dp, end = 28.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text(if (effectiveDarkTheme) "Light Theme" else "Dark Theme") },
                        icon = {
                            Icon(
                                imageVector = if (effectiveDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = null
                            )
                        },
                        selected = false,
                        onClick = { onThemeModeSelected(nextThemeMode) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Trash Bin") },
                        icon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }.invokeOnCompletion {
                                onOpenTrash()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().height(0.dp))
    }
}
