import re

def main():
    with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
        content = f.read()

    # Add PathEffect import
    if "import androidx.compose.ui.graphics.PathEffect" not in content:
        content = content.replace("import androidx.compose.ui.graphics.Path", "import androidx.compose.ui.graphics.Path\nimport androidx.compose.ui.graphics.PathEffect")

    # Add Rect import
    if "import androidx.compose.ui.geometry.Rect" not in content:
        content = content.replace("import androidx.compose.ui.geometry.Offset", "import androidx.compose.ui.geometry.Offset\nimport androidx.compose.ui.geometry.Rect")

    # Add commitLassoSelection function
    commit_lasso_func = """
    val commitLassoSelection = {
        if (selectedLines.isNotEmpty()) {
            val finalLines = selectedLines.map { line ->
                line.copy(points = line.points.map { it + selectionOffset })
            }
            drawingLines.addAll(finalLines)
            selectedLines.clear()
            selectionOffset = Offset.Zero
            lassoBoundingBox = null
            currentLassoPath = null
            commitChanges()
        }
    }
"""
    content = content.replace("val currentCommitChanges by rememberUpdatedState(commitChanges)", commit_lasso_func + "\n    val currentCommitChanges by rememberUpdatedState(commitChanges)")

    # Modify clear tools when pen clicked
    content = re.sub(
        r"isEraserMode = false\s*isHighlighterMode = false\s*isTextMode = false",
        "isEraserMode = false\n                                    isHighlighterMode = false\n                                    isTextMode = false\n                                    isLassoMode = false\n                                    commitLassoSelection()",
        content
    )

    # In Toolbar, replace `if (!isEraserMode && !isHighlighterMode && !isTextMode)` with the Lasso check too
    content = content.replace("if (!isEraserMode && !isHighlighterMode && !isTextMode)", "if (!isEraserMode && !isHighlighterMode && !isTextMode && !isLassoMode)")

    # Toolbar Eraser
    content = re.sub(
        r"isEraserMode = true\s*isHighlighterMode = false\s*isTextMode = false",
        "isEraserMode = true\n                                    isHighlighterMode = false\n                                    isTextMode = false\n                                    isLassoMode = false\n                                    commitLassoSelection()",
        content
    )

    # Toolbar Highlighter
    content = re.sub(
        r"isHighlighterMode = true\s*isEraserMode = false\s*isTextMode = false",
        "isHighlighterMode = true\n                                    isEraserMode = false\n                                    isTextMode = false\n                                    isLassoMode = false\n                                    commitLassoSelection()",
        content
    )

    # Toolbar Text
    content = re.sub(
        r"isTextMode = true\s*isHighlighterMode = false\s*isEraserMode = false",
        "isTextMode = true\n                                    isHighlighterMode = false\n                                    isEraserMode = false\n                                    isLassoMode = false\n                                    commitLassoSelection()",
        content
    )

    # Insert Lasso Icon in Toolbar
    lasso_button = """
                    Box {
                        IconButton(
                            onClick = {
                                commitActiveText()
                                if (!isLassoMode) {
                                    isLassoMode = true
                                    isEraserMode = false
                                    isHighlighterMode = false
                                    isTextMode = false
                                } else {
                                    commitLassoSelection()
                                    isLassoMode = false
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isLassoMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(LassoIcon, contentDescription = "Lasso", modifier = Modifier.size(20.dp))
                        }
                    }
"""
    if "Icon(LassoIcon" not in content:
        content = content.replace("Box {\n                        IconButton(\n                            onClick = {\n                                showColorMenu = true", lasso_button + "\n                    Box {\n                        IconButton(\n                            onClick = {\n                                showColorMenu = true")

    # Raycast helper function
    raycast_helper = """
private fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    var isInside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        if ((polygon[i].y > point.y) != (polygon[j].y > point.y) &&
            (point.x < (polygon[j].x - polygon[i].x) * (point.y - polygon[i].y) / (polygon[j].y - polygon[i].y) + polygon[i].x)
        ) {
            isInside = !isInside
        }
        j = i
    }
    return isInside
}
"""
    if "isPointInPolygon" not in content:
        content += "\n" + raycast_helper

    # pointerInput handler
    old_pointer = "val isStylus = change.type == PointerType.Stylus"
    new_pointer = """
                                            if (isLassoMode) {
                                                change.consume()
                                                if (change.pressed && !change.previousPressed) {
                                                    if (selectedLines.isNotEmpty() && lassoBoundingBox?.let { it.translate(selectionOffset).contains(change.position) } == true) {
                                                        // Tap inside bounding box -> start dragging
                                                    } else {
                                                        commitLassoSelection()
                                                        currentLassoPath = listOf(change.position)
                                                    }
                                                } else if (change.pressed && change.previousPressed) {
                                                    if (selectedLines.isNotEmpty()) {
                                                        selectionOffset += change.position - change.previousPosition
                                                    } else if (currentLassoPath != null) {
                                                        currentLassoPath = currentLassoPath!! + change.position
                                                    }
                                                } else if (!change.pressed && change.previousPressed) {
                                                    if (currentLassoPath != null) {
                                                        val path = currentLassoPath!!
                                                        // Close the path
                                                        val finalPath = path + path.first()
                                                        currentLassoPath = finalPath

                                                        // Find all inside
                                                        var minX = Float.POSITIVE_INFINITY
                                                        var minY = Float.POSITIVE_INFINITY
                                                        var maxX = Float.NEGATIVE_INFINITY
                                                        var maxY = Float.NEGATIVE_INFINITY

                                                        val toSelect = mutableListOf<DrawingLine>()
                                                        val remaining = mutableListOf<DrawingLine>()
                                                        for (line in drawingLines) {
                                                            if (line.points.any { p -> isPointInPolygon(p, finalPath) }) {
                                                                toSelect.add(line)
                                                                for (p in line.points) {
                                                                    if (p.x < minX) minX = p.x
                                                                    if (p.y < minY) minY = p.y
                                                                    if (p.x > maxX) maxX = p.x
                                                                    if (p.y > maxY) maxY = p.y
                                                                }
                                                            } else {
                                                                remaining.add(line)
                                                            }
                                                        }

                                                        if (toSelect.isNotEmpty()) {
                                                            drawingLines.clear()
                                                            drawingLines.addAll(remaining)
                                                            selectedLines.addAll(toSelect)
                                                            lassoBoundingBox = Rect(minX, minY, maxX, maxY)
                                                        } else {
                                                            currentLassoPath = null
                                                        }
                                                    }
                                                }
                                                continue
                                            }

                                            val isStylus = change.type == PointerType.Stylus
"""
    if "if (isLassoMode) {" not in content:
        content = content.replace(old_pointer, new_pointer)

    # Render selected lines and lasso outline
    render_lasso = """
                            // Draw actively selected lines
                            if (selectedLines.isNotEmpty()) {
                                androidx.compose.ui.graphics.drawscope.withTransform({
                                    translate(left = selectionOffset.x, top = selectionOffset.y)
                                }) {
                                    selectedLines.forEach { line ->
                                        val activeLineColor = if (line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White) strokeColor else line.color
                                        val finalColor = when {
                                            line.isEraser -> Color.Black
                                            line.isHighlighter -> activeLineColor.copy(alpha = 0.4f)
                                            else -> activeLineColor
                                        }
                                        val finalBlendMode = when {
                                            line.isEraser -> androidx.compose.ui.graphics.BlendMode.Clear
                                            line.isHighlighter -> androidx.compose.ui.graphics.BlendMode.Multiply
                                            else -> androidx.compose.ui.graphics.BlendMode.SrcOver
                                        }
                                        drawPath(
                                            path = line.toPath(),
                                            color = finalColor,
                                            style = Stroke(
                                                width = line.strokeWidth,
                                                cap = if (line.isHighlighter) StrokeCap.Square else StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            ),
                                            blendMode = finalBlendMode
                                        )
                                    }
                                }

                                lassoBoundingBox?.let { box ->
                                    drawRect(
                                        color = primaryColor.copy(alpha = 0.3f),
                                        topLeft = box.topLeft + selectionOffset,
                                        size = box.size,
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    )
                                }
                            }

                            // Draw lasso drawing path
                            currentLassoPath?.let { pathOffsets ->
                                if (selectedLines.isEmpty()) {
                                    val lp = Path()
                                    if (pathOffsets.isNotEmpty()) {
                                        lp.moveTo(pathOffsets.first().x, pathOffsets.first().y)
                                        for (i in 1 until pathOffsets.size) {
                                            lp.lineTo(pathOffsets[i].x, pathOffsets[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = lp,
                                        color = primaryColor,
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    )
                                }
                            }

                            // Draw active line"""
    if "// Draw actively selected lines" not in content:
        content = content.replace("// Draw active line", render_lasso)

    # Need to update drawingLines rendering to handle Text when rendering selected lines?
    # Or just ignore text for lasso right now? It says text has `if (line.text != null && line.points.isNotEmpty()) { return@forEach }`
    # We should let native Text compose handle selections or leave it out. The Text compose loop iterates `drawingLines`. We might need it to iterate `selectedLines` too.

    with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "w", encoding="utf-8") as f:
        f.write(content)

if __name__ == "__main__":
    main()

