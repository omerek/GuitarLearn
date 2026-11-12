package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omerek.guitarlearn.data.Songs

@Composable
fun SongsScreen(modifier: Modifier = Modifier, onOpen: (String) -> Unit) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Şarkılar ve etütler", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Kolaydan zora sıralı. Anonim türküler, kamu malı şarkılar ve özgün alıştırmalar.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(Songs.all.sortedBy { it.level }, key = { it.id }) { song ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(song.id) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium)
                        Text(song.source, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Akorlar: ${song.chords.joinToString(" ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text("★".repeat(song.level), color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}
