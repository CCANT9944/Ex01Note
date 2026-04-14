import os
import re
snote_file = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteEditor.kt"
with open(snote_file, "r", encoding="utf-8") as f:
    content = f.read()
data_start = content.find("private const val PREFS_NAME")
data_end = content.find("private val EraserIcon")
data_content = content[data_start:data_end]
icons_start = data_end
icons_end = content.find("@Composable\nfun SNoteEditor")
icons_content = content[icons_start:icons_end]
snote_data = """package com.example.ex01.ui.editor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import org.json.JSONArray
import org.json.JSONObject
""" + data_content
with open(r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteData.kt", "w", encoding="utf-8") as f:
    f.write(snote_data)
snote_icons = """package com.example.ex01.ui.editor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
""" + icons_content
with open(r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteIcons.kt", "w", encoding="utf-8") as f:
    f.write(snote_icons)
new_content = content[:data_start] + content[icons_end:]
with open(snote_file, "w", encoding="utf-8") as f:
    f.write(new_content)
print("Extraction successful!")
