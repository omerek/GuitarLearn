package com.omerek.guitarlearn.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

/** Tamamlanan dersler ve akor geçişi rekorları. Telefonda (SharedPreferences) saklanır. */
class Progress(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    val completedLessons = mutableStateOf(prefs.getStringSet(KEY_DONE, emptySet())!!.toSet())
    val bestChanges = mutableStateMapOf<String, Int>().apply {
        prefs.all.forEach { (k, v) -> if (k.startsWith(PREFIX_BEST) && v is Int) put(k.removePrefix(PREFIX_BEST), v) }
    }

    fun isDone(lessonId: String) = lessonId in completedLessons.value

    fun setDone(lessonId: String, done: Boolean) {
        val next = if (done) completedLessons.value + lessonId else completedLessons.value - lessonId
        completedLessons.value = next
        prefs.edit { putStringSet(KEY_DONE, next) }
    }

    fun best(from: String, to: String): Int = bestChanges[key(from, to)] ?: 0

    /** Yeni rekor ise kaydeder ve true döner. */
    fun recordChanges(from: String, to: String, count: Int): Boolean {
        val k = key(from, to)
        if (count <= (bestChanges[k] ?: 0)) return false
        bestChanges[k] = count
        prefs.edit { putInt(PREFIX_BEST + k, count) }
        return true
    }

    private fun key(a: String, b: String) = listOf(a, b).sorted().joinToString("-")

    private companion object {
        const val KEY_DONE = "done_lessons"
        const val PREFIX_BEST = "best_"
    }
}
