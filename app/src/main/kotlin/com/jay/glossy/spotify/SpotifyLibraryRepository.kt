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
<<<<<<< HEAD
import kotlinx.serialization.Serializable
=======
>>>>>>> origin/main
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

<<<<<<< HEAD
/**
 * A playlist with the tracks the last online visit loaded, kept so the
 * playlist screen has something to draw when there is no network. The list
 * cache above only holds the playlists themselves — enough for the library
 * list, not for the screen a playlist opens into.
 */
@Serializable
data class SpotifyPlaylistDetailCache(
    val playlist: SpotifyPlaylist,
    val tracks: List<SpotifyTrack> = emptyList(),
)

/** Saved copies of one playlist's detail, one preference key per playlist. */
private const val SpotifyPlaylistDetailCachePrefix = "spotify_playlist_detail_"

private fun spotifyPlaylistDetailKey(playlistId: String) =
    stringPreferencesKey("$SpotifyPlaylistDetailCachePrefix$playlistId")

=======
>>>>>>> origin/main
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

<<<<<<< HEAD
    /**
     * Whether a Spotify session exists at all, without touching the network.
     *
     * "The token could not be refreshed" is not "not connected": offline, the
     * refresh always fails, and treating that as a logout hid the playlists
     * that are already saved on the device. A saved cookie or a saved token is
     * what the screens should believe.
     */
    suspend fun isSessionConfigured(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[SpotifySpDcKey].orEmpty().isNotBlank() ||
            prefs[SpotifyAccessTokenKey].orEmpty().isNotBlank()
    }

    /**
     * The playlist as it was last saved, for the screen to open on — the whole
     * list when the network never answered, or just the header from the
     * playlist list when the detail was never cached.
     */
    suspend fun cachedPlaylistDetail(playlistId: String): SpotifyPlaylistDetailCache? {
        if (playlistId.isBlank()) return null
        val stored = context.dataStore.data.first()[spotifyPlaylistDetailKey(playlistId)]
        if (!stored.isNullOrBlank()) {
            runCatching {
                spotifyCacheJson.decodeFromString(SpotifyPlaylistDetailCache.serializer(), stored)
            }.getOrNull()?.let { return it }
        }
        return _playlists.value
            .firstOrNull { it.id == playlistId }
            ?.let { SpotifyPlaylistDetailCache(playlist = it) }
    }

    /**
     * One playlist and its tracks: the live copy when the network answers, the
     * last saved copy when it does not.
     *
     * Only a visit that has no saved copy to fall back on is allowed to fail —
     * that is the only case where there is nothing to show.
     */
    suspend fun loadPlaylistDetail(playlistId: String): SpotifyPlaylistDetailCache {
        val cached = cachedPlaylistDetail(playlistId)
        return try {
            val detail =
                SpotifyPlaylistDetailCache(
                    playlist = playlist(playlistId),
                    tracks = playlistTracks(playlistId),
                )
            context.safeDataStoreEdit { prefs ->
                prefs[spotifyPlaylistDetailKey(playlistId)] =
                    spotifyCacheJson.encodeToString(SpotifyPlaylistDetailCache.serializer(), detail)
            }
            detail
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            cached ?: throw error
        }
    }

=======
>>>>>>> origin/main
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
<<<<<<< HEAD
            // The saved playlist details are named after their playlist, so
            // they are found by prefix rather than by key.
            prefs.asMap().keys
                .filter { it.name.startsWith(SpotifyPlaylistDetailCachePrefix) }
                .forEach { prefs.remove(it) }
=======
>>>>>>> origin/main
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
