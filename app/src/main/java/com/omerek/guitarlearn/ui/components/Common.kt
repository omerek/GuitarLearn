package com.omerek.guitarlearn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.omerek.guitarlearn.ui.theme.FingerColors
import com.omerek.guitarlearn.ui.theme.HeaderBrush
import com.omerek.guitarlearn.ui.theme.WoodDeep

/**
 * Ekranların üstündeki ahşap renkli başlık alanı.
 * [onBack] verilirse solda geri düğmesi çıkar; [content] başlığın altına ek içerik (ilerleme vb.) koyar.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onBack: (() -> Unit)? = null,
    eyebrow: String? = null,
    actions: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(HeaderBrush)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = if (onBack != null) 4.dp else 18.dp, bottom = 20.dp),
    ) {
        if (onBack != null || actions != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                }
                Spacer(Modifier.weight(1f))
                actions?.invoke()
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    Modifier.size(46.dp).background(Color.White.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                if (eyebrow != null) {
                    Text(eyebrow.uppercase(), color = Color(0xFFFFD7B0), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
                }
                Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                if (subtitle != null) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (content != null) {
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

/** Bölüm başlığı: solda renkli çubuk. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(20.dp).background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Akor adı + şeması + dinle düğmesi. */
@Composable
fun ChordCard(chord: Chord, modifier: Modifier = Modifier, diagramWidth: Int = 120, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(chord.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text(chord.turkishName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                PlayChordButton(chord)
            }
            Spacer(Modifier.height(6.dp))
            ChordDiagram(chord, width = diagramWidth.dp)
        }
    }
}

@Composable
fun PlayChordButton(chord: Chord, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick = { SoundPlayer.playChord(chord.midiNotes) }, modifier = modifier.size(40.dp)) {
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "${chord.name} akorunu dinle")
    }
}

/** Parmak renklerinin açıklaması. */
@Composable
fun FingerLegend(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("İşaret", "Orta", "Yüzük", "Serçe").forEachIndexed { i, name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(18.dp).background(FingerColors[i], CircleShape), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
                Text(name, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Bir akorun ayrıntısını gösteren pencere. */
@Composable
fun ChordDialog(name: String, onDismiss: () -> Unit) {
    val chord = Chords.get(name)
    // Pencere açıkken akor gitardaki LED'lerde de yansın
    androidx.compose.runtime.DisposableEffect(chord) {
        val previous = com.omerek.guitarlearn.led.LedController.currentChord
        com.omerek.guitarlearn.led.LedController.show(chord)
        onDispose { com.omerek.guitarlearn.led.LedController.show(previous) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
        title = {
            if (chord != null) {
                Column {
                    Text(chord.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(chord.turkishName, style = MaterialTheme.typography.titleSmall)
                }
            } else Text(name)
        },
        text = {
            if (chord == null) {
                Text("Bu akor kütüphanede yok.")
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                ) {
                    ChordDiagram(chord, width = 200.dp)
                    Spacer(Modifier.height(10.dp))
                    FingerLegend()
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Gitarı tutuşuna göre", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        LedStatusChip()
                    }
                    Spacer(Modifier.height(6.dp))
                    StringFretboard(chord)
                    Spacer(Modifier.height(6.dp))
                    StringColorLegend()
                    if (chord.tip.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        TipBox(chord.tip)
                    }
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = { SoundPlayer.playChord(chord.midiNotes) }) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Dinle")
                    }
                }
            }
        },
    )
}

/** Tab: koyu "klavye" zemin üzerinde eş aralıklı yazı, ▶ ile çalar. */
@Composable
fun TabView(title: String, tab: String, bpm: Int, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TAB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    Text(title, style = MaterialTheme.typography.titleSmall)
                }
                FilledTonalIconButton(onClick = {
                    SoundPlayer.playSequence(TabParser.parse(tab).map { it.midiNotes }, bpm)
                }) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Dinle") }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(WoodDeep, RoundedCornerShape(12.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    tab,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    softWrap = false,
                    color = Color(0xFFF3E9D2),
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
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("RİTİM · $bpm BPM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    Text(title, style = MaterialTheme.typography.titleSmall)
                }
                FilledTonalIconButton(
                    onClick = { if (active) Metronome.stop() else Metronome.start(bpm = bpm, beatsPerBar = beats, owner = owner) },
                    colors = if (active) IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) else IconButtonDefaults.filledTonalIconButtonColors(),
                ) {
                    Icon(if (active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = if (active) "Durdur" else "Metronomla çal")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                pattern.forEachIndexed { i, c ->
                    val highlight = active && state.beat == i / 2
                    val onBeat = i % 2 == 0
                    Column(
                        Modifier
                            .weight(1f)
                            .background(
                                when {
                                    highlight -> MaterialTheme.colorScheme.tertiary
                                    onBeat -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surfaceContainer
                                },
                                RoundedCornerShape(10.dp),
                            )
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            when (c) { 'D' -> "↓"; 'U' -> "↑"; else -> "·" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                highlight -> MaterialTheme.colorScheme.onTertiary
                                c == '-' -> MaterialTheme.colorScheme.outlineVariant
                                c == 'D' -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.tertiary
                            },
                        )
                        Text(
                            counts.getOrElse(i) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (highlight) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Metronomun çalan vuruşunu gösteren noktalar. */
@Composable
fun BeatDots(beatsPerBar: Int, current: Int, modifier: Modifier = Modifier, dotSize: Int = 18, onDark: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(beatsPerBar) { i ->
            val on = i == current
            val idle = if (onDark) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
            Box(
                Modifier
                    .size(dotSize.dp)
                    .background(
                        when {
                            on && i == 0 -> MaterialTheme.colorScheme.tertiary
                            on -> if (onDark) Color.White else MaterialTheme.colorScheme.primary
                            else -> idle
                        },
                        CircleShape,
                    )
                    .then(if (onDark) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)),
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
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Büyük, ortalanmış bir eylem düğmesi. */
@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
    }
}

/** Küçük akor etiketi (ör. şarkı listesinde). */
@Composable
fun ChordPill(name: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Text(
        name,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}
