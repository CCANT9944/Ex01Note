
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    text = f.read()
text = text.replace('enabled = undoHistory.isNotEmpty()  activeTextInputPosition != null  selectedLines.isNotEmpty()', 'enabled = undoHistory.isNotEmpty() |' + '| activeTextInputPosition != null |' + '| selectedLines.isNotEmpty()')
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(text)
