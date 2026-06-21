package com.helix.flashcards.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.UUID
import android.graphics.Path as AndroidPath

private const val STROKE_COLOR = 0xFFE6E6E6.toInt()

@Composable
fun HandwritingScreen(
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var eraserMode by remember { mutableStateOf(false) }
    var eraserPos by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current
    val widthPx = with(density) { 3.dp.toPx() }
    val eraseRadius = with(density) { 22.dp.toPx() }

    fun eraseAt(p: Offset) {
        // remove whole strokes that the eraser touches
        strokes.removeAll { stroke -> stroke.any { (it - p).getDistance() <= eraseRadius } }
    }

    Column(Modifier.fillMaxSize().background(DarkBg)) {
        // Toolbar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) { Text("Cancel", color = Accent) }
            Spacer(Modifier.weight(1f))

            // Pen / Eraser toggle
            FilterChip(
                selected = eraserMode,
                onClick = { eraserMode = !eraserMode },
                label = { Text(if (eraserMode) "Eraser" else "Pen") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkSurface,
                    labelColor = Accent,
                    selectedContainerColor = DarkCard,
                    selectedLabelColor = AccentLight
                )
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }, enabled = strokes.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = AccentLight)
            }
            IconButton(onClick = { strokes.clear() }, enabled = strokes.isNotEmpty()) {
                Icon(Icons.Default.Clear, "Clear", tint = AccentLight)
            }
            TextButton(onClick = {
                if (strokes.isNotEmpty()) {
                    val path = saveStrokesCropped(context, strokes, widthPx)
                    if (path != null) onSave(path) else onCancel()
                } else onCancel()
            }) { Text("Save", color = AccentLight) }
        }

        // Drawing surface
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .pointerInput(eraserMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // S Pen side button reports as a secondary button press
                        var erasing = eraserMode || currentEvent.buttons.isSecondaryPressed
                        var pts = listOf(down.position)
                        if (erasing) { eraserPos = down.position; eraseAt(down.position) }
                        else current = pts
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (event.buttons.isSecondaryPressed) erasing = true
                            if (!change.pressed) break
                            val pos = change.position
                            if (erasing) {
                                eraserPos = pos
                                eraseAt(pos)
                            } else {
                                pts = pts + pos
                                current = pts
                            }
                            change.consume()
                        }

                        if (!erasing && pts.size > 1) strokes.add(pts)
                        current = emptyList()
                        eraserPos = null
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                (strokes + listOf(current)).forEach { pts ->
                    if (pts.size > 1) {
                        drawPath(
                            buildSmoothPath(pts),
                            color = AccentLight,
                            style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
                // eraser cursor
                eraserPos?.let { p ->
                    drawCircle(color = Color(0x33FFFFFF), radius = eraseRadius, center = p)
                }
            }
            if (strokes.isEmpty() && current.isEmpty()) {
                Text(
                    "Write with your pen — side button erases",
                    color = Accent.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val cur = points[i]
        path.quadraticBezierTo(prev.x, prev.y, (prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
    }
    return path
}

/** Rasterize strokes to a transparent PNG cropped tightly to the writing + padding. */
private fun saveStrokesCropped(context: Context, strokes: List<List<Offset>>, widthPx: Float): String? {
    val all = strokes.flatten()
    if (all.isEmpty()) return null

    val pad = widthPx * 2f + 24f
    val minX = all.minOf { it.x } - pad
    val minY = all.minOf { it.y } - pad
    val maxX = all.maxOf { it.x } + pad
    val maxY = all.maxOf { it.y } + pad

    val w = (maxX - minX).toInt().coerceAtLeast(1)
    val h = (maxY - minY).toInt().coerceAtLeast(1)

    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = Paint().apply {
        color = STROKE_COLOR
        strokeWidth = widthPx
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    strokes.forEach { pts ->
        if (pts.size > 1) {
            val p = AndroidPath()
            p.moveTo(pts[0].x - minX, pts[0].y - minY)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]; val cur = pts[i]
                p.quadTo(prev.x - minX, prev.y - minY, (prev.x + cur.x) / 2f - minX, (prev.y + cur.y) / 2f - minY)
            }
            canvas.drawPath(p, paint)
        }
    }
    val file = File(context.filesDir, "draw_${UUID.randomUUID()}.png")
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return file.absolutePath
}