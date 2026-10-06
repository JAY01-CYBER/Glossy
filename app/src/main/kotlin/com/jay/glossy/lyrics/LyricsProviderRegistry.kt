/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lyrics

import com.jay.glossy.R
import java.util.Locale

object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "Spotify" to SpotifyLyricsProvider,
        "BetterLyrics" to BetterLyricsProvider,
        "Paxsenix" to PaxsenixLyricsProvider,
        "LrcLib" to LrcLibLyricsProvider,
        "KuGou" to KuGouLyricsProvider,
        "LyricsPlus" to LyricsPlusProvider,
        "YouLyPlus" to YouLyPlusLyricsProvider,
        "Musixmatch" to MusixmatchLyricsProvider,
        "Unison" to UnisonLyricsProvider,
        "BiniLyrics" to BiniLyricsProvider,
        "SimpMusic" to SimpMusicLyricsProvider,
        "NetEase" to NetEaseLyricsProvider,
        "Megalobiz" to MegalobizLyricsProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
        "YouTube" to YouTubeLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

    fun getProviderName(provider: LyricsProvider): String? =
        providerMap.entries.find { it.value == provider }?.key

    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) {
            return getDefaultProviderOrder()
        }
        // Two separators have shipped: the priority screen serialises with ","
        // while an older migration wrote ";". Splitting on only the comma made
        // a semicolon-joined order parse to a single unknown token, which was
        // filtered away — so the whole list came back empty and the resolver
        // silently fell back to the default order, ignoring whatever the user
        // had arranged. Accept both, and match names case-insensitively so a
        // hand-edited or older entry still lands on its provider.
        val byLowercaseName = providerNames.associateBy { it.lowercase(Locale.ROOT) }
        return orderString
            .split(';', ',')
            .map { it.trim() }
            .mapNotNull { stored -> byLowercaseName[stored.lowercase(Locale.ROOT)] }
    }

    fun serializeProviderOrder(providers: List<String>): String {
        // The comma is the canonical separator: every writer must agree on it,
        // or deserialization has to guess which format it is reading.
        return providers.filter { it in providerNames }.joinToString(",")
    }

    fun getDefaultProviderOrder(): List<String> = listOf(
        "NetEase",
        "Spotify",
        "Musixmatch",
        "YouLyPlus",
        "Unison",
        "BiniLyrics",
        "SimpMusic",
        "BetterLyrics",
        "LrcLib",
        "KuGou",
        "Paxsenix",
        "Megalobiz",
        "LyricsPlus",
        "YouTubeSubtitle",
        "YouTube",
    )

    fun getOrderedProviders(orderString: String): List<LyricsProvider> {
        val order = deserializeProviderOrder(orderString)
        return order.mapNotNull { getProviderByName(it) }
    }
}
