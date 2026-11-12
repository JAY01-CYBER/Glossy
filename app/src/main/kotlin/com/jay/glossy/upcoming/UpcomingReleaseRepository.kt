/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.upcoming

import android.content.Context
import android.os.SystemClock
import com.jay.glossy.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.io.File
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Finds an artist's next announced release in the MusicBrainz calendar.
 *
 * YouTube Music does not publish pre-release albums through the InnerTube API,
 * so this is the one part of the artist page that comes from somewhere else.
 * MusicBrainz is free and keyless, but it asks clients to make no more than one
 * request per second and to identify themselves, so every call goes through a
 * single throttled lane and carries the app's User-Agent.
 *
 * Answers are cached for a day — including "nothing announced", so an artist
 * without a release does not get re-queried on every visit. The cache lives in
 * cacheDir: losing it only costs one lookup.
 */
@Singleton
class UpcomingReleaseRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val client =
            HttpClient(OkHttp) {
                expectSuccess = false
                install(HttpTimeout) {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
                engine {
                    config {
                        retryOnConnectionFailure(true)
                    }
                }
            }

        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            }

        private val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)

        private val requestMutex = Mutex()
        private var lastRequestAtMillis = 0L

        private val cacheMutex = Mutex()
        private var cache: MutableMap<String, CachedReleaseLookup>? = null

        /**
         * The nearest announced release for [artistName], or `null` when the
         * artist has none. A failed lookup is returned as a failure instead of
         * an empty answer so a network problem is never cached as "nothing
         * announced".
         */
        suspend fun findUpcomingRelease(artistName: String): Result<UpcomingRelease?> =
            withContext(Dispatchers.IO) {
                val cacheKey = normalizeArtistName(artistName)
                if (cacheKey.isEmpty()) return@withContext Result.success(null)

                val cached =
                    cacheMutex.withLock {
                        loadCacheLocked()[cacheKey]?.takeIf { isFresh(it) }
                    }
                if (cached != null) {
                    val release = cached.release?.toModel()
                    // A release that has come out since the lookup was cached
                    // is no longer upcoming, so ask again instead of leaving a
                    // "coming today" card on the page.
                    val stillUpcoming =
                        cached.release == null ||
                            (release != null && release.releaseDate.isAfter(LocalDate.now()))
                    if (stillUpcoming) return@withContext Result.success(release)
                }

                try {
                    val release = lookupUpcomingRelease(artistName)
                    cacheMutex.withLock { putCacheLocked(cacheKey, release) }
                    Result.success(release)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    Timber.tag(TAG).w(throwable, "Could not look up an upcoming release for %s", artistName)
                    // Offline or MusicBrainz is down: an expired answer is
                    // still better than making the card vanish mid-session.
                    val stale = cacheMutex.withLock { loadCacheLocked()[cacheKey]?.release?.toModel() }
                    if (stale != null && stale.releaseDate.isAfter(LocalDate.now())) {
                        Result.success(stale)
                    } else {
                        Result.failure(throwable)
                    }
                }
            }

        private suspend fun lookupUpcomingRelease(artistName: String): UpcomingRelease? {
            val artistId = findArtistId(artistName) ?: return null
            val today = LocalDate.now()

            val nearest =
                fetchUpcomingReleaseGroups(artistId)
                    .asSequence()
                    .mapNotNull { group ->
                        val date = parseCompleteDate(group.firstReleaseDate) ?: return@mapNotNull null
                        if (!date.isAfter(today)) return@mapNotNull null
                        if (group.secondaryTypes.any(EXCLUDED_SECONDARY_TYPES::contains)) {
                            return@mapNotNull null
                        }
                        // A null primary type is common for releases that have
                        // only just been announced; the track count below fills
                        // the label in when MusicBrainz has not typed it yet.
                        if (group.primaryType != null && group.primaryType.toReleaseType() == null) {
                            return@mapNotNull null
                        }
                        ReleaseCandidate(
                            musicBrainzId = group.id,
                            title = group.title,
                            releaseDate = date,
                            primaryType = group.primaryType,
                        )
                    }
                    .minByOrNull { it.releaseDate }
                    ?: return null

            val trackCount = fetchTrackCount(nearest.musicBrainzId)
            return UpcomingRelease(
                musicBrainzId = nearest.musicBrainzId,
                title = nearest.title,
                artistName = artistName,
                releaseDate = nearest.releaseDate,
                type = nearest.primaryType.toReleaseType() ?: inferReleaseType(trackCount),
                trackCount = trackCount,
            )
        }

        /**
         * MusicBrainz search can return similarly named acts, so only an exact
         * normalized name match is accepted. Ties go to the highest search
         * score, which is the act MusicBrainz itself considers the best match.
         */
        private suspend fun findArtistId(artistName: String): String? {
            val normalizedName = normalizeArtistName(artistName)
            if (normalizedName.isEmpty()) return null

            val escapedName = artistName.replace("\"", "\\\"")
            val response =
                json.decodeFromString<ArtistSearchResponse>(
                    requestJsonBody(
                        path = "/artist",
                        parameters =
                            mapOf(
                                "query" to "artist:\"$escapedName\"",
                                "limit" to ARTIST_SEARCH_LIMIT.toString(),
                            ),
                    ),
                )

            return response.artists
                .asSequence()
                .filter { normalizeArtistName(it.name) == normalizedName }
                .maxByOrNull { it.score }
                ?.takeIf { it.score >= MIN_ARTIST_SCORE }
                ?.id
        }

        private suspend fun fetchUpcomingReleaseGroups(artistId: String): List<MbReleaseGroup> =
            json
                .decodeFromString<ReleaseGroupBrowseResponse>(
                    requestJsonBody(
                        path = "/release-group",
                        parameters =
                            mapOf(
                                "artist" to artistId,
                                "limit" to RELEASE_GROUP_LIMIT.toString(),
                            ),
                    ),
                ).releaseGroups

        /**
         * Track counts live on the releases inside a group, not on the group
         * itself, so this is one extra request. A release group can contain
         * several editions; the first official edition that reports a count is
         * the one used.
         */
        private suspend fun fetchTrackCount(releaseGroupId: String): Int? =
            try {
                val response =
                    json.decodeFromString<ReleaseBrowseResponse>(
                        requestJsonBody(
                            path = "/release",
                            parameters =
                                mapOf(
                                    "release-group" to releaseGroupId,
                                    "inc" to "media",
                                    "limit" to RELEASE_LIMIT.toString(),
                                ),
                        ),
                    )

                response.releases
                    .sortedWith(
                        compareBy<MbRelease> { it.status != OFFICIAL_RELEASE_STATUS }
                            .thenBy { it.date.orEmpty() },
                    )
                    .firstNotNullOfOrNull { release ->
                        release.media.sumOf { it.trackCount ?: 0 }.takeIf { it > 0 }
                    }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Timber.tag(TAG).w(throwable, "Could not read the track count for %s", releaseGroupId)
                null
            }

        private suspend fun requestJsonBody(
            path: String,
            parameters: Map<String, String>,
        ): String =
            requestMutex.withLock {
                val elapsed = SystemClock.elapsedRealtime() - lastRequestAtMillis
                if (elapsed in 0 until MIN_REQUEST_INTERVAL_MILLIS) {
                    delay(MIN_REQUEST_INTERVAL_MILLIS - elapsed)
                }

                val response =
                    client.get("$MUSIC_BRAINZ_BASE_URL$path") {
                        parameter("fmt", "json")
                        parameters.forEach { (name, value) -> parameter(name, value) }
                        header(HttpHeaders.UserAgent, USER_AGENT)
                        header(HttpHeaders.Accept, "application/json")
                    }
                lastRequestAtMillis = SystemClock.elapsedRealtime()

                if (!response.status.isSuccess()) {
                    throw IOException("MusicBrainz answered ${response.status.value} for $path")
                }
                response.bodyAsText()
            }

        private fun isFresh(entry: CachedReleaseLookup): Boolean =
            System.currentTimeMillis() - entry.fetchedAtMillis < CACHE_TTL_MILLIS

        private fun loadCacheLocked(): MutableMap<String, CachedReleaseLookup> {
            cache?.let { return it }

            val loaded =
                runCatching {
                    if (!cacheFile.exists()) {
                        LinkedHashMap<String, CachedReleaseLookup>()
                    } else {
                        json
                            .decodeFromString<UpcomingReleaseCache>(cacheFile.readText())
                            .entries
                            .associateByTo(
                                LinkedHashMap<String, CachedReleaseLookup>(),
                                CachedReleaseLookup::artistKey,
                            )
                    }
                }.getOrElse { throwable ->
                    Timber.tag(TAG).w(throwable, "Discarding the unreadable upcoming-release cache")
                    LinkedHashMap<String, CachedReleaseLookup>()
                }

            cache = loaded
            return loaded
        }

        private fun putCacheLocked(
            cacheKey: String,
            release: UpcomingRelease?,
        ) {
            val entries = loadCacheLocked()
            entries[cacheKey] =
                CachedReleaseLookup(
                    artistKey = cacheKey,
                    fetchedAtMillis = System.currentTimeMillis(),
                    release = release?.toCached(),
                )

            val trimmed =
                entries.entries
                    .sortedByDescending { it.value.fetchedAtMillis }
                    .take(MAX_CACHE_ENTRIES)
                    .associateTo(LinkedHashMap<String, CachedReleaseLookup>()) { it.key to it.value }

            cache = trimmed
            writeCacheLocked(trimmed)
        }

        private fun writeCacheLocked(entries: Map<String, CachedReleaseLookup>) {
            runCatching {
                val temporaryFile = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
                temporaryFile.writeText(
                    json.encodeToString(UpcomingReleaseCache(entries.values.toList())),
                )
                if (!temporaryFile.renameTo(cacheFile)) {
                    temporaryFile.copyTo(cacheFile, overwrite = true)
                    temporaryFile.delete()
                }
            }.onFailure { throwable ->
                Timber.tag(TAG).w(throwable, "Could not write the upcoming-release cache")
            }
        }

        private companion object {
            const val TAG = "UpcomingRelease"
            const val MUSIC_BRAINZ_BASE_URL = "https://musicbrainz.org/ws/2"
            const val CACHE_FILE_NAME = "upcoming_releases.json"
            const val CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000L
            const val MAX_CACHE_ENTRIES = 100
            const val MIN_REQUEST_INTERVAL_MILLIS = 1_050L
            const val REQUEST_TIMEOUT_MILLIS = 15_000L
            const val CONNECT_TIMEOUT_MILLIS = 10_000L
            const val ARTIST_SEARCH_LIMIT = 5
            const val MIN_ARTIST_SCORE = 80
            const val RELEASE_GROUP_LIMIT = 100
            const val RELEASE_LIMIT = 25
            const val OFFICIAL_RELEASE_STATUS = "Official"

            val USER_AGENT = "Glossy/${BuildConfig.VERSION_NAME} (https://github.com/NullTrigger/Glossy)"
            val EXCLUDED_SECONDARY_TYPES =
                setOf(
                    "Interview",
                    "Audiobook",
                    "Audio drama",
                    "Spokenword",
                )
        }
    }

private fun parseCompleteDate(value: String?): LocalDate? {
    if (value.isNullOrBlank() || value.length != 10) return null
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

/**
 * An announced release group before its track count is known. Kept separate
 * from [UpcomingRelease] because the type may still be undecided at this point.
 */
private data class ReleaseCandidate(
    val musicBrainzId: String,
    val title: String,
    val releaseDate: LocalDate,
    val primaryType: String?,
)

/**
 * Guesses the shape of a release that MusicBrainz has not typed yet from the
 * number of tracks, using the usual album/EP/single split.
 */
private fun inferReleaseType(trackCount: Int?): ReleaseType =
    when {
        trackCount == null -> ReleaseType.ALBUM
        trackCount <= 3 -> ReleaseType.SINGLE
        trackCount <= 6 -> ReleaseType.EP
        else -> ReleaseType.ALBUM
    }

private fun String?.toReleaseType(): ReleaseType? =
    when (this) {
        "Album" -> ReleaseType.ALBUM
        "EP" -> ReleaseType.EP
        "Single" -> ReleaseType.SINGLE
        else -> null
    }

private fun UpcomingRelease.toCached(): CachedRelease =
    CachedRelease(
        musicBrainzId = musicBrainzId,
        title = title,
        artistName = artistName,
        releaseDate = releaseDate.toString(),
        type = type.name,
        trackCount = trackCount,
    )

private fun CachedRelease.toModel(): UpcomingRelease? {
    val date = runCatching { LocalDate.parse(releaseDate) }.getOrNull() ?: return null
    val releaseType = runCatching { ReleaseType.valueOf(type) }.getOrNull() ?: return null
    return UpcomingRelease(
        musicBrainzId = musicBrainzId,
        title = title,
        artistName = artistName,
        releaseDate = date,
        type = releaseType,
        trackCount = trackCount,
    )
}

@Serializable
private data class ArtistSearchResponse(
    val artists: List<MbArtist> = emptyList(),
)

@Serializable
private data class MbArtist(
    val id: String,
    val name: String,
    val score: Int = 0,
)

@Serializable
private data class ReleaseGroupBrowseResponse(
    @SerialName("release-groups") val releaseGroups: List<MbReleaseGroup> = emptyList(),
)

@Serializable
private data class MbReleaseGroup(
    val id: String,
    val title: String,
    @SerialName("primary-type") val primaryType: String? = null,
    @SerialName("secondary-types") val secondaryTypes: List<String> = emptyList(),
    @SerialName("first-release-date") val firstReleaseDate: String? = null,
)

@Serializable
private data class ReleaseBrowseResponse(
    val releases: List<MbRelease> = emptyList(),
)

@Serializable
private data class MbRelease(
    val status: String? = null,
    val date: String? = null,
    val media: List<MbMedium> = emptyList(),
)

@Serializable
private data class MbMedium(
    @SerialName("track-count") val trackCount: Int? = null,
)

@Serializable
private data class UpcomingReleaseCache(
    val entries: List<CachedReleaseLookup> = emptyList(),
)

@Serializable
private data class CachedReleaseLookup(
    val artistKey: String,
    val fetchedAtMillis: Long,
    val release: CachedRelease? = null,
)

@Serializable
private data class CachedRelease(
    val musicBrainzId: String,
    val title: String,
    val artistName: String,
    val releaseDate: String,
    val type: String,
    val trackCount: Int? = null,
)
