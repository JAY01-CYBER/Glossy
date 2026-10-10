package com.jay.glossy.ui.player

import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.jay.glossy.constants.CanvasCacheMode
import com.jay.glossy.constants.CanvasCacheModeKey
import com.jay.glossy.ui.player.CanvasCacheManager
import com.jay.glossy.utils.rememberEnumPreference
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun YouTubeCanvasBackground(
    videoUrl: String?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    scrimOpacity: Float = 0.6f,
) {
    val context = LocalContext.current
    val url = videoUrl?.takeIf { it.isNotBlank() } ?: return

    var isVideoReady by remember(url) { mutableStateOf(false) }
    var reloadTick by remember(url) { mutableIntStateOf(0) }
    var retriesUsed by remember(url) { mutableIntStateOf(0) }

    val (canvasCacheMode) = rememberEnumPreference(CanvasCacheModeKey, defaultValue = CanvasCacheMode.VIDEO_AND_URL)
    val enableVideoCache = canvasCacheMode == CanvasCacheMode.VIDEO_AND_URL

    val mediaSourceFactory = remember(enableVideoCache) {
        CanvasCacheManager.getMediaSourceFactory(context, enableVideoCache)
    }

    val exoPlayer = remember(url) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2000, 15000, 500, 2000)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .build()
                setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
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

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (retriesUsed < MaxCanvasRetries) {
                    retriesUsed++
                    isVideoReady = false
                    reloadTick++
                } else {
                    isVideoReady = false
                }
            }

            override fun onRenderedFirstFrame() {
                isVideoReady = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(url, exoPlayer, reloadTick) {
        if (reloadTick > 0) delay(CanvasRetryDelayMillis)
        val normalizedUrl = url.trim()
        val mimeType = if (normalizedUrl.contains(".m3u8", true) || normalizedUrl.lowercase(Locale.ROOT).split('?').first().endsWith(".m3u8")) {
            MimeTypes.APPLICATION_M3U8
        } else {
            MimeTypes.VIDEO_MP4
        }

        exoPlayer.stop()
        isVideoReady = false
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
        animationSpec = tween(500),
        label = "canvasBackgroundAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        AndroidVideoView(
            exoPlayer = exoPlayer,
            modifier = Modifier.fillMaxSize(),
        )

        // Dark gradient scrim overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = scrimOpacity * 0.3f),
                        0.5f to Color.Black.copy(alpha = scrimOpacity * 0.5f),
                        1.0f to Color.Black.copy(alpha = scrimOpacity),
                    )
                ),
        )
    }
}

@Composable
private fun AndroidVideoView(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { viewContext ->
            AspectRatioFrameLayout(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                val textureView = TextureView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    isOpaque = false
                }
                addView(textureView)
                exoPlayer.setVideoTextureView(textureView)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { view ->
            // No update needed
        },
        modifier = modifier,
    )
}

private const val MaxCanvasRetries = 3
private const val CanvasRetryDelayMillis = 1200L