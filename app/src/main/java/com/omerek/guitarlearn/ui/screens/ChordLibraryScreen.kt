package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omerek.guitarlearn.data.ChordLevel
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.ui.components.ChordCard
import com.omerek.guitarlearn.ui.components.ChordDialog
import com.omerek.guitarlearn.ui.components.FingerLegend
import com.omerek.guitarlearn.ui.components.ScreenHeader
import com.omerek.guitarlearn.ui.components.SectionTitle

@Composable
fun ChordLibraryScreen(modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader(
                "Akor kütüphanesi",
                "${Chords.all.size} akor · dokun, büyüt, dinle",
                icon = Icons.Rounded.GridView,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            FingerLegend(Modifier.padding(horizontal = 16.dp))
        }
        ChordLevel.entries.forEach { level ->
            val chords = Chords.all.filter { it.level == level }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(level.title, Modifier.padding(horizontal = 16.dp), trailing = "${chords.size} akor")
            }
            itemsIndexed(chords, key = { _, c -> c.name }) { i, chord ->
                ChordCard(
                    chord,
                    modifier = Modifier.padding(start = if (i % 2 == 0) 16.dp else 6.dp, end = if (i % 2 == 0) 6.dp else 16.dp),
                    onClick = { selected = chord.name },
                )
            }
        }
    }

    selected?.let { ChordDialog(it, onDismiss = { selected = null }) }
}
