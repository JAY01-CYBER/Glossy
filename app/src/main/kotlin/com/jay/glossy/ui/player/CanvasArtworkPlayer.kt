/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player

import android.content.Context
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.jay.glossy.constants.CanvasCacheMode
import com.jay.glossy.constants.CanvasCacheModeKey
import com.jay.glossy.utils.rememberEnumPreference
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale

/** How many times a canvas that errors mid-track is prepared again. */
private const val MaxCanvasRetries = 3

/** Pause before a retry, so a flapping network is not hammered. */
private const val CanvasRetryDelayMillis = 1200L

/**
 * How long after the window comes back an error still reads as a *surface*
 * problem rather than a *clip* problem.
 *
 * Returning from the background is not instant: the Activity window is rebuilt,
 * a brand-new TextureView is attached, and the decoder is handed a surface that
 * did not exist a moment ago. Anything the renderer complains about inside that
 * window is about the surface coming up.
 *
 * Counting those as clip failures is what used to cost a canvas the rest of the
 * track. Three of them exhausted the retry budget, and the last one wrote
 * `UNPLAYABLE` against the URL — which [CanvasVerifier] then trusted for half an
 * hour and used to refuse that very clip. Because the health cache is keyed by
 * URL and shared, one background trip killed the canvas in the player *and* the
 * Spotlight card at once, and only a restart brought it back. A transient race
 * on a dying window must never be able to write a verdict about a URL.
 */
private const val SurfaceSettleMillis = 1_500L

/**
 * How the player behaves while the app is in the background.
 *
 * A canvas is a looping video drawn into a [TextureView], and a TextureView's
 * SurfaceTexture lives on the Activity's window: the moment that window is
 * released — which is what stopping an app does — the decoder has nowhere to
 * draw. The renderer then reports an error, the player spends its whole retry
 * budget on a screen nobody can see, and it comes back to the foreground having
 * already given up on the clip: the animated canvas is gone for the rest of the
 * track, which is exactly the "canvas does not load after the app was in the
 * background" report.
 *
 * So the canvas now follows the app's lifecycle: it pauses while the app is
 * stopped and spends no retries there, and on the way back it is given a fresh
 * TextureView, a fresh retry budget and a re-prepare. Measured cold start is
 * untouched — nothing here happens until the app has actually been stopped.
 */
private enum class CanvasForegroundState { FOREGROUND, BACKGROUND }

object CanvasCacheManager {
    private var videoCache: SimpleCache? = null
    private var okHttpClient: OkHttpClient? = null

    @Synchronized
    fun getVideoCache(context: Context): SimpleCache {
        if (videoCache == null) {
            val cacheDir = File(context.filesDir, "canvas_video_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(256 * 1024 * 1024L)
            val databaseProvider = StandaloneDatabaseProvider(context)
            videoCache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return videoCache!!
    }

    fun clearVideoCache(context: Context) {
        videoCache?.release()
        videoCache = null
        File(context.filesDir, "canvas_video_cache").deleteRecursively()
    }

    /**
     * The network source the player (and the prefetcher's cache writer) both
     * read through. Shared so a canvas is fetched over one client, with one set
     * of timeouts and one connection pool.
     */
    @Synchronized
    fun getUpstreamDataSourceFactory(context: Context): DataSource.Factory {
        if (okHttpClient == null) {
            okHttpClient = OkHttpClient.Builder().build()
        }
        return DefaultDataSource.Factory(context, OkHttpDataSource.Factory(okHttpClient!!))
    }

    fun getMediaSourceFactory(context: Context, enableVideoCache: Boolean): DefaultMediaSourceFactory {
        val upstreamFactory = getUpstreamDataSourceFactory(context)

        return if (enableVideoCache) {
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(getVideoCache(context))
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            DefaultMediaSourceFactory(cacheDataSourceFactory)
        } else {
            DefaultMediaSourceFactory(upstreamFactory)
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun CanvasArtworkPlayer(
    primaryUrl: String?,
    fallbackUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onVideoReady: (Boolean) -> Unit = {} 
) {
    val context = LocalContext.current
    val primary = primaryUrl?.takeIf { it.isNotBlank() }
    val fallback = fallbackUrl?.takeIf { it.isNotBlank() }
    val initial = primary ?: fallback ?: return
    
    var currentUrl by remember(initial) { mutableStateOf(initial) }
    var isVideoReady by remember(initial) { mutableStateOf(false) }
    // A canvas that errors part-way through a track used to stay dead for the
    // rest of the song (one black frame while the static artwork stayed faded
    // out). Retrying re-prepares the same URL a bounded number of times, and
    // once the budget is gone the caller is told the video is not ready so the
    // artwork comes back.
    var reloadTick by remember(initial) { mutableIntStateOf(0) }
    var retriesUsed by remember(initial) { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    // Which "generation" of the video view is alive. Bumped when the app comes
    // back to the foreground so the AndroidView below builds a brand-new
    // TextureView (with a brand-new SurfaceTexture) for the re-prepare that runs
    // alongside it. See [CanvasForegroundState].
    var videoViewGeneration by remember(initial) { mutableIntStateOf(0) }
    // Held in state, not read from the lifecycle on demand: the player's error
    // listener has to know whether an error is worth a retry, and an error that
    // arrives while the app is stopped says nothing about the clip.
    var foregroundState by remember(initial) {
        mutableStateOf(
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                CanvasForegroundState.FOREGROUND
            } else {
                CanvasForegroundState.BACKGROUND
            },
        )
    }
    // Wall-clock deadline for the window described by [SurfaceSettleMillis].
    // [Long.MAX_VALUE] while the app is stopped, so an error that slips in
    // before the pause lands is still read as a surface problem. Read from
    // callbacks only, never from composition, so it stays out of recomposition.
    var surfaceSettleUntil by remember(initial) { mutableLongStateOf(0L) }

    // YAHAN FIX KIYA: Video ka aspect ratio track karne ke liye variable add kiya
    var videoAspectRatio by remember(initial) { mutableFloatStateOf(1f) }

    val (canvasCacheMode) = rememberEnumPreference(CanvasCacheModeKey, defaultValue = CanvasCacheMode.VIDEO_AND_URL)
    val enableVideoCache = canvasCacheMode == CanvasCacheMode.VIDEO_AND_URL

    val mediaSourceFactory = remember(enableVideoCache) {
        CanvasCacheManager.getMediaSourceFactory(context, enableVideoCache)
    }

    val exoPlayer = remember(initial) {
        val loadControl = DefaultLoadControl.Builder()
            // Larger buffer: canvas clips are short loops, so keeping a few
            // seconds fully buffered avoids mid-loop stutter on slow networks.
            .setBufferDurationsMs(2000, 15000, 500, 2000)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                // High-quality canvas: prefer the best video quality available and
                // only step down if the device reports decoder performance problems.
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .setMinVideoBitrate(1_500_000)
                    .build()
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                    false
                )
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = isPlaying
            }
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        // Adding an observer replays the events the owner has already reached,
        // so the flag (not the event) decides whether this was a real return to
        // the foreground: a cold start sees ON_START without ever having stopped.
        var wasStopped = false
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        wasStopped = true
                        foregroundState = CanvasForegroundState.BACKGROUND
                        // Detach *before* the window dies. A TextureView's
                        // SurfaceTexture lives on the Activity window, so
                        // leaving the player bound to a view whose surface is
                        // about to be destroyed is what produced the error all
                        // of this exists to contain. Detaching first leaves the
                        // renderer nothing to fail against.
                        exoPlayer.setVideoTextureView(null)
                        // No surface to decode into and nothing to show: stop
                        // burning battery on frames nobody can see.
                        exoPlayer.pause()
                        // Anything still in flight belongs to the old window.
                        surfaceSettleUntil = Long.MAX_VALUE
                    }

                    Lifecycle.Event.ON_START -> {
                        foregroundState = CanvasForegroundState.FOREGROUND
                        if (wasStopped) {
                            wasStopped = false
                            // Rebuild the TextureView and re-prepare the clip:
                            // the window this player was drawing into is gone.
                            videoViewGeneration++
                            // A fresh budget and a settle window, both because
                            // whatever failed, failed on a window that no
                            // longer exists.
                            retriesUsed = 0
                            surfaceSettleUntil = System.currentTimeMillis() + SurfaceSettleMillis
                        }
                    }

                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(exoPlayer, primary, fallback) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Checked before anything else, because every branch below ends
                // in a judgement about the *clip*: one spends a retry, the other
                // writes UNPLAYABLE. A surface that is gone, or still coming
                // back up, explains an error on its own and is not evidence
                // about the URL. Stay quiet and let the foreground pass
                // re-prepare with a full budget.
                if (foregroundState == CanvasForegroundState.BACKGROUND ||
                    System.currentTimeMillis() < surfaceSettleUntil
                ) {
                    isVideoReady = false
                    onVideoReady(false)
                    return
                }

                val next = if (currentUrl == primary) fallback else null
                if (!next.isNullOrBlank()) {
                    currentUrl = next
                    isVideoReady = false
                    onVideoReady(false)
                } else if (retriesUsed < MaxCanvasRetries) {
                    retriesUsed++
                    isVideoReady = false
                    onVideoReady(false)
                    reloadTick++
                } else {
                    // Out of retries: let the static artwork return rather than
                    // leaving the player showing an empty canvas frame.
                    //
                    // This is also the most honest canvas check there is: the
                    // clip was fed to a real decoder, three times, and refused
                    // to draw. Remembering it means the next lookup for this
                    // song refuses the URL outright (CanvasVerifier) instead of
                    // walking here again — and the providers get asked for
                    // something that works.
                    CanvasVerifier.rememberHealth(currentUrl, CanvasUrlHealth.UNPLAYABLE)
                    isVideoReady = false
                    onVideoReady(false)
                }
            }
            override fun onRenderedFirstFrame() {
                // A frame was decoded and drawn: the URL serves a video, which
                // is the answer the whole check is after. Cheap to record and it
                // saves the background probe a request later.
                CanvasVerifier.rememberHealth(currentUrl, CanvasUrlHealth.PLAYABLE)
                // A frame decoded into a window that is not on screen proves the
                // clip is fine but says nothing about what can be seen. Fading
                // the canvas in here would show an empty surface, so the claim
                // waits for a first frame that lands on a live window.
                if (foregroundState == CanvasForegroundState.BACKGROUND) return
                isVideoReady = true
                onVideoReady(true)
            }
            // YAHAN FIX KIYA: Video ki actual height/width get karke ratio update karna
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        
        // Initial check in case player already knows the size
        if (exoPlayer.videoSize.width > 0 && exoPlayer.videoSize.height > 0) {
            videoAspectRatio = exoPlayer.videoSize.width.toFloat() / exoPlayer.videoSize.height
        }
        
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(currentUrl, exoPlayer, reloadTick, videoViewGeneration) {
        if (reloadTick > 0) delay(CanvasRetryDelayMillis)
        // Back from the background: the clip deserves the retry budget again —
        // whatever it failed with happened on a window that no longer exists.
        if (videoViewGeneration > 0 && foregroundState == CanvasForegroundState.FOREGROUND) {
            retriesUsed = 0
        }
        val normalizedUrl = currentUrl.trim()
        val mimeType = if (normalizedUrl.contains(".m3u8", true) || normalizedUrl.lowercase(Locale.ROOT).split('?').first().endsWith(".m3u8")) {
            MimeTypes.APPLICATION_M3U8
        } else {
            MimeTypes.VIDEO_MP4
        }

        exoPlayer.stop()
        isVideoReady = false
        onVideoReady(false)
        exoPlayer.setMediaItem(MediaItem.Builder().setUri(normalizedUrl).setMimeType(mimeType).build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlaying
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "canvasAlpha"
    )

    // Keyed on the generation: after a trip to the background the whole view is
    // rebuilt rather than reused, because the old TextureView's SurfaceTexture
    // died with the Activity's window. A fresh view is attached and handed to
    // the player exactly the way it is on a cold start, which is the one code
    // path already known to put a canvas on screen.
    key(videoViewGeneration) {
        AndroidView(
            factory = { viewContext ->
                AspectRatioFrameLayout(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    val textureView = TextureView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        isOpaque = false
                    }
                    addView(textureView)
                    exoPlayer.setVideoTextureView(textureView)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { view ->
                // YAHAN FIX KIYA: AndroidView ko video ka asli ratio pass karna jisse wo stretch na ho!
                view.setAspectRatio(videoAspectRatio)
            },
            modifier = modifier.alpha(alpha),
        )
    }
}
