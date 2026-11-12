package com.omerek.guitarlearn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omerek.guitarlearn.data.Progress
import com.omerek.guitarlearn.data.Songs
import com.omerek.guitarlearn.data.UserSong
import com.omerek.guitarlearn.data.UserSongStore
import com.omerek.guitarlearn.ui.platform.rememberBackupActions
import com.omerek.guitarlearn.ui.screens.SongEditorScreen
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.omerek.guitarlearn.data.Tool
import com.omerek.guitarlearn.ui.screens.ChordLibraryScreen
import com.omerek.guitarlearn.ui.screens.LessonDetailScreen
import com.omerek.guitarlearn.ui.screens.LessonsScreen
import com.omerek.guitarlearn.ui.screens.MetronomeScreen
import com.omerek.guitarlearn.ui.screens.SongDetailScreen
import com.omerek.guitarlearn.ui.screens.SongsScreen
import com.omerek.guitarlearn.ui.screens.TunerScreen

enum class Tab(val label: String, val icon: ImageVector) {
    LESSONS("Dersler", Icons.Rounded.School),
    CHORDS("Akorlar", Icons.Rounded.GridView),
    SONGS("Şarkılar", Icons.Rounded.LibraryMusic),
    TUNER("Akort", Icons.Rounded.GraphicEq),
    METRONOME("Metronom", Icons.Rounded.Timer),
}

/**
 * Uygulamanın kökü: alt menü + basit, durum tabanlı gezinme.
 * Ders veya şarkı açıkken geri tuşu listeye döner.
 */
@Composable
fun GuitarApp(progress: Progress, userSongs: UserSongStore) {
    var tab by rememberSaveable { mutableStateOf(Tab.LESSONS) }
    var openLesson by rememberSaveable { mutableStateOf<String?>(null) }
    var openSong by rememberSaveable { mutableStateOf<String?>(null) }
    // Düzenlenen kullanıcı şarkısı: "new" ya da kimliği
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backup = rememberBackupActions(userSongs) { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    // Şarkıya dersten gelindiyse geri tuşu derse dönsün
    var songFromLesson by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = editing == null && (openSong != null || openLesson != null || tab != Tab.LESSONS)) {
        when {
            openSong != null -> {
                openSong = null
                if (songFromLesson) { tab = Tab.LESSONS; songFromLesson = false }
            }
            openLesson != null -> openLesson = null
            else -> tab = Tab.LESSONS
        }
    }

    Scaffold(
        // Başlık alanları durum çubuğunun arkasına uzanır; boşlukları ekranlar kendisi ayarlar
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = {
                            if (tab == t) { openLesson = null; openSong = null }
                            editing = null
                            tab = t
                            songFromLesson = false
                        },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, fontSize = 11.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            Tab.LESSONS -> {
                val lessonId = openLesson
                if (lessonId == null) {
                    LessonsScreen(progress, modifier, onOpen = { openLesson = it })
                } else {
                    LessonDetailScreen(
                        lessonId = lessonId,
                        progress = progress,
                        modifier = modifier,
                        onBack = { openLesson = null },
                        onOpenSong = { openSong = it; songFromLesson = true; tab = Tab.SONGS },
                        onOpenTool = { tool -> tab = if (tool == Tool.TUNER) Tab.TUNER else Tab.METRONOME },
                        onNextLesson = { openLesson = it },
                    )
                }
            }
            Tab.CHORDS -> ChordLibraryScreen(modifier)
            Tab.SONGS -> {
                val songId = openSong
                val editId = editing
                val userId = songId?.removePrefix(UserSong.ID_PREFIX)?.takeIf { songId.startsWith(UserSong.ID_PREFIX) }
                when {
                    editId != null -> {
                        val initial = if (editId == "new") null else userSongs.get(editId)
                        BackHandler { editing = null }
                        SongEditorScreen(
                            initial = initial,
                            modifier = modifier,
                            onSave = { song ->
                                userSongs.upsert(song)
                                editing = null
                                openSong = UserSong.ID_PREFIX + song.id
                                scope.launch { snackbar.showSnackbar("\"${song.title}\" kaydedildi") }
                            },
                            onCancel = { editing = null },
                            onDelete = { id ->
                                userSongs.delete(id)
                                editing = null
                                openSong = null
                            },
                        )
                    }
                    songId == null -> SongsScreen(
                        userSongs = userSongs.songs,
                        modifier = modifier,
                        onOpen = { openSong = it },
                        onNew = { editing = "new" },
                        onExport = backup.export,
                        onImport = backup.import,
                    )
                    else -> {
                        val song = if (userId != null) userSongs.get(userId)?.toSong() else Songs.get(songId)
                        if (song == null) {
                            LaunchedEffect(songId) { openSong = null }
                        } else {
                            SongDetailScreen(
                                song = song,
                                modifier = modifier,
                                onBack = {
                                    openSong = null
                                    if (songFromLesson) { tab = Tab.LESSONS; songFromLesson = false }
                                },
                                onEdit = userId?.let { id -> { editing = id } },
                            )
                        }
                    }
                }
            }
            Tab.TUNER -> TunerScreen(modifier)
            Tab.METRONOME -> MetronomeScreen(modifier)
        }
    }
}
