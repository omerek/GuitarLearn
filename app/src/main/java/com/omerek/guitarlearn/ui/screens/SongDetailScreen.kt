package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.omerek.guitarlearn.ui.components.ScreenHeader
import com.omerek.guitarlearn.ui.components.SectionTitle
import com.omerek.guitarlearn.ui.platform.KeepScreenOn
import com.omerek.guitarlearn.ui.platform.rememberMicPermission
import com.omerek.guitarlearn.ui.theme.Amber
import com.omerek.guitarlearn.ui.theme.WoodDark
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.data.Segment
import com.omerek.guitarlearn.data.SongLine
import com.omerek.guitarlearn.data.SongParser
import com.omerek.guitarlearn.data.Song
import com.omerek.guitarlearn.ui.components.BeatDots
import com.omerek.guitarlearn.ui.components.ChordDiagram
import com.omerek.guitarlearn.ui.components.ChordDialog
import com.omerek.guitarlearn.ui.components.StrumPatternView
import com.omerek.guitarlearn.ui.components.TipBox
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import com.omerek.guitarlearn.audio.ChordListener
import com.omerek.guitarlearn.audio.ChordMatcher
import com.omerek.guitarlearn.led.LedButton
import com.omerek.guitarlearn.led.LedController
import kotlinx.coroutines.flow.MutableSharedFlow

@Composable
fun SongDetailScreen(song: Song, modifier: Modifier = Modifier, onBack: () -> Unit, onEdit: (() -> Unit)? = null) {
    val songId = song.id
    val lines = remember(song.content) { SongParser.parse(song.content) }
    val scroll = rememberScrollState()
    var autoScroll by remember { mutableStateOf(false) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var dialogChord by remember { mutableStateOf<String?>(null) }
    val metronome by Metronome.state.collectAsState()
    val owner = "song:${song.id}"
    val metronomeOn = metronome.running && metronome.owner == owner

    // Takip modu: sıradaki akoru ekranda ve gitardaki LED'lerde gösterir, mikrofonla doğru akoru duyunca ilerler
    val follow = remember(songId) { FollowState(songSteps(lines)) }
    var following by rememberSaveable { mutableStateOf(false) }
    val mic = rememberMicPermission()
    val micGranted = mic.granted
    fun startFollowing() {
        following = true
        if (!mic.granted) mic.request()
    }

    LaunchedEffect(following, follow.index, follow.finished) {
        if (following && !follow.finished) LedController.show(follow.current?.let { Chords.get(it) }) else LedController.clear()
    }
    DisposableEffect(songId) { onDispose { LedController.clear() } }

    // Gitardaki önceki/sonraki düğmeleri
    LaunchedEffect(songId) {
        LedController.buttons.collect { b ->
            if (!following) { startFollowing(); return@collect }
            when (b) {
                LedButton.PREVIOUS -> follow.back()
                LedButton.NEXT -> if (follow.finished) follow.restart() else follow.advance()
            }
        }
    }

    // Mikrofon: her vuruşta beklenen akorla karşılaştır
    val listenNow = following && micGranted && follow.micOn && !follow.finished
    LaunchedEffect(listenNow) {
        if (!listenNow) return@LaunchedEffect
        val matcher = ChordMatcher(Chords.all)
        val strums = MutableSharedFlow<FloatArray>(extraBufferCapacity = 4)
        val listener = ChordListener { strums.tryEmit(it) }
        listener.start()
        try {
            strums.collect { chroma ->
                val expected = follow.current?.let { Chords.get(it) } ?: return@collect
                val m = matcher.match(chroma, expected)
                if (m.ok) follow.advance()
                follow.lastMatch = m
            }
        } finally {
            listener.stop()
        }
    }

    // Çalınan satırı ekranda görünür tut
    val lineCoords = remember(songId) { mutableStateMapOf<Int, LayoutCoordinates>() }
    var viewport by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val topGap = with(LocalDensity.current) { 140.dp.toPx() }
    LaunchedEffect(following, follow.index) {
        if (!following) return@LaunchedEffect
        val start = lineCoords.keys.filter { it <= follow.index }.maxOrNull() ?: return@LaunchedEffect
        val line = lineCoords[start]?.takeIf { it.isAttached } ?: return@LaunchedEffect
        val box = viewport?.takeIf { it.isAttached } ?: return@LaunchedEffect
        scroll.animateScrollBy(line.positionInRoot().y - box.positionInRoot().y - topGap)
    }

    // Şarkı açıkken ekran kapanmasın; çıkınca metronom dursun
    KeepScreenOn()
    DisposableEffect(songId) {
        onDispose { if (Metronome.state.value.owner == owner) Metronome.stop() }
    }

    LaunchedEffect(autoScroll, speed) {
        while (autoScroll) {
            scroll.scrollBy(speed)
            if (scroll.value >= scroll.maxValue) autoScroll = false
            delay(16)
        }
    }

    Box(modifier.fillMaxSize().onGloballyPositioned { viewport = it }) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            ScreenHeader(
                title = song.title,
                subtitle = "${song.source} · ${song.bpm} BPM · ${song.beatsPerBar}/4",
                eyebrow = if (onEdit != null) "Şarkılarım" else "Şarkı",
                onBack = onBack,
                actions = onEdit?.let { edit ->
                    {
                        TextButton(onClick = edit) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(" Düzenle", color = Color.White)
                        }
                    }
                },
            )
            Column(Modifier.padding(16.dp)) {
            // Şarkıdaki akorlar
            SectionTitle("Bu şarkıdaki akorlar", trailing = "dokun: büyüt")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                song.chords.forEach { name ->
                    val chord = Chords.get(name)
                    if (chord == null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        ) {
                            Column(Modifier.padding(10.dp).width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(16.dp))
                                Text("Şema yok", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    chord?.let { chord ->
                        Card(
                            Modifier.clickable { dialogChord = name },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                ChordDiagram(chord, width = 84.dp, showStringNames = false)
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

            var stepCounter = 0
            lines.forEach { line ->
                when (line) {
                    is SongLine.Blank -> Spacer(Modifier.height(14.dp))
                    is SongLine.Header -> Text(
                        line.text.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 4.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    is SongLine.Lyric -> {
                        val start = stepCounter
                        stepCounter += line.segments.count { it.chord != null }
                        LyricLine(
                            line.segments,
                            onChord = { dialogChord = it },
                            firstStep = start,
                            activeStep = if (following) follow.index else -1,
                            modifier = Modifier.onGloballyPositioned { lineCoords[start] = it },
                        )
                    }
                }
            }
            Spacer(Modifier.height(if (following) 420.dp else 160.dp))
            }
        }

        // Alttaki kontrol çubuğu: otomatik kaydırma ve metronom
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = WoodDark,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (following) {
                    FollowPanel(follow, micAvailable = micGranted, onToggleMic = {
                        if (!mic.granted) mic.request() else follow.micOn = !follow.micOn
                    })
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ControlButton(
                        icon = if (autoScroll) Icons.Rounded.Pause else Icons.Rounded.ArrowDownward,
                        label = if (autoScroll) "Durdur" else "Kaydır",
                        active = autoScroll,
                        onClick = { autoScroll = !autoScroll },
                    )
                    Spacer(Modifier.width(8.dp))
                    ControlButton(
                        icon = if (metronomeOn) Icons.Rounded.Stop else Icons.Rounded.Timer,
                        label = "${song.bpm} BPM",
                        active = metronomeOn,
                        onClick = { if (metronomeOn) Metronome.stop() else Metronome.start(song.bpm, song.beatsPerBar, owner) },
                    )
                    Spacer(Modifier.width(8.dp))
                    ControlButton(
                        icon = Icons.Rounded.Hearing,
                        label = "Takip",
                        active = following,
                        onClick = { if (following) following = false else startFollowing() },
                    )
                    Spacer(Modifier.weight(1f))
                    if (metronomeOn) BeatDots(song.beatsPerBar, metronome.beat, dotSize = 12, onDark = true)
                }
                if (autoScroll) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hız", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        Slider(
                            value = speed,
                            onValueChange = { speed = it },
                            valueRange = 0.3f..4f,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber),
                        )
                    }
                }
            }
        }
    }

    dialogChord?.let { ChordDialog(it, onDismiss = { dialogChord = null }) }
}

/** Bir söz satırı: her kelimenin üstünde (varsa) akoru durur; satır ekrana sığmazsa alta kayar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricLine(
    segments: List<Segment>,
    onChord: (String) -> Unit,
    firstStep: Int = 0,
    activeStep: Int = -1,
    modifier: Modifier = Modifier,
) {
    // Her parça: (akor, akorun şarkıdaki adım numarası, kelime)
    val pieces = remember(segments, firstStep) {
        var step = firstStep
        segments.flatMap { seg ->
            val words = Regex("""\S+\s*|\s+""").findAll(seg.text).map { it.value }.toList().ifEmpty { listOf("") }
            val segStep = if (seg.chord != null) step++ else -1
            words.mapIndexed { i, w -> Triple(if (i == 0) seg.chord else null, if (i == 0) segStep else -1, w) }
        }
    }
    FlowRow(modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        pieces.forEach { (chord, step, word) ->
            val active = chord != null && step == activeStep
            val gap = if (word.isNotEmpty() && word.last().isWhitespace()) 6.dp else 0.dp
            Column(Modifier.padding(end = gap)) {
                Text(
                    text = chord ?: " ",
                    color = if (active) WoodDark else MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    modifier = (if (chord != null) Modifier.clickable { onChord(chord) } else Modifier)
                        .then(if (active) Modifier.background(Amber, RoundedCornerShape(6.dp)).padding(horizontal = 4.dp) else Modifier)
                        .padding(end = 4.dp),
                )
                Text(text = word.trimEnd(), fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Amber else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (active) WoodDark else Color.White
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
