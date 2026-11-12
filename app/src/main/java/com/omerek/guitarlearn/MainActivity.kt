package com.omerek.guitarlearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.data.Progress
import com.omerek.guitarlearn.ui.GuitarApp
import com.omerek.guitarlearn.ui.theme.GuitarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val progress = Progress(applicationContext)
        setContent {
            GuitarTheme {
                GuitarApp(progress)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Uygulama arka plana geçince metronom susar
        Metronome.stop()
    }
}
