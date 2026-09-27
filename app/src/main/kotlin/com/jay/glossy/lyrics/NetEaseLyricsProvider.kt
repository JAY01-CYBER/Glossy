package com.jay.glossy.lyrics

import android.content.Context
import com.jay.glossy.constants.EnableNetEaseKey
import com.jay.glossy.netease.NetEase
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get

object NetEaseLyricsProvider : LyricsProvider {
    override val name: String = "NetEase"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableNetEaseKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = NetEase.getLyrics(title = title, artist = artist, duration = duration)

    override suspend fun getAllLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        NetEase.getAllLyrics(title = title, artist = artist, duration = duration, callback = callback)
    }
}
