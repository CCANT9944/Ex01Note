package com.example.ex01.ui.components

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*


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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.scale
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val targetHeight = if (isCollapsed) 128.dp else 380.dp
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            when {
                isSNote -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Create,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                bodyStyleNote -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(targetHeight)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onMenuClick
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Column(modifier = Modifier.fillMaxSize().padding(end = 4.dp, start = 4.dp)) {
                    if (isSNote) {
                        if (note.body.isBlank()) {
                            Text(
                                text = "Empty canvas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        } else {
                            var lines by remember(note.body) { mutableStateOf<List<com.example.ex01.ui.editor.DrawingLine>?>(null) }
                            LaunchedEffect(note.body) {
                                withContext(Dispatchers.Default) {
                                    lines = com.example.ex01.ui.editor.deserializeDrawing(note.body)
                                }
                            }
                            
                            if (lines.isNullOrEmpty()) {
                                Text(
                                    text = "S-Note Drawing",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            } else {
                                val strokeColor = MaterialTheme.colorScheme.onSurface
                                val isNightMode = strokeColor.luminance() > 0.5f // text is bright
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(100.dp).padding(4.dp)) {
                                    scale(0.28f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                                        for (line in lines!!) {
                                            if (line.text != null && line.points.isNotEmpty()) {
                                                val uiColor = if (line.color == androidx.compose.ui.graphics.Color.Unspecified) strokeColor else line.color
                                                val finalColor = if (isNightMode && uiColor.luminance() < 0.4f) androidx.compose.ui.graphics.Color.White else uiColor
                                                val paint = android.graphics.Paint().apply {
                                                    textSize = line.strokeWidth
                                                    isAntiAlias = true
                                                    color = android.graphics.Color.argb((finalColor.alpha * 255).toInt(), (finalColor.red * 255).toInt(), (finalColor.green * 255).toInt(), (finalColor.blue * 255).toInt())
                                                }
                                                drawContext.canvas.nativeCanvas.drawText(line.text, line.points.first().x, line.points.first().y - paint.fontMetrics.ascent, paint)
                                                continue
                                            }
                                            
                                            val activeLineColor = if (line.color == androidx.compose.ui.graphics.Color.Unspecified || line.color == androidx.compose.ui.graphics.Color.Black || line.color == androidx.compose.ui.graphics.Color.White) strokeColor else line.color
                                            val finalColor = when {
                                                line.isEraser -> androidx.compose.ui.graphics.Color.Transparent
                                                line.isHighlighter -> activeLineColor.copy(alpha = 0.4f)
                                                else -> activeLineColor
                                            }
                                            val finalBlendMode = when {
                                                line.isEraser -> androidx.compose.ui.graphics.BlendMode.Clear
                                                line.isHighlighter -> androidx.compose.ui.graphics.BlendMode.Multiply
                                                else -> androidx.compose.ui.graphics.BlendMode.SrcOver
                                            }
                                            
                                            drawPath(
                                                path = line.toPath(),
                                                color = finalColor,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = line.strokeWidth,
                                                    cap = if (line.isHighlighter) androidx.compose.ui.graphics.StrokeCap.Square else androidx.compose.ui.graphics.StrokeCap.Round,
                                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                                ),
                                                blendMode = finalBlendMode
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (bodyStyleNote) {
                        val previewBody = notePageBody(note.body, 0).trim().replace(Regex("[\uE000-\uE009]"), "")
                        if (previewBody.isNotBlank()) {
                            Text(
                                text = AnnotatedString(previewBody),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isCollapsed) 6 else 12,
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
                            val taking = if (isCollapsed) 5 else 8
                            previewItems.take(taking).forEachIndexed { index, item ->
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
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp)
                                )
                            }
                            if (previewItems.size > taking) {
                                Text(
                                    text = "...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    modifier = Modifier.padding(start = 12.dp)
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
            text = item.text.replace(Regex("[\uE000-\uE009]"), ""),
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
















