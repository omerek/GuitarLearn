package com.omerek.guitarlearn.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import java.io.File

/**
 * Kullanıcı şarkılarını telefonun uygulama klasöründe (user_songs.json) tutar.
 * Uygulama silinince bu dosya da silinir; bu yüzden yedek dosyasına dışa/içe aktarma vardır.
 */
class UserSongStore(private val context: Context) {
    private val file = File(context.filesDir, "user_songs.json")

    val songs = mutableStateListOf<UserSong>().apply {
        try {
            if (file.exists()) addAll(UserSongBackup.decode(file.readText()))
        } catch (_: Exception) {
            // Bozuk dosyayı silme; kenara kopyala ki veri kaybolmasın
            file.copyTo(File(context.filesDir, "user_songs.broken.json"), overwrite = true)
        }
    }

    fun get(id: String): UserSong? = songs.firstOrNull { it.id == id }

    fun upsert(song: UserSong) {
        val updated = song.copy(updatedAt = System.currentTimeMillis())
        val i = songs.indexOfFirst { it.id == song.id }
        if (i >= 0) songs[i] = updated else songs.add(0, updated)
        persist()
    }

    fun delete(id: String) {
        songs.removeAll { it.id == id }
        persist()
    }

    /** Tüm şarkıları kullanıcının seçtiği dosyaya yazar. */
    fun exportTo(uri: Uri) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(UserSongBackup.encode(songs).toByteArray()) }
            ?: throw IllegalStateException("Dosya açılamadı")
    }

    /** Yedek dosyasından şarkıları ekler; aynı kimlikli şarkılar güncellenir. Eklenen/güncellenen sayısını döndürür. */
    fun importFrom(uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: throw IllegalStateException("Dosya açılamadı")
        val imported = UserSongBackup.decode(text)
        imported.forEach { s ->
            val i = songs.indexOfFirst { it.id == s.id }
            if (i >= 0) songs[i] = s else songs.add(s)
        }
        persist()
        return imported.size
    }

    private fun persist() {
        val tmp = File(context.filesDir, "user_songs.json.tmp")
        tmp.writeText(UserSongBackup.encode(songs))
        tmp.renameTo(file)
    }
}
