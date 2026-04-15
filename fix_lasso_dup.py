
import re
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    content = f.read()
# count the number of times commitLassoSelection is declared
# Replace all of them with empty, then put one back.
while "val commitLassoSelection =" in content:
    start_idx = content.find("val commitLassoSelection = {")
    if start_idx == -1:
        break
    # find closing brace
    # we know it ends with commitChanges()\n        }\n    }
    end_idx = content.find("commitChanges()", start_idx)
    if end_idx != -1:
        end_idx = content.find("}", end_idx) # } for if
        end_idx = content.find("}", end_idx + 1) # } for commitLassoSelection
        content = content[:start_idx] + content[end_idx+1:]
    else:
        break
commit_lasso_func = """
    val commitLassoSelection = {
        if (selectedLines.isNotEmpty()) {
            val finalLines = selectedLines.map { line ->
                line.copy(points = line.points.map { it + selectionOffset })
            }
            drawingLines.addAll(finalLines)
            selectedLines.clear()
            selectionOffset = Offset.Zero
            lassoBoundingBox = null
            currentLassoPath = null
            commitChanges()
        }
    }
"""
content = content.replace("val currentCommitChanges by rememberUpdatedState(commitChanges)", commit_lasso_func + "\\n    val currentCommitChanges by rememberUpdatedState(commitChanges)")
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "w", encoding="utf-8") as f:
    f.write(content)

