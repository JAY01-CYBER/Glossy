package com.jay.glossy.lyrics

import android.content.Context
import com.jay.glossy.youlyplus.YouLyPlus
import com.jay.glossy.constants.EnableYouLyPlusKey // <-- New Key
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get

object YouLyPlusLyricsProvider : LyricsProvider {
    override val name = "YouLyPlus" // <-- New Name

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableYouLyPlusKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = YouLyPlus.getLyrics(title, artist, duration, album, id)

    override suspend fun getAllLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        YouLyPlus.getAllLyrics(title, artist, duration, album, id, null, callback)
    }
}
