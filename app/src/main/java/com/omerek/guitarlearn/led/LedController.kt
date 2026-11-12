package com.omerek.guitarlearn.led

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.omerek.guitarlearn.data.Chord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Gitar LED kartını yönetir:
 *  - USB-OTG ile takılı kart (ESP32-S2 Mini ya da ESP32-S3) varsa USB'den konuşur (önceliklidir).
 *  - Yoksa Bluetooth (BLE) ile ESP32-S3 kartına bağlanır; son bağlanılan kart hatırlanır ve menzile girince bağlanılır.
 * Harici kütüphane kullanılmaz.
 */
@SuppressLint("MissingPermission") // Bluetooth çağrılarından önce hasBlePermission() denetlenir
object LedController {
    private const val ACTION_PERMISSION = "com.omerek.guitarlearn.USB_PERMISSION"
    private const val ESPRESSIF_VID = 0x303A
    private const val SCAN_MS = 10_000L

    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences
    private val usb: UsbManager by lazy { app.getSystemService(Context.USB_SERVICE) as UsbManager }
    private val bluetooth: BluetoothAdapter? by lazy {
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }
    private val main = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(LedState())
    val state: StateFlow<LedState> = _state.asStateFlow()

    private val _buttons = MutableSharedFlow<LedButton>(extraBufferCapacity = 8)

    /** Gitara takılı önceki/sonraki düğmeleri. Açık olan ekran (şarkı, akor penceresi) dinler. */
    val buttons: SharedFlow<LedButton> = _buttons

    private val _config = MutableStateFlow(LedConfig())
    val config: StateFlow<LedConfig> = _config.asStateFlow()

    private val _scanResults = MutableStateFlow<List<BleCandidate>>(emptyList())

    /** Bluetooth taramasında bulunan kartlar. */
    val scanResults: StateFlow<List<BleCandidate>> = _scanResults.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _update = MutableStateFlow<FirmwareUpdateState>(FirmwareUpdateState.Idle)

    /** Kart yazılımı güncellemesinin durumu (yalnızca USB). */
    val firmwareUpdate: StateFlow<FirmwareUpdateState> = _update.asStateFlow()
    private val updateReplies = LinkedBlockingQueue<String>()

    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "led-link").apply { isDaemon = true } }
    @Volatile private var link: LedLink? = null
    @Volatile private var usbDeviceId: Int? = null
    @Volatile private var connecting = false
    @Volatile private var pendingBle: BleLink? = null
    private var current: List<FretNote> = emptyList()

    /** LED'lerde şu an gösterilen akor (geçici bir pencere kapanınca geri yüklemek için). */
    var currentChord: Chord? = null
        private set

    /** Hatırlanan Bluetooth kartının adresi (yoksa null). */
    val savedBleAddress: String? get() = if (::prefs.isInitialized) prefs.getString("bleAddress", null) else null

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
                    else setState(LedState(LedStatus.NEEDS_PERMISSION, "USB izni verilmedi"))
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val dev = deviceExtra(intent)
                    if (usbDeviceId != null && (dev == null || dev.deviceId == usbDeviceId)) {
                        dropLink("Kart çıkarıldı")
                        connectSavedBle() // kablosuz kart varsa ona geç
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> connect()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun deviceExtra(intent: Intent): UsbDevice? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        else intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

    private fun setState(s: LedState) { _state.value = s }

    // ---------------------------------------------------------------- USB

    /**
     * Önce takılı USB kartını dener; yoksa hatırlanan Bluetooth kartına bağlanır.
     * Uygulama açılınca ve USB takılınca çağrılır.
     */
    fun connect() {
        if (!::app.isInitialized || connecting) return
        if (link != null && _state.value.transport == LedTransport.USB) return
        val device = findUsbDevice()
        if (device == null) {
            if (link == null) connectSavedBle()
            return
        }
        if (!usb.hasPermission(device)) {
            setState(LedState(LedStatus.NEEDS_PERMISSION, "USB izni bekleniyor"))
            val flags = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getBroadcast(app, 0, Intent(ACTION_PERMISSION).setPackage(app.packageName), flags)
            usb.requestPermission(device, pi)
            return
        }
        // USB öncelikli: Bluetooth bağlıysa bırak
        if (link != null) dropLink("USB'ye geçiliyor")
        connecting = true
        writer.execute {
            if (!CdcPort.hasCdc(device)) setState(LedState(LedStatus.NO_DEVICE, "Arduino açılıyor…"))
            val p: LedLink? = try {
                if (CdcPort.hasCdc(device)) CdcPort.open(usb, device) else SerialChipLink.open(usb, device)
            } catch (e: Exception) { null }
            connecting = false
            if (p == null) {
                setState(LedState(LedStatus.ERROR, "Karta bağlanılamadı"))
                return@execute
            }
            usbDeviceId = device.deviceId
            attach(p, LedTransport.USB)
        }
    }

    private fun findUsbDevice(): UsbDevice? {
        val all = usb.deviceList.values
        return all.firstOrNull { it.vendorId == ESPRESSIF_VID && CdcPort.hasCdc(it) }
            ?: all.firstOrNull { CdcPort.hasCdc(it) }
            ?: all.firstOrNull { SerialChipLink.driverFor(it) != null } // Arduino Nano (CH340/FTDI/CP210x)
    }

    // ---------------------------------------------------------------- Bluetooth

    /** Android sürümüne göre gereken Bluetooth izinleri. */
    fun blePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasBlePermission(): Boolean = ::app.isInitialized && blePermissions().all {
        ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED
    }

    val bluetoothAvailable: Boolean get() = ::app.isInitialized && bluetooth != null
    val bluetoothOn: Boolean get() = bluetooth?.isEnabled == true

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val c = BleCandidate(
                name = result.scanRecord?.deviceName ?: result.device.name ?: "Gitar LED",
                address = result.device.address,
                rssi = result.rssi,
            )
            _scanResults.value = (_scanResults.value.filter { it.address != c.address } + c).sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            _scanning.value = false
            setState(_state.value.copy(message = "Bluetooth taraması başlatılamadı ($errorCode)"))
        }
    }

    /** Yakındaki LED kartlarını 10 saniye boyunca arar. */
    fun startScan() {
        val adapter = bluetooth ?: return
        if (!hasBlePermission() || !adapter.isEnabled || _scanning.value) return
        val scanner = adapter.bluetoothLeScanner ?: return
        _scanResults.value = emptyList()
        _scanning.value = true
        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(BleLink.SERVICE)).build())
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        runCatching { scanner.startScan(filters, settings, scanCallback) }.onFailure { _scanning.value = false }
        main.postDelayed({ stopScan() }, SCAN_MS)
    }

    fun stopScan() {
        if (!_scanning.value) return
        _scanning.value = false
        runCatching { bluetooth?.bluetoothLeScanner?.stopScan(scanCallback) }
    }

    /** Taramada seçilen karta bağlanır ve onu hatırlar. */
    fun connectBle(address: String) {
        stopScan()
        prefs.edit().putString("bleAddress", address).apply()
        if (_state.value.transport == LedTransport.USB && link != null) {
            setState(_state.value.copy(message = "USB bağlıyken USB kullanılır. Kablosuz için kabloyu çıkar."))
            return
        }
        dropLink("Bağlanıyor…")
        startBle(address, autoConnect = false)
    }

    /** Hatırlanan Bluetooth kartını unutur ve bağlantıyı keser. */
    fun forgetBle() {
        prefs.edit().remove("bleAddress").apply()
        pendingBle?.close(); pendingBle = null
        if (_state.value.transport == LedTransport.BLE) dropLink("Bluetooth kartı unutuldu")
    }

    private fun connectSavedBle() {
        val address = savedBleAddress ?: run {
            if (link == null) setState(LedState(LedStatus.NO_DEVICE, "Kart bağlı değil"))
            return
        }
        if (pendingBle != null || link != null) return
        if (!hasBlePermission() || !bluetoothOn) {
            setState(LedState(LedStatus.NO_DEVICE, "Bluetooth kartı için Bluetooth'u aç"))
            return
        }
        startBle(address, autoConnect = true)
    }

    private fun startBle(address: String, autoConnect: Boolean) {
        val adapter = bluetooth ?: return
        if (!hasBlePermission() || !adapter.isEnabled) {
            setState(LedState(LedStatus.NEEDS_PERMISSION, "Bluetooth izni ya da Bluetooth kapalı"))
            return
        }
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return
        pendingBle?.close()
        setState(LedState(LedStatus.NO_DEVICE, if (autoConnect) "Bluetooth kartı bekleniyor…" else "Bluetooth ile bağlanıyor…"))
        pendingBle = BleLink.connect(
            app, device, autoConnect,
            onReady = { l ->
                if (pendingBle === l) pendingBle = null
                writer.execute { attach(l, LedTransport.BLE) }
            },
            onLost = { l ->
                if (pendingBle === l) pendingBle = null
                if (link === l) {
                    dropLink("Bluetooth bağlantısı koptu")
                    // Menzile geri girince kendiliğinden bağlansın
                    main.postDelayed({ connectSavedBle() }, 1500)
                } else if (link == null) {
                    setState(LedState(LedStatus.ERROR, "Bluetooth kartına bağlanılamadı"))
                }
            },
        )
    }

    // ---------------------------------------------------------------- Ortak

    /** Yazıcı iş parçacığında çağrılır: bağlantıyı devralır ve kartı kurar. */
    private fun attach(l: LedLink, transport: LedTransport) {
        if (link != null && link !== l) runCatching { link?.close() }
        link = l
        setState(LedState(LedStatus.CONNECTED, "Kart bağlandı (${transport.title})", transport = transport))
        l.startReading { line ->
            if (LedProtocol.isUpdateReply(line)) { updateReplies.offer(line.trim()); return@startReading }
            LedProtocol.parseButton(line)?.let { _buttons.tryEmit(it) }
            LedProtocol.parseHelloInfo(line)?.let { info ->
                _state.value = _state.value.copy(
                    firmwareOk = true,
                    message = "LED kartı hazır (${transport.title})",
                    firmwareVersion = info.version,
                    board = info.board,
                )
            }
        }
        sendNow(LedProtocol.hello())
        sendSetup()
        sendNow(LedProtocol.connectedAnimation())
        // Animasyon sürerken kart okumaz; Nano'nun 64 baytlık tamponu taşmasın
        Thread.sleep(900)
        sendNow(LedProtocol.frame(LedLayout.pixels(current, _config.value)))
    }

    private fun dropLink(message: String) {
        val l = link
        link = null
        usbDeviceId = null
        if (l != null) writer.execute { l.close() }
        setState(LedState(LedStatus.NO_DEVICE, message))
    }

    // ---------------------------------------------------------------- Yazılım güncellemesi

    /** Seçilen .bin dosyasını denetler; sorun yoksa null, varsa açıklaması. */
    fun checkFirmware(bin: ByteArray): String? {
        val s = _state.value
        if (s.status != LedStatus.CONNECTED || s.transport != LedTransport.USB) return "Güncelleme yalnızca USB ile yapılabilir. Kartı kabloyla bağla."
        if (s.board == "NANO") return "Arduino Nano telefondan güncellenemez. Yeni yazılımı bilgisayardan Arduino IDE ile yükle."
        if (s.firmwareVersion == null) return "Karttaki yazılım güncellemeyi desteklemiyor. Bu ilk sefer için bilgisayardan yükle."
        return FirmwareImage.problem(bin, s.board)
    }

    fun resetFirmwareUpdate() {
        if (_update.value !is FirmwareUpdateState.Running) _update.value = FirmwareUpdateState.Idle
    }

    /**
     * Yeni kart yazılımını USB üzerinden gönderir. Kart MD5 ile doğrular ve yeniden başlar;
     * bir şey ters giderse eski yazılım çalışmaya devam eder.
     */
    fun updateFirmware(bin: ByteArray) {
        checkFirmware(bin)?.let { _update.value = FirmwareUpdateState.Failed(it); return }
        if (_update.value is FirmwareUpdateState.Running) return
        val l = link ?: return
        _update.value = FirmwareUpdateState.Running(0f)
        writer.execute {
            val error = runUpdate(l, bin)
            _update.value = if (error == null) FirmwareUpdateState.Done else FirmwareUpdateState.Failed(error)
            if (error != null && link === l) {
                // Kart eski yazılımla çalışmaya devam ediyor: LED'leri geri yükle
                sendSetup()
                sendNow(LedProtocol.frame(LedLayout.pixels(current, _config.value)))
            }
        }
    }

    /** Yazıcı iş parçacığında çalışır; başarılıysa null, değilse hata metni döner. */
    private fun runUpdate(l: LedLink, bin: ByteArray): String? {
        updateReplies.clear()
        fun await(prefix: String, seconds: Long): String? {
            val reply = updateReplies.poll(seconds, TimeUnit.SECONDS) ?: return "Kart cevap vermedi"
            if (reply.startsWith("U ERR")) return "Kart hata bildirdi: " + reply.removePrefix("U ERR").trim()
            if (!reply.startsWith(prefix)) return "Beklenmeyen cevap: $reply"
            return null
        }
        if (!l.write(LedProtocol.updateStart(bin.size, FirmwareImage.md5Hex(bin)).toByteArray(Charsets.US_ASCII))) {
            return "Karta yazılamadı"
        }
        // Kart önce flaşta yer açar (silme birkaç saniye sürebilir)
        await("U OK", 40)?.let { return it }
        var offset = 0
        while (offset < bin.size) {
            val n = minOf(4096, bin.size - offset)
            if (!l.write(bin.copyOfRange(offset, offset + n))) return "Gönderim yarıda kaldı"
            offset += n
            await("U ACK", 20)?.let { return it }
            _update.value = FirmwareUpdateState.Running(offset.toFloat() / bin.size)
        }
        return await("U DONE", 40)
    }

    /** Ekranda gösterilen akoru LED'lere de yansıtır. null = söndür. */
    fun show(chord: Chord?) {
        currentChord = chord
        current = chord?.let { LedLayout.pressedNotes(it) } ?: emptyList()
        send(LedProtocol.frame(LedLayout.pixels(current, _config.value)))
    }

    fun clear() = show(null)

    fun test() {
        if (link == null) return
        writer.execute {
            sendNow(LedProtocol.test())
            // Test animasyonu bitene kadar bekle (kart bu sırada komut okumaz)
            Thread.sleep((6L * _config.value.ledCount * 30 + 600).coerceAtMost(8000))
            sendNow(LedProtocol.frame(LedLayout.pixels(current, _config.value)))
        }
    }

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
        if (link == null) return
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
        if (link == null) return
        writer.execute { sendNow(text) }
    }

    private fun sendNow(text: String) {
        val l = link ?: return
        if (!l.write(text.toByteArray(Charsets.US_ASCII)) && link === l) {
            val wasBle = _state.value.transport == LedTransport.BLE
            dropLink("Kartla bağlantı koptu")
            if (wasBle) main.postDelayed({ connectSavedBle() }, 1500)
        }
    }
}
