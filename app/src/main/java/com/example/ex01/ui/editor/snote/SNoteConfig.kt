package com.example.ex01.ui.editor.snote
object SNoteConfig {
    const val ROW_HEIGHT_MULTIPLIER = 1.2f
    const val PAGE_GAP_DP = 24f

    fun getRowHeight(textSize: Float): Float = textSize * ROW_HEIGHT_MULTIPLIER



    fun snapYToRow(y: Float, pageHeightPx: Float, rowHeight: Float, density: Float): Float {
        val snappedRelative = kotlin.math.round(y / rowHeight) * rowHeight
        return snappedRelative.coerceAtLeast(0f)
    }
}
