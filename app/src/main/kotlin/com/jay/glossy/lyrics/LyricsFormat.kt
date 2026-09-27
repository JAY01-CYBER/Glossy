/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lyrics

import com.jay.glossy.R

private val LRC_TIMESTAMP_HINT = Regex("""\[\d{1,2}:\d{2}""")

/**
 * Word-level tags (`<mm:ss.xx>`). Apple Music-style syllable files and a few
 * providers ship only these, with no line-level `[mm:ss.xx]` stamp; they are
 * just as followable as LRC, so they must not be classified as plain text.
 */
private val WORD_TIMESTAMP_HINT = Regex("""<\d{1,2}:\d{2}\.\d{2,3}>""")

/**
 * Whether raw lyrics text appears to be time-synced (LRC-style), including when a BOM or
 * leading blank lines precede the first `[mm:ss.xx]` tag.
 *
 * A real `[mm:ss]` timestamp is the only reliable signal. Text that merely starts with
 * `[` used to be accepted as well, which quietly sent plain lyrics down the timed path in
 * the player: "[Verse 1]"-style section headers parsed into no timed lines, so the lyrics
 * strip and the lyrics screen had nothing to advance to and stayed stuck on the opening
 * lines instead of showing the plain text.
 */
fun lyricsTextLooksSynced(lyrics: String?): Boolean {
    if (lyrics.isNullOrBlank()) return false
    val t = lyrics.trim().removePrefix("\uFEFF").trimStart()
    return LRC_TIMESTAMP_HINT.containsMatchIn(t) || WORD_TIMESTAMP_HINT.containsMatchIn(t)
}
