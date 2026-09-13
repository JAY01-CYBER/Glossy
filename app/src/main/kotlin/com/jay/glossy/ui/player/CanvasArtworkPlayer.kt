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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import okhttp3.OkHttpClient
import java.util.Locale

// SINGLETON MANAGER TO KEEP EXOPLAYER ALIVE AND PREVENT RESTARTS
object CanvasPlayerManager {
    var exoPlayer: ExoPlayer? = null
    var currentUrl: String? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            val okHttpClient = OkHttpClient.Builder().build()
            val mediaSourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, OkHttpDataSource.Factory(okHttpClient)))
            
            // FAST BUFFERING LOGIC
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
                    // No playWhenReady here, handled in Composable to save battery when app minimized
                }
        }
        return exoPlayer!!
    }

    fun play(context: Context, url: String) {
        val normalizedUrl = url.trim()
        if (currentUrl == normalizedUrl && exoPlayer != null) return
        
        val player = getPlayer(context)
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

    val exoPlayer = remember { CanvasPlayerManager.getPlayer(context) }

    // Load new video url when song changes
    LaunchedEffect(initialUrl) {
        if (CanvasPlayerManager.currentUrl != initialUrl) {
            isVideoReady = false // Reset for fade animation
        }
        CanvasPlayerManager.play(context, initialUrl)
    }

    // THIS ENSURES CANVAS ALWAYS PLAYS INDEPENDENTLY OF SONG PAUSE
    DisposableEffect(exoPlayer) {
        exoPlayer.playWhenReady = true
        onDispose {
            exoPlayer.playWhenReady = false // Pause when mini player is active to save battery
        }
    }

    DisposableEffect(exoPlayer, initialUrl) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (CanvasPlayerManager.currentUrl == primaryUrl && !fallbackUrl.isNullOrBlank()) {
                    CanvasPlayerManager.play(context, fallbackUrl)
                    isVideoReady = false 
                }
            }
            override fun onRenderedFirstFrame() { isVideoReady = true }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        
        // Instant resum
        if (exoPlayer.videoSize.width > 0 && CanvasPlayerManager.currentUrl == initialUrl) {
            isVideoReady = true
            videoAspectRatio = exoPlayer.videoSize.width.toFloat() / exoPlayer.videoSize.height
        }

        onDispose { 
            exoPlayer.removeListener(listener)
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(300),
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
            val textureView = view.getChildAt(0) as? TextureView
            if (textureView != null && exoPlayer.videoSurfaceView != textureView) {
                exoPlayer.setVideoTextureView(textureView)
            }
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
