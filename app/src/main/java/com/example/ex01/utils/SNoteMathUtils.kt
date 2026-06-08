package com.example.ex01.utils
import com.example.ex01.ui.editor.snote.SNoteConfig

import androidx.compose.ui.geometry.Offset
import com.example.ex01.ui.editor.snote.DrawingLine

data class SelectionBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val cX: Float get() = (minX + maxX) / 2f
    val cY: Float get() = (minY + maxY) / 2f
}

fun computeSelectionBounds(
    lines: List<DrawingLine>,
    textBounds: Map<DrawingLine, Pair<Float, Float>> = emptyMap()
): SelectionBounds? {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    for (l in lines) {
        if (l.isEraser) continue
        if (l.text != null && l.points.isNotEmpty()) {
            val startPt = l.points.first()
            val bounds = textBounds[l]
            val tw = bounds?.first ?: (l.strokeWidth * 0.6f * l.text.length.toFloat())
            val th = bounds?.second ?: (l.strokeWidth * 1.5f)
            if (startPt.x < minX) minX = startPt.x
            if (startPt.y < minY) minY = startPt.y
            if (startPt.x + tw > maxX) maxX = startPt.x + tw
            if (startPt.y + th > maxY) maxY = startPt.y + th
        } else {
            val halfStroke = l.strokeWidth / 2f
            for (pt in l.points) {
                if (pt.x - halfStroke < minX) minX = pt.x - halfStroke
                if (pt.y - halfStroke < minY) minY = pt.y - halfStroke
                if (pt.x + halfStroke > maxX) maxX = pt.x + halfStroke
                if (pt.y + halfStroke > maxY) maxY = pt.y + halfStroke
            }
        }
    }
    return if (minX <= maxX && minY <= maxY) SelectionBounds(minX, minY, maxX, maxY) else null
}

fun isPointInSelectionBounds(
    point: Offset,
    selectedLines: List<DrawingLine>,
    dragOffset: Offset,
    scale: Float,
    textBounds: Map<DrawingLine, Pair<Float, Float>> = emptyMap()
): Boolean {
    val b = computeSelectionBounds(selectedLines, textBounds) ?: return false
    val sMinX = b.cX + (b.minX - b.cX) * scale + dragOffset.x
    val sMinY = b.cY + (b.minY - b.cY) * scale + dragOffset.y
    val sMaxX = b.cX + (b.maxX - b.cX) * scale + dragOffset.x
    val sMaxY = b.cY + (b.maxY - b.cY) * scale + dragOffset.y
    val pad = SNoteConfig.PAGE_GAP_DP
    return point.x in (sMinX - pad)..(sMaxX + pad) && point.y in (sMinY - pad)..(sMaxY + pad)
}

fun isPointInScaleHandle(
    point: Offset,
    selectedLines: List<DrawingLine>,
    dragOffset: Offset,
    scale: Float,
    textBounds: Map<DrawingLine, Pair<Float, Float>> = emptyMap()
): Boolean {
    val b = computeSelectionBounds(selectedLines, textBounds) ?: return false
    val sMaxX = b.cX + (b.maxX - b.cX) * scale + dragOffset.x
    val sMaxY = b.cY + (b.maxY - b.cY) * scale + dragOffset.y
    val dx = point.x - (sMaxX + 16f)
    val dy = point.y - (sMaxY + 16f)
    return dx * dx + dy * dy <= 60f * 60f
}

fun isPointInMenuHandle(
    point: Offset,
    selectedLines: List<DrawingLine>,
    dragOffset: Offset,
    scale: Float,
    textBounds: Map<DrawingLine, Pair<Float, Float>> = emptyMap()
): Boolean {
    val b = computeSelectionBounds(selectedLines, textBounds) ?: return false
    val sMaxX = b.cX + (b.maxX - b.cX) * scale + dragOffset.x
    val sMinY = b.cY + (b.minY - b.cY) * scale + dragOffset.y
    val dx = point.x - (sMaxX + 16f)
    val dy = point.y - (sMinY - 16f)
    return dx * dx + dy * dy <= 60f * 60f
}

fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var isInside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]
        if ((pi.y > point.y) != (pj.y > point.y) &&
            point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
        ) {
            isInside = !isInside
        }
        j = i
    }
    return isInside
}
