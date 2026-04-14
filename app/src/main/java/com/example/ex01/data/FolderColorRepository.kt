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

class FolderColorRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    private fun keyForFolder(id: Int) = "folder_icon_color_$id"

    fun setColor(folderId: Int, color: Long?) {
        prefs.edit {
            if (color == null) {
                remove(keyForFolder(folderId))
            } else {
                putLong(keyForFolder(folderId), color)
            }
        }
    }

    fun colorFlow(folderId: Int): Flow<Long?> = callbackFlow {
        val key = keyForFolder(folderId)
        val sentinel = Long.MIN_VALUE

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getLong(key, sentinel).takeIf { it != sentinel })
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getLong(key, sentinel).takeIf { it != sentinel })
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}

