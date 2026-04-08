package com.example.ex01

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class DrawingLine(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f
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
            val color = Color(lineObj.optLong("color", Color.Black.value.toLong()).toULong())
            val stroke = lineObj.optDouble("stroke", 5.0).toFloat()
            lines.add(DrawingLine(points, color, stroke))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return lines
}

@Composable
fun SNoteEditor(
    serializedBody: String,
    onSerializedBodyChange: (String) -> Unit
) {
    val drawingLines = remember { mutableStateListOf<DrawingLine>() }
    var currentPath by remember { mutableStateOf<List<Offset>?>(null) }
    var currentProperties by remember { mutableStateOf(DrawingLine(emptyList())) }
    var isEraserMode by remember { mutableStateOf(false) }
    var currentThickness by remember { mutableStateOf(6f) }
    var showThicknessMenu by remember { mutableStateOf(false) }
    var currentEraserThickness by remember { mutableStateOf(40f) }
    var showEraserThicknessMenu by remember { mutableStateOf(false) }

    LaunchedEffect(serializedBody) {
        if (drawingLines.isEmpty() && serializedBody.isNotBlank() && currentPath == null) {
            drawingLines.addAll(deserializeDrawing(serializedBody))
        }
    }

    val commitChanges = {
        onSerializedBodyChange(serializeDrawing(drawingLines))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                IconButton(
                    onClick = {
                        if (!isEraserMode) {
                            showThicknessMenu = true
                        } else {
                            isEraserMode = false
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (!isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Icon(Icons.Default.Create, contentDescription = "Pen")
                }
                DropdownMenu(
                    expanded = showThicknessMenu,
                    onDismissRequest = { showThicknessMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Thin") }, onClick = { currentThickness = 3f; showThicknessMenu = false })
                    DropdownMenuItem(text = { Text("Medium") }, onClick = { currentThickness = 6f; showThicknessMenu = false })
                    DropdownMenuItem(text = { Text("Thick") }, onClick = { currentThickness = 12f; showThicknessMenu = false })
                }
            }
            Box {
                IconButton(
                    onClick = {
                        if (isEraserMode) {
                            showEraserThicknessMenu = true
                        } else {
                            isEraserMode = true
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isEraserMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Eraser")
                }
                DropdownMenu(
                    expanded = showEraserThicknessMenu,
                    onDismissRequest = { showEraserThicknessMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Thin") }, onClick = { currentEraserThickness = 20f; showEraserThicknessMenu = false })
                    DropdownMenuItem(text = { Text("Medium") }, onClick = { currentEraserThickness = 40f; showEraserThicknessMenu = false })
                    DropdownMenuItem(text = { Text("Thick") }, onClick = { currentEraserThickness = 80f; showEraserThicknessMenu = false })
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = {
                drawingLines.clear()
                commitChanges()
            }) {
                Text("Clear All")
            }
        }

        val strokeColor = MaterialTheme.colorScheme.onSurface
        val eraserColor = MaterialTheme.colorScheme.surface

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(8.dp),
            color = eraserColor
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                val isStylus = change.type == PointerType.Stylus
                                val isStylusEraser = change.type == PointerType.Eraser

                                if (!isStylus && !isStylusEraser) continue

                                if (change.pressed) {
                                    if (currentPath == null) {
                                        currentPath = listOf(change.position)
                                        val actualEraserMode = isStylusEraser || isEraserMode
                                        currentProperties = DrawingLine(
                                            points = currentPath!!,
                                            color = if (actualEraserMode) eraserColor else strokeColor,
                                            strokeWidth = if (actualEraserMode) currentEraserThickness else currentThickness
                                        )
                                    } else {
                                        currentPath = currentPath!! + change.position
                                    }
                                } else {
                                    if (currentPath != null) {
                                        drawingLines.add(currentProperties.copy(points = currentPath!!))
                                        currentPath = null
                                        commitChanges()
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Draw saved lines
                drawingLines.forEach { line ->
                    drawPath(
                        path = line.toPath(),
                        color = line.color,
                        style = Stroke(
                            width = line.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Draw active line
                currentPath?.let { pathOffsets ->
                    val activeLine = currentProperties.copy(points = pathOffsets)
                    drawPath(
                        path = activeLine.toPath(),
                        color = activeLine.color,
                        style = Stroke(
                            width = activeLine.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
