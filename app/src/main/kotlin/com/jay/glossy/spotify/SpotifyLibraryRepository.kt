package com.jay.glossy.spotify

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.jay.glossy.spotifycore.Spotify
import com.jay.glossy.spotifycore.SpotifyAuth
import com.jay.glossy.spotifycore.models.SpotifyPlaylist
import com.jay.glossy.spotifycore.models.SpotifyPlaylistTracksRef
import com.jay.glossy.spotifycore.models.SpotifyTrack
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.safeDataStoreEdit
import javax.inject.Inject
import javax.inject.Singleton

val SpotifySpDcKey = stringPreferencesKey("spotify_sp_dc")
val SpotifyAccessTokenKey = stringPreferencesKey("spotify_access_token")
val SpotifyAccessTokenExpiresAtKey = longPreferencesKey("spotify_token_expires_at")
val SpotifyAccountNameKey = stringPreferencesKey("spotify_account_name")
val SpotifyLibraryPlaylistsCacheKey = stringPreferencesKey("spotify_library_playlists_cache")

@Singleton
class SpotifyLibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _playlists = MutableStateFlow<List<SpotifyPlaylist>>(emptyList())
    val playlists: StateFlow<List<SpotifyPlaylist>> = _playlists.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val tokenRefreshMutex = Mutex()
    private val spotifyCacheJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // NAYA FUNCTION: Instant load from cache
    suspend fun restoreCachedPlaylists() = withContext(Dispatchers.IO) {
        if (_playlists.value.isNotEmpty()) return@withContext
        val cached = context.dataStore.data.first()[SpotifyLibraryPlaylistsCacheKey].orEmpty()
        if (cached.isBlank()) return@withContext
        runCatching {
            spotifyCacheJson.decodeFromString(
                ListSerializer(SpotifyPlaylist.serializer()),
                cached,
            )
        }.onSuccess { cachedPlaylists ->
            _playlists.value = cachedPlaylists
        }
    }

    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val token = prefs[SpotifyAccessTokenKey].orEmpty()
        val expiresAt = prefs[SpotifyAccessTokenExpiresAtKey] ?: 0L
        if (token.isNotBlank() && expiresAt > System.currentTimeMillis() + 60000L) {
            Spotify.accessToken = token
            return@withContext true
        }
        val spDc = prefs[SpotifySpDcKey].orEmpty()
        if (spDc.isBlank()) return@withContext false
        
        refreshAccessToken(spDc).isSuccess
    }

    suspend fun connectWithCookies(spDc: String) = withContext(Dispatchers.IO) {
        context.safeDataStoreEdit { prefs ->
            prefs[SpotifySpDcKey] = spDc
            prefs.remove(SpotifyAccessTokenKey)
            prefs.remove(SpotifyLibraryPlaylistsCacheKey)
        }
        Spotify.accessToken = null
        _playlists.value = emptyList()
        refreshAccessToken(spDc).getOrThrow()
    }

    suspend fun refreshPlaylists(): List<SpotifyPlaylist> = withContext(Dispatchers.IO) {
        _isRefreshing.value = true
        _errorMessage.value = null
        try {
            ensureAuthenticated()
            val loaded = fetchAllPlaylists()
            _playlists.value = loaded
            context.safeDataStoreEdit { prefs ->
                prefs[SpotifyLibraryPlaylistsCacheKey] = spotifyCacheJson.encodeToString(
                    ListSerializer(SpotifyPlaylist.serializer()),
                    loaded,
                )
            }
            loaded
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            _errorMessage.value = error.message
            _playlists.value
        } finally {
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchAllPlaylists(): List<SpotifyPlaylist> {
        val playlists = ArrayList<SpotifyPlaylist>()
        var offset = 0
        val limit = 50

        while (true) {
            val page = Spotify.myPlaylists(limit = limit, offset = offset).getOrNull() ?: break
            if (page.items.isEmpty()) break
            playlists += page.items.map { playlist ->
                if (playlist.tracks?.total != null) {
                    playlist
                } else {
                    playlistTrackCount(playlist.id)?.let { 
                        playlist.copy(tracks = SpotifyPlaylistTracksRef(total = it)) 
                    } ?: playlist
                }
            }
            offset += page.items.size
            if (offset >= page.total || page.items.size < limit) break
        }
        return playlists
    }

    private suspend fun playlistTrackCount(playlistId: String): Int? = try {
        Spotify.playlistTracks(playlistId = playlistId, limit = 1, offset = 0).getOrNull()?.total
    } catch (_: Exception) {
        null
    }

    suspend fun playlist(playlistId: String): SpotifyPlaylist = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        Spotify.playlist(playlistId).getOrThrow()
    }

    suspend fun playlistTracks(playlistId: String): List<SpotifyTrack> = withContext(Dispatchers.IO) {
        ensureAuthenticated()
        val tracks = ArrayList<SpotifyTrack>()
        var offset = 0
        val limit = 50

        while (true) {
            val page = Spotify.playlistTracks(playlistId = playlistId, limit = limit, offset = offset).getOrThrow()
            if (page.items.isEmpty()) break
            val pageTracks = page.items.mapNotNull { it.track?.takeUnless(SpotifyTrack::isLocal) }
            tracks += pageTracks
            offset += page.items.size
            if (offset >= page.total || page.items.size < limit) break
        }
        tracks
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        context.safeDataStoreEdit { prefs ->
            prefs.remove(SpotifySpDcKey)
            prefs.remove(SpotifyAccessTokenKey)
            prefs.remove(SpotifyAccessTokenExpiresAtKey)
            prefs.remove(SpotifyAccountNameKey)
            prefs.remove(SpotifyLibraryPlaylistsCacheKey)
        }
        _playlists.value = emptyList()
        Spotify.accessToken = null
    }

    private suspend fun ensureAuthenticated() {
        if (!restoreSession()) error("Not connected")
    }

    private suspend fun refreshAccessToken(spDc: String): Result<Unit> = try {
        tokenRefreshMutex.withLock {
            val token = SpotifyAuth.fetchAccessToken(spDc, "").getOrThrow()
            Spotify.accessToken = token.accessToken
            context.safeDataStoreEdit { p ->
                p[SpotifyAccessTokenKey] = token.accessToken
                p[SpotifyAccessTokenExpiresAtKey] = token.accessTokenExpirationTimestampMs
            }
            Spotify.me().onSuccess { user ->
                context.safeDataStoreEdit { it[SpotifyAccountNameKey] = user.displayName.orEmpty() }
            }
            Result.success(Unit)
        }
    } catch (e: Exception) { Result.failure(e) }
}
