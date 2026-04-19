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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Column(modifier = Modifier.fillMaxSize().padding(end = 4.dp, start = 4.dp)) {
                    if (isSNote) {
                        Text(
                            text = if (note.body.isBlank()) "Empty canvas" else "S-Note Drawing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
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
