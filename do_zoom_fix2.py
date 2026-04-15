import re
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    text = f.read()
pattern = r'''(\s+Box\(\s+modifier = Modifier\s+\.fillMaxSize\(\)\s+\.transformable\(state = transformableState\)\s+\.graphicsLayer\([^)]+\)\s+\)\s+\{\s+Box\(\s+modifier = Modifier\s+\.fillMaxSize\(\)\s+\.verticalScroll\(rememberScrollState\(\)\)\s+\) \{)'''
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
text = re.sub(pattern, new_box, text)
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(text)
