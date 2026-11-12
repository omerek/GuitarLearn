package com.omerek.guitarlearn.ui.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.omerek.guitarlearn.data.UserSongStore
import java.time.LocalDate

/** Yedekleme düğmelerinin çağıracağı işlemler. */
class BackupActions(val export: () -> Unit, val import: () -> Unit)

/**
 * Android'in dosya seçicisini kullanır: kullanıcı yedeği istediği yere (İndirilenler, Drive klasörü, USB…)
 * kaydeder ve oradan geri yükler. Uygulama internete hiçbir şey göndermez.
 */
@Composable
fun rememberBackupActions(store: UserSongStore, onResult: (String) -> Unit): BackupActions {
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching { store.exportTo(uri) }
                .onSuccess { onResult("${store.songs.size} şarkı yedeklendi") }
                .onFailure { onResult("Yedeklenemedi: ${it.message}") }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { store.importFrom(uri) }
                .onSuccess { onResult("$it şarkı yedekten yüklendi") }
                .onFailure { onResult(it.message ?: "Yedek okunamadı") }
        }
    }
    return BackupActions(
        export = { exporter.launch("gitar-ogren-sarkilarim-${LocalDate.now()}.json") },
        import = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
    )
}
