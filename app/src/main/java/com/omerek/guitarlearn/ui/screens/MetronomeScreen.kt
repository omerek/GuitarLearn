package com.omerek.guitarlearn.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.ui.components.BeatDots
import com.omerek.guitarlearn.ui.components.BigButton
import com.omerek.guitarlearn.ui.components.ScreenHeader
import com.omerek.guitarlearn.ui.components.TipBox
import com.omerek.guitarlearn.ui.theme.HeaderBrush
import kotlin.math.roundToInt

private const val OWNER = "metronome-screen"

@Composable
fun MetronomeScreen(modifier: Modifier = Modifier) {
    val state by Metronome.state.collectAsState()
    var lastTap by remember { mutableLongStateOf(0L) }
    val mine = state.running && state.owner == OWNER
    val pulse = remember { Animatable(1f) }

    // Her vuruşta daire hafifçe "atar"
    LaunchedEffect(state.beat, mine) {
        if (mine && state.beat >= 0) {
            pulse.snapTo(if (state.beat == 0) 1.08f else 1.04f)
            pulse.animateTo(1f, tween(180))
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Metronom", "Tempoyu tut, hızını adım adım artır", icon = Icons.Rounded.Timer)

        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalIconButton(onClick = { Metronome.setBpm(state.bpm - 1) }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Yavaşlat")
                }
                Box(
                    Modifier
                        .size(190.dp)
                        .scale(pulse.value)
                        .background(HeaderBrush, CircleShape)
                        .border(6.dp, if (mine && state.beat == 0) MaterialTheme.colorScheme.tertiary else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${state.bpm}", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 64.sp)
                        Text("BPM", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                        Text(tempoName(state.bpm), color = Color(0xFFFFD7B0), style = MaterialTheme.typography.labelMedium)
                    }
                }
                FilledTonalIconButton(onClick = { Metronome.setBpm(state.bpm + 1) }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = "Hızlandır")
                }
            }
            Spacer(Modifier.height(20.dp))
            BeatDots(state.beatsPerBar, if (mine) state.beat else -1, dotSize = 22)
            Spacer(Modifier.height(12.dp))
            Slider(
                value = state.bpm.toFloat(),
                onValueChange = { Metronome.setBpm(it.roundToInt()) },
                valueRange = Metronome.MIN_BPM.toFloat()..Metronome.MAX_BPM.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                listOf(2, 3, 4, 6).forEach { b ->
                    FilterChip(
                        selected = state.beatsPerBar == b,
                        onClick = { Metronome.setBeatsPerBar(b) },
                        label = { Text("$b/4", fontWeight = FontWeight.Bold) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            BigButton(if (mine) "Durdur" else "Başlat", icon = if (mine) Icons.Rounded.Stop else Icons.Rounded.PlayArrow) {
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Rounded.TouchApp, contentDescription = null)
                Text("  Tempoya dokun (en az iki kez)")
            }
            Spacer(Modifier.height(16.dp))
            TipBox("Yeni bir akor geçişini ya da şarkıyı önce yavaş (60 BPM civarı) çal. Hatasız çalabildiğinde tempoyu 5 BPM artır.")
        }
    }
}

private fun tempoName(bpm: Int) = when {
    bpm < 60 -> "Largo · çok ağır"
    bpm < 76 -> "Adagio · ağır"
    bpm < 108 -> "Moderato · orta"
    bpm < 140 -> "Allegro · hızlı"
    else -> "Presto · çok hızlı"
}
