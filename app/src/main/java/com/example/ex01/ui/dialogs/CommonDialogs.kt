package com.example.ex01.ui.dialogs

import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.window.*
import androidx.lifecycle.compose.*
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.theme.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.editor.snote.*
import kotlinx.coroutines.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputDialog(
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
fun ConfirmDialog(
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

