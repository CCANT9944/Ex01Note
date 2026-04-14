import os
import shutil
base_dir = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01"
test_dir = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\test\java\com\example\ex01"
folders = ['ui/screens', 'ui/editor', 'ui/dialogs', 'ui/components', 'data']
for folder in folders:
    os.makedirs(os.path.join(base_dir, folder), exist_ok=True)
moves = [
    # Screens
    ("NoteEditScreen.kt", "ui/screens"),
    ("TrashScreen.kt", "ui/screens"),
    ("NoteUi.kt", "ui/screens"), # Home screen
    # Editor
    ("ChecklistEditor.kt", "ui/editor"),
    ("SNoteEditor.kt", "ui/editor"),
    ("PageBodyEditor.kt", "ui/editor"),
    ("NoteWritingToolbar.kt", "ui/editor"),
    ("RichTextEditorController.kt", "ui/editor"),
    ("RichTextMarkup.kt", "ui/editor"),
    ("NotePages.kt", "ui/editor"),
    # Dialogs
    ("AppDialogs.kt", "ui/dialogs"),
    ("AppSettingsDialog.kt", "ui/dialogs"),
    # Components
    ("HomeTopAppBar.kt", "ui/components"),
    ("NoteComponents.kt", "ui/components"),
    # Data
    ("NoteDatabase.kt", "data"),
    ("CollapsedFoldersRepository.kt", "data"),
    ("CollapsedNotesRepository.kt", "data"),
    ("FolderColorRepository.kt", "data"),
    ("LastOpenNoteRepository.kt", "data")
]
# Move them
for file_name, folder in moves:
    src = os.path.join(base_dir, file_name)
    dst = os.path.join(base_dir, folder, file_name)
    if os.path.exists(src):
        shutil.move(src, dst)
# Update packages and add imports to all .kt in base_dir + test_dir
all_kt_files = []
for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith('.kt'):
            all_kt_files.append(os.path.join(root, f))
for root, dirs, files in os.walk(test_dir):
    for f in files:
        if f.endswith('.kt'):
            all_kt_files.append(os.path.join(root, f))
imports_to_add = [
    "import com.example.ex01.*",
    "import com.example.ex01.data.*",
    "import com.example.ex01.ui.screens.*",
    "import com.example.ex01.ui.editor.*",
    "import com.example.ex01.ui.dialogs.*",
    "import com.example.ex01.ui.components.*",
    "import com.example.ex01.ui.theme.*",
    "import com.example.ex01.widget.*"
]
for file_path in all_kt_files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    # Determine what the package should be
    rel_path = os.path.relpath(file_path, base_dir)
    if file_path.startswith(test_dir):
        rel_path = os.path.relpath(file_path, test_dir)
        new_package = "package com.example.ex01"
    else:
        new_package = "package com.example.ex01"
        if os.path.dirname(rel_path):
            new_package += "." + os.path.dirname(rel_path).replace(os.sep, '.')
    lines = content.split('\n')
    new_lines = []
    has_injected = False
    for i, line in enumerate(lines):
        if line.startswith("package com.example.ex01"):
            new_lines.append(new_package)
            if file_path.startswith(base_dir) and not os.path.dirname(rel_path):
                pass
            elif not has_injected:
                new_lines.append("")
                for imp in imports_to_add:
                    new_lines.append(imp)
                new_lines.append("")
                has_injected = True
        elif line.strip() in imports_to_add:
            continue
        elif line.startswith("import com.example.ex01.") and line.count('.') <= 4 and line.endswith('*'):
            continue # Remove old wildcards
        else:
            new_lines.append(line)
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(new_lines))
