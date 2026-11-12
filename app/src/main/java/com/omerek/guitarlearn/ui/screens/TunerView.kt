package com.omerek.guitarlearn.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.audio.SoundPlayer
import com.omerek.guitarlearn.audio.Tuning
import com.omerek.guitarlearn.ui.components.ScreenHeader
import com.omerek.guitarlearn.ui.components.TipBox
import com.omerek.guitarlearn.ui.theme.InTune
import com.omerek.guitarlearn.ui.theme.NearTune
import com.omerek.guitarlearn.ui.theme.OffTune
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val IN_TUNE_CENTS = 5.0

/**
 * Akort ekranının görünümü (Android'e bağımlı değil).
 * @param frequency mikrofondan ölçülen frekans, ses yoksa <= 0
 */
@Composable
fun TunerView(
    frequency: Float,
    micGranted: Boolean,
    onRequestMic: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelected: Int? = null,
) {
    var selected by rememberSaveable { mutableStateOf(initialSelected) } // tel numarası; null = otomatik
    var tuned by remember { mutableStateOf(setOf<Int>()) }

    val freq = frequency.toDouble()
    val hasPitch = freq > 0
    val target = when {
        selected != null -> Tuning.standard.first { it.number == selected }
        hasPitch -> Tuning.closestString(freq)
        else -> null
    }
    val cents = if (hasPitch && target != null) Tuning.cents(freq, target.frequency) else 0.0
    val inTune = hasPitch && abs(cents) < IN_TUNE_CENTS
    val stateColor by animateColorAsState(
        when {
            !hasPitch -> MaterialTheme.colorScheme.outlineVariant
            inTune -> InTune
            abs(cents) < 20 -> NearTune
            else -> OffTune
        },
        label = "tuneColor",
    )
    val needle by animateFloatAsState(if (hasPitch) cents.coerceIn(-50.0, 50.0).toFloat() else 0f, tween(150), label = "needle")

    // Bir tel ~0,6 sn boyunca akortlu kalırsa işaretle
    LaunchedEffect(inTune, target?.number) {
        if (inTune && target != null) {
            kotlinx.coroutines.delay(600)
            tuned = tuned + target.number
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Akort", "Standart akort · Mi La Re Sol Si Mi", icon = Icons.Rounded.GraphicEq)

        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Gauge(needle, stateColor, hasPitch)
                        NoteBubble(
                            note = target?.note ?: "–",
                            solfege = target?.solfege ?: "",
                            color = stateColor,
                            inTune = inTune,
                            modifier = Modifier.offset(y = 30.dp),
                        )
                    }
                    Spacer(Modifier.height(40.dp))
                    Text(
                        text = when {
                            !micGranted -> "Mikrofon izni gerekli"
                            !hasPitch -> "Bir teli çal…"
                            inTune -> "Akortlu"
                            abs(cents) >= 50 -> if (cents < 0) "Çok pes · teli epey sık" else "Çok tiz · teli epey gevşet"
                            cents < 0 -> "Pes · teli biraz sık"
                            else -> "Tiz · teli biraz gevşet"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = if (hasPitch) stateColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (hasPitch) {
                            val heard = Tuning.nearestNote(freq)
                            "%.1f Hz · %+.0f sent · duyulan ${heard.solfege}${heard.octave}".format(freq, cents)
                        } else {
                            target?.let { "Hedef: ${it.number}. tel ${it.solfege} · %.2f Hz".format(it.frequency) } ?: "Uygulama hangi teli çaldığını kendisi bulur"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            StringSelector(
                selected = selected,
                active = target?.number.takeIf { hasPitch || selected != null },
                tuned = tuned,
                onSelect = { selected = it },
            )

            Spacer(Modifier.height(12.dp))
            if (!micGranted) {
                Button(onClick = onRequestMic, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Mikrofon iznini ver")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ses kaydedilmez ve hiçbir yere gönderilmez. İzin vermeden de referans seslerle kulaktan akort edebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }
            FilledTonalButton(
                onClick = { SoundPlayer.playNote((target ?: Tuning.standard.first()).frequency, 3.0) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("${(target ?: Tuning.standard.first()).number}. telin referans sesini çal")
            }
            Spacer(Modifier.height(16.dp))
            TipBox("Teli tek başına, parmak ucuyla orta kuvvette çal ve ses sönene kadar bekle. Çok bozuk bir teli akort ederken yukarıdan o teli seçmek daha güvenilirdir.")
            Spacer(Modifier.height(8.dp))
            TipBox("Naylon teller yeniyken çok esner; ilk haftalarda akortun sık kaçması normaldir.")
        }
    }
}

@Composable
private fun NoteBubble(note: String, solfege: String, color: Color, inTune: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(104.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
            .border(6.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(note, fontSize = 40.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, lineHeight = 40.sp)
            if (inTune) {
                Icon(Icons.Rounded.Check, contentDescription = "Akortlu", tint = InTune, modifier = Modifier.size(20.dp))
            } else {
                Text(solfege, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Gitar kafası gibi: 6 tel düğmesi, akortlanan tellerde yeşil tik. */
@Composable
private fun StringSelector(selected: Int?, active: Int?, tuned: Set<Int>, onSelect: (Int?) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Teller", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Otomatik") })
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tuning.standard.forEach { s ->
                val isActive = active == s.number
                val isTuned = s.number in tuned
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceContainerLowest
                            },
                        )
                        .border(
                            2.dp,
                            if (selected == s.number) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { onSelect(if (selected == s.number) null else s.number) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val fg = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Text(s.note, fontSize = 22.sp, fontWeight = FontWeight.Black, color = fg)
                    Text(s.solfege, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .size(20.dp)
                            .background(if (isTuned) InTune else fg.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isTuned) {
                            Icon(Icons.Rounded.Check, contentDescription = "Akortlu", tint = Color.White, modifier = Modifier.size(14.dp))
                        } else {
                            Text("${s.number}", fontSize = 10.sp, lineHeight = 10.sp, color = fg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** -50..+50 sent aralığında renkli yarım daire gösterge. */
@Composable
private fun Gauge(cents: Float, color: Color, active: Boolean) {
    val tick = MaterialTheme.colorScheme.outline
    val needleColor = if (active) color else MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.9f)) {
        val stroke = size.width * 0.06f
        val radius = size.width / 2 - stroke
        val center = Offset(size.width / 2, size.height - stroke * 0.2f)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)
        // Kırmızı → sarı → yeşil → sarı → kırmızı bölgeler
        val zones = listOf(
            -50f to -20f to OffTune, -20f to -5f to NearTune, -5f to 5f to InTune, 5f to 20f to NearTune, 20f to 50f to OffTune,
        )
        zones.forEach { (range, c) ->
            val start = 270f + range.first / 50f * 90f
            val sweep = (range.second - range.first) / 50f * 90f
            drawArc(c.copy(alpha = 0.85f), start, sweep, false, arcTopLeft, arcSize, style = Stroke(stroke))
        }
        // Çentikler
        for (c in -50..50 step 10) {
            val a = Math.toRadians(270.0 + c / 50.0 * 90.0)
            val outer = radius - stroke * 0.8f
            val inner = outer - if (c == 0) stroke * 1.1f else stroke * 0.55f
            drawLine(
                tick,
                Offset(center.x + (cos(a) * inner).toFloat(), center.y + (sin(a) * inner).toFloat()),
                Offset(center.x + (cos(a) * outer).toFloat(), center.y + (sin(a) * outer).toFloat()),
                strokeWidth = if (c == 0) 5f else 3f,
                cap = StrokeCap.Round,
            )
        }
        // İbre
        val angle = Math.toRadians(270.0 + cents / 50.0 * 90.0)
        val len = radius * 0.95f
        val tip = Offset(center.x + (cos(angle) * len).toFloat(), center.y + (sin(angle) * len).toFloat())
        drawLine(Color.Black.copy(alpha = 0.18f), center + Offset(3f, 3f), tip + Offset(3f, 3f), strokeWidth = stroke * 0.32f, cap = StrokeCap.Round)
        drawLine(
            Brush.linearGradient(listOf(needleColor, needleColor.copy(alpha = 0.7f)), center, tip),
            center,
            tip,
            strokeWidth = stroke * 0.32f,
            cap = StrokeCap.Round,
        )
    }
}
