/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.viewmodels

import com.jay.glossy.R

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.flow.combine
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.BrowseEndpoint
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.innertube.utils.completed
import com.jay.glossy.constants.HideExplicitKey
import com.jay.glossy.constants.HideVideoSongsKey
import com.jay.glossy.constants.HideYoutubeShortsKey
import com.jay.glossy.constants.InnerTubeCookieKey
import com.jay.glossy.constants.QuickPicks
import com.jay.glossy.constants.QuickPicksKey
import com.jay.glossy.constants.RandomizeHomeOrderKey
import com.jay.glossy.constants.ShowWrappedCardKey
import com.jay.glossy.constants.WrappedSeenKey
import com.jay.glossy.db.MusicDatabase
import com.jay.glossy.db.entities.Album
import com.jay.glossy.db.entities.LocalItem
import com.jay.glossy.db.entities.Song
import com.jay.glossy.db.entities.SpeedDialItem
import com.jay.glossy.extensions.filterVideoSongs
import com.jay.glossy.extensions.toEnum
import com.metrolist.models.SimilarRecommendation
import com.jay.glossy.ui.screens.wrapped.WrappedAudioService
import com.jay.glossy.ui.screens.wrapped.WrappedManager
import com.jay.glossy.utils.SyncUtils
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.safeDataStoreEdit
import com.jay.glossy.utils.get
import com.jay.glossy.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.random.Random

import androidx.datastore.preferences.core.stringPreferencesKey

sealed class HomeSection(
    val id: String,
    val baseWeight: Int,
) {
    data object SpeedDial : HomeSection("speed_dial", 100)
    data object QuickPicks : HomeSection("quick_picks", 90)
    data object Charts : HomeSection("charts", 85)
    data object DailyDiscover : HomeSection("daily_discover", 80)
    data object KeepListening : HomeSection("keep_listening", 50)
    data object AccountPlaylists : HomeSection("account_playlists", 40)
    data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)
    data object FromTheCommunity : HomeSection("from_the_community", 20)
    data class SimilarRecommendation(val index: Int) : HomeSection("similar_recommendation_$index", 10)
    data class HomePageSection(val index: Int) : HomeSection("home_page_section_$index", 10)
    data object MoodAndGenres : HomeSection("mood_and_genres", 5)
}

data class DailyDiscoverItem(
    val seed: Song,
    val recommendation: YTItem,
    val relatedEndpoint: BrowseEndpoint?
)

data class CommunityPlaylistItem(
    val playlist: PlaylistItem,
    val songs: List<SongItem>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    val wrappedManager: WrappedManager,
    private val wrappedAudioService: WrappedAudioService,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val isRandomizing = MutableStateFlow(false)
    
    // Used to trigger reshuffles on pull-to-refresh
    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val dailyDiscover = MutableStateFlow<List<DailyDiscoverItem>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    val savedPodcastShows = MutableStateFlow<List<PodcastItem>>(emptyList())
    val episodesForLater = MutableStateFlow<List<SongItem>>(emptyList())

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val pinnedSpeedDialItems: StateFlow<List<SpeedDialItem>> =
        database.speedDialDao.getAll()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val speedDialItems: StateFlow<List<YTItem>> =
        combine(
            database.speedDialDao.getAll(),
            keepListening,
            quickPicks
        ) { pinned, keepListening, quick ->
            val pinnedItems = pinned.map { it.toYTItem() }
            val filled = pinnedItems.toMutableList()
            val targetSize = 27

            if (filled.size < targetSize) {
                keepListening?.let { k ->
                    val needed = targetSize - filled.size
                    val available = k.filter { item ->
                        filled.none { p -> p.id == item.id }
                    }.mapNotNull { item ->
                        when (item) {
                            is Song -> SongItem(
                                id = item.id,
                                title = item.title,
                                artists = item.artists.map { Artist(name = it.name, id = it.id) },
                                thumbnail = item.thumbnailUrl ?: "",
                                explicit = false
                            )
                            is Album -> AlbumItem(
                                browseId = item.id,
                                playlistId = item.album.playlistId ?: "",
                                title = item.title,
                                artists = item.artists.map { Artist(name = it.name, id = it.id) },
                                year = item.album.year,
                                thumbnail = item.thumbnailUrl ?: ""
                            )
                            is com.jay.glossy.db.entities.Artist -> ArtistItem(
                                id = item.id,
                                title = item.title,
                                thumbnail = item.thumbnailUrl,
                                shuffleEndpoint = null,
                                radioEndpoint = null
                            )
                            else -> null
                        }
                    }
                    filled.addAll(available.take(needed))
                }
            }

            if (filled.size < targetSize) {
                quick?.let { q ->
                    val needed = targetSize - filled.size
                    val available = q.filter { song ->
                        filled.none { p -> p.id == song.id }
                    }.map { song ->
                        SongItem(
                            id = song.id,
                            title = song.title,
                            artists = song.artists.map { Artist(name = it.name, id = it.id) },
                            thumbnail = song.thumbnailUrl ?: "",
                            explicit = false
                        )
                    }
                    filled.addAll(available.take(needed))
                }
            }
            
            filled.distinctBy { it.id }.take(targetSize)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Heavy Processing Moved to Background StateFlows
    val featuredPodcasts: StateFlow<List<PodcastItem>> = combine(homePage, selectedChip, refreshTrigger) { page, chip, _ ->
        if (chip == null) {
            emptyList()
        } else {
            page?.sections
                ?.flatMap { it.items }
                ?.filterIsInstance<EpisodeItem>()
                ?.mapNotNull { episode ->
                    episode.podcast?.let { podcast ->
                        PodcastItem(
                            id = podcast.id,
                            title = podcast.name,
                            author = episode.author,
                            episodeCountText = null,
                            thumbnail = episode.thumbnail,
                            playEndpoint = null,
                            shuffleEndpoint = null,
                        )
                    }
                }?.distinctBy { it.id }
                ?.shuffled()
                ?.take(10) ?: emptyList()
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val spotlightItems: StateFlow<List<SongItem>> = combine(homePage, refreshTrigger) { page, _ ->
        page?.sections
            ?.flatMap { it.items }
            ?.filterIsInstance<SongItem>()
            ?.distinctBy { it.id }
            ?.shuffled()
            ?.take(8) ?: emptyList()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val homeSections: StateFlow<List<HomeSection>> = combine(
        combine(selectedChip, speedDialItems, quickPicks, dailyDiscover) { a, b, c, d -> listOf(a, b, c, d) },
        combine(keepListening, accountPlaylists, forgottenFavorites, communityPlaylists) { a, b, c, d -> listOf(a, b, c, d) },
        combine(similarRecommendations, homePage, explorePage) { a, b, c -> listOf(a, b, c) },
        combine(
            context.dataStore.data.map { it[RandomizeHomeOrderKey] ?: true }.distinctUntilChanged(),
            refreshTrigger
        ) { rand, trigger -> Pair(rand, trigger) }
    ) { group1, group2, group3, randAndTrigger ->
        val chip = group1[0] as HomePage.Chip?
        val speedDial = group1[1] as List<YTItem>
        val quickPicksList = group1[2] as List<Song>?
        val dailyDiscoverList = group1[3] as List<DailyDiscoverItem>?

        val keepListeningList = group2[0] as List<LocalItem>?
        val accountPlaylistsList = group2[1] as List<PlaylistItem>?
        val forgottenFavoritesList = group2[2] as List<Song>?
        val communityPlaylistsList = group2[3] as List<CommunityPlaylistItem>?

        val similarRecommendationsList = group3[0] as List<SimilarRecommendation>?
        val homePageObj = group3[1] as HomePage?
        val explorePageObj = group3[2] as ExplorePage?

        val randomizeHomeOrder = randAndTrigger.first
        val seed = randAndTrigger.second

        val list = mutableListOf<HomeSection>()
        val chipActive = chip != null

        if (!chipActive && speedDial.isNotEmpty()) list.add(HomeSection.SpeedDial)
        if (!chipActive && quickPicksList?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
        if (!chipActive && communityPlaylistsList?.isNotEmpty() == true) list.add(HomeSection.FromTheCommunity)
        if (!chipActive && dailyDiscoverList?.isNotEmpty() == true) list.add(HomeSection.DailyDiscover)
        if (!chipActive) list.add(HomeSection.Charts)
        if (!chipActive && keepListeningList?.isNotEmpty() == true) list.add(HomeSection.KeepListening)
        if (!chipActive && accountPlaylistsList?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
        if (!chipActive && forgottenFavoritesList?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)

        if (!chipActive) {
            similarRecommendationsList?.indices?.forEach { i -> list.add(HomeSection.SimilarRecommendation(i)) }
        }

        homePageObj?.sections?.indices?.forEach { i -> list.add(HomeSection.HomePageSection(i)) }

        if (explorePageObj?.moodAndGenres != null) list.add(HomeSection.MoodAndGenres)

        if (randomizeHomeOrder) {
            list.sortedByDescending { section ->
                val sectionRandom = Random(seed + section.id.hashCode())
                val base = when (section) {
                    HomeSection.SpeedDial, HomeSection.QuickPicks, HomeSection.DailyDiscover, HomeSection.Charts -> 500
                    HomeSection.KeepListening, HomeSection.AccountPlaylists, HomeSection.ForgottenFavorites, HomeSection.FromTheCommunity -> 300
                    else -> 100
                }
                val modifier = when (section) {
                    HomeSection.SpeedDial, HomeSection.QuickPicks, HomeSection.DailyDiscover, HomeSection.Charts -> sectionRandom.nextInt(-200, 400)
                    HomeSection.KeepListening, HomeSection.AccountPlaylists, HomeSection.ForgottenFavorites, HomeSection.FromTheCommunity -> sectionRandom.nextInt(-100, 400)
                    else -> sectionRandom.nextInt(-50, 50)
                }
                base + modifier
            }
        } else {
            val defaultOrder = mapOf(
                HomeSection.SpeedDial to 100, HomeSection.QuickPicks to 90, HomeSection.Charts to 85,
                HomeSection.FromTheCommunity to 80, HomeSection.DailyDiscover to 70,
                HomeSection.KeepListening to 60, HomeSection.AccountPlaylists to 50,
                HomeSection.ForgottenFavorites to 40, HomeSection.MoodAndGenres to 10
            )
            list.sortedByDescending { section ->
                when (section) {
                    is HomeSection.SimilarRecommendation -> 30 - section.index
                    is HomeSection.HomePageSection -> 20 - section.index
                    else -> defaultOrder[section] ?: 0
                }
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun getRandomItem(): YTItem? {
        try {
            isRandomizing.value = true
            kotlinx.coroutines.delay(1000)

            val userSongs = mutableListOf<YTItem>()
            val otherSources = mutableListOf<YTItem>()

            quickPicks.value?.let { songs ->
                userSongs.addAll(songs.map { song ->
                    SongItem(
                        id = song.id, title = song.title,
                        artists = song.artists.map { Artist(name = it.name, id = it.id) },
                        thumbnail = song.thumbnailUrl ?: "", explicit = false
                    )
                })
            }

            keepListening.value?.let { items ->
                items.forEach { item ->
                    when (item) {
                        is Song -> userSongs.add(SongItem(
                            id = item.id, title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            thumbnail = item.thumbnailUrl ?: "", explicit = false
                        ))
                        is Album -> otherSources.add(AlbumItem(
                            browseId = item.id, playlistId = item.album.playlistId ?: "", title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            year = item.album.year, thumbnail = item.thumbnailUrl ?: ""
                        ))
                        is com.jay.glossy.db.entities.Artist -> otherSources.add(ArtistItem(
                            id = item.id, title = item.title, thumbnail = item.thumbnailUrl,
                            shuffleEndpoint = null, radioEndpoint = null
                        ))
                        else -> {}
                    }
                }
            }

            otherSources.addAll(allYtItems.value)

            val item = if (userSongs.isNotEmpty() && (otherSources.isEmpty() || Random.nextFloat() < 0.8f)) {
                userSongs.distinctBy { it.id }.shuffled().firstOrNull()
            } else {
                otherSources.distinctBy { it.id }.shuffled().firstOrNull()
            } ?: userSongs.firstOrNull() ?: otherSources.firstOrNull()

            return item
        } finally {
            isRandomizing.value = false
        }
    }

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

	val showWrappedCard: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        val showWrappedPref = prefs[ShowWrappedCardKey] ?: false
        val seen = prefs[WrappedSeenKey] ?: false
        val isBeforeDate = LocalDate.now().isBefore(LocalDate.of(2026, 2, 1))

        isBeforeDate && (!seen || showWrappedPref)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val wrappedSeen: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WrappedSeenKey] ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun togglePin(item: YTItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val speedDialItem = SpeedDialItem.fromYTItem(item)
            val isPinned = database.speedDialDao.isPinned(speedDialItem.id).first()
            if (isPinned) {
                database.speedDialDao.delete(speedDialItem.id)
            } else {
                database.speedDialDao.insert(speedDialItem)
            }
        }
    }

    fun markWrappedAsSeen() {
        viewModelScope.launch(Dispatchers.IO) {
            context.safeDataStoreEdit {
                it[WrappedSeenKey] = true
            }
        }
    }
    private var lastProcessedCookie: String? = null
    private var isProcessingAccountData = false

    private suspend fun getDailyDiscover() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val likedSongs = database.likedSongsByCreateDateAsc().first()
        if (likedSongs.isEmpty()) return

        val seeds = likedSongs.distinctBy { it.id }.shuffled().take(5)
        val items = java.util.Collections.synchronizedList(mutableListOf<DailyDiscoverItem>())

        kotlinx.coroutines.coroutineScope {
            seeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            val recommendations = page.songs
                                .filter { item ->
                                    if (hideVideoSongs && item.isVideoSong) return@filter false
                                    if (item.explicit) return@filter false
                                    true
                                }
                                .shuffled()

                            val recommendation = recommendations.firstOrNull { rec -> rec.id != seed.id }
                            if (recommendation != null) {
                                items.add(DailyDiscoverItem(seed = seed, recommendation = recommendation, relatedEndpoint = endpoint))
                            }
                        }
                    }
                }
            }.forEach { it.join() }
        }
        dailyDiscover.value = items.toList().distinctBy { it.recommendation.id }.shuffled()
    }

    private suspend fun getQuickPicks() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> {
                val relatedSongs = database.quickPicks().first().filterVideoSongs(hideVideoSongs)
                val forgotten = database.forgottenFavorites().first().filterVideoSongs(hideVideoSongs).take(8)

                val recentSong = database.events().first().firstOrNull()?.song
                val ytSimilarSongs = mutableListOf<Song>()

                if (recentSong != null) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = recentSong.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            page.songs.take(10).forEach { ytSong ->
                                database.song(ytSong.id).first()?.let { localSong ->
                                    if (!hideVideoSongs || !localSong.song.isVideo) {
                                        ytSimilarSongs.add(localSong)
                                    }
                                }
                            }
                        }
                    }
                }

                val combined = (relatedSongs + forgotten + ytSimilarSongs)
                    .distinctBy { it.id }
                    .shuffled()
                    .take(20)

                quickPicks.value = combined.ifEmpty { relatedSongs.distinctBy { it.id }.shuffled().take(20) }
            }
            QuickPicks.LAST_LISTEN -> {
                val song = database.events().first().firstOrNull()?.song
                if (song != null && database.hasRelatedSongs(song.id)) {
                    quickPicks.value = database.getRelatedSongs(song.id).first().filterVideoSongs(hideVideoSongs).distinctBy { it.id }.shuffled().take(20)
                }
            }
        }
    }

    private suspend fun getCommunityPlaylists() {
        val fromTimeStamp = LocalDateTime.now().minusWeeks(4)
        val artistSeeds = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(3)
        val songSeeds = database.mostPlayedSongs(fromTimeStamp = fromTimeStamp, limit = 5, offset = 0, toTimeStamp = LocalDateTime.now()).first()
            .shuffled().take(2)

        val candidatePlaylists = java.util.Collections.synchronizedList(mutableListOf<PlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            artistSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    YouTube.artist(seed.id).onSuccess { page ->
                        page.sections.forEach { section ->
                            section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" && 
                                    playlist.author?.name != "YouTube" && 
                                    playlist.author?.name != "Playlist" &&
                                    playlist.author?.name != seed.artist.name &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }
            
            songSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            page.playlists.forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" && 
                                    playlist.author?.name != "YouTube" && 
                                    playlist.author?.name != "Playlist" &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }
        }

        val uniqueCandidates = candidatePlaylists.distinctBy { it.id }.shuffled().take(5)
        val playlists = java.util.Collections.synchronizedList(mutableListOf<CommunityPlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            uniqueCandidates.map { playlist ->
                launch(Dispatchers.IO) {
                    YouTube.playlist(playlist.id).onSuccess { page ->
                        val songs = page.songs.take(10)
                        if (songs.isNotEmpty()) {
                            val songCountText = page.playlist.songCountText ?: playlist.songCountText
                            val updatedPlaylist = playlist.copy(songCountText = songCountText)
                            playlists.add(CommunityPlaylistItem(updatedPlaylist, songs))
                        }
                    }
                }
            }.forEach { it.join() }
        }

        communityPlaylists.value = playlists.shuffled()
    }

    private suspend fun load() {
        isLoading.value = true
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        val fromTimeStamp = LocalDateTime.now().minusWeeks(2)

        coroutineScope {
            launch(Dispatchers.IO) { getQuickPicks() }

            launch(Dispatchers.IO) {
                forgottenFavorites.value = database.forgottenFavorites().first()
                    .filterVideoSongs(hideVideoSongs).distinctBy { it.id }.shuffled().take(20)
            }

            launch(Dispatchers.IO) {
                val songs = database.mostPlayedSongs(fromTimeStamp = fromTimeStamp, limit = 15, offset = 5, toTimeStamp = LocalDateTime.now()).first()
                    .filterVideoSongs(hideVideoSongs).shuffled().take(10)
                val albums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first()
                    .filter { it.album.thumbnailUrl != null }.shuffled().take(5)
                val artists = database.mostPlayedArtists(fromTimeStamp).first()
                    .filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
                keepListening.value = (songs + albums + artists).distinctBy {
                    when(it) {
                        is Song -> it.id
                        is Album -> it.id
                        is Artist -> it.id
                        is Playlist -> it.id
                        else -> it.toString()
                    }
                }.shuffled()
            }

            launch(Dispatchers.IO) {
                YouTube.home().onSuccess { page ->
                    homePage.value = page.copy(
                        sections = page.sections.mapNotNull { section ->
                            val filtered = section.items
                                .filterOutNulls()
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .filterYoutubeShorts(hideYoutubeShorts)
                                .distinctBy { it.id }
                            if (filtered.isEmpty()) null else section.copy(items = filtered)
                        }
                    )
                }.onFailure { reportException(it) }
            }

            if (YouTube.cookie != null) {
                launch(Dispatchers.IO) { loadAccountPlaylists() }
            }
        }

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }
        isLoading.value = false

        viewModelScope.launch(Dispatchers.IO) { getDailyDiscover() }
        viewModelScope.launch(Dispatchers.IO) { getCommunityPlaylists() }

        viewModelScope.launch(Dispatchers.IO) {
            YouTube.explore().onSuccess { page ->
                explorePage.value = page.copy(
                    newReleaseAlbums = page.newReleaseAlbums.filterOutNulls().filterExplicit(hideExplicit).distinctBy { it.id },
                    moodAndGenres = page.moodAndGenres.filterOutNulls().distinctBy { "${it.title}_${it.endpoint.browseId}" }
                )
            }.onFailure { reportException(it) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val artistRecommendations = database.mostPlayedArtists(fromTimeStamp, limit = 15).first()
                .filter { it.artist.isYouTubeArtist }
                .shuffled().take(4)
                .mapNotNull {
                    val items = mutableListOf<YTItem>()
                    YouTube.artist(it.id).onSuccess { page ->
                        page.sections.takeLast(3).forEach { section -> items += section.items }
                    }
                    SimilarRecommendation(
                        title = it,
                        items = items
                            .distinctBy { item -> item.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .shuffled().take(12)
                            .ifEmpty { return@mapNotNull null }
                    )
                }

            val songRecommendations = database.mostPlayedSongs(fromTimeStamp = fromTimeStamp, limit = 15, offset = 0, toTimeStamp = LocalDateTime.now()).first()
                .filter { it.album != null }
                .shuffled().take(3)
                .mapNotNull { song ->
                    val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                        ?: return@mapNotNull null
                    val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                    SimilarRecommendation(
                        title = song,
                        items = (page.songs.shuffled().take(10) +
                                page.albums.shuffled().take(5) +
                                page.artists.shuffled().take(3) +
                                page.playlists.shuffled().take(3))
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .shuffled()
                            .ifEmpty { return@mapNotNull null }
                    )
                }

            val albumRecommendations = database.mostPlayedAlbums(fromTimeStamp, limit = 10).first()
                .filter { it.album.thumbnailUrl != null }
                .shuffled().take(2)
                .mapNotNull { album ->
                    val items = mutableListOf<YTItem>()
                    YouTube.album(album.id).onSuccess { page ->
                        page.otherVersions.let { items += it }
                    }
                    album.artists.firstOrNull()?.id?.let { artistId ->
                        YouTube.artist(artistId).onSuccess { page ->
                            page.sections.lastOrNull()?.items?.let { items += it }
                        }
                    }
                    SimilarRecommendation(
                        title = album,
                        items = items
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .shuffled().take(10)
                            .ifEmpty { return@mapNotNull null }
                    )
                }

            similarRecommendations.value = (artistRecommendations + songRecommendations + albumRecommendations).shuffled()
            allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                    homePage.value?.sections?.flatMap { it.items }.orEmpty()
        }
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = YouTube.home(continuation).getOrNull() ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = (homePage.value?.sections.orEmpty() + nextSections.sections).mapNotNull { section ->
                    val filteredItems = section.items
                        .filterOutNulls()
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                        .distinctBy { it.id }
                    if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                }
            )
            _isLoadingMore.value = false
        }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            val nextSections = YouTube.home(params = chip.endpoint?.params).getOrNull() ?: return@launch

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = nextSections.sections.mapNotNull { section ->
                    section.copy(items = section.items.filterOutNulls().filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts).distinctBy { it.id })
                }
            )
            selectedChip.value = chip

            if (chip.title.contains("Podcast", ignoreCase = true)) {
                fetchPodcastData()
            }
        }
    }

    private suspend fun fetchPodcastData() {
        YouTube.savedPodcastShows().onSuccess { shows ->
            savedPodcastShows.value = shows.filterOutNulls().distinctBy { it.id }
        }.onFailure {
            reportException(it)
        }

        YouTube.episodesForLater().onSuccess { episodes ->
            episodesForLater.value = episodes.filterOutNulls().distinctBy { it.id }
        }.onFailure {
            reportException(it)
        }
    }

    private suspend fun loadAccountPlaylists() {
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
            accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>()
                .filterOutNulls()
                .filterNot { it.id == "SE" }
                .filterYoutubeShorts(hideYoutubeShorts)
                .distinctBy { it.id }
        }.onFailure {
            reportException(it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> List<T>.filterOutNulls(): List<T> =
        (this as List<T?>).filterNotNull()

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        refreshTrigger.value = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            val currentChip = selectedChip.value
            if (currentChip != null) {
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                val nextSections = YouTube.home(params = currentChip.endpoint?.params).getOrNull()
                if (nextSections != null) {
                    homePage.value = nextSections.copy(
                        chips = homePage.value?.chips,
                        sections = nextSections.sections.mapNotNull { section ->
                            section.copy(items = section.items.filterOutNulls().filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts).distinctBy { it.id })
                        }
                    )
                }
            } else {
                load()
            }
            isRefreshing.value = false
        }
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }
    }

    override fun onCleared() {
        super.onCleared()
        wrappedManager.dispose()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }

        viewModelScope.launch(Dispatchers.IO) {
            showWrappedCard.collect { shouldShow ->
                if (shouldShow && !wrappedManager.state.value.isDataReady) {
                    try {
                        wrappedManager.prepare()
                        val state = wrappedManager.state.first { it.isDataReady }
                        val trackMap = state.trackMap
                        if (trackMap.isNotEmpty()) {
                            val firstTrackId = trackMap.entries.first().value
                            wrappedAudioService.prepareTrack(firstTrackId)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            combine(
                context.dataStore.data.map { it[InnerTubeCookieKey] }.distinctUntilChanged(),
                context.dataStore.data.map { it[stringPreferencesKey("guest_name")] }.distinctUntilChanged()
            ) { cookie, guestName ->
                Pair(cookie, guestName)
            }.collect { (cookie, guestName) ->
                if (isProcessingAccountData) return@collect

                lastProcessedCookie = cookie
                isProcessingAccountData = true

                try {
                    if (cookie != null && cookie.isNotEmpty()) {
                        YouTube.cookie = cookie

                        YouTube.accountInfo().onSuccess { info ->
                            accountName.value = info.name
                            accountImageUrl.value = info.thumbnailUrl
                        }.onFailure {
                            accountName.value = if (!guestName.isNullOrBlank()) guestName else "Guest"
                            reportException(it)
                        }
                    } else {
                        accountName.value = if (!guestName.isNullOrBlank()) guestName else "Guest"
                        accountImageUrl.value = null
                        accountPlaylists.value = null
                    }
                } finally {
                    isProcessingAccountData = false
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[HideYoutubeShortsKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    if (YouTube.cookie != null && accountPlaylists.value != null) {
                        loadAccountPlaylists()
                    }
                }
        }
    }

    private var isHomeDataLoaded = false

    fun loadHomeData() {
        if (isHomeDataLoaded) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cookie = context.dataStore.data
                    .map { it[InnerTubeCookieKey] }
                    .distinctUntilChanged()
                    .first()

                if (!cookie.isNullOrEmpty()) {
                    YouTube.cookie = cookie
                }

                isHomeDataLoaded = true
                load()
            } catch (e: Exception) {
                isHomeDataLoaded = false
                Timber.e(e, "Failed to load home data")
            }
        }
    }
}
