package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omerek.guitarlearn.data.ChordLevel
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.ui.components.ChordCard
import com.omerek.guitarlearn.ui.components.ChordDialog

@Composable
fun ChordLibraryScreen(modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Akor kütüphanesi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        ChordLevel.entries.forEach { level ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    level.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(Chords.all.filter { it.level == level }, key = { it.name }) { chord ->
                ChordCard(chord, onClick = { selected = chord.name })
            }
        }
    }

    selected?.let { ChordDialog(it, onDismiss = { selected = null }) }
}
