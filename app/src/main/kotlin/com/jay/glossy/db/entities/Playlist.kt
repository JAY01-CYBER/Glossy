/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.db.entities

import com.jay.glossy.R

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class Playlist(
    @Embedded
    val playlist: PlaylistEntity,
    val songCount: Int,
    @Relation(
        entity = SongEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        projection = ["thumbnailUrl"],
        associateBy =
        Junction(
            value = PlaylistSongMapPreview::class,
            parentColumn = "playlistId",
            entityColumn = "songId",
        ),
    )
    val songThumbnails: List<String?>,
) : LocalItem() {
    override val id: String
        get() = playlist.id
    override val title: String
        get() = playlist.name
    override val thumbnailUrl: String?
        get() = null
    
    /**
     * Build a clean collage from individual song artworks. A playlist thumbnail
     * supplied by YouTube can already be a collage; placing that image inside
     * another collage makes the same four artworks repeat. Prefer distinct song
     * artwork and use the playlist thumbnail only when no song artwork exists.
     */
    val thumbnails: List<String>
        get() {
            fun canonical(url: String): String = when {
                url.contains("googleusercontent.com/") -> url.substringBefore("=w").substringBefore("=s")
                url.contains("ggpht.com/") -> url.substringBefore("=s")
                url.contains("ytimg.com/") -> url.substringBefore("?")
                else -> url.substringBefore("?")
            }

            val songArtwork = songThumbnails.asSequence()
                .filterNotNull()
                .map(String::trim)
                .filter { it.startsWith("https://") || it.startsWith("http://") }
                .distinctBy(::canonical)
                .take(4)
                .toList()

            if (songArtwork.isNotEmpty()) return songArtwork

            // Local playlists may not have song artwork yet (e.g. metadata has
            // not been fetched). Keep the saved playlist artwork as a fallback.
            return listOfNotNull(playlist.thumbnailUrl?.trim()?.takeIf {
                it.isNotEmpty() && (it.startsWith("https://") || it.startsWith("http://"))
            }).distinctBy(::canonical).take(1)
        }


}
