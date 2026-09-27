/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.jay.glossy.ui.component

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.jay.glossy.constants.BackgroundBlurEnabledKey
import com.jay.glossy.constants.BackgroundBlurStrengthKey
import com.jay.glossy.constants.CanvasThumbnailAnimationKey
import com.jay.glossy.playback.PlayerConnection
import com.jay.glossy.ui.player.CanvasArtworkPlaybackCache
import com.jay.glossy.ui.player.CanvasCacheManager
import com.jay.glossy.ui.player.CanvasResolver
import com.jay.glossy.ui.player.rememberCanvasEnabled
import com.jay.glossy.utils.blurred
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/**
 * App backdrop: the current song's animated canvas playing heavily blurred
 * behind everything, or — when the song has no canvas — the album artwork
 * blurred the same way. Falls back to the plain theme base when nothing is
 * playing. A strength-driven scrim sits on top for text legibility.
 *
 * The canvas layer only runs while [CanvasThumbnailAnimationKey] ("Canvas
 * Background") is on: with it off the backdrop stays on the blurred artwork and
 * nothing is resolved or decoded, which is also where most of the backdrop's
 * battery and frame cost lives.
 *
 * The artwork layer is blurred on the CPU, so it looks the same on every API
 * level. The canvas video keeps its downscale trick on Android 12+ (where a
 * RenderEffect gaussian hides the upscale); below Android 12 the upscale alone
 * reads as pixelation, so the texture is sampled, blurred and drawn instead.
 */
@Composable
fun BackgroundBlurBackdrop(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    playerConnection: PlayerConnection? = null,
) {
    val (enabled, _) = rememberPreference(BackgroundBlurEnabledKey, defaultValue = true)
    val (strength, _) = rememberPreference(BackgroundBlurStrengthKey, defaultValue = 0.6f)
    // Master canvas switch AND the mobile-data policy: on a metered connection
    // the backdrop keeps the blurred artwork unless the user opted in.
    val canvasEnabled = rememberCanvasEnabled()

    if (!enabled || strength <= 0.01f) return

    val s = strength.coerceIn(0f, 1f)
    val scheme = MaterialTheme.colorScheme
    val base = if (pureBlack) Color.Black else scheme.background
    // Denser scrims than a soft frost: the backdrop should read as one hard,
    // opaque pane rather than letting the artwork bleed through it.
    val scrimTop = if (pureBlack) 0.62f else 0.62f - 0.18f * s
    val scrimBottom = if (pureBlack) 0.85f else 0.80f - 0.20f * s

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
    var canvasUrl by remember(mediaId, canvasEnabled) {
        mutableStateOf(
            if (canvasEnabled) {
                mediaId?.let { CanvasArtworkPlaybackCache.get(it) }?.preferredAnimationUrl
            } else {
                null
            },
        )
    }
    LaunchedEffect(mediaId, title, artist, canvasEnabled) {
        if (!canvasEnabled) {
            canvasUrl = null
            return@LaunchedEffect
        }
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
            BlurredArtworkBackdrop(
                url = artworkUrl,
                blurStrength = s,
            )
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

    // Android 12+ has a real gaussian: keep the cheap downscale-and-upscale and
    // let RenderEffect smooth it. Everywhere else there is nothing to hide the
    // upscale, so the texture is sampled and blurred on the CPU at a low rate
    // instead of being stretched into visible blocks.
    val supportsGaussianBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Downscale divisor: mild (5x) at strength 0 -> heavy (14x) at 1.0.
        val divisor = (5f + 9f * strength).roundToInt().coerceIn(5, 14)
        val smallW = maxWidth / divisor
        val smallH = maxHeight / divisor

        var textureView by remember { mutableStateOf<TextureView?>(null) }
        var blurredFrame by remember { mutableStateOf<ImageBitmap?>(null) }
        val sampleRadius = (smallW.value * 0.10f).roundToInt().coerceAtLeast(1)

        LaunchedEffect(
            supportsGaussianBlur,
            textureView,
            frameReady,
            isPlaying,
            appVisible,
            smallW,
            smallH,
            sampleRadius,
        ) {
            val view = textureView
            if (supportsGaussianBlur || view == null) return@LaunchedEffect
            while (isActive) {
                if (isPlaying && appVisible && frameReady) {
                    val frame = withContext(Dispatchers.Default) {
                        runCatching {
                            val sampled = view.getBitmap(
                                smallW.value.roundToInt().coerceAtLeast(1),
                                smallH.value.roundToInt().coerceAtLeast(1),
                            ) ?: return@runCatching null
                            sampled.blurred(sampleRadius)
                        }.getOrNull()
                    }
                    if (frame != null) blurredFrame = frame.asImageBitmap()
                }
                delay(120)
            }
        }

        AndroidView(
            factory = { viewContext ->
                TextureView(viewContext).apply {
                    isOpaque = false
                    player.setVideoTextureView(this)
                    textureView = this
                }
            },
            onRelease = { _ ->
                player.setVideoTextureView(null)
                textureView = null
            },
            modifier = Modifier
                .align(Alignment.Center)
                .size(
                    width = if (supportsGaussianBlur) smallW else 1.dp,
                    height = if (supportsGaussianBlur) smallH else 1.dp,
                )
                .then(
                    if (supportsGaussianBlur) {
                        Modifier.blur(radius = (48f + 112f * strength).dp)
                    } else {
                        Modifier
                    },
                )
                .graphicsLayer {
                    // Slight overscan so blur sampling never pulls in edges.
                    scaleX = if (supportsGaussianBlur) divisor * 1.15f else 1f
                    scaleY = if (supportsGaussianBlur) divisor * 1.15f else 1f
                    alpha = if (supportsGaussianBlur) canvasAlpha else 1f
                    // Force rasterization at the small size — this is what
                    // turns the upscale into a blur on Android 12+.
                    compositingStrategy = CompositingStrategy.Offscreen
                },
        )

        // Pre-12 output: the blurred, CPU-processed frame drawn over the whole
        // backdrop. It also covers the 1dp texture view that produced it.
        if (!supportsGaussianBlur) {
            blurredFrame?.let { frame ->
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(canvasAlpha),
                )
            }
        }
    }
}
