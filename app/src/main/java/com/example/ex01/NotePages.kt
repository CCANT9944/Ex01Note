@file:Suppress("unused")

package com.example.ex01

private const val NOTE_PAGE_SEPARATOR = '\u000C'

fun splitNotePages(serializedBody: String): List<String> {
    return if (serializedBody.isEmpty()) {
        listOf("")
    } else {
        val pages = mutableListOf<String>()
        var startIndex = 0

        while (startIndex <= serializedBody.length) {
            val separatorIndex = serializedBody.indexOf(NOTE_PAGE_SEPARATOR, startIndex)
            if (separatorIndex < 0) {
                pages.add(serializedBody.substring(startIndex))
                break
            }

            pages.add(serializedBody.substring(startIndex, separatorIndex))
            startIndex = separatorIndex + 1

            if (startIndex == serializedBody.length) {
                pages.add("")
                break
            }
        }

        pages
    }
}

fun joinNotePages(pages: List<String>): String {
    return pages.joinToString(NOTE_PAGE_SEPARATOR.toString())
}

@Suppress("unused")
fun notePageBody(serializedBody: String, pageIndex: Int): String {
    return splitNotePages(serializedBody).getOrElse(pageIndex) { "" }
}

fun replaceNotePage(serializedBody: String, pageIndex: Int, pageBody: String): String {
    val pages = splitNotePages(serializedBody).toMutableList()
    while (pages.size <= pageIndex) {
        pages.add("")
    }
    pages[pageIndex] = pageBody
    return joinNotePages(pages)
}

fun appendNotePage(serializedBody: String, pageBody: String = ""): String {
    return joinNotePages(splitNotePages(serializedBody) + pageBody)
}

