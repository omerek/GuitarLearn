package com.omerek.guitarlearn.ui.platform

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat

/** Bu bileşen ekrandayken telefonun ekranı kendiliğinden kapanmaz. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Çalışma anı izni (mikrofon, Bluetooth): granted şu anki durum, request() izni sorar. */
class MicPermission(private val state: State<Boolean>, private val ask: () -> Unit) {
    val granted: Boolean get() = state.value
    fun request() = ask()
}

@Composable
fun rememberMicPermission(): MicPermission {
    val context = LocalContext.current
    val granted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted.value = it }
    return remember(launcher) { MicPermission(granted) { launcher.launch(Manifest.permission.RECORD_AUDIO) } }
}

/** Gitar LED kartı için Bluetooth izinleri (Android sürümüne göre tarama/bağlanma ya da konum). */
@Composable
fun rememberBluetoothPermission(): MicPermission {
    val context = LocalContext.current
    val perms = com.omerek.guitarlearn.led.LedController.blePermissions()
    fun check() = perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    val granted = remember { mutableStateOf(check()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted.value = check()
        if (granted.value) com.omerek.guitarlearn.led.LedController.connect()
    }
    return remember(launcher) { MicPermission(granted) { launcher.launch(perms) } }
}

/**
 * Telefondan bir dosya seçtirir ve içeriğini okur (ör. kart yazılımı .bin dosyası).
 * Dönen işlev seçiciyi açar; sonuç onPicked(baytlar, dosya adı) ile gelir, iptal/hata durumunda baytlar null.
 */
@Composable
fun rememberBinaryFilePicker(onPicked: (ByteArray?, String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/')
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        onPicked(bytes, name)
    }
    return { launcher.launch(arrayOf("application/octet-stream", "application/macbinary", "*/*")) }
}
