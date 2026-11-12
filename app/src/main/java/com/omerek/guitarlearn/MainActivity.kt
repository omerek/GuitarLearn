package com.omerek.guitarlearn

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.omerek.guitarlearn.audio.Metronome
import com.omerek.guitarlearn.data.Progress
import com.omerek.guitarlearn.led.LedController
import com.omerek.guitarlearn.ui.GuitarApp
import com.omerek.guitarlearn.ui.theme.GuitarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        val progress = Progress(applicationContext)
        // Gitar LED kartı: takılıysa bağlan (USB takılınca uygulama bu yolla da açılır)
        LedController.init(applicationContext)
        setContent {
            GuitarTheme {
                GuitarApp(progress)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) LedController.connect()
    }

    override fun onStop() {
        super.onStop()
        // Uygulama arka plana geçince metronom susar
        Metronome.stop()
    }
}
