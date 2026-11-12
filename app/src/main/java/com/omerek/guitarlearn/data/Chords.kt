package com.omerek.guitarlearn.data

import com.omerek.guitarlearn.audio.Tuning

/**
 * Bir akor şeması.
 * @param frets 6 eleman, kalın Mi telinden (6. tel) ince Mi teline (1. tel): -1 = çalma, 0 = açık tel, n = n. perde.
 * @param fingers aynı sırayla parmak numarası (1 işaret, 2 orta, 3 yüzük, 4 serçe, 0 = parmak yok).
 * @param barreFret barre (tüm telleri işaret parmağıyla basma) varsa perde numarası.
 */
data class Chord(
    val name: String,
    val turkishName: String,
    val frets: List<Int>,
    val fingers: List<Int>,
    val level: ChordLevel,
    val barreFret: Int? = null,
    val tip: String = "",
) {
    init {
        require(frets.size == 6 && fingers.size == 6) { "$name: 6 tel olmalı" }
    }

    /** Çalınan tellerin MIDI notaları (sesli dinleme için). */
    val midiNotes: List<Int>
        get() = frets.mapIndexedNotNull { i, f -> if (f < 0) null else Tuning.openStringMidi[i] + f }
}

enum class ChordLevel(val title: String) {
    BASIC("Temel akorlar"),
    SEVENTH("7'li, sus ve renkli akorlar"),
    BARRE("Barre akorlar"),
}

object Chords {
    private const val X = -1

    val all: List<Chord> = listOf(
        Chord("Em", "Mi minör", listOf(0, 2, 2, 0, 0, 0), listOf(0, 2, 3, 0, 0, 0), ChordLevel.BASIC,
            tip = "En kolay akor: sadece iki parmak. Tüm telleri çal."),
        Chord("Am", "La minör", listOf(X, 0, 2, 2, 1, 0), listOf(0, 0, 2, 3, 1, 0), ChordLevel.BASIC,
            tip = "Em şeklini bir tel aşağı kaydır, işaret parmağını 2. tele ekle. 6. teli çalma."),
        Chord("E", "Mi majör", listOf(0, 2, 2, 1, 0, 0), listOf(0, 2, 3, 1, 0, 0), ChordLevel.BASIC,
            tip = "Em'e işaret parmağını 3. tel 1. perdeye ekle."),
        Chord("A", "La majör", listOf(X, 0, 2, 2, 2, 0), listOf(0, 0, 1, 2, 3, 0), ChordLevel.BASIC,
            tip = "Üç parmak aynı perdede yan yana. Klasik gitarın geniş sapında rahat sığar."),
        Chord("D", "Re majör", listOf(X, X, 0, 2, 3, 2), listOf(0, 0, 0, 1, 3, 2), ChordLevel.BASIC,
            tip = "Sadece ince 4 teli çal. Parmaklar üçgen şeklinde."),
        Chord("Dm", "Re minör", listOf(X, X, 0, 2, 3, 1), listOf(0, 0, 0, 2, 3, 1), ChordLevel.BASIC,
            tip = "D'ye benzer; 1. teldeki parmak bir perde geri (işaret parmağı) gelir."),
        Chord("C", "Do majör", listOf(X, 3, 2, 0, 1, 0), listOf(0, 3, 2, 0, 1, 0), ChordLevel.BASIC,
            tip = "Parmaklar merdiven gibi: 5. tel 3. perde, 4. tel 2. perde, 2. tel 1. perde."),
        Chord("G", "Sol majör", listOf(3, 2, 0, 0, 0, 3), listOf(2, 1, 0, 0, 0, 3), ChordLevel.BASIC,
            tip = "Tüm telleri çal. Ortadaki açık tellere parmakların değmemesine dikkat et."),
        Chord("E7", "Mi yedili", listOf(0, 2, 0, 1, 0, 0), listOf(0, 2, 0, 1, 0, 0), ChordLevel.SEVENTH,
            tip = "E akorundan yüzük parmağını kaldır."),
        Chord("A7", "La yedili", listOf(X, 0, 2, 0, 2, 0), listOf(0, 0, 2, 0, 3, 0), ChordLevel.SEVENTH,
            tip = "A akorunun ortadaki parmağını kaldır."),
        Chord("D7", "Re yedili", listOf(X, X, 0, 2, 1, 2), listOf(0, 0, 0, 2, 1, 3), ChordLevel.SEVENTH,
            tip = "D'nin ters çevrilmiş üçgeni gibi düşün."),
        Chord("G7", "Sol yedili", listOf(3, 2, 0, 0, 0, 1), listOf(3, 2, 0, 0, 0, 1), ChordLevel.SEVENTH,
            tip = "Genellikle C akoruna geçmeden önce çalınır."),
        Chord("B7", "Si yedili", listOf(X, 2, 1, 2, 0, 2), listOf(0, 2, 1, 3, 0, 4), ChordLevel.SEVENTH,
            tip = "Dört parmak gerekir; Em tonundaki şarkılarda çok kullanılır."),
        Chord("Am7", "La minör yedili", listOf(X, 0, 2, 0, 1, 0), listOf(0, 0, 2, 0, 1, 0), ChordLevel.SEVENTH,
            tip = "Am'den yüzük parmağını kaldır."),
        Chord("Em7", "Mi minör yedili", listOf(0, 2, 0, 0, 0, 0), listOf(0, 2, 0, 0, 0, 0), ChordLevel.SEVENTH,
            tip = "Tek parmakla çalınan yumuşak bir akor."),
        Chord("Fmaj7", "Fa majör yedili", listOf(X, X, 3, 2, 1, 0), listOf(0, 0, 3, 2, 1, 0), ChordLevel.SEVENTH,
            tip = "F akorunun kolay hali. Barre öğrenene kadar F yerine kullan."),
        Chord("Dm7", "Re minör yedili", listOf(X, X, 0, 2, 1, 1), listOf(0, 0, 0, 2, 1, 1), ChordLevel.SEVENTH,
            tip = "İşaret parmağı 1. perdede iki teli birden basar."),
        Chord("Cmaj7", "Do majör yedili", listOf(X, 3, 2, 0, 0, 0), listOf(0, 3, 2, 0, 0, 0), ChordLevel.SEVENTH,
            tip = "C akorundan işaret parmağını kaldır; yumuşak, caz tınılı bir ses."),
        Chord("Dmaj7", "Re majör yedili", listOf(X, X, 0, 2, 2, 2), listOf(0, 0, 0, 1, 2, 3), ChordLevel.SEVENTH),
        Chord("Amaj7", "La majör yedili", listOf(X, 0, 2, 1, 2, 0), listOf(0, 0, 2, 1, 3, 0), ChordLevel.SEVENTH),
        Chord("Gmaj7", "Sol majör yedili", listOf(3, 2, 0, 0, 0, 2), listOf(3, 2, 0, 0, 0, 1), ChordLevel.SEVENTH),
        Chord("Asus2", "La sus2", listOf(X, 0, 2, 2, 0, 0), listOf(0, 0, 1, 2, 0, 0), ChordLevel.SEVENTH),
        Chord("Asus4", "La sus4", listOf(X, 0, 2, 2, 3, 0), listOf(0, 0, 1, 2, 3, 0), ChordLevel.SEVENTH),
        Chord("Dsus2", "Re sus2", listOf(X, X, 0, 2, 3, 0), listOf(0, 0, 0, 1, 3, 0), ChordLevel.SEVENTH),
        Chord("Dsus4", "Re sus4", listOf(X, X, 0, 2, 3, 3), listOf(0, 0, 0, 1, 3, 4), ChordLevel.SEVENTH),
        Chord("Esus4", "Mi sus4", listOf(0, 2, 2, 2, 0, 0), listOf(0, 2, 3, 4, 0, 0), ChordLevel.SEVENTH),
        Chord("Cadd9", "Do add9", listOf(X, 3, 2, 0, 3, 0), listOf(0, 2, 1, 0, 3, 0), ChordLevel.SEVENTH),
        Chord("D/F#", "Re / Fa# bas", listOf(2, 0, 0, 2, 3, 2), listOf(1, 0, 0, 2, 4, 3), ChordLevel.SEVENTH,
            tip = "Bas notası Fa# olan D. Başparmakla 6. teli basan da olur."),
        Chord("G/B", "Sol / Si bas", listOf(X, 2, 0, 0, 0, 3), listOf(0, 1, 0, 0, 0, 3), ChordLevel.SEVENTH),
        Chord("C/G", "Do / Sol bas", listOf(3, 3, 2, 0, 1, 0), listOf(3, 4, 2, 0, 1, 0), ChordLevel.SEVENTH),
        Chord("Eb", "Mi bemol majör", listOf(X, X, 1, 3, 4, 3), listOf(0, 0, 1, 2, 4, 3), ChordLevel.SEVENTH),
        Chord("F", "Fa majör", listOf(1, 3, 3, 2, 1, 1), listOf(1, 3, 4, 2, 1, 1), ChordLevel.BARRE, barreFret = 1,
            tip = "İşaret parmağı 1. perdede tüm telleri basar. Başta zorlar; sabırla çalış."),
        Chord("Bm", "Si minör", listOf(X, 2, 4, 4, 3, 2), listOf(0, 1, 3, 4, 2, 1), ChordLevel.BARRE, barreFret = 2,
            tip = "2. perdede barre, Am şeklini 2 perde yukarı taşı gibi düşün. 6. teli çalma."),
        Chord("Fm", "Fa minör", listOf(1, 3, 3, 1, 1, 1), listOf(1, 3, 4, 1, 1, 1), ChordLevel.BARRE, barreFret = 1),
        Chord("Bb", "Si bemol majör", listOf(X, 1, 3, 3, 3, 1), listOf(0, 1, 2, 3, 4, 1), ChordLevel.BARRE, barreFret = 1),
        Chord("B", "Si majör", listOf(X, 2, 4, 4, 4, 2), listOf(0, 1, 2, 3, 4, 1), ChordLevel.BARRE, barreFret = 2),
        Chord("F#", "Fa diyez majör", listOf(2, 4, 4, 3, 2, 2), listOf(1, 3, 4, 2, 1, 1), ChordLevel.BARRE, barreFret = 2),
        Chord("F#m", "Fa diyez minör", listOf(2, 4, 4, 2, 2, 2), listOf(1, 3, 4, 1, 1, 1), ChordLevel.BARRE, barreFret = 2,
            tip = "Em şeklinin 2. perdeye barre ile taşınmış hali."),
        Chord("Gm", "Sol minör", listOf(3, 5, 5, 3, 3, 3), listOf(1, 3, 4, 1, 1, 1), ChordLevel.BARRE, barreFret = 3),
        Chord("Cm", "Do minör", listOf(X, 3, 5, 5, 4, 3), listOf(0, 1, 3, 4, 2, 1), ChordLevel.BARRE, barreFret = 3),
        Chord("C#m", "Do diyez minör", listOf(X, 4, 6, 6, 5, 4), listOf(0, 1, 3, 4, 2, 1), ChordLevel.BARRE, barreFret = 4),
        Chord("C#", "Do diyez majör", listOf(X, 4, 6, 6, 6, 4), listOf(0, 1, 2, 3, 4, 1), ChordLevel.BARRE, barreFret = 4),
        Chord("G#m", "Sol diyez minör", listOf(4, 6, 6, 4, 4, 4), listOf(1, 3, 4, 1, 1, 1), ChordLevel.BARRE, barreFret = 4),
        Chord("Ab", "La bemol majör", listOf(4, 6, 6, 5, 4, 4), listOf(1, 3, 4, 2, 1, 1), ChordLevel.BARRE, barreFret = 4),
    )

    private val byName = all.associateBy { it.name }

    private val enharmonic = mapOf(
        "Db" to "C#", "C#" to "Db", "D#" to "Eb", "Eb" to "D#", "Gb" to "F#", "F#" to "Gb",
        "G#" to "Ab", "Ab" to "G#", "A#" to "Bb", "Bb" to "A#",
    )

    /**
     * Akoru adına göre bulur. Yazım düzeltilir ("am" → Am), eş sesli adlar denenir (A# = Bb),
     * "D/F#" gibi bas notalı akor kütüphanede yoksa ana akor (D) döner.
     */
    fun get(name: String): Chord? {
        val n = ChordName.normalize(name) ?: name.trim()
        byName[n]?.let { return it }
        val m = Regex("^([A-G][#b]?)(.*)$").find(n) ?: return null
        val (root, rest) = m.destructured
        enharmonic[root]?.let { alt -> byName[alt + rest]?.let { return it } }
        if ("/" in n) return get(n.substringBefore('/'))
        return null
    }
}
