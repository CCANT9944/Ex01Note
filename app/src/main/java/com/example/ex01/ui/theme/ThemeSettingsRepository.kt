package com.example.ex01.ui.theme

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

class ThemeSettingsRepository(context: Context) {
	private val appContext = context.applicationContext
	private val prefs: SharedPreferences = appContext.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

	private val key = "theme_mode"
	private val defaultThemeMode = ThemeMode.LIGHT

	private fun normalizeThemeMode(stored: String?): ThemeMode {
		return when (stored) {
			ThemeMode.DARK.name -> ThemeMode.DARK
			else -> defaultThemeMode
		}
	}

	fun setThemeMode(themeMode: ThemeMode) {
		prefs.edit { putString(key, themeMode.name) }
	}

	fun themeModeFlow(): Flow<ThemeMode> = callbackFlow {
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
			if (changedKey == key) {
				trySend(normalizeThemeMode(prefs.getString(key, null)))
			}
		}

		prefs.registerOnSharedPreferenceChangeListener(listener)
		trySend(normalizeThemeMode(prefs.getString(key, null)))
		awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
	}
}


