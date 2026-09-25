package com.jay.glossy.spotifycore.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPlaylist(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    val owner: SpotifyPlaylistOwner? = null,
    val tracks: SpotifyPlaylistTracksRef? = null,
    val collaborative: Boolean = false,
    val uri: String? = null
)

@Serializable
data class SpotifyPlaylistOwner(
    val id: String = "",
    @SerialName("display_name") val displayName: String? = null,
    val uri: String? = null
)

@Serializable
data class SpotifyPlaylistTracksRef(
    val total: Int? = null,
    val href: String? = null
)

@Serializable
data class SpotifyPlaylistTrack(
    val track: SpotifyTrack? = null,
    val uid: String? = null
)
