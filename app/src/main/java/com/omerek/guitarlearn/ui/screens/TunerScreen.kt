package com.omerek.guitarlearn.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.omerek.guitarlearn.audio.TunerEngine
import com.omerek.guitarlearn.ui.platform.KeepScreenOn

/** Akort ekranı: mikrofon izni ve dinleme döngüsü burada, görünüm TunerView içinde. */
@Composable
fun TunerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    if (!granted) {
        TunerView(frequency = -1f, micGranted = false, onRequestMic = { launcher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = modifier)
        return
    }

    val engine = remember { TunerEngine() }
    val lifecycleOwner = LocalLifecycleOwner.current
    KeepScreenOn()
    // Ekran görünürken mikrofonu dinle, arka plana geçince bırak
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> engine.start()
                Lifecycle.Event.ON_PAUSE -> engine.stop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) engine.start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.stop()
        }
    }
    val reading by engine.reading.collectAsState()
    TunerView(frequency = reading.frequency, micGranted = true, onRequestMic = {}, modifier = modifier)
}
