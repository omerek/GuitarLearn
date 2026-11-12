package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.omerek.guitarlearn.data.UserSong
import com.omerek.guitarlearn.ui.components.SectionTitle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.data.Song
import com.omerek.guitarlearn.data.Songs
import com.omerek.guitarlearn.ui.components.ChordPill
import com.omerek.guitarlearn.ui.components.ScreenHeader
import com.omerek.guitarlearn.ui.theme.Amber

/** Şarkı kapakları için seviyeye göre renkler. */
private val CoverColors = listOf(
    listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)),
    listOf(Color(0xFF29B6F6), Color(0xFF0277BD)),
    listOf(Color(0xFFFFA726), Color(0xFFE65100)),
    listOf(Color(0xFFEC407A), Color(0xFFAD1457)),
)

private val LevelNames = listOf("Çok kolay", "Kolay", "Orta", "Zorlayıcı")

@Composable
fun SongsScreen(
    userSongs: List<UserSong>,
    modifier: Modifier = Modifier,
    onOpen: (String) -> Unit,
    onNew: () -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
) {
    var menu by remember { mutableStateOf(false) }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                "Şarkılar",
                "Kendi şarkıların ve hazır örnekler",
                icon = Icons.Rounded.LibraryMusic,
            )
            Spacer(Modifier.height(6.dp))
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Şarkılarım", Modifier.weight(1f), trailing = if (userSongs.isEmpty()) null else "${userSongs.size}")
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Yedekleme") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Yedekle (dosyaya kaydet)") },
                            leadingIcon = { Icon(Icons.Rounded.Upload, contentDescription = null) },
                            enabled = userSongs.isNotEmpty(),
                            onClick = { menu = false; onExport() },
                        )
                        DropdownMenuItem(
                            text = { Text("Yedekten yükle") },
                            leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                            onClick = { menu = false; onImport() },
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = onNew,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Yeni şarkı ekle", style = MaterialTheme.typography.titleMedium)
            }
        }
        if (userSongs.isEmpty()) {
            item {
                Text(
                    "Çalmak istediğin şarkıların sözlerini ve akorlarını kendin girebilirsin. Şarkılar yalnızca bu telefonda saklanır; " +
                        "sağ üstteki menüden yedek dosyası alıp uygulamayı yeniden kurduğunda geri yükleyebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        items(userSongs, key = { "u-" + it.id }) { us ->
            SongCard(us.toSong().copy(level = 0), Modifier.padding(horizontal = 16.dp)) { onOpen(UserSong.ID_PREFIX + us.id) }
        }
        item {
            SectionTitle("Hazır şarkılar ve etütler", Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), trailing = "${Songs.all.size}")
        }
        items(Songs.all.sortedBy { it.level }, key = { it.id }) { song ->
            SongCard(song, Modifier.padding(horizontal = 16.dp)) { onOpen(song.id) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SongCard(song: Song, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val colors = if (song.level == 0) listOf(Color(0xFFA1887F), Color(0xFF5D4037)) else CoverColors[(song.level - 1).coerceIn(0, CoverColors.lastIndex)]
            Box(
                Modifier
                    .size(64.dp)
                    .background(Brush.linearGradient(colors), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    song.chords.firstOrNull() ?: "♪",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                if (song.level == 0) {
                    Text(
                        "${song.source} · ${song.bpm} BPM",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(4) { i ->
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (i < song.level) Amber else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${LevelNames[(song.level - 1).coerceIn(0, 3)]} · ${song.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    song.chords.take(10).forEach { ChordPill(it) }
                }
            }
        }
    }
}
