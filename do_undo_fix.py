import os
import re
file_path = r'C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteEditor.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()
# Replace val undoneLines = viewModel.undoneLines
content = re.sub(
    r'val undoneLines = viewModel\.undoneLines',
    r'val undoStack = viewModel.undoStack\n    val redoStack = viewModel.redoStack\n\n'
    r'    val pushUndoState = {\n'
    r'        undoStack.add(drawingLines.toList())\n'
    r'        redoStack.clear()\n'
    r'        if (undoStack.size > 50) undoStack.removeAt(0)\n'
    r'    }',
    content
)
# text active commit
content = re.sub(
    r'undoneLines\.clear\(\)\s*\}\s*originalHitLine',
    r'}\n            originalHitLine',
    content
)
# First undo block
undo_click_old = r'''                                if (originalHitLine != null) \{
                                    val index = if (originalHitIndex in 0\.\.drawingLines.size) originalHitIndex else drawingLines.size
                                    drawingLines\.add\(index, originalHitLine!!\)
                                    originalHitLine = null
                                    originalHitIndex = -1
                                \}
                                commitChanges\(\)
                            \} else if \(drawingLines\.isNotEmpty\(\)\) \{
                                undoneLines\.add\(drawingLines\.removeAt\(drawingLines\.size - 1\)\)
                                commitChanges\(\)
                            \}'''
undo_click_new = r'''                                if (originalHitLine != null) {
                                    val index = if (originalHitIndex in 0..drawingLines.size) originalHitIndex else drawingLines.size
                                    drawingLines.add(index, originalHitLine!!)
                                    originalHitLine = null
                                    originalHitIndex = -1
                                }
                                commitChanges()
                            } else if (undoStack.isNotEmpty()) {
                                redoStack.add(drawingLines.toList())
                                val prev = undoStack.removeAt(undoStack.size - 1)
                                drawingLines.clear()
                                drawingLines.addAll(prev)
                                commitChanges()
                            }'''
content = content.replace(undo_click_old, undo_click_new)
# Redo block
redo_click_old = r'''                        onClick = \{
                            commitActiveText\(\)
                            if \(undoneLines\.isNotEmpty\(\)\) \{
                                drawingLines\.add\(undoneLines\.removeAt\(undoneLines\.size - 1\)\)
                                commitChanges\(\)
                            \}
                        \},
                        modifier = Modifier\.size\(38\.dp\),
                        enabled = undoneLines\.isNotEmpty\(\)'''
redo_click_new = r'''                        onClick = {
                            commitActiveText()
                            if (redoStack.isNotEmpty()) {
                                undoStack.add(drawingLines.toList())
                                val next = redoStack.removeAt(redoStack.size - 1)
                                drawingLines.clear()
                                drawingLines.addAll(next)
                                commitChanges()
                            }
                        },
                        modifier = Modifier.size(38.dp),
                        enabled = redoStack.isNotEmpty()'''
content = re.sub(redo_click_old, redo_click_new, content)
# Path clear
path_clear_old = r'''                                                if \(!isTextMode && currentPath != null\) \{
                                                    drawingLines\.add\(currentProperties\.copy\(points = currentPath!!\)\)
                                                    undoneLines\.clear\(\)'''
path_clear_new = r'''                                                if (!isTextMode && currentPath != null) {
                                                    pushUndoState()
                                                    drawingLines.add(currentProperties.copy(points = currentPath!!))'''
content = re.sub(path_clear_old, path_clear_new, content)
# Eraser delete line
eraser_del_old = r'''                                                                    if \(originalHitLine == null\) \{
                                                                        originalHitLine = drawingLines\[hitIndex\]
                                                                        originalHitIndex = hitIndex
                                                                    \}
                                                                    drawingLines\.removeAt\(hitIndex\)'''
eraser_del_new = r'''                                                                    if (originalHitLine == null) {
                                                                        pushUndoState()
                                                                        originalHitLine = drawingLines[hitIndex]
                                                                        originalHitIndex = hitIndex
                                                                    }
                                                                    drawingLines.removeAt(hitIndex)'''
content = re.sub(eraser_del_old, eraser_del_new, content)
# Clear all
clear_all_old = r'''                        onClick = \{
                            drawingLines\.clear\(\)
                            undoneLines\.clear\(\)'''
clear_all_new = r'''                        onClick = {
                            pushUndoState()
                            drawingLines.clear()'''
content = re.sub(clear_all_old, clear_all_new, content)
### TEXT addition state push
text_add_old = r'''            if \(activeTextInputPosition != null && activeTextValue\.text\.isNotEmpty\(\)\) \{
                val cVal = Color\(currentColorValue\.toULong\(\)\)
                val chosenColor = if \(cVal in ALLOWED_PEN_COLORS\) cVal else Color\.Unspecified
                drawingLines\.add\('''
text_add_new = r'''            if (activeTextInputPosition != null && activeTextValue.text.isNotEmpty()) {
                val cVal = Color(currentColorValue.toULong())
                val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
                pushUndoState()
                drawingLines.add('''
content = re.sub(text_add_old, text_add_new, content)
### Lasso state
lasso_find_old = r'''                                                        if \(toSelect\.isNotEmpty\(\)\) \{
                                                            drawingLines\.clear\(\)'''
lasso_find_new = r'''                                                        if (toSelect.isNotEmpty()) {
                                                            pushUndoState()
                                                            drawingLines.clear()'''
content = re.sub(lasso_find_old, lasso_find_new, content)
with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("done")
