import os
file_path = r'C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteEditor.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('if (selectedLines.isNotEmpty()  currentLassoPath != null)', 'if (selectedLines.isNotEmpty() || currentLassoPath != null)')
content = content.replace('enabled = undoStack.isNotEmpty()  activeTextInputPosition != null', 'enabled = undoStack.isNotEmpty() || activeTextInputPosition != null')
with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done!")
