package com.jay.glossy.spotify

import androidx.compose.runtime.Immutable
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import com.jay.glossy.spotifycore.models.SpotifyTrack
import javax.inject.Inject

class ResolveSpotifyPlaylistDownloadsUseCase @Inject constructor() {
    suspend operator fun invoke(tracks: List<SpotifyTrack>): ImmutableList<SpotifyDownloadItem> =
        withContext(Dispatchers.IO) {
            val resolvedItems = ArrayList<SpotifyDownloadItem>(tracks.size)

            tracks.chunked(MAX_CONCURRENT_RESOLUTIONS).forEach { chunk ->
                val resolvedChunk = coroutineScope {
                    chunk.map { track ->
                        async {
                            SpotifyPlaybackResolver.resolveToMetadata(track)?.let { metadata ->
                                SpotifyDownloadItem(
                                    id = metadata.id,
                                    title = metadata.title,
                                )
                            }
                        }
                    }.awaitAll().filterNotNull()
                }
                resolvedItems += resolvedChunk
            }

            ImmutableList.copyOf(resolvedItems.distinctBy(SpotifyDownloadItem::id))
        }

    private companion object {
        const val MAX_CONCURRENT_RESOLUTIONS = 8
    }
}

@Immutable
data class SpotifyDownloadItem(
    val id: String,
    val title: String,
)
