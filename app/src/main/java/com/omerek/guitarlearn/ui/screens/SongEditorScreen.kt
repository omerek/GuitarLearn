package com.omerek.guitarlearn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.data.ChordName
import com.omerek.guitarlearn.data.ChordSheetImporter
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.data.LyricTokens
import com.omerek.guitarlearn.data.SongParser
import com.omerek.guitarlearn.data.UserSong
import com.omerek.guitarlearn.ui.components.BigButton
import com.omerek.guitarlearn.ui.components.ChordDialog
import com.omerek.guitarlearn.ui.components.ScreenHeader
import com.omerek.guitarlearn.ui.components.SectionTitle
import com.omerek.guitarlearn.ui.components.TipBox
import java.util.UUID

private const val ERASER = "\u0000silgi"

/**
 * Kullanıcının kendi şarkısını girdiği ekran:
 * 1) bilgiler, 2) akor listesi ("a, g, em"), 3) sözler, 4) akorları kelimelere dokunarak yerleştirme.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongEditorScreen(
    initial: UserSong?,
    onSave: (UserSong) -> Unit,
    onCancel: () -> Unit,
    onDelete: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    startInPlaceMode: Boolean = false,
) {
    val id = remember { initial?.id ?: UUID.randomUUID().toString().take(8) }
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var artist by rememberSaveable { mutableStateOf(initial?.artist ?: "") }
    var bpmText by rememberSaveable { mutableStateOf((initial?.bpm ?: 80).toString()) }
    var beats by rememberSaveable { mutableIntStateOf(initial?.beatsPerBar ?: 4) }
    var chordsText by rememberSaveable { mutableStateOf(initial?.chords?.joinToString(", ") ?: "") }
    var content by rememberSaveable { mutableStateOf(initial?.content ?: "") }
    var placeMode by rememberSaveable { mutableStateOf(startInPlaceMode) }
    var selectedChord by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var previewChord by remember { mutableStateOf<String?>(null) }

    val parsed = remember(chordsText) { ChordName.parseList(chordsText) }
    // Palet: kullanıcının yazdıkları + sözlerde geçenler
    val palette = remember(parsed, content) { (parsed.chords + SongParser.chordsIn(content)).distinct() }
    val activeChord = selectedChord?.takeIf { it == ERASER || it in palette } ?: palette.firstOrNull()

    fun build() = UserSong(
        id = id,
        title = title.trim(),
        artist = artist.trim(),
        bpm = bpmText.toIntOrNull()?.coerceIn(40, 220) ?: 80,
        beatsPerBar = beats,
        chords = parsed.chords,
        content = content.trimEnd(),
        updatedAt = initial?.updatedAt ?: 0L,
    )
    val dirty = initial?.let { build().copy(updatedAt = it.updatedAt) != it } ?: (title.isNotBlank() || content.isNotBlank() || chordsText.isNotBlank())
    val canSave = title.isNotBlank()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        ScreenHeader(
            title = if (initial == null) "Yeni şarkı" else "Şarkıyı düzenle",
            subtitle = "Yalnızca bu telefonda saklanır",
            eyebrow = "Şarkılarım",
            onBack = { if (dirty) confirmDiscard = true else onCancel() },
            actions = {
                TextButton(onClick = { onSave(build()) }, enabled = canSave) {
                    Icon(Icons.Rounded.Save, contentDescription = null, tint = if (canSave) Color.White else Color.White.copy(alpha = 0.4f))
                    Text(" Kaydet", color = if (canSave) Color.White else Color.White.copy(alpha = 0.4f))
                }
            },
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 1. Bilgiler
            SectionTitle("1 · Şarkı bilgileri")
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Şarkının adı *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = artist, onValueChange = { artist = it },
                label = { Text("Sanatçı (isteğe bağlı)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { bpmText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Tempo (BPM)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(130.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Ölçü", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(2, 3, 4, 6).forEach { b ->
                            FilterChip(selected = beats == b, onClick = { beats = b }, label = { Text("$b/4") })
                        }
                    }
                }
            }

            // 2. Akorlar
            SectionTitle("2 · Kullanılan akorlar")
            OutlinedTextField(
                value = chordsText,
                onValueChange = { chordsText = it },
                label = { Text("Akorlar") },
                placeholder = { Text("ör. a, g, em, d7") },
                supportingText = { Text("Virgülle ayır. Küçük harf de olur: am → Am, f#m → F#m") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (parsed.chords.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    parsed.chords.forEach { name ->
                        val known = Chords.get(name) != null
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (known) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = known) { previewChord = name }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(if (known) "şema var" else "şema yok", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (parsed.invalid.isNotEmpty()) {
                Text(
                    "Tanınmadı: ${parsed.invalid.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // 3. Sözler ve yerleştirme
            SectionTitle("3 · Sözler ve akorlar")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !placeMode, onClick = { placeMode = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Sözleri yaz") }
                SegmentedButton(
                    selected = placeMode, onClick = { placeMode = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Akorları yerleştir") }
            }

            if (!placeMode) {
                if (ChordSheetImporter.looksLikeChordSheet(content)) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Metinde akor satırları var. Akorları alttaki sözlerin üstüne otomatik yerleştireyim mi?",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                content = ChordSheetImporter.convert(content)
                                val found = SongParser.chordsIn(content)
                                chordsText = (parsed.chords + found).distinct().joinToString(", ")
                                placeMode = true
                            }) { Text("Yerleştir") }
                        }
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Sözler") },
                    placeholder = { Text("Sözleri buraya yaz ya da yapıştır.\nBölüm başlığı için: # Nakarat") },
                    minLines = 10,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
                TipBox(
                    "Sözleri yazdıktan sonra \"Akorları yerleştir\"e geç ve akorları kelimelere dokunarak koy. " +
                        "İstersen doğrudan da yazabilirsin: [Am]Üsküdar'a gider iken. " +
                        "İnternetten kopyaladığın \"akor üstte, söz altta\" metni yapıştırırsan otomatik dönüştürmeyi önerir.",
                )
            } else {
                PlacementEditor(
                    content = content,
                    palette = palette,
                    active = activeChord,
                    onSelect = { selectedChord = it },
                    onChange = { content = it },
                )
            }

            Spacer(Modifier.height(4.dp))
            BigButton("Kaydet", icon = Icons.Rounded.Save, modifier = Modifier, onClick = { if (canSave) onSave(build()) })
            if (!canSave) {
                Text("Kaydetmek için şarkıya bir ad ver.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (initial != null && onDelete != null) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(" Şarkıyı sil", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Değişiklikler kaydedilmedi") },
            text = { Text("Kaydetmeden çıkarsan yaptığın değişiklikler kaybolur.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; if (canSave) onSave(build()) else onCancel() }) { Text(if (canSave) "Kaydet" else "Tamam") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false; onCancel() }) { Text("Kaydetmeden çık") } },
        )
    }
    if (confirmDelete && initial != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Şarkı silinsin mi?") },
            text = { Text("\"${initial.title}\" bu telefondan silinecek. Yedek dosyan varsa oradan geri yükleyebilirsin.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(initial.id) }) { Text("Sil", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } },
        )
    }
    previewChord?.let { ChordDialog(it) { previewChord = null } }
}

/** Akor paleti + kelimelere dokunarak akor yerleştirme. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlacementEditor(
    content: String,
    palette: List<String>,
    active: String?,
    onSelect: (String) -> Unit,
    onChange: (String) -> Unit,
) {
    val lines = remember(content) { LyricTokens.parse(content) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Bir akor seç, sonra kelimeye dokun", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            if (palette.isEmpty()) {
                Text(
                    "Önce yukarıya kullanacağın akorları yaz (ör. a, g, em).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    palette.forEach { name ->
                        PaletteChip(name, selected = active == name) { onSelect(name) }
                    }
                    PaletteChip("Sil", selected = active == ERASER, eraser = true) { onSelect(ERASER) }
                }
            }
        }
    }

    if (content.isBlank()) {
        TipBox("Önce \"Sözleri yaz\" sekmesinde sözleri gir. Sadece akor yürüyüşü girmek istersen satır sonundaki + ile akor ekleyebilirsin.")
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        val placeChord = if (active == ERASER) null else active
        lines.forEachIndexed { li, line ->
            when (line) {
                is LyricTokens.Line.Raw -> if (line.text.isBlank()) {
                    Spacer(Modifier.height(12.dp))
                } else {
                    Text(
                        line.text.trim().removePrefix("#").trim().uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                is LyricTokens.Line.Words -> FlowRow(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    line.words.forEachIndexed { wi, w ->
                        Column(
                            Modifier
                                .padding(end = 3.dp, bottom = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (w.chord != null) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainer)
                                .clickable(enabled = active != null) {
                                    onChange(LyricTokens.setChord(content, li, wi, placeChord))
                                }
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        ) {
                            Text(
                                w.chord ?: " ",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                            )
                            Text(w.text.trimEnd().ifEmpty { "♪" }, fontSize = 17.sp)
                        }
                    }
                    if (placeChord != null) {
                        Box(
                            Modifier
                                .padding(bottom = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .clickable { onChange(LyricTokens.appendChord(content, li, placeChord)) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                        ) { Text("+ $placeChord", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                    }
                }
            }
        }
        if (placeChord != null) {
            TextButton(onClick = { onChange(if (content.isBlank()) "[$placeChord]" else "$content\n[$placeChord]") }) {
                Text("+ Yeni akor satırı ($placeChord)")
            }
        }
    }
}

@Composable
private fun PaletteChip(name: String, selected: Boolean, eraser: Boolean = false, onClick: () -> Unit) {
    val bg = when {
        selected && eraser -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (eraser) Icon(Icons.Rounded.Delete, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(name, color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
