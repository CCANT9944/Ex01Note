import os
import re

def split_file(filepath, out_dir, mapping):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    header_match = re.search(r'^(package .*?)\n(.*?)(?=\n@|\nfun |\nclass |\ninternal fun |\ndata class )', content, flags=re.DOTALL)
    if header_match:
        package_decl = header_match.group(1)
        # CRITICAL FIX: REMOVE HEADER FROM CONTENT
        content = content[header_match.end():]
    else:
        package_decl = f"package {filepath.replace('app/src/main/java/', '').rsplit('/', 1)[0].replace('/', '.')}"

    imports = """
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
"""

    header = package_decl + "\n" + imports + "\n\n"

    blocks_raw = re.split(r'\n(?=@+(?:OptIn|Composable|Preview|Experimental|Suppress)[^\n]*\n|fun |class |data class |internal fun |object |val )', '\n' + content)

    blocks = {}
    buffer = ""

    for b in blocks_raw:
        if not b.strip(): continue

        m = re.search(r'(?:fun|class|data class|object|val|var)\s+([A-Za-z0-9_]+)', b)
        if m:
            name = m.group(1)
            blocks[name] = buffer + b.strip() + '\n\n'
            buffer = ""
        else:
            buffer += b.strip() + '\n'

    os.makedirs(out_dir, exist_ok=True)

    for filename, fn_names in mapping.items():
        out_content = header
        wrote = False
        for fn in fn_names:
            if fn in blocks:
                out_content += blocks[fn]
                wrote = True
                del blocks[fn]

        if wrote:
            with open(os.path.join(out_dir, filename), 'w', encoding='utf-8') as f:
                f.write(out_content)

    if blocks:
        out_content = header
        for name, blk in blocks.items():
            out_content += blk
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(out_content)
    else:
        if os.path.exists(filepath):
            os.remove(filepath)

mapping_dialogs = {
    'FolderDialogs.kt': ['FolderActionsDialog', 'FolderColorDialog', 'FolderTreeEntry', 'MoveToFolderDialog', 'buildFolderTreeRows', 'collectFolderDescendantIds', 'buildFolderBreadcrumb', 'visit'],
    'NoteDialogs.kt': ['NoteActionsDialog', 'NotePreviewDialog', 'ChangeListStyleDialog', 'ListStyleOptionRow'],
    'CreateItemDialogs.kt': ['CreateItemDialogs', 'CreateListDialog'],
    'CommonDialogs.kt': ['TextInputDialog', 'ConfirmDialog']
}
split_file('app/src/main/java/com/example/ex01/ui/dialogs/AppDialogs.kt', 'app/src/main/java/com/example/ex01/ui/dialogs', mapping_dialogs)

mapping_components = {
    'FolderCard.kt': ['FolderCard', 'FolderLabelStack'],
    'NoteCard.kt': ['NoteCard', 'NoteItemRow', 'PreviewFormattedText', 'SNotePreview', 'FreeTextPreview', 'ChecklistPreview'],
    'EmptyStateCard.kt': ['EmptyStateCard', 'SectionHeader'],
    'GridComponents.kt': ['FolderGrid', 'NoteGrid']
}
split_file('app/src/main/java/com/example/ex01/ui/components/NoteComponents.kt', 'app/src/main/java/com/example/ex01/ui/components', mapping_components)

