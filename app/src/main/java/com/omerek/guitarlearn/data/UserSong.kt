package com.omerek.guitarlearn.data

/** Kullanıcının kendi girdiği şarkı. Yalnızca telefonda saklanır. */
data class UserSong(
    val id: String,
    val title: String,
    val artist: String = "",
    val bpm: Int = 80,
    val beatsPerBar: Int = 4,
    /** Kullanıcının "A, G, Em" diye girdiği akor paleti. */
    val chords: List<String> = emptyList(),
    /** "[Am]söz" biçiminde sözler ve akorlar. */
    val content: String = "",
    val strumPattern: String = "D-DU-UDU",
    val updatedAt: Long = 0L,
) {
    /** Şarkı ekranında gösterilebilen ortak biçim. */
    fun toSong(): Song = Song(
        id = ID_PREFIX + id,
        title = title.ifBlank { "Adsız şarkı" },
        source = artist.ifBlank { "Benim şarkım" },
        level = 0,
        bpm = bpm,
        beatsPerBar = beatsPerBar,
        strumPattern = strumPattern,
        content = content,
    )

    /** Palette olup henüz sözlere yerleştirilmemiş akorlar dahil, şarkıdaki tüm akorlar. */
    val allChords: List<String> get() = (chords + SongParser.chordsIn(content)).distinct()

    companion object {
        const val ID_PREFIX = "user:"
    }
}

/**
 * Yedek dosyası biçimi (JSON). Android'e bağımlı olmasın ve test edilebilsin diye küçük bir
 * JSON yazıcı/okuyucu kullanılır.
 */
object UserSongBackup {
    const val FORMAT = "gitar-ogren-sarkilar"
    const val VERSION = 1

    fun encode(songs: List<UserSong>): String = buildString {
        append("{\n  \"format\": ").append(q(FORMAT)).append(",\n  \"version\": ").append(VERSION).append(",\n  \"songs\": [")
        songs.forEachIndexed { i, s ->
            if (i > 0) append(",")
            append("\n    {")
            append("\"id\": ").append(q(s.id))
            append(", \"title\": ").append(q(s.title))
            append(", \"artist\": ").append(q(s.artist))
            append(", \"bpm\": ").append(s.bpm)
            append(", \"beatsPerBar\": ").append(s.beatsPerBar)
            append(", \"chords\": [").append(s.chords.joinToString(", ") { q(it) }).append("]")
            append(", \"strumPattern\": ").append(q(s.strumPattern))
            append(", \"updatedAt\": ").append(s.updatedAt)
            append(", \"content\": ").append(q(s.content))
            append("}")
        }
        append("\n  ]\n}\n")
    }

    /** Yedek dosyasını okur. Biçim tanınmazsa IllegalArgumentException fırlatır. */
    fun decode(text: String): List<UserSong> {
        val root = Json(text).parse() as? Map<*, *> ?: throw IllegalArgumentException("Yedek dosyası okunamadı")
        if (root["format"] != FORMAT) throw IllegalArgumentException("Bu dosya bir Gitar Öğren yedeği değil")
        val list = root["songs"] as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            UserSong(
                id = m["id"] as? String ?: return@mapNotNull null,
                title = m["title"] as? String ?: "",
                artist = m["artist"] as? String ?: "",
                bpm = (m["bpm"] as? Number)?.toInt() ?: 80,
                beatsPerBar = (m["beatsPerBar"] as? Number)?.toInt() ?: 4,
                chords = (m["chords"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                content = m["content"] as? String ?: "",
                strumPattern = m["strumPattern"] as? String ?: "D-DU-UDU",
                updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: 0L,
            )
        }
    }

    private fun q(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
        append('"')
    }

    /** Küçük, kendi içinde yeterli bir JSON okuyucu (nesne, dizi, metin, sayı, true/false/null). */
    private class Json(private val s: String) {
        private var i = 0

        fun parse(): Any? {
            val v = value()
            ws()
            if (i != s.length) error("Fazladan karakter")
            return v
        }

        private fun error(msg: String): Nothing = throw IllegalArgumentException("Yedek dosyası bozuk: $msg (konum $i)")

        private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }

        private fun value(): Any? {
            ws()
            if (i >= s.length) error("Beklenmeyen son")
            return when (s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> num()
            }
        }

        private fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) error("Tanınmayan değer")
            i += word.length
            return v
        }

        private fun num(): Number {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            val t = s.substring(start, i)
            return t.toLongOrNull() ?: t.toDoubleOrNull() ?: error("Sayı beklendi")
        }

        private fun str(): String {
            i++ // "
            val sb = StringBuilder()
            while (true) {
                if (i >= s.length) error("Kapanmayan metin")
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val e = s[i++]
                        when (e) {
                            'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                            'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun arr(): List<Any?> {
            i++
            val out = mutableListOf<Any?>()
            ws()
            if (s[i] == ']') { i++; return out }
            while (true) {
                out += value()
                ws()
                when (s.getOrNull(i++)) {
                    ',' -> continue
                    ']' -> return out
                    else -> error("',' ya da ']' beklendi")
                }
            }
        }

        private fun obj(): Map<String, Any?> {
            i++
            val out = linkedMapOf<String, Any?>()
            ws()
            if (s[i] == '}') { i++; return out }
            while (true) {
                ws()
                if (s.getOrNull(i) != '"') error("Anahtar beklendi")
                val k = str()
                ws()
                if (s.getOrNull(i++) != ':') error("':' beklendi")
                out[k] = value()
                ws()
                when (s.getOrNull(i++)) {
                    ',' -> continue
                    '}' -> return out
                    else -> error("',' ya da '}' beklendi")
                }
            }
        }
    }
}
