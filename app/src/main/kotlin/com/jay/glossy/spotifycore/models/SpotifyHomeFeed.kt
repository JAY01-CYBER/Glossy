package com.jay.glossy.spotifycore.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyHomeFeed(
    val greeting: String? = null,
    val sections: List<SpotifyHomeFeedSection> = emptyList()
)

@Serializable
data class SpotifyHomeFeedSection(
    val sectionUri: String,
    val title: String?,
    val typename: String,
    val totalCount: Int,
    val items: List<SpotifyHomeFeedItem>
)

@Serializable
sealed class SpotifyHomeFeedItem {
    @Serializable
    data class Playlist(
        val uri: String,
        val id: String,
        val name: String,
        val description: String?,
        val format: String?,
        val totalCount: Int,
        val imageUrl: String?,
        val extractedColorHex: String?,
        val ownerName: String?,
        val madeForUsername: String?
    ) : SpotifyHomeFeedItem()

    @Serializable
    data class Album(
        val uri: String,
        val id: String,
        val name: String,
        val albumType: String?,
        val artists: List<SpotifySimpleArtist>,
        val imageUrl: String?
    ) : SpotifyHomeFeedItem()

    @Serializable
    data class Artist(
        val uri: String,
        val id: String,
        val name: String,
        val imageUrl: String?
    ) : SpotifyHomeFeedItem()
}
