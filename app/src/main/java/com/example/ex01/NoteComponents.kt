package com.example.ex01

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    viewModel: NoteViewModel,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuRename: () -> Unit = {},
    onMenuExpand: (() -> Unit)? = null,
    onMenuChangeStyle: (() -> Unit)? = null,
    onMenuDelete: () -> Unit = {},
    onMenuMoveToFolder: (() -> Unit)? = null,
    isCollapsed: Boolean = false,
    onMenuCollapse: () -> Unit = {}
) {
    val items by viewModel.getItems(note.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val bodyStyleNote = note.kind == NoteKinds.FREE_TEXT && note.listStyle == NoteListStyles.CHECKLIST
    val isSNote = note.kind == NoteKinds.SNOTE
    val targetHeight = if (isCollapsed) 64.dp else 190.dp
    val animatedHeight by animateDpAsState(targetValue = targetHeight)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            if (isCollapsed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 24.dp, bottom = 16.dp)
                ) {
                    when {
                        isSNote -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        bodyStyleNote -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        else -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (note.kind == NoteKinds.CHECKLIST || note.kind == NoteKinds.FREE_TEXT || note.kind == NoteKinds.SNOTE) {
                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(end = 28.dp, start = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when {
                            isSNote -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Default.Create,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            bodyStyleNote -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            else -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (note.kind == NoteKinds.CHECKLIST || note.kind == NoteKinds.FREE_TEXT || note.kind == NoteKinds.SNOTE) {
                            Spacer(Modifier.width(8.dp))
                        }

                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    if (isSNote) {
                        Text(
                            text = if (note.body.isBlank()) "Empty canvas" else "S-Note Drawing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else if (bodyStyleNote) {
                        val previewBody = notePageBody(note.body, 0).trim()
                        val renderedPreviewBody by produceState<AnnotatedString?>(initialValue = null, previewBody) {
                            value = withContext(Dispatchers.Default) {
                                renderRichTextMarkup(previewBody)
                            }
                        }
                        if (previewBody.isNotBlank()) {
                            Text(
                                text = renderedPreviewBody ?: AnnotatedString(previewBody),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "No text yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        val previewItems = items.sortedBy { it.id }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            previewItems.take(3).forEachIndexed { index, item ->
                                val leadingLabel = when (note.listStyle) {
                                    NoteListStyles.BULLETED -> "•"
                                    NoteListStyles.NUMBERED -> "${index + 1}."
                                    else -> null
                                }
                                NoteItemRow(
                                    item = item,
                                    onCheckedChange = null,
                                    onDeleteClick = null,
                                    showDeleteButton = false,
                                    showCheckbox = false,
                                    leadingLabel = leadingLabel,
                                    compact = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                                )
                            }
                            if (previewItems.size > 3) {
                                Text(
                                    text = "...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 12.dp),
                                    fontSize = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            CardMenuButton(
                isCollapsed = isCollapsed,
                onMenuClick = onMenuClick,
                onMenuCollapse = onMenuCollapse,
                onMenuRename = onMenuRename,
                onMenuExpand = onMenuExpand,
                        onMenuChangeStyle = onMenuChangeStyle,
                onMenuDelete = onMenuDelete,
                onMenuMoveToFolder = onMenuMoveToFolder,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardMenuButton(
    isCollapsed: Boolean,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit,
    onMenuCollapse: () -> Unit,
    onMenuRename: () -> Unit,
    onMenuExpand: (() -> Unit)? = null,
    onMenuChangeStyle: (() -> Unit)? = null,
    onMenuDelete: () -> Unit,
    onMenuMoveToFolder: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { menuExpanded = true; onMenuClick() },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Menu",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            if (isCollapsed && onMenuExpand != null) {
                DropdownMenuItem(text = { Text("Expand") }, onClick = {
                    menuExpanded = false
                    onMenuExpand()
                })
            } else {
                DropdownMenuItem(text = { Text(if (isCollapsed) "Expand" else "Collapse") }, onClick = {
                    menuExpanded = false
                    onMenuCollapse()
                })
            }
            DropdownMenuItem(text = { Text("Rename") }, onClick = {
                menuExpanded = false
                onMenuRename()
            })
            if (onMenuChangeStyle != null) {
                DropdownMenuItem(text = { Text("Change style") }, onClick = {
                    menuExpanded = false
                    onMenuChangeStyle()
                })
            }
            if (onMenuMoveToFolder != null) {
                DropdownMenuItem(text = { Text("Move to folder") }, onClick = {
                    menuExpanded = false
                    onMenuMoveToFolder()
                })
            }
            DropdownMenuItem(text = { Text("Delete") }, onClick = {
                menuExpanded = false
                onMenuDelete()
            })
        }
    }
}

@Composable
fun NoteItemRow(
    item: NoteItem,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    showDeleteButton: Boolean = true,
    showCheckbox: Boolean = true,
    leadingLabel: String? = null,
    compact: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 1.dp else 2.dp)
    ) {
        if (showCheckbox) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange,
                enabled = onCheckedChange != null
            )
        } else if (leadingLabel != null) {
            Text(
                text = leadingLabel,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 18.dp).padding(end = 4.dp)
            )
        }
        Text(
            text = item.text,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (showCheckbox) 4.dp else 0.dp),
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (showCheckbox && item.isChecked) TextDecoration.LineThrough else null,
            color = if (showCheckbox && item.isChecked) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (showDeleteButton && onDeleteClick != null) {
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Item")
            }
        }
    }
}
















