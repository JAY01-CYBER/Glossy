/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * YouTube as a canvas source: the visualizer clips and Shorts a label uploads
 * for a song are often the only animated artwork that exists for it.
 *
 * Two things decide whether this provider can answer at all, and both are
 * deliberately chosen here rather than left to the client's playback path:
 *
 *  - **The lookup goes through the app's shared [YouTube] client**, not a
 *    private one. It carries the session's visitor data, proxy and cookies, so
 *    the request looks like the rest of the app's traffic instead of an
 *    anonymous burst YouTube is free to answer with a bot check.
 *
 *  - **The stream URL is asked for with clients that work while signed out.**
 *    A canvas is wanted by everyone, and most people never sign into YouTube in
 *    the app. `WEB_REMIX` — the client the player uses for music metadata —
 *    answers `UNPLAYABLE` / "Video unavailable" for ordinary user-uploaded
 *    clips when there is no account behind it, and its streaming data comes
 *    back with no URL at all without a PoToken. `IOS` answers those same clips
 *    with plain progressive URLs that serve bytes without a signature, a token
 *    or a special User-Agent, and it needs no login, so it is asked first. The
 *    remaining clients are fallbacks for the clips it declines.
 *
 * Answers are cached per song for a day; a lookup that fails outright throws
 * [CanvasLookupUnavailable] so the report can tell "the lookup broke" apart
 * from "YouTube had nothing for this song".
 */

package com.jay.glossy.ui.player

import com.jay.glossy.canvas.CanvasArtwork
import com.jay.glossy.canvas.CanvasLookupUnavailable
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.YouTube.SearchFilter
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.response.PlayerResponse
import kotlinx.coroutines.CancellationException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object YouTubeCanvasProvider {
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24

    /** A clip longer than this is a music video, not a looping canvas. */
    private const val MAX_VIDEO_DURATION_SECONDS = 90

    /**
     * A label's own visualizer can run the whole song while it loops the
     * animated artwork, so it is allowed to be longer than a canvas clip — but
     * still bounded, because ten minutes is the most a loop can justify and an
     * hour of it is not one.
     */
    private const val MAX_OFFICIAL_VISUALIZER_SECONDS = 600

    /**
     * How long a "no canvas" answer is remembered.
     *
     * A song YouTube has no animation for otherwise re-runs the whole search
     * chain — six searches, then a player request per surviving candidate —
     * every single time it is played. The empty answer is a fact about the
     * song, so it is cached, briefly.
     */
    private const val NEGATIVE_CACHE_TTL_MS = 1000L * 60L * 30L

    /** Most candidates given the per-video stream-URL lookup, which is slow. */
    private const val MAX_PROBED_CANDIDATES = 5

    /**
     * The clients a stream URL is asked for, in order of how well they answer
     * while signed out. Every one of them is login-free: a canvas must not
     * depend on the account that happens to be connected.
     */
    private val streamClients =
        listOf(
            YouTubeClient.IOS,
            YouTubeClient.ANDROID_VR_NO_AUTH,
            YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
            YouTubeClient.WEB_REMIX,
        )

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        videoId: String? = null,
    ): CanvasArtwork? {
        val key = "$song|$artist|${album ?: ""}".lowercase(Locale.ROOT)
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        // The track's own video first. The app is already playing a YouTube
        // video id, and for any song YouTube Music carries as a real video —
        // an official music video, an artist upload — that id *is* the
        // song's original animation: exact by construction, and one player
        // request instead of five searches. Only the auto-generated "- Topic"
        // uploads (a still image with the audio attached) have nothing to
        // animate, and those fall through to the search below.
        videoId?.let { id ->
            getTrackVideoCanvas(id, song, artist)?.let { result ->
                cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
                return result
            }
        }

        for (query in buildSearchQueries(song, artist)) {
            searchAndExtractVideoUrl(query, song, artist)?.let { result ->
                cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
                return result
            }
        }

        // Remember the empty answer briefly so a song with no canvas is not
        // re-searched on every play.
        cache[key] = CacheEntry(null, System.currentTimeMillis() + NEGATIVE_CACHE_TTL_MS)
        return null
    }

    /** Drops every remembered answer, positive and negative. */
    fun clearCache() {
        cache.clear()
    }

    /**
     * The animation behind the track's own video id, or null when that id has
     * nothing to animate.
     *
     * [MUSIC_VIDEO_TYPE_ATV] is YouTube Music's auto-generated "- Topic"
     * upload: the audio over one still frame, so playing it would freeze the
     * canvas exactly where the still artwork already was. An official music
     * video ([MUSIC_VIDEO_TYPE_OMV]) or an upload from the artist's own
     * channel moves and is accepted; a stranger's upload of the song — however
     * closely its title matches — is somebody's edit, not the song's canvas,
     * and falls through to the search below.
     */
    private suspend fun getTrackVideoCanvas(
        videoId: String,
        song: String,
        artist: String,
    ): CanvasArtwork? {
        if (videoId.isBlank()) return null

        val songTokens =
            normalizeCanvasSongTitle(song)
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

        for (client in streamClients) {
            val response =
                try {
                    YouTube.player(videoId, null, client).getOrNull()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                } ?: continue

            if (response.playabilityStatus.status != "OK") continue
            val details = response.videoDetails ?: continue

            val title = details.title.orEmpty()
            val author = details.author.orEmpty()
            if (details.musicVideoType == MUSIC_VIDEO_TYPE_ATV || author.endsWith("Topic", ignoreCase = true)) return null

            // The id belongs to the song being resolved only when its own
            // title says so — a stale or foreign id must not paint a
            // different song's video under this title.
            if (songTokens.isNotEmpty() && !songTokens.all { title.contains(it, true) }) return null

            // ...and only when the id *is* the song's own release. A stranger's
            // upload of the song — a "person edit", a dance clip, a re-upload —
            // carries the song's name in its title too, and letting it stand in
            // is how somebody's edit became the canvas. An official music video
            // (OMV) is accepted outright; anything else must come from a channel
            // that names the artist or is a VEVO channel. A user upload that is
            // neither is refused, edit or not.
            if (details.musicVideoType != MUSIC_VIDEO_TYPE_OMV && !isOfficialUpload(author, artist)) return null
            if (looksLikeFanEdit(title, song)) return null

            // A canvas is a *loop*, so its length is part of what makes it one.
            // A full-length music video is the opposite of what YouTube Music
            // paints behind a song — the app shows the animated cover and keeps
            // the video for its own watch page — and repeating three minutes of
            // somebody's footage behind the controls is not an animated canvas.
            // Refusing it here sends the song to the providers that answer with
            // the cover itself.
            if ((details.lengthSeconds.toLongOrNull() ?: 0L) > MAX_VIDEO_DURATION_SECONDS) return null

            val videoUrl = bestVideoUrl(response) ?: continue
            return CanvasArtwork(
                name = title,
                artist = author.takeIf { it.isNotBlank() },
                videoUrl = videoUrl,
            )
        }
        return null
    }

    private fun buildSearchQueries(
        song: String,
        artist: String,
    ): List<String> {
        val normalizedSong =
            song
                .replace(Regex("\\s*\\[[^]]*]"), "")
                .replace(Regex("\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)", RegexOption.IGNORE_CASE), "")
                .replace(
                    Regex(
                        "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                        RegexOption.IGNORE_CASE,
                    ),
                    "",
                )
                .replace(Regex("\\s+"), " ")
                .trim()

        val normalizedArtist =
            artist
                .split(
                    Regex(
                        "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                        RegexOption.IGNORE_CASE,
                    ),
                    limit = 2,
                )
                .firstOrNull()
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
                ?: ""

        return listOf(
            "$normalizedArtist $normalizedSong official visualizer",
            "$normalizedArtist $normalizedSong visualizer",
            "$normalizedArtist $normalizedSong canvas",
            "$normalizedArtist $normalizedSong animated artwork",
            "$normalizedArtist $normalizedSong official artwork",
            "$normalizedArtist $normalizedSong #shorts",
            "$normalizedSong visualizer",
            "$normalizedSong #shorts",
        ).distinct()
    }

    /**
     * Titles that mark a clip as somebody's edit, cover or AI-made montage
     * rather than the song's own canvas. The search happily returns fan
     * uploads that carry the song's name — "Rathinamo audio edit", a slowed
     * re-upload, an AI-art montage — and until they were refused the player
     * showed them under the song's title as its canvas.
     *
     * A marker only rejects a clip when the song's own title does not carry
     * the same word, so a track actually called "Edit" does not lose every
     * candidate to this list.
     */
    private val notCanvasMarkers =
        listOf(
            """\baudio\s+edits?\b""",
            """\bedits?\b""",
            """\bcovers?\b""",
            """\bmashups?\b""",
            """\bmedley\b""",
            """\bremix(es)?\b""",
            """\bmix(es)?\b""",
            """\bslowed\b""",
            """\breverb\b""",
            """\bsped\s*-?\s*up\b""",
            """\bnightcore\b""",
            """\bbass\s*-?\s*boost(ed)?\b""",
            """\blo\s*-?\s*fi\b""",
            """\b8d\b""",
            """\bbootleg\b""",
            """\brework\b""",
            """\bflip\b""",
            """\bkaraoke\b""",
            """\binstrumental\b""",
            """\bai\s+(art|images?|video|generated|covers?|edits?)\b""",
            """\bai\b""",
            """\bamv\b""",
            """\bfan\s*-?\s*made\b""",
            """\bcreations?\b""",
            """\breactions?\b""",
            """\blyric(al)?\s+videos?\b""",
            """\bbehind\s+the\s+scenes\b""",
            """\btrailer\b""",
            // A WhatsApp-status clip and a dance/person edit are the two
            // shapes a fan re-upload of a song usually takes, and neither
            // title always carries the word "edit".
            """\bstatus\b""",
            """\bwhats\s*-?\s*app\b""",
            """\bfan\b""",
            """\btributes?\b""",
            """\bparod(y|ies)\b""",
            """\bmeme(s)?\b""",
            """\bediting\b""",
        ).map { Regex(it, RegexOption.IGNORE_CASE) }

    private fun looksLikeFanEdit(
        resultTitle: String,
        songTitle: String,
    ): Boolean =
        notCanvasMarkers.any { marker -> marker.containsMatchIn(resultTitle) && !marker.containsMatchIn(songTitle) } ||
            (isMashupTitle(resultTitle) && !isMashupTitle(songTitle))

    /**
     * A title that mashes two songs together — "SongA x SongB", "SongA × SongB",
     * "SongA vs SongB". The song being resolved is a single track, so a clip that
     * joins it to another song is an edit of two recordings, never this one's own
     * canvas. The same test is applied to the song's own title first, so a track
     * genuinely released as a collaboration ("A x B") is not refused for it.
     */
    private fun isMashupTitle(title: String): Boolean =
        Regex("""\s+[x×]\s+|\s+vs\.?\s+""", RegexOption.IGNORE_CASE).containsMatchIn(title)

    /**
     * Titles that say the clip *is* the song's own release rather than
     * somebody's footage of it: "(Official Video)", "Music Video", "OMV".
     * "Official audio" deliberately does not count — those uploads are a
     * still image over the audio, which has nothing to animate.
     */
    private val ownVideoClaim =
        Regex("""\b(official|music)\s+video\b|\bomv\b|\bmvm\b""", RegexOption.IGNORE_CASE)

    /**
     * Whether the upload looks like the song's own release rather than a fan
     * re-upload: the artist's own channel, a VEVO channel, or YouTube's
     * auto-generated "<Artist> - Topic" channel.
     */
    private fun isOfficialUpload(
        uploader: String,
        artist: String,
    ): Boolean {
        val channel = uploader.lowercase(Locale.ROOT)
        if (artist.isNotBlank() && channel.contains(artist.lowercase(Locale.ROOT))) return true
        if (channel.endsWith("vevo")) return true
        if (channel.endsWith("topic")) return true
        return false
    }

    /** One clip the search found, before it is ranked against the others. */
    private class Candidate(
        val item: SongItem,
        val title: String,
        val uploader: String,
        val claimsOwnAnimation: Boolean,
    )

    /**
     * Searches for video uploads of one query and returns the first clip that
     * validates and yields a URL.
     *
     * A failed search or a search endpoint that refuses the request is reported
     * as [CanvasLookupUnavailable] — the question was never answered, and the
     * caller must not remember that as "this song has no canvas".
     */
    private suspend fun searchAndExtractVideoUrl(
        query: String,
        songValidation: String,
        artistValidation: String,
    ): CanvasArtwork? {
        val videos =
            try {
                YouTube.search(query, SearchFilter.FILTER_VIDEO).getOrThrow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw CanvasLookupUnavailable("YouTube search failed", e)
            }

        val songTokens =
            normalizeCanvasSongTitle(songValidation)
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

        val candidates =
            videos.items.filterIsInstance<SongItem>().mapNotNull { item ->
                val resultTitle = item.title
                val uploader = item.artists.firstOrNull()?.name.orEmpty()

                // A "- Topic" upload is YouTube Music's auto-generated audio
                // over one still frame — the same upload the direct path
                // refuses — and it ranks first here as an "official" channel,
                // so it must be refused too or a frozen artwork wins as the
                // canvas.
                if (uploader.endsWith("Topic", ignoreCase = true)) return@mapNotNull null

                // Every word of the song's own title has to appear in the
                // clip's title. The substring test this replaces let a clip
                // through on one shared word, which is how a canvas belonging
                // to another song reached this song's player.
                if (songTokens.isNotEmpty() && !songTokens.all { resultTitle.contains(it, true) }) return@mapNotNull null

                // The name in the video's *title* counts as the artist here. A
                // visualizer is uploaded under a label as often as under the
                // artist's own account, and the uploader name is then either a
                // different entity or a misspelling — "Talwiinder" against an
                // uploader called "Talvinder" — so demanding the uploader match
                // rejected exactly the clips this provider exists to find.
                val artistAgrees =
                    artistValidation.isBlank() ||
                        uploader.contains(artistValidation, true) ||
                        resultTitle.contains(artistValidation, true)
                if (!artistAgrees) return@mapNotNull null

                if (looksLikeFanEdit(resultTitle, songValidation)) return@mapNotNull null

                // A title that claims to *be* the song's own animation or
                // release: a visualizer, a canvas clip, an official music
                // video. "#shorts" deliberately does not count — it only says
                // the clip is short, and a stranger's Short of somebody
                // dancing to the song is exactly the fan edit that must not
                // win here.
                val claimsOwnAnimation =
                    resultTitle.contains("visualizer", true) ||
                        resultTitle.contains("canvas", true) ||
                        ownVideoClaim.containsMatchIn(resultTitle) ||
                        item.musicVideoType == MUSIC_VIDEO_TYPE_OMV

                // Length is a hard part of the answer, not a tie-break. This
                // used to be waived for anything whose title claimed to be the
                // song's own video, so an "(Official Video)" entry — minutes of
                // footage with people in it — passed the canvas window and
                // became the animation. A claimed title changes *whose* clip it
                // is, never *what kind* of clip it is; only a loop is a canvas.
                // An unknown length is left to the checks above rather than
                // refused on a number the search did not give.
                val durationSeconds = item.duration ?: 0
                if (durationSeconds > MAX_VIDEO_DURATION_SECONDS) {
                    // The one exception to the canvas window: a label's own
                    // visualizer, which loops the animated artwork for the
                    // whole song. It must say so in its title *and* come from
                    // the song's own channel, and an "(Official Video)" never
                    // qualifies — that is footage, not an animated cover —
                    // because ownVideoClaim is consulted first.
                    val claimsOfficialVisualizer =
                        !ownVideoClaim.containsMatchIn(resultTitle) &&
                            (
                                resultTitle.contains("visualizer", true) ||
                                    resultTitle.contains("canvas", true) ||
                                    resultTitle.contains("animated artwork", true)
                            ) &&
                            isOfficialUpload(uploader, artistValidation)
                    if (!claimsOfficialVisualizer || durationSeconds > MAX_OFFICIAL_VISUALIZER_SECONDS) {
                        return@mapNotNull null
                    }
                }

                // A channel that is not the song's own — no artist name, no
                // VEVO, no OMV type — may only stand in when the title
                // explicitly claims to be the song's own animation. A bare
                // re-upload from a stranger is somebody's edit of the song
                // however closely the title matches, and it is what used to
                // show up as the canvas.
                if (!isOfficialUpload(uploader, artistValidation) && !claimsOwnAnimation) return@mapNotNull null

                Candidate(item, resultTitle, uploader, claimsOwnAnimation)
            }

        // The song's own uploads come first, then clips whose title names the
        // artist, and only then anything left that matched — a title that
        // claims the song's own animation wins a tie. Taking the first search
        // hit as-is is what let a fan edit outrank the label's own canvas.
        val ordered =
            candidates.sortedWith(
                compareBy<Candidate> { candidate ->
                    when {
                        isOfficialUpload(candidate.uploader, artistValidation) -> 0
                        artistValidation.isNotBlank() && candidate.title.contains(artistValidation, true) -> 1
                        else -> 2
                    }
                }.thenByDescending { it.claimsOwnAnimation },
            )

        // Each candidate costs a player request per client, so only the best
        // few are probed; the ordering above already put the song's own
        // uploads at the front.
        for (candidate in ordered.take(MAX_PROBED_CANDIDATES)) {
            val videoUrl = getVideoStreamUrl(candidate.item.id)
            if (videoUrl.isNullOrBlank()) continue
            return CanvasArtwork(
                name = candidate.title,
                // Reported as the artist's when the channel or the title says
                // so, so the canvas check downstream has a name to judge
                // instead of always falling back to the title alone.
                artist =
                    candidate.uploader
                        .takeIf { it.isNotBlank() && it.contains(artistValidation, true) }
                        ?: artistValidation.takeIf { it.isNotBlank() && candidate.title.contains(it, true) },
                videoUrl = videoUrl,
            )
        }

        return null
    }

    /**
     * The first direct video URL any login-free client is willing to hand over.
     *
     * Clients are tried in turn rather than raced: a refusal from one says
     * nothing about the next, and the whole sequence costs a handful of
     * requests that are only paid once per song thanks to the cache above.
     */
    private suspend fun getVideoStreamUrl(videoId: String): String? {
        for (client in streamClients) {
            val response =
                try {
                    YouTube.player(videoId, null, client).getOrNull()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            if (response == null || response.playabilityStatus.status != "OK") continue
            bestVideoUrl(response)?.let { return it }
        }
        return null
    }

    /**
     * The highest-quality video-only rendition this response can actually hand
     * over as a playable address.
     *
     * Renditions are walked from the largest down and the first that yields a
     * usable URL wins. Picking the single largest with `maxByOrNull` and then
     * trying to decrypt only that one was the quality problem: the very top
     * rendition is often delivered solely as a signature cipher whose signature
     * this cannot compute, so the whole client was abandoned — and with it every
     * lower rendition beneath it that carried a plain, playable URL. Walking the
     * list keeps the best quality that truly plays instead of throwing the
     * client away for the sake of one unplayable top entry.
     */
    private fun bestVideoUrl(response: PlayerResponse): String? {
        val streaming = response.streamingData

        // A plain-file rendition first, when the client offers one. A canvas is
        // read in a loop — seek, play, wrap around — so the address it plays
        // from has to behave like a file: one address, range-capable, whole.
        // The adaptive video-only entries are transport-fragmented streams;
        // they are a fine way to drive a one-shot watch, and a poor one to keep
        // a loop alive on. Resolution is the price and it is a cheap one: a
        // canvas is drawn at artwork size, not full screen.
        playableUrlFrom(streaming?.formats.orEmpty())?.let { return it }
        return playableUrlFrom(streaming?.adaptiveFormats.orEmpty())
    }

    /** The largest rendition in [formats] that yields a usable address. */
    private fun playableUrlFrom(formats: List<PlayerResponse.StreamingData.Format>): String? {
        val videoFormats =
            formats
                .filter { !it.isAudio && it.mimeType.startsWith("video/") }
                .filter { it.width != null && it.height != null }
                .sortedByDescending { (it.height ?: 0) * (it.width ?: 0) }

        for (format in videoFormats) {
            format.url?.takeIf { it.isNotBlank() }?.let { return it }
            format.signatureCipher?.let { decryptSignature(it) }?.let { return it }
            format.cipher?.let { decryptSignature(it) }?.let { return it }
        }
        return null
    }

    /**
     * Last resort for a client that answers with a ciphered address anyway. The
     * players the app uses for playback de-obfuscate these properly; this only
     * keeps the plain `&signature=` form working for the rare format that
     * arrives that way, and returns null when the cipher would need real
     * signature deciphering.
     */
    private fun decryptSignature(cipher: String): String? {
        val params =
            cipher.split("&").mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) null else part.substring(0, separator) to part.substring(separator + 1)
            }.toMap()

        val signature = params["s"] ?: params["sig"] ?: params["signature"] ?: return null
        // A real `s` needs the player's signature function; a `sig` is already
        // the finished value.
        if (params["s"] != null && params["sig"] == null && params["signature"] == null) return null
        val url = params["url"] ?: return null
        return "$url&signature=$signature"
    }
}
