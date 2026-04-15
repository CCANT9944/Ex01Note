
import re
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    text = f.read()
# 1. Clean up the messy unclosed saveLayer
text = text.replace("                              // Push layer for true transparent erasing without blur\n                              drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), androidx.compose.ui.graphics.Paint())", "")
text = text.replace("                                                      drawContext.canvas.restore()", "")
# 2. Remove the old background canvas block completely.
bg_canvas_regex = r"""                            // Background & Dividers Layer\s*Canvas\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.height\(pageHeightDp \* pageCount\)\s*\) \{\s*// Fill whole background first\s*drawRect\(color = eraserColor, size = size\)\s*drawPageLayout\(pageCount, pageHeightPx\)\s*\}"""
text = re.sub(bg_canvas_regex, "", text)
# 3. Inside the Strokes canvas, right at the start of onDraw (after modifier/pointerInput...)
# We can just put it at the beginning of the draw scope!
# The draw scope starts after `) {` of the Canvas. Let's find `// Draw saved lines`
inject_bg = """                            // Draw background first
                            drawRect(color = eraserColor, size = size)
                            // Draw saved lines"""
text = text.replace("                            // Draw saved lines", inject_bg)
# 4. Change eraser blending
old_blend = """                                val finalBlendMode = when {
                                    line.isEraser -> androidx.compose.ui.graphics.BlendMode.Clear
                                    line.isHighlighter -> androidx.compose.ui.graphics.BlendMode.Multiply
                                    else -> androidx.compose.ui.graphics.BlendMode.SrcOver
                                }"""
new_blend = """                                val finalBlendMode = when {
                                    line.isHighlighter -> androidx.compose.ui.graphics.BlendMode.Multiply
                                    else -> androidx.compose.ui.graphics.BlendMode.SrcOver
                                }"""
text = text.replace(old_blend, new_blend)
old_color = """                                val finalColor = when {
                                    line.isEraser -> Color.Black
                                    line.isHighlighter -> activeLineColor.copy(alpha = 0.4f)
                                    else -> activeLineColor
                                }"""
new_color = """                                val finalColor = when {
                                    line.isEraser -> eraserColor
                                    line.isHighlighter -> activeLineColor.copy(alpha = 0.4f)
                                    else -> activeLineColor
                                }"""
text = text.replace(old_color, new_color)
# For ACTIVE stroke erasing:
old_active_color = """                                        val finalColor = when {
                                            line.isEraser -> Color.Black
                                            line.isHighlighter -> activeLineColor.copy(alpha = 0.4f)
                                            else -> activeLineColor
                                        }"""
new_active_color = """                                        val finalColor = when {
                                            line.isEraser -> eraserColor
                                            line.isHighlighter -> activeLineColor.copy(alpha = 0.4f)
                                            else -> activeLineColor
                                        }"""
text = text.replace(old_active_color, new_active_color)
# 5. At the end of the drawing scope, right before // Render Static Text natively via Compose
# wait, // Render Static Text natively via Compose is OUTSIDE the Canvas!
# The end of Canvas is just before that! Let's inject drawPageLayout(pageCount, pageHeightPx)
# Wait, let's find the exact end of Canvas block
inject_layout = """                            // Draw notebook dividers and numbers on top so strokes pass underneath neatly
                            drawPageLayout(pageCount, pageHeightPx)
                        }
                        // Render Static Text natively via Compose"""
text = text.replace("                        }\n\n                        // Render Static Text natively via Compose", inject_layout)
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "w", encoding="utf-8") as f:
    f.write(text)

