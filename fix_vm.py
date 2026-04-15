import re
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteViewModel.kt', 'r', encoding='utf-8') as f:
    vm = f.read()
# Add lasso and undo fields to VM
new_vm_fields = """var isTextMode by mutableStateOf(false)
    var isLassoMode by mutableStateOf(false)
    val selectedLines = mutableStateListOf<DrawingLine>()
    var lassoBoundingBox by mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
    var currentLassoPath by mutableStateOf<List<Offset>?>(null)
    var selectionOffset by mutableStateOf(Offset.Zero)
    val undoHistory = mutableStateListOf<List<DrawingLine>>()
    val redoHistory = mutableStateListOf<List<DrawingLine>>()
    fun pushUndoState() {
        if (undoHistory.size > 30) {
            undoHistory.removeAt(0)
        }
        val snapshot = mutableListOf<DrawingLine>()
        for(l in drawingLines) {
            snapshot.add(l.copy())
        }
        undoHistory.add(snapshot)
        redoHistory.clear()
    }"""
vm = vm.replace("var isTextMode by mutableStateOf(false)", new_vm_fields)
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(vm)
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    ed = f.read()
# Remove the duplicated lasso and undo fields from SNoteEditor composable
to_remove = """var isTextMode by remember { mutableStateOf(false) }
    var isLassoMode by remember { mutableStateOf(false) }
    val selectedLines = remember { mutableStateListOf<DrawingLine>() }
    var lassoBoundingBox by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var currentLassoPath by remember { mutableStateOf<List<Offset>?>(null) }
    var selectionOffset by remember { mutableStateOf(Offset.Zero) }"""
ed = ed.replace(to_remove, "") # This was added just after 1st 100 lines somewhere? Wait, do I know where it is?
to_remove_undo = """val undoHistory = remember { mutableStateListOf<List<DrawingLine>>() }
    val redoHistory = remember { mutableStateListOf<List<DrawingLine>>() }
    fun pushUndoState() {
        // Only keep at most 30 states to prevent extreme memory usage
        if (undoHistory.size > 30) {
            undoHistory.removeAt(0)
        }
        undoHistory.add(drawingLines.map { it.copy() })
        redoHistory.clear()
    }"""
ed = ed.replace(to_remove_undo, "")
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(ed)
print("done")