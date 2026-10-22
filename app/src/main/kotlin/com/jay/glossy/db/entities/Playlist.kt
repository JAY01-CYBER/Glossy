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
            fun canonical(url: String): String = if (
                url.startsWith("https://lh3.googleusercontent.com/") ||
                url.startsWith("https://yt3.googleusercontent.com/")
            ) url.substringBefore("=w") else url.substringBefore("=s")

            val songArtwork = songThumbnails.asSequence()
                .filterNotNull()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinctBy(::canonical)
                .take(4)
                .toList()

            if (songArtwork.isNotEmpty()) return songArtwork

            return listOfNotNull(playlist.thumbnailUrl?.trim()?.takeIf(String::isNotEmpty))
                .distinctBy(::canonical)
                .take(1)
        }


}
