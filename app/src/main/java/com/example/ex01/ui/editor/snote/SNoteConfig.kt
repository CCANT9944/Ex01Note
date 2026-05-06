package com.example.ex01.ui.editor.snote
object SNoteConfig {
    const val ROW_HEIGHT_MULTIPLIER = 1.2f
    const val PAGE_GAP_DP = 24f
    const val PAGE_TOP_MARGIN_DP = 8f
    fun getRowHeight(textSize: Float): Float = textSize * ROW_HEIGHT_MULTIPLIER

    fun getRowsPerPage(pageHeightPx: Float, textSize: Float, density: Float): Int {
        if (pageHeightPx <= 0) return 0
        val gapPx = PAGE_GAP_DP * density
        val topMarginPx = PAGE_TOP_MARGIN_DP * density
        val contentHeight = pageHeightPx - gapPx - topMarginPx
        val rowHeight = getRowHeight(textSize)
        return kotlin.math.floor(contentHeight / rowHeight).toInt()
    }

    fun snapYToRow(y: Float, pageHeightPx: Float, rowHeight: Float, density: Float): Float {
        val gapPx = PAGE_GAP_DP * density
        val topMarginPx = PAGE_TOP_MARGIN_DP * density
        val pageIndex = kotlin.math.floor(y / pageHeightPx).toInt()
        val pageStart = pageIndex * pageHeightPx
        val contentStart = pageStart + topMarginPx
        val contentEnd = pageStart + pageHeightPx - gapPx
        
        // If position is in the gap or top margin, snap to nearest valid content area
        if (y < contentStart) {
            return contentStart
        }
        if (y >= contentEnd) {
            val lastValidY = contentEnd - rowHeight
            return kotlin.math.max(contentStart, lastValidY)
        }
        
        // Snap to nearest row relative to the content start of the page
        val relativeY = y - contentStart
        val snappedRelative = kotlin.math.round(relativeY / rowHeight) * rowHeight
        val snapped = contentStart + snappedRelative
        
        // CRITICAL: Ensure the row's bottom doesn't touch or overlap the gap
        val maxValidY = contentEnd - rowHeight
        return snapped.coerceIn(contentStart, maxValidY)
    }
}
