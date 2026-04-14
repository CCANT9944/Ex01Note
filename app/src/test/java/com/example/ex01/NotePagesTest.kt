package com.example.ex01

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*


import org.junit.Assert.assertEquals
import org.junit.Test

class NotePagesTest {
    @Test
    fun splitAndJoinNotePages_roundTripPageContent() {
        val serialized = "First page\u000CSecond page\u000CThird page"

        val pages = splitNotePages(serialized)

        assertEquals(listOf("First page", "Second page", "Third page"), pages)
        assertEquals(serialized, joinNotePages(pages))
    }

    @Test
    fun replaceNotePage_addsMissingPages_andKeepsExistingContent() {
        val serialized = "First page"

        val updated = replaceNotePage(serialized, 2, "Third page")

        assertEquals("First page\u000C\u000C\u000BPage\u000BThird page", updated)
        assertEquals(3, splitNotePages(updated).size)
        assertEquals("First page", notePageBody(updated, 0))
        assertEquals("", notePageBody(updated, 1))
        assertEquals("Third page", notePageBody(updated, 2))
    }

    @Test
    fun notePreviewBody_returnsFirstPage() {
        val serialized = "Preview page\u000CHidden page"

        assertEquals("Preview page", notePageBody(serialized, 0))
    }
}
