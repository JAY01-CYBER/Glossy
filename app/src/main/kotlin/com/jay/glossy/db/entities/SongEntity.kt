/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.db.entities

import com.jay.glossy.R

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.metrolist.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Immutable
@Entity(
    tableName = "song",
    indices = [
        Index(
            value = ["albumId"],
        ),
    ],
)
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val duration: Int = -1, // in seconds
    val thumbnailUrl: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    @ColumnInfo(defaultValue = "0")
    val explicit: Boolean = false,
    val year: Int? = null,
    val date: LocalDateTime? = null, // ID3 tag property
    val dateModified: LocalDateTime? = null, // file property
    val liked: Boolean = false,
    val likedDate: LocalDateTime? = null,
    val totalPlayTime: Long = 0, // in milliseconds
    val inLibrary: LocalDateTime? = null,
    val dateDownload: LocalDateTime? = null,
    @ColumnInfo(name = "isLocal", defaultValue = false.toString())
    val isLocal: Boolean = false,
    val libraryAddToken: String? = null,
    val libraryRemoveToken: String? = null,
    @ColumnInfo(defaultValue = "0")
    val lyricsOffset: Int = 0,
    @ColumnInfo(defaultValue = true.toString())
    val romanizeLyrics: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val isDownloaded: Boolean = false,
    @ColumnInfo(name = "isUploaded", defaultValue = false.toString())
    val isUploaded: Boolean = false,
    @ColumnInfo(name = "isVideo", defaultValue = false.toString())
    val isVideo: Boolean = false,
    @ColumnInfo(name = "isEpisode", defaultValue = false.toString())
    val isEpisode: Boolean = false,
    @ColumnInfo(name = "playbackPosition", defaultValue = "NULL")
    val playbackPosition: Long? = null,
    @ColumnInfo(name = "uploadEntityId", defaultValue = "NULL")
    val uploadEntityId: String? = null,
    @ColumnInfo(name = "isCached", defaultValue = "0")
    val isCached: Boolean = false,
) {
    fun localToggleLike() =
        copy(
            liked = !liked,
            likedDate = if (!liked) LocalDateTime.now() else null,
        )

    fun toggleLike() =
        copy(
            liked = !liked,
            likedDate = if (!liked) LocalDateTime.now() else null,
            inLibrary = if (!liked) inLibrary ?: LocalDateTime.now() else inLibrary,
        ).also {
            CoroutineScope(Dispatchers.IO).launch {
                YouTube.likeVideo(id, !liked)
            }
        }

    fun toggleLibrary(syncToYouTube: Boolean = true) =
        copy(
            liked = if (inLibrary == null) liked else false,
            inLibrary = if (inLibrary == null) LocalDateTime.now() else null,
            likedDate = if (inLibrary == null) likedDate else null,
        ).also {
            if (syncToYouTube) {
                CoroutineScope(Dispatchers.IO).launch {
                    // Use the new reliable method that fetches fresh tokens
                    val addToLibrary = inLibrary == null
                    YouTube.toggleSongLibrary(id, addToLibrary)
                }
            }
        }

    fun toggleUploaded() =
        copy(
            isUploaded = !isUploaded,
        )
}

/**
 * The per-song lyrics offset as the lyrics views must apply it.
 *
 * The stored number is what the offset dialog edits, and its sign belongs to
 * the user: "+50" means the lyrics should arrive 50 ms *later*. The views find
 * the line being sung by comparing the playback clock against each line's own
 * timestamp, so a positive offset has to move that clock *back* — which is why
 * the sign is negated here, once, instead of at every call site. Each read site
 * keeps adding a single signed value, and the tap-a-line seek, which inverts
 * the same expression, stays correct without a second edit.
 */
val SongEntity.lyricsClockOffset: Int
    get() = -lyricsOffset
