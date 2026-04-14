import re
content = open('app/src/main/java/com/example/ex01/ui/components/NoteComponents.kt', 'r', encoding='utf-8').read()
# remove animateContentSize
content = re.sub(r'\s*\.animateContentSize\(\)', '', content)
# remove produceState and renderRichTextMarkup completely, just use AnnotatedString
pattern1 = r'val renderedPreviewBody by produceState<AnnotatedString\?>.*?renderRichTextMarkup\(previewBody\).*?\}'
content = re.sub(pattern1, '', content, flags=re.DOTALL)
# replace usages of renderedPreviewBody ?: AnnotatedString(previewBody) with AnnotatedString(previewBody)
content = content.replace('renderedPreviewBody ?: AnnotatedString(previewBody)', 'AnnotatedString(previewBody)')
# Wait, there's another occurrence of renderedPreviewBody = renderRichTextMarkup(previewBody) ?
# In my python script, I accidentally had:
# val renderedPreviewBody = renderRichTextMarkup(previewBody)
pattern2 = r'val renderedPreviewBody = renderRichTextMarkup\(previewBody\)'
content = re.sub(pattern2, '', content)
# remove leading spaces that might be left over from deleted lines
content = re.sub(r'\n\s*\n\s*if \(previewBody\.isNotBlank', '\n                        if (previewBody.isNotBlank', content)
open('app/src/main/java/com/example/ex01/ui/components/NoteComponents.kt', 'w', encoding='utf-8').write(content)
print('Optimized NoteCard')
