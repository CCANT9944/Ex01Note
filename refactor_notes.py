# -*- coding: utf-8 -*-
import sys
import re
def replace_file():
    content = open('app/src/main/java/com/example/ex01/ui/components/NoteComponents.kt', 'r', encoding='utf-8').read()
    match = re.search(r'(val animatedHeight by animateDpAsState\(targetValue = targetHeight\)\s*\n\s*)Card.*?\}\s*\}\s*\}\s*\}\s*$', content, re.DOTALL)
    if not match:
        print('Not found')
        return
    new_str = '''\g<1>Column(modifier = modifier) {
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
                .height(animatedHeight)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onMenuClick
                )
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        val previewBody = notePageBody(note.body, 0).trim()
                        val renderedPreviewBody = renderRichTextMarkup(previewBody)
                        if (previewBody.isNotBlank()) {
                            Text(
                                text = renderedPreviewBody ?: AnnotatedString(previewBody),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isCollapsed) 4 else 10,
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
                            val taking = if (isCollapsed) 3 else 5
                            previewItems.take(taking).forEachIndexed { index, item ->
                                val leadingLabel = when (note.listStyle) {
                                    NoteListStyles.BULLETED -> ""
                                    NoteListStyles.NUMBERED -> "\."
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
                            if (previewItems.size > taking) {
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
        }
    }
}
'''
    new_content = re.sub(r'(val animatedHeight by animateDpAsState\(targetValue = targetHeight\)\s*\n\s*)Card.*?\}\s*\}\s*\}\s*\}\s*$', new_str, content, flags=re.DOTALL)
    open('app/src/main/java/com/example/ex01/ui/components/NoteComponents.kt', 'w', encoding='utf-8').write(new_content)
    print('replaced successfully')
replace_file()
