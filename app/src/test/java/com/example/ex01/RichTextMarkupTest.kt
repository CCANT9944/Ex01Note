package com.example.ex01

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextMarkupTest {
    @Test
    fun wrapSelectionWithTag_wrapsSelectedTextAndKeepsSelectionInside() {
        val value = TextFieldValue("Hello world", selection = TextRange(6, 11))

        val wrapped = wrapSelectionWithTag(value, BOLD_OPEN_MARKER.toString(), BOLD_CLOSE_MARKER.toString())

        assertEquals("Hello \uE000world\uE001", wrapped.text)
        assertEquals(TextRange(12), wrapped.selection)
    }

    @Test
    fun clearRichTextFormatting_removesMarkupFromSelection() {
        val value = TextFieldValue("Hello \uE000world\uE001", selection = TextRange(0, 13))

        val cleared = clearRichTextFormatting(value)

        assertEquals("Hello world", cleared.text)
        assertEquals(TextRange(0, 11), cleared.selection)
    }

    @Test
    fun renderRichTextMarkup_hidesMarkersAndLegacyBoldTags() {
        val complete = renderRichTextMarkup("Hello [b]world[/b]")
        val markerBased = renderRichTextMarkup("Hello \uE000world\uE001")

        assertEquals("Hello world", complete.text)
        assertEquals("Hello world", markerBased.text)
        assertTrue(complete.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(markerBased.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun renderRichTextMarkup_hidesMarkersAndLegacyItalicTags() {
        val complete = renderRichTextMarkup("Hello [i]world[/i]")
        val markerBased = renderRichTextMarkup("Hello \uE002world\uE003")

        assertEquals("Hello world", complete.text)
        assertEquals("Hello world", markerBased.text)
        assertTrue(complete.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(markerBased.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun renderRichTextMarkup_hidesMarkersAndLegacyUnderlineTags() {
        val complete = renderRichTextMarkup("Hello [u]world[/u]")
        val markerBased = renderRichTextMarkup("Hello \uE004world\uE005")

        assertEquals("Hello world", complete.text)
        assertEquals("Hello world", markerBased.text)
        assertTrue(complete.spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
        assertTrue(markerBased.spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
    }

    @Test
    fun renderRichTextMarkup_hidesMarkersAndLegacyStrikeThroughTags() {
        val complete = renderRichTextMarkup("Hello [strike]world[/strike]")
        val markerBased = renderRichTextMarkup("Hello \uE006world\uE007")

        assertEquals("Hello world", complete.text)
        assertEquals("Hello world", markerBased.text)
        assertTrue(complete.spanStyles.any { it.item.textDecoration?.contains(TextDecoration.LineThrough) == true })
        assertTrue(markerBased.spanStyles.any { it.item.textDecoration?.contains(TextDecoration.LineThrough) == true })
    }

    @Test
    fun richTextVisualTransformation_mapsFinalVisibleOffsetToRawEnd() {
        val raw = "First \uE000row\uE001\nLast \uE000row\uE001"
        val transformed = richTextVisualTransformation().filter(AnnotatedString(raw))

        assertEquals("First row\nLast row", transformed.text.text)
        assertEquals(raw.length - 1, transformed.offsetMapping.transformedToOriginal(transformed.text.length))
    }

    @Test
    fun richTextVisualTransformation_mapsFormattedVisibleStartInsideSpan() {
        val raw = "\uE000Hello\uE001"
        val transformed = richTextVisualTransformation().filter(AnnotatedString(raw))

        assertEquals("Hello", transformed.text.text)
        assertEquals(1, transformed.offsetMapping.transformedToOriginal(0))
        assertEquals(6, transformed.offsetMapping.transformedToOriginal(5))
        assertTrue(isBoldFormattingActive(TextFieldValue(raw, selection = TextRange(6))))
    }

    @Test
    fun richTextVisualTransformation_mapsEmptyBoldSpanCaretBetweenMarkers() {
        val raw = "\uE000\uE001"
        val transformed = richTextVisualTransformation().filter(AnnotatedString(raw))

        assertEquals("", transformed.text.text)
        assertEquals(1, transformed.offsetMapping.transformedToOriginal(0))
        assertTrue(isBoldFormattingActive(TextFieldValue(raw, selection = TextRange(1))))
    }

    @Test
    fun richTextVisualTransformation_mapsLongMarkerHeavyCaretToTransformedEnd() {
        val raw = buildString {
            append("a".repeat(228))
            append(BOLD_OPEN_MARKER)
            append(BOLD_CLOSE_MARKER)
        }
        val transformed = richTextVisualTransformation().filter(AnnotatedString(raw))

        assertEquals("a".repeat(228), transformed.text.text)
        assertEquals(transformed.text.length, transformed.offsetMapping.originalToTransformed(raw.length))
    }

    @Test
    fun normalizeRichTextMarkup_convertsLegacyBoldTagsToMarkers() {
        val normalized = normalizeRichTextMarkup(
            TextFieldValue("Cătălin [b]test[/b]", selection = TextRange(19, 19))
        )

        assertEquals("Cătălin \uE000test\uE001", normalized.text)
        assertEquals(TextRange(14), normalized.selection)
    }

    @Test
    fun normalizeRichTextMarkup_convertsLegacyItalicTagsToMarkers() {
        val normalized = normalizeRichTextMarkup(
            TextFieldValue("Cătălin [i]test[/i]", selection = TextRange(19, 19))
        )

        assertEquals("Cătălin \uE002test\uE003", normalized.text)
        assertEquals(TextRange(14), normalized.selection)
    }

    @Test
    fun sanitizeRichTextTyping_stripsPartialBoldTags() {
        val sanitized = sanitizeRichTextTyping(
            TextFieldValue("Ce faci azi?[/b", selection = TextRange(12, 12))
        )

        assertEquals("Ce faci azi?", sanitized.text)
        assertEquals(TextRange(12, 12), sanitized.selection)
    }

    @Test
    fun collapseEmptyBoldSpans_removesMarkers_whenBoldedWord_isDeleted() {
        val collapsed = collapseEmptyBoldSpans(
            TextFieldValue("Cătălin \uE000\uE001", selection = TextRange(8, 8))
        )

        assertEquals("Cătălin ", collapsed.text)
        assertEquals(TextRange(8, 8), collapsed.selection)
    }

    @Test
    fun collapseEmptyFormattingSpans_removesEmptySpan_afterDeletion_whenPreservationDisabled() {
        val collapsed = collapseEmptyFormattingSpans(
            TextFieldValue("Cătălin \uE000\uE001", selection = TextRange(9, 9)),
            preserveCollapsedSelectionSpan = false
        )

        assertEquals("Cătălin ", collapsed.text)
        assertEquals(TextRange(8, 8), collapsed.selection)
    }

    @Test
    fun toggleBoldFormatting_wrapsAndUnwrapsSelectedText() {
        val selected = TextFieldValue("Hello world", selection = TextRange(6, 11))
        val bolded = toggleBoldFormatting(selected)

        assertEquals("Hello \uE000world\uE001", bolded.text)
        assertEquals(TextRange(12), bolded.selection)

        val unbolded = toggleBoldFormatting(
            TextFieldValue(bolded.text, selection = TextRange(7, 12))
        )

        assertEquals("Hello world", unbolded.text)
        assertEquals(TextRange(6, 11), unbolded.selection)
    }

    @Test
    fun toggleBoldFormatting_splitsBoldSpanAroundSelectedText() {
        val value = TextFieldValue("A \uE000hello brave world\uE001 Z", selection = TextRange(9, 14))

        val unbolded = toggleBoldFormatting(value)

        assertEquals("A \uE000hello \uE001brave\uE000 world\uE001 Z", unbolded.text)
        assertEquals(TextRange(10, 15), unbolded.selection)
    }

    @Test
    fun toggleBoldFormatting_onlyUnboldsLastRowInThreeRowSelection() {
        val value = TextFieldValue(
            "\uE000First row\nMiddle row\nLast row\uE001",
            selection = TextRange(22, 30)
        )

        val unbolded = toggleBoldFormatting(value)

        assertEquals("\uE000First row\nMiddle row\n\uE001Last row", unbolded.text)
        assertEquals(TextRange(23, 31), unbolded.selection)
    }

    @Test
    fun toggleBoldFormatting_onlyUnboldsLastRowInThreeRowReversedSelection() {
        val value = TextFieldValue(
            "\uE000First row\nMiddle row\nLast row\uE001",
            selection = TextRange(30, 22)
        )

        val unbolded = toggleBoldFormatting(value)

        assertEquals("\uE000First row\nMiddle row\n\uE001Last row", unbolded.text)
        assertEquals(TextRange(23, 31), unbolded.selection)
    }

    @Test
    fun toggleBoldFormatting_unboldsAllWhenFullSelectionIncludesNestedBoldLastWord() {
        val value = TextFieldValue(
            "\uE000First row\nMiddle row\n\uE000Last row\uE001\uE001",
            selection = TextRange(0, 32)
        )

        val unbolded = toggleBoldFormatting(value)

        assertEquals("First row\nMiddle row\nLast row", unbolded.text)
        assertEquals(TextRange(0, 29), unbolded.selection)
    }

    @Test
    fun toggleItalicFormatting_unitalicizesAllWhenFullSelectionIncludesNestedItalicLastWord() {
        val value = TextFieldValue(
            "\uE002First row\nMiddle row\n\uE002Last row\uE003\uE003",
            selection = TextRange(0, 32)
        )

        val unitalicized = toggleItalicFormatting(value)

        assertEquals("First row\nMiddle row\nLast row", unitalicized.text)
        assertEquals(TextRange(0, 29), unitalicized.selection)
    }

    @Test
    fun toggleBoldFormatting_removesWholeBoldSpanWhenEntireSpanIsSelected() {
        val value = TextFieldValue("Hello \uE000world\uE001", selection = TextRange(6, 12))

        val unbolded = toggleBoldFormatting(value)

        assertEquals("Hello world", unbolded.text)
        assertEquals(TextRange(6, 11), unbolded.selection)
    }

    @Test
    fun toggleBoldFormatting_removesBoldSpanWhenCaretIsAtSpanEnd() {
        val bolded = toggleBoldFormatting(TextFieldValue("Hello world", selection = TextRange(6, 11)))

        val unbolded = toggleBoldFormatting(
            TextFieldValue(bolded.text, selection = TextRange(13)) // bolded string has 2 markers now, so end is 13
        )

        assertEquals("Hello \uE000world\uE001\uE000\uE001", unbolded.text)
        assertEquals(TextRange(14), unbolded.selection) 
    }

    @Test
    fun toggleBoldFormatting_removesBoldSpanWhenCaretIsInsideSpan() {
        val value = TextFieldValue("Hello \uE000world\uE001", selection = TextRange(9))

        val unbolded = toggleBoldFormatting(value)

        // Now expects the span to be split at the caret instead of entirely removed
        assertEquals("Hello \uE000wo\uE001\uE000rld\uE001", unbolded.text)
        assertEquals(TextRange(10), unbolded.selection)
    }

    @Test
    fun toggleBoldFormatting_preservesNestedItalicSpanWhenSelectionCrossesIt() {
        val value = TextFieldValue("A \uE000ab \uE002cd\uE003 ef\uE001 Z", selection = TextRange(4, 12))

        val unbolded = toggleBoldFormatting(value)
        val rendered = renderRichTextMarkup(unbolded.text)

        assertEquals("A ab cd ef Z", rendered.text)
        assertTrue(rendered.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun toggleBoldFormatting_removesAllBoldFromSelectAllText() {
        val plainValue = TextFieldValue(
            "First row\nMiddle row\nLast row",
            selection = TextRange(0, 29)
        )

        val bolded = toggleBoldFormatting(plainValue)

        assertEquals("\uE000First row\nMiddle row\nLast row\uE001", bolded.text)
        assertEquals(TextRange(30), bolded.selection)

        val value = TextFieldValue(bolded.text, selection = TextRange(0, bolded.text.length))

        val unbolded = toggleBoldFormatting(value)

        assertEquals("First row\nMiddle row\nLast row", unbolded.text)
        assertEquals(TextRange(0, 29), unbolded.selection)
    }

    @Test
    fun toggleItalicFormatting_removesAllItalicFromSelectAllText() {
        val plainValue = TextFieldValue(
            "First row\nMiddle row\nLast row",
            selection = TextRange(0, 29)
        )

        val italicized = toggleItalicFormatting(plainValue)

        assertEquals("\uE002First row\nMiddle row\nLast row\uE003", italicized.text)
        assertEquals(TextRange(30), italicized.selection)

        val value = TextFieldValue(italicized.text, selection = TextRange(0, italicized.text.length))

        val unitalicized = toggleItalicFormatting(value)

        assertEquals("First row\nMiddle row\nLast row", unitalicized.text)
        assertEquals(TextRange(0, 29), unitalicized.selection)
    }

    @Test
    fun toggleUnderlineFormatting_removesAllUnderlineFromSelectAllText() {
        val plainValue = TextFieldValue(
            "First row\nMiddle row\nLast row",
            selection = TextRange(0, 29)
        )

        val underlined = toggleUnderlineFormatting(plainValue)

        assertEquals("\uE004First row\nMiddle row\nLast row\uE005", underlined.text)
        assertEquals(TextRange(30), underlined.selection)

        val value = TextFieldValue(underlined.text, selection = TextRange(0, underlined.text.length))

        val ununderlined = toggleUnderlineFormatting(value)

        assertEquals("First row\nMiddle row\nLast row", ununderlined.text)
        assertEquals(TextRange(0, 29), ununderlined.selection)
    }

    @Test
    fun toggleUnderlineFormatting_removesWholeUnderlineSpanWhenEntireSpanIsSelected() {
        val value = TextFieldValue("Hello \uE004world\uE005", selection = TextRange(6, 12))

        val ununderlined = toggleUnderlineFormatting(value)

        assertEquals("Hello world", ununderlined.text)
        assertEquals(TextRange(6, 11), ununderlined.selection)
    }

    @Test
    fun toggleUnderlineFormatting_insertsAndRemovesCaretSpan() {
        val enabled = toggleUnderlineFormatting(TextFieldValue("Hello", selection = TextRange(5)))

        assertEquals("Hello\uE004\uE005", enabled.text)
        assertEquals(TextRange(6), enabled.selection)
        assertTrue(isUnderlineFormattingActive(enabled))

        val disabled = toggleUnderlineFormatting(enabled)

        assertEquals("Hello", disabled.text)
        assertEquals(TextRange(5), disabled.selection)
        assertFalse(isUnderlineFormattingActive(disabled))
    }

    @Test
    fun toggleStrikethroughFormatting_wrapsAndUnwrapsSelectedText() {
        val selected = TextFieldValue("Hello world", selection = TextRange(6, 11))
        val striked = toggleStrikethroughFormatting(selected)

        assertEquals("Hello \uE006world\uE007", striked.text)
        assertEquals(TextRange(12), striked.selection)

        val unstriked = toggleStrikethroughFormatting(
            TextFieldValue(striked.text, selection = TextRange(7, 12))
        )

        assertEquals("Hello world", unstriked.text)
        assertEquals(TextRange(6, 11), unstriked.selection)
    }

    @Test
    fun toggleStrikethroughFormatting_insertsAndRemovesCaretSpan() {
        val enabled = toggleStrikethroughFormatting(TextFieldValue("Hello", selection = TextRange(5)))

        assertEquals("Hello\uE006\uE007", enabled.text)
        assertEquals(TextRange(6), enabled.selection)
        assertTrue(isStrikethroughFormattingActive(enabled))

        val disabled = toggleStrikethroughFormatting(enabled)

        assertEquals("Hello", disabled.text)
        assertEquals(TextRange(5), disabled.selection)
        assertFalse(isStrikethroughFormattingActive(disabled))
    }

    @Test
    fun toggleBulletFormatting_appliesToTheWholeSelectedLine() {
        val selected = TextFieldValue("Hello world", selection = TextRange(6, 11))
        val bulleted = toggleBulletFormatting(selected)

        assertEquals("\uE008Hello world\uE009", bulleted.text)
        assertEquals(TextRange(7, 12), bulleted.selection)
    }

    @Test
    fun toggleBulletFormatting_appliesToAllSelectedLines() {
        val selected = TextFieldValue(
            "First line\nSecond line\nThird line",
            selection = TextRange(3, 25)
        )

        val bulleted = toggleBulletFormatting(selected)

        assertEquals("\uE008First line\uE009\n\uE008Second line\uE009\n\uE008Third line\uE009", bulleted.text)
    }

    @Test
    fun toggleBulletFormatting_unbulletsTheWholeSelectedLine() {
        val bulleted = TextFieldValue("\uE008Hello world\uE009", selection = TextRange(7, 12))
        val unbulleted = toggleBulletFormatting(bulleted)

        assertEquals("Hello world", unbulleted.text)
        assertEquals(TextRange(6, 11), unbulleted.selection)
    }

    @Test
    fun indentSelectedLines_indentsCollapsedCaretLine() {
        val indented = indentSelectedLines(TextFieldValue("Hello", selection = TextRange(0)))

        assertEquals("    Hello", indented.text)
        assertEquals(TextRange(4), indented.selection)
    }

    @Test
    fun indentSelectedLines_indentsEverySelectedLine() {
        val selected = TextFieldValue("First\nSecond\nThird", selection = TextRange(0, 18))

        val indented = indentSelectedLines(selected)

        assertEquals("    First\n    Second\n    Third", indented.text)
        assertEquals(TextRange(4, 30), indented.selection)
    }

    @Test
    fun outdentSelectedLines_removesLeadingSpacesFromEachSelectedLine() {
        val selected = TextFieldValue("    First\n  Second\nThird", selection = TextRange(0, 24))

        val outdented = outdentSelectedLines(selected)

        assertEquals("First\nSecond\nThird", outdented.text)
        assertEquals(TextRange(0, 18), outdented.selection)
    }

    @Test
    fun outdentSelectedLines_keepsSelectionStableInsideRemovedIndent() {
        val selected = TextFieldValue("    First\n    Second", selection = TextRange(2, 14))

        val outdented = outdentSelectedLines(selected)

        assertEquals("First\nSecond", outdented.text)
        assertEquals(TextRange(0, 6), outdented.selection)
    }

    @Test
    fun renderRichTextMarkup_hidesBulletMarkersAndShowsBulletPrefix() {
        val rendered = renderRichTextMarkup("\uE008Hello world\uE009\n\uE008Second line\uE009")

        assertEquals("• Hello world\n• Second line", rendered.text)
    }

    @Test
    fun richTextFormattingState_detectsBulletAtCaret() {
        val value = TextFieldValue("Hello \uE008world\uE009", selection = TextRange(7))

        val state = richTextFormattingState(value)

        assertTrue(state.bulletActive)
    }

    @Test
    fun richTextFormattingState_detectsIndentedCaretLine() {
        val value = TextFieldValue("    Hello", selection = TextRange(4))

        val state = richTextFormattingState(value)

        assertTrue(state.indentActive)
    }

    @Test
    fun richTextFormattingState_detectsIndentedSelectionAcrossLines() {
        val value = TextFieldValue("    First\nSecond", selection = TextRange(0, 16))

        val state = richTextFormattingState(value)

        assertTrue(state.indentActive)
    }

    @Test
    fun richTextFormattingState_returnsFalseForPlainLineIndentState() {
        val value = TextFieldValue("Hello", selection = TextRange(0))

        val state = richTextFormattingState(value)

        assertFalse(state.indentActive)
    }

    @Test
    fun toggleBoldFormatting_insertsAndRemovesCaretSpan() {
        val enabled = toggleBoldFormatting(TextFieldValue("Hello", selection = TextRange(5)))

        assertEquals("Hello\uE000\uE001", enabled.text)
        assertEquals(TextRange(6), enabled.selection)
        assertTrue(isBoldFormattingActive(enabled))

        val disabled = toggleBoldFormatting(enabled)

        assertEquals("Hello", disabled.text)
        assertEquals(TextRange(5), disabled.selection)
        assertFalse(isBoldFormattingActive(disabled))
    }

    @Test
    fun toggleItalicFormatting_wrapsAndUnwrapsSelectedText() {
        val selected = TextFieldValue("Hello world", selection = TextRange(6, 11))
        val italicized = toggleItalicFormatting(selected)

        assertEquals("Hello \uE002world\uE003", italicized.text)
        assertEquals(TextRange(12), italicized.selection)

        val unitalicized = toggleItalicFormatting(
            TextFieldValue(italicized.text, selection = TextRange(7, 12))
        )

        assertEquals("Hello world", unitalicized.text)
        assertEquals(TextRange(6, 11), unitalicized.selection)
    }

    @Test
    fun toggleItalicFormatting_onlyUnitalicizesLastRowInThreeRowSelection() {
        val value = TextFieldValue(
            "\uE002First row\nMiddle row\nLast row\uE003",
            selection = TextRange(22, 30)
        )

        val unitalicized = toggleItalicFormatting(value)

        assertEquals("\uE002First row\nMiddle row\n\uE003Last row", unitalicized.text)
        assertEquals(TextRange(23, 31), unitalicized.selection)
    }

    @Test
    fun toggleItalicFormatting_onlyUnitalicizesLastRowInThreeRowReversedSelection() {
        val value = TextFieldValue(
            "\uE002First row\nMiddle row\nLast row\uE003",
            selection = TextRange(30, 22)
        )

        val unitalicized = toggleItalicFormatting(value)

        assertEquals("\uE002First row\nMiddle row\n\uE003Last row", unitalicized.text)
        assertEquals(TextRange(23, 31), unitalicized.selection)
    }

    @Test
    fun toggleItalicFormatting_removesWholeItalicSpanWhenEntireSpanIsSelected() {
        val value = TextFieldValue("Hello \uE002world\uE003", selection = TextRange(6, 12))

        val unitalicized = toggleItalicFormatting(value)

        assertEquals("Hello world", unitalicized.text)
        assertEquals(TextRange(6, 11), unitalicized.selection)
    }

    @Test
    fun toggleItalicFormatting_removesItalicSpanWhenCaretIsAtSpanEnd() {
        val italicized = toggleItalicFormatting(TextFieldValue("Hello world", selection = TextRange(6, 11)))

        val unitalicized = toggleItalicFormatting(
            TextFieldValue(italicized.text, selection = TextRange(13))
        )

        assertEquals("Hello \uE002world\uE003\uE002\uE003", unitalicized.text)
        assertEquals(TextRange(14), unitalicized.selection)
    }

    @Test
    fun toggleItalicFormatting_removesItalicSpanWhenCaretIsInsideSpan() {
        val value = TextFieldValue("Hello \uE002world\uE003", selection = TextRange(9))

        val unitalicized = toggleItalicFormatting(value)

        // Now expects the span to be split at the caret instead of entirely removed
        assertEquals("Hello \uE002wo\uE003\uE002rld\uE003", unitalicized.text)
        assertEquals(TextRange(10), unitalicized.selection)
    }

    @Test
    fun toggleBoldFormatting_afterTypedSpace_keepsPreviousWordBold() {
        val value = TextFieldValue("A \uE000word \uE001", selection = TextRange(8))

        val unbolded = toggleBoldFormatting(value)

        assertEquals("A \uE000word\uE001 ", unbolded.text)
        assertEquals(TextRange(9), unbolded.selection)
        assertTrue(isBoldFormattingActive(TextFieldValue(unbolded.text, selection = TextRange(4))))
    }

    @Test
    fun toggleItalicFormatting_afterTypedSpace_keepsPreviousWordItalic() {
        val value = TextFieldValue("A \uE002word \uE003", selection = TextRange(8))

        val unitalicized = toggleItalicFormatting(value)

        assertEquals("A \uE002word\uE003 ", unitalicized.text)
        assertEquals(TextRange(9), unitalicized.selection)
        assertTrue(isItalicFormattingActive(TextFieldValue(unitalicized.text, selection = TextRange(4))))
    }

    @Test
    fun toggleItalicFormatting_preservesNestedBoldSpanWhenSelectionCrossesIt() {
        val value = TextFieldValue("A \uE002ab \uE000cd\uE001 ef\uE003 Z", selection = TextRange(4, 12))

        val unitalicized = toggleItalicFormatting(value)
        val rendered = renderRichTextMarkup(unitalicized.text)

        assertEquals("A ab cd ef Z", rendered.text)
        assertTrue(rendered.spanStyles.any { it.item.fontWeight != null })
    }

    @Test
    fun toggleItalicFormatting_insertsAndRemovesCaretSpan() {
        val enabled = toggleItalicFormatting(TextFieldValue("Hello", selection = TextRange(5)))

        assertEquals("Hello\uE002\uE003", enabled.text)
        assertEquals(TextRange(6), enabled.selection)
        assertTrue(isItalicFormattingActive(enabled))

        val disabled = toggleItalicFormatting(enabled)

        assertEquals("Hello", disabled.text)
        assertEquals(TextRange(5), disabled.selection)
        assertFalse(isItalicFormattingActive(disabled))
    }

    @Test
    fun toggleItalicFormatting_atPlainTextEnd_doesNotRemovePreviousItalicSpan() {
        val value = TextFieldValue("A \uE002italic\uE003 plain", selection = TextRange("A \uE002italic\uE003 plain".length))

        val toggled = toggleItalicFormatting(value)

        assertEquals("A \uE002italic\uE003 plain\uE002\uE003", toggled.text)
        assertEquals(TextRange(value.text.length + 1), toggled.selection)
    }

    @Test
    fun toggleUnderlineFormatting_wrapsAndUnwrapsSelectedText() {
        val selected = TextFieldValue("Hello world", selection = TextRange(6, 11))
        val underlined = toggleUnderlineFormatting(selected)

        assertEquals("Hello \uE004world\uE005", underlined.text)
        assertEquals(TextRange(12), underlined.selection)

        val ununderlined = toggleUnderlineFormatting(
            TextFieldValue(underlined.text, selection = TextRange(7, 12))
        )

        assertEquals("Hello world", ununderlined.text)
        assertEquals(TextRange(6, 11), ununderlined.selection)
    }

    @Test
    fun sanitizeRichTextTyping_preservesNewBoldSpanAtCaret() {
        val enabled = toggleBoldFormatting(TextFieldValue("Hello", selection = TextRange(5)))
        val sanitized = sanitizeRichTextTyping(enabled)

        assertEquals("Hello\uE000\uE001", sanitized.text)
        assertEquals(TextRange(6), sanitized.selection)
    }

    @Test
    fun sanitizeRichTextTyping_preservesNestedBoldAndItalicSpans() {
        val value = TextFieldValue("A \uE000bold \uE002italic\uE003 text\uE001 Z", selection = TextRange(23, 23))

        val sanitized = sanitizeRichTextTyping(value)

        assertEquals("A \uE000bold \uE002italic\uE003 text\uE001 Z", sanitized.text)
        assertEquals(TextRange(23), sanitized.selection)
    }

    @Test
    fun collapseEmptyFormattingSpans_removesOnlyEmptyItalicSpanInsideBoldText() {
        val value = TextFieldValue("A \uE000bold \uE002\uE003 text\uE001", selection = TextRange(0, 15))

        val collapsed = collapseEmptyFormattingSpans(value)

        assertEquals("A \uE000bold  text\uE001", collapsed.text)
        assertEquals(TextRange(0, 13), collapsed.selection)
    }

    @Test
    fun isBoldFormattingActive_returnsTrue_whenCaretIsInsideBoldMarkers() {
        val value = TextFieldValue("Hello \uE000world\uE001", selection = TextRange(7))

        assertTrue(isBoldFormattingActive(value))
    }

    @Test
    fun isBoldFormattingActive_returnsFalse_whenCaretIsOutsideBoldMarkers() {
        val value = TextFieldValue("Hello \uE000world\uE001", selection = TextRange(6))

        assertFalse(isBoldFormattingActive(value))
    }

    @Test
    fun isBoldFormattingActive_supportsLegacyBoldTags() {
        val value = TextFieldValue("Hello [b]world[/b]", selection = TextRange(9))

        assertTrue(isBoldFormattingActive(value))
    }

    @Test
    fun isItalicFormattingActive_supportsLegacyItalicTags() {
        val value = TextFieldValue("Hello [i]world[/i]", selection = TextRange(9))

        assertTrue(isItalicFormattingActive(value))
    }

    @Test
    fun richTextFormattingState_detectsBothStylesInNestedText() {
        val value = TextFieldValue("A \uE000bold \uE002italic\uE003 text\uE001 Z", selection = TextRange(11, 11))

        val state = richTextFormattingState(value)

        assertTrue(state.boldActive)
        assertTrue(state.italicActive)
    }

    @Test
    fun richTextFormattingState_detectsBothStylesAcrossSelectionRange() {
        val value = TextFieldValue("A \uE000bold \uE002italic\uE003 text\uE001 Z", selection = TextRange(2, 21))

        val state = richTextFormattingState(value)

        assertTrue(state.boldActive)
        assertTrue(state.italicActive)
    }

    @Test
    fun renderRichTextMarkup_preservesBoldAndUnderlineTogether() {
        val rendered = renderRichTextMarkup("A \uE000bold \uE004under\uE005 text\uE001 Z")

        assertEquals("A bold under text Z", rendered.text)
        assertTrue(rendered.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(rendered.spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
    }

    @Test
    fun renderRichTextMarkup_preservesBoldAndItalicTogether() {
        val rendered = renderRichTextMarkup("A \uE000bold \uE002italic\uE003 text\uE001 Z")

        assertEquals("A bold italic text Z", rendered.text)
        assertTrue(rendered.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(rendered.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun renderRichTextMarkup_preservesItalicAndUnderlineTogether() {
        val rendered = renderRichTextMarkup("A \uE002italic \uE004under\uE005 text\uE003 Z")

        assertEquals("A italic under text Z", rendered.text)
        assertTrue(rendered.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(rendered.spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
    }

    @Test
    fun renderRichTextMarkup_preservesBoldItalicAndUnderlineTogether() {
        val rendered = renderRichTextMarkup("A \uE000bold \uE002italic \uE004under\uE005\uE003 text\uE001 Z")

        assertEquals("A bold italic under text Z", rendered.text)
        assertTrue(rendered.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(rendered.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(rendered.spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
    }

    @Test
    fun renderRichTextMarkup_preservesUnderlineAndStrikeThroughTogether() {
        val rendered = renderRichTextMarkup("A \uE004\uE006both\uE007\uE005 Z")

        assertEquals("A both Z", rendered.text)
        assertTrue(rendered.spanStyles.any {
            it.item.textDecoration?.contains(TextDecoration.Underline) == true &&
                it.item.textDecoration?.contains(TextDecoration.LineThrough) == true
        })
    }

    @Test
    fun richTextFormattingState_detectsUnderlineInSelectionRange() {
        val value = TextFieldValue("A \uE004underlined\uE005 text", selection = TextRange(2, 12))

        val state = richTextFormattingState(value)

        assertTrue(state.underlineActive)
    }

    @Test
    fun richTextFormattingState_detectsStrikeThroughInSelectionRange() {
        val value = TextFieldValue("A \uE006struck\uE007 text", selection = TextRange(2, 9))

        val state = richTextFormattingState(value)

        assertTrue(state.strikethroughActive)
    }

    @Test
    fun richTextFormattingState_detectsBoldAndItalicAtEndOfNestedSpan() {
        val raw = "A \uE000bold \uE002italic\uE003 text\uE001 Z"
        val caretAtNestedSpanEnd = raw.indexOf('\uE003')
        val value = TextFieldValue(raw, selection = TextRange(caretAtNestedSpanEnd))

        val state = richTextFormattingState(value)

        assertTrue(state.boldActive)
        assertTrue(state.italicActive)
    }

    @Test
    fun isUnderlineFormattingActive_returnsTrue_whenSelectionCrossesUnderlineSpan() {
        val value = TextFieldValue("Start \uE004under\uE005 end", selection = TextRange(5, 10))

        assertTrue(isUnderlineFormattingActive(value))
    }

    @Test
    fun isBoldFormattingActive_returnsTrue_whenSelectionCrossesBoldSpan() {
        val value = TextFieldValue("Start \uE000bold\uE001 end", selection = TextRange(5, 10))

        assertTrue(isBoldFormattingActive(value))
    }

    @Test
    fun isItalicFormattingActive_returnsTrue_whenSelectionCrossesItalicSpan() {
        val value = TextFieldValue("Start \uE002ital\uE003 end", selection = TextRange(5, 10))

        assertTrue(isItalicFormattingActive(value))
    }

    @Test
    fun isStrikethroughFormattingActive_returnsTrue_whenSelectionCrossesStrikeThroughSpan() {
        val value = TextFieldValue("Start \uE006strike\uE007 end", selection = TextRange(5, 11))

        assertTrue(isStrikethroughFormattingActive(value))
    }
}
