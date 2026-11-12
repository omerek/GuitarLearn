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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.data.Chord
import com.omerek.guitarlearn.ui.theme.FingerColors
import com.omerek.guitarlearn.ui.theme.FretSilver
import com.omerek.guitarlearn.ui.theme.Fretboard
import com.omerek.guitarlearn.ui.theme.FretboardLight
import com.omerek.guitarlearn.ui.theme.InTune
import com.omerek.guitarlearn.ui.theme.NutIvory
import com.omerek.guitarlearn.ui.theme.OffTune
import com.omerek.guitarlearn.ui.theme.StringColor

private val StringNames = listOf("E", "A", "D", "G", "B", "e")

/**
 * Akor şeması: gerçek bir gitar sapı gibi ahşap klavye, fildişi eşik, gümüş perdeler.
 * Dikey çizgiler teller (solda kalın Mi), noktalar parmak yerleri; renk ve sayı hangi parmak olduğunu söyler.
 * Üstte O = açık tel, X = çalma.
 */
@Composable
fun ChordDiagram(chord: Chord, modifier: Modifier = Modifier, width: Dp = 140.dp, showStringNames: Boolean = true) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val pressed = chord.frets.filter { it > 0 }
    val maxFret = pressed.maxOrNull() ?: 1
    val startFret = if (maxFret <= 4) 1 else pressed.min()
    val fretCount = 4

    Canvas(modifier = modifier.width(width).aspectRatio(if (showStringNames) 0.74f else 0.82f)) {
        val u = size.width / 140f
        val boardLeft = 22f * u
        val boardRight = size.width - 10f * u
        val boardTop = 28f * u
        val boardBottom = size.height - (if (showStringNames) 20f else 6f) * u
        val pad = 9f * u
        val firstString = boardLeft + pad
        val lastString = boardRight - pad
        val stringGap = (lastString - firstString) / 5f
        val fretGap = (boardBottom - boardTop) / fretCount

        // Klavye (ahşap)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Fretboard, FretboardLight, Fretboard), boardLeft, boardRight),
            topLeft = Offset(boardLeft, boardTop),
            size = Size(boardRight - boardLeft, boardBottom - boardTop),
            cornerRadius = CornerRadius(6f * u, 6f * u),
        )
        // Sedef işaretler (3, 5, 7, 9. perde)
        for (f in 0 until fretCount) {
            val absolute = startFret + f
            if (absolute in listOf(3, 5, 7, 9, 15)) {
                drawCircle(
                    NutIvory.copy(alpha = 0.55f),
                    radius = 4.5f * u,
                    center = Offset((boardLeft + boardRight) / 2, boardTop + (f + 0.5f) * fretGap),
                )
            }
        }
        // Perdeler
        for (f in 1..fretCount) {
            val y = boardTop + f * fretGap
            drawLine(Color.Black.copy(alpha = 0.35f), Offset(boardLeft, y + 1.5f * u), Offset(boardRight, y + 1.5f * u), strokeWidth = 1.5f * u)
            drawLine(FretSilver, Offset(boardLeft, y), Offset(boardRight, y), strokeWidth = 2.6f * u)
        }
        // Eşik (1. perdeden başlıyorsa kalın fildişi)
        if (startFret == 1) {
            drawRoundRect(
                NutIvory,
                topLeft = Offset(boardLeft - 1f * u, boardTop - 5f * u),
                size = Size(boardRight - boardLeft + 2f * u, 8f * u),
                cornerRadius = CornerRadius(2f * u, 2f * u),
            )
        } else {
            drawLine(FretSilver, Offset(boardLeft, boardTop), Offset(boardRight, boardTop), strokeWidth = 2.6f * u)
            val label = measurer.measure(
                "$startFret.",
                TextStyle(fontSize = (12 * u).sp, color = labelColor, fontWeight = FontWeight.Bold),
            )
            drawText(label, topLeft = Offset(boardLeft - label.size.width - 3f * u, boardTop + fretGap / 2 - label.size.height / 2))
        }
        // Teller (kalından inceye)
        for (s in 0 until 6) {
            val x = firstString + s * stringGap
            val w = (3.4f - s * 0.42f) * u
            drawLine(Color.Black.copy(alpha = 0.4f), Offset(x + 1.2f * u, boardTop), Offset(x + 1.2f * u, boardBottom), strokeWidth = w)
            drawLine(StringColor, Offset(x, boardTop), Offset(x, boardBottom), strokeWidth = w)
        }

        val radius = stringGap * 0.42f
        // Barre
        chord.barreFret?.let { bf ->
            val strings = chord.frets.indices.filter { chord.frets[it] >= bf }
            if (strings.size > 1) {
                val y = boardTop + (bf - startFret + 0.5f) * fretGap
                val x0 = firstString + strings.first() * stringGap
                val x1 = firstString + strings.last() * stringGap
                val h = radius * 2
                drawRoundRect(
                    Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(x0 - radius + 1.5f * u, y - radius + 2f * u),
                    size = Size(x1 - x0 + h, h),
                    cornerRadius = CornerRadius(radius, radius),
                )
                drawRoundRect(
                    FingerColors[0],
                    topLeft = Offset(x0 - radius, y - radius),
                    size = Size(x1 - x0 + h, h),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
        }

        val markerY = boardTop - 15f * u
        chord.frets.forEachIndexed { s, fret ->
            val x = firstString + s * stringGap
            when {
                fret < 0 -> {
                    val d = 5f * u
                    drawLine(OffTune, Offset(x - d, markerY - d), Offset(x + d, markerY + d), strokeWidth = 2.4f * u)
                    drawLine(OffTune, Offset(x - d, markerY + d), Offset(x + d, markerY - d), strokeWidth = 2.4f * u)
                }
                fret == 0 -> drawCircle(InTune, 5.5f * u, Offset(x, markerY), style = Stroke(2.4f * u))
                else -> {
                    val y = boardTop + (fret - startFret + 0.5f) * fretGap
                    val finger = chord.fingers[s]
                    val color = FingerColors.getOrElse(finger - 1) { FingerColors[0] }
                    drawCircle(Color.Black.copy(alpha = 0.35f), radius, Offset(x + 1.5f * u, y + 2f * u))
                    drawCircle(color, radius, Offset(x, y))
                    drawCircle(Color.White.copy(alpha = 0.25f), radius * 0.45f, Offset(x - radius * 0.3f, y - radius * 0.3f))
                    if (finger > 0) {
                        val t = measurer.measure(
                            "$finger",
                            TextStyle(fontSize = (12 * u).sp, color = Color.White, fontWeight = FontWeight.ExtraBold),
                        )
                        drawText(t, topLeft = Offset(x - t.size.width / 2, y - t.size.height / 2))
                    }
                }
            }
        }

        if (showStringNames) {
            StringNames.forEachIndexed { s, name ->
                val t = measurer.measure(name, TextStyle(fontSize = (10 * u).sp, color = labelColor))
                drawText(t, topLeft = Offset(firstString + s * stringGap - t.size.width / 2, boardBottom + 4f * u))
            }
        }
    }
}
