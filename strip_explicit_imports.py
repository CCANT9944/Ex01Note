import os
import re
base_dir = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01"
test_dir = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\test\java\com\example\ex01"
all_files = []
for d in [base_dir, test_dir]:
    for root, _, files in os.walk(d):
        for f in files:
            if f.endswith('.kt'):
                all_files.append(os.path.join(root, f))
for path in all_files:
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.read().split('\n')
    new_lines = []
    changed = False
    for line in lines:
        stripped = line.strip()
        # Remove any specific import starting with com.example.ex01.
        # But keep wildcard imports
        if stripped.startswith("import com.example.ex01.") and not stripped.endswith("*") and not stripped.endswith("R"):
            changed = True
            continue
        new_lines.append(line)
    if changed:
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(new_lines))
print("Done stripping explicit imports.")
