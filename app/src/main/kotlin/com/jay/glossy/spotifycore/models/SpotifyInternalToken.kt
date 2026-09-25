package com.jay.glossy.spotifycore.models

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyInternalToken(
    val accessToken: String,
    val accessTokenExpirationTimestampMs: Long,
    val isAnonymous: Boolean = false
)
