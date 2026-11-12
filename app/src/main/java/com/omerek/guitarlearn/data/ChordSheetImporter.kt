package com.omerek.guitarlearn.data

/**
 * İnternette sık görülen "akor satırı üstte, söz satırı altta" metnini uygulamanın
 * "[Am]söz" biçimine çevirir. Örnek:
 *
 *     Am            Dm
 *     Üsküdar'a gider iken
 *
 * → "[Am]Üsküdar'a gider [Dm]iken"
 */
object ChordSheetImporter {
    private val barTokens = setOf("|", "||", "-", "/", "%", "x2", "x3", "x4", "(x2)", "(x3)", "(x4)", "2x", "3x", "4x")
    private val sectionWords = listOf(
        "intro", "verse", "chorus", "bridge", "outro", "pre-chorus", "solo", "interlude", "ending",
        "giriş", "kıta", "nakarat", "köprü", "bitiş", "ara", "ara müzik", "bölüm",
    )

    /** Satır yalnızca akorlardan (ve ölçü çizgilerinden) oluşuyorsa true. */
    fun isChordLine(line: String): Boolean {
        val tokens = line.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return false
        var chords = 0
        for (t in tokens) {
            when {
                t.lowercase() in barTokens -> {}
                ChordName.normalize(t.trim('(', ')', '|')) != null -> chords++
                else -> return false
            }
        }
        return chords > 0
    }

    /** Metinde en az bir "akor satırı" var mı (otomatik dönüştürme önerilsin mi)? */
    fun looksLikeChordSheet(text: String): Boolean =
        text.lines().any { it.isNotBlank() && '[' !in it && isChordLine(it) }

    private fun sectionHeader(line: String): String? {
        val t = line.trim()
        val inner = when {
            t.startsWith("[") && t.endsWith("]") -> t.substring(1, t.length - 1)
            t.endsWith(":") -> t.dropLast(1)
            else -> return null
        }.trim()
        if (inner.isEmpty() || ChordName.normalize(inner) != null) return null
        val lower = inner.lowercase()
        return if (sectionWords.any { lower.startsWith(it) } || t.startsWith("[")) inner else null
    }

    fun convert(text: String): String {
        val lines = text.replace("\t", "    ").lines()
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trimEnd()
            val header = sectionHeader(line)
            when {
                header != null -> out += "# $header"
                line.isBlank() -> out += ""
                '[' !in line && isChordLine(line) -> {
                    val next = lines.getOrNull(i + 1)?.trimEnd()
                    val nextIsLyric = next != null && next.isNotBlank() && !isChordLine(next) && sectionHeader(next) == null
                    if (nextIsLyric) {
                        out += merge(line, next!!)
                        i++
                    } else {
                        out += chordPositions(line).joinToString(" ") { "[${it.second}]" }
                    }
                }
                else -> out += line
            }
            i++
        }
        return out.joinToString("\n").trim('\n')
    }

    /** Akor satırındaki her akorun sütunu ve düzgün adı. */
    private fun chordPositions(chordLine: String): List<Pair<Int, String>> =
        Regex("""\S+""").findAll(chordLine).mapNotNull { m ->
            ChordName.normalize(m.value.trim('(', ')', '|'))?.let { m.range.first to it }
        }.toList()

    private fun merge(chordLine: String, lyric: String): String {
        val sb = StringBuilder(lyric)
        val positions = chordPositions(chordLine)
        // Sağdan sola ekle ki önceki sütunlar kaymasın
        for ((col, chord) in positions.reversed()) {
            if (col > sb.length) {
                sb.append(" ".repeat(col - sb.length))
            }
            sb.insert(col, "[$chord]")
        }
        return sb.toString().trimEnd()
    }
}
