package com.example.ex01.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.color.ColorProvider
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.ImageProvider
import androidx.glance.Image
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import android.content.ComponentName
import android.content.Intent
import androidx.glance.appwidget.action.actionStartActivity
import com.example.ex01.Note
import com.example.ex01.NoteDatabase
import com.example.ex01.NoteItem

class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        val prefs = context.getSharedPreferences("notes_widget_prefs", Context.MODE_PRIVATE)
        val noteId = prefs.getInt("widget_$appWidgetId", -1)

        val dao = NoteDatabase.getDatabase(context).noteDao()

        val note = if (noteId != -1) dao.getNoteByIdOnce(noteId) else null
        val items = if (noteId != -1) dao.getItemsForNoteOnce(noteId) else emptyList()

        provideContent {
            WidgetContent(context = context, note = note, checklist = items)
        }
    }
}

@Composable
fun WidgetContent(context: Context, note: Note?, checklist: List<NoteItem>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(day = Color.White, night = Color.White))
            .cornerRadius(16.dp)
            .padding(16.dp)
            // Opens the app and passes the specific noteId to open the Checklist!
            .clickable(
                actionStartActivity(
                    Intent(context, com.example.ex01.MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        if (note != null) {
                            putExtra("widget_note_id", note.id)
                        }
                    }
                )
            )
    ) {
        if (note == null) {
            Text(
                text = "Setup Required",
                style = TextStyle(color = ColorProvider(day = Color.Red, night = Color.Red))
            )
            return@Column 
        }

        Text(
            text = note.title.ifBlank { "Untitled List" },
            style = TextStyle(
                color = ColorProvider(day = Color.Black, night = Color.White),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        if (checklist.isEmpty()) {
            Text(
                text = "List is empty.",
                style = TextStyle(color = ColorProvider(day = Color.Gray, night = Color.LightGray))
            )
        } else {
            LazyColumn {
                items(checklist) { item ->
                    val intentAction = actionStartActivity(
                        Intent(context, com.example.ex01.MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("widget_note_id", note.id)
                        }
                    )
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(intentAction),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconRes = if (item.isChecked) 
                            android.R.drawable.checkbox_on_background 
                        else 
                            android.R.drawable.checkbox_off_background
                        
                        Image(
                            provider = ImageProvider(iconRes),
                            contentDescription = "Checkbox",
                            modifier = GlanceModifier.width(20.dp).height(20.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = item.text,
                            style = TextStyle(color = ColorProvider(day = Color.DarkGray, night = Color.LightGray))
                        )
                    }
                }
            }
        }
    }
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}
