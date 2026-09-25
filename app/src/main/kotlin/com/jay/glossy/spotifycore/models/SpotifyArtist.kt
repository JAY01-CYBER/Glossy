package com.jay.glossy.spotifycore.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyArtist(
    val id: String = "",
    val name: String = "",
    val images: List<SpotifyImage> = emptyList(),
    val uri: String? = null
)

@Serializable
data class SpotifySimpleArtist(
    val id: String? = null,
    val name: String = "",
    val uri: String? = null
)
