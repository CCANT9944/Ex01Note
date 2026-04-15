import re
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    text = f.read()
# We need to modify the transformableState definition to include the bounding logic dynamically.
old_transformable = '''    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        zoomOffset += panChange
    }'''
new_transformable = '''    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        // Calculate max pan distance based on current scale and viewport
        val maxPanX = (viewportWidthPx * scale - viewportWidthPx).coerceAtLeast(0f) / 2f
        val maxPanY = (viewportHeightPx * scale - viewportHeightPx).coerceAtLeast(0f) / 2f
        val newOffset = zoomOffset + panChange
        zoomOffset = Offset(
            x = newOffset.x.coerceIn(-maxPanX, maxPanX),
            y = newOffset.y.coerceIn(-maxPanY, maxPanY)
        )
    }'''
text = text.replace(old_transformable, new_transformable)
old_bwc = '''                LaunchedEffect(availableHeight) {
                    if (pageHeightDp == 0.dp) {
                        pageHeightDp = availableHeight
                        pageHeightPx = with(density) { availableHeight.toPx() }
                    }
                }'''
new_bwc = '''                val availableWidth = this.maxWidth
                LaunchedEffect(availableWidth, availableHeight) {
                    viewportWidthPx = with(density) { availableWidth.toPx() }
                    viewportHeightPx = with(density) { availableHeight.toPx() }
                    if (pageHeightDp == 0.dp) {
                        pageHeightDp = availableHeight
                        pageHeightPx = viewportHeightPx
                    }
                }'''
text = text.replace(old_bwc, new_bwc)
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(text)
