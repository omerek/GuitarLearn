package com.omerek.guitarlearn.data

import androidx.compose.runtime.mutableStateListOf

class UserSongStore(initial: List<UserSong> = emptyList()) {
    val songs = mutableStateListOf<UserSong>().apply { addAll(initial) }
    fun get(id: String): UserSong? = songs.firstOrNull { it.id == id }
    fun upsert(song: UserSong) { val i = songs.indexOfFirst { it.id == song.id }; if (i >= 0) songs[i] = song else songs.add(0, song) }
    fun delete(id: String) { songs.removeAll { it.id == id } }
}
