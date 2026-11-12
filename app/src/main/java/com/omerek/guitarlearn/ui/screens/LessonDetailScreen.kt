package com.omerek.guitarlearn.ui.screens

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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("‹ Dersler") }
            Text(lesson.unit, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("${index + 1}. ${lesson.title}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(lesson.summary, style = MaterialTheme.typography.bodyMedium)
        }
        items(lesson.blocks) { block ->
            when (block) {
                is LessonBlock.Text -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
                is LessonBlock.Tip -> TipBox(block.text)
                is LessonBlock.Chords -> LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(block.names) { name -> Chords.get(name)?.let { ChordCard(it) } }
                }
                is LessonBlock.Tab -> TabView(block.title, block.tab, block.bpm)
                is LessonBlock.Strum -> StrumPatternView(block.title, block.pattern, block.bpm)
                is LessonBlock.ChordChange -> ChordChangeExercise(block.from, block.to, progress)
                is LessonBlock.SongLink -> {
                    val song = Songs.get(block.songId)
                    if (song != null) {
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("🎵 Şarkı ile pratik", style = MaterialTheme.typography.labelLarge)
                                    Text(song.title, style = MaterialTheme.typography.titleMedium)
                                    Text("Akorlar: ${song.chords.joinToString(" ")}", style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(onClick = { onOpenSong(song.id) }) { Text("Aç") }
                            }
                        }
                    }
                }
                is LessonBlock.OpenTool -> OutlinedButton(onClick = { onOpenTool(block.tool) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (block.tool == Tool.TUNER) "🎯 Akort ediciyi aç" else "⏱ Metronomu aç")
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            if (!isDone) {
                BigButton("✓ Dersi tamamladım") {
                    progress.setDone(lesson.id, true)
                    next?.let { onNextLesson(it.id) } ?: onBack()
                }
            } else {
                Column {
                    Text("Bu dersi tamamladın ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row {
                        if (next != null) TextButton(onClick = { onNextLesson(next.id) }) { Text("Sonraki ders ›") }
                        TextButton(onClick = { progress.setDone(lesson.id, false) }) { Text("Tamamlanmadı olarak işaretle") }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    } }
}
