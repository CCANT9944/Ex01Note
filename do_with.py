
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'r', encoding='utf-8') as f:
    ed = f.read()
ed = ed.replace('line.color == Color.Unspecified   line.color == Color.Black   line.color == Color.White', 'line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White')
ed = ed.replace('line.color == Color.Unspecified  line.color == Color.Black  line.color == Color.White', 'line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White')
ed = ed.replace('line.color == Color.Unspecified line.color == Color.Black line.color == Color.White', 'line.color == Color.Unspecified || line.color == Color.Black || line.color == Color.White')
with open('app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt', 'w', encoding='utf-8') as f:
    f.write(ed)
