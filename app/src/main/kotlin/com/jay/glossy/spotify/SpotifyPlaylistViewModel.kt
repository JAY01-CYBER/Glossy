package com.jay.glossy.spotify

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.collect.ImmutableList
<<<<<<< HEAD
=======
import com.jay.glossy.spotifycore.Spotify
>>>>>>> origin/main
import com.jay.glossy.spotifycore.models.SpotifyPlaylist
import com.jay.glossy.spotifycore.models.SpotifyTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpotifyPlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resolveSpotifyPlaylistDownloads: ResolveSpotifyPlaylistDownloadsUseCase,
<<<<<<< HEAD
    private val library: SpotifyLibraryRepository,
=======
>>>>>>> origin/main
) : ViewModel() {
    private val playlistId: String = savedStateHandle.get<String>("playlistId").orEmpty()

    private val _uiState = MutableStateFlow(SpotifyPlaylistUiState(isLoading = true))
    val uiState: StateFlow<SpotifyPlaylistUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<SpotifyPlaylistEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var reloadJob: Job? = null
    private var downloadResolutionJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        if (playlistId.isBlank()) {
            _uiState.value = SpotifyPlaylistUiState(errorMessage = "Missing Spotify playlist")
            return
        }
        reloadJob?.cancel()
        downloadResolutionJob?.cancel()
        downloadResolutionJob = null
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                downloadItems = ImmutableList.of(),
                isResolvingDownloads = false,
            )
        }
        reloadJob = viewModelScope.launch(Dispatchers.IO) {
<<<<<<< HEAD
            // Whatever was saved for this playlist is the screen's first frame.
            // Offline that is the whole playlist; online the live copy replaces
            // it within a moment. Without this, opening a playlist offline was
            // a spinner and then an error, with the tracks sitting on disk.
            val cached = runCatching { library.cachedPlaylistDetail(playlistId) }.getOrNull()
            if (cached != null) {
                _uiState.value =
                    SpotifyPlaylistUiState(
                        playlist = cached.playlist,
                        tracks = cached.tracks,
                        isLoading = true,
                    )
            }
            try {
                val detail = library.loadPlaylistDetail(playlistId)
                _uiState.value =
                    SpotifyPlaylistUiState(
                        playlist = detail.playlist,
                        tracks = detail.tracks,
                        isLoading = false,
                    )
=======
            try {
                val playlistRes = Spotify.playlist(playlistId).getOrThrow()
                val tracksRes = Spotify.playlistTracks(playlistId).getOrThrow().items.mapNotNull { it.track }
                _uiState.value = SpotifyPlaylistUiState(
                    playlist = playlistRes,
                    tracks = tracksRes,
                    isLoading = false,
                )
>>>>>>> origin/main
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
<<<<<<< HEAD
                        // A saved copy with tracks already answers the question;
                        // an error line under it would only be noise. A header
                        // with no tracks yet still gets the real message.
                        errorMessage = if (it.tracks.isNotEmpty()) null else (error.message ?: "Failed to load playlist"),
=======
                        errorMessage = error.message ?: "Failed to load playlist",
>>>>>>> origin/main
                    )
                }
            }
        }
    }

    fun resolveDownloads() {
        val state = _uiState.value
        if (state.downloadItems.isNotEmpty()) {
            eventChannel.trySend(SpotifyPlaylistEvent.DownloadsResolved(state.downloadItems))
            return
        }
        if (state.tracks.isEmpty() || downloadResolutionJob?.isActive == true) return

        val tracks = state.tracks
        downloadResolutionJob = viewModelScope.launch {
            _uiState.update { it.copy(isResolvingDownloads = true) }
            try {
                val items = resolveSpotifyPlaylistDownloads(tracks)
                if (items.isEmpty()) {
                    _uiState.update { it.copy(isResolvingDownloads = false) }
                    eventChannel.send(SpotifyPlaylistEvent.DownloadResolutionFailed)
                } else {
                    _uiState.update {
                        it.copy(
                            downloadItems = items,
                            isResolvingDownloads = false,
                        )
                    }
                    eventChannel.send(SpotifyPlaylistEvent.DownloadsResolved(items))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update { it.copy(isResolvingDownloads = false) }
                eventChannel.send(SpotifyPlaylistEvent.DownloadResolutionFailed)
            }
        }
    }
}

sealed interface SpotifyPlaylistEvent {
    @Immutable
    data class DownloadsResolved(
        val items: ImmutableList<SpotifyDownloadItem>,
    ) : SpotifyPlaylistEvent

    data object DownloadResolutionFailed : SpotifyPlaylistEvent
}

@Immutable
data class SpotifyPlaylistUiState(
    val playlist: SpotifyPlaylist? = null,
    val tracks: List<SpotifyTrack> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val downloadItems: ImmutableList<SpotifyDownloadItem> = ImmutableList.of(),
    val isResolvingDownloads: Boolean = false,
)
