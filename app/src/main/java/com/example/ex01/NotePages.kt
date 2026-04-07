@file:Suppress("unused")

package com.example.ex01

private const val NOTE_PAGE_SEPARATOR = '\u000C'
private const val PAGE_NAME_SEPARATOR = '\u000B'

data class NotePage(
    val name: String,
    val body: String
)

fun parseNotePage(raw: String, index: Int): NotePage {
    return if (raw.startsWith(PAGE_NAME_SEPARATOR)) {
        val nextSepIndex = raw.indexOf(PAGE_NAME_SEPARATOR, 1)
        if (nextSepIndex > 0) {
            val name = raw.substring(1, nextSepIndex)
            val body = raw.substring(nextSepIndex + 1)
            NotePage(name, body)
        } else {
            NotePage("Page", raw.substring(1))
        }
    } else {
        NotePage("Page", raw)
    }
}

fun formatNotePage(page: NotePage): String {
    return "$PAGE_NAME_SEPARATOR${page.name}$PAGE_NAME_SEPARATOR${page.body}"
}

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
    val raw = splitNotePages(serializedBody).getOrElse(pageIndex) { "" }
    return parseNotePage(raw, pageIndex).body
}

fun replaceNotePage(serializedBody: String, pageIndex: Int, pageBody: String): String {
    val pages = splitNotePages(serializedBody).toMutableList()
    val parsed = if (pageIndex < pages.size) parseNotePage(pages[pageIndex], pageIndex) else NotePage(if (pageIndex == 0) "Main" else "Page", "")
    val newPage = parsed.copy(body = pageBody)
    while (pages.size <= pageIndex) {
        pages.add("")
    }
    pages[pageIndex] = formatNotePage(newPage)
    return joinNotePages(pages)
}

fun appendNotePage(serializedBody: String, pageBody: String = ""): String {
    return joinNotePages(splitNotePages(serializedBody) + formatNotePage(NotePage("Page", pageBody)))
}

fun deleteNotePage(serializedBody: String, pageIndex: Int): String {
    val pages = splitNotePages(serializedBody).toMutableList()
    if (pageIndex in pages.indices) {
        pages.removeAt(pageIndex)
    }
    if (pages.isEmpty()) {
        return ""
    }
    return joinNotePages(pages)
}

fun renameNotePage(serializedBody: String, pageIndex: Int, newName: String): String {
    val pages = splitNotePages(serializedBody).toMutableList()
    val parsed = if (pageIndex < pages.size) parseNotePage(pages[pageIndex], pageIndex) else NotePage("", "")
    val newPage = parsed.copy(name = newName)
    while (pages.size <= pageIndex) {
        pages.add("")
    }
    pages[pageIndex] = formatNotePage(newPage)
    return joinNotePages(pages)
}
