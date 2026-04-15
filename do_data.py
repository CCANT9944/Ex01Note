new_data = """package com.example.ex01.ui.editor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
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
@Serializer(forClass = Color::class)
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeLong(value.value.toLong())
    }
    override fun deserialize(decoder: Decoder): Color {
        return Color(decoder.decodeLong().toULong())
    }
}
@Serializable
@SerialName("Offset")
private data class OffsetSurrogate(val x: Float, val y: Float)
object OffsetSerializer : KSerializer<Offset> {
    private val surrogateSerializer = OffsetSurrogate.serializer()
    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor
    override fun serialize(encoder: Encoder, value: Offset) {
        val surrogate = OffsetSurrogate(value.x, value.y)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }
    override fun deserialize(decoder: Decoder): Offset {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return Offset(surrogate.x, surrogate.y)
    }
}
@Serializable
data class DrawingLine(
    val points: List<@Serializable(with = OffsetSerializer::class) Offset>,
    @Serializable(with = ColorSerializer::class) val color: Color = Color.Black,
    @SerialName("stroke") val strokeWidth: Float = 5f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val text: String? = null
) {
    fun toPath(): Path {
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
        }
        return path
    }
}
private val jsonFormat = Json { 
    ignoreUnknownKeys = true 
    encodeDefaults = true 
    isLenient = true
}
fun serializeDrawing(lines: List<DrawingLine>): String {
    return jsonFormat.encodeToString(lines)
}
fun deserializeDrawing(jsonStr: String): List<DrawingLine> {
    if (jsonStr.isBlank()) return emptyList()
    return try {
        val parsedLines = jsonFormat.decodeFromString<List<DrawingLine>>(jsonStr)
        // Ensure older instances map their heuristic colors exactly like before!
        parsedLines.map { line ->
            val inferredEraser = line.isEraser || (!jsonStr.contains("isEraser") && (line.strokeWidth >= 20f || line.color == Color(0xFFFFFBFE) || line.color == Color(0xFF1C1B1F) || line.color == Color(0xFF141218)))
            val finalColor = if (!inferredEraser && line.color !in ALLOWED_PEN_COLORS) {
                Color.Unspecified
            } else {
                line.color
            }
            line.copy(color = finalColor, isEraser = inferredEraser)
        }
    } catch(e: Exception) {
        // Absolute fallback ensuring user NEVER loses data if kotlinx fails to parse legacy org.json shapes!
        legacyDeserializeDrawing(jsonStr)
    }
}
private fun legacyDeserializeDrawing(json: String): List<DrawingLine> {
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
            val inferredEraser = isEraser || (!lineObj.has("isEraser") && (stroke >= 20f || rawColor == Color(0xFFFFFBFE) || rawColor == Color(0xFF1C1B1F) || rawColor == Color(0xFF141218)))
            val finalColor = if (!inferredEraser && rawColor !in ALLOWED_PEN_COLORS) {
                Color.Unspecified
            } else {
                rawColor
            }
            lines.add(DrawingLine(points, finalColor, stroke, inferredEraser, isHighlighter, text))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return lines
}
"""
with open(r"C:\Users\guest_xnqhrf8\AndroidStudioProjects\ex01\app\src\main\java\com\example\ex01\ui\editor\SNoteData.kt", "w", encoding="utf-8") as f:
    f.write(new_data)
print("Kotlinx Serialization migration executed!")
