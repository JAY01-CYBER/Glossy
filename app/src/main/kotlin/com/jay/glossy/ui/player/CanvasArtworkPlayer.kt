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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
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
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale

// Yahan se singleton ExoPlayer hata diya gaya hai taaki ghost frames na aaye.
// CacheManager ab sirf video ki storage file (SimpleCache) sambhalega.
object CanvasCacheManager {
    private var videoCache: SimpleCache? = null
    private var okHttpClient: OkHttpClient? = null

    @Synchronized
    fun getVideoCache(context: Context): SimpleCache {
        if (videoCache == null) {
            val cacheDir = File(context.filesDir, "canvas_video_cache")
            // 256MB LRU Cache jisse bandwidth bache
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

    fun getMediaSourceFactory(context: Context, enableVideoCache: Boolean): DefaultMediaSourceFactory {
        if (okHttpClient == null) {
            okHttpClient = OkHttpClient.Builder().build()
        }
        val upstreamFactory = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(okHttpClient!!))

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
) {
    val context = LocalContext.current
    val primary = primaryUrl?.takeIf { it.isNotBlank() }
    val fallback = fallbackUrl?.takeIf { it.isNotBlank() }
    val initial = primary ?: fallback ?: return
    
    // M3Play ki tarah isko locally maintain kar rahe hain taki state fresh rahe
    var currentUrl by remember(initial) { mutableStateOf(initial) }
    var isVideoReady by remember(initial) { mutableStateOf(false) }
    var videoAspectRatio by remember(initial) { mutableStateOf(1f) }

    val (canvasCacheMode) = rememberEnumPreference(CanvasCacheModeKey, defaultValue = CanvasCacheMode.VIDEO_AND_URL)
    val enableVideoCache = canvasCacheMode == CanvasCacheMode.VIDEO_AND_URL

    val mediaSourceFactory = remember(enableVideoCache) {
        CanvasCacheManager.getMediaSourceFactory(context, enableVideoCache)
    }

    // Naya gaana aate hi completely naya player banega (No ghost frames!)
    val exoPlayer = remember(initial) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(500, 5000, 100, 500)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                trackSelectionParameters = trackSelectionParameters.buildUpon().setForceHighestSupportedBitrate(true).build()
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

    // Play/Pause state ko live sync karna
    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Video render hone par listeners set karna
    DisposableEffect(exoPlayer, primary, fallback) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val next = if (currentUrl == primary) fallback else null
                if (!next.isNullOrBlank()) {
                    currentUrl = next
                    isVideoReady = false // Fallback URL try karte time animation zero kar do
                }
            }
            override fun onRenderedFirstFrame() {
                isVideoReady = true // Ekdam smooth entry trigger hogi yahan se
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // URL update aur media player me inject karna
    LaunchedEffect(currentUrl, exoPlayer) {
        val normalizedUrl = currentUrl.trim()
        val mimeType = if (normalizedUrl.contains(".m3u8", true) || normalizedUrl.lowercase(Locale.ROOT).split('?').first().endsWith(".m3u8")) {
            MimeTypes.APPLICATION_M3U8
        } else {
            MimeTypes.VIDEO_MP4
        }

        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.Builder().setUri(normalizedUrl).setMimeType(mimeType).build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlaying
    }

    // Composable destroy hote hi memory clean karna
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Alpha animation ko thoda aur smooth (600ms) bana diya
    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(600),
        label = "canvasAlpha"
    )

    AndroidView(
        factory = { viewContext ->
            AspectRatioFrameLayout(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                
                val textureView = TextureView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
                addView(textureView)
                exoPlayer.setVideoTextureView(textureView)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { view -> 
            view.setAspectRatio(videoAspectRatio)
        },
        onRelease = { view ->
            val textureView = view.getChildAt(0) as? TextureView
            if (textureView != null) {
                exoPlayer.clearVideoTextureView(textureView)
            }
        },
        modifier = modifier.alpha(alpha),
    )
}
