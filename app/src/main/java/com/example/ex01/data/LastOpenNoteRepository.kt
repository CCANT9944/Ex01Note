package com.example.ex01.data

import com.example.ex01.*
import com.example.ex01.data.*
import com.example.ex01.ui.screens.*
import com.example.ex01.ui.editor.*
import com.example.ex01.ui.dialogs.*
import com.example.ex01.ui.components.*
import com.example.ex01.ui.theme.*
import com.example.ex01.widget.*


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

