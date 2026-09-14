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

object CanvasCacheManager {
    private var videoCache: SimpleCache? = null

    fun getVideoCache(context: Context): SimpleCache {
        if (videoCache == null) {
            val cacheDir = File(context.filesDir, "canvas_video_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(256 * 1024 * 1024L) // 256MB Limit
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
}

@Composable
fun CanvasArtworkPlayer(
    primaryUrl: String?,
    fallbackUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val primary = primaryUrl?.takeIf { it.isNotBlank() }
    val fallback = fallbackUrl?.takeIf { it.isNotBlank() }
    val initial = primary ?: fallback ?: return
    
    var currentUrl by remember(initial) { mutableStateOf(initial) }
    var isVideoReady by remember(initial) { mutableStateOf(false) }
    var videoAspectRatio by remember(initial) { mutableStateOf(1f) }

    val (canvasCacheMode) = rememberEnumPreference(CanvasCacheModeKey, defaultValue = CanvasCacheMode.VIDEO_AND_URL)
    val enableVideoCache = canvasCacheMode == CanvasCacheMode.VIDEO_AND_URL

    val exoPlayer = remember(enableVideoCache) {
        val okHttpClient = OkHttpClient.Builder().build()
        val upstreamFactory = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(okHttpClient))
        
        val mediaSourceFactory = if (enableVideoCache) {
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(CanvasCacheManager.getVideoCache(context))
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            DefaultMediaSourceFactory(cacheDataSourceFactory)
        } else {
            DefaultMediaSourceFactory(upstreamFactory)
        }
        
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
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

    DisposableEffect(exoPlayer, primary, fallback) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val next = when (currentUrl) {
                    primary -> fallback
                    else -> null
                }
                if (!next.isNullOrBlank()) {
                    currentUrl = next
                    isVideoReady = false 
                }
            }
            override fun onRenderedFirstFrame() {
                isVideoReady = true
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

    LaunchedEffect(currentUrl, exoPlayer) {
        val normalized = currentUrl.trim()
        val mimeType = when {
            normalized.contains(".m3u8", ignoreCase = true) || 
            normalized.lowercase(Locale.ROOT).split('?').first().endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
            else -> MimeTypes.VIDEO_MP4
        }

        val mediaItem = MediaItem.Builder()
            .setUri(normalized)
            .setMimeType(mimeType)
            .build()

        exoPlayer.stop()
        isVideoReady = false
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
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
        modifier = modifier.alpha(alpha),
    )
}
