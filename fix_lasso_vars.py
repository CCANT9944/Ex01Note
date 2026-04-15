import re
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    text = f.read()
# I need to find ar isTextMode and put the lasso variables after it
old_vars = "var isTextMode by remember { mutableStateOf(false) }"
new_vars = """var isTextMode by remember { mutableStateOf(false) }
    var isLassoMode by remember { mutableStateOf(false) }
    val selectedLines = remember { mutableStateListOf<DrawingLine>() }
    var lassoBoundingBox by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var currentLassoPath by remember { mutableStateOf<List<Offset>?>(null) }
    var selectionOffset by remember { mutableStateOf(Offset.Zero) }"""
text = text.replace(old_vars, new_vars)
# Fixing the stripped || from undo enabled
text = text.replace("enabled = undoHistory.isNotEmpty()  activeTextInputPosition != null  selectedLines.isNotEmpty()", "enabled = undoHistory.isNotEmpty() || activeTextInputPosition != null || selectedLines.isNotEmpty()")
text = text.replace("enabled = drawingLines.isNotEmpty()  activeTextInputPosition != null", "enabled = drawingLines.isNotEmpty() || activeTextInputPosition != null")
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("done")