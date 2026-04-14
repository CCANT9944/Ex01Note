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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CollapsedFoldersRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    private fun keyForFolder(id: Int) = "folder_collapsed_$id"

    fun setCollapsed(folderId: Int, collapsed: Boolean) {
        prefs.edit { putBoolean(keyForFolder(folderId), collapsed) }
    }

    fun isCollapsedFlow(folderId: Int): Flow<Boolean> = callbackFlow {
        val key = keyForFolder(folderId)
        // emit current value
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

