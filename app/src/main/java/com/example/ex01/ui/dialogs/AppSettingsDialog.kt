package com.example.ex01.ui.dialogs

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsDialog(
    themeSettingsRepository: ThemeSettingsRepository,
    currentThemeMode: ThemeMode,
    onDismissRequest: () -> Unit,
    onOpenTrash: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val isApplyingTheme = remember { mutableStateOf(false) }

    if (isApplyingTheme.value) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss */ },
            confirmButton = {},
            title = { Text("Applying Theme...") },
            text = { Text("Please wait a moment while the theme is applied.") },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .width(350.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Trash Bin") },
                    icon = {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { onOpenTrash() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                val isDark = currentThemeMode == ThemeMode.DARK
                NavigationDrawerItem(
                    label = { Text(if (isDark) "Light Mode" else "Dark Mode") },
                    icon = {
                        Icon(imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = null)
                    },
                    selected = false,
                    onClick = {
                        if (!isApplyingTheme.value) {
                            isApplyingTheme.value = true
                            coroutineScope.launch {
                                delay(100)
                                themeSettingsRepository.setThemeMode(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK)
                                delay(400)
                                isApplyingTheme.value = false
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
