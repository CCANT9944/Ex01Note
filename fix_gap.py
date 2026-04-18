import sys
content = open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt', encoding='utf-8').read()
old1 = '''private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPageLayout(
    pageCount: Int,
    pageHeightPx: Float
) {
    // Draw thick visual dividers to make pages look distinct
    for (i in 1 until pageCount) {
        val y = i * pageHeightPx
        drawRect(
            color = Color.Gray.copy(alpha = 0.3f),
            topLeft = Offset(0f, y - 8.dp.toPx()),
            size = Size(size.width, 16.dp.toPx())
        )
        // Add a slight shadow/borderline effect
        drawLine(
            color = Color.DarkGray,
            start = Offset(0f, y - 8.dp.toPx()),
            end = Offset(size.width, y - 8.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.DarkGray,
            start = Offset(0f, y + 8.dp.toPx()),
            end = Offset(size.width, y + 8.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
    // Draw page numbers at the bottom right
    val textSizePx = 14.dp.toPx()
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = textSizePx
        textAlign = android.graphics.Paint.Align.RIGHT
        isAntiAlias = true
    }
    for (i in 0 until pageCount) {
        val pageBottomY = (i + 1) * pageHeightPx
        drawContext.canvas.nativeCanvas.drawText(
            "Page \",
            size.width - 16.dp.toPx(),
            pageBottomY - 24.dp.toPx(),
            textPaint
        )
    }
}'''
new1 = '''private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPageLayout(
    pageCount: Int,
    pageHeightPx: Float
) {
    for (i in 1 until pageCount) {
        val y = i * pageHeightPx
        drawLine(
            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.7f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
        )
    }
}'''
old2 = '''                        }
                    }
                }
                FilledIconButton('''
new2 = '''                        }
                        for (i in 0 until pageCount) {
                            val bottomY = pageHeightDp * (i + 1) - 34.dp
                            Text(
                                text = "Page \",
                                color = androidx.compose.ui.graphics.Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier
                                    .offset(x = availableWidth - 80.dp, y = bottomY)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                FilledIconButton('''
content = content.replace(old1, new1)
content = content.replace(old2, new2)
with open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Replaced:", content != open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt', encoding='utf-8').read() or "(actually didn't verify cleanly since overwrote)")
