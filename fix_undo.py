import re
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    text = f.read()
# Replace undoneLines creation
old_undone = "val undoneLines = remember { mutableStateListOf<DrawingLine>() }"
new_undone = """val undoHistory = remember { mutableStateListOf<List<DrawingLine>>() }
    val redoHistory = remember { mutableStateListOf<List<DrawingLine>>() }
    fun pushUndoState() {
        // Only keep at most 30 states to prevent extreme memory usage
        if (undoHistory.size > 30) {
            undoHistory.removeAt(0)
        }
        undoHistory.add(drawingLines.map { it.copy() })
        redoHistory.clear()
    }"""
text = text.replace(old_undone, new_undone)
# Replace all undirected drawingLines modification where undoneLines is cleared
text = text.replace("undoneLines.clear()", "")
# Replace undoneLines condition checking lengths
# Undo Button
old_undo_btn_click = """                            if (activeTextInputPosition != null) {
                                commitActiveText()
                            } else if (drawingLines.isNotEmpty()) {
                                undoneLines.add(drawingLines.removeAt(drawingLines.size - 1))
                                commitChanges()
                            }"""
new_undo_btn_click = """                            if (activeTextInputPosition != null) {
                                commitActiveText()
                            } else if (selectedLines.isNotEmpty()) {
                                drawingLines.addAll(selectedLines)
                                selectedLines.clear()
                                selectionOffset = Offset.Zero
                                lassoBoundingBox = null
                                currentLassoPath = null
                                commitChanges()
                            } else if (undoHistory.isNotEmpty()) {
                                redoHistory.add(drawingLines.map { it.copy() })
                                drawingLines.clear()
                                drawingLines.addAll(undoHistory.removeLast())
                                commitChanges()
                            }"""
text = text.replace(old_undo_btn_click, new_undo_btn_click)
old_undo_btn_enabled = "enabled = drawingLines.isNotEmpty() || activeTextInputPosition != null"
new_undo_btn_enabled = "enabled = undoHistory.isNotEmpty() || activeTextInputPosition != null || selectedLines.isNotEmpty()"
text = text.replace(old_undo_btn_enabled, new_undo_btn_enabled)
# Redo Button
old_redo_btn_click = """                            if (undoneLines.isNotEmpty()) {
                                drawingLines.add(undoneLines.removeAt(undoneLines.size - 1))
                                commitChanges()
                            }"""
new_redo_btn_click = """                            if (redoHistory.isNotEmpty()) {
                                undoHistory.add(drawingLines.map { it.copy() })
                                drawingLines.clear()
                                drawingLines.addAll(redoHistory.removeLast())
                                commitChanges()
                            }"""
text = text.replace(old_redo_btn_click, new_redo_btn_click)
old_redo_btn_enabled = "enabled = undoneLines.isNotEmpty()"
new_redo_btn_enabled = "enabled = redoHistory.isNotEmpty()"
text = text.replace(old_redo_btn_enabled, new_redo_btn_enabled)
# Adding to undo stack when completing lines or lasso
text = text.replace("drawingLines.add(currentProperties.copy(points = currentPath!!))", "pushUndoState()\n                                                    drawingLines.add(currentProperties.copy(points = currentPath!!))")
# Add to undo for Clear All
old_clear_all = """                            drawingLines.clear()
                              commitChanges()"""
new_clear_all = """                            pushUndoState()
                              drawingLines.clear()
                              commitChanges()"""
text = text.replace(old_clear_all, new_clear_all)
# Replace commitLassoSelection's addition
old_commit_lasso = """    val commitLassoSelection = {
        if (selectedLines.isNotEmpty()) {
            val finalLines = selectedLines.map { line ->
                line.copy(points = line.points.map { it + selectionOffset })
            }
            drawingLines.addAll(finalLines)
            selectedLines.clear()"""
new_commit_lasso = """    val commitLassoSelection = {
        if (selectedLines.isNotEmpty()) {
            pushUndoState()
            val finalLines = selectedLines.map { line ->
                line.copy(points = line.points.map { it + selectionOffset })
            }
            drawingLines.addAll(finalLines)
            selectedLines.clear()"""
text = text.replace(old_commit_lasso, new_commit_lasso)
# lasso select removes lines from drawingLines. 
# Wait, selecting doesn't change history logic unless it commits. 
# But if we push to undoHistory when selecting, then undoing a selection undoes the drawing? No, we mapped undo click to deselect.
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("done")