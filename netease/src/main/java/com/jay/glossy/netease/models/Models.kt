package com.jay.glossy.netease.models

import kotlinx.serialization.Serializable

@Serializable
data class NeteaseSearchResponse(
    val result: NeteaseSearchResult? = null,
    val code: Int? = null
)

@Serializable
data class NeteaseSearchResult(
    val songs: List<NeteaseSong> = emptyList()
)

@Serializable
data class NeteaseSong(
    val id: Long? = null,
    val name: String? = null
)

@Serializable
data class NeteaseLyricsResponse(
    val lrc: NeteaseLyricData? = null,
    val klyric: NeteaseLyricData? = null,
    val yrc: NeteaseLyricData? = null,
    val code: Int? = null
)

@Serializable
data class NeteaseLyricData(
    val lyric: String? = null
)
