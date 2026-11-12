package com.omerek.guitarlearn.led

import com.omerek.guitarlearn.data.Chord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Önizleme için USB'siz LED denetleyicisi. */
object LedController {
    val state: StateFlow<LedState> = MutableStateFlow(LedState(LedStatus.CONNECTED, "LED kartı hazır", true))
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
