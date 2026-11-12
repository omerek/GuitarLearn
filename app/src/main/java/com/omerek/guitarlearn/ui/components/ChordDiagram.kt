package com.omerek.guitarlearn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.data.Chord

/**
 * Akor şeması çizer: dikey çizgiler teller (solda kalın Mi), yatay çizgiler perdeler.
 * Noktaların içinde parmak numarası yazar; üstte O = açık tel, X = çalma.
 */
@Composable
fun ChordDiagram(chord: Chord, modifier: Modifier = Modifier, width: Dp = 140.dp) {
    val measurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.onSurface
    val dotColor = MaterialTheme.colorScheme.primary
    val onDot = MaterialTheme.colorScheme.onPrimary
    val muted = MaterialTheme.colorScheme.error

    val pressed = chord.frets.filter { it > 0 }
    val maxFret = pressed.maxOrNull() ?: 1
    val startFret = if (maxFret <= 4) 1 else (pressed.min())
    val fretCount = 4

    Canvas(modifier = modifier.width(width).aspectRatio(0.82f)) {
        val scale = size.width / 140f
        val left = 26f * scale
        val right = size.width - 14f * scale
        val top = 30f * scale
        val bottom = size.height - 8f * scale
        val stringGap = (right - left) / 5f
        val fretGap = (bottom - top) / fretCount
        val stroke = 1.6f * scale

        // Perde çizgileri
        for (f in 0..fretCount) {
            val y = top + f * fretGap
            val w = if (f == 0 && startFret == 1) 6f * scale else stroke
            drawLine(lineColor, Offset(left, y), Offset(right, y), strokeWidth = w)
        }
        // Teller
        for (s in 0 until 6) {
            val x = left + s * stringGap
            drawLine(lineColor, Offset(x, top), Offset(x, bottom), strokeWidth = stroke * (1.6f - s * 0.12f))
        }
        // Başlangıç perdesi numarası (ör. "5fr")
        if (startFret > 1) {
            val label = measurer.measure("${startFret}", TextStyle(fontSize = (11 * scale).sp, color = lineColor))
            drawText(label, topLeft = Offset(2f * scale, top + fretGap / 2 - label.size.height / 2))
        }
        // Barre
        chord.barreFret?.let { bf ->
            val row = bf - startFret
            val strings = chord.frets.indices.filter { chord.frets[it] >= bf }
            if (strings.size > 1) {
                val y = top + (row + 0.5f) * fretGap
                val x0 = left + strings.first() * stringGap
                val x1 = left + strings.last() * stringGap
                val h = stringGap * 0.7f
                drawRoundRect(
                    dotColor,
                    topLeft = Offset(x0 - h / 2, y - h / 2),
                    size = Size(x1 - x0 + h, h),
                    cornerRadius = CornerRadius(h / 2, h / 2),
                )
            }
        }
        val radius = stringGap * 0.36f
        val markStyle = TextStyle(fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
        chord.frets.forEachIndexed { s, fret ->
            val x = left + s * stringGap
            when {
                fret < 0 -> {
                    val t = measurer.measure("X", markStyle.copy(color = muted))
                    drawText(t, topLeft = Offset(x - t.size.width / 2, top - 6f * scale - t.size.height))
                }
                fret == 0 -> {
                    drawCircle(lineColor, radius * 0.7f, Offset(x, top - 6f * scale - radius * 0.8f), style = Stroke(stroke))
                }
                else -> {
                    val y = top + (fret - startFret + 0.5f) * fretGap
                    drawCircle(dotColor, radius, Offset(x, y))
                    val finger = chord.fingers[s]
                    if (finger > 0) {
                        val t = measurer.measure("$finger", TextStyle(fontSize = (11 * scale).sp, color = onDot, fontWeight = FontWeight.Bold))
                        drawText(t, topLeft = Offset(x - t.size.width / 2, y - t.size.height / 2))
                    }
                }
            }
        }
    }
}

