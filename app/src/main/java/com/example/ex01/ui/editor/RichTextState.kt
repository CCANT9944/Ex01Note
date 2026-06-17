@file:Suppress("unused")

package com.example.ex01.ui.editor

import androidx.compose.ui.text.input.TextFieldValue

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

internal data class RichTextDepth(
    val boldDepth: Int,
    val italicDepth: Int,
    val underlineDepth: Int,
    val strikethroughDepth: Int,
    val bulletDepth: Int
)

internal inline fun scanRichTextFormatting(
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

internal fun formattingDepthAtOffset(raw: String, offset: Int): RichTextDepth {
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

internal fun isIndentedSelection(raw: String, start: Int, end: Int): Boolean {
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

internal fun isFormattingActive(value: TextFieldValue, markers: FormattingMarkerPair): Boolean {
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
