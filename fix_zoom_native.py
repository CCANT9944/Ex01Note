
import re
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    text = f.read()
# Let us remove the scaleX and scaleY from the wrapper Box
# And instead pass it to the graphicsLayer of the Canvas, Wait, NO, if we put CompositingStrategy.Offscreen, it ALWAYS rasters at intrinsic size before applying scale to the whole node.
# To properly erase with a BlendMode.Clear while zoomed, without blur:
# Instead of Offscreen on a Box that is big, we can use `withTransform({ scale(scale, scale); translate(zoomOffset) })` inside `Canvas {}`!
# BUT then touch pointer coordinates have to be transformed back!
# Touch coordinates currently:
# change.position is in unscaled Box coordinates because pointerInput is inside the wrapper. Oh wait, pointerInput is ON the wrapper? No, pointerInput for zoom is above the wrapper. Touch for drawing is inside the wrapper.
# If pointerInput for drawing is inside a Composable with `.graphicsLayer { scaleX=scale }`, Compose AUTOMATICALLY scales the `change.position` so you get the unscaled local coordinates!! This is magical.
# BUT, if we have `.graphicsLayer { compositingStrategy = Offscreen }`, wait, ANY rotation/scale on a layer with Offscreen Strategy causes the layer to be rasterized at unscaled size and then the texture is stretched.
# Fix: Don't use Offscreen or alpha=0.99f! Is there another way to erase? 
# Wait, Android Canvas BlendMode.Clear needs a layer.
# But `withTransform` + `saveLayer` inside the draw phase works natively! YES! `drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())`
pattern_offscreen = r"\s*\.graphicsLayer \{ compositingStrategy = androidx\.compose\.ui\.graphics\.CompositingStrategy\.Offscreen \}.*?\n"
text = re.sub(pattern_offscreen, "\n", text)
# add saveLayer inside the strokes canvas
canvas_strokes_start = text.find("// Drawing Storkes Layer")
canvas_drawing_idx = text.find("drawingLines.forEach { line ->", canvas_strokes_start)
if canvas_drawing_idx != -1:
    # insert saveLayer
    layer_push = """
                              // Push layer for true transparent erasing without blur
                              drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), androidx.compose.ui.graphics.Paint())
                              """
    text = text[:canvas_drawing_idx] + layer_push + text[canvas_drawing_idx:]
    # insert restore
    render_static_text_start = text.find("// Render Static Text natively via Compose", canvas_drawing_idx)
    text = text[:render_static_text_start] + "                              drawContext.canvas.restore()\n\n" + """                              """ + text[render_static_text_start:]
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "w", encoding="utf-8") as f:
    f.write(text)

