/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lyrics

import com.jay.glossy.R

<<<<<<< HEAD
/**
 * Whether raw lyrics text can actually be followed, that is, whether
 * [LyricsUtils.parseLyrics] finds at least one timed line in it.
 *
 * This used to be a pair of looser hints (`\[\d{1,2}:\d{2` and `<mm:ss.xx>`)
 * sitting next to the parser's much stricter line pattern. Text the hint called
 * synced — `[0:12.34]`, `[00:12]`, or a whole song glued into one physical line
 * — parsed to zero timed entries, so the player announced "Lyrics aren't
 * time-synced" for a file it had itself just classified as timed, and the lines
 * never followed the song. Asking the parser is the only answer that cannot
 * drift from parsing again.
 */
fun lyricsTextLooksSynced(lyrics: String?): Boolean {
    if (lyrics.isNullOrBlank()) return false
    return LyricsUtils.parseLyrics(lyrics).isNotEmpty()
=======
private val LRC_TIMESTAMP_HINT = Regex("""\[\d{1,2}:\d{2}""")

/**
 * Whether raw lyrics text appears to be time-synced (LRC-style), including when a BOM or
 * leading blank lines precede the first `[mm:ss.xx]` tag.
 */
fun lyricsTextLooksSynced(lyrics: String?): Boolean {
    if (lyrics.isNullOrBlank()) return false
    val t = lyrics.trim().removePrefix("\uFEFF").trimStart()
    if (t.startsWith('[')) return true
    return LRC_TIMESTAMP_HINT.containsMatchIn(t.take(4096))
>>>>>>> origin/main
}
