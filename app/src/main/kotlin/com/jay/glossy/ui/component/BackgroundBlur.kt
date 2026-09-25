/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.jay.glossy.ui.component

import android.graphics.Bitmap
import android.os.Build
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.jay.glossy.constants.BackgroundBlurEnabledKey
import com.jay.glossy.constants.BackgroundBlurStrengthKey
import com.jay.glossy.playback.PlayerConnection
import com.jay.glossy.ui.player.CanvasArtworkPlaybackCache
import com.jay.glossy.ui.player.CanvasCacheManager
import com.jay.glossy.ui.player.CanvasResolver
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/**
 * App backdrop: the current song's animated canvas playing heavily blurred
 * behind everything, or — when the song has no canvas — the album artwork
 * blurred the same way. Falls back to the plain theme base when nothing is
 * playing. A strength-driven scrim sits on top for text legibility.
 *
 * Blur works on every API level via a downscale trick: the canvas video is
 * rendered into a small TextureView which is then scaled back up through an
 * offscreen layer (GPU bilinear upsample = blur). Android 12+ adds a real
 * RenderEffect gaussian on top for extra smoothness.
 */
@Composable
fun BackgroundBlurBackdrop(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    playerConnection: PlayerConnection? = null,
) {
    val (enabled, _) = rememberPreference(BackgroundBlurEnabledKey, defaultValue = true)
    val (strength, _) = rememberPreference(BackgroundBlurStrengthKey, defaultValue = 0.6f)

    if (!enabled || strength <= 0.01f) return

    val s = strength.coerceIn(0f, 1f)
    val scheme = MaterialTheme.colorScheme
    val base = if (pureBlack) Color.Black else scheme.background
    val scrimTop = if (pureBlack) 0.55f else 0.55f - 0.25f * s
    val scrimBottom = if (pureBlack) 0.78f else 0.70f - 0.28f * s

    val metadata = playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val isPlaying = playerConnection?.isPlaying?.collectAsStateWithLifecycle()?.value ?: false

    val context = LocalContext.current
    val mediaId = metadata?.id
    val title = metadata?.title ?: ""
    val artist = metadata?.artists?.joinToString { it.name } ?: ""
    val album = metadata?.album?.title ?: ""
    val artworkUrl = metadata?.thumbnailUrl

    // Canvas for the current track: instant hit from the playback cache, then
    // a style-aware resolve (ALL races every provider) fills it in.
    var canvasUrl by remember(mediaId) {
        mutableStateOf(
            mediaId?.let { CanvasArtworkPlaybackCache.get(it) }?.preferredAnimationUrl,
        )
    }
    LaunchedEffect(mediaId, title, artist) {
        if (mediaId.isNullOrBlank() || title.isBlank() || artist.isBlank()) return@LaunchedEffect
        canvasUrl = try {
            CanvasResolver.resolve(
                context = context,
                mediaId = mediaId,
                songTitle = title,
                artistName = artist,
                albumName = album,
            )?.preferredAnimationUrl
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    Box(modifier = modifier.fillMaxSize().background(base)) {
        // Blurred artwork underneath — also the fallback when no canvas exists
        // and the visible layer while the canvas video buffers.
        if (!artworkUrl.isNullOrBlank()) {
            BlurredArtworkLayer(url = artworkUrl, strength = s)
        }
        // Animated canvas on top, fading in on the first decoded frame.
        if (!canvasUrl.isNullOrBlank()) {
            CanvasVideoLayer(
                url = canvasUrl!!,
                isPlaying = isPlaying,
                strength = s,
            )
        }
        // Legibility scrim (strength-driven).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            base.copy(alpha = scrimTop),
                            base.copy(alpha = scrimBottom),
                        ),
                    ),
                ),
        )
    }
}

/**
 * Album artwork blurred on every API level: decode/downscale to a tiny bitmap,
 * let the GPU bilinearly upscale it to full screen, then optionally add a real
 * gaussian on Android 12+. Cheap — drawn once, no per-frame cost.
 */
@Composable
private fun BlurredArtworkLayer(url: String, strength: Float) {
    val context = LocalContext.current
    // Quantized decode size so slider drags don't re-fetch: strong = smaller
    // source = heavier blur.
    val targetPx = when {
        strength < 0.33f -> 160
        strength < 0.66f -> 96
        else -> 64
    }

    var bitmap by remember(url, targetPx) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url, targetPx) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                val result = context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .build(),
                )
                val source = result.image?.toBitmap() ?: return@withContext null
                val w = targetPx
                val h = (source.height.toLong() * w / source.width.coerceAtLeast(1))
                    .coerceIn(1L, 4096L).toInt()
                // Two-step downscale keeps the tiny bitmap smooth instead of aliased.
                val step1 = Bitmap.createScaledBitmap(source, w * 3, h * 3, true)
                val step2 = Bitmap.createScaledBitmap(step1, w, h, true)
                if (step1 !== source && step1 !== step2) step1.recycle()
                step2.asImageBitmap()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    bitmap?.let { image ->
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur((10f + 30f * strength).dp)
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

/**
 * Animated canvas video, blurred on every API level: the TextureView renders
 * the video at 1/(4..16) of the screen (divisor follows the strength slider)
 * and an offscreen graphics layer scales that small raster back up — GPU
 * bilinear upsampling reads as a heavy blur. Android 12+ stacks a real
 * gaussian on the upscaled result. Muted, looped, paused with playback or
 * when the app goes to the background.
 */
@Composable
private fun CanvasVideoLayer(url: String, isPlaying: Boolean, strength: Float) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mediaSourceFactory = remember { CanvasCacheManager.getMediaSourceFactory(context, true) }
    val player = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(2000, 15000, 500, 2000)
                    .build(),
            )
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    false,
                )
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ALL
            }
    }

    var appVisible by remember { mutableStateOf(true) }
    var frameReady by remember(url) { mutableStateOf(false) }
    val currentIsPlaying by rememberUpdatedState(isPlaying)

    // Stop decoding when the app is backgrounded; release on leave.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> appVisible = false
                Lifecycle.Event.ON_START -> appVisible = true
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    LaunchedEffect(isPlaying, appVisible) {
        player.playWhenReady = isPlaying && appVisible && currentIsPlaying
    }

    LaunchedEffect(url) {
        frameReady = false
        val normalized = url.trim()
        val mimeType = if (normalized.contains(".m3u8", true) ||
            normalized.lowercase(Locale.ROOT).split('?').first().endsWith(".m3u8")
        ) {
            MimeTypes.APPLICATION_M3U8
        } else {
            MimeTypes.VIDEO_MP4
        }
        player.setMediaItem(
            MediaItem.Builder().setUri(normalized).setMimeType(mimeType).build(),
        )
        player.prepare()
    }

    // First-frame listener for the fade-in.
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                frameReady = true
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Hide the layer; the blurred artwork below shows through.
                frameReady = false
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val canvasAlpha by animateFloatAsState(
        targetValue = if (frameReady) 1f else 0f,
        label = "backdropCanvasAlpha",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Downscale divisor: mild (4x) at strength 0 → heavy (16x) at 1.0.
        val divisor = (4f + 12f * strength).roundToInt().coerceIn(4, 16)
        val smallW = maxWidth / divisor
        val smallH = maxHeight / divisor

        AndroidView(
            factory = { viewContext ->
                TextureView(viewContext).apply {
                    isOpaque = false
                    player.setVideoTextureView(this)
                }
            },
            onRelease = { _ ->
                player.setVideoTextureView(null)
            },
            modifier = Modifier
                .align(Alignment.Center)
                .size(smallW, smallH)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(radius = (8f + 28f * strength).dp)
                    } else {
                        Modifier
                    },
                )
                .graphicsLayer {
                    // Slight overscan so blur sampling never pulls in edges.
                    scaleX = divisor * 1.15f
                    scaleY = divisor * 1.15f
                    alpha = canvasAlpha
                    // Force rasterization at the small size — this is what
                    // turns the upscale into a blur on pre-12 devices.
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        )
    }
}
