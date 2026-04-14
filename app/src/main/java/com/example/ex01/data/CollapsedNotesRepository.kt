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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

class CollapsedNotesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    private fun keyForNote(id: Int) = "note_collapsed_$id"

    fun setCollapsed(noteId: Int, collapsed: Boolean) {
        prefs.edit { putBoolean(keyForNote(noteId), collapsed) }
    }

    fun isCollapsedFlow(noteId: Int): Flow<Boolean> = callbackFlow {
        val key = keyForNote(noteId)
        trySend(prefs.getBoolean(key, true))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getBoolean(key, true))
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}

