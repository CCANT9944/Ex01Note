
import re
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    text = f.read()
# 1. The panning bounds logic is incorrect when TransformOrigin is missing! It uses Center (0.5) by default.
old_graphics_wrapper = """                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = zoomOffset.x
                                    translationY = zoomOffset.y
                                }"""
new_graphics_wrapper = """                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = zoomOffset.x
                                    translationY = zoomOffset.y
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f) // explicit
                                }"""
text = text.replace(old_graphics_wrapper, new_graphics_wrapper)
# 2. To fix blurry drawings without losing correct erasing
# We need to map gestures into scaled coordinates so we do not scale an offscreen bitmap.
# Wait, actually CompositingStrategy.Offscreen creates a static sized texture. If we scale it, it gets blurry.
# Let us revert the explicit graphicsLayer scale and use the internal Native Canvas scale!
# Actually, the user says "zoom on canvas and scroll very buggy".
# "detectTransformGestures" might battle with verticalScroll if it was there? No, we removed verticalScroll.
# What if we just apply the pan to translation and NO offscreen strategy, or use alpha = 0.99f again?
# Users said: "zoom on canvas and scroll very buggy and draws i added before not readable" -> unreadable means BLURRY.
# Let us restore alpha = 0.99f. It might have been better? No, alpha=0.99f makes it blurry too!
# To keep vector quality, the scale must be applied AT THE DRAWING phase via `withTransform({ scale(scale, scale) })` instead of a Modifier `graphicsLayer`, OR we just remove compositingStrategy scaling and apply scaling inside the `graphicsLayer` of individual chunks? No, if we use `graphicsLayer { scaleX=scale; scaleY=scale }` without `CompositingStrategy.Offscreen`, it redraws vector bounds crisply!

