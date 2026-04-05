@file:Suppress("unused")

package com.example.ex01

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.graphics.toColorInt

private val richTextTagRegex = Regex(
    pattern = """\[(/?)(b|i|u|s|strike|size|font|color|hl)(?:=([^]]+))?]""",
    options = setOf(RegexOption.IGNORE_CASE)
)

const val BOLD_OPEN_MARKER: Char = '\uE000'
const val BOLD_CLOSE_MARKER: Char = '\uE001'
const val ITALIC_OPEN_MARKER: Char = '\uE002'
const val ITALIC_CLOSE_MARKER: Char = '\uE003'
const val UNDERLINE_OPEN_MARKER: Char = '\uE004'
const val UNDERLINE_CLOSE_MARKER: Char = '\uE005'
const val STRIKETHROUGH_OPEN_MARKER: Char = '\uE006'
const val STRIKETHROUGH_CLOSE_MARKER: Char = '\uE007'
const val BULLET_OPEN_MARKER: Char = '\uE008'
const val BULLET_CLOSE_MARKER: Char = '\uE009'
private const val INDENT_UNIT = "    "

data class FormattingMarkerPair(
    val openMarker: Char,
    val closeMarker: Char,
    val tagName: String
)

private val BOLD_MARKERS = FormattingMarkerPair(BOLD_OPEN_MARKER, BOLD_CLOSE_MARKER, "b")
private val ITALIC_MARKERS = FormattingMarkerPair(ITALIC_OPEN_MARKER, ITALIC_CLOSE_MARKER, "i")
private val UNDERLINE_MARKERS = FormattingMarkerPair(UNDERLINE_OPEN_MARKER, UNDERLINE_CLOSE_MARKER, "u")
private val STRIKETHROUGH_MARKERS = FormattingMarkerPair(STRIKETHROUGH_OPEN_MARKER, STRIKETHROUGH_CLOSE_MARKER, "s")
private val BULLET_MARKERS = FormattingMarkerPair(BULLET_OPEN_MARKER, BULLET_CLOSE_MARKER, "bullet")

private data class RichTextTagToken(
    val endExclusive: Int,
    val tag: String? = null,
    val isClosing: Boolean = false,
    val argument: String = "",
    val isComplete: Boolean = false
)

private fun findMarkupBoundary(raw: String, start: Int): Int {
    var index = start + 1
    while (index < raw.length && !raw[index].isWhitespace() && raw[index] != '[') {
        index++
    }
    return index
}

private fun isPartialRichTextTagPrefix(candidate: String): Boolean {
    val lower = candidate.lowercase()
    return lower == "[b" ||
        lower == "[/b" ||
        lower == "[i" ||
        lower == "[/i" ||
        lower == "[size" ||
        lower == "[u" ||
        lower == "[/u" ||
        lower == "[s" ||
        lower == "[/s" ||
        lower == "[strike" ||
        lower == "[/strike" ||
        lower == "[/size" ||
        lower == "[font" ||
        lower == "[/font" ||
        lower == "[color" ||
        lower == "[/color" ||
        lower == "[hl" ||
        lower == "[/hl" ||
        lower.startsWith("[size=") ||
        lower.startsWith("[/size=") ||
        lower.startsWith("[s=") ||
        lower.startsWith("[/s=") ||
        lower.startsWith("[strike=") ||
        lower.startsWith("[/strike=") ||
        lower.startsWith("[i=") ||
        lower.startsWith("[/i=") ||
        lower.startsWith("[u=") ||
        lower.startsWith("[/u=") ||
        lower.startsWith("[font=") ||
        lower.startsWith("[/font=") ||
        lower.startsWith("[color=") ||
        lower.startsWith("[/color=") ||
        lower.startsWith("[hl=") ||
        lower.startsWith("[/hl=")
}

private fun parseRichTextTagToken(raw: String, start: Int): RichTextTagToken? {
    if (start >= raw.length || raw[start] != '[') return null

    val closingBracket = raw.indexOf(']', start + 1)
    if (closingBracket >= 0) {
        val match = richTextTagRegex.matchEntire(raw.substring(start, closingBracket + 1)) ?: return null
        return RichTextTagToken(
            endExclusive = closingBracket + 1,
            tag = match.groupValues[2].lowercase(),
            isClosing = match.groupValues[1] == "/",
            argument = match.groupValues[3],
            isComplete = true
        )
    }

    val boundary = findMarkupBoundary(raw, start)
    if (boundary <= start + 1) return null

    val candidate = raw.substring(start, boundary)
    return if (isPartialRichTextTagPrefix(candidate)) {
        RichTextTagToken(endExclusive = boundary)
    } else {
        null
    }
}

private fun rebuildValue(
    original: TextFieldValue,
    text: String,
    selection: TextRange
): TextFieldValue {
    val composition = original.composition?.let {
        TextRange(
            start = it.start.coerceIn(0, text.length),
            end = it.end.coerceIn(0, text.length)
        )
    }

    return TextFieldValue(
        text = text,
        selection = selection,
        composition = composition
    )
}

fun collapseEmptyBoldSpans(
    value: TextFieldValue,
    preserveCollapsedSelectionSpan: Boolean = true
): TextFieldValue {
    return collapseEmptyFormattingSpans(
        value = value,
        markerPairs = listOf(BOLD_MARKERS),
        preserveCollapsedSelectionSpan = preserveCollapsedSelectionSpan
    )
}

fun collapseEmptyFormattingSpans(
    value: TextFieldValue,
    markerPairs: List<FormattingMarkerPair> = listOf(BOLD_MARKERS, ITALIC_MARKERS, UNDERLINE_MARKERS, STRIKETHROUGH_MARKERS, BULLET_MARKERS),
    preserveCollapsedSelectionSpan: Boolean = true
): TextFieldValue {
    val raw = value.text
    if (raw.isEmpty()) return value

    data class OpenSpan(val index: Int, val marker: Char, var visibleChars: Int = 0)

    val keep = BooleanArray(raw.length) { true }
    val openSpans = ArrayDeque<OpenSpan>()
    val selectionStart = minOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)
    val selectionEnd = maxOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)
    val isCollapsedSelection = selectionStart == selectionEnd
    val openMarkers = markerPairs.associateBy { it.openMarker }
    val closeMarkers = markerPairs.associateBy { it.closeMarker }

    for (index in raw.indices) {
        when (raw[index]) {
            in openMarkers.keys -> openSpans.addLast(OpenSpan(index, raw[index]))
            in closeMarkers.keys -> {
                val matchingOpen = closeMarkers[raw[index]]?.openMarker
                val openIndex = openSpans.indexOfLast { it.marker == matchingOpen }
                val open = if (openIndex >= 0) openSpans.removeAt(openIndex) else null
                if (open == null || matchingOpen == null) {
                    keep[index] = false
                } else if (open.visibleChars == 0) {
                    val preserveEmptySpan = preserveCollapsedSelectionSpan && isCollapsedSelection &&
                        selectionStart > open.index && selectionStart <= index

                    if (!preserveEmptySpan) {
                        keep[open.index] = false
                        keep[index] = false
                    }
                }
            }
            else -> openSpans.forEach { it.visibleChars++ }
        }
    }

    openSpans.forEach { keep[it.index] = false }

    val originalToCleaned = IntArray(raw.length + 1)
    val cleaned = StringBuilder(raw.length)
    var cleanedIndex = 0

    for (index in raw.indices) {
        originalToCleaned[index] = cleanedIndex
        if (keep[index]) {
            cleaned.append(raw[index])
            cleanedIndex++
        }
    }
    originalToCleaned[raw.length] = cleanedIndex

    val start = value.selection.start.coerceIn(0, raw.length)
    val end = value.selection.end.coerceIn(0, raw.length)

    return rebuildValue(
        original = value,
        text = cleaned.toString(),
        selection = TextRange(originalToCleaned[start], originalToCleaned[end])
    )
}

fun normalizeRichTextMarkup(value: TextFieldValue): TextFieldValue {
    val raw = value.text
    if (raw.isEmpty()) return value
    val originalToCleaned = IntArray(raw.length + 1)
    val cleaned = StringBuilder(raw.length)
    var cleanedIndex = 0

    var rawIndex = 0
    while (rawIndex < raw.length) {
        val current = raw[rawIndex]

        if (current == BOLD_OPEN_MARKER || current == BOLD_CLOSE_MARKER ||
            current == ITALIC_OPEN_MARKER || current == ITALIC_CLOSE_MARKER ||
            current == UNDERLINE_OPEN_MARKER || current == UNDERLINE_CLOSE_MARKER ||
            current == STRIKETHROUGH_OPEN_MARKER || current == STRIKETHROUGH_CLOSE_MARKER ||
            current == BULLET_OPEN_MARKER || current == BULLET_CLOSE_MARKER
        ) {
            originalToCleaned[rawIndex] = cleanedIndex
            cleaned.append(current)
            cleanedIndex++
            rawIndex++
            continue
        }

        val token = parseRichTextTagToken(raw, rawIndex)
        if (token != null) {
            for (index in rawIndex until token.endExclusive.coerceAtMost(originalToCleaned.size)) {
                originalToCleaned[index] = cleanedIndex
            }

            if (token.isComplete && token.tag == "b") {
                cleaned.append(if (token.isClosing) BOLD_CLOSE_MARKER else BOLD_OPEN_MARKER)
                cleanedIndex++
            } else if (token.isComplete && token.tag == "i") {
                cleaned.append(if (token.isClosing) ITALIC_CLOSE_MARKER else ITALIC_OPEN_MARKER)
                cleanedIndex++
            } else if (token.isComplete && token.tag == "u") {
                cleaned.append(if (token.isClosing) UNDERLINE_CLOSE_MARKER else UNDERLINE_OPEN_MARKER)
                cleanedIndex++
            } else if (token.isComplete && (token.tag == "s" || token.tag == "strike")) {
                cleaned.append(if (token.isClosing) STRIKETHROUGH_CLOSE_MARKER else STRIKETHROUGH_OPEN_MARKER)
                cleanedIndex++
            }

            rawIndex = token.endExclusive
            continue
        }

        originalToCleaned[rawIndex] = cleanedIndex
        cleaned.append(current)
        cleanedIndex++
        rawIndex++
    }

    originalToCleaned[raw.length] = cleanedIndex

    val start = value.selection.start.coerceIn(0, raw.length)
    val end = value.selection.end.coerceIn(0, raw.length)

    return rebuildValue(
        original = value,
        text = cleaned.toString(),
        selection = TextRange(originalToCleaned[start], originalToCleaned[end])
    )
}

fun sanitizeRichTextTyping(value: TextFieldValue): TextFieldValue {
    return collapseEmptyFormattingSpans(normalizeRichTextMarkup(value))
}

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
    val allSelectedLinesBulleted = selectedLines.isNotEmpty() && selectedLines.all { line ->
        isBulletedLine(raw, line)
    }

    val originalToCleaned = IntArray(raw.length + 1)
    val rebuilt = StringBuilder(raw.length + selectedLines.size * 2)
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
        val isSelectedLine = lineIndex in firstSelectedLineIndex..lastSelectedLineIndex
        val lineIsBulleted = isBulletedLine(raw, line)

        when {
            !isSelectedLine -> {
                for (index in line.start until line.endExclusive) {
                    appendOriginal(index)
                }
                originalToCleaned[line.endExclusive] = cleanedIndex
            }

            allSelectedLinesBulleted && lineIsBulleted -> {
                originalToCleaned[line.start] = cleanedIndex
                for (index in (line.start + 1) until (line.endExclusive - 1)) {
                    appendOriginal(index)
                }
                if (line.endExclusive > line.start) {
                    originalToCleaned[line.endExclusive - 1] = cleanedIndex
                }
                originalToCleaned[line.endExclusive] = cleanedIndex
            }

            allSelectedLinesBulleted -> {
                for (index in line.start until line.endExclusive) {
                    appendOriginal(index)
                }
                originalToCleaned[line.endExclusive] = cleanedIndex
            }

            lineIsBulleted -> {
                for (index in line.start until line.endExclusive) {
                    appendOriginal(index)
                }
                originalToCleaned[line.endExclusive] = cleanedIndex
            }

            else -> {
                appendInserted(BULLET_OPEN_MARKER)
                for (index in line.start until line.endExclusive) {
                    appendOriginal(index)
                }
                originalToCleaned[line.endExclusive] = cleanedIndex
                appendInserted(BULLET_CLOSE_MARKER)
            }
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

private fun countLeadingSpaces(raw: String, start: Int, endExclusive: Int): Int {
    var count = 0
    while (start + count < endExclusive && raw[start + count] == ' ') {
        count++
    }
    return count
}

private data class RawLineRange(
    val start: Int,
    val endExclusive: Int,
    val newlineIndex: Int? = null
)

private fun collectLineRanges(raw: String): List<RawLineRange> {
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

private fun findLineIndexForOffset(lineRanges: List<RawLineRange>, offset: Int): Int {
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

private fun isBulletedLine(raw: String, line: RawLineRange): Boolean {
    return line.endExclusive - line.start >= 2 &&
        raw[line.start] == BULLET_OPEN_MARKER &&
        raw[line.endExclusive - 1] == BULLET_CLOSE_MARKER
}

private fun toggleFormatting(value: TextFieldValue, markers: FormattingMarkerPair): TextFieldValue {
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
                val shouldSplitAtWhitespaceBoundary = start > contentStart && raw[start - 1].isWhitespace()

                if (shouldSplitAtWhitespaceBoundary) {
                    var splitIndex = start
                    while (splitIndex > contentStart && raw[splitIndex - 1].isWhitespace()) {
                        splitIndex--
                    }

                    val formattedBefore = raw.substring(contentStart, splitIndex)
                    val plainAfter = raw.substring(splitIndex, enclosingMarkers.closeIndex)
                    val prefix = raw.substring(0, enclosingMarkers.openIndex)
                    val suffix = raw.substring(enclosingMarkers.closeIndex + 1)
                    val rebuilt = buildString(raw.length + 4) {
                        append(prefix)
                        if (formattedBefore.isNotEmpty()) {
                            append(markers.openMarker)
                            append(formattedBefore)
                            append(markers.closeMarker)
                        }
                        append(plainAfter)
                        append(suffix)
                    }

                    rebuildValue(normalized, rebuilt, TextRange(rebuilt.length - suffix.length))
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


private fun previousVisibleCharacterBefore(raw: String, offset: Int): Char? {
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

private fun findTrailingWhitespaceSplitIndex(
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

private fun stripFormattingMarkers(
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

private fun removeFormattingMarkers(
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

private fun countVisibleRichTextCharacters(raw: String, startIndex: Int, endIndexExclusive: Int): Int {
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

private fun hasVisibleRichTextCharacters(raw: String, startIndex: Int, endIndexExclusive: Int): Boolean {
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

private data class FormattingMarkerRange(
    val openIndex: Int,
    val closeIndex: Int
)

private data class FormattingSpanRange(
    val openIndex: Int,
    val closeIndex: Int
)

private fun collectFormattingSpanRanges(raw: String): List<FormattingSpanRange> {
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

private fun expandSelectionAwayFromNestedFormatting(
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

private fun findEnclosingFormattingMarkers(
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

fun isBoldFormattingActive(value: TextFieldValue): Boolean = isFormattingActive(value, BOLD_MARKERS)

fun isItalicFormattingActive(value: TextFieldValue): Boolean = isFormattingActive(value, ITALIC_MARKERS)

fun isUnderlineFormattingActive(value: TextFieldValue): Boolean = isFormattingActive(value, UNDERLINE_MARKERS)

fun isStrikethroughFormattingActive(value: TextFieldValue): Boolean = isFormattingActive(value, STRIKETHROUGH_MARKERS)

data class RichTextFormattingState(
    val boldActive: Boolean,
    val italicActive: Boolean,
    val underlineActive: Boolean,
    val strikethroughActive: Boolean,
    val bulletActive: Boolean,
    val indentActive: Boolean
)

private data class RichTextDepth(
    val boldDepth: Int,
    val italicDepth: Int,
    val underlineDepth: Int,
    val strikethroughDepth: Int,
    val bulletDepth: Int
)

private inline fun scanRichTextFormatting(
    raw: String,
    untilExclusive: Int = raw.length,
    onVisibleCharacter: (index: Int, depth: RichTextDepth) -> Unit = { _, _ -> }
): RichTextDepth {
    var boldDepth = 0
    var italicDepth = 0
    var underlineDepth = 0
    var strikethroughDepth = 0
    var bulletDepth = 0
    var rawIndex = 0
    val limit = untilExclusive.coerceIn(0, raw.length)

    while (rawIndex < raw.length && rawIndex < limit) {
        when (raw[rawIndex]) {
            BOLD_OPEN_MARKER -> {
                boldDepth++
                rawIndex++
            }
            BOLD_CLOSE_MARKER -> {
                if (boldDepth > 0) boldDepth--
                rawIndex++
            }
            ITALIC_OPEN_MARKER -> {
                italicDepth++
                rawIndex++
            }
            ITALIC_CLOSE_MARKER -> {
                if (italicDepth > 0) italicDepth--
                rawIndex++
            }
            UNDERLINE_OPEN_MARKER -> {
                underlineDepth++
                rawIndex++
            }
            UNDERLINE_CLOSE_MARKER -> {
                if (underlineDepth > 0) underlineDepth--
                rawIndex++
            }
            STRIKETHROUGH_OPEN_MARKER -> {
                strikethroughDepth++
                rawIndex++
            }
            STRIKETHROUGH_CLOSE_MARKER -> {
                if (strikethroughDepth > 0) strikethroughDepth--
                rawIndex++
            }
            BULLET_OPEN_MARKER -> {
                bulletDepth++
                rawIndex++
            }
            BULLET_CLOSE_MARKER -> {
                if (bulletDepth > 0) bulletDepth--
                rawIndex++
            }
            else -> {
                val token = parseRichTextTagToken(raw, rawIndex)
                if (token != null) {
                    if (token.isComplete) {
                        when (token.tag) {
                            "b" -> if (token.isClosing) {
                                if (boldDepth > 0) boldDepth--
                            } else {
                                boldDepth++
                            }
                            "i" -> if (token.isClosing) {
                                if (italicDepth > 0) italicDepth--
                            } else {
                                italicDepth++
                            }
                            "u" -> if (token.isClosing) {
                                if (underlineDepth > 0) underlineDepth--
                            } else {
                                underlineDepth++
                            }
                            "s", "strike" -> if (token.isClosing) {
                                if (strikethroughDepth > 0) strikethroughDepth--
                            } else {
                                strikethroughDepth++
                            }
                            "bullet" -> if (token.isClosing) {
                                if (bulletDepth > 0) bulletDepth--
                            } else {
                                bulletDepth++
                            }
                        }
                    }
                    rawIndex = token.endExclusive
                } else {
                    onVisibleCharacter(rawIndex, RichTextDepth(boldDepth, italicDepth, underlineDepth, strikethroughDepth, bulletDepth))
                    rawIndex++
                }
            }
        }
    }

    return RichTextDepth(boldDepth, italicDepth, underlineDepth, strikethroughDepth, bulletDepth)
}

private fun formattingDepthAtOffset(raw: String, offset: Int): RichTextDepth {
    return scanRichTextFormatting(raw, untilExclusive = offset)
}

fun richTextFormattingState(value: TextFieldValue): RichTextFormattingState {
    val raw = value.text
    if (raw.isEmpty()) return RichTextFormattingState(false, false, false, false, false, false)

    val start = minOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)
    val end = maxOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)

    if (start == end) {
        val depth = formattingDepthAtOffset(raw, start)
        return RichTextFormattingState(
            depth.boldDepth > 0,
            depth.italicDepth > 0,
            depth.underlineDepth > 0,
            depth.strikethroughDepth > 0,
            depth.bulletDepth > 0,
            isIndentedSelection(raw, start, end)
        )
    }

    var boldActive = false
    var italicActive = false
    var underlineActive = false
    var strikethroughActive = false
    var bulletActive = false

    scanRichTextFormatting(raw, untilExclusive = end) { index, depth ->
        if (index >= start) {
            if (depth.boldDepth > 0) boldActive = true
            if (depth.italicDepth > 0) italicActive = true
            if (depth.underlineDepth > 0) underlineActive = true
            if (depth.strikethroughDepth > 0) strikethroughActive = true
            if (depth.bulletDepth > 0) bulletActive = true
        }
    }

    return RichTextFormattingState(
        boldActive,
        italicActive,
        underlineActive,
        strikethroughActive,
        bulletActive,
        isIndentedSelection(raw, start, end)
    )
}

private fun isIndentedSelection(raw: String, start: Int, end: Int): Boolean {
    if (raw.isEmpty()) return false

    val lineRanges = collectLineRanges(raw)
    val selectionEndOffset = if (start == end) start else (end - 1).coerceAtLeast(start)
    val firstSelectedLineIndex = findLineIndexForOffset(lineRanges, start)
    val lastSelectedLineIndex = findLineIndexForOffset(lineRanges, selectionEndOffset)

    for (lineIndex in firstSelectedLineIndex..lastSelectedLineIndex) {
        val line = lineRanges[lineIndex]
        if (countLeadingSpaces(raw, line.start, line.endExclusive) > 0) {
            return true
        }
    }

    return false
}

private fun isFormattingActive(value: TextFieldValue, markers: FormattingMarkerPair): Boolean {
    val raw = value.text
    if (raw.isEmpty()) return false

    val start = minOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)
    val end = maxOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)

    if (start == end) {
        val depth = formattingDepthAtOffset(raw, start)
        return when (markers) {
            BOLD_MARKERS -> depth.boldDepth > 0
            ITALIC_MARKERS -> depth.italicDepth > 0
            UNDERLINE_MARKERS -> depth.underlineDepth > 0
            STRIKETHROUGH_MARKERS -> depth.strikethroughDepth > 0
            BULLET_MARKERS -> depth.bulletDepth > 0
            else -> false
        }
    }

    var active = false
    scanRichTextFormatting(raw, untilExclusive = end) { index, depth ->
        if (index >= start) {
            when (markers) {
                BOLD_MARKERS -> if (depth.boldDepth > 0) active = true
                ITALIC_MARKERS -> if (depth.italicDepth > 0) active = true
                UNDERLINE_MARKERS -> if (depth.underlineDepth > 0) active = true
                STRIKETHROUGH_MARKERS -> if (depth.strikethroughDepth > 0) active = true
                BULLET_MARKERS -> if (depth.bulletDepth > 0) active = true
            }
        }
    }

    return active
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

private fun stripRichTextMarkup(raw: String, originalToCleaned: IntArray? = null): String {
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

fun renderRichTextMarkup(raw: String): AnnotatedString {
    return richTextTransform(raw).text
}

fun richTextVisualTransformation(): VisualTransformation = object : VisualTransformation {
    private var cachedRaw: String? = null
    private var cachedResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val cached = cachedResult
        if (cachedRaw == raw && cached != null) {
            return cached
        }

        val transformed = richTextTransform(raw)
        cachedRaw = raw
        cachedResult = transformed
        return transformed
    }
}


private fun richTextTransform(raw: String): TransformedText {
    if (raw.isBlank()) {
        return TransformedText(AnnotatedString(raw), OffsetMapping.Identity)
    }

    val builder = AnnotatedString.Builder()
    val boldDepth = ArrayDeque<Unit>()
    val italicDepth = ArrayDeque<Unit>()
    val underlineDepth = ArrayDeque<Unit>()
    val strikeDepth = ArrayDeque<Unit>()
    val bulletDepth = ArrayDeque<Unit>()
    val colors = ArrayDeque<Color>()
    val highlights = ArrayDeque<Color>()
    val originalToTransformed = IntArray(raw.length + 1)
    val transformedToOriginal = mutableListOf<Int>()

    fun currentStyle(): SpanStyle {
        val decorations = buildList {
            if (underlineDepth.isNotEmpty()) add(TextDecoration.Underline)
            if (strikeDepth.isNotEmpty()) add(TextDecoration.LineThrough)
        }

        return SpanStyle(
            fontWeight = if (boldDepth.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (italicDepth.isNotEmpty()) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when (decorations.size) {
                0 -> TextDecoration.None
                1 -> decorations.first()
                else -> TextDecoration.combine(decorations)
            },
            color = colors.lastOrNull() ?: Color.Unspecified,
            background = highlights.lastOrNull() ?: Color.Unspecified
        )
    }

    var rawIndex = 0
    var visibleIndex = 0
    while (rawIndex < raw.length) {
        val current = raw[rawIndex]
        if (current == BOLD_OPEN_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            boldDepth.addLast(Unit)
            rawIndex++
            continue
        }
        if (current == BOLD_CLOSE_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            if (boldDepth.isNotEmpty()) boldDepth.removeLast()
            rawIndex++
            continue
        }
        if (current == ITALIC_OPEN_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            italicDepth.addLast(Unit)
            rawIndex++
            continue
        }
        if (current == ITALIC_CLOSE_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            if (italicDepth.isNotEmpty()) italicDepth.removeLast()
            rawIndex++
            continue
        }
        if (current == UNDERLINE_OPEN_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            underlineDepth.addLast(Unit)
            rawIndex++
            continue
        }
        if (current == UNDERLINE_CLOSE_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            if (underlineDepth.isNotEmpty()) underlineDepth.removeLast()
            rawIndex++
            continue
        }
        if (current == STRIKETHROUGH_OPEN_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            strikeDepth.addLast(Unit)
            rawIndex++
            continue
        }
        if (current == STRIKETHROUGH_CLOSE_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            if (strikeDepth.isNotEmpty()) strikeDepth.removeLast()
            rawIndex++
            continue
        }
        if (current == BULLET_OPEN_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            builder.append("• ")
            if (transformedToOriginal.isEmpty()) {
                transformedToOriginal.add(rawIndex)
            }
            transformedToOriginal.add(rawIndex)
            transformedToOriginal.add(rawIndex)
            visibleIndex += 2
            bulletDepth.addLast(Unit)
            rawIndex++
            continue
        }
        if (current == BULLET_CLOSE_MARKER) {
            originalToTransformed[rawIndex] = visibleIndex
            if (bulletDepth.isNotEmpty()) bulletDepth.removeLast()
            rawIndex++
            continue
        }

        val token = parseRichTextTagToken(raw, rawIndex)
        if (token != null) {
            for (index in rawIndex until token.endExclusive.coerceAtMost(originalToTransformed.size)) {
                originalToTransformed[index] = visibleIndex
            }

            if (token.isComplete) {
                val tag = token.tag.orEmpty()
                val arg = token.argument.trim()

                when (tag) {
                    "b" -> if (token.isClosing) {
                        if (boldDepth.isNotEmpty()) boldDepth.removeLast()
                    } else {
                        boldDepth.addLast(Unit)
                    }
                    "i" -> if (token.isClosing) {
                        if (italicDepth.isNotEmpty()) italicDepth.removeLast()
                    } else {
                        italicDepth.addLast(Unit)
                    }
                    "u" -> if (token.isClosing) {
                        if (underlineDepth.isNotEmpty()) underlineDepth.removeLast()
                    } else {
                        underlineDepth.addLast(Unit)
                    }
                    "s", "strike" -> if (token.isClosing) {
                        if (strikeDepth.isNotEmpty()) strikeDepth.removeLast()
                    } else {
                        strikeDepth.addLast(Unit)
                    }
                    "bullet" -> if (token.isClosing) {
                        if (bulletDepth.isNotEmpty()) bulletDepth.removeAt(bulletDepth.lastIndex)
                    } else {
                        if (bulletDepth.isEmpty()) {
                            builder.append("• ")
                            transformedToOriginal.add(rawIndex)
                            transformedToOriginal.add(rawIndex)
                            visibleIndex += 2
                        }
                        bulletDepth.addLast(Unit)
                    }
                    "color" -> if (token.isClosing) {
                        if (colors.isNotEmpty()) colors.removeLast()
                    } else {
                        parseMarkupColor(arg)?.let { colors.addLast(it) }
                    }
                    "hl" -> if (token.isClosing) {
                        if (highlights.isNotEmpty()) highlights.removeLast()
                    } else {
                        parseMarkupColor(arg)?.let { highlights.addLast(it) }
                    }
                }
            }

            rawIndex = token.endExclusive
            continue
        }

        originalToTransformed[rawIndex] = visibleIndex

        builder.pushStyle(currentStyle())
        builder.append(raw[rawIndex])
        builder.pop()

        if (transformedToOriginal.isEmpty()) {
            transformedToOriginal.add(rawIndex)
        }

        rawIndex++
        visibleIndex++

        transformedToOriginal.add(rawIndex)
    }

    if (transformedToOriginal.isEmpty()) {
        val firstFormattingOpen = raw.indexOfFirst {
            it == BOLD_OPEN_MARKER || it == ITALIC_OPEN_MARKER || it == UNDERLINE_OPEN_MARKER || it == STRIKETHROUGH_OPEN_MARKER || it == BULLET_OPEN_MARKER
        }
        transformedToOriginal.add(
            if (firstFormattingOpen >= 0) (firstFormattingOpen + 1).coerceAtMost(raw.length) else raw.length
        )
    }

    originalToTransformed[raw.length] = visibleIndex

    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            val clamped = offset.coerceIn(0, raw.length)
            return originalToTransformed[clamped]
        }

        override fun transformedToOriginal(offset: Int): Int {
            val clamped = offset.coerceIn(0, transformedToOriginal.lastIndex)
            return transformedToOriginal[clamped]
        }
    }

    return TransformedText(builder.toAnnotatedString(), offsetMapping)
}



fun parseMarkupColor(raw: String): Color? {
    if (raw.isBlank()) return null
    return runCatching { Color(raw.toColorInt()) }.getOrNull()
}


