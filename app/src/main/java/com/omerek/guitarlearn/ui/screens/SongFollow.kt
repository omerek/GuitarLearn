package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.audio.ChordMatch
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.data.SongLine
import com.omerek.guitarlearn.ui.components.LedStatusChip
import com.omerek.guitarlearn.ui.components.StringFretboard
import com.omerek.guitarlearn.ui.theme.Amber
import com.omerek.guitarlearn.ui.theme.InTune
import com.omerek.guitarlearn.ui.theme.OffTune

/** Şarkıdaki akorların çalınma sırası (sözlerin üstündeki her akor bir adım). */
fun songSteps(lines: List<SongLine>): List<String> =
    lines.filterIsInstance<SongLine.Lyric>().flatMap { l -> l.segments.mapNotNull { it.chord } }

/** Takip modu durumu: hangi akordayız, mikrofon son ne duydu. */
@Stable
class FollowState(val steps: List<String>) {
    var index by mutableIntStateOf(0)
    var finished by mutableStateOf(false)
    var lastMatch by mutableStateOf<ChordMatch?>(null)
    var micOn by mutableStateOf(true)

    val current: String? get() = steps.getOrNull(index)
    val next: String? get() = steps.getOrNull(index + 1)

    fun advance() {
        if (index < steps.lastIndex) { index++; lastMatch = null } else finished = true
    }

    fun back() {
        if (finished) { finished = false; return }
        if (index > 0) { index--; lastMatch = null }
    }

    fun restart() { index = 0; finished = false; lastMatch = null }
}

/** Takip paneli: şu anki akor, sıradaki akor, renkli klavye ve mikrofonun ne duyduğu. */
@Composable
fun FollowPanel(state: FollowState, micAvailable: Boolean, onToggleMic: () -> Unit, modifier: Modifier = Modifier) {
    val chord = state.current?.let { Chords.get(it) }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = { state.back() }) { Icon(Icons.Rounded.SkipPrevious, "Önceki akor") }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                if (state.finished) {
                    Text("Şarkı bitti!", color = Amber, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    Text("Baştan başlamak için ileri tuşuna bas", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(state.current ?: "-", color = Amber, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "sıradaki: ${state.next ?: "son"}  ·  ${state.index + 1}/${state.steps.size}",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }
                    HeardLine(state, micAvailable)
                }
            }
            FilledTonalIconButton(onClick = { if (state.finished) state.restart() else state.advance() }) {
                Icon(Icons.Rounded.SkipNext, "Sonraki akor")
            }
        }
        if (chord != null && !state.finished) {
            Spacer(Modifier.height(6.dp))
            StringFretboard(
                chord,
                Modifier.background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp)).padding(4.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onToggleMic) {
                Icon(
                    if (state.micOn && micAvailable) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                    contentDescription = "Mikrofonla otomatik geçiş",
                    tint = if (state.micOn && micAvailable) InTune else Color.White.copy(alpha = 0.6f),
                )
            }
            Text(
                if (state.micOn && micAvailable) "Doğru akoru duyunca kendiliğinden geçer" else "Düğmelerle geç",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
            )
            LedStatusChip(onDark = true)
        }
    }
}

@Composable
private fun HeardLine(state: FollowState, micAvailable: Boolean) {
    val m = state.lastMatch
    val (text, color) = when {
        !micAvailable || !state.micOn -> "Mikrofon kapalı" to Color.White.copy(alpha = 0.6f)
        Chords.get(state.current ?: "") == null -> "Bu akor tanınamıyor, düğmeyle geç" to Color.White.copy(alpha = 0.6f)
        m == null -> "Dinliyorum… akoru bas ve tellere vur" to Color.White.copy(alpha = 0.8f)
        m.ok -> "Doğru! ✓ Sıradaki akora geçildi" to InTune
        else -> "Duyulan: ${m.best} (%${(m.expectedScore * 100).toInt()} benzer) — tekrar dene" to OffTune
    }
    Text(text, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
}
