package com.omerek.guitarlearn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.data.Chord
import com.omerek.guitarlearn.led.LedArrangement
import com.omerek.guitarlearn.led.LedController
import com.omerek.guitarlearn.led.LedLayout
import com.omerek.guitarlearn.led.LedStatus
import com.omerek.guitarlearn.led.StringColors
import com.omerek.guitarlearn.ui.theme.FretSilver
import com.omerek.guitarlearn.ui.theme.Fretboard
import com.omerek.guitarlearn.ui.theme.FretboardLight
import com.omerek.guitarlearn.ui.theme.NutIvory
import com.omerek.guitarlearn.ui.theme.StringColor

/** Telin ekrandaki rengi (LED'deki renkle aynı). */
fun stringColor(stringNumber: Int): Color = Color(0xFF000000 or StringColors.of(stringNumber).toLong())

val MutedColor = Color(0xFF000000 or StringColors.MUTED.toLong())

/**
 * Gitarı çalarken gördüğün gibi yatay klavye: solda eşik, sağa doğru perdeler.
 * En ince tel (1. tel) en altta. Basılacak yerler telin renginde nokta, susturulan teller kırmızı X.
 */
@Composable
fun StringFretboard(chord: Chord, modifier: Modifier = Modifier, minFrets: Int = 5) {
    val notes = remember(chord) { LedLayout.pressedNotes(chord) }
    val muted = remember(chord) { LedLayout.mutedStrings(chord) }
    val frets = maxOf(minFrets, (notes.maxOfOrNull { it.fret } ?: 0) + 1)
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier.fillMaxWidth().aspectRatio(2.6f)) {
        val u = size.height / 100f
        val left = 18f * u
        val right = size.width - 4f * u
        val top = 6f * u
        val bottom = size.height - 18f * u
        val stringGap = (bottom - top) / 6f
        val fretGap = (right - left) / frets
        // 1. tel en altta, 6. tel en üstte
        fun yOf(stringNumber: Int) = top + (6 - stringNumber + 0.5f) * stringGap

        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Fretboard, FretboardLight, Fretboard), top, bottom),
            topLeft = Offset(left, top), size = Size(right - left, bottom - top), cornerRadius = CornerRadius(4f * u),
        )
        for (f in 1..frets) {
            val x = left + f * fretGap
            drawLine(FretSilver, Offset(x, top), Offset(x, bottom), strokeWidth = 1.8f * u)
            if (f in listOf(3, 5, 7, 9, 12, 15)) {
                drawCircle(NutIvory.copy(alpha = 0.45f), 2.6f * u, Offset(left + (f - 0.5f) * fretGap, (top + bottom) / 2))
            }
            val t = measurer.measure("$f", TextStyle(fontSize = (9f * u).toSp(), color = labelColor))
            drawText(t, topLeft = Offset(left + (f - 0.5f) * fretGap - t.size.width / 2, bottom + 2f * u))
        }
        drawRect(NutIvory, topLeft = Offset(left - 3f * u, top), size = Size(4f * u, bottom - top))
        for (s in 1..6) {
            val y = yOf(s)
            drawLine(StringColor, Offset(left, y), Offset(right, y), strokeWidth = (0.9f + (s - 1) * 0.28f) * u)
        }
        val r = stringGap * 0.38f
        for (n in notes) {
            val c = Offset(left + (n.fret - 0.5f) * fretGap, yOf(n.stringNumber))
            drawCircle(Color.Black.copy(alpha = 0.55f), r + 1.2f * u, c)
            drawCircle(stringColor(n.stringNumber), r, c)
            drawCircle(Color.White.copy(alpha = 0.35f), r * 0.4f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
        }
        for (s in muted) {
            val c = Offset(left - 10f * u, yOf(s))
            val d = r * 0.75f
            drawLine(MutedColor, Offset(c.x - d, c.y - d), Offset(c.x + d, c.y + d), strokeWidth = 2.2f * u)
            drawLine(MutedColor, Offset(c.x - d, c.y + d), Offset(c.x + d, c.y - d), strokeWidth = 2.2f * u)
        }
        // Boş teller bilerek işaretlenmez (LED'de de yanmaz)
    }
}

/** Tel numarası – renk açıklaması. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StringColorLegend(modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (s in 1..6) LegendDot(stringColor(s), "$s. tel")
        LegendDot(MutedColor, "çalma", cross = true)
    }
}

@Composable
private fun LegendDot(color: Color, label: String, cross: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (cross) {
            Text("✕", color = color, fontWeight = FontWeight.Black, fontSize = 13.sp)
        } else {
            Box(Modifier.size(12.dp).border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape).padding(1.dp).background(color, CircleShape))
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** LED kartının durumunu gösteren küçük düğme; dokununca LED ayarları açılır. */
@Composable
fun LedStatusChip(modifier: Modifier = Modifier, onDark: Boolean = false) {
    val state by LedController.state.collectAsState()
    var open by remember { mutableStateOf(false) }
    val connected = state.status == LedStatus.CONNECTED
    val dot = when (state.status) {
        LedStatus.CONNECTED -> Color(0xFF2E9E5B)
        LedStatus.NEEDS_PERMISSION, LedStatus.ERROR -> Color(0xFFF2A516)
        LedStatus.NO_DEVICE -> Color.Gray
    }
    Row(
        modifier
            .background(if (onDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .clickable { open = true }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Lightbulb, null, tint = dot, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            if (connected) "LED bağlı" else "LED",
            style = MaterialTheme.typography.labelMedium,
            color = if (onDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (open) LedSettingsDialog(onDismiss = { open = false })
}

@Composable
fun LedSettingsDialog(onDismiss: () -> Unit) {
    val state by LedController.state.collectAsState()
    val config by LedController.config.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tamam") } },
        title = { Text("Gitar LED'leri") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (state.status != LedStatus.CONNECTED) {
                    Text(
                        "ESP32-S2 Mini'yi OTG kablosuyla telefona tak. Android \"Gitar Öğren açılsın mı?\" diye sorarsa izin ver.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FilledTonalButton(onClick = { LedController.connect() }) { Text("Bağlan") }
                }

                Text("Takılı perde sayısı", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { LedController.updateConfig(config.copy(fretCount = config.fretCount - 1)) }) {
                        Icon(Icons.Rounded.Remove, "Azalt")
                    }
                    Text(
                        "${config.fretCount}",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    FilledTonalIconButton(onClick = { LedController.updateConfig(config.copy(fretCount = config.fretCount + 1)) }) {
                        Icon(Icons.Rounded.Add, "Artır")
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Toplam ${config.ledCount} LED", style = MaterialTheme.typography.bodySmall)
                }

                Text("Dizilim", style = MaterialTheme.typography.labelLarge)
                LedArrangement.entries.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().clickable { LedController.updateConfig(config.copy(arrangement = a)) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = config.arrangement == a, onClick = { LedController.updateConfig(config.copy(arrangement = a)) })
                        Text(a.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (config.arrangement == LedArrangement.PER_STRING) {
                    SwitchRow("Her perdede ilk LED 1. tel (ince)", config.firstStringFirst) {
                        LedController.updateConfig(config.copy(firstStringFirst = it))
                    }
                    SwitchRow("Şerit perdeden perdeye yön değiştiriyor", config.zigzag) {
                        LedController.updateConfig(config.copy(zigzag = it))
                    }
                }

                Text("Parlaklık", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = config.brightness.toFloat(),
                    onValueChange = { LedController.updateConfig(config.copy(brightness = it.toInt())) },
                    valueRange = 5f..255f,
                )
                if (state.status == LedStatus.CONNECTED) {
                    FilledTonalButton(onClick = { LedController.test() }) { Text("LED testi") }
                }
                Spacer(Modifier.height(2.dp))
                StringColorLegend()
            }
        },
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
