package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.data.Segment
import com.omerek.guitarlearn.data.SongLine
import com.omerek.guitarlearn.data.SongParser
import com.omerek.guitarlearn.data.Songs
import com.omerek.guitarlearn.ui.components.BeatDots
import com.omerek.guitarlearn.ui.components.ChordDiagram
import com.omerek.guitarlearn.ui.components.ChordDialog
import com.omerek.guitarlearn.ui.components.StrumPatternView
import com.omerek.guitarlearn.ui.components.TipBox
import kotlinx.coroutines.delay

@Composable
fun SongDetailScreen(songId: String, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val song = Songs.get(songId) ?: return
    val lines = remember(songId) { SongParser.parse(song.content) }
    val scroll = rememberScrollState()
    var autoScroll by remember { mutableStateOf(false) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var dialogChord by remember { mutableStateOf<String?>(null) }
    val metronome by Metronome.state.collectAsState()
    val owner = "song:${song.id}"
    val metronomeOn = metronome.running && metronome.owner == owner

    // Şarkı açıkken ekran kapanmasın; çıkınca metronom dursun
    val view = LocalView.current
    DisposableEffect(songId) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            if (Metronome.state.value.owner == owner) Metronome.stop()
        }
    }

    LaunchedEffect(autoScroll, speed) {
        while (autoScroll) {
            scroll.scrollBy(speed)
            if (scroll.value >= scroll.maxValue) autoScroll = false
            delay(16)
        }
    }

    Column(modifier.fillMaxSize()) {
        // Sabit üst çubuk: geri, kaydırma ve metronom
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("‹ Geri") }
                    Spacer(Modifier.weight(1f))
                    FilterChip(
                        selected = autoScroll,
                        onClick = { autoScroll = !autoScroll },
                        label = { Text(if (autoScroll) "⏸ Kaydır" else "▶ Kaydır") },
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = metronomeOn,
                        onClick = { if (metronomeOn) Metronome.stop() else Metronome.start(song.bpm, song.beatsPerBar, owner) },
                        label = { Text("♩ ${song.bpm}") },
                    )
                }
                if (autoScroll) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("Hız", style = MaterialTheme.typography.labelMedium)
                        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.3f..4f, modifier = Modifier.weight(1f))
                    }
                }
                if (metronomeOn) {
                    BeatDots(song.beatsPerBar, metronome.beat, Modifier.padding(start = 16.dp, bottom = 4.dp), dotSize = 12)
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
        ) {
            Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "${song.source} · ${song.bpm} BPM · ${song.beatsPerBar}/4",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))

            // Şarkıdaki akorlar
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                song.chords.forEach { name ->
                    Chords.get(name)?.let { chord ->
                        Card(Modifier.clickable { dialogChord = name }) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(chord.name, fontWeight = FontWeight.Bold)
                                ChordDiagram(chord, width = 72.dp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            StrumPatternView("Önerilen vuruş", song.strumPattern, song.bpm)
            if (song.note.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                TipBox(song.note)
            }
            Spacer(Modifier.height(16.dp))

            lines.forEach { line ->
                when (line) {
                    is SongLine.Blank -> Spacer(Modifier.height(14.dp))
                    is SongLine.Header -> Text(
                        line.text,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                    is SongLine.Lyric -> LyricLine(line.segments, onChord = { dialogChord = it })
                }
            }
            Spacer(Modifier.height(200.dp))
        }
    }

    dialogChord?.let { ChordDialog(it, onDismiss = { dialogChord = null }) }
}

/** Bir söz satırı: her kelimenin üstünde (varsa) akoru durur; satır ekrana sığmazsa alta kayar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricLine(segments: List<Segment>, onChord: (String) -> Unit) {
    val pieces = remember(segments) {
        segments.flatMap { seg ->
            val words = Regex("""\S+\s*|\s+""").findAll(seg.text).map { it.value }.toList().ifEmpty { listOf("") }
            words.mapIndexed { i, w -> (if (i == 0) seg.chord else null) to w }
        }
    }
    FlowRow(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        pieces.forEach { (chord, word) ->
            val gap = if (word.isNotEmpty() && word.last().isWhitespace()) 6.dp else 0.dp
            Column(Modifier.padding(end = gap)) {
                Text(
                    text = chord ?: " ",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = (if (chord != null) Modifier.clickable { onChord(chord) } else Modifier).padding(end = 4.dp),
                )
                Text(text = word.trimEnd(), fontSize = 18.sp)
            }
        }
    }
}
