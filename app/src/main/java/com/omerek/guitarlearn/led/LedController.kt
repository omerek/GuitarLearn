package com.omerek.guitarlearn.led

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.omerek.guitarlearn.data.Chord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Telefona USB-OTG ile takılan ESP32-S2 Mini'yi (USB CDC seri port) bulur, bağlanır ve LED komutlarını gönderir.
 * Harici kütüphane kullanmaz: Android'in USB Host API'siyle basit bir CDC-ACM sürücüsüdür.
 */
object LedController {
    private const val ACTION_PERMISSION = "com.omerek.guitarlearn.USB_PERMISSION"
    private const val ESPRESSIF_VID = 0x303A

    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences
    private val usb: UsbManager by lazy { app.getSystemService(Context.USB_SERVICE) as UsbManager }

    private val _state = MutableStateFlow(LedState())
    val state: StateFlow<LedState> = _state.asStateFlow()

    private val _buttons = MutableSharedFlow<LedButton>(extraBufferCapacity = 8)

    /** Gitara takılı önceki/sonraki düğmeleri. Açık olan ekran (şarkı, akor penceresi) dinler. */
    val buttons: SharedFlow<LedButton> = _buttons

    private val _config = MutableStateFlow(LedConfig())
    val config: StateFlow<LedConfig> = _config.asStateFlow()

    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "led-usb").apply { isDaemon = true } }
    @Volatile private var port: CdcPort? = null
    @Volatile private var connecting = false
    private var current: List<FretNote> = emptyList()

    /** LED'lerde şu an gösterilen akor (geçici bir pencere kapanınca geri yüklemek için). */
    var currentChord: Chord? = null
        private set

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        prefs = app.getSharedPreferences("led", Context.MODE_PRIVATE)
        _config.value = LedConfig(
            arrangement = LedArrangement.entries.getOrElse(prefs.getInt("arrangement", 0)) { LedArrangement.PER_FRET },
            fretCount = prefs.getInt("frets", 12),
            brightness = prefs.getInt("brightness", 60),
            firstStringFirst = prefs.getBoolean("firstStringFirst", true),
            zigzag = prefs.getBoolean("zigzag", false),
        )
        val filter = IntentFilter().apply {
            addAction(ACTION_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        connect()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PERMISSION -> {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) connect()
                    else _state.value = LedState(LedStatus.NEEDS_PERMISSION, "USB izni verilmedi")
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val dev = deviceExtra(intent)
                    if (dev == null || dev.deviceId == port?.device?.deviceId) disconnect("Kart çıkarıldı")
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> connect()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun deviceExtra(intent: Intent): UsbDevice? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        else intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

    /** Takılı kartı bulur; izin yoksa ister, varsa bağlanır. Uygulama USB takılınca açıldığında da çağrılır. */
    fun connect() {
        if (!::app.isInitialized || port != null || connecting) return
        val device = findDevice()
        if (device == null) {
            _state.value = LedState(LedStatus.NO_DEVICE, "Kart bağlı değil")
            return
        }
        if (!usb.hasPermission(device)) {
            _state.value = LedState(LedStatus.NEEDS_PERMISSION, "USB izni bekleniyor")
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getBroadcast(app, 0, Intent(ACTION_PERMISSION).setPackage(app.packageName), flags)
            usb.requestPermission(device, pi)
            return
        }
        connecting = true
        writer.execute {
            val p = try { CdcPort.open(usb, device) } catch (e: Exception) { null }
            connecting = false
            if (p == null) {
                _state.value = LedState(LedStatus.ERROR, "Karta bağlanılamadı")
                return@execute
            }
            port = p
            _state.value = LedState(LedStatus.CONNECTED, "Kart bağlandı")
            p.startReading { line ->
                LedProtocol.parseButton(line)?.let { _buttons.tryEmit(it) }
                if (LedProtocol.parseHello(line) != null) {
                    _state.value = _state.value.copy(firmwareOk = true, message = "LED kartı hazır")
                }
            }
            sendNow(LedProtocol.hello())
            sendSetup()
            sendNow(LedProtocol.connectedAnimation())
            sendNow(LedProtocol.frame(LedLayout.pixels(current, _config.value)))
        }
    }

    private fun findDevice(): UsbDevice? {
        val all = usb.deviceList.values
        return all.firstOrNull { it.vendorId == ESPRESSIF_VID && CdcPort.hasCdc(it) }
            ?: all.firstOrNull { CdcPort.hasCdc(it) }
    }

    private fun disconnect(message: String) {
        val p = port
        port = null
        writer.execute { p?.close() }
        _state.value = LedState(LedStatus.NO_DEVICE, message)
    }

    /** Ekranda gösterilen akoru LED'lere de yansıtır. null = söndür. */
    fun show(chord: Chord?) {
        currentChord = chord
        current = chord?.let { LedLayout.pressedNotes(it) } ?: emptyList()
        send(LedProtocol.frame(LedLayout.pixels(current, _config.value)))
    }

    fun clear() = show(null)

    fun test() = send(LedProtocol.test())

    fun updateConfig(newConfig: LedConfig) {
        val c = newConfig.copy(fretCount = newConfig.fretCount.coerceIn(1, 24), brightness = newConfig.brightness.coerceIn(5, 255))
        _config.value = c
        prefs.edit()
            .putInt("arrangement", c.arrangement.ordinal)
            .putInt("frets", c.fretCount)
            .putInt("brightness", c.brightness)
            .putBoolean("firstStringFirst", c.firstStringFirst)
            .putBoolean("zigzag", c.zigzag)
            .apply()
        writer.execute {
            sendSetup()
            sendNow(LedProtocol.frame(LedLayout.pixels(current, c)))
        }
    }

    private fun sendSetup() {
        val c = _config.value
        sendNow(LedProtocol.count(c.ledCount))
        sendNow(LedProtocol.brightness(c.brightness))
    }

    private fun send(text: String) {
        if (port == null) return
        writer.execute { sendNow(text) }
    }

    private fun sendNow(text: String) {
        val p = port ?: return
        if (!p.write(text.toByteArray(Charsets.US_ASCII))) disconnect("Kartla bağlantı koptu")
    }
}

/** En küçük CDC-ACM (USB seri port) sürücüsü: veri arayüzündeki bulk uçlarla yazar/okur. */
private class CdcPort(
    val device: UsbDevice,
    private val conn: UsbDeviceConnection,
    private val interfaces: List<UsbInterface>,
    private val out: UsbEndpoint,
    private val input: UsbEndpoint?,
) {
    @Volatile private var open = true

    fun write(data: ByteArray): Boolean {
        var offset = 0
        while (offset < data.size) {
            val n = minOf(out.maxPacketSize, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + n)
            val sent = conn.bulkTransfer(out, chunk, n, 500)
            if (sent < 0) return false
            offset += sent
        }
        return true
    }

    fun startReading(onLine: (String) -> Unit) {
        val ep = input ?: return
        thread(name = "led-usb-read", isDaemon = true) {
            val buf = ByteArray(ep.maxPacketSize.coerceAtLeast(64))
            val line = StringBuilder()
            while (open) {
                val n = try { conn.bulkTransfer(ep, buf, buf.size, 300) } catch (e: Exception) { -1 }
                if (n <= 0) continue
                for (i in 0 until n) {
                    val ch = buf[i].toInt().toChar()
                    if (ch == '\n') { onLine(line.toString()); line.clear() }
                    else if (line.length < 200) line.append(ch)
                }
            }
        }
    }

    fun close() {
        open = false
        interfaces.forEach { runCatching { conn.releaseInterface(it) } }
        runCatching { conn.close() }
    }

    companion object {
        fun hasCdc(d: UsbDevice): Boolean =
            (0 until d.interfaceCount).any { d.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_CDC_DATA }

        fun open(usb: UsbManager, device: UsbDevice): CdcPort? {
            val all = (0 until device.interfaceCount).map { device.getInterface(it) }
            val data = all.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA } ?: return null
            val control = all.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_COMM }
            var out: UsbEndpoint? = null
            var input: UsbEndpoint? = null
            for (i in 0 until data.endpointCount) {
                val ep = data.getEndpoint(i)
                if (ep.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
                if (ep.direction == UsbConstants.USB_DIR_OUT) out = ep else input = ep
            }
            if (out == null) return null
            val conn = usb.openDevice(device) ?: return null
            val claimed = listOfNotNull(control, data).filter { conn.claimInterface(it, true) }
            if (data !in claimed) { conn.close(); return null }
            control?.let {
                // 115200 8N1 (1200 baud kullanılmaz: ESP32'yi yükleme moduna sokar) ve DTR+RTS açık
                val lineCoding = byteArrayOf(0x00, 0xC2.toByte(), 0x01, 0x00, 0, 0, 8)
                conn.controlTransfer(0x21, 0x20, 0, it.id, lineCoding, lineCoding.size, 500)
                conn.controlTransfer(0x21, 0x22, 0x03, it.id, null, 0, 500)
            }
            return CdcPort(device, conn, claimed, out, input)
        }
    }
}
