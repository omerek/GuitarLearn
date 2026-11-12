package com.omerek.guitarlearn.data

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf

class Progress(done: Set<String> = emptySet()) {
    val completedLessons = mutableStateOf(done)
    val bestChanges = mutableStateMapOf<String, Int>()
    fun isDone(lessonId: String) = lessonId in completedLessons.value
    fun setDone(lessonId: String, done: Boolean) {}
    fun best(from: String, to: String): Int = bestChanges["$from-$to"] ?: 0
    fun recordChanges(from: String, to: String, count: Int): Boolean = false
}
