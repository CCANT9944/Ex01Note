import re
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    text = f.read()
# Make sure imports exist
if "import androidx.compose.foundation.gestures.transformable" not in text:
    text = text.replace("import androidx.compose.foundation.gestures.detectTransformGestures", "import androidx.compose.foundation.gestures.detectTransformGestures\nimport androidx.compose.foundation.gestures.transformable\nimport androidx.compose.foundation.gestures.rememberTransformableState\nimport androidx.compose.ui.graphics.TransformOrigin")
if "var scale by remember" not in text:
    state_vars = '''    var pageHeightPx by remember { mutableFloatStateOf(0f) }
    var pageHeightDp by remember { mutableStateOf(0.dp) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += panChange
    }
    LaunchedEffect(serializedBody, pageHeightPx) {'''
    text = text.replace('''    var pageHeightPx by remember { mutableFloatStateOf(0f) }
    var pageHeightDp by remember { mutableStateOf(0.dp) }
    LaunchedEffect(serializedBody, pageHeightPx) {''', state_vars)
old_box = '''                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (pageHeightDp > 0.dp) {'''
new_box = '''                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = transformableState)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (pageHeightDp > 0.dp) {'''
text = text.replace(old_box, new_box)
old_end_box = '''                    }
                }
                FilledIconButton('''
new_end_box = '''                        }
                    }
                }
                FilledIconButton('''
text = text.replace(old_end_box, new_end_box)
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(text)
