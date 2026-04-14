content = open("app/src/main/java/com/example/ex01/ui/components/NoteComponents.kt", "r", encoding="utf-8").read()
content = content.replace("@OptIn(ExperimentalMaterial3Api::class)\n@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)", "@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)")
open("app/src/main/java/com/example/ex01/ui/components/NoteComponents.kt", "w", encoding="utf-8").write(content)
