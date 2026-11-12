package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omerek.guitarlearn.data.Lessons
import com.omerek.guitarlearn.data.Progress

@Composable
fun LessonsScreen(progress: Progress, modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    val done = progress.completedLessons.value
    val total = Lessons.all.size
    val doneCount = Lessons.all.count { it.id in done }
    val next = Lessons.all.firstOrNull { it.id !in done }
    val grouped = Lessons.all.withIndex().groupBy { it.value.unit }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Gitar Öğren", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Sıfırdan şarkı çalmaya, klasik gitar için adım adım dersler.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("İlerlemen: $doneCount / $total ders", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { doneCount / total.toFloat() }, modifier = Modifier.fillMaxWidth())
                    if (next != null) {
                        Spacer(Modifier.height(12.dp))
                        Text("Sıradaki: ${next.title}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        androidx.compose.material3.Button(onClick = { onOpen(next.id) }) { Text("Devam et ▶") }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text("Tüm dersleri bitirdin! Şimdi şarkılar sekmesinde pratik yap. 🎉")
                    }
                }
            }
        }
        grouped.forEach { (unit, lessons) ->
            item(key = unit) {
                Text(
                    unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            items(lessons, key = { it.value.id }) { (index, lesson) ->
                val isDone = lesson.id in done
                Card(Modifier.fillMaxWidth().clickable { onOpen(lesson.id) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .background(
                                    if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (isDone) "✓" else "${index + 1}",
                                color = if (isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                            Text(lesson.summary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
