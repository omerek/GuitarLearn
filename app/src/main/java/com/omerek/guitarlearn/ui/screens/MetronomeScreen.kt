package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.ui.components.BeatDots
import com.omerek.guitarlearn.ui.components.BigButton
import com.omerek.guitarlearn.ui.components.TipBox
import kotlin.math.roundToInt

private const val OWNER = "metronome-screen"

@Composable
fun MetronomeScreen(modifier: Modifier = Modifier) {
    val state by Metronome.state.collectAsState()
    var lastTap by remember { mutableLongStateOf(0L) }
    val mine = state.running && state.owner == OWNER

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Metronom", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Text("${state.bpm}", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("BPM · ${tempoName(state.bpm)}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        BeatDots(state.beatsPerBar, if (mine) state.beat else -1, dotSize = 26)
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { Metronome.setBpm(state.bpm - 5) }) { Text("−5") }
            OutlinedButton(onClick = { Metronome.setBpm(state.bpm - 1) }) { Text("−1") }
            OutlinedButton(onClick = { Metronome.setBpm(state.bpm + 1) }) { Text("+1") }
            OutlinedButton(onClick = { Metronome.setBpm(state.bpm + 5) }) { Text("+5") }
        }
        Slider(
            value = state.bpm.toFloat(),
            onValueChange = { Metronome.setBpm(it.roundToInt()) },
            valueRange = Metronome.MIN_BPM.toFloat()..Metronome.MAX_BPM.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Ölçü", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 3, 4, 6).forEach { b ->
                FilterChip(
                    selected = state.beatsPerBar == b,
                    onClick = { Metronome.setBeatsPerBar(b) },
                    label = { Text("$b/4") },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        BigButton(if (mine) "■ Durdur" else "▶ Başlat") {
            if (mine) Metronome.stop() else Metronome.start(owner = OWNER)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                // Vuruşa dokunarak tempo bulma
                val now = System.currentTimeMillis()
                val diff = now - lastTap
                if (diff in 250..2000) Metronome.setBpm((60000.0 / diff).roundToInt())
                lastTap = now
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("👆 Tempoya dokun (en az iki kez)") }
        Spacer(Modifier.height(16.dp))
        TipBox("Yeni bir akor geçişini ya da şarkıyı önce yavaş (60 BPM civarı) çal. Hatasız çalabildiğinde tempoyu 5 BPM artır.")
    }
}

private fun tempoName(bpm: Int) = when {
    bpm < 60 -> "Largo (çok ağır)"
    bpm < 76 -> "Adagio (ağır)"
    bpm < 108 -> "Andante / Moderato (orta)"
    bpm < 140 -> "Allegro (hızlı)"
    else -> "Presto (çok hızlı)"
}
