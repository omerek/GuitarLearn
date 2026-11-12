package com.omerek.guitarlearn.data

/** Şarkı metnindeki bir satır. */
sealed interface SongLine {
    data class Header(val text: String) : SongLine
    data class Lyric(val segments: List<Segment>) : SongLine
    data object Blank : SongLine
}

/** Akorun üstünde durduğu söz parçası. Akor yoksa chord = null. */
data class Segment(val chord: String?, val text: String)

/**
 * Şarkı biçimi:
 *  - "# Nakarat" -> bölüm başlığı
 *  - "[Am]Üsküdar'a gider iken" -> akor, hemen sonraki hecenin üzerinde gösterilir
 *  - boş satır -> boşluk
 */
object SongParser {
    private val chordRegex = Regex("""\[([^\]]+)]""")

    fun parse(content: String): List<SongLine> = content.trimIndent().lines().map { raw ->
        val line = raw.trimEnd()
        when {
            line.isBlank() -> SongLine.Blank
            line.startsWith("#") -> SongLine.Header(line.removePrefix("#").trim())
            else -> SongLine.Lyric(parseSegments(line))
        }
    }

    fun parseSegments(line: String): List<Segment> {
        val segments = mutableListOf<Segment>()
        var last = 0
        var pendingChord: String? = null
        for (m in chordRegex.findAll(line)) {
            val text = line.substring(last, m.range.first)
            if (text.isNotEmpty() || pendingChord != null) segments += Segment(pendingChord, text)
            pendingChord = m.groupValues[1].trim()
            last = m.range.last + 1
        }
        val tail = line.substring(last)
        if (tail.isNotEmpty() || pendingChord != null) segments += Segment(pendingChord, tail)
        return segments
    }

    /** Şarkıda geçen akorlar, ilk geçiş sırasıyla. */
    fun chordsIn(content: String): List<String> =
        chordRegex.findAll(content).map { it.groupValues[1].trim() }.distinct().toList()
}
