@file:Suppress("unused")

package com.example.ex01.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.graphics.toColorInt

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
