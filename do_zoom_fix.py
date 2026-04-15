import re
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    text = f.read()
# Add a boolean state for tracking two fingers
if "import androidx.compose.ui.input.pointer.PointerEventPass" not in text:
    text = text.replace("import androidx.compose.ui.input.pointer.PointerType", "import androidx.compose.ui.input.pointer.PointerType\nimport androidx.compose.ui.input.pointer.PointerEventPass")
if "var isMultiTouch by remember" not in text:
    state_vars = '''    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    var isMultiTouch by remember { mutableStateOf(false) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->'''
    text = text.replace('''    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->''', state_vars)
old_box = '''                  Box(
                      modifier = Modifier
                          .fillMaxSize()
                          .transformable(state = transformableState)
                          .graphicsLayer(
                              scaleX = scale,
                              scaleY = scale,
                              translationX = zoomOffset.x,
                              translationY = zoomOffset.y,
                              transformOrigin = TransformOrigin(0.5f, 0.5f)
                          )
                  ) {
                      Box(
                          modifier = Modifier
                              .fillMaxSize()
                              .verticalScroll(rememberScrollState())
                      ) {'''
new_box = '''                  Box(
                      modifier = Modifier
                          .fillMaxSize()
                          .pointerInput(Unit) {
                              awaitPointerEventScope {
                                  while (true) {
                                      val event = awaitPointerEvent(PointerEventPass.Initial)
                                      isMultiTouch = event.changes.count { it.pressed } > 1
                                  }
                              }
                          }
                          .transformable(state = transformableState)
                          .graphicsLayer(
                              scaleX = scale,
                              scaleY = scale,
                              translationX = zoomOffset.x,
                              translationY = zoomOffset.y,
                              transformOrigin = TransformOrigin(0.5f, 0.5f)
                          )
                  ) {
                      Box(
                          modifier = Modifier
                              .fillMaxSize()
                              .verticalScroll(
                                  state = rememberScrollState(),
                                  enabled = scale == 1f && !isMultiTouch
                              )
                      ) {'''
text = text.replace(old_box, new_box)
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(text)
