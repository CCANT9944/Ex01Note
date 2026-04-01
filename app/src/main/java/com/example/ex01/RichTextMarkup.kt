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
import androidx.core.graphics.toColorInt

private val richTextTagRegex = Regex(
    pattern = """\[(/?)(b|i|size|font|color|hl)(?:=([^]]+))?]""",
    options = setOf(RegexOption.IGNORE_CASE)
)

const val BOLD_OPEN_MARKER: Char = '\uE000'
const val BOLD_CLOSE_MARKER: Char = '\uE001'
const val ITALIC_OPEN_MARKER: Char = '\uE002'
const val ITALIC_CLOSE_MARKER: Char = '\uE003'

data class FormattingMarkerPair(
    val openMarker: Char,
    val closeMarker: Char,
    val tagName: String
)

private val BOLD_MARKERS = FormattingMarkerPair(BOLD_OPEN_MARKER, BOLD_CLOSE_MARKER, "b")
private val ITALIC_MARKERS = FormattingMarkerPair(ITALIC_OPEN_MARKER, ITALIC_CLOSE_MARKER, "i")

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
        lower == "[/size" ||
        lower == "[font" ||
        lower == "[/font" ||
        lower == "[color" ||
        lower == "[/color" ||
        lower == "[hl" ||
        lower == "[/hl" ||
        lower.startsWith("[size=") ||
        lower.startsWith("[/size=") ||
        lower.startsWith("[i=") ||
        lower.startsWith("[/i=") ||
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
    markerPairs: List<FormattingMarkerPair> = listOf(BOLD_MARKERS, ITALIC_MARKERS),
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
            current == ITALIC_OPEN_MARKER || current == ITALIC_CLOSE_MARKER
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

            if (enclosingMarkers != null && start > enclosingMarkers.openIndex && start <= enclosingMarkers.closeIndex) {
                val originalToCleaned = IntArray(raw.length + 1)
                val cleanedText = removeFormattingMarkers(
                    raw = raw,
                    openIndex = enclosingMarkers.openIndex,
                    closeIndex = enclosingMarkers.closeIndex,
                    originalToCleaned = originalToCleaned
                )

                rebuildValue(normalized, cleanedText, TextRange(originalToCleaned[start]))
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
            val (safeStart, safeEnd) = expandSelectionAwayFromNestedFormatting(
                raw = raw,
                start = start,
                end = end,
                outerRange = wrappingMarkers
            )

            if (safeStart <= contentStart && safeEnd >= contentEnd) {
                val originalToCleaned = IntArray(raw.length + 1)
                val cleaned = removeFormattingMarkers(
                    raw = raw,
                    openIndex = wrappingMarkers.openIndex,
                    closeIndex = wrappingMarkers.closeIndex,
                    originalToCleaned = originalToCleaned
                )

                rebuildValue(
                    normalized,
                    cleaned,
                    TextRange(originalToCleaned[safeStart], originalToCleaned[safeEnd])
                )
            } else {
                val selectedStart = safeStart.coerceIn(contentStart, contentEnd)
                val selectedEnd = safeEnd.coerceIn(contentStart, contentEnd)
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
    openIndex: Int,
    closeIndex: Int,
    originalToCleaned: IntArray? = null
): String {
    val cleaned = StringBuilder(raw.length - 2)
    var cleanedIndex = 0

    for (index in raw.indices) {
        originalToCleaned?.let { if (index < it.size) it[index] = cleanedIndex }
        if (index == openIndex || index == closeIndex) continue

        cleaned.append(raw[index])
        cleanedIndex++
    }

    originalToCleaned?.let { if (raw.length < it.size) it[raw.length] = cleanedIndex }
    return cleaned.toString()
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
                    if (depth == 0 && currentOpenIndex >= 0 && currentOpenIndex <= start && rawIndex >= end) {
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

data class RichTextFormattingState(
    val boldActive: Boolean,
    val italicActive: Boolean
)

fun richTextFormattingState(value: TextFieldValue): RichTextFormattingState {
    val raw = value.text
    if (raw.isEmpty()) return RichTextFormattingState(false, false)

    val start = minOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)
    val end = maxOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)

    if (start == end) {
        fun formattingDepthAtOffset(offset: Int): Pair<Boolean, Boolean> {
            var boldDepth = 0
            var italicDepth = 0
            var rawIndex = 0

            while (rawIndex < offset && rawIndex < raw.length) {
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
                                }
                            }
                            rawIndex = token.endExclusive
                        } else {
                            rawIndex++
                        }
                    }
                }
            }

            return (boldDepth > 0) to (italicDepth > 0)
        }

        val (boldActive, italicActive) = formattingDepthAtOffset(start)
        return RichTextFormattingState(boldActive, italicActive)
    }

    var boldDepth = 0
    var italicDepth = 0
    var rawIndex = 0
    var boldActive = false
    var italicActive = false

    while (rawIndex < raw.length && rawIndex < end) {
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
                        }
                    }
                    rawIndex = token.endExclusive
                } else {
                    if (rawIndex >= start && boldDepth > 0) boldActive = true
                    if (rawIndex >= start && italicDepth > 0) italicActive = true
                    rawIndex++
                }
            }
        }

        if (rawIndex >= start && rawIndex < end) {
            if (boldDepth > 0) boldActive = true
            if (italicDepth > 0) italicActive = true
        }
    }

    return RichTextFormattingState(boldActive, italicActive)
}

private fun isFormattingActive(value: TextFieldValue, markers: FormattingMarkerPair): Boolean {
    val raw = value.text
    if (raw.isEmpty()) return false

    val start = minOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)
    val end = maxOf(value.selection.start, value.selection.end).coerceIn(0, raw.length)

    fun formattingDepthAtOffset(offset: Int): Boolean {
        var depth = 0
        var rawIndex = 0

        while (rawIndex < offset && rawIndex < raw.length) {
            when (raw[rawIndex]) {
                markers.openMarker -> {
                    depth++
                    rawIndex++
                }
                markers.closeMarker -> {
                    if (depth > 0) depth--
                    rawIndex++
                }
                else -> {
                    val token = parseRichTextTagToken(raw, rawIndex)
                    if (token != null) {
                        if (token.isComplete && token.tag == markers.tagName) {
                            if (token.isClosing) {
                                if (depth > 0) depth--
                            } else {
                                depth++
                            }
                        }
                        rawIndex = token.endExclusive
                    } else {
                        rawIndex++
                    }
                }
            }
        }

        return depth > 0
    }

    if (start == end) {
        return formattingDepthAtOffset(start)
    }

    var depth = 0
    var rawIndex = 0
    while (rawIndex < raw.length && rawIndex < end) {
        when (raw[rawIndex]) {
            markers.openMarker -> {
                depth++
                rawIndex++
            }
            markers.closeMarker -> {
                if (depth > 0) depth--
                rawIndex++
            }
            else -> {
                val token = parseRichTextTagToken(raw, rawIndex)
                if (token != null) {
                    if (token.isComplete && token.tag == markers.tagName) {
                        if (token.isClosing) {
                            if (depth > 0) depth--
                        } else {
                            depth++
                        }
                    }
                    rawIndex = token.endExclusive
                } else {
                    if (rawIndex >= start && depth > 0) return true
                    rawIndex++
                }
            }
        }

        if (rawIndex >= start && rawIndex < end && depth > 0) {
            return true
        }
    }

    return false
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
            current == ITALIC_OPEN_MARKER || current == ITALIC_CLOSE_MARKER
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
    val colors = ArrayDeque<Color>()
    val highlights = ArrayDeque<Color>()
    val originalToTransformed = IntArray(raw.length + 1)
    val transformedToOriginal = mutableListOf<Int>()

    fun currentStyle(): SpanStyle = SpanStyle(
        color = colors.lastOrNull() ?: Color.Unspecified,
        background = highlights.lastOrNull() ?: Color.Unspecified
    )

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
            it == BOLD_OPEN_MARKER || it == ITALIC_OPEN_MARKER
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


