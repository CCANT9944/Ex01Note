lines = open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt').read()
import re
# 1. commitLassoSelection
lines = lines.replace('''                    strokeWidth = l.strokeWidth * selectionScale
                )
            }
            drawingLines.addAll(finalizedLines)''', '''                    strokeWidth = l.strokeWidth * selectionScale
                )
            }
            if (selectionDragOffset != Offset.Zero || selectionScale != 1f) {
                viewModel.preLassoState?.let { viewModel.pushUndoState(it) }
            }
            viewModel.preLassoState = null
            drawingLines.addAll(finalizedLines)''')
# 2. new stroke 1
lines = lines.replace('''                                                    if (pageHeightPx > 0f && relY > pageHeightPx - gapPx) {
                                                        drawingLines.add(currentProperties.copy(points = currentPath!!))
                                                        undoneLines.clear()''', '''                                                    if (pageHeightPx > 0f && relY > pageHeightPx - gapPx) {
                                                        viewModel.pushUndoState()
                                                        drawingLines.add(currentProperties.copy(points = currentPath!!))''')
# 3. hitIndex != -1
lines = lines.replace('''                                                    if (hitIndex != -1) {
                                                        val hitLine = drawingLines.removeAt(hitIndex)''', '''                                                    if (hitIndex != -1) {
                                                        viewModel.preEditTextState = drawingLines.toList()
                                                        val hitLine = drawingLines.removeAt(hitIndex)''')
# 4. lasso path finish
lines = lines.replace('''                                                        if (newSelection.isNotEmpty()) {
                                                            drawingLines.clear()
                                                            drawingLines.addAll(remainingLines)
                                                            selectedLines.addAll(newSelection)''', '''                                                        if (newSelection.isNotEmpty()) {
                                                            viewModel.preLassoState = drawingLines.toList()
                                                            drawingLines.clear()
                                                            drawingLines.addAll(remainingLines)
                                                            selectedLines.addAll(newSelection)''')
# 5. stroke 2
lines = lines.replace('''                                                } else if (!isTextMode && currentPath != null) {
                                                    drawingLines.add(currentProperties.copy(points = currentPath!!))
                                                    undoneLines.clear()''', '''                                                } else if (!isTextMode && currentPath != null) {
                                                    viewModel.pushUndoState()
                                                    drawingLines.add(currentProperties.copy(points = currentPath!!))''')
# 6. delete lasso selection
lines = lines.replace('''                                        onClick = {
                                            drawingLines.removeAll(selectedLines)
                                            selectedLines.clear()
                                            showLassoMenu = false
                                        }''', '''                                        onClick = {
                                            viewModel.preLassoState?.let { viewModel.pushUndoState(it) }
                                            viewModel.preLassoState = null
                                            selectedLines.clear()
                                            showLassoMenu = false
                                        }''')
# 7. clear all
lines = lines.replace('''                            drawingLines.clear()
                            undoneLines.clear()''', '''                            viewModel.pushUndoState()
                            drawingLines.clear()''')
open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt', 'w').write(lines)
