package com.jay.glossy.spotifycore.models

import kotlinx.serialization.Serializable

@Serializable
sealed class SpotifyLibraryItem {
    @Serializable
    data class Playlist(val playlist: SpotifyPlaylist) : SpotifyLibraryItem()
    
    @Serializable
    data class Folder(val folder: SpotifyLibraryFolder) : SpotifyLibraryItem()
}

@Serializable
data class SpotifyLibraryFolder(
    val uri: String,
    val name: String,
    val totalChildren: Int
)
