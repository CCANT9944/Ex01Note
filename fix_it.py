content = open("app/src/main/java/com/example/ex01/ui/components/NoteComponents.kt", "r", encoding="utf-8").read()
content = content.replace("NoteListStyles.NUMBERED -> \"\\.\"", "NoteListStyles.NUMBERED -> \"${index + 1}.\"")
open("app/src/main/java/com/example/ex01/ui/components/NoteComponents.kt", "w", encoding="utf-8").write(content)
