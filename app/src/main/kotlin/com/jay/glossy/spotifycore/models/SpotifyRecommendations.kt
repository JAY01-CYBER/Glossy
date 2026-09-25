package com.jay.glossy.spotifycore.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyRecommendations(
    val tracks: List<SpotifyTrack> = emptyList()
)
