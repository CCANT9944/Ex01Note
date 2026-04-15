
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    text = f.read()
# find Box with graphicsLayer
start_idx = text.find("if (pageHeightDp > 0.dp) {")
if start_idx != -1:
    print(text[start_idx:start_idx+1000])
print("\n---")
# find canvas
canvas_idx = text.find("Canvas(", start_idx)
if canvas_idx != -1:
    print(text[canvas_idx:canvas_idx+500])

