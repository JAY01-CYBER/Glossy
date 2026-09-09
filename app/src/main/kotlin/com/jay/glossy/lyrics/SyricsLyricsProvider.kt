package com.jay.glossy.lyrics

import android.content.Context
import com.jay.glossy.constants.EnableSyricsKey
import com.jay.glossy.syrics.Syrics
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get

object SyricsLyricsProvider : LyricsProvider {
    override val name: String = "Syrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableSyricsKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = Syrics.getLyrics(title = title, artist = artist, duration = duration)

    override suspend fun getAllLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        Syrics.getAllLyrics(title = title, artist = artist, duration = duration, callback = callback)
    }
}
