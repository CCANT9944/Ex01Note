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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ex01.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsDialog(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val effectiveDarkTheme = currentThemeMode == ThemeMode.DARK
    val nextThemeMode = if (effectiveDarkTheme) ThemeMode.LIGHT else ThemeMode.DARK
    val toggleLabel = if (effectiveDarkTheme) "Theme / Light" else "Theme / Dark"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val hasOpenedOnce = remember { mutableStateOf(false) }

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
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(240.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Button(
                            onClick = { onThemeModeSelected(nextThemeMode) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(toggleLabel)
                        }
                    }
                }
            }
        }
    ) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.height(0.dp))
    }
}
