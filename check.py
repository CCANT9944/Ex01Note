import sys
content = open('app/src/main/java/com/example/ex01/ui/editor/snote/SNoteEditor.kt', encoding='utf-8').read()
if 'density' in content:
    print('yes')
