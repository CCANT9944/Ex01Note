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
fun FolderActionsDialog(
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
fun FolderColorDialog(
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

data class FolderTreeEntry(
    val folder: Folder,
    val depth: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderDialog(
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

fun buildFolderTreeRows(
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

fun collectFolderDescendantIds(
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

fun buildFolderBreadcrumb(
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

