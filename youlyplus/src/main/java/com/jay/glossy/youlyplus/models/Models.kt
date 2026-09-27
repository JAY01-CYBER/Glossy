package com.jay.glossy.youlyplus.models

import kotlinx.serialization.Serializable

@Serializable
data class LyricsResponse(
    val id: Int? = null,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    val lyrics: List<LyricsItem>? = null,
    val type: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
)

@Serializable
data class LyricsItem(
    val text: String? = null,
    val time: Long? = null,         // milliseconds
    val duration: Long? = null,     // milliseconds
    val syllabus: List<Syllable>? = null,
)

@Serializable
data class Syllable(
    val text: String? = null,
    val time: Long? = null,         // milliseconds
    val duration: Long? = null,     // milliseconds
    val isBackground: Boolean? = null,
)
