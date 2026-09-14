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
import androidx.compose.animation.core.snap
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

object CanvasPlayerManager {
    var exoPlayer: ExoPlayer? = null
    var currentUrl: String? = null
    private var videoCache: SimpleCache? = null 
    private var currentCacheMode: Boolean? = null 

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
        exoPlayer?.release()
        exoPlayer = null
        videoCache?.release()
        videoCache = null
        File(context.filesDir, "canvas_video_cache").deleteRecursively()
        currentUrl = null
    }

    fun getPlayer(context: Context, enableVideoCache: Boolean): ExoPlayer {
        if (exoPlayer == null || currentCacheMode != enableVideoCache) {
            exoPlayer?.release()
            currentCacheMode = enableVideoCache

            val okHttpClient = OkHttpClient.Builder().build()
            val upstreamFactory = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(okHttpClient))
            
            val mediaSourceFactory = if (enableVideoCache) {
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(getVideoCache(context))
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                DefaultMediaSourceFactory(cacheDataSourceFactory)
            } else {
                DefaultMediaSourceFactory(upstreamFactory)
            }
            
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(500, 5000, 100, 500)
                .build()
            
            exoPlayer = ExoPlayer.Builder(context)
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
                }
        }
        return exoPlayer!!
    }

    fun play(context: Context, url: String, enableVideoCache: Boolean) {
        val normalizedUrl = url.trim()
        if (currentUrl == normalizedUrl && exoPlayer != null && currentCacheMode == enableVideoCache) return
        
        val player = getPlayer(context, enableVideoCache)
        val mimeType = if (normalizedUrl.contains(".m3u8", true) || normalizedUrl.lowercase(Locale.ROOT).split('?').first().endsWith(".m3u8")) {
            MimeTypes.APPLICATION_M3U8
        } else {
            MimeTypes.VIDEO_MP4
        }

        player.setMediaItem(MediaItem.Builder().setUri(normalizedUrl).setMimeType(mimeType).build())
        player.prepare()
        currentUrl = normalizedUrl
    }
}

@Composable
fun CanvasArtworkPlayer(
    primaryUrl: String?,
    fallbackUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val initialUrl = primaryUrl?.takeIf { it.isNotBlank() } ?: fallbackUrl?.takeIf { it.isNotBlank() } ?: return
    
    var isVideoReady by remember { mutableStateOf(false) }
    var videoAspectRatio by remember { mutableStateOf(1f) }
    var activeUrl by remember { mutableStateOf<String?>(null) }

    val (canvasCacheMode) = rememberEnumPreference(CanvasCacheModeKey, defaultValue = CanvasCacheMode.VIDEO_AND_URL)
    val enableVideoCache = canvasCacheMode == CanvasCacheMode.VIDEO_AND_URL

    val exoPlayer = remember(enableVideoCache) { CanvasPlayerManager.getPlayer(context, enableVideoCache) }

    LaunchedEffect(initialUrl, enableVideoCache) {
        if (activeUrl != initialUrl) {
            isVideoReady = false 
        }
        activeUrl = initialUrl
        CanvasPlayerManager.play(context, initialUrl, enableVideoCache)
    }

    DisposableEffect(exoPlayer) {
        exoPlayer.playWhenReady = true
        onDispose {
            exoPlayer.playWhenReady = false 
        }
    }

    DisposableEffect(exoPlayer, initialUrl) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (CanvasPlayerManager.currentUrl == primaryUrl && !fallbackUrl.isNullOrBlank()) {
                    CanvasPlayerManager.play(context, fallbackUrl, enableVideoCache)
                    activeUrl = fallbackUrl
                    isVideoReady = false 
                }
            }
            override fun onRenderedFirstFrame() { 
                if (activeUrl == CanvasPlayerManager.currentUrl) {
                    isVideoReady = true 
                }
            }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        
        if (exoPlayer.videoSize.width > 0 && CanvasPlayerManager.currentUrl == initialUrl) {
            isVideoReady = true
            videoAspectRatio = exoPlayer.videoSize.width.toFloat() / exoPlayer.videoSize.height
        }

        onDispose { 
            exoPlayer.removeListener(listener)
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady && activeUrl == initialUrl) 1f else 0f,
        animationSpec = if (isVideoReady) tween(500) else snap(),
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
