package com.example.ex01.ui.editor
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
