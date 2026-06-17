@file:Suppress("unused")

package com.example.ex01.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal fun rebuildValue(
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

fun normalizeBulletNewlines(value: TextFieldValue): TextFieldValue {
    val raw = value.text
    if (!raw.contains('\n')) return value

    var needsRefactoring = false
    var currentDepth = 0
    for(i in raw.indices) {
        val char = raw[i]
        if (char == BULLET_OPEN_MARKER) currentDepth++
        else if (char == BULLET_CLOSE_MARKER) {
            if (currentDepth > 0) currentDepth--
        } else if (char == '\n' && currentDepth > 0) {
            needsRefactoring = true
            break
        }
    }
    if (!needsRefactoring) return value

    val rebuilt = StringBuilder(raw.length + 10)
    val originalToCleaned = IntArray(raw.length + 1)
    var cleanedIndex = 0
    currentDepth = 0

    for (i in raw.indices) {
        val char = raw[i]
        
        if (char == '\n' && currentDepth > 0) {
            for (d in 0 until currentDepth) {
                rebuilt.append(BULLET_CLOSE_MARKER)
                cleanedIndex++
            }
            originalToCleaned[i] = cleanedIndex
            rebuilt.append('\n')
            cleanedIndex++
            for (d in 0 until currentDepth) {
                rebuilt.append(BULLET_OPEN_MARKER)
                cleanedIndex++
            }
        } else {
            originalToCleaned[i] = cleanedIndex
            if (char == BULLET_OPEN_MARKER) currentDepth++
            else if (char == BULLET_CLOSE_MARKER) {
                if (currentDepth > 0) currentDepth--
            }
            rebuilt.append(char)
            cleanedIndex++
        }
    }
    originalToCleaned[raw.length] = cleanedIndex

    val start = value.selection.start.coerceIn(0, raw.length)
    val end = value.selection.end.coerceIn(0, raw.length)
    
    return rebuildValue(
        original = value,
        text = rebuilt.toString(),
        selection = TextRange(originalToCleaned[start], originalToCleaned[end])
    )
}

fun sanitizeRichTextTyping(value: TextFieldValue): TextFieldValue {
    return collapseEmptyFormattingSpans(normalizeBulletNewlines(normalizeRichTextMarkup(value)))
}
