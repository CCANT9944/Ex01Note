
import re
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    content = f.read()
content = content.replace("\\n    val currentCommitChanges by rememberUpdatedState(commitChanges)", "\n    val currentCommitChanges by rememberUpdatedState(commitChanges)")
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "w", encoding="utf-8") as f:
    f.write(content)

