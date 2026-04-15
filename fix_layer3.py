
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    text = f.read()
start = text.find("                            // Draw actively selected lines")
if start != -1:
    print(text[start-500:start+100])
print("\n---")
layer_idx = text.find("drawContext.canvas.saveLayer")
if layer_idx != -1:
    print(text[layer_idx-300:layer_idx+300])

