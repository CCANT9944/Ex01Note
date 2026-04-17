package com.example.ex01.ui.editor.snote
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import org.json.JSONArray
import org.json.JSONObject
const val PREFS_NAME = "snote_settings"
const val PEN_THIN = 3f
const val PEN_MEDIUM = 6f
const val PEN_THICK = 12f

const val ERASER_THIN = 20f
const val ERASER_MEDIUM = 40f
const val ERASER_THICK = 80f

const val HIGHLIGHTER_THIN = 15f
const val HIGHLIGHTER_MEDIUM = 30f
const val HIGHLIGHTER_THICK = 50f

const val TEXT_MEDIUM = 40f
const val TEXT_LARGE = 64f

val ALLOWED_PEN_COLORS = listOf(
    Color.Red,
    Color.Blue,
    Color.Green,
    Color(0xFFA52A2A), // Brown
    Color(0xFFFF9999), // Light Red
    Color(0xFF99FF99), // Light Green
    Color(0xFF99CCFF), // Light Blue
    Color.Yellow
)

data class DrawingLine(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val text: String? = null
) {
    @Transient // Prevent potential serialization/reflection problems
    private var _cachedPath: Path? = null

    fun toPath(): Path {
        if (_cachedPath != null) return _cachedPath!!
        val p = Path()
        if (points.isNotEmpty()) {
            p.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                p.lineTo(points[i].x, points[i].y)
            }
        }
        _cachedPath = p
        return p
    }
}

fun serializeDrawing(lines: List<DrawingLine>): String {
    val jsonArray = JSONArray()
    for (line in lines) {
        val pointArray = JSONArray()
        for (point in line.points) {
            val pObj = JSONObject()
            pObj.put("x", point.x)
            pObj.put("y", point.y)
            pointArray.put(pObj)
        }
        val lineObj = JSONObject()
        lineObj.put("points", pointArray)
        lineObj.put("color", line.color.value.toLong())
        lineObj.put("stroke", line.strokeWidth.toDouble()) // Float cast
        lineObj.put("isEraser", line.isEraser)
        lineObj.put("isHighlighter", line.isHighlighter)
        if (line.text != null) {
            lineObj.put("text", line.text)
        }
        jsonArray.put(lineObj)
    }
    return jsonArray.toString()
}

fun deserializeDrawing(json: String): List<DrawingLine> {
    if (json.isBlank()) return emptyList()
    val lines = mutableListOf<DrawingLine>()
    try {
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val lineObj = jsonArray.getJSONObject(i)
            val pointArray = lineObj.getJSONArray("points")
            val points = mutableListOf<Offset>()
            for (p in 0 until pointArray.length()) {
                val pObj = pointArray.getJSONObject(p)
                points.add(Offset(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
            }
            val rawColor = Color(lineObj.optLong("color", Color.Unspecified.value.toLong()).toULong())
            val stroke = lineObj.optDouble("stroke", 5.0).toFloat()
            val isEraser = lineObj.optBoolean("isEraser", false)
            val isHighlighter = lineObj.optBoolean("isHighlighter", false)
            val text = if (lineObj.has("text")) lineObj.getString("text") else null

            // Heuristic for old lines without isEraser flag: old eraser lines either matched the default M3 surface colors or were much thicker (min eraser thickness is 20f, max pen is 12f).
            val inferredEraser = isEraser || (!lineObj.has("isEraser") && (stroke >= 20f || rawColor == Color(0xFFFFFBFE) || rawColor == Color(0xFF1C1B1F) || rawColor == Color(0xFF141218)))

            // For older drawings that hardcoded "black" or "white" or specific theme surface colors, fallback to Unspecified so it dynamically adapts.
            val finalColor = if (!inferredEraser && rawColor !in ALLOWED_PEN_COLORS) {
                Color.Unspecified
            } else {
                rawColor
            }

            val line = DrawingLine(points, finalColor, stroke, inferredEraser, isHighlighter, text)
            line.toPath() // precalculate in background to prevent UI freeze
            lines.add(line)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return lines
}
