
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    text = f.read()
start = text.find("fun DrawScope.drawPageLayout")
if start == -1:
    start = text.find("drawPageLayout")
if start != -1:
    print(text[start:start+1000])

