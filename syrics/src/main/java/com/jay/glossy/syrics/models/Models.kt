package com.jay.glossy.syrics.models

import kotlinx.serialization.Serializable

@Serializable
data class SyricsSearchResponse(
    val tracks: List<SyricsTrack> = emptyList()
)

@Serializable
data class SyricsTrack(
    val id: String? = null,
    val name: String? = null,
    val artist: String? = null
)

@Serializable
data class SyricsLyricsResponse(
    val error: Boolean? = null,
    val syncType: String? = null,
    val lines: List<SyricsLine> = emptyList()
)

@Serializable
data class SyricsLine(
    val startTimeMs: String? = null,
    val words: String? = null,
    val syllables: List<SyricsSyllable> = emptyList()
)

@Serializable
data class SyricsSyllable(
    val startTimeMs: String? = null,
    val numChars: Int? = null,
    val chars: String? = null
)
