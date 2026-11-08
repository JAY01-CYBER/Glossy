/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jay.glossy.db.MusicDatabase
import com.jay.glossy.db.entities.Album
import com.jay.glossy.db.entities.Artist
import com.jay.glossy.db.entities.Song
import com.jay.glossy.db.entities.SongWithStats
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * One playable row of the monthly replay. [SongWithStats] carries the listening
 * time for the selected month, while [Song] is what the queue needs to start
 * playback, so both are kept together instead of looked up per tap.
 */
data class ReplaySong(
    val song: Song,
    val playTimeMs: Long,
)

/**
 * Everything the Replay header and its three ranked lists need for one month.
 * An empty month is a valid state: [hasPlays] stays false while the header
 * still lets the listener switch months or years.
 */
data class ReplayMonth(
    val yearMonth: YearMonth,
    val totalPlayTimeMs: Long = 0L,
    val uniqueSongs: Int = 0,
    val uniqueArtists: Int = 0,
    val uniqueAlbums: Int = 0,
    val topArtists: List<Artist> = emptyList(),
    val topSongs: List<ReplaySong> = emptyList(),
    val topAlbums: List<Album> = emptyList(),
) {
    val totalMinutes: Long
        get() = totalPlayTimeMs / 60_000L

    val hasPlays: Boolean
        get() = totalPlayTimeMs > 0L
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReplayViewModel
    @Inject
    constructor(
        database: MusicDatabase,
    ) : ViewModel() {
        private val today = LocalDate.now()

        val selectedMonth = MutableStateFlow(YearMonth.now())

        /**
         * Years worth offering in the picker: from the first play ever recorded
         * up to the current year. Falls back to just this year on a fresh
         * install, where the picker has nothing else to show.
         */
        val availableYears: StateFlow<List<Int>> =
            database
                .firstEvent()
                .map { firstEvent ->
                    val firstYear = firstEvent?.event?.timestamp?.year ?: today.year
                    (firstYear..today.year).reversed().toList()
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000L),
                    listOf(today.year),
                )

        val month: StateFlow<ReplayMonth> =
            selectedMonth
                .flatMapLatest { yearMonth ->
                    val from = yearMonth.atDay(1).atStartOfDay()
                    val to = yearMonth.atEndOfMonth().atTime(23, 59, 59)

                    val counts =
                        combine(
                            database.getTotalPlayTimeInRange(from, to),
                            database.getUniqueSongCountInRange(from, to),
                            database.getUniqueArtistCountInRange(from, to),
                            database.getUniqueAlbumCountInRange(from, to),
                        ) { playTime, songs, artists, albums ->
                            MonthCounts(
                                playTimeMs = playTime ?: 0L,
                                songs = songs,
                                artists = artists,
                                albums = albums,
                            )
                        }

                    val content =
                        combine(
                            database.mostPlayedArtists(
                                fromTimeStamp = from,
                                limit = TOP_ARTIST_COUNT,
                                toTimeStamp = to,
                            ),
                            database.mostPlayedSongsStats(
                                fromTimeStamp = from,
                                limit = TOP_SONG_COUNT,
                                toTimeStamp = to,
                            ),
                            database.mostPlayedSongs(
                                fromTimeStamp = from,
                                limit = TOP_SONG_COUNT,
                                toTimeStamp = to,
                            ),
                            database.mostPlayedAlbums(
                                fromTimeStamp = from,
                                limit = TOP_ALBUM_COUNT,
                                toTimeStamp = to,
                            ),
                        ) { artists, songStats, songs, albums ->
                            MonthContent(artists, songStats, songs, albums)
                        }

                    combine(counts, content) { monthCounts, monthContent ->
                        ReplayMonth(
                            yearMonth = yearMonth,
                            totalPlayTimeMs = monthCounts.playTimeMs,
                            uniqueSongs = monthCounts.songs,
                            uniqueArtists = monthCounts.artists,
                            uniqueAlbums = monthCounts.albums,
                            topArtists = monthContent.artists,
                            topSongs = monthContent.toReplaySongs(),
                            topAlbums = monthContent.albums,
                        )
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000L),
                    ReplayMonth(yearMonth = selectedMonth.value),
                )

        fun selectMonth(month: Month) {
            val current = selectedMonth.value
            if (current.year == today.year && month.value > today.monthValue) return
            selectedMonth.value = YearMonth.of(current.year, month)
        }

        fun selectYear(year: Int) {
            val current = selectedMonth.value
            // The current year cannot have future months, so a year change
            // lands on the latest selectable month instead of an empty one.
            val latestMonth = if (year == today.year) today.monthValue else Month.DECEMBER.value
            selectedMonth.value = YearMonth.of(year, current.monthValue.coerceAtMost(latestMonth))
        }

        private data class MonthCounts(
            val playTimeMs: Long,
            val songs: Int,
            val artists: Int,
            val albums: Int,
        )

        private data class MonthContent(
            val artists: List<Artist>,
            val songStats: List<SongWithStats>,
            val songs: List<Song>,
            val albums: List<Album>,
        ) {
            fun toReplaySongs(): List<ReplaySong> {
                val songsById = songs.associateBy { it.id }
                return songStats.mapNotNull { stats ->
                    val song = songsById[stats.id] ?: return@mapNotNull null
                    ReplaySong(
                        song = song,
                        playTimeMs = stats.timeListened ?: 0L,
                    )
                }
            }
        }

        private companion object {
            const val TOP_ARTIST_COUNT = 6
            const val TOP_SONG_COUNT = 5
            const val TOP_ALBUM_COUNT = 6
        }
    }
