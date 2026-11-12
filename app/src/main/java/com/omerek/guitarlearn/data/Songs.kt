package com.omerek.guitarlearn.data

/**
 * Şarkılar. Yalnızca anonim / kamu malı sözler ve uygulama için yazılmış özgün etütler kullanılır.
 * Akor düzenlemeleri başlangıç seviyesi için sadeleştirilmiştir.
 */
data class Song(
    val id: String,
    val title: String,
    val source: String,
    val level: Int,
    val bpm: Int,
    val beatsPerBar: Int,
    val strumPattern: String,
    val content: String,
    val note: String = "",
) {
    val chords: List<String> get() = SongParser.chordsIn(content)
}

object Songs {
    val all: List<Song> = listOf(
        Song(
            id = "etut-em-am",
            title = "İlk Etüt: Em – Am",
            source = "Özgün alıştırma",
            level = 1,
            bpm = 60,
            beatsPerBar = 4,
            strumPattern = "D-D-D-D-",
            note = "Her akorda 4 aşağı vuruş yap. Akor değişirken ritmi bozma; geç kalırsan bile vuruşa devam et.",
            content = """
                # Bölüm 1
                [Em]1 2 3 4 [Em]1 2 3 4 [Am]1 2 3 4 [Am]1 2 3 4
                [Em]1 2 3 4 [Em]1 2 3 4 [Am]1 2 3 4 [Am]1 2 3 4

                # Bölüm 2 (daha sık değişim)
                [Em]1 2 3 4 [Am]1 2 3 4 [Em]1 2 3 4 [Am]1 2 3 4
                [Em]1 2 3 4 [Am]1 2 3 4 [Em]1 2 3 4
            """,
        ),
        Song(
            id = "etut-e-a-d",
            title = "Etüt: E – A – D",
            source = "Özgün alıştırma",
            level = 2,
            bpm = 70,
            beatsPerBar = 4,
            strumPattern = "D-D-D-DU",
            content = """
                # Ana tema
                [A]1 2 3 4 [D]1 2 3 4 [A]1 2 3 4 [E]1 2 3 4
                [A]1 2 3 4 [D]1 2 3 4 [E]1 2 3 4 [A]1 2 3 4

                # Köprü
                [D]1 2 3 4 [D]1 2 3 4 [A]1 2 3 4 [A]1 2 3 4
                [E]1 2 3 4 [D]1 2 3 4 [A]1 2 3 4
            """,
        ),
        Song(
            id = "uskudar",
            title = "Üsküdar'a Gider İken",
            source = "Anonim türkü",
            level = 2,
            bpm = 80,
            beatsPerBar = 2,
            strumPattern = "D-DU",
            note = "Basit eşlik düzenlemesi. Önce sadece akor değişimlerine odaklan, sonra söyleyerek çal.",
            content = """
                # 1. kıta
                [Am]Üsküdar'a gider iken [Dm]aldı da bir yağ[Am]mur
                [Am]Kâtibimin setresi uzun, [E]eteği ça[Am]mur
                [Am]Kâtip uykudan uyanmış, [Dm]gözleri mah[Am]mur

                # Nakarat
                [E]Kâtip benim, ben kâtibin, [Am]el ne karışır
                [Dm]Kâtibime kolalı da [E]gömlek ne ya[Am]raşır
            """,
        ),
        Song(
            id = "kumbaya",
            title = "Kumbaya",
            source = "Geleneksel halk şarkısı",
            level = 3,
            bpm = 70,
            beatsPerBar = 4,
            strumPattern = "D-D-DUDU",
            content = """
                # Nakarat
                [G]Kum ba yah, my Lord, [C]kum ba [G]yah
                [G]Kum ba yah, my Lord, [D]kum ba [D]yah
                [G]Kum ba yah, my Lord, [C]kum ba [G]yah
                Oh [C]Lord, [G]kum ba [D]yah [G]

                # Kıta
                [G]Someone's singing, Lord, [C]kum ba [G]yah
                [G]Someone's singing, Lord, [D]kum ba [D]yah
                [G]Someone's singing, Lord, [C]kum ba [G]yah
                Oh [C]Lord, [G]kum ba [D]yah [G]
            """,
        ),
        Song(
            id = "iyi-ki-dogdun",
            title = "İyi ki Doğdun",
            source = "Geleneksel doğum günü şarkısı",
            level = 3,
            bpm = 90,
            beatsPerBar = 3,
            strumPattern = "D-D-D-",
            note = "3/4 ölçü: \"bir-iki-üç\" diye say, her ölçünün ilk vuruşunu biraz daha güçlü çal.",
            content = """
                [G]İyi ki doğdun [D7]sen
                [D7]İyi ki doğdun [G]sen
                [G]İyi ki doğdun [C]sevgili dostum
                [G]İyi ki [D7]doğdun [G]sen
            """,
        ),
        Song(
            id = "amazing-grace",
            title = "Amazing Grace",
            source = "Geleneksel ilahi (J. Newton, 1779)",
            level = 3,
            bpm = 80,
            beatsPerBar = 3,
            strumPattern = "D-DUDU",
            content = """
                # 1. kıta
                A[G]mazing grace, how [G7]sweet the [C]sound
                That [G]saved a wretch like [D]me
                I [G]once was lost, but [G7]now am [C]found
                Was [G]blind, but [D]now I [G]see

                # 2. kıta
                'Twas [G]grace that taught my [G7]heart to [C]fear
                And [G]grace my fears re[D]lieved
                How [G]precious did that [G7]grace ap[C]pear
                The [G]hour I [D]first be[G]lieved
            """,
        ),
        Song(
            id = "oh-susanna",
            title = "Oh! Susanna",
            source = "S. Foster, 1848 (kamu malı)",
            level = 3,
            bpm = 100,
            beatsPerBar = 4,
            strumPattern = "D-DUD-DU",
            content = """
                # Kıta
                I [G]come from Alabama with my [D]banjo on my knee
                I'm [G]going to Louisiana, my [D]true love for to [G]see

                # Nakarat
                Oh, Su[C]sanna, oh [G]don't you cry for [D]me
                For I [G]come from Alabama with my [D]banjo on my [G]knee
            """,
        ),
        Song(
            id = "aksam-etudu",
            title = "Akşam Etüdü: Am – Fmaj7 – C – G",
            source = "Özgün alıştırma",
            level = 4,
            bpm = 72,
            beatsPerBar = 4,
            strumPattern = "D-DU-UDU",
            note = "Popüler müzikte en çok kullanılan dizilerden biri. Fmaj7 rahatladıkça F barre ile dene. Arpej (p-i-m-a) ile de çalabilirsin.",
            content = """
                # Tema
                [Am]1 2 3 4 [Fmaj7]1 2 3 4 [C]1 2 3 4 [G]1 2 3 4
                [Am]1 2 3 4 [Fmaj7]1 2 3 4 [C]1 2 3 4 [G]1 2 3 4

                # Barre ile
                [Am]1 2 3 4 [F]1 2 3 4 [C]1 2 3 4 [G]1 2 3 4
                [Am]1 2 3 4 [F]1 2 3 4 [G]1 2 3 4 [Am]1 2 3 4
            """,
        ),
        Song(
            id = "canakkale",
            title = "Çanakkale Türküsü",
            source = "Anonim türkü",
            level = 4,
            bpm = 66,
            beatsPerBar = 4,
            strumPattern = "D-D-DUDU",
            note = "Ağır ve duygulu çalınır. B7 için önce tek başına geçişi çalış: Em → B7 → Em.",
            content = """
                # 1. kıta
                [Em]Çanakkale içinde [Am]aynalı çar[Em]şı
                [Em]Ana ben gidiyorum [B7]düşmana kar[Em]şı
                [D]Of gençli[Em]ğim [B7]ey[Em]vah

                # 2. kıta
                [Em]Çanakkale içinde [Am]vurdular be[Em]ni
                [Em]Ölmeden mezara [B7]koydular be[Em]ni
                [D]Of gençli[Em]ğim [B7]ey[Em]vah
            """,
        ),
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Song? = byId[id]
}
