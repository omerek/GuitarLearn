package com.omerek.guitarlearn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.data.Progress
import kotlinx.coroutines.delay

/**
 * "Bir dakikalık akor geçişi" egzersizi: 60 saniyede iki akor arasında kaç kez temiz geçiş
 * yapabildiğini sayarsın. Her geçişte büyük düğmeye dokun. Rekorun kaydedilir.
 */
@Composable
fun ChordChangeExercise(from: String, to: String, progress: Progress, modifier: Modifier = Modifier) {
    val duration = 60
    var running by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(duration) }
    var count by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<String?>(null) }
    val best = progress.best(from, to)

    LaunchedEffect(running) {
        if (running) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
            running = false
            val record = progress.recordChanges(from, to, count)
            result = if (record) "Yeni rekor: $count geçiş! 🎉" else "$count geçiş. Rekorun: ${progress.best(from, to)}"
        }
    }

    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("1 DAKİKA AKOR GEÇİŞİ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    Text("$from ↔ $to", style = MaterialTheme.typography.titleLarge)
                }
                if (best > 0) {
                    Row(
                        Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = "Rekor", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Text(" $best", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                "Akorları sırayla bas, her temiz geçişte büyük düğmeye dokun. Hedef: dakikada 30+ geçiş.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Chords.get(from)?.let { ChordDiagram(it, width = 120.dp) }
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(28.dp))
                Chords.get(to)?.let { ChordDiagram(it, width = 120.dp) }
            }
            Spacer(Modifier.height(8.dp))
            if (running) {
                LinearProgressIndicator(
                    progress = { secondsLeft / duration.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { count++ },
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                ) {
                    Text("Geçiş: $count   (${secondsLeft} sn)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { running = false; secondsLeft = duration }, modifier = Modifier.align(Alignment.End)) {
                    Text("İptal")
                }
            } else {
                result?.let { Text(it, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { count = 0; secondsLeft = duration; result = null; running = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text(if (result == null) " Başla (60 sn)" else " Tekrar dene")
                }
            }
        }
    }
}
