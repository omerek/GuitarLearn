package com.omerek.guitarlearn.audio

class ChordListener(private val onStrum: (FloatArray) -> Unit) {
    fun start() {}
    fun stop() {}
}
