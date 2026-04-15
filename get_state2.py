
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    text = f.read()
start = text.find("                            // Draw saved lines")
if start != -1:
    print(text[start-100:start+1500])

