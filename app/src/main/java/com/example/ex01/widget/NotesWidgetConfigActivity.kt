package com.example.ex01.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.ex01.Note
import com.example.ex01.NoteDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // By default, if the user backs out, the widget should not be created.
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WidgetConfigScreen(
                        onNotesSelected = { noteId, snoteId ->
                            saveSelectionAndFinish(noteId, snoteId)
                        }
                    )
                }
            }
        }
    }

    private fun saveSelectionAndFinish(noteId: Int, snoteId: Int) {
        val prefs = getSharedPreferences("notes_widget_prefs", MODE_PRIVATE)
        prefs.edit {
            putInt("widget_$appWidgetId", noteId)
            putInt("widget_snote_$appWidgetId", snoteId)
        }

        // Trigger an update on a background scope so it doesn't get cancelled by finish()
        val appContext = applicationContext
        GlobalScope.launch(Dispatchers.IO) {
            val glanceId = GlanceAppWidgetManager(appContext).getGlanceIdBy(appWidgetId)
            NotesWidget().update(appContext, glanceId)
        }

        // Also broadcast the standard update intent for safety
        val updateIntent = Intent(this, NotesWidgetReceiver::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        sendBroadcast(updateIntent)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun WidgetConfigScreen(onNotesSelected: (Int, Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var checklists by remember { mutableStateOf<List<Note>>(emptyList()) }
    var snotes by remember { mutableStateOf<List<Note>>(emptyList()) }

    var selectedChecklistId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        val dao = NoteDatabase.getDatabase(context).noteDao()
        checklists = dao.getAllChecklistsOnce()
        snotes = dao.getAllSNotesOnce()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedChecklistId == null) {
            Text("Step 1: Select a Checklist (Left Pane)", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (checklists.isEmpty()) {
                Text("No checklists available. Create one first!")
            } else {
                LazyColumn {
                    items(checklists) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedChecklistId = note.id }
                        ) {
                            Text(
                                text = note.title.ifBlank { "Untitled List" },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Text("Step 2: Select an SNote (Right Pane)", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (snotes.isEmpty()) {
                Text("No SNotes available. Create one first!")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onNotesSelected(selectedChecklistId!!, -1) }) {
                    Text("Skip SNote")
                }
            } else {
                LazyColumn {
                    items(snotes) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onNotesSelected(selectedChecklistId!!, note.id) }
                        ) {
                            Text(
                                text = note.title.ifBlank { "Untitled SNote" },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { onNotesSelected(selectedChecklistId!!, -1) }) {
                            Text("Skip SNote")
                        }
                    }
                }
            }
        }
    }
}
