import os
def strip_private(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    content = content.replace("private const val", "const val")
    content = content.replace("private val ALLOWED_PEN_COLORS", "val ALLOWED_PEN_COLORS")
    content = content.replace("private val EraserIcon", "val EraserIcon")
    content = content.replace("private val TextIcon", "val TextIcon")
    content = content.replace("private val HighlighterIcon", "val HighlighterIcon")
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
strip_private(r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteData.kt")
strip_private(r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteIcons.kt")
print("Removed private modifiers!")
