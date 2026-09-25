package com.jay.glossy.spotify

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.collect.ImmutableList
import com.jay.glossy.spotifycore.Spotify
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
            try {
                val playlistRes = Spotify.playlist(playlistId).getOrThrow()
                val tracksRes = Spotify.playlistTracks(playlistId).getOrThrow().items.mapNotNull { it.track }
                _uiState.value = SpotifyPlaylistUiState(
                    playlist = playlistRes,
                    tracks = tracksRes,
                    isLoading = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load playlist",
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
