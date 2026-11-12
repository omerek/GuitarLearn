package preview

import androidx.compose.runtime.Composable
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.data.Progress
import com.omerek.guitarlearn.data.UserSong
import com.omerek.guitarlearn.data.UserSongStore
import com.omerek.guitarlearn.ui.GuitarApp
import com.omerek.guitarlearn.ui.components.ChordDialog
import com.omerek.guitarlearn.ui.components.LedSettingsDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omerek.guitarlearn.audio.ChordMatch
import com.omerek.guitarlearn.data.SongParser
import com.omerek.guitarlearn.data.Songs
import com.omerek.guitarlearn.ui.theme.WoodDark
import com.omerek.guitarlearn.ui.screens.*

object Screens {
    val all: List<Pair<String, @Composable () -> Unit>> = listOf(
        "app" to { GuitarApp(Progress(setOf("l01", "l02", "l03")), UserSongStore()) },
        "lessons" to { LessonsScreen(Progress(setOf("l01", "l02", "l03")), onOpen = {}) },
        "lesson04" to { LessonDetailScreen("l04", Progress(), onBack = {}, onOpenSong = {}, onOpenTool = {}, onNextLesson = {}) },
        "lesson11" to { LessonDetailScreen("l11", Progress(), onBack = {}, onOpenSong = {}, onOpenTool = {}, onNextLesson = {}) },
        "chords" to { ChordLibraryScreen() },
        "chordDialog" to { ChordDialog("F") {} },
        "songFollow" to { FollowPreview() },
        "chordDialogA" to { ChordDialog("A") {} },
        "ledSettings" to { LedSettingsDialog {} },
        "songs" to { SongsScreen(emptyList(), onOpen = {}) },
        "songsMine" to { SongsScreen(listOf(Demo.song), onOpen = {}) },
        "song" to { SongDetailScreen(Songs.get("uskudar")!!, onBack = {}) },
        "songMine" to { SongDetailScreen(Demo.song.toSong(), onBack = {}, onEdit = {}) },
        "editorNew" to { SongEditorScreen(null, onSave = {}, onCancel = {}) },
        "editorWrite" to { SongEditorScreen(Demo.sheet, onSave = {}, onCancel = {}) },
        "editorPlace" to { SongEditorScreen(Demo.song, onSave = {}, onCancel = {}, onDelete = {}, startInPlaceMode = true) },
        "tunerFlat" to { TunerView(107.2f, true, {}) },
        "tunerOk" to { TunerView(146.9f, true, {}) },
        "tunerNoMic" to { TunerView(-1f, false, {}) },
        "metronome" to { MetronomeScreen() },
    )
}

@Composable
private fun FollowPreview() {
    val state = FollowState(songSteps(SongParser.parse(Songs.get("uskudar")!!.content))).apply {
        index = 2
        lastMatch = ChordMatch("Am", 0.71f, "Em", 0.86f, false)
    }
    Box(Modifier.padding(12.dp)) {
        Surface(Modifier.fillMaxWidth().align(Alignment.BottomCenter), shape = RoundedCornerShape(24.dp), color = WoodDark) {
            FollowPanel(state, micAvailable = true, onToggleMic = {}, modifier = Modifier.padding(12.dp))
        }
    }
}

/** Önizleme için örnek kullanıcı şarkısı (özgün sözler). */
object Demo {
    val song = UserSong(
        id = "demo", title = "Sabah Yolu", artist = "Benim şarkım", bpm = 88,
        chords = listOf("G", "D", "Em", "C", "F#m"),
        content = "# Kıta\n[G]Sabah olur [D]yollara düşerim\n[Em]Gün doğar, [C]ben de yürürüm\n\n# Nakarat\n[C]Yol uzun, [G]yürek hafif\n[D]Şarkım benimle [G]",
    )
    val sheet = UserSong(
        id = "sheet", title = "Yapıştırılmış şarkı", chords = listOf("Am", "G"),
        content = "Am          G\nBir gün daha geçti işte\nF           E\nDönmedin sen hâlâ",
    )
}
