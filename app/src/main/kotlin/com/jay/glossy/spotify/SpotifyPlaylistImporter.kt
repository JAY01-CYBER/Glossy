package com.jay.glossy.spotify

import com.jay.glossy.db.MusicDatabase
import com.jay.glossy.db.entities.PlaylistEntity
import com.jay.glossy.spotifycore.Spotify
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.YouTube.SearchFilter
import com.metrolist.innertube.models.SongItem
import com.metrolist.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object SpotifyPlaylistImporter {
    private const val MAX_CONCURRENT_RESOLUTIONS = 8

    suspend fun importPlaylist(
        database: MusicDatabase, 
        playlistId: String, 
        playlistName: String
    ): Int = withContext(Dispatchers.IO) {
        val page = Spotify.playlistTracks(playlistId, limit = 100).getOrThrow()
        val tracks = page.items.mapNotNull { it.track }
        if (tracks.isEmpty()) error("Playlist has no playable tracks")

        val entity = PlaylistEntity(name = playlistName)
        database.transaction { insert(entity) }
        val localPlaylist = database.playlist(entity.id).firstOrNull() ?: error("Could not create local playlist")
        
        val songIds = mutableListOf<String>()
        tracks.chunked(MAX_CONCURRENT_RESOLUTIONS).forEach { chunk ->
            val resolvedChunk = coroutineScope {
                chunk.map { track ->
                    async {
                        val artist = track.artists.firstOrNull()?.name ?: ""
                        YouTube.search("${track.name} $artist", SearchFilter.FILTER_SONG).getOrNull()?.items
                            ?.filterIsInstance<SongItem>()?.firstOrNull()?.let { song ->
                                val metadata = song.toMediaMetadata()
                                database.transaction { insert(metadata) }
                                metadata.id
                            }
                    }
                }.awaitAll().filterNotNull()
            }
            songIds.addAll(resolvedChunk)
        }
        
        database.addSongsToPlaylist(localPlaylist, songIds.map { it to null })
        songIds.size
    }
}
