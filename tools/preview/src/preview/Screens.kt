package preview

import androidx.compose.runtime.Composable
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.data.Progress
import com.omerek.guitarlearn.ui.GuitarApp
import com.omerek.guitarlearn.ui.components.ChordDialog
import com.omerek.guitarlearn.ui.screens.*

object Screens {
    val all: List<Pair<String, @Composable () -> Unit>> = listOf(
        "app" to { GuitarApp(Progress(setOf("l01", "l02", "l03"))) },
        "lessons" to { LessonsScreen(Progress(setOf("l01", "l02", "l03")), onOpen = {}) },
        "lesson04" to { LessonDetailScreen("l04", Progress(), onBack = {}, onOpenSong = {}, onOpenTool = {}, onNextLesson = {}) },
        "lesson11" to { LessonDetailScreen("l11", Progress(), onBack = {}, onOpenSong = {}, onOpenTool = {}, onNextLesson = {}) },
        "chords" to { ChordLibraryScreen() },
        "chordDialog" to { ChordDialog("F") {} },
        "songs" to { SongsScreen(onOpen = {}) },
        "song" to { SongDetailScreen("uskudar", onBack = {}) },
        "tunerFlat" to { TunerView(107.2f, true, {}) },
        "tunerOk" to { TunerView(146.9f, true, {}) },
        "tunerNoMic" to { TunerView(-1f, false, {}) },
        "metronome" to { MetronomeScreen() },
    )
}
