
import re
with open("app/src/main/java/com/example/ex01/ui/editor/SNoteEditor.kt", "r", encoding="utf-8") as f:
    text = f.read()
# I see it injected `drawContext` BEFORE the Canvas { } or in the modifier maybe!
# Oh it is inside pointerInput! NO.
# Let's check the context exactly.
start = text.find("Canvas(")
while start != -1:
    end = text.find("{", start)
    print(text[start:end+100])
    start = text.find("Canvas(", start+1)

