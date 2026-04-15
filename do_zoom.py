import re
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    text = f.read()
# Add gestures import
if "import androidx.compose.foundation.gestures.detectTransformGestures" not in text:
    text = text.replace("import androidx.compose.ui.input.pointer.pointerInput", "import androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.foundation.gestures.detectTransformGestures\nimport androidx.compose.ui.graphics.TransformOrigin")
# Add state variables
state_vars = '''    var pageHeightPx by remember { mutableFloatStateOf(0f) }
    var pageHeightDp by remember { mutableStateOf(0.dp) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(serializedBody, pageHeightPx) {'''
if "var scale by remember" not in text:
    text = text.replace('''    var pageHeightPx by remember { mutableFloatStateOf(0f) }
    var pageHeightDp by remember { mutableStateOf(0.dp) }
    LaunchedEffect(serializedBody, pageHeightPx) {''', state_vars)
# Replace verticalScroll Box with Transform Gesture Box and scaled inner wrapper
old_box = '''                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (pageHeightDp > 0.dp) {'''
new_box = '''                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageHeightPx, pageCount, scale) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                val newOffset = offset + pan
                                val maxW = (size.width * scale - size.width).coerceAtLeast(0f)
                                val maxH = (pageHeightPx * pageCount * scale - size.height).coerceAtLeast(0f)
                                offset = Offset(
                                    x = newOffset.x.coerceIn(-maxW / 2f, maxW / 2f),
                                    y = newOffset.y.coerceIn(-maxH, 0f)
                                )
                            }
                        }
                ) {
                    if (pageHeightDp > 0.dp) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pageHeightDp * pageCount)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                    transformOrigin = TransformOrigin(0f, 0f)
                                )
                        ) {'''
if "detectTransformGestures" not in text:
    text = text.replace(old_box, new_box)
# Adjust offset blocks for Text and TextField
old_text_offset = '''                                        .offset {
                                            IntOffset(
                                                kotlin.math.round(line.points.first().x).toInt(),
                                                kotlin.math.round(line.points.first().y).toInt()
                                            )
                                        }'''
new_text_offset = '''                                        .offset {
                                            IntOffset(
                                                kotlin.math.round(line.points.first().x).toInt(),
                                                kotlin.math.round(line.points.first().y).toInt()
                                            )
                                        }'''
# Adjust end bounds of Box
old_end_box = '''                    }
                }
                FilledIconButton('''
new_end_box = '''                        }
                    }
                }
                FilledIconButton('''
if "detectTransformGestures" in new_box and "transformOrigin = TransformOrigin" not in old_end_box:
    text = text.replace(old_end_box, new_end_box)
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(text)
