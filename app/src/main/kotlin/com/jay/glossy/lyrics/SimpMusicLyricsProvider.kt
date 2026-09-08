package com.jay.glossy.lyrics

import android.content.Context
import com.jay.glossy.constants.EnableSimpMusicKey
import com.jay.glossy.simpmusic.SimpMusic
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get

object SimpMusicLyricsProvider : LyricsProvider {
    override val name: String = "SimpMusic"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableSimpMusicKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = SimpMusic.getLyrics(
        videoId = id, 
        title = title, 
        artist = artist, 
        duration = duration
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
        SimpMusic.getAllLyrics(
            videoId = id, 
            title = title, 
            artist = artist, 
            duration = duration, 
            callback = callback
        )
    }
}
