/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.upcoming

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

/**
 * An announced release that is not out yet.
 *
 * YouTube Music does not expose pre-release albums to third-party clients, so
 * this model is filled from MusicBrainz instead. [trackCount] is optional
 * because the release calendar does not always know how many tracks a record
 * will carry.
 */
data class UpcomingRelease(
    val musicBrainzId: String,
    val title: String,
    val artistName: String,
    val releaseDate: LocalDate,
    val type: ReleaseType,
    val trackCount: Int?,
) {
    /**
     * Cover Art Archive address for this release group.
     *
     * The archive answers with a redirect when the artwork exists and a 404
     * when it does not, so the card keeps the artist image underneath this
     * image and lets the archive cover it when it loads.
     */
    val coverArtUrl: String
        get() = "https://coverartarchive.org/release-group/$musicBrainzId/front-500"
}

enum class ReleaseType {
    ALBUM,
    EP,
    SINGLE,
}

private val DIACRITICS = Regex("\\p{Mn}+")
private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
private val TOPIC_SUFFIX = Regex("""\s*-\s*topic\s*$""", RegexOption.IGNORE_CASE)
private val VEVO_SUFFIX = Regex("""\s*vevo\s*$""", RegexOption.IGNORE_CASE)

/**
 * A form that lets two provider spellings of the same name meet: case,
 * diacritics, punctuation, ampersands and spacing are all ignored.
 */
internal fun normalizeForComparison(value: String): String =
    Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)
        .replace("&", " and ")
        .replace(NON_ALPHANUMERIC, " ")
        .trim()

/**
 * Strips the channel decorations YouTube adds to auto-generated artist pages
 * before comparing the name with MusicBrainz.
 */
internal fun normalizeArtistName(value: String): String =
    normalizeForComparison(
        value
            .replace(TOPIC_SUFFIX, "")
            .replace(VEVO_SUFFIX, ""),
    )
