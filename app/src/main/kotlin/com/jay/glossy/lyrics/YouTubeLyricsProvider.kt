/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lyrics

import com.jay.glossy.R

import android.content.Context
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
<<<<<<< HEAD
import timber.log.Timber
=======
>>>>>>> origin/main

object YouTubeLyricsProvider : LyricsProvider {
    override val name = "YouTube Music"

    override fun isEnabled(context: Context) = true

<<<<<<< HEAD
    private val stampRegex = Regex("""\[\d{1,2}:\d{2}(?:[.:]\d{1,3})?\]""")

    private var loggedRawPayload = false

    /**
     * One short log per process of what this endpoint actually returns. Timed
     * lyrics from here are shaped `[mm:ss.xx] text`; when a song that is timed in
     * YouTube Music still arrives unsynced, this line says whether the app was
     * handed timestamps at all and in which shape, instead of leaving it to
     * guesswork.
     */
    private fun logRawPayloadOnce(lyrics: String) {
        if (loggedRawPayload) return
        loggedRawPayload = true
        Timber.tag("YouTubeLyrics")
            .i(
                "raw payload: stamps=%d head=%s",
                stampRegex.findAll(lyrics).count(),
                lyrics.take(240).replace("\n", "\\n"),
            )
    }

=======
>>>>>>> origin/main
    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val nextResult = YouTube.next(WatchEndpoint(videoId = id)).getOrThrow()
<<<<<<< HEAD
            val lyrics =
=======
            Result.success(
>>>>>>> origin/main
                YouTube
                    .lyrics(
                        endpoint = nextResult.lyricsEndpoint
                            ?: throw IllegalStateException("Lyrics endpoint not found"),
                    ).getOrThrow() ?: throw IllegalStateException("Lyrics unavailable")
<<<<<<< HEAD
            logRawPayloadOnce(lyrics)
            Result.success(lyrics)
=======
            )
>>>>>>> origin/main
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
