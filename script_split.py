import sys
content = open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt', encoding='utf-8').read()
old1 = "var pageHeightDp by remember { mutableStateOf(0.dp) }"
new1 = """var pageHeightDp by remember { mutableStateOf(0.dp) }
    var currentCanvasWidthPx by remember { mutableFloatStateOf(0f) }
    var currentDensity = 1f"""
content = content.replace(old1, new1)
old2 = """            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableHeight = this.maxHeight
                val availableWidth = this.maxWidth
                val density = LocalDensity.current
                val availableWidthPx = with(density) { availableWidth.toPx() }"""
new2 = """            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableHeight = this.maxHeight
                val availableWidth = this.maxWidth
                val density = LocalDensity.current
                val availableWidthPx = with(density) { availableWidth.toPx() }
                currentCanvasWidthPx = availableWidthPx
                currentDensity = density.density"""
content = content.replace(old2, new2)
old_commit = """    val commitActiveText = {
        commitLassoSelection()
        if (activeTextInputPosition != null) {
            if (activeTextValue.text.isNotBlank()) {
                val cVal = Color(currentColorValue.toULong())
                val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
                drawingLines.add(
                    DrawingLine(
                        points = listOf(activeTextInputPosition!!),
                        color = chosenColor,
                        strokeWidth = currentTextSize,
                        text = activeTextValue.text
                    )
                )
                undoneLines.clear()
            }
            originalHitLine = null
            originalHitIndex = -1
            activeTextInputPosition = null
            activeTextValue = TextFieldValue("")
            commitChanges()
        }
    }"""
new_commit = """    val commitActiveText = {
        commitLassoSelection()
        if (activeTextInputPosition != null) {
            if (activeTextValue.text.isNotBlank()) {
                val cVal = Color(currentColorValue.toULong())
                val chosenColor = if (cVal in ALLOWED_PEN_COLORS) cVal else Color.Unspecified
                // --- TEXT PAGINATION ALGORITHM ---
                val rowHeight = TEXT_LARGE * 1.2f
                val pEst = android.graphics.Paint().apply { textSize = currentTextSize }
                val startX = activeTextInputPosition!!.x
                val startY = activeTextInputPosition!!.y
                val maxTextWidthPx = currentCanvasWidthPx - startX - (4f * currentDensity)
                val hardLines = activeTextValue.text.split("\\n")
                var currentBlockLines = mutableListOf<String>()
                var currentBlockY = startY
                var currentAccumulatedHeight = 0f
                var globalY = startY
                for (lineStr in hardLines) {
                    val w = pEst.measureText(lineStr)
                    val visualRows = if (maxTextWidthPx > 0f) kotlin.math.max(1, kotlin.math.ceil((w / maxTextWidthPx).toDouble()).toInt()) else 1
                    val segmentHeight = visualRows * rowHeight
                    if (pageHeightPx > 0f && (globalY + currentAccumulatedHeight + segmentHeight) > (kotlin.math.floor((globalY + currentAccumulatedHeight) / pageHeightPx) + 1) * pageHeightPx - (24f * currentDensity)) {
                        // The next line would breach the page gap! Commit current block and start a new one on the next page.
                        if (currentBlockLines.isNotEmpty()) {
                            drawingLines.add(
                                DrawingLine(
                                    points = listOf(Offset(startX, currentBlockY)),
                                    color = chosenColor,
                                    strokeWidth = currentTextSize,
                                    text = currentBlockLines.joinToString("\\n")
                                )
                            )
                            currentBlockLines.clear()
                        }
                        // Advance to next page perfectly snapped to row height grid
                        val targetPage = kotlin.math.floor((globalY + currentAccumulatedHeight + segmentHeight) / pageHeightPx).toInt()
                        currentBlockY = targetPage * pageHeightPx + (32f * currentDensity)
                        // Align mathematically to row height to preserve hit-detection bounds tracking
                        currentBlockY = kotlin.math.ceil(currentBlockY / rowHeight) * rowHeight
                        globalY = currentBlockY
                        currentAccumulatedHeight = 0f
                    }
                    currentBlockLines.add(lineStr)
                    currentAccumulatedHeight += segmentHeight
                }
                if (currentBlockLines.isNotEmpty()) {
                    drawingLines.add(
                        DrawingLine(
                            points = listOf(Offset(startX, currentBlockY)),
                            color = chosenColor,
                            strokeWidth = currentTextSize,
                            text = currentBlockLines.joinToString("\\n")
                        )
                    )
                }
                // ------------------------------------
                undoneLines.clear()
            }
            originalHitLine = null
            originalHitIndex = -1
            activeTextInputPosition = null
            activeTextValue = TextFieldValue("")
            commitChanges()
        }
    }"""
content = content.replace(old_commit, new_commit)
with open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated SNoteEditor successfully.")
