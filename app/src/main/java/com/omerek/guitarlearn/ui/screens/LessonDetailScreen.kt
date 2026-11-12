package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import com.omerek.guitarlearn.ui.components.FingerLegend
import com.omerek.guitarlearn.ui.components.ScreenHeader
import com.omerek.guitarlearn.ui.theme.AccentBrush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.data.LessonBlock
import com.omerek.guitarlearn.data.Lessons
import com.omerek.guitarlearn.data.Progress
import com.omerek.guitarlearn.data.Songs
import com.omerek.guitarlearn.data.Tool
import com.omerek.guitarlearn.ui.components.BigButton
import com.omerek.guitarlearn.ui.components.ChordCard
import com.omerek.guitarlearn.ui.components.ChordChangeExercise
import com.omerek.guitarlearn.ui.components.StrumPatternView
import com.omerek.guitarlearn.ui.components.TabView
import com.omerek.guitarlearn.ui.components.TipBox

@Composable
fun LessonDetailScreen(
    lessonId: String,
    progress: Progress,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenTool: (Tool) -> Unit,
    onNextLesson: (String) -> Unit,
) {
    val lesson = Lessons.get(lessonId) ?: return
    val index = Lessons.all.indexOf(lesson)
    val next = Lessons.all.getOrNull(index + 1)
    val isDone = progress.isDone(lesson.id)

    // Dersten çıkınca dersin başlattığı metronomu durdur
    DisposableEffect(lessonId) {
        onDispose { if (Metronome.state.value.owner.startsWith("strum:")) Metronome.stop() }
    }

    key(lessonId) { LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = lesson.title,
                subtitle = lesson.summary,
                eyebrow = "${lesson.unit} · Ders ${index + 1}/${Lessons.all.size}",
                onBack = onBack,
            )
        }
        items(lesson.blocks) { block ->
            Box(Modifier.padding(horizontal = 16.dp)) { LessonBlockView(block, progress, onOpenSong, onOpenTool) }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                if (!isDone) {
                    BigButton("Dersi tamamladım", icon = Icons.Rounded.CheckCircle) {
                        progress.setDone(lesson.id, true)
                        next?.let { onNextLesson(it.id) } ?: onBack()
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Bu dersi tamamladın", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (next != null) Button(onClick = { onNextLesson(next.id) }) { Text("Sonraki") }
                    }
                    TextButton(onClick = { progress.setDone(lesson.id, false) }) { Text("Tamamlanmadı olarak işaretle") }
                }
            }
        }
    } }
}

@Composable
private fun LessonBlockView(
    block: LessonBlock,
    progress: Progress,
    onOpenSong: (String) -> Unit,
    onOpenTool: (Tool) -> Unit,
) {
    when (block) {
        is LessonBlock.Text -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
        is LessonBlock.Tip -> TipBox(block.text)
        is LessonBlock.Chords -> Column {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(block.names) { name -> Chords.get(name)?.let { ChordCard(it, Modifier.width(170.dp), diagramWidth = 140) } }
            }
            Spacer(Modifier.height(8.dp))
            FingerLegend()
        }
        is LessonBlock.Tab -> TabView(block.title, block.tab, block.bpm)
        is LessonBlock.Strum -> StrumPatternView(block.title, block.pattern, block.bpm)
        is LessonBlock.ChordChange -> ChordChangeExercise(block.from, block.to, progress)
        is LessonBlock.SongLink -> {
            val song = Songs.get(block.songId) ?: return
            Card(
                Modifier.fillMaxWidth().clickable { onOpenSong(song.id) },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Row(
                    Modifier.background(AccentBrush).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp).background(Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ŞARKIYLA PRATİK", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
                        Text(song.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Akorlar: ${song.chords.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                    }
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Aç", tint = Color.White)
                }
            }
        }
        is LessonBlock.OpenTool -> OutlinedButton(
            onClick = { onOpenTool(block.tool) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(if (block.tool == Tool.TUNER) Icons.Rounded.GraphicEq else Icons.Rounded.Timer, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (block.tool == Tool.TUNER) "Akort ediciyi aç" else "Metronomu aç")
        }
    }
}
