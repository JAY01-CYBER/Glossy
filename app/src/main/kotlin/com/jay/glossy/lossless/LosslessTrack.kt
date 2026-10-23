/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lossless

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * One lossless file on the device, as the player needs to know it.
 *
 * Only what the lane actually uses: where the audio is, what to match it
 * against, and the three numbers that decide whether the output can carry it
 * without being resampled. Everything else about the file stays on disk.
 */
@Serializable
data class LosslessTrack(
    /** A `content://` URI from the picked folder's tree grant. */
    val uri: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    /** Length of the audio, used to tell a remaster from a different edit. */
    val durationMs: Long = 0L,
    /** 44100, 96000 …; 0 when the file does not say. */
    val sampleRateHz: Int = 0,
    /** 16, 24 …; 0 when the file does not say. */
    val bitDepth: Int = 0,
    /** FLAC or WAV — the containers the lane opens. */
    val codec: String = "",
) {
    /** `FLAC · 24-bit · 96 kHz`, the line settings and the player show. */
    val badge: String
        get() =
            listOfNotNull(
                codec.takeIf { it.isNotBlank() },
                bitDepth.takeIf { it > 0 }?.let { "$it-bit" },
                sampleRateHz.takeIf { it > 0 }?.let { String.format(Locale.ROOT, "%.1f kHz", it / 1000f) },
            ).joinToString(" · ")
}

/**
 * How a title from YouTube and a title from a file are compared.
 *
 * YouTube names carry the things a rip does not — "(Official Video)", "[HD]",
 * "feat. …" — and two files of the same song rarely agree on punctuation, so
 * everything that is not a letter or a digit goes, along with the handful of
 * words that only ever describe the upload rather than the recording.
 */
object LosslessKey {
    private val noise =
        setOf(
            "official", "video", "music", "lyrics", "lyric", "audio", "visualizer",
            "hd", "hq", "4k", "mv", "m", "v", "remaster", "remastered", "explicit",
        )

    fun normalise(raw: String): String =
        raw
            .lowercase(Locale.ROOT)
            .replace('’', '\'')
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() && it !in noise }
            .joinToString(" ")

    /** The pair that has to agree for a file to stand in for a stream. */
    fun pair(title: String, artist: String): String =
        normalise(title) + "|" + normalise(artist)
}
