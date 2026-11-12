package com.omerek.guitarlearn.led

import com.omerek.guitarlearn.data.Chord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Önizleme için USB/Bluetooth'suz LED denetleyicisi. */
object LedController {
    val state: StateFlow<LedState> =
        MutableStateFlow(LedState(LedStatus.CONNECTED, "LED kartı hazır (Bluetooth)", true, LedTransport.BLE))
    val scanResults: StateFlow<List<BleCandidate>> =
        MutableStateFlow(listOf(BleCandidate("GitarLED", "7C:DF:A1:00:12:34", -58)))
    val scanning: StateFlow<Boolean> = MutableStateFlow(false)
    val savedBleAddress: String? = "7C:DF:A1:00:12:34"
    val bluetoothAvailable = true
    val bluetoothOn = true
    fun blePermissions(): Array<String> = emptyArray()
    fun startScan() {}
    fun stopScan() {}
    fun connectBle(address: String) {}
    fun forgetBle() {}
    private val _config = MutableStateFlow(LedConfig(fretCount = 5))
    val config: StateFlow<LedConfig> = _config
    val buttons: SharedFlow<LedButton> = MutableSharedFlow()
    var currentChord: Chord? = null
        private set
    fun connect() {}
    fun show(chord: Chord?) { currentChord = chord }
    fun clear() = show(null)
    fun test() {}
    fun updateConfig(c: LedConfig) { _config.value = c }
}
