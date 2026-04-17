package com.example.ex01.ui.components

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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun FolderCard(
    folder: Folder,
    viewModel: NoteViewModel,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuRename: () -> Unit = {},
    onMenuDelete: () -> Unit = {},
    onMenuMoveToFolder: (() -> Unit)? = null,
    onMenuChangeColor: (() -> Unit)? = null,
    isCollapsed: Boolean = false,
    onMenuCollapse: () -> Unit = {},
    showMenu: Boolean = true,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    val childFolders by viewModel.getFoldersByParent(folder.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by viewModel.getNotesByFolder(folder.id).collectAsStateWithLifecycle(initialValue = emptyList())
    
    val targetHeight = when {
        isCollapsed -> 80.dp
        else -> 148.dp
    }
    val animatedHeight by animateDpAsState(targetValue = targetHeight)
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val containerColor = if (isLightTheme) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = if (isLightTheme) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }
    val elevation = if (isLightTheme) {
        if (isCollapsed) 2.dp else 1.dp
    } else {
        if (isCollapsed) 4.dp else 2.dp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (showMenu) onMenuClick() }
            ),

        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        shadowElevation = elevation,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            if (isCollapsed) {
                Column(modifier = Modifier.fillMaxSize()) {
                    FolderLabelStack(
                        name = folder.name,
                        modifier = Modifier.fillMaxWidth(),
                        iconSize = 48.dp,
                        textStyle = MaterialTheme.typography.titleSmall.copy(fontSize = 11.sp),
                        textColor = if (showMenu) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        iconTint = iconTint,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        spacing = 0.dp
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    FolderLabelStack(
                        name = folder.name,
                        modifier = Modifier.fillMaxWidth(),
                        iconSize = 44.dp,
                        textStyle = MaterialTheme.typography.titleSmall.copy(fontSize = 11.sp),
                        textColor = MaterialTheme.colorScheme.onSurface,
                        iconTint = iconTint,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        spacing = (-4).dp
                    )


                    Spacer(Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 24.dp, start = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        if (childFolders.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                childFolders.take(2).forEach { childFolder ->
                                    FolderLabelStack(
                                        name = childFolder.name,
                                        modifier = Modifier.fillMaxWidth(),
                                        iconSize = 10.dp,
                                        textStyle = MaterialTheme.typography.labelSmall,
                                        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        iconAlpha = 0.65f,
                                        horizontalAlignment = Alignment.Start,
                                        spacing = 1.dp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (childFolders.size > 2) {
                                    Text(
                                        text = "+${childFolders.size - 2} more subfolders",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (notes.isNotEmpty()) {
                            Spacer(Modifier.height(1.dp))
                        }

                        if (notes.isEmpty() && childFolders.isEmpty()) {
                            Text(
                                "Empty",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        } else {
                            notes.take(2).forEach { note ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    when (note.kind) {
                                        NoteKinds.CHECKLIST -> Icon(
                                            Icons.AutoMirrored.Filled.FormatListBulleted,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        NoteKinds.FREE_TEXT -> Icon(
                                            Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        NoteKinds.SNOTE -> Icon(
                                            Icons.Default.Create,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (notes.size > 2) {
                                Text(
                                    text = "+${notes.size - 2} more",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
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
fun FolderLabelStack(
    name: String,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 28.dp,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    iconAlpha: Float = 0.7f,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    spacing: androidx.compose.ui.unit.Dp = 4.dp,
    fontWeight: FontWeight = FontWeight.SemiBold,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(iconSize)) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = iconTint.copy(alpha = iconAlpha),
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = name,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = textColor,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    }
}

