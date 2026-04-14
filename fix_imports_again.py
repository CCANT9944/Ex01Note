import os
base_dir = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01"
test_dir = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\test\java\com\example\ex01"
all_files = []
for d in [base_dir, test_dir]:
    for root, _, files in os.walk(d):
        for f in files:
            if f.endswith('.kt'):
                all_files.append(os.path.join(root, f))
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
for path in all_files:
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.read().split('\n')
    # Check if they are already there
    has_wildcard = sum(1 for line in lines if line.strip() in imports_to_add) > 0
    if not has_wildcard:
        new_lines = []
        injected = False
        for line in lines:
            if line.startswith("package com.example.") and not injected:
                new_lines.append(line)
                new_lines.append("")
                new_lines.extend(imports_to_add)
                new_lines.append("")
                injected = True
            else:
                new_lines.append(line)
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(new_lines))
