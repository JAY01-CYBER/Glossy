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
     * Prefer playlist artwork, but keep song artwork as a fallback. Some local
     * playlists contain a stale/custom thumbnail URL; using that URL alone
     * makes the whole card fall back to the generic music placeholder even
     * when the playlist's songs have usable artwork.
     */
    val thumbnails: List<String>
        get() = buildList {
            playlist.thumbnailUrl
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(::add)

            songThumbnails
                .asSequence()
                .filterNotNull()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .forEach { thumbnail ->
                    if (thumbnail !in this) add(thumbnail)
                }
        }
}
