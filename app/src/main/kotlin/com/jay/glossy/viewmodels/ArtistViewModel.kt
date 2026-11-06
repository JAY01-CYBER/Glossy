/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.viewmodels

import com.jay.glossy.R

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.pages.ArtistPage
import com.jay.glossy.constants.HideExplicitKey
import com.jay.glossy.constants.HideVideoSongsKey
import com.jay.glossy.constants.HideYoutubeShortsKey
import com.jay.glossy.constants.UpcomingReleaseRemindersKey
import com.jay.glossy.db.MusicDatabase
import com.jay.glossy.db.entities.ArtistEntity
import com.jay.glossy.db.entities.deserializeArtistPage
import com.jay.glossy.db.entities.serializeArtistPage
import com.jay.glossy.db.entities.toArtistPage
import com.jay.glossy.extensions.filterExplicit
import com.jay.glossy.extensions.filterExplicitAlbums
import com.jay.glossy.upcoming.UpcomingRelease
import com.jay.glossy.upcoming.UpcomingReleaseRepository
import com.jay.glossy.upcoming.normalizeForComparison
import com.jay.glossy.utils.SyncUtils
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get
import com.jay.glossy.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import com.jay.glossy.extensions.filterVideoSongs as filterVideoSongsLocal

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val syncUtils: SyncUtils,
    private val upcomingReleaseRepository: UpcomingReleaseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    private val isPodcastChannel = savedStateHandle.get<Boolean>("isPodcastChannel") ?: false
    var artistPage by mutableStateOf<ArtistPage?>(null)

    /**
     * The artist's next announced release, filled from MusicBrainz once the
     * artist page has a name to match. `null` while the lookup runs and when
     * the artist has nothing announced.
     */
    var upcomingRelease by mutableStateOf<UpcomingRelease?>(null)
        private set

    /** Release ids the listener saved a local reminder for. */
    val remindedReleaseIds = context.dataStore.data
        .map { it[UpcomingReleaseRemindersKey].orEmpty() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private var upcomingReleaseArtistName: String? = null

    // Track API subscription state separately
    private val _apiSubscribed = MutableStateFlow<Boolean?>(null)

    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Combine API state with local database state - local takes precedence when not logged in
    val isChannelSubscribed = kotlinx.coroutines.flow.combine(
        _apiSubscribed,
        database.artist(artistId),
    ) { apiState, localArtist ->
        val locallyBookmarked = localArtist?.artist?.bookmarkedAt != null
        locallyBookmarked || (apiState == true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val librarySongs = context.dataStore.data
        .map { (it[HideExplicitKey] ?: false) to (it[HideVideoSongsKey] ?: false) }
        .distinctUntilChanged()
        .flatMapLatest { (hideExplicit, hideVideoSongs) ->
            database.artistSongsPreview(artistId).map { it.filterExplicit(hideExplicit).filterVideoSongsLocal(hideVideoSongs) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val libraryAlbums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistAlbumsPreview(artistId).map { it.filterExplicitAlbums(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            // Load cached page first for instant display, then fetch fresh data
            loadCachedPage()

            context.dataStore.data
                .map {
                    Triple(
                        it[HideExplicitKey] ?: false,
                        it[HideVideoSongsKey] ?: false,
                        it[HideYoutubeShortsKey] ?: false
                    )
                }
                .distinctUntilChanged()
                .collect {
                    fetchArtistsFromYTM()
                }
        }
    }

    private suspend fun loadCachedPage() {
        try {
            val cachedJson = database.artist(artistId).firstOrNull()?.artist?.cachedPageJson
            if (cachedJson != null) {
                val cachedDto = withContext(Dispatchers.IO) { deserializeArtistPage(cachedJson) }
                val page = cachedDto.toArtistPage()
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

                val filteredSections = page.sections
                    .map { section ->
                        section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                    }
                    .filter { section -> section.items.isNotEmpty() }

                artistPage = page.copy(sections = filteredSections)
                _apiSubscribed.value = page.isSubscribed
                viewModelScope.launch { fetchUpcomingRelease(page.artist.title) }
            }
        } catch (e: Exception) {
            reportException(e)
        }
    }

    fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            YouTube.artist(artistId)
                .onSuccess { page ->
                    // Collect all items from all sections and resolve artist IDs once
                    val allItems = page.sections.flatMap { it.items }
                    val resolvedIdMap = if (allItems.isNotEmpty()) {
                        YouTube.resolveArtistIdMap(allItems)
                    } else {
                        emptyMap()
                    }

                    fun com.metrolist.innertube.models.Artist.resolve() =
                        if (id == null) resolvedIdMap[name]?.let { copy(id = it) } ?: this else this

                    // Resolve artist IDs and fetch durations from more endpoint
                    val resolvedSections = page.sections.map { section ->
                        section.copy(items = section.items.map { item ->
                            when (item) {
                                is SongItem -> item.copy(artists = item.artists.map { it.resolve() })
                                is AlbumItem -> item.copy(artists = item.artists?.map { it.resolve() })
                                is PlaylistItem -> item.copy(author = item.author?.resolve())
                                is EpisodeItem -> item.copy(author = item.author?.resolve())
                                is PodcastItem -> item.copy(author = item.author?.resolve())
                                else -> item
                            }
                        })
                    }

                    // Fetch song durations from the more endpoint if the first section has songs without duration
                    var sectionsWithDurations = resolvedSections
                    if (resolvedSections.isNotEmpty()) {
                        val needDurations = resolvedSections.first().items.any {
                            it is SongItem && it.duration == null
                        }
                        if (needDurations) {
                            val moreEndpoint = resolvedSections.first().moreEndpoint
                            if (moreEndpoint != null) {
                                try {
                                    val moreResult = withContext(Dispatchers.IO) {
                                        YouTube.artistItems(moreEndpoint)
                                    }
                                    moreResult
                                        .onSuccess { moreItems ->
                                            val durationById = moreItems.items.filterIsInstance<SongItem>()
                                                .associate { it.id to it.duration }
                                            if (durationById.isNotEmpty()) {
                                                sectionsWithDurations = listOf(resolvedSections.first().copy(
                                                    items = resolvedSections.first().items.map { item ->
                                                        if (item is SongItem && item.duration == null) {
                                                            item.copy(duration = durationById[item.id] ?: item.duration)
                                                        } else item
                                                    }
                                                )) + resolvedSections.drop(1)
                                            }
                                        }
                                        .onFailure { e -> reportException(e) }
                                } catch (e: Exception) {
                                    reportException(e)
                                }
                            }
                        }
                    }

                    val resolvedPage = page.copy(sections = sectionsWithDurations)
                    val filteredSections = resolvedPage.sections
                        .map { section ->
                            section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                        }
                        .filter { section -> section.items.isNotEmpty() }

                    artistPage = resolvedPage.copy(sections = filteredSections)
                    // Cache page data + persist artist metadata
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val cachedJson = serializeArtistPage(
                                sections = resolvedPage.sections,
                                description = resolvedPage.description,
                                subscriberCountText = resolvedPage.subscriberCountText,
                                monthlyListenerCount = resolvedPage.monthlyListenerCount,
                                isSubscribed = resolvedPage.isSubscribed,
                                artist = resolvedPage.artist,
                            )
                            val existingArtist = database.artist(artistId).firstOrNull()?.artist
                            if (existingArtist != null) {
                                database.update(
                                    existingArtist.copy(
                                        name = resolvedPage.artist.title,
                                        channelId = resolvedPage.artist.channelId ?: existingArtist.channelId,
                                        thumbnailUrl = resolvedPage.artist.thumbnail ?: existingArtist.thumbnailUrl,
                                        cachedPageJson = cachedJson,
                                        lastUpdateTime = java.time.LocalDateTime.now(),
                                    )
                                )
                            } else {
                                val apiArtist = resolvedPage.artist
                                database.insert(
                                    ArtistEntity(
                                        id = artistId,
                                        name = apiArtist.title,
                                        channelId = apiArtist.channelId,
                                        thumbnailUrl = apiArtist.thumbnail,
                                        cachedPageJson = cachedJson,
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            reportException(e)
                        }
                    }
                    // Store API subscription state
                    _apiSubscribed.value = resolvedPage.isSubscribed
                    viewModelScope.launch { fetchUpcomingRelease(resolvedPage.artist.title) }
                }.onFailure {
                    reportException(it)
                }
        }
    }

    private suspend fun fetchUpcomingRelease(artistName: String) {
        if (isPodcastChannel || upcomingReleaseArtistName == artistName) return
        upcomingReleaseArtistName = artistName
        upcomingReleaseRepository
            .findUpcomingRelease(artistName)
            .onSuccess { release -> upcomingRelease = release }
            .onFailure { throwable ->
                // A failed lookup is not a confirmed "nothing announced": allow
                // the next pass — a retry or a preference change — to ask again.
                upcomingReleaseArtistName = null
                reportException(throwable)
            }
    }

    fun toggleUpcomingReleaseReminder(releaseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { preferences ->
                val reminders = preferences[UpcomingReleaseRemindersKey].orEmpty()
                preferences[UpcomingReleaseRemindersKey] =
                    if (releaseId in reminders) reminders - releaseId else reminders + releaseId
            }
        }
    }

    /**
     * Looks for the release on YouTube Music when the card is opened. The
     * release may not have reached YouTube yet, in which case there is nothing
     * to navigate to.
     */
    suspend fun findAlbumIdForUpcomingRelease(release: UpcomingRelease): String? =
        withContext(Dispatchers.IO) {
            val targetTitle = normalizeForComparison(release.title)
            if (targetTitle.isEmpty()) return@withContext null

            val targetArtist = normalizeForComparison(release.artistName)
            val albums =
                YouTube
                    .search(
                        "${release.artistName} ${release.title}",
                        YouTube.SearchFilter.FILTER_ALBUM,
                    ).getOrNull()
                    ?.items
                    ?.filterIsInstance<AlbumItem>()
                    .orEmpty()

            albums
                .firstOrNull { album ->
                    val title = normalizeForComparison(album.title)
                    val titleMatches =
                        title == targetTitle ||
                            title.startsWith(targetTitle) ||
                            targetTitle.startsWith(title)
                    val albumArtists = album.artists.orEmpty()
                    val artistMatches =
                        albumArtists.isEmpty() ||
                            albumArtists.any { artist ->
                                normalizeForComparison(artist.name) == targetArtist
                            }
                    titleMatches && artistMatches
                }?.id
        }

    fun toggleChannelSubscription() {
        val channelId = artistPage?.artist?.channelId ?: artistId
        val isCurrentlySubscribed = isChannelSubscribed.value
        val shouldBeSubscribed = !isCurrentlySubscribed

        Timber.d("[CHANNEL_TOGGLE] toggleChannelSubscription called: artistId=$artistId, channelId=$channelId, isCurrentlySubscribed=$isCurrentlySubscribed, shouldBeSubscribed=$shouldBeSubscribed")

        // Optimistically update API state for immediate UI feedback
        _apiSubscribed.value = shouldBeSubscribed

        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("[CHANNEL_TOGGLE] Inside coroutine, updating database...")
            // Update local database first (optimistic update)
            // Call DAO methods directly - they're synchronous on IO dispatcher
            val artist = libraryArtist.value?.artist
            Timber.d("[CHANNEL_TOGGLE] libraryArtist.value?.artist = $artist")
            if (artist != null) {
                val newBookmark = if (shouldBeSubscribed) {
                    artist.bookmarkedAt ?: java.time.LocalDateTime.now()
                } else {
                    null
                }
                // Also set isPodcastChannel if subscribing from podcast context
                val updatedArtist = artist.copy(
                    bookmarkedAt = newBookmark,
                    isPodcastChannel = if (shouldBeSubscribed && isPodcastChannel) true else artist.isPodcastChannel
                )
                Timber.d("[CHANNEL_TOGGLE] Updating existing artist: ${artist.id} -> bookmarkedAt=$newBookmark, isPodcastChannel=${updatedArtist.isPodcastChannel}")
                database.update(updatedArtist)
            } else if (shouldBeSubscribed) {
                Timber.d("[CHANNEL_TOGGLE] No existing artist, inserting new one")
                artistPage?.artist?.let {
                    database.insert(
                        ArtistEntity(
                            id = artistId,
                            name = it.title,
                            channelId = it.channelId,
                            thumbnailUrl = it.thumbnail,
                            bookmarkedAt = java.time.LocalDateTime.now(),
                            isPodcastChannel = isPodcastChannel,
                        )
                    )
                    Timber.d("[CHANNEL_TOGGLE] Inserted new artist: $artistId, isPodcastChannel=$isPodcastChannel")
                } ?: Timber.d("[CHANNEL_TOGGLE] artistPage?.artist is null, cannot insert")
            } else {
                Timber.d("[CHANNEL_TOGGLE] No artist and shouldBeSubscribed=false, nothing to do")
            }

            Timber.d("[CHANNEL_TOGGLE] Calling syncUtils.subscribeChannel($channelId, $shouldBeSubscribed)")
            // Sync with YouTube (handles login check internally)
            syncUtils.subscribeChannel(channelId, shouldBeSubscribed)
        }
    }
}
