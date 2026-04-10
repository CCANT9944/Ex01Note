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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import androidx.glance.layout.ContentScale
import androidx.glance.ImageProvider
import androidx.glance.Image
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.content.res.Configuration
import androidx.glance.appwidget.action.actionStartActivity
import com.example.ex01.Note
import com.example.ex01.NoteDatabase
import com.example.ex01.NoteItem
import com.example.ex01.deserializeDrawing

class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        val prefs = context.getSharedPreferences("notes_widget_prefs", Context.MODE_PRIVATE)
        val noteId = prefs.getInt("widget_$appWidgetId", -1)
        val snoteId = prefs.getInt("widget_snote_$appWidgetId", -1) // Fetch SNote setting!

        val dao = NoteDatabase.getDatabase(context).noteDao()

        val checklistNote = if (noteId != -1) dao.getNoteByIdOnce(noteId) else null
        val items = if (noteId != -1) dao.getItemsForNoteOnce(noteId) else emptyList()
        val snoteNote = if (snoteId != -1) dao.getNoteByIdOnce(snoteId) else null

        provideContent {
            WidgetContent(context = context, note = checklistNote, checklist = items, snoteNote = snoteNote) // Pass both!
        }
    }
}

fun renderSNoteChunks(context: Context, snoteBody: String): List<Bitmap> {
    val lines = try {
        deserializeDrawing(snoteBody)
    } catch (e: Exception) {
        emptyList()
    }
    if (lines.isEmpty()) return emptyList()

    val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    for (line in lines) {
        if (line.isEraser || line.points.size < 2) continue // Ignore eraser strokes and purely single-tap garbage bounds
        for (point in line.points) {
            if (point.x < minX) minX = point.x
            if (point.y < minY) minY = point.y
            if (point.x > maxX) maxX = point.x
            if (point.y > maxY) maxY = point.y
        }
    }
    
    if (minX == Float.MAX_VALUE || maxX < minX || maxY < minY) {
        return emptyList() // If there are no usable lines, return nothing
    }

    // Anchor exactly to the physical canvas layout width to prevent widget from vertically over-stretching tight crops
    val renderMinX = 0f
    val renderMaxX = maxOf(maxX + 64f, 1000f)
    val renderMinY = maxOf(0f, minY - 64f)
    val renderMaxY = maxY + 64f

    val scale = 0.6f

    val totalWidth = ((renderMaxX - renderMinX) * scale).toInt().coerceAtLeast(100)
    val totalHeight = ((renderMaxY - renderMinY) * scale).toInt().coerceAtLeast(100)

    val maxChunkHeight = 4096
    val chunks = mutableListOf<Bitmap>()
    var currentY = 0

    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    val clearPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    while (currentY < totalHeight) {
        val chunkHeight = minOf(maxChunkHeight, totalHeight - currentY)
        val bitmap = Bitmap.createBitmap(totalWidth, chunkHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.TRANSPARENT)

        canvas.save()
        canvas.translate(0f, -currentY.toFloat())
        canvas.scale(scale, scale)
        canvas.translate(-renderMinX, -renderMinY)

        for (line in lines) {
            val path = android.graphics.Path()
            if (line.points.isNotEmpty()) {
                path.moveTo(line.points.first().x, line.points.first().y)
                for (i in 1 until line.points.size) {
                    path.lineTo(line.points[i].x, line.points[i].y)
                }
            }

            // Make the strokes slightly thicker for widget visibility without having to zoom the full Canvas
            val widgetStrokeMultiplier = 2.5f

            if (line.isEraser) {
                clearPaint.strokeWidth = line.strokeWidth * widgetStrokeMultiplier
                canvas.drawPath(path, clearPaint)
            } else {
                paint.strokeWidth = line.strokeWidth * widgetStrokeMultiplier
                var uiColor = line.color
                
                if (isNightMode) {
                    val luminance = (0.299f * uiColor.red) + (0.587f * uiColor.green) + (0.114f * uiColor.blue)
                    if (luminance < 0.4f) {
                        uiColor = Color.White
                    }
                }
                
                paint.color = android.graphics.Color.argb(
                    (uiColor.alpha * 255).toInt(),
                    (uiColor.red * 255).toInt(),
                    (uiColor.green * 255).toInt(),
                    (uiColor.blue * 255).toInt()
                )
                canvas.drawPath(path, paint)
            }
        }
        canvas.restore()
        chunks.add(bitmap)
        currentY += chunkHeight
    }
    return chunks
}

@Composable
fun WidgetContent(context: Context, note: Note?, checklist: List<NoteItem>, snoteNote: Note?) {
    val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    val bgColor = if (isNightMode) Color(0xFF141218) else Color.White
    val textColor = if (isNightMode) Color.White else Color.DarkGray
    val titleColor = if (isNightMode) Color.White else Color.Black
    val dividerColor = if (isNightMode) Color.DarkGray else Color.LightGray

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(day = bgColor, night = bgColor))
            .cornerRadius(16.dp)
    ) {
        // Left Column for Checklist
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
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
                    text = "Setup Required\n(Tap to refresh)",
                    style = TextStyle(color = ColorProvider(day = Color.Red, night = Color.Red))
                )
                return@Column
            }

            Text(
                text = note.title.ifBlank { "Untitled List" },
                style = TextStyle(
                    color = ColorProvider(day = titleColor, night = titleColor),
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
                                style = TextStyle(
                                    color = ColorProvider(day = if (item.isChecked) Color.Gray else textColor, night = if (item.isChecked) Color.Gray else textColor),
                                    textDecoration = if (item.isChecked) androidx.glance.text.TextDecoration.LineThrough else androidx.glance.text.TextDecoration.None
                                )
                            )
                        }
                    }
                }
            }
        }

        // Divider
        Spacer(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ColorProvider(day = dividerColor, night = dividerColor))
        )

        // Right Column for SNote
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            val snoteIntentAction = actionStartActivity(
                Intent(context, com.example.ex01.MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    if (snoteNote != null) {
                        putExtra("widget_note_id", snoteNote.id)
                    }
                }
            )

            if (snoteNote != null) {
                Text(
                    text = snoteNote.title.ifBlank { "Untitled SNote" },
                    style = TextStyle(
                        color = ColorProvider(day = titleColor, night = titleColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    modifier = GlanceModifier.clickable(snoteIntentAction)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))

                val chunks = renderSNoteChunks(context, snoteNote.body)
                if (chunks.isEmpty()) {
                    Box(modifier = GlanceModifier.fillMaxSize().clickable(snoteIntentAction), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Empty SNote",
                            style = TextStyle(color = ColorProvider(day = Color.Gray, night = Color.Gray))
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        items(chunks) { bmp ->
                            Image(
                                provider = ImageProvider(bmp),
                                contentDescription = "SNote Drawing",
                                contentScale = ContentScale.Fit,
                                modifier = GlanceModifier.fillMaxWidth().clickable(snoteIntentAction)
                            )
                        }
                    }
                }
            } else {
                Box(modifier = GlanceModifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Setup Required",
                        style = TextStyle(color = ColorProvider(day = Color.Red, night = Color.Red))
                    )
                }
            }
        }
    }
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}
