package com.ronix.learning

import java.time.LocalDate

/** Extracts the 11-char video id from watch, youtu.be and embed links. Returns null when invalid. */
fun youtubeId(url: String): String? =
    Regex("""(?:youtube\.com/(?:watch\?(?:.*&)?v=|embed/)|youtu\.be/)([A-Za-z0-9_-]{11})""")
        .find(url.trim())?.groupValues?.get(1)

/** Percentage 0..100 of lessons watched. */
fun progressPercent(watched: Int, total: Int): Int = if (total <= 0) 0 else (watched.coerceAtMost(total) * 100) / total

/** Consecutive learning days ending today (or yesterday). Days are ISO yyyy-MM-dd. */
fun streak(days: Collection<String>, today: LocalDate = LocalDate.now()): Int {
    val set = days.toSet()
    var d = if (today.toString() in set) today else today.minusDays(1)
    var n = 0
    while (d.toString() in set) { n++; d = d.minusDays(1) }
    return n
}
