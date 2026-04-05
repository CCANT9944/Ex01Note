package com.example.ex01

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class LastOpenNoteRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    private val key = "last_open_note_id"

    fun setLastOpenNoteId(noteId: Int) {
        prefs.edit { putInt(key, noteId) }
    }

    fun lastOpenNoteId(): Int? {
        return if (prefs.contains(key)) prefs.getInt(key, -1) else null
    }

    fun clearLastOpenNoteId() {
        prefs.edit { remove(key) }
    }
}

