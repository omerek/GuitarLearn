package com.omerek.guitarlearn.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.omerek.guitarlearn.audio.SoundPlayer
import com.omerek.guitarlearn.audio.TunerEngine
import com.omerek.guitarlearn.audio.Tuning
import com.omerek.guitarlearn.ui.components.TipBox
import com.omerek.guitarlearn.ui.theme.InTune
import com.omerek.guitarlearn.ui.theme.OffTune
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val IN_TUNE_CENTS = 5.0

@Composable
fun TunerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Akort", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Text("Standart akort · Mi La Re Sol Si Mi", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        if (granted) {
            TunerContent()
        } else {
            Spacer(Modifier.height(32.dp))
            Text(
                "Akort edici telin sesini duymak için mikrofonu kullanır. Ses kaydedilmez ve hiçbir yere gönderilmez.",
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Mikrofon iznini ver") }
            Spacer(Modifier.height(24.dp))
            Text("İzin vermeden de referans seslerle kulaktan akort edebilirsin:", textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            ReferenceButtons()
        }
    }
}

@Composable
private fun TunerContent() {
    val engine = remember { TunerEngine() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

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
        view.keepScreenOn = true
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.stop()
            view.keepScreenOn = false
        }
    }

    val reading by engine.reading.collectAsState()
    var selected by rememberSaveable { mutableStateOf<Int?>(null) } // tel numarası; null = otomatik

    val freq = reading.frequency.toDouble()
    val hasPitch = freq > 0
    val target = when {
        selected != null -> Tuning.standard.first { it.number == selected }
        hasPitch -> Tuning.closestString(freq)
        else -> null
    }
    val cents = if (hasPitch && target != null) Tuning.cents(freq, target.frequency) else 0.0
    val clamped = cents.coerceIn(-50.0, 50.0).toFloat()
    val needle by animateFloatAsState(if (hasPitch) clamped else 0f, tween(120), label = "needle")
    val inTune = hasPitch && abs(cents) < IN_TUNE_CENTS
    val color = when {
        !hasPitch -> MaterialTheme.colorScheme.outline
        inTune -> InTune
        abs(cents) < 20 -> Color(0xFFF9A825)
        else -> OffTune
    }

    // Hedef tel ve nota
    Text(
        text = target?.let { "${it.number}. tel · ${it.solfege} (${it.note}${it.octave})" } ?: "Bir tel çal…",
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.height(8.dp))
    Gauge(needle, color, hasPitch)
    Text(
        text = if (hasPitch) "%.1f Hz".format(freq) else "— Hz",
        style = MaterialTheme.typography.bodyLarge,
    )
    if (hasPitch) {
        val heard = Tuning.nearestNote(freq)
        Text("Duyulan: ${heard.solfege} (${heard.name}${heard.octave})", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = when {
            !hasPitch -> "Teli tek başına çal ve sesi sönene kadar bekle."
            inTune -> "Akortlu ✓"
            abs(cents) >= 50 -> if (cents < 0) "Çok pes: teli epey sık" else "Çok tiz: teli epey gevşet"
            cents < 0 -> "Pes (%+.0f sent): teli biraz sık".format(cents)
            else -> "Tiz (%+.0f sent): teli biraz gevşet".format(cents)
        },
        style = MaterialTheme.typography.titleMedium,
        color = color,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        FilterChip(selected = selected == null, onClick = { selected = null }, label = { Text("Otomatik") })
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Tuning.standard.forEach { s ->
            val active = target?.number == s.number
            FilterChip(
                selected = selected == s.number || (selected == null && active && hasPitch),
                onClick = { selected = if (selected == s.number) null else s.number },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(s.note, fontWeight = FontWeight.Bold)
                        Text("${s.number}", fontSize = 10.sp)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = {
        val s = target ?: Tuning.standard.first()
        SoundPlayer.playNote(s.frequency, 3.0)
    }) { Text("🔊 ${target?.let { "${it.number}. telin" } ?: "6. telin"} referans sesini çal") }

    Spacer(Modifier.height(16.dp))
    TipBox("Otomatik modda uygulama hangi teli çaldığını kendisi bulur. Bir teli seçersen sadece o tele göre ölçer; çok bozuk bir teli akort ederken bu daha güvenilirdir.")
    Spacer(Modifier.height(8.dp))
    TipBox("Sessiz bir ortamda, teli tırnakla değil parmak ucuyla orta kuvvette çal. Naylon teller yeniyse akort sık kaçar.")
}

@Composable
private fun ReferenceButtons() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Tuning.standard.forEach { s ->
            OutlinedButton(onClick = { SoundPlayer.playNote(s.frequency, 3.0) }, modifier = Modifier.weight(1f)) {
                Text(s.note)
            }
        }
    }
}

/** -50..+50 sent aralığında yarım daire gösterge. */
@Composable
private fun Gauge(cents: Float, color: Color, active: Boolean) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val tick = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth(0.9f).aspectRatio(2f)) {
        val stroke = size.width * 0.05f
        val radius = size.width / 2 - stroke
        val center = Offset(size.width / 2, size.height - stroke / 2)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        drawArc(track, 180f, 180f, false, arcTopLeft, Size(radius * 2, radius * 2), style = Stroke(stroke, cap = StrokeCap.Round))
        // Yeşil "akortlu" bölge (±5 sent)
        val zone = (IN_TUNE_CENTS / 50.0 * 90.0).toFloat()
        drawArc(InTune.copy(alpha = 0.35f), 270f - zone, zone * 2, false, arcTopLeft, Size(radius * 2, radius * 2), style = Stroke(stroke))
        // Çentikler: her 10 sent
        for (c in -50..50 step 10) {
            val a = Math.toRadians(270.0 + c / 50.0 * 90.0)
            val outer = radius - stroke
            val inner = outer - if (c == 0) stroke * 1.4f else stroke * 0.7f
            drawLine(
                tick,
                Offset(center.x + (cos(a) * inner).toFloat(), center.y + (sin(a) * inner).toFloat()),
                Offset(center.x + (cos(a) * outer).toFloat(), center.y + (sin(a) * outer).toFloat()),
                strokeWidth = 3f,
            )
        }
        // İbre
        val angle = Math.toRadians(270.0 + cents / 50.0 * 90.0)
        val len = radius * 0.9f
        drawLine(
            if (active) color else tick,
            center,
            Offset(center.x + (cos(angle) * len).toFloat(), center.y + (sin(angle) * len).toFloat()),
            strokeWidth = stroke * 0.35f,
            cap = StrokeCap.Round,
        )
        drawCircle(if (active) color else tick, stroke * 0.6f, center)
    }
}
