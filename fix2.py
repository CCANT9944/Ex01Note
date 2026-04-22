lines = open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt').read()
lines = lines.replace('undoneLines.clear()', '')
lines = lines.replace('availableWidth - l.points.first().x', 'availableWidthPx - l.points.first().x')
open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt', 'w').write(lines)
