@file:Suppress("unused")

package com.example.ex01.ui.editor

internal val richTextTagRegex = Regex(
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
internal const val INDENT_UNIT = "    "

data class FormattingMarkerPair(
    val openMarker: Char,
    val closeMarker: Char,
    val tagName: String
)

internal val BOLD_MARKERS = FormattingMarkerPair(BOLD_OPEN_MARKER, BOLD_CLOSE_MARKER, "b")
internal val ITALIC_MARKERS = FormattingMarkerPair(ITALIC_OPEN_MARKER, ITALIC_CLOSE_MARKER, "i")
internal val UNDERLINE_MARKERS = FormattingMarkerPair(UNDERLINE_OPEN_MARKER, UNDERLINE_CLOSE_MARKER, "u")
internal val STRIKETHROUGH_MARKERS = FormattingMarkerPair(STRIKETHROUGH_OPEN_MARKER, STRIKETHROUGH_CLOSE_MARKER, "s")
internal val BULLET_MARKERS = FormattingMarkerPair(BULLET_OPEN_MARKER, BULLET_CLOSE_MARKER, "bullet")

internal data class RichTextTagToken(
    val endExclusive: Int,
    val tag: String? = null,
    val isClosing: Boolean = false,
    val argument: String = "",
    val isComplete: Boolean = false
)

internal fun findMarkupBoundary(raw: String, start: Int): Int {
    var index = start + 1
    while (index < raw.length && !raw[index].isWhitespace() && raw[index] != '[') {
        index++
    }
    return index
}

internal fun isPartialRichTextTagPrefix(candidate: String): Boolean {
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

internal fun parseRichTextTagToken(raw: String, start: Int): RichTextTagToken? {
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
