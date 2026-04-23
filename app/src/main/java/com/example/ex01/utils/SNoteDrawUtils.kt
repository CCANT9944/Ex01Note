package com.example.ex01.utils

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.example.ex01.ui.editor.snote.DrawingLine

fun DrawScope.drawSNoteLine(
    line: DrawingLine,
    path: Path,
    strokeColor: Color,
    eraserColor: Color
) {
    val activeColor = if (line.color == Color.Unspecified) strokeColor else line.color
    val finalColor = when {
        line.isEraser -> eraserColor
        line.isHighlighter -> activeColor.copy(alpha = 0.4f)
        else -> activeColor
    }
    val blendMode = when {
        line.isEraser -> androidx.compose.ui.graphics.BlendMode.Clear
        line.isHighlighter -> androidx.compose.ui.graphics.BlendMode.Multiply
        else -> androidx.compose.ui.graphics.BlendMode.SrcOver
    }
    drawPath(
        path = path,
        color = finalColor,
        style = Stroke(
            width = line.strokeWidth * (if (line.isHighlighter) 1.5f else 1f),
            cap = if (line.isHighlighter) StrokeCap.Square else StrokeCap.Round,
            join = StrokeJoin.Round
        ),
        blendMode = blendMode
    )
}

fun android.graphics.Canvas.drawSNoteLine(
    line: DrawingLine,
    path: android.graphics.Path,
    widgetStrokeMultiplier: Float = 1f,
    uiColor: android.graphics.Color? = null,
    isNightMode: Boolean = false,
    paint: Paint,
    clearPaint: Paint
) {
    if (line.isEraser) {
        val oldWidth = clearPaint.strokeWidth
        clearPaint.strokeWidth = line.strokeWidth * widgetStrokeMultiplier
        drawPath(path, clearPaint)
        clearPaint.strokeWidth = oldWidth
    } else if (line.isHighlighter) {
        val oldWidth = paint.strokeWidth
        val oldColor = paint.color
        val oldCap = paint.strokeCap
        val oldXfer = paint.xfermode
        paint.strokeWidth = line.strokeWidth * widgetStrokeMultiplier * 1.5f
        val finalUiColor = uiColor ?: android.graphics.Color.valueOf(line.color.value.toLong())
        var cColor = finalUiColor.toArgb()
        if (line.color == Color.Unspecified) {
            cColor = if (isNightMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        }
        val originalAlpha = android.graphics.Color.alpha(cColor)
        val hAlpha = kotlin.math.min(255, (originalAlpha * 0.4f).toInt())
        paint.color = android.graphics.Color.argb(
           hAlpha,
           android.graphics.Color.red(cColor),
           android.graphics.Color.green(cColor),
           android.graphics.Color.blue(cColor)
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        paint.strokeCap = Paint.Cap.SQUARE
        drawPath(path, paint)
        paint.xfermode = oldXfer
        paint.strokeCap = oldCap
        paint.color = oldColor
        paint.strokeWidth = oldWidth
    } else {
        val oldWidth = paint.strokeWidth
        val oldColor = paint.color
        val oldCap = paint.strokeCap
        paint.strokeWidth = line.strokeWidth * widgetStrokeMultiplier
        val finalUiColor = uiColor ?: android.graphics.Color.valueOf(line.color.value.toLong())
        var cColor = finalUiColor.toArgb()
        if (line.color == Color.Unspecified) {
            cColor = if (isNightMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        }
        paint.color = cColor
        paint.strokeCap = Paint.Cap.ROUND
        drawPath(path, paint)
        paint.strokeWidth = oldWidth
        paint.color = oldColor
        paint.strokeCap = oldCap
    }
}
