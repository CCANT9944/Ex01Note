import sys, glob, re, os
def strip_unused(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    lines = content.split('\n')
    non_import_text = '\n'.join([l for l in lines if not l.strip().startswith('import ') and not l.strip().startswith('package ')])
    words = set(re.findall(r'\b\w+\b', non_import_text))
    new_lines = []
    removed = 0
    for line in lines:
        if line.strip().startswith('import '):
            match = re.match(r'import\s+(.*?)(?:\s+as\s+\w+)?$', line.strip())
            if match:
                import_path = match.group(1)
                class_name = import_path.split('.')[-1]
                if class_name != '*' and class_name not in words and 'Theme' not in class_name:
                    if class_name in ['getValue', 'setValue', 'provideDelegate']:
                        new_lines.append(line)
                    else:
                        removed += 1
                        continue
        new_lines.append(line)
    if removed > 0:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(new_lines))
        print(f"Removed {removed} imports from {file_path}")
for f in glob.glob('app/src/main/java/com/example/ex01/**/*.kt', recursive=True):
    strip_unused(f)
