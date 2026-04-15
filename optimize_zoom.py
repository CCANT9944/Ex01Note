
import re
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    content = f.read()
# Replace block 1: object graphicsLayer with lambda graphicsLayer on wrapper Box
old_wrapper_graphics = """                                .height(pageHeightDp * pageCount)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = zoomOffset.x,
                                    translationY = zoomOffset.y
                                )"""
new_wrapper_graphics = """                                .height(pageHeightDp * pageCount)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = zoomOffset.x
                                    translationY = zoomOffset.y
                                }"""
content = content.replace(old_wrapper_graphics, new_wrapper_graphics)
# Replace block 2: graphicsLayer(alpha = 0.99f)
old_canvas_graphics = """.graphicsLayer(alpha = 0.99f) // Force offscreen layer to support true transparent erasing"""
new_canvas_graphics = """.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen } // Force offscreen layer to support true transparent erasing"""
content = content.replace(old_canvas_graphics, new_canvas_graphics)
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "w", encoding="utf-8") as f:
    f.write(content)

