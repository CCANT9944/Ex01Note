import os
vm_path = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteViewModel.kt"
vm_code = """package com.example.ex01.ui.editor
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
class SNoteViewModel : ViewModel() {
    val drawingLines = mutableStateListOf<DrawingLine>()
    val undoneLines = mutableStateListOf<DrawingLine>()
    var currentPath by mutableStateOf<List<Offset>?>(null)
    var currentProperties by mutableStateOf(DrawingLine(emptyList()))
    var isEraserMode by mutableStateOf(false)
    var isHighlighterMode by mutableStateOf(false)
    var isTextMode by mutableStateOf(false)
    var showTextSizeMenu by mutableStateOf(false)
    var activeTextInputPosition by mutableStateOf<Offset?>(null)
    var activeTextValue by mutableStateOf(TextFieldValue(""))
    var showHighlighterThicknessMenu by mutableStateOf(false)
    var showThicknessMenu by mutableStateOf(false)
    var showEraserThicknessMenu by mutableStateOf(false)
    var showColorMenu by mutableStateOf(false)
    var showClearWarning by mutableStateOf(false)
    var pageCount by mutableIntStateOf(1)
    var initialLoadDone by mutableStateOf(false)
    var originalHitLine by mutableStateOf<DrawingLine?>(null)
    var originalHitIndex by mutableIntStateOf(-1)
}
"""
with open(vm_path, "w", encoding="utf-8") as f:
    f.write(vm_code)
snote_file = r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteEditor.kt"
with open(snote_file, "r", encoding="utf-8") as f:
    content = f.read()
content = content.replace("""@Composable
fun SNoteEditor(
    serializedBody: String,
    onSerializedBodyChange: (String) -> Unit
) {""", """@Composable
fun SNoteEditor(
    serializedBody: String,
    onSerializedBodyChange: (String) -> Unit,
    viewModel: SNoteViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) = with(viewModel) {""")
removals = [
    '    val drawingLines = remember { mutableStateListOf<DrawingLine>() }\n',
    '    val undoneLines = remember { mutableStateListOf<DrawingLine>() }\n',
    '    var currentPath by remember { mutableStateOf<List<Offset>?>(null) }\n',
    '    var currentProperties by remember { mutableStateOf(DrawingLine(emptyList())) }\n',
    '    var isEraserMode by remember { mutableStateOf(false) }\n',
    '    var isHighlighterMode by remember { mutableStateOf(false) }\n',
    '    var isTextMode by remember { mutableStateOf(false) }\n',
    '    var showTextSizeMenu by remember { mutableStateOf(false) }\n',
    '    var activeTextInputPosition by remember { mutableStateOf<Offset?>(null) }\n',
    '    var activeTextValue by remember { mutableStateOf(TextFieldValue("")) }\n',
    '    var showHighlighterThicknessMenu by remember { mutableStateOf(false) }\n',
    '    var showThicknessMenu by remember { mutableStateOf(false) }\n',
    '    var showEraserThicknessMenu by remember { mutableStateOf(false) }\n',
    '    var showColorMenu by remember { mutableStateOf(false) }\n',
    '    var showClearWarning by remember { mutableStateOf(false) }\n',
    '    var pageCount by remember { mutableIntStateOf(1) }\n',
    '    var initialLoadDone by remember { mutableStateOf(false) }\n',
    '    var originalHitLine by remember { mutableStateOf<DrawingLine?>(null) }\n',
    '    var originalHitIndex by remember { mutableIntStateOf(-1) }\n',
]
for rem in removals:
    content = content.replace(rem, "")
with open(snote_file, "w", encoding="utf-8") as f:
    f.write(content)
print("ViewModel extracted successfully!")
