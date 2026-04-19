package com.example.ex01.utils

import androidx.compose.ui.geometry.Offset
import com.example.ex01.ui.editor.snote.DrawingLine

fun isPointInSelectionBounds(
    point: Offset,
    selectedLines: List<DrawingLine>,
    dragOffset: Offset,
    scale: Float
): Boolean {
    if (selectedLines.isEmpty()) return false
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    selectedLines.forEach { l ->
        if (l.isEraser) return@forEach
        val halfStroke = l.strokeWidth / 2f
        l.points.forEach { pt ->
            if (pt.x - halfStroke < minX) minX = pt.x - halfStroke
            if (pt.y - halfStroke < minY) minY = pt.y - halfStroke
            if (pt.x + halfStroke > maxX) maxX = pt.x + halfStroke
            if (pt.y + halfStroke > maxY) maxY = pt.y + halfStroke
        }
    }

    val cX = (minX + maxX) / 2f
    val cY = (minY + maxY) / 2f

    val sMinX = cX + (minX - cX) * scale + dragOffset.x
    val sMinY = cY + (minY - cY) * scale + dragOffset.y
    val sMaxX = cX + (maxX - cX) * scale + dragOffset.x
    val sMaxY = cY + (maxY - cY) * scale + dragOffset.y

    val boundsPadding = 24f
    return point.x in (sMinX - boundsPadding)..(sMaxX + boundsPadding) &&
           point.y in (sMinY - boundsPadding)..(sMaxY + boundsPadding)
}

fun isPointInScaleHandle(
    point: Offset,
    selectedLines: List<DrawingLine>,
    dragOffset: Offset,
    scale: Float
): Boolean {
    if (selectedLines.isEmpty()) return false
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    selectedLines.forEach { l ->
        if (l.isEraser) return@forEach
        val halfStroke = l.strokeWidth / 2f
        l.points.forEach { pt ->
            if (pt.x - halfStroke < minX) minX = pt.x - halfStroke
            if (pt.y - halfStroke < minY) minY = pt.y - halfStroke
            if (pt.x + halfStroke > maxX) maxX = pt.x + halfStroke
            if (pt.y + halfStroke > maxY) maxY = pt.y + halfStroke
        }
    }
    val cX = (minX + maxX) / 2f
    val cY = (minY + maxY) / 2f

    val sMaxX = cX + (maxX - cX) * scale + dragOffset.x
    val sMaxY = cY + (maxY - cY) * scale + dragOffset.y

    val boundsPadding = 16f
    val handleX = sMaxX + boundsPadding
    val handleY = sMaxY + boundsPadding
    val hitRadius = 60f // generous hit area for touch

    val dx = point.x - handleX
    val dy = point.y - handleY
    return (dx * dx + dy * dy) <= hitRadius * hitRadius
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
