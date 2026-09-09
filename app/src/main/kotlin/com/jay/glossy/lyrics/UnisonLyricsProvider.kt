package com.jay.glossy.lyrics

import android.content.Context
import com.jay.glossy.unison.Unison
import com.jay.glossy.constants.EnableUnisonKey
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get

object UnisonLyricsProvider : LyricsProvider {
    override val name = "Unison"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableUnisonKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = Unison.getLyrics(
        title = title,
        artist = artist,
        duration = duration,
        album = album,
        videoId = id.takeIf { it.isNotBlank() },
    )

    override suspend fun getAllLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        Unison.getAllLyrics(
            title = title,
            artist = artist,
            duration = duration,
            album = album,
            videoId = id.takeIf { it.isNotBlank() },
            callback = callback,
        )
    }
}
