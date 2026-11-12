package com.omerek.guitarlearn.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SoundPlayer {
    fun playChord(midiNotes: List<Int>) {}
    fun playNote(frequency: Double, seconds: Double = 2.5) {}
    fun playSequence(events: List<List<Int>>, bpm: Int) {}
    fun stop() {}
}

data class MetronomeState(val running: Boolean = false, val bpm: Int = 70, val beatsPerBar: Int = 4, val beat: Int = -1, val owner: String = "")

object Metronome {
    const val MIN_BPM = 40
    const val MAX_BPM = 220
    val mutable = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = mutable
    fun setBpm(bpm: Int) { mutable.value = mutable.value.copy(bpm = bpm) }
    fun setBeatsPerBar(beats: Int) { mutable.value = mutable.value.copy(beatsPerBar = beats) }
    fun toggle(owner: String = "") {}
    fun start(bpm: Int = 70, beatsPerBar: Int = 4, owner: String = "") { mutable.value = MetronomeState(true, bpm, beatsPerBar, 0, owner) }
    fun stop() { mutable.value = mutable.value.copy(running = false, beat = -1, owner = "") }
}
