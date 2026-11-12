package com.omerek.guitarlearn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.audio.SoundPlayer
import com.omerek.guitarlearn.data.Chord
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.data.TabParser

/** Akor adı + şeması + dinle düğmesi. */
@Composable
fun ChordCard(chord: Chord, modifier: Modifier = Modifier, diagramWidth: Int = 120, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(chord.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(chord.turkishName, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            ChordDiagram(chord, width = diagramWidth.dp)
            TextButton(onClick = { SoundPlayer.playChord(chord.midiNotes) }) { Text("▶ Dinle") }
        }
    }
}

/** Bir akorun ayrıntısını gösteren pencere. */
@Composable
fun ChordDialog(name: String, onDismiss: () -> Unit) {
    val chord = Chords.get(name)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
        title = { Text(if (chord != null) "${chord.name} · ${chord.turkishName}" else name) },
        text = {
            if (chord == null) {
                Text("Bu akor kütüphanede yok.")
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    ChordDiagram(chord, width = 180.dp)
                    Spacer(Modifier.height(8.dp))
                    if (chord.tip.isNotEmpty()) Text(chord.tip, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = { SoundPlayer.playChord(chord.midiNotes) }) { Text("▶ Dinle") }
                }
            }
        },
    )
}

/** Tab metnini eş aralıklı yazıyla gösterir, ▶ ile çalar. */
@Composable
fun TabView(title: String, tab: String, bpm: Int, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = {
                    SoundPlayer.playSequence(TabParser.parse(tab).map { it.midiNotes }, bpm)
                }) { Text("▶ Dinle") }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    tab,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * Vuruş kalıbı: 8 kutu ("1 ve 2 ve 3 ve 4 ve"), D = aşağı ↓, U = yukarı ↑.
 * Metronom bu kalıp için çalıyorsa çalan vuruş vurgulanır.
 */
@Composable
fun StrumPatternView(title: String, pattern: String, bpm: Int, modifier: Modifier = Modifier) {
    val owner = "strum:$title:$pattern"
    val state by Metronome.state.collectAsState()
    val active = state.running && state.owner == owner
    val beats = pattern.length / 2
    val counts = (1..beats).flatMap { listOf("$it", "ve") }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                pattern.forEachIndexed { i, c ->
                    val highlight = active && state.beat == i / 2
                    Column(
                        Modifier
                            .weight(1f)
                            .background(
                                if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            when (c) { 'D' -> "↓"; 'U' -> "↑"; else -> "·" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (c == '-') MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        )
                        Text(counts.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                if (active) Metronome.stop() else Metronome.start(bpm = bpm, beatsPerBar = beats, owner = owner)
            }) { Text(if (active) "■ Durdur" else "♩ Metronomla çal ($bpm BPM)") }
        }
    }
}

/** Metronomun çalan vuruşunu gösteren noktalar. */
@Composable
fun BeatDots(beatsPerBar: Int, current: Int, modifier: Modifier = Modifier, dotSize: Int = 18) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(beatsPerBar) { i ->
            val on = i == current
            Box(
                Modifier
                    .size(dotSize.dp)
                    .background(
                        when {
                            on && i == 0 -> MaterialTheme.colorScheme.tertiary
                            on -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape,
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}

/** İpucu kutusu. */
@Composable
fun TipBox(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text("💡", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Büyük, ortalanmış bir eylem düğmesi. */
@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(52.dp)) {
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
    }
}
