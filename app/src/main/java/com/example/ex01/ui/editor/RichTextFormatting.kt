@file:Suppress("unused")

package com.example.ex01.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun toggleBoldFormatting(value: TextFieldValue): TextFieldValue = toggleFormatting(value, BOLD_MARKERS)

fun toggleItalicFormatting(value: TextFieldValue): TextFieldValue = toggleFormatting(value, ITALIC_MARKERS)

fun toggleUnderlineFormatting(value: TextFieldValue): TextFieldValue = toggleFormatting(value, UNDERLINE_MARKERS)

fun toggleStrikethroughFormatting(value: TextFieldValue): TextFieldValue = toggleFormatting(value, STRIKETHROUGH_MARKERS)

fun toggleBulletFormatting(value: TextFieldValue): TextFieldValue {
    val normalized = normalizeRichTextMarkup(value)
    val raw = normalized.text
    if (raw.isEmpty()) return normalized

    val start = minOf(normalized.selection.start, normalized.selection.end).coerceIn(0, raw.length)
    val end = maxOf(normalized.selection.start, normalized.selection.end).coerceIn(0, raw.length)

    val lineRanges = collectLineRanges(raw)
    val selectionEndOffset = if (start == end) start else (end - 1).coerceAtLeast(start)
    val firstSelectedLineIndex = findLineIndexForOffset(lineRanges, start)
    val lastSelectedLineIndex = findLineIndexForOffset(lineRanges, selectionEndOffset)
    val selectedLines = lineRanges.subList(firstSelectedLineIndex, lastSelectedLineIndex + 1)
    
    val bulletedLinesBefore = BooleanArray(lineRanges.size) { index ->
        isBulletedLine(raw, lineRanges[index])
    }
    
    val allSelectedLinesBulletedBefore = selectedLines.isNotEmpty() && (firstSelectedLineIndex..lastSelectedLineIndex).all {
        bulletedLinesBefore[it]
    }

    val bulletedLinesAfter = BooleanArray(lineRanges.size) { index ->
        if (index in firstSelectedLineIndex..lastSelectedLineIndex) {
            !allSelectedLinesBulletedBefore
        } else {
            bulletedLinesBefore[index]
        }
    }

    val originalToCleaned = IntArray(raw.length + 1)
    val rebuilt = StringBuilder(raw.length + lineRanges.size * 2)
    var cleanedIndex = 0

    fun appendOriginal(index: Int) {
        originalToCleaned[index] = cleanedIndex
        rebuilt.append(raw[index])
        cleanedIndex++
    }

    fun appendInserted(char: Char) {
        rebuilt.append(char)
        cleanedIndex++
    }

    for (lineIndex in lineRanges.indices) {
        val line = lineRanges[lineIndex]
        val shouldBeBulleted = bulletedLinesAfter[lineIndex]

        originalToCleaned[line.start] = cleanedIndex
        
        if (shouldBeBulleted) {
            appendInserted(BULLET_OPEN_MARKER)
        }

        for (index in line.start until line.endExclusive) {
            // Strip any existing bullet markers
            if (raw[index] != BULLET_OPEN_MARKER && raw[index] != BULLET_CLOSE_MARKER) {
                appendOriginal(index)
            } else {
                originalToCleaned[index] = cleanedIndex
            }
        }

        originalToCleaned[line.endExclusive] = cleanedIndex

        if (shouldBeBulleted) {
            appendInserted(BULLET_CLOSE_MARKER)
        }

        if (line.newlineIndex != null) {
            rebuilt.append('\n')
            cleanedIndex++
        }
    }

    return rebuildValue(
        original = normalized,
        text = rebuilt.toString(),
        selection = TextRange(originalToCleaned[start], originalToCleaned[end])
    )
}

fun indentSelectedLines(value: TextFieldValue): TextFieldValue {
    return shiftSelectedLines(value, indent = true)
}

fun outdentSelectedLines(value: TextFieldValue): TextFieldValue {
    return shiftSelectedLines(value, indent = false)
}

private fun shiftSelectedLines(value: TextFieldValue, indent: Boolean): TextFieldValue {
    val raw = value.text
    if (raw.isEmpty()) return value

    val lineRanges = collectLineRanges(raw)
    val start = value.selection.start.coerceIn(0, raw.length)
    val end = value.selection.end.coerceIn(0, raw.length)
    val selectionEndOffset = if (start == end) start else (end - 1).coerceAtLeast(start)
    val firstSelectedLineIndex = findLineIndexForOffset(lineRanges, start)
    val lastSelectedLineIndex = findLineIndexForOffset(lineRanges, selectionEndOffset)

    val originalToCleaned = IntArray(raw.length + 1)
    val rebuilt = StringBuilder(raw.length + if (indent) (lastSelectedLineIndex - firstSelectedLineIndex + 1) * INDENT_UNIT.length else 0)
    var cleanedIndex = 0

    fun appendOriginal(index: Int) {
        originalToCleaned[index] = cleanedIndex
        rebuilt.append(raw[index])
        cleanedIndex++
    }

    fun appendIndentPrefix() {
        rebuilt.append(INDENT_UNIT)
        cleanedIndex += INDENT_UNIT.length
    }

    for (lineIndex in lineRanges.indices) {
        val line = lineRanges[lineIndex]
        val isSelectedLine = lineIndex in firstSelectedLineIndex..lastSelectedLineIndex

        if (!isSelectedLine) {
            for (index in line.start until line.endExclusive) {
                appendOriginal(index)
            }
            originalToCleaned[line.endExclusive] = cleanedIndex
        } else if (indent) {
            originalToCleaned[line.start] = cleanedIndex + INDENT_UNIT.length
            appendIndentPrefix()
            for (index in line.start until line.endExclusive) {
                appendOriginal(index)
            }
            originalToCleaned[line.endExclusive] = cleanedIndex
        } else {
            val leadingSpaces = countLeadingSpaces(raw, line.start, line.endExclusive).coerceAtMost(INDENT_UNIT.length)
            originalToCleaned[line.start] = cleanedIndex
            for (index in line.start until (line.start + leadingSpaces)) {
                originalToCleaned[index] = cleanedIndex
            }
            for (index in (line.start + leadingSpaces) until line.endExclusive) {
                appendOriginal(index)
            }
            originalToCleaned[line.endExclusive] = cleanedIndex
        }

        if (line.newlineIndex != null) {
            rebuilt.append('\n')
            cleanedIndex++
        }
    }

    originalToCleaned[raw.length] = cleanedIndex

    return rebuildValue(
        original = value,
        text = rebuilt.toString(),
        selection = TextRange(originalToCleaned[start], originalToCleaned[end])
    )
}

internal fun countLeadingSpaces(raw: String, start: Int, endExclusive: Int): Int {
    var count = 0
    while (start + count < endExclusive && raw[start + count] == ' ') {
        count++
    }
    return count
}

internal data class RawLineRange(
    val start: Int,
    val endExclusive: Int,
    val newlineIndex: Int? = null
)

internal fun collectLineRanges(raw: String): List<RawLineRange> {
    val ranges = mutableListOf<RawLineRange>()
    var lineStart = 0

    while (lineStart <= raw.length) {
        val newlineIndex = raw.indexOf('\n', lineStart)
        val lineEndExclusive = if (newlineIndex >= 0) newlineIndex else raw.length
        ranges.add(RawLineRange(lineStart, lineEndExclusive, if (newlineIndex >= 0) newlineIndex else null))

        if (newlineIndex < 0) break
        lineStart = newlineIndex + 1
    }

    return ranges
}

internal fun findLineIndexForOffset(lineRanges: List<RawLineRange>, offset: Int): Int {
    if (lineRanges.isEmpty()) return 0

    val clampedOffset = offset.coerceAtLeast(0)
    for (index in lineRanges.indices) {
        val line = lineRanges[index]
        if (clampedOffset >= line.start && clampedOffset <= line.endExclusive) {
            return index
        }
    }

    return lineRanges.lastIndex
}

internal fun isBulletedLine(raw: String, line: RawLineRange): Boolean {
    if (formattingDepthAtOffset(raw, line.start).bulletDepth > 0) return true
    for (i in line.start until line.endExclusive) {
        if (raw[i] == BULLET_OPEN_MARKER) return true
    }
    return false
}

internal fun toggleFormatting(value: TextFieldValue, markers: FormattingMarkerPair): TextFieldValue {
    val normalized = normalizeRichTextMarkup(value)
    val raw = normalized.text
    val start = minOf(normalized.selection.start, normalized.selection.end).coerceIn(0, raw.length)
    val end = maxOf(normalized.selection.start, normalized.selection.end).coerceIn(0, raw.length)

    return if (start == end) {
        val hasImmediateEmptySpan = start > 0 && start < raw.length &&
            raw[start - 1] == markers.openMarker && raw[start] == markers.closeMarker

        if (hasImmediateEmptySpan) {
            val newText = buildString(raw.length - 2) {
                append(raw.substring(0, start - 1))
                append(raw.substring(start + 1))
            }
            rebuildValue(normalized, newText, TextRange(start - 1))
        } else {
            val enclosingMarkers = findEnclosingFormattingMarkers(raw, start, end, markers)
                ?: if (start > 0 && raw[start - 1] == markers.closeMarker) {
                    findEnclosingFormattingMarkers(raw, start - 1, start - 1, markers)
                } else {
                    null
                }

            if (enclosingMarkers != null && start > enclosingMarkers.openIndex && start <= enclosingMarkers.closeIndex) {
                val contentStart = enclosingMarkers.openIndex + 1


                val shouldSplitAtWhitespaceBoundary = true // Always split the formatting span at the cursor

                if (shouldSplitAtWhitespaceBoundary) {
                    var splitIndex = start
                    while (splitIndex > contentStart && raw[splitIndex - 1].isWhitespace()) {
                        splitIndex--
                    }

                    val formattedBefore = raw.substring(contentStart, splitIndex)
                    val trailingWhitespaceIndex = start
                    var trailingWhitespaceEnd = start
                    while (trailingWhitespaceEnd < enclosingMarkers.closeIndex && raw[trailingWhitespaceEnd].isWhitespace()) {
                        trailingWhitespaceEnd++
                    }

                    val spaceBetween = raw.substring(splitIndex, start)
                    val remainingAfterCursor = raw.substring(start, enclosingMarkers.closeIndex)
                    val prefix = raw.substring(0, enclosingMarkers.openIndex)
                    val suffix = raw.substring(enclosingMarkers.closeIndex + 1)
                    val rebuilt = buildString(raw.length + 8) {
                        append(prefix)
                        if (formattedBefore.isNotEmpty()) {
                            append(markers.openMarker)
                            append(formattedBefore)
                            append(markers.closeMarker)
                        }
                        append(spaceBetween)
                        // At the cursor, if there is text after, we wrap it
                        if (remainingAfterCursor.isNotEmpty()) {
                            append(markers.openMarker)
                            append(remainingAfterCursor)
                            append(markers.closeMarker)
                        }
                        append(suffix)
                    }

                    val cursorOffset = prefix.length +
                                       (if (formattedBefore.isNotEmpty()) formattedBefore.length + 2 else 0) +
                                       spaceBetween.length

                    rebuildValue(normalized, rebuilt, TextRange(cursorOffset))
                } else if (start == enclosingMarkers.closeIndex) {
                    rebuildValue(normalized, raw, TextRange(start + 1))
                } else if (start == contentStart) {
                    rebuildValue(normalized, raw, TextRange(start - 1))
                } else {
                    val originalToCleaned = IntArray(raw.length + 1)
                    val cleanedText = removeFormattingMarkers(
                        raw = raw,
                        markers = markers,
                        startIndex = enclosingMarkers.openIndex,
                        endIndexInclusive = enclosingMarkers.closeIndex,
                        originalToCleaned = originalToCleaned
                    )

                    rebuildValue(normalized, cleanedText, TextRange(originalToCleaned[start]))
                }
            } else {
                val newText = buildString(raw.length + 2) {
                    append(raw.substring(0, start))
                    append(markers.openMarker)
                    append(markers.closeMarker)
                    append(raw.substring(start))
                }
                rebuildValue(normalized, newText, TextRange(start + 1))
            }
        }
    } else if (start == 0 && end == raw.length) {
        if (raw.indexOf(markers.openMarker) >= 0 || raw.indexOf(markers.closeMarker) >= 0) {
            val originalToCleaned = IntArray(raw.length + 1)
            val cleanedText = stripFormattingMarkers(raw, markers, originalToCleaned)
            rebuildValue(normalized, cleanedText, TextRange(originalToCleaned[0], originalToCleaned[raw.length]))
        } else {
            wrapSelectionWithTag(normalized, markers.openMarker.toString(), markers.closeMarker.toString())
        }
    } else {
        val wrappingMarkers = findEnclosingFormattingMarkers(raw, start, end, markers)

        if (wrappingMarkers != null) {
            val contentStart = wrappingMarkers.openIndex + 1
            val contentEnd = wrappingMarkers.closeIndex
                val selectedVisibleLength = countVisibleRichTextCharacters(raw, start, end)
                val fullVisibleLength = countVisibleRichTextCharacters(raw, contentStart, contentEnd)
                val selectionTailHasVisibleCharacters = hasVisibleRichTextCharacters(raw, end, contentEnd)

                if (selectedVisibleLength >= fullVisibleLength ||
                    (start <= contentStart && !selectionTailHasVisibleCharacters)
                ) {
                val originalToCleaned = IntArray(raw.length + 1)
                val cleaned = removeFormattingMarkers(
                    raw = raw,
                    markers = markers,
                    startIndex = wrappingMarkers.openIndex,
                    endIndexInclusive = wrappingMarkers.closeIndex,
                    originalToCleaned = originalToCleaned
                )

                rebuildValue(
                    normalized,
                    cleaned,
                    TextRange(originalToCleaned[start], originalToCleaned[end])
                )
            } else {
                val selectedStart = start.coerceIn(contentStart, contentEnd)
                val selectedEnd = end.coerceIn(contentStart, contentEnd)
                val beforeFormatted = raw.substring(contentStart, selectedStart)
                val selectedText = raw.substring(selectedStart, selectedEnd)
                val afterFormatted = raw.substring(selectedEnd, contentEnd)
                val prefix = raw.substring(0, wrappingMarkers.openIndex)
                val suffix = raw.substring(wrappingMarkers.closeIndex + 1)
                val cleaned = StringBuilder(raw.length + 4)
                cleaned.append(prefix)

                if (beforeFormatted.isNotEmpty()) {
                    cleaned.append(markers.openMarker)
                    cleaned.append(beforeFormatted)
                    cleaned.append(markers.closeMarker)
                }

                val selectionStart = cleaned.length
                cleaned.append(selectedText)
                val selectionEnd = cleaned.length

                if (afterFormatted.isNotEmpty()) {
                    cleaned.append(markers.openMarker)
                    cleaned.append(afterFormatted)
                    cleaned.append(markers.closeMarker)
                }

                cleaned.append(suffix)

                rebuildValue(normalized, cleaned.toString(), TextRange(selectionStart, selectionEnd))
            }
        } else {
            wrapSelectionWithTag(normalized, markers.openMarker.toString(), markers.closeMarker.toString())
        }
    }
}

internal fun previousVisibleCharacterBefore(raw: String, offset: Int): Char? {
    var index = offset - 1
    while (index >= 0) {
        when (raw[index]) {
            BOLD_OPEN_MARKER, BOLD_CLOSE_MARKER,
            ITALIC_OPEN_MARKER, ITALIC_CLOSE_MARKER,
            UNDERLINE_OPEN_MARKER, UNDERLINE_CLOSE_MARKER,
            BULLET_OPEN_MARKER, BULLET_CLOSE_MARKER -> index--
            else -> return raw[index]
        }
    }
    return null
}

internal fun findTrailingWhitespaceSplitIndex(
    raw: String,
    contentStart: Int,
    contentEndExclusive: Int
): Int? {
    var index = contentEndExclusive - 1
    var seenWhitespace = false

    while (index >= contentStart) {
        when (raw[index]) {
            BOLD_OPEN_MARKER, BOLD_CLOSE_MARKER,
            ITALIC_OPEN_MARKER, ITALIC_CLOSE_MARKER,
            UNDERLINE_OPEN_MARKER, UNDERLINE_CLOSE_MARKER -> index--
            else -> {
                if (raw[index].isWhitespace()) {
                    seenWhitespace = true
                    index--
                } else {
                    return if (seenWhitespace) index + 1 else null
                }
            }
        }
    }

    return if (seenWhitespace) contentStart else null
}

internal fun stripFormattingMarkers(
    raw: String,
    markers: FormattingMarkerPair,
    originalToCleaned: IntArray? = null
): String {
    val cleaned = StringBuilder(raw.length)
    var cleanedIndex = 0

    for (index in raw.indices) {
        originalToCleaned?.let { if (index < it.size) it[index] = cleanedIndex }
        when (raw[index]) {
            markers.openMarker, markers.closeMarker -> Unit
            else -> {
                cleaned.append(raw[index])
                cleanedIndex++
            }
        }
    }

    originalToCleaned?.let { if (raw.length < it.size) it[raw.length] = cleanedIndex }
    return cleaned.toString()
}

internal fun removeFormattingMarkers(
    raw: String,
    markers: FormattingMarkerPair,
    startIndex: Int,
    endIndexInclusive: Int,
    originalToCleaned: IntArray? = null
): String {
    val cleaned = StringBuilder(raw.length - 2)
    var cleanedIndex = 0
    val normalizedStart = startIndex.coerceAtLeast(0)
    val normalizedEnd = endIndexInclusive.coerceAtMost(raw.lastIndex)

    for (index in raw.indices) {
        originalToCleaned?.let { if (index < it.size) it[index] = cleanedIndex }
        if (index in normalizedStart..normalizedEnd &&
            (raw[index] == markers.openMarker || raw[index] == markers.closeMarker)
        ) {
            continue
        }

        cleaned.append(raw[index])
        cleanedIndex++
    }

    originalToCleaned?.let { if (raw.length < it.size) it[raw.length] = cleanedIndex }
    return cleaned.toString()
}

internal fun countVisibleRichTextCharacters(raw: String, startIndex: Int, endIndexExclusive: Int): Int {
    var count = 0
    val start = startIndex.coerceIn(0, raw.length)
    val end = endIndexExclusive.coerceIn(start, raw.length)

    for (index in start until end) {
        when (raw[index]) {
            BOLD_OPEN_MARKER, BOLD_CLOSE_MARKER,
            ITALIC_OPEN_MARKER, ITALIC_CLOSE_MARKER,
            UNDERLINE_OPEN_MARKER, UNDERLINE_CLOSE_MARKER,
            STRIKETHROUGH_OPEN_MARKER, STRIKETHROUGH_CLOSE_MARKER,
            BULLET_OPEN_MARKER, BULLET_CLOSE_MARKER -> Unit
            else -> count++
        }
    }

    return count
}

internal fun hasVisibleRichTextCharacters(raw: String, startIndex: Int, endIndexExclusive: Int): Boolean {
    val start = startIndex.coerceIn(0, raw.length)
    val end = endIndexExclusive.coerceIn(start, raw.length)

    for (index in start until end) {
        when (raw[index]) {
            BOLD_OPEN_MARKER, BOLD_CLOSE_MARKER,
            ITALIC_OPEN_MARKER, ITALIC_CLOSE_MARKER,
            UNDERLINE_OPEN_MARKER, UNDERLINE_CLOSE_MARKER,
            STRIKETHROUGH_OPEN_MARKER, STRIKETHROUGH_CLOSE_MARKER,
            BULLET_OPEN_MARKER, BULLET_CLOSE_MARKER -> Unit
            else -> return true
        }
    }

    return false
}

internal data class FormattingMarkerRange(
    val openIndex: Int,
    val closeIndex: Int
)

internal data class FormattingSpanRange(
    val openIndex: Int,
    val closeIndex: Int
)

internal fun collectFormattingSpanRanges(raw: String): List<FormattingSpanRange> {
    val spans = mutableListOf<FormattingSpanRange>()
    val openStack = ArrayDeque<Pair<FormattingMarkerPair, Int>>()

    for (index in raw.indices) {
        when (raw[index]) {
            BOLD_OPEN_MARKER -> openStack.addLast(BOLD_MARKERS to index)
            ITALIC_OPEN_MARKER -> openStack.addLast(ITALIC_MARKERS to index)
            UNDERLINE_OPEN_MARKER -> openStack.addLast(UNDERLINE_MARKERS to index)
            BOLD_CLOSE_MARKER -> {
                val openIndex = openStack.indexOfLast { it.first == BOLD_MARKERS }
                if (openIndex >= 0) {
                    val open = openStack.removeAt(openIndex)
                    spans.add(FormattingSpanRange(open.second, index))
                }
            }
            ITALIC_CLOSE_MARKER -> {
                val openIndex = openStack.indexOfLast { it.first == ITALIC_MARKERS }
                if (openIndex >= 0) {
                    val open = openStack.removeAt(openIndex)
                    spans.add(FormattingSpanRange(open.second, index))
                }
            }
            UNDERLINE_CLOSE_MARKER -> {
                val openIndex = openStack.indexOfLast { it.first == UNDERLINE_MARKERS }
                if (openIndex >= 0) {
                    val open = openStack.removeAt(openIndex)
                    spans.add(FormattingSpanRange(open.second, index))
                }
            }
        }
    }

    return spans
}

internal fun expandSelectionAwayFromNestedFormatting(
    raw: String,
    start: Int,
    end: Int,
    outerRange: FormattingMarkerRange
): Pair<Int, Int> {
    var adjustedStart = start
    var adjustedEnd = end
    val nestedSpans = collectFormattingSpanRanges(raw).filterNot {
        it.openIndex == outerRange.openIndex && it.closeIndex == outerRange.closeIndex
    }

    var changed: Boolean
    do {
        changed = false
        for (span in nestedSpans) {
            if (adjustedStart > span.openIndex && adjustedStart < span.closeIndex) {
                adjustedStart = span.openIndex + 1
                changed = true
            }
            if (adjustedEnd > span.openIndex && adjustedEnd < span.closeIndex) {
                adjustedEnd = span.closeIndex
                changed = true
            }
        }
    } while (changed)

    return adjustedStart to adjustedEnd
}

internal fun findEnclosingFormattingMarkers(
    raw: String,
    start: Int,
    end: Int,
    markers: FormattingMarkerPair
): FormattingMarkerRange? {
    var depth = 0
    var currentOpenIndex = -1
    var rawIndex = 0

    while (rawIndex < raw.length) {
        when (raw[rawIndex]) {
            markers.openMarker -> {
                if (depth == 0) {
                    currentOpenIndex = rawIndex
                }
                depth++
            }

            markers.closeMarker -> {
                if (depth > 0) {
                    depth--
                    if (depth == 0 && currentOpenIndex <= start && rawIndex >= end) {
                        return FormattingMarkerRange(currentOpenIndex, rawIndex)
                    }
                }
            }
        }
        rawIndex++
    }

    return null
}

fun stripColorAndHighlightMarkup(value: TextFieldValue): TextFieldValue {
    val raw = value.text
    if (raw.isEmpty()) return value

    val cleaned = StringBuilder(raw.length)
    val originalToCleaned = IntArray(raw.length + 1)
    var rawIndex = 0
    var cleanedIndex = 0

    while (rawIndex < raw.length) {
        val lower = raw.substring(rawIndex).lowercase()
        val isRemovedTag = raw[rawIndex] == '[' && (
            lower.startsWith("[color") || lower.startsWith("[/color") ||
                lower.startsWith("[hl") || lower.startsWith("[/hl") ||
                lower.startsWith("[size") || lower.startsWith("[/size") ||
                lower.startsWith("[font") || lower.startsWith("[/font")
            )

        if (isRemovedTag) {
            val closingBracket = raw.indexOf(']', rawIndex + 1)
            val endExclusive = if (closingBracket >= 0) closingBracket + 1 else raw.length

            for (index in rawIndex until endExclusive.coerceAtMost(originalToCleaned.size)) {
                originalToCleaned[index] = cleanedIndex
            }

            rawIndex = endExclusive
            continue
        }

        originalToCleaned[rawIndex] = cleanedIndex
        cleaned.append(raw[rawIndex])
        rawIndex++
        cleanedIndex++
    }

    originalToCleaned[raw.length] = cleanedIndex

    val start = value.selection.start.coerceIn(0, raw.length)
    val end = value.selection.end.coerceIn(0, raw.length)

    return TextFieldValue(
        text = cleaned.toString(),
        selection = TextRange(originalToCleaned[start], originalToCleaned[end])
    )
}

fun wrapSelectionWithTag(
    value: TextFieldValue,
    openTag: String,
    closeTag: String = openTag
): TextFieldValue {
    val start = minOf(value.selection.start, value.selection.end).coerceIn(0, value.text.length)
    val end = maxOf(value.selection.start, value.selection.end).coerceIn(0, value.text.length)

    if (start == end) return value

    val selectedText = value.text.substring(start, end)
    val newText = buildString(value.text.length + openTag.length + closeTag.length) {
        append(value.text.substring(0, start))
        append(openTag)
        append(selectedText)
        append(closeTag)
        append(value.text.substring(end))
    }
    return rebuildValue(
        original = value,
        text = newText,
        selection = TextRange(start + openTag.length + selectedText.length)
    )
}

fun clearRichTextFormatting(value: TextFieldValue): TextFieldValue {
    val start = minOf(value.selection.start, value.selection.end).coerceIn(0, value.text.length)
    val end = maxOf(value.selection.start, value.selection.end).coerceIn(0, value.text.length)

    return if (start == end) {
        val originalToCleaned = IntArray(value.text.length + 1)
        val cleanedText = stripRichTextMarkup(value.text, originalToCleaned)
        rebuildValue(
            original = value,
            text = cleanedText,
            selection = TextRange(originalToCleaned[start])
        )
    } else {
        val before = value.text.substring(0, start)
        val selected = value.text.substring(start, end)
        val after = value.text.substring(end)
        val cleanedSelected = stripRichTextMarkup(selected)

        rebuildValue(
            original = value,
            text = before + cleanedSelected + after,
            selection = TextRange(start, start + cleanedSelected.length)
        )
    }
}

internal fun stripRichTextMarkup(raw: String, originalToCleaned: IntArray? = null): String {
    val cleanedText = StringBuilder(raw.length)
    var rawIndex = 0
    var cleanedIndex = 0

    while (rawIndex < raw.length) {
        val current = raw[rawIndex]
        if (current == BOLD_OPEN_MARKER || current == BOLD_CLOSE_MARKER ||
            current == ITALIC_OPEN_MARKER || current == ITALIC_CLOSE_MARKER ||
            current == UNDERLINE_OPEN_MARKER || current == UNDERLINE_CLOSE_MARKER ||
            current == STRIKETHROUGH_OPEN_MARKER || current == STRIKETHROUGH_CLOSE_MARKER ||
            current == BULLET_OPEN_MARKER || current == BULLET_CLOSE_MARKER
        ) {
            originalToCleaned?.let { if (rawIndex < it.size) it[rawIndex] = cleanedIndex }
            rawIndex++
            continue
        }

        val token = parseRichTextTagToken(raw, rawIndex)
        if (token != null) {
            originalToCleaned?.let {
                for (index in rawIndex until token.endExclusive.coerceAtMost(it.size)) {
                    it[index] = cleanedIndex
                }
            }
            if (token.isComplete && token.tag == "u") {
                cleanedText.append(if (token.isClosing) UNDERLINE_CLOSE_MARKER else UNDERLINE_OPEN_MARKER)
                cleanedIndex++
            } else if (token.isComplete && (token.tag == "s" || token.tag == "strike")) {
                cleanedText.append(if (token.isClosing) STRIKETHROUGH_CLOSE_MARKER else STRIKETHROUGH_OPEN_MARKER)
                cleanedIndex++
            }
            rawIndex = token.endExclusive
            continue
        }

        originalToCleaned?.let { if (rawIndex < it.size) it[rawIndex] = cleanedIndex }

        cleanedText.append(raw[rawIndex])
        rawIndex++
        cleanedIndex++
    }

    originalToCleaned?.let { if (raw.length < it.size) it[raw.length] = cleanedIndex }

    return cleanedText.toString()
}
