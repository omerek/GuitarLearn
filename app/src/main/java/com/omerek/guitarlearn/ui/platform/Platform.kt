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

/** Mikrofon izni: granted şu anki durum, request() izni sorar. */
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
