/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * A full-screen now-playing design:
 * *  - [VinylNowPlaying]      Warm turntable: a spinning vinyl record with the
 *    artwork printed on the disc, swinging tonearm and a transport + *    Lyrics / Cast / Queue dock.
 *
 *  - [CapsuleNowPlaying]    Maroon vertical gradient, square cover and the
 *    transport parked inside one dark pill with a pale-pink play disc.
 *
 *  - [CinematicNowPlaying]  Full-bleed artwork with legibility scrims, glass
 *    header discs and a white play disc in a five-up transport.
 *
 * The design paints no animated canvas: the record shows plain artwork. The
 * canvas engines stay exclusive to the artwork surfaces — the standard player
 * designs and the Featured Spotlight cards — all behind the single "Canvas
 * Background" switch, so this screen looks identical whether it is on or off.
 */

package com.jay.glossy.ui.player

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.jay.glossy.constants.MiniLyricsStyle
import com.jay.glossy.constants.MiniLyricsStyleKey
import com.jay.glossy.constants.PlayerHorizontalPadding
import com.jay.glossy.constants.PlayerBackgroundStyle
import com.jay.glossy.constants.PlayerBackgroundStyleKey
import com.jay.glossy.constants.ShowLyricsOnPlayerKey
import com.jay.glossy.extensions.toggleRepeatMode
import com.jay.glossy.listentogether.RoomRole
import com.jay.glossy.playback.ExoDownloadService
import com.jay.glossy.playback.PlayerConnection
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.CastButton
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.WavySlider
import com.jay.glossy.ui.component.rememberAmbientMotionEnabled
import com.jay.glossy.ui.menu.PlayerMenu
import com.jay.glossy.ui.utils.ShowMediaInfo
import com.jay.glossy.utils.joinToArtistString
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.makeTimeString
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2

private val VinylAccent = Color(0xFFE8A33D)
private val VinylBackground = Color(0xFF1B1712)
private val VinylTextSecondary = Color(0xFFC9BDA2)

// Warm cream equivalents for the Vinyl design.
private val VinylSurface = Color(0xFFF1E3C9)
private val VinylOnSurface = Color(0xFF241505)

// Used until the artwork has been sampled, and whenever a cover has no usable
// colour to draw from.
private val VinylFallbackColors = ArtworkColors(
    accent = VinylAccent,
    onAccent = Color(0xFF241505),
    side = Color(0xFF2A231A),
    onSide = Color(0xFFF2E6D0),
    surface = VinylSurface,
    onSurface = VinylOnSurface,
)

/**
 * Plain white music notes that drift upward while [isPlaying]. One shared
 * infinite transition feeds every note through a phase offset, and the
 * per-note transform is applied in the draw phase so nothing recomposes per
 * frame.
 */
@Composable
fun FloatingMusicNotes(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    noteCount: Int = 7,
) {
    // Pure ambience, so the notes follow the app's ambient-motion rule: on a
    // slow phone, or with animations switched off, the vinyl design simply
    // doesn't draw them.
    AnimatedVisibility(
        visible = isPlaying && rememberAmbientMotionEnabled(),
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(400)),
        modifier = modifier,
    ) {
        val transition = rememberInfiniteTransition(label = "floatingMusicNotes")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                // Slow drift: the notes are ambience, not motion. Faster than
                // this reads as a blur on the artwork.
                animation = tween(9000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "floatingMusicNoteProgress",
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerHeight = maxHeight
            val containerWidth = maxWidth
            repeat(noteCount) { index ->
                val phase = index.toFloat() / noteCount
                val drift = ((index % 3) - 1) * 0.32f
                val iconSize = 18 + (index % 3) * 6
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(iconSize.dp)
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            val t = (progress + phase) % 1f
                            translationY = -t * (containerHeight.toPx() + 24.dp.toPx())
                            translationX = drift * containerWidth.toPx() + t * 18.dp.toPx()
                            alpha = when {
                                t < 0.12f -> t / 0.12f
                                t > 0.75f -> ((1f - t) / 0.25f).coerceIn(0f, 1f)
                                else -> 1f
                            } * 0.5f
                            val s = 0.72f + t * 0.42f
                            scaleX = s
                            scaleY = s
                            rotationZ = -12f + t * 26f
                        },
                )
            }
        }
    }
}

private data class PlayerFlags(
    val playbackState: Int = Player.STATE_IDLE,
    val isPlaying: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val isMuted: Boolean = false,
    val isGuest: Boolean = false,
    val isCasting: Boolean = false,
    val castIsPlaying: Boolean = false,
)

@Composable
private fun rememberPlayerFlags(): PlayerFlags {
    val playerConnection = LocalPlayerConnection.current ?: return PlayerFlags()
    val playbackState by playerConnection.playbackState.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val listenTogetherManager = LocalListenTogetherManager.current
    val roleState = listenTogetherManager?.role
        ?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isGuest = roleState?.value == RoomRole.GUEST

    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }

    return PlayerFlags(
        playbackState = playbackState,
        isPlaying = if (isCasting) castIsPlaying else isPlaying,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        isMuted = isMuted,
        isGuest = isGuest,
        isCasting = isCasting,
        castIsPlaying = castIsPlaying,
    )
}

/** Shared tap handler for the centre transport button. */
private fun playPauseAction(connection: PlayerConnection, flags: PlayerFlags): () -> Unit = {
    when {
        flags.isGuest -> connection.toggleMute()
        flags.isCasting -> {
            val handler = try {
                connection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
            if (flags.castIsPlaying) handler?.pause() else handler?.play()
        }
        flags.playbackState == Player.STATE_ENDED -> {
            connection.player.seekTo(0, 0)
            connection.player.playWhenReady = true
        }
        else -> connection.togglePlayPause()
    }
}

/**
 * Seek bar plus elapsed/total labels for both custom designs.
 *
 * The ticking playback position is read *here*, inside the only subtree that
 * changes on every tick. Reading it in the screen itself — which is what a
 * `position: Long` parameter forces — re-executed the whole design, blur layers
 * and palette animations included, ten times a second while a song played.
 */
@Composable
private fun GlossySeekBar(
    positionProvider: () -> Long,
    duration: Long,
    activeColor: Color,
    labelColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    labelModifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    val safeDuration = if (duration > 0L) duration else 1L
    val displayPosition = sliderPosition ?: positionProvider()

    WavySlider(
        value = displayPosition.toFloat(),
        valueRange = 0f..safeDuration.toFloat(),
        onValueChange = { sliderPosition = it.toLong() },
        onValueChangeFinished = {
            sliderPosition?.let { playerConnection?.player?.seekTo(it) }
            sliderPosition = null
        },
        colors = SliderDefaults.colors(
            activeTrackColor = activeColor,
            inactiveTrackColor = activeColor.copy(alpha = 0.25f),
            thumbColor = activeColor,
        ),
        isPlaying = isPlaying,
        modifier = modifier.fillMaxWidth(),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(labelModifier),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(makeTimeString(displayPosition), color = labelColor, fontSize = 12.sp)
        Text(
            text = if (duration != C.TIME_UNSET) makeTimeString(safeDuration) else "",
            color = labelColor,
            fontSize = 12.sp,
        )
    }
}

// ---------------------------------------------------------------------------
// Vinyl
// ---------------------------------------------------------------------------

@Composable
fun VinylNowPlaying(
    bottomSheetState: BottomSheetState,
    /** Live playback position — see [GlossySeekBar] for why it is a provider. */
    position: () -> Long,
    duration: Long,
    onOpenQueue: () -> Unit,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val flags = rememberPlayerFlags()
    val showLyricsOnPlayer by rememberPreference(ShowLyricsOnPlayerKey, defaultValue = false)
    val (miniLyricsStyle) = rememberEnumPreference(MiniLyricsStyleKey, defaultValue = MiniLyricsStyle.CLASSIC)
    // Guarded once here: the label button's progress ring divides by it, and a
    // branch in the draw phase is the one place it is easy to avoid.
    val safeDuration = if (duration > 0L) duration else 1L
    // The record keeps its warm identity, but the controls take the colour of
    // whatever is playing.
    val colors = animatedArtworkColors(
        rememberArtworkPalette(
            mediaId = mediaMetadata?.id,
            thumbnailUrl = mediaMetadata?.thumbnailUrl,
            fallback = VinylFallbackColors,
        ),
    )

    var showLyrics by rememberSaveable { mutableStateOf(false) }

    // The warm base colour is Vinyl's signature look, but it must never paint
    // over the user's chosen player background: with Blur/Gradient/Mesh
    // enabled the design steps aside to a light scrim so that background
    // (which the sheet below already drew) shows through.
    val playerBackground by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT,
    )
    val vinylBase =
        if (playerBackground == PlayerBackgroundStyle.DEFAULT) VinylBackground else Color.Black.copy(alpha = 0.30f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(vinylBase)
            .statusBarsPadding(),
    ) {
        if (showLyrics) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .padding(bottom = bottomInset),
            ) {
                InlineLyricsView(
                    mediaMetadata = mediaMetadata,
                    showLyrics = showLyrics,
                    positionProvider = position,
                )
                GlossyDock(
                    lyricsActive = true,
                    onToggleLyrics = { showLyrics = false },
                    onOpenQueue = onOpenQueue,
                    colors = colors,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PlayerHorizontalPadding)
                .padding(bottom = bottomInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VinylTopBar(
                bottomSheetState = bottomSheetState,
                onCollapse = { bottomSheetState.collapseSoft() },
                title = mediaMetadata?.title.orEmpty(),
                artist = mediaMetadata?.artists?.joinToArtistString(", ") { it.name }.orEmpty(),
                colors = colors,
            )

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // weight(1f) gives the artwork zone all leftover height.
                    // TopCenter + top padding keeps a small breathing gap below
                    // the top bar while eliminating the excess space that used to
                    // pool above the record when the box centred it.
                    .weight(1f)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    // Sized by the shorter side of the slot: the lyrics strip
                    // below grows when the active line wraps onto a second row,
                    // and a width-sized square used to overflow the space left
                    // for it instead of shrinking into it.
                    val fittedSide =
                        if (maxHeight.value.isFinite()) minOf(maxWidth, maxHeight) else maxWidth
                    val artworkSide by animateDpAsState(targetValue = fittedSide, label = "vinylSide")
                    Box(
                        modifier = Modifier.size(artworkSide),
                        contentAlignment = Alignment.Center,
                    ) {
                        SpinningVinyl(
                            thumbnailUrl = mediaMetadata?.thumbnailUrl,
                            isPlaying = flags.isPlaying,
                        )
                        // Spans the whole square so the arm's geometry can be derived
                        // from the disc's own radius instead of a hard-coded arm length.
                        VinylTonearm(
                            isPlaying = flags.isPlaying,
                            modifier = Modifier.fillMaxSize(),
                        )
                        FloatingMusicNotes(
                            isPlaying = flags.isPlaying,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // Canvas-glow mini lyrics: parked at the very bottom of the
                // artwork zone, so the line reads as the line directly above the
                // song title instead of words floating over the middle of the
                // record.
                //
                // It stays an overlay on the weighted zone rather than a child
                // of the column below: the record is sized by the shorter side
                // of that slot, so a block inserted under the disc would take
                // its height and shrink the artwork (which is why the classic
                // strip used to cost the design real estate), while an overlay
                // only fills space the zone already leaves empty.
                if (showLyricsOnPlayer && !showLyrics && miniLyricsStyle == MiniLyricsStyle.CANVAS_GLOW) {
                    PlayerCanvasGlowLyrics(
                        mediaMetadata = mediaMetadata,
                        positionProvider = position,
                        accent = colors.accent,
                        textSize = 20.sp,
                        // Bottom of the slot, not its centre, and on the title's
                        // own edge: the words read as the line above the song name
                        // rather than a caption floating over the record.
                        alignment = Alignment.BottomStart,
                        // Almost no inner padding: the title sits ~8dp below this
                        // block, so the default's 16dp would push the words a
                        // pocket's width away from the name it belongs to.
                        contentPadding = PaddingValues(start = 0.dp, end = 16.dp, top = 4.dp, bottom = 0.dp),
                        alignToStart = true,
                        onExpand = { showLyrics = true },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(),
                    )
                }
            }

            if (showLyricsOnPlayer && miniLyricsStyle == MiniLyricsStyle.CLASSIC) {
                Spacer(Modifier.height(14.dp))
                PlayerSyncedLyricsView(
                    mediaMetadata = mediaMetadata,
                    positionProvider = position,
                    accent = colors.accent,
                    onExpand = { showLyrics = true },
                    modifier = Modifier.fillMaxWidth(),
                    // The parent column above already insets by
                    // PlayerHorizontalPadding; letting the strip add its own
                    // pushed the lyrics a double-padding right of the title.
                    horizontalPadding = 0.dp,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = mediaMetadata?.title.orEmpty(),
                color = Color(0xFFF2E6D0),
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().basicMarquee(),
            )
            Text(
                text = mediaMetadata?.artists?.joinToArtistString(", ") { it.name }.orEmpty(),
                color = VinylTextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().basicMarquee(),
            )

            Spacer(Modifier.height(14.dp))

            GlossySeekBar(
                positionProvider = position,
                duration = duration,
                activeColor = VinylAccent,
                labelColor = VinylTextSecondary,
                isPlaying = flags.isPlaying,
            )

            Spacer(Modifier.height(12.dp))

            VinylTransportRow(
                connection = playerConnection,
                flags = flags,
                colors = colors,
                // The live position is read inside the button's draw phase, not
                // here, so the ticking playback clock repaints the ring alone.
                progress = { (position().toFloat() / safeDuration).coerceIn(0f, 1f) },
            )

            Spacer(Modifier.height(14.dp))

            GlossyActionPills(
                connection = playerConnection,
                colors = colors,
                // Shuffle and Repeat rewrite the queue itself, so a listen-together
                // guest must not be able to fire them for everyone in the room.
                enabled = !flags.isGuest,
            )

            Spacer(Modifier.height(12.dp))

            GlossyDock(
                lyricsActive = showLyrics,
                onToggleLyrics = { showLyrics = !showLyrics },
                onOpenQueue = onOpenQueue,
                colors = colors,
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * The record's geometry, in fractions of the disc's own diameter. Kept in one
 * place because the artwork, the label and the tonearm's resting position all
 * describe the same object from different call sites.
 */
private object VinylDisc {
    /**
     * Radius of the disc box, as a fraction of the square that holds it.
     *
     * Slightly under the disc's old size on purpose: the corner the tonearm
     * lives in is the only space it has, and an arm has to be long enough to
     * read as an arm rather than a stub. Every point taken off the record is
     * given to the arm.
     */
    const val BoxFraction = 0.87f

    /**
     * Where the arm's bearing sits, in fractions of the square.
     *
     * As far into the corner as the counterweight behind it allows. The bearing
     * is the far end of the arm, so each unit it moves outwards is a unit of
     * arm: at 0.84/0.10 the arm was 22% of the square and read as a stub, and
     * at 0.95/0.05 it is 30% and reads as an arm.
     */
    const val PivotX = 0.95f
    const val PivotY = 0.05f

    /** Draw-to-draw step of the groove band, as a fraction of the radius. */
    const val GrooveStep = 0.009f

    /** Innermost and outermost groove, as fractions of the radius. */
    const val GrooveInner = 0.655f
    const val GrooveOuter = 0.985f

    /** The printed picture disc, and the paper label at its centre. */
    const val Artwork = 0.615f
    const val Label = 0.205f

    /** Where the stylus sits on the grooves, as a fraction of the radius. */
    const val StylusGroove = 0.78f
}

/**
 * The vinyl disc: the track's artwork pressed onto a grooved black record,
 * spinning while playing. It deliberately never draws an animated canvas — the
 * record shows plain artwork, and the canvas engines stay exclusive to the
 * other player designs.
 *
 * The disc is drawn the way a record is: a matte-black pressing with a dense,
 * modulated groove band, brighter separators where a side is banded, a run-out
 * of tight grooves around the label, and a machined edge catch-light. The
 * artwork is pressed *into* that surface (a picture disc) rather than floating
 * on top of it, and the label is a printed one with a ring and a hollow
 * spindle.
 *
 * Two layers sit outside the rotation on purpose. Grooves, artwork and label
 * turn with the record; the sheen and the rim highlight must not, because they
 * describe a light source in the room rather than the thing that is spinning.
 */
@Composable
private fun SpinningVinyl(
    thumbnailUrl: String?,
    isPlaying: Boolean,
) {
    val transition = rememberInfiniteTransition(label = "vinylSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vinylAngle",
    )
    val displayAngle = if (isPlaying) angle else 0f

    Box(
        modifier =
            Modifier
                // 0.90 keeps the disc radius at 0.45 × the parent square, which is
                // the figure VinylTonearm derives its pivot geometry from.
                .fillMaxSize(VinylDisc.BoxFraction),
        contentAlignment = Alignment.Center,
    ) {
        // Warm halo behind the pressing, so the black disc sits in the room's
        // light instead of being pasted onto it.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.62f to Color.Transparent,
                        0.90f to VinylAccent.copy(alpha = 0.05f),
                        1f to Color.Transparent,
                    ),
                    center = center,
                    radius = outerRadius,
                ),
                radius = outerRadius,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .rotate(displayAngle)
                    .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerRadius = size.minDimension / 2f
                val grooveStroke = 1.dp.toPx()

                // The pressing itself: near-black with a warm centre, so the
                // middle reads as the lit part of the disc and the edges fall
                // away.
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF201D1A),
                            0.55f to Color(0xFF141312),
                            1f to Color(0xFF070707),
                        ),
                        center = center,
                        radius = outerRadius,
                    ),
                    radius = outerRadius,
                )

                // Groove band. Two alternating alphas rather than one flat
                // stroke: a single alpha reads as printed rings, an alternating
                // one reads as a cut surface catching light.
                var position = VinylDisc.GrooveInner
                var ring = 0
                while (position <= VinylDisc.GrooveOuter) {
                    val alpha = if (ring % 2 == 0) 0.045f else 0.085f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = outerRadius * position,
                        style = Stroke(width = grooveStroke),
                    )
                    position += VinylDisc.GrooveStep
                    ring++
                }

                // Banded separators: a real side carries three or four wider
                // gaps between its tracks.
                listOf(0.735f, 0.845f, 0.945f).forEach { band ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.13f),
                        radius = outerRadius * band,
                        style = Stroke(width = grooveStroke * 2f),
                    )
                }

                // Run-out: the tight grooves the arm rides into at the end of a
                // side, hard against the label.
                listOf(0.645f, 0.656f).forEach { runOut ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.11f),
                        radius = outerRadius * runOut,
                        style = Stroke(width = grooveStroke * 1.4f),
                    )
                }

                // A shadowed step at the artwork's edge, so the picture sits in
                // the surface rather than on it.
                drawCircle(
                    color = Color.Black.copy(alpha = 0.55f),
                    radius = outerRadius * (VinylDisc.Artwork + 0.012f),
                    style = Stroke(width = 3.dp.toPx()),
                )

                // Machined edge: a dark outer band with a bright catch-light
                // just inside it.
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = outerRadius - grooveStroke / 2f,
                    style = Stroke(width = grooveStroke * 2f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = outerRadius - grooveStroke * 3f,
                    style = Stroke(width = grooveStroke * 1.2f),
                )
            }

            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailUrl)
                            .crossfade(400)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxSize(VinylDisc.Artwork)
                            .clip(CircleShape),
                )
            }

            // Centre label: printed paper, a ring of the design's accent, and a
            // hollow spindle the arm's own pivot echoes.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize(VinylDisc.Label)
                        .clip(CircleShape)
                        .background(Color(0xFF14100B))
                        .border(1.5.dp, VinylAccent.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val labelRadius = size.minDimension / 2f
                    // A warm bloom over the paper, so the label is not a flat
                    // hole punched in the middle of the record.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to VinylAccent.copy(alpha = 0.16f),
                                0.7f to VinylAccent.copy(alpha = 0.05f),
                                1f to Color.Transparent,
                            ),
                            center = center,
                            radius = labelRadius,
                        ),
                        radius = labelRadius,
                    )
                    // A single fine ring, the way a pressed label carries one.
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f),
                        radius = labelRadius * 0.80f,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }

                // Spindle: a hole with a lit rim, not a painted dot.
                Box(
                    modifier =
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF07060A))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                )
            }
        }

        // Sheen: fixed to the screen, not to the record. Two bands crossing at
        // an angle is the reflection a glossy disc shows under a ceiling light.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.085f),
                                0.22f to Color.White.copy(alpha = 0.015f),
                                0.45f to Color.Transparent,
                                0.72f to Color.White.copy(alpha = 0.045f),
                                1f to Color.Transparent,
                            ),
                        ),
                    ),
        )
    }
}

/**
 * The tonearm, drawn the way a tonearm is made: one straight axis from the
 * machined bearing to the stylus, carrying a knurled counterweight on its tail
 * and a headshell with a cartridge, a finger lift and a needle on its nose.
 *
 * The whole assembly rotates about the bearing, resting on the outer groove
 * band while playing and swinging clear when paused, and because the canvas
 * spans the whole square the geometry follows the disc's radius at any screen
 * size.
 *
 * The look is deliberately quiet. Premium arms are gunmetal and black, and the
 * finish carries them: a fine tapered tube instead of a fat bar, a highlight
 * down one edge and a shadow down the other so it reads as a cylinder, a
 * counterweight with real knurling and a set screw, an anti-skate weight on its
 * wire, and exactly one note of the design's accent — the tip of the needle.
 * A coloured block for a headshell is what a toy turntable has.
 */
@Composable
private fun VinylTonearm(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    // Rotating away from the disc lifts the stylus off the record.
    val lift by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 18f,
        animationSpec = tween(900),
        label = "tonearmAngle",
    )
    Canvas(modifier = modifier) {
        val discCentre = Offset(size.width / 2f, size.height / 2f)
        val discRadius = size.width * VinylDisc.BoxFraction / 2f
        val pivot = Offset(size.width * VinylDisc.PivotX, size.height * VinylDisc.PivotY)

        val towardsPivot = pivot - discCentre
        val stylusRest = discCentre + towardsPivot / towardsPivot.getDistance() * (discRadius * VinylDisc.StylusGroove)
        val arm = stylusRest - pivot
        val armLength = arm.getDistance()
        val armAngle = atan2(arm.y, arm.x) * 180f / PI.toFloat()

        // Gunmetal and black; the accent appears once, at the needle.
        val gunmetal = Color(0xFF4B463E)
        val gunmetalDark = Color(0xFF2A2723)
        val gunmetalLight = Color(0xFF8F8878)
        val headshellBody = Color(0xFF302D28)
        val cartridgeBody = Color(0xFF161412)
        val needleBody = Color(0xFFB9B2A2)

        rotate(degrees = armAngle + lift, pivot = pivot) {
            val px = pivot.x
            val py = pivot.y

            // ---- tail ------------------------------------------------------
            // Stub the counterweight turns on, then the weight itself: a dark
            // sleeve of knurled metal with a set screw on top.
            drawRoundRect(
                color = gunmetalDark,
                topLeft = Offset(px - 5.dp.toPx(), py - 2.6.dp.toPx()),
                size = Size(7.dp.toPx(), 5.2.dp.toPx()),
                cornerRadius = CornerRadius(1.3.dp.toPx()),
            )
            // Kept short enough that the weight stays inside the square: the
            // bearing is nearly in the corner now, and a long tail would be
            // drawn off the edge of it.
            val weightStart = px - 17.dp.toPx()
            val weightSize = Size(15.dp.toPx(), 13.dp.toPx())
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(weightStart + 0.6.dp.toPx(), py - weightSize.height / 2f + 1.2.dp.toPx()),
                size = weightSize,
                cornerRadius = CornerRadius(2.5.dp.toPx()),
            )
            drawRoundRect(
                color = gunmetalDark,
                topLeft = Offset(weightStart, py - weightSize.height / 2f),
                size = weightSize,
                cornerRadius = CornerRadius(2.5.dp.toPx()),
            )
            // Knurling: fine ridges, which is what makes the weight read as
            // metal instead of as a hole in the arm.
            var ridge = weightStart + 1.6.dp.toPx()
            val ridgeEnd = weightStart + weightSize.width - 1.6.dp.toPx()
            while (ridge <= ridgeEnd) {
                drawLine(
                    color = Color.White.copy(alpha = 0.075f),
                    start = Offset(ridge, py - weightSize.height / 2f + 1.dp.toPx()),
                    end = Offset(ridge, py + weightSize.height / 2f - 1.dp.toPx()),
                    strokeWidth = 0.7.dp.toPx(),
                )
                ridge += 2.1.dp.toPx()
            }
            // Lit top edge, shaded bottom edge.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                topLeft = Offset(weightStart + 1.dp.toPx(), py - weightSize.height / 2f + 0.8.dp.toPx()),
                size = Size(weightSize.width - 2.dp.toPx(), 1.dp.toPx()),
                cornerRadius = CornerRadius(0.5.dp.toPx()),
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.28f),
                topLeft = Offset(weightStart + 1.dp.toPx(), py + weightSize.height / 2f - 1.8.dp.toPx()),
                size = Size(weightSize.width - 2.dp.toPx(), 1.dp.toPx()),
                cornerRadius = CornerRadius(0.5.dp.toPx()),
            )
            // Set screw.
            drawCircle(
                color = gunmetalLight.copy(alpha = 0.8f),
                radius = 1.2.dp.toPx(),
                center = Offset(weightStart + weightSize.width / 2f, py - weightSize.height / 2f - 0.6.dp.toPx()),
            )

            // Anti-skate: a wire off the bearing housing with a small weight on
            // the end, the detail that says "set up by someone who cares".
            drawLine(
                color = gunmetalLight.copy(alpha = 0.4f),
                start = Offset(px - 3.dp.toPx(), py + 4.5.dp.toPx()),
                end = Offset(px + 0.5.dp.toPx(), py + 12.dp.toPx()),
                strokeWidth = 0.7.dp.toPx(),
            )
            drawCircle(
                color = gunmetal,
                radius = 2.dp.toPx(),
                center = Offset(px + 0.5.dp.toPx(), py + 13.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = 2.dp.toPx(),
                center = Offset(px + 0.5.dp.toPx(), py + 13.dp.toPx()),
                style = Stroke(0.7.dp.toPx()),
            )

            // ---- tube ------------------------------------------------------
            // Tapered, drawn as a closed path rather than a bar: it is narrow
            // at the headshell and slightly wider at the bearing, which is how
            // a real arm is machined. A dark copy behind it lifts the arm off
            // the artwork.
            val headshellLength = 21.dp.toPx()
            val tubeStart = 2.dp.toPx()
            val tubeEnd = armLength - headshellLength + 2.dp.toPx()
            val halfAtBearing = 1.95.dp.toPx()
            val halfAtHeadshell = 1.25.dp.toPx()
            val shadowPath =
                Path().apply {
                    moveTo(px + tubeStart, py - halfAtBearing + 1.4.dp.toPx())
                    lineTo(px + tubeEnd, py - halfAtHeadshell + 1.4.dp.toPx())
                    lineTo(px + tubeEnd, py + halfAtHeadshell + 1.4.dp.toPx())
                    lineTo(px + tubeStart, py + halfAtBearing + 1.4.dp.toPx())
                    close()
                }
            drawPath(path = shadowPath, color = Color.Black.copy(alpha = 0.3f))
            val tubePath =
                Path().apply {
                    moveTo(px + tubeStart, py - halfAtBearing)
                    lineTo(px + tubeEnd, py - halfAtHeadshell)
                    lineTo(px + tubeEnd, py + halfAtHeadshell)
                    lineTo(px + tubeStart, py + halfAtBearing)
                    close()
                }
            drawPath(path = tubePath, color = gunmetal)
            // Cylinder shading: bright along the top, dark along the bottom.
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(px + tubeStart, py - halfAtBearing + 0.9.dp.toPx()),
                end = Offset(px + tubeEnd, py - halfAtHeadshell + 0.7.dp.toPx()),
                strokeWidth = 0.9.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.35f),
                start = Offset(px + tubeStart, py + halfAtBearing - 0.4.dp.toPx()),
                end = Offset(px + tubeEnd, py + halfAtHeadshell - 0.3.dp.toPx()),
                strokeWidth = 0.9.dp.toPx(),
                cap = StrokeCap.Round,
            )

            // ---- head ---------------------------------------------
            // Cueing lever beside the bearing, where a hand finds it.
            drawRoundRect(
                color = gunmetal,
                topLeft = Offset(px + 6.dp.toPx(), py + 3.5.dp.toPx()),
                size = Size(2.2.dp.toPx(), 13.dp.toPx()),
                cornerRadius = CornerRadius(1.1.dp.toPx()),
            )
            drawCircle(
                color = gunmetalLight,
                radius = 1.9.dp.toPx(),
                center = Offset(px + 7.1.dp.toPx(), py + 16.5.dp.toPx()),
            )

            // Headshell: a slim plate, angled down onto the record, carrying the
            // cartridge.
            val headStart = px + armLength - headshellLength
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.3f),
                topLeft = Offset(headStart + 0.8.dp.toPx(), py - 6.dp.toPx() + 1.2.dp.toPx()),
                size = Size(headshellLength, 12.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            drawRoundRect(
                color = headshellBody,
                topLeft = Offset(headStart, py - 6.dp.toPx()),
                size = Size(headshellLength, 12.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(headStart + 1.5.dp.toPx(), py - 5.4.dp.toPx()),
                size = Size(headshellLength - 3.dp.toPx(), 1.dp.toPx()),
                cornerRadius = CornerRadius(0.5.dp.toPx()),
            )
            // Cartridge under the plate, with the two mounting screws.
            drawRoundRect(
                color = cartridgeBody,
                topLeft = Offset(headStart + 1.5.dp.toPx(), py - 2.4.dp.toPx()),
                size = Size(headshellLength - 3.dp.toPx(), 8.dp.toPx()),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
            listOf(4.dp, 14.dp).forEach { offset ->
                drawCircle(
                    color = gunmetalLight.copy(alpha = 0.55f),
                    radius = 0.8.dp.toPx(),
                    center = Offset(headStart + offset.toPx(), py + 3.4.dp.toPx()),
                )
            }
            // Finger lift: the small tab that makes the head liftable.
            drawRoundRect(
                color = gunmetalLight.copy(alpha = 0.9f),
                topLeft = Offset(px + armLength - 3.4.dp.toPx(), py - 11.5.dp.toPx()),
                size = Size(2.2.dp.toPx(), 6.dp.toPx()),
                cornerRadius = CornerRadius(1.1.dp.toPx()),
            )
            // Stylus: the one accent on the arm, at the point the geometry put
            // on the grooves.
            drawLine(
                color = needleBody,
                start = Offset(px + armLength - 1.6.dp.toPx(), py + 5.dp.toPx()),
                end = Offset(px + armLength - 0.6.dp.toPx(), py + 8.4.dp.toPx()),
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = VinylAccent.copy(alpha = 0.95f),
                radius = 0.85.dp.toPx(),
                center = Offset(px + armLength - 0.6.dp.toPx(), py + 8.4.dp.toPx()),
            )

            // ---- bearing ---------------------------------------------------
            // Drawn last so the tube passes under it: a machined housing with a
            // shadowed rim, a lit face and a dark centre.
            drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = 11.6.dp.toPx(), center = pivot)
            drawCircle(color = Color(0xFF17150F), radius = 10.4.dp.toPx(), center = pivot)
            drawCircle(color = gunmetal, radius = 8.2.dp.toPx(), center = pivot)
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = 7.7.dp.toPx(),
                center = pivot,
                style = Stroke(0.9.dp.toPx()),
            )
            drawCircle(color = Color(0xFF26231E), radius = 4.dp.toPx(), center = pivot)
            drawCircle(color = VinylAccent.copy(alpha = 0.75f), radius = 1.5.dp.toPx(), center = pivot)
        }
    }
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun TrackMenuButton(
    bottomSheetState: BottomSheetState,
    colors: ArtworkColors,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    GlossyIconButton(
        icon = R.drawable.more_vert,
        container = colors.side.copy(alpha = 0.65f),
        onClick = {
            mediaMetadata?.let { metadata ->
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = metadata,
                        playerBottomSheetState = bottomSheetState,
                        onShowDetailsDialog = {
                            bottomSheetPageState.show { ShowMediaInfo(metadata.id) }
                        },
                        onDismiss = { menuState.dismiss() },
                    )
                }
            }
        },
    )
}

@Composable
private fun VinylTopBar(
    bottomSheetState: BottomSheetState,
    onCollapse: () -> Unit,
    title: String,
    artist: String,
    colors: ArtworkColors,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIconButton(
            icon = R.drawable.arrow_back,
            onClick = onCollapse,
            tint = VinylAccent,
            container = colors.side.copy(alpha = 0.65f),
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The design's own name is not useful here — the bar tells the
            // listener what is playing, the way the other designs do.
            Text(
                text = artist,
                color = VinylTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = title,
                color = Color(0xFFF2E6D0),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TrackMenuButton(bottomSheetState = bottomSheetState, colors = colors)
    }
}

@Composable
private fun GlossyIconButton(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    /** Tonal disc behind the icon. Null leaves a bare icon, for rows that
     *  already sit on a surface of their own. */
    container: Color? = null,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .then(
                if (container != null) {
                    Modifier
                        .background(container)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun VinylTransportRow(
    connection: PlayerConnection,
    flags: PlayerFlags,
    colors: ArtworkColors,
    /** 0f..1f playback progress, drawn around the label button. */
    progress: () -> Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VinylSkipButton(
            icon = R.drawable.skip_previous,
            enabled = flags.canSkipPrevious && !flags.isGuest,
            colors = colors,
            onClick = connection::seekToPrevious,
        )
        VinylLabelButton(
            flags = flags,
            colors = colors,
            progress = progress,
            onClick = playPauseAction(connection, flags),
        )
        VinylSkipButton(
            icon = R.drawable.skip_next,
            enabled = flags.canSkipNext && !flags.isGuest,
            colors = colors,
            onClick = connection::seekToNext,
        )
    }
}

private fun transportIcon(flags: PlayerFlags): Int = when {
    flags.isGuest -> if (flags.isMuted) R.drawable.volume_off else R.drawable.volume_up
    flags.playbackState == Player.STATE_ENDED -> R.drawable.replay
    flags.isPlaying -> R.drawable.pause
    else -> R.drawable.play
}

/** Diameter of the accent label button — the turntable's centrepiece. */
private val LabelButtonSize = 86.dp

/** Diameter of the tonal skip discs that flank it. */
private val SkipButtonSize = 56.dp

/**
 * The label button: the accent disc at the centre of the transport, wearing the
 * song's progress around its rim the way a record's label carries the groove.
 *
 * The ring is read from [progress] inside the draw phase, so the ticking
 * playback clock repaints this button rather than recomposing the transport row
 * — or the whole screen, which is what a position read in the screen body used
 * to do ten times a second.
 */
@Composable
private fun VinylLabelButton(
    flags: PlayerFlags,
    colors: ArtworkColors,
    progress: () -> Float,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "labelButtonScale",
    )
    Box(
        modifier = Modifier
            .size(LabelButtonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // The button is the one raised element on the deck, so it gets a
            // real cast shadow instead of a flat tint.
            .artworkDropShadow(CircleShape, radius = 22.dp, offsetY = 10.dp, alpha = 0.45f)
            .clip(CircleShape)
            .background(colors.accent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.5.dp.toPx()
            val inset = stroke / 2f + 6.dp.toPx()
            val ringSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            // The whole rim is always drawn, so the unplayed remainder reads as
            // track rather than as an empty gap.
            drawArc(
                color = colors.onAccent.copy(alpha = 0.20f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = ringSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val played = progress().coerceIn(0f, 1f)
            if (played > 0.002f) {
                drawArc(
                    color = colors.onAccent.copy(alpha = 0.90f),
                    startAngle = -90f,
                    sweepAngle = 360f * played,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = ringSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Icon(
            painter = painterResource(transportIcon(flags)),
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(38.dp),
        )
    }
}

/**
 * A tonal skip disc: round, rimmed and deliberately quiet. The accent belongs to
 * the label button, so previous/next read as its neighbours instead of three
 * competing blobs in a row of rounded squares.
 */
@Composable
private fun VinylSkipButton(
    icon: Int,
    enabled: Boolean,
    colors: ArtworkColors,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "skipButtonScale",
    )
    Box(
        modifier = Modifier
            .size(SkipButtonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // A disabled disc fades in place instead of dropping its fill,
                // so the transport keeps its shape at the ends of the queue.
                alpha = if (enabled) 1f else 0.42f
            }
            .clip(CircleShape)
            .background(colors.side)
            .border(1.dp, colors.onSide.copy(alpha = 0.14f), CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.onSide,
            modifier = Modifier.size(26.dp),
        )
    }
}

/**
 * The action row under the transport: Shuffle, Like, Download and Repeat.
 *
 * These four were previously Like / Download / Repeat, which left Shuffle —
 * the one playback mode users reach for most, and the only mode with no way to
 * reach it from this screen — with no button at all. All four are icon-only so
 * the row reads as one set of equal controls rather than a circle next to two
 * wide labelled pills.
 */
@Composable
private fun GlossyActionPills(
    connection: PlayerConnection,
    colors: ArtworkColors,
    /** A guest does not own the queue, so the mode toggles are theirs to break. */
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val mediaMetadata by connection.mediaMetadata.collectAsStateWithLifecycle()
    val currentSong by connection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val shuffleEnabled by connection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by connection.repeatMode.collectAsStateWithLifecycle()
    val isEpisode = currentSong?.song?.isEpisode == true
    val isFavorite = if (isEpisode) {
        currentSong?.song?.inLibrary != null
    } else {
        currentSong?.song?.liked == true
    }
    val isDownloaded = currentSong?.song?.isDownloaded == true

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionPill(
            icon = if (shuffleEnabled) R.drawable.shuffle_on else R.drawable.shuffle,
            label = "Shuffle",
            active = shuffleEnabled,
            colors = colors,
            enabled = enabled,
            showLabel = false,
            onClick = { connection.player.shuffleModeEnabled = !shuffleEnabled },
        )
        ActionPill(
            icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
            label = "Like",
            active = isFavorite,
            colors = colors,
            showLabel = false,
            onClick = { connection.toggleLike() },
        )
        ActionPill(
            icon = if (isDownloaded) R.drawable.check else R.drawable.download,
            label = if (isDownloaded) "Downloaded" else "Download",
            active = isDownloaded,
            colors = colors,
            showLabel = false,
            onClick = {
                mediaMetadata?.let { metadata ->
                    if (isDownloaded) {
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            metadata.id,
                            false,
                        )
                    } else {
                        database.transaction { insert(metadata) }
                        val downloadRequest = DownloadRequest
                            .Builder(metadata.id, metadata.id.toUri())
                            .setCustomCacheKey(metadata.id)
                            .setData(metadata.title.toByteArray())
                            .build()
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                    }
                }
            },
        )
        ActionPill(
            icon = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
            label = "Repeat",
            active = repeatMode != Player.REPEAT_MODE_OFF,
            colors = colors,
            enabled = enabled,
            showLabel = false,
            onClick = { connection.player.toggleRepeatMode() },
        )
    }
}

@Composable
private fun ActionPill(
    icon: Int,
    label: String,
    active: Boolean,
    colors: ArtworkColors,
    onClick: () -> Unit,
    showLabel: Boolean = true,
    /** A disabled pill fades in place, so the row keeps its shape and rhythm. */
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "pillScale",
    )
    val container = if (active) colors.accent else colors.surface
    val content = if (active) colors.onAccent else colors.onSurface
    // Every pill carries a rim: it is what makes a row of cream, accent and
    // icon-only buttons read as one set of controls rather than three styles.
    val rim = if (active) colors.onAccent.copy(alpha = 0.35f) else colors.onSurface.copy(alpha = 0.12f)
    val shape = if (showLabel) RoundedCornerShape(50) else CircleShape

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                if (!enabled) alpha = 0.42f
            }
            .then(if (showLabel) Modifier.height(46.dp) else Modifier.size(46.dp))
            .clip(shape)
            .background(container)
            .border(1.dp, rim, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (showLabel) 20.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = content,
                modifier = Modifier.size(18.dp),
            )
            if (showLabel) {
                Spacer(Modifier.width(7.dp))
                Text(
                    text = label,
                    color = content,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun GlossyDock(
    lyricsActive: Boolean,
    onToggleLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    colors: ArtworkColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // Centred rather than spread edge to edge: the Cast button renders
        // nothing in builds without Google Cast (FOSS/izzy), which left a hole
        // in the middle of a three-slot row that was laid out to be full.
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionPill(
            icon = R.drawable.lyrics,
            label = "Lyrics",
            active = lyricsActive,
            colors = colors,
            onClick = onToggleLyrics,
        )
        CastButton(
            modifier = Modifier.size(46.dp),
            tintColor = colors.onSurface,
        )
        ActionPill(
            icon = R.drawable.queue_music,
            label = "Queue",
            active = false,
            colors = colors,
            onClick = onOpenQueue,
        )
    }
}

// ---------------------------------------------------------------------------
// Shared pieces for the Capsule and Cinematic designs
// ---------------------------------------------------------------------------

/**
 * A round icon button for the two preview designs: press-scaled, with an
 * optional tonal disc and rim, and a quiet fade when it is disabled.
 */
@Composable
private fun StyleIconButton(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    iconSize: Dp = 24.dp,
    buttonSize: Dp = 44.dp,
    container: Color? = null,
    rim: Color? = null,
    enabled: Boolean = true,
    /** Extra fade for quiet side controls — pressed feedback is a scale. */
    contentAlpha: Float = 1f,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "styleIconButtonScale",
    )
    Box(
        modifier = modifier
            .size(buttonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) contentAlpha else contentAlpha * 0.42f
            }
            .clip(CircleShape)
            .then(if (container != null) Modifier.background(container) else Modifier)
            .then(if (rim != null) Modifier.border(1.dp, rim, CircleShape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** The overflow (⋮) button for both new designs — it opens the player menu. */
@Composable
private fun StyleMenuButton(
    bottomSheetState: BottomSheetState,
    tint: Color,
    container: Color? = null,
    rim: Color? = null,
    buttonSize: Dp = 44.dp,
    iconSize: Dp = 24.dp,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    StyleIconButton(
        icon = R.drawable.more_vert,
        tint = tint,
        container = container,
        rim = rim,
        buttonSize = buttonSize,
        iconSize = iconSize,
        contentDescription = "More options",
        onClick = {
            mediaMetadata?.let { metadata ->
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = metadata,
                        playerBottomSheetState = bottomSheetState,
                        onShowDetailsDialog = {
                            bottomSheetPageState.show { ShowMediaInfo(metadata.id) }
                        },
                        onDismiss = { menuState.dismiss() },
                    )
                }
            }
        },
    )
}

/** The heart, with the liked state read from the current song. */
@Composable
private fun StyleLikeButton(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    iconSize: Dp = 24.dp,
    buttonSize: Dp = 44.dp,
    contentAlpha: Float = 1f,
) {
    val connection = LocalPlayerConnection.current ?: return
    val currentSong by connection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val isEpisode = currentSong?.song?.isEpisode == true
    val liked = if (isEpisode) {
        currentSong?.song?.inLibrary != null
    } else {
        currentSong?.song?.liked == true
    }

    StyleIconButton(
        icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
        onClick = { connection.toggleLike() },
        modifier = modifier,
        tint = tint,
        iconSize = iconSize,
        buttonSize = buttonSize,
        contentAlpha = contentAlpha,
        contentDescription = "Like",
    )
}

/**
 * The centre transport disc: pale pink for Capsule, white for Cinematic.
 *
 * [PlayerFlags] and the player connection decide play / pause / replay / the
 * guest's volume controls, exactly as the Vinyl label button does.
 */
@Composable
private fun StylePlayDisc(
    flags: PlayerFlags,
    connection: PlayerConnection,
    size: Dp,
    iconSize: Dp,
    surface: Color,
    ink: Color,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "stylePlayDiscScale",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(surface)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = playPauseAction(connection, flags),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(transportIcon(flags)),
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Artwork for the new designs: the shared thumbnail's high-resolution URL
 * rewrite, straight into a plain `AsyncImage`. Like Vinyl, these designs never
 * paint an animated canvas — the cover stays a still image whatever the
 * "Canvas Background" switch says.
 */
@Composable
private fun StyleArtwork(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val highResUrl = remember(thumbnailUrl) {
        thumbnailUrl
            ?.replace(Regex("=[wh]\\d+-[wh]\\d+.*"), "=w1080-h1080-l90-rj")
            ?.replace(Regex("-[wh]\\d+-[wh]\\d+.*"), "-w1080-h1080-l90-rj")
            ?.replace(Regex("=s\\d+.*"), "=s1080-l90-rj")
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(highResUrl)
            .crossfade(400)
            .build(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}

/**
 * The straight seek bar both new designs use, drawn rather than assembled so
 * the track, played bar, thumb tick and end dot land on the mock's geometry.
 *
 * [fraction] and the two callbacks are read through [rememberUpdatedState], so
 * the gesture handlers always see the live playback progress instead of the
 * value captured when the pointer input was installed.
 */
@Composable
private fun StyleSeekBar(
    fraction: Float,
    onScrub: (Float) -> Unit,
    onCommit: (Float) -> Unit,
    trackHeight: Dp,
    cornerRadius: Dp,
    trackColor: Color,
    playedColor: Color,
    canvasHeight: Dp,
    modifier: Modifier = Modifier,
    tickThumb: Boolean = false,
    endDot: Boolean = false,
) {
    val fractionState by rememberUpdatedState(fraction)
    val scrubState by rememberUpdatedState(onScrub)
    val commitState by rememberUpdatedState(onCommit)
    var dragging by remember { mutableStateOf<Float?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeight)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val tapped = (offset.x / size.width).coerceIn(0f, 1f)
                    scrubState(tapped)
                    commitState(tapped)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        dragging?.let(commitState)
                        dragging = null
                    },
                    onDragCancel = { dragging = null },
                ) { change, dragAmount ->
                    change.consume()
                    val next = ((dragging ?: fractionState) + dragAmount / size.width)
                        .coerceIn(0f, 1f)
                    dragging = next
                    scrubState(next)
                }
            },
    ) {
        val canvasHeightPx = canvasHeight.toPx()
        val trackHeightPx = trackHeight.toPx()
        val trackTop = (canvasHeightPx - trackHeightPx) / 2f
        val radius = cornerRadius.toPx()
        val playedWidth = size.width * fraction

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackHeightPx),
            cornerRadius = CornerRadius(radius),
        )
        if (playedWidth > 0f) {
            drawRoundRect(
                color = playedColor,
                topLeft = Offset(0f, trackTop),
                size = Size(maxOf(playedWidth, trackHeightPx), trackHeightPx),
                cornerRadius = CornerRadius(radius),
            )
        }
        if (tickThumb) {
            val tickWidth = 4.dp.toPx()
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(
                    playedWidth - tickWidth / 2f,
                    (canvasHeightPx - 40.dp.toPx()) / 2f,
                ),
                size = Size(tickWidth, 40.dp.toPx()),
                cornerRadius = CornerRadius(tickWidth / 2f),
            )
        }
        if (endDot) {
            val dot = 5.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = dot / 2f,
                center = Offset(size.width - dot / 2f - 2.dp.toPx(), trackTop + trackHeightPx / 2f),
            )
        }
    }
}

/**
 * Seek bar plus its timestamps, kept in their own composable so the ticking
 * playback clock repaints this block alone instead of the whole design.
 */
@Composable
private fun StyleSeekBlock(
    position: () -> Long,
    duration: Long,
    trackHeight: Dp,
    cornerRadius: Dp,
    trackColor: Color,
    playedColor: Color,
    labelColor: Color,
    canvasHeight: Dp,
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 11.sp,
    labelHorizontalPadding: Dp = 0.dp,
    tickThumb: Boolean = false,
    endDot: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current
    val safeDuration = if (duration > 0L) duration else 1L
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val livePosition = position()
    val displayPosition = scrubFraction?.let { (it * safeDuration).toLong() } ?: livePosition
    val displayFraction = scrubFraction
        ?: ((livePosition.toFloat() / safeDuration).coerceIn(0f, 1f))

    Column(modifier = modifier.fillMaxWidth()) {
        StyleSeekBar(
            fraction = displayFraction,
            onScrub = { scrubFraction = it },
            onCommit = { committed ->
                scrubFraction = null
                playerConnection?.player?.seekTo((committed * safeDuration).toLong())
            },
            trackHeight = trackHeight,
            cornerRadius = cornerRadius,
            trackColor = trackColor,
            playedColor = playedColor,
            canvasHeight = canvasHeight,
            tickThumb = tickThumb,
            endDot = endDot,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = labelHorizontalPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = makeTimeString(displayPosition),
                color = labelColor,
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (duration != C.TIME_UNSET) makeTimeString(safeDuration) else "",
                color = labelColor,
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Capsule
// ---------------------------------------------------------------------------

/**
 * Capsule's colours when there is nothing to sample yet: the maroon of the
 * original reference. As soon as a song with artwork is playing the background
 * comes from that artwork's palette instead — see [capsuleGradientStops].
 */
internal val CapsuleFallbackColors = ArtworkColors(
    accent = Color(0xFF8E0B03),
    onAccent = Color(0xFFFFF0EB),
    side = Color(0xFF3A100A),
    onSide = Color(0xFFFFF0EB),
    surface = Color(0xFF7B0501),
    onSurface = Color(0xFFFFF0EB),
)

/**
 * The Capsule background: nine stops that fall from the song's own colour to
 * near-black, so the screen wears the artwork's gradient instead of a fixed
 * brand red. Pale accents are deepened first — white type sits on this — and
 * the first stop is what the sheet paints in the status-bar band above.
 */
internal fun capsuleGradientStops(colors: ArtworkColors): List<Color> {
    val base =
        if (colors.accent.luminance() > 0.35f) {
            lerp(colors.accent, Color.Black, 0.45f)
        } else {
            colors.accent
        }
    val top = lerp(base, Color.Black, 0.08f)
    return listOf(0.06f, 0.14f, 0.24f, 0.34f, 0.45f, 0.56f, 0.68f, 0.79f, 0.88f)
        .map { stop -> lerp(top, Color.Black, stop).copy(alpha = 1f) }
}

private val CapsuleInk = Color(0xFFFFF0EB)
private val CapsulePlaySurface = Color(0xFFFFB4A8)
private val CapsulePlayInk = Color(0xFF111112)

/**
 * Capsule: the maroon vertical gradient, a square cover over it, and the
 * transport parked inside one dark pill with a pale-pink play disc.
 *
 * Self-contained like [VinylNowPlaying] — it paints its own background, header,
 * artwork and bottom row, so it opts out of the shared thumbnail, controls and
 * queue peek.
 */
@Composable
fun CapsuleNowPlaying(
    bottomSheetState: BottomSheetState,
    /** Live playback position — a provider, so the ticking clock stays a leaf. */
    position: () -> Long,
    duration: Long,
    /** Nine artwork-derived stops; the first one is the band above the sheet. */
    backgroundStops: List<Color>,
    onOpenQueue: () -> Unit,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT,
    )
    val canvasEnabled = rememberCanvasEnabled()
    val canvasCorner = rememberCanvasCornerSize()
    var canvasReady by remember { mutableStateOf(false) }
    // The still cover fades out once a canvas frame is really up, so the video
    // and the artwork are never both drawn every frame.
    val coverAlpha by animateFloatAsState(
        targetValue = if (canvasReady) 0f else 1f,
        animationSpec = tween(250),
        label = "capsuleCoverAlpha",
    )
    val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
    val shuffleEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val flags = rememberPlayerFlags()
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    val showLyricsOnPlayer by rememberPreference(ShowLyricsOnPlayerKey, defaultValue = false)
    val (miniLyricsStyle) = rememberEnumPreference(MiniLyricsStyleKey, defaultValue = MiniLyricsStyle.CLASSIC)

    // What the small centred line under the header is playing from.
    val playingFrom = queueTitle ?: mediaMetadata?.album?.title

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (playerBackground == PlayerBackgroundStyle.DEFAULT) {
                    Brush.verticalGradient(backgroundStops)
                } else {
                    // Blur / Gradient / Animated Mesh was picked, so the design
                    // steps aside and the sheet's own background shows through,
                    // leaving only a scrim under the white type.
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.45f)),
                    )
                },
            )
            .statusBarsPadding(),
    ) {
        if (showLyrics) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .padding(bottom = bottomInset),
            ) {
                InlineLyricsView(
                    mediaMetadata = mediaMetadata,
                    showLyrics = showLyrics,
                    positionProvider = position,
                )
                CapsuleBottomRow(
                    lyricsActive = true,
                    onToggleLyrics = { showLyrics = false },
                    onOpenQueue = onOpenQueue,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp)
                .padding(bottom = bottomInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleIconButton(
                    icon = R.drawable.expand_more,
                    onClick = { bottomSheetState.collapseSoft() },
                    tint = Color(0xFFFFFAF5),
                    iconSize = 26.dp,
                    contentDescription = "Collapse",
                )
                Text(
                    text = "Now Playing",
                    color = CapsuleInk.copy(alpha = 0.73f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                StyleMenuButton(bottomSheetState = bottomSheetState, tint = Color(0xFFFFF7EF))
            }

            if (!playingFrom.isNullOrBlank()) {
                Text(
                    text = playingFrom,
                    color = Color(0xFFFFECE5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // The cover keeps its square shape and shares the leftover height
            // above and below instead of pooling under the controls.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 21.dp),
                contentAlignment = Alignment.Center,
            ) {
                val coverSide =
                    if (maxHeight.value.isFinite()) minOf(maxWidth, maxHeight) else maxWidth
                Box(
                    modifier = Modifier
                        .size(coverSide)
                        .clip(RoundedCornerShape(if (canvasEnabled) canvasCorner else 28.dp))
                        .background(Color(0xFF3A0C07)),
                ) {
                    if (!canvasEnabled) {
                        StyleArtwork(
                            thumbnailUrl = mediaMetadata?.thumbnailUrl,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = coverAlpha }) {
                            StyleArtwork(
                                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        PlayerCanvasArtwork(
                            mediaId = mediaMetadata?.id,
                            title = mediaMetadata?.title.orEmpty(),
                            artist = mediaMetadata?.artists?.joinToArtistString(", ") { it.name }.orEmpty(),
                            album = mediaMetadata?.album?.title.orEmpty(),
                            modifier = Modifier.fillMaxSize(),
                            onVideoReady = { canvasReady = it },
                        )
                    }

                    // Canvas-glow mini lyrics: the line being sung is painted on
                    // the cover itself, low — just above the cover's bottom
                    // edge — so it costs the design no height at all and the
                    // artwork above stays exactly the size it was.
                    if (showLyricsOnPlayer && !showLyrics && miniLyricsStyle == MiniLyricsStyle.CANVAS_GLOW) {
                        PlayerCanvasGlowLyrics(
                            mediaMetadata = mediaMetadata,
                            positionProvider = position,
                            accent = CapsulePlaySurface,
                            textSize = 22.sp,
                            alignment = Alignment.BottomCenter,
                            onExpand = { showLyrics = true },
                            modifier = Modifier.fillMaxSize().padding(bottom = 18.dp),
                        )
                    }
                }
            }

            // The "Show lyrics on player" strip every other design carries: the
            // active line under the cover, tappable to open the full lyrics
            // view. The artwork above is weighted, so it gives up the room.
            if (showLyricsOnPlayer && !showLyrics && miniLyricsStyle == MiniLyricsStyle.CLASSIC) {
                Spacer(Modifier.height(14.dp))
                PlayerSyncedLyricsView(
                    mediaMetadata = mediaMetadata,
                    positionProvider = position,
                    accent = CapsulePlaySurface,
                    onExpand = { showLyrics = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalPadding = 0.dp,
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata?.title.orEmpty(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                    Text(
                        text = mediaMetadata?.artists?.joinToArtistString(", ") { it.name }.orEmpty(),
                        color = Color(0xFFFFF8F4).copy(alpha = 0.84f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                }
                Spacer(Modifier.width(8.dp))
                StyleLikeButton(
                    tint = CapsuleInk,
                    iconSize = 26.dp,
                    contentAlpha = 0.7f,
                )
            }

            Spacer(Modifier.height(24.dp))

            StyleSeekBlock(
                position = position,
                duration = duration,
                trackHeight = 4.dp,
                cornerRadius = 2.dp,
                trackColor = Color.White.copy(alpha = 0.19f),
                playedColor = Color(0xFFFFFDFB),
                labelColor = Color(0xFFFFEEE8).copy(alpha = 0.56f),
                canvasHeight = 20.dp,
                labelHorizontalPadding = 31.dp,
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(94.dp)
                    .padding(horizontal = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleIconButton(
                    icon = if (shuffleEnabled) R.drawable.shuffle_on else R.drawable.shuffle,
                    onClick = { playerConnection.player.shuffleModeEnabled = !shuffleEnabled },
                    tint = CapsuleInk,
                    contentAlpha = if (shuffleEnabled) 1f else 0.6f,
                    enabled = !flags.isGuest,
                    contentDescription = "Shuffle",
                )

                Box(
                    modifier = Modifier
                        .height(94.dp)
                        .clip(RoundedCornerShape(47.dp))
                        .background(Color.White.copy(alpha = 0.11f))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StyleIconButton(
                            icon = R.drawable.skip_previous,
                            onClick = playerConnection::seekToPrevious,
                            iconSize = 34.dp,
                            tint = Color.White,
                            enabled = flags.canSkipPrevious && !flags.isGuest,
                            contentDescription = "Previous",
                        )
                        StylePlayDisc(
                            flags = flags,
                            connection = playerConnection,
                            size = 70.dp,
                            iconSize = 30.dp,
                            surface = CapsulePlaySurface,
                            ink = CapsulePlayInk,
                        )
                        StyleIconButton(
                            icon = R.drawable.skip_next,
                            onClick = playerConnection::seekToNext,
                            iconSize = 34.dp,
                            tint = Color.White,
                            enabled = flags.canSkipNext && !flags.isGuest,
                            contentDescription = "Next",
                        )
                    }
                }

                StyleIconButton(
                    icon = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        R.drawable.repeat_one
                    } else {
                        R.drawable.repeat
                    },
                    onClick = { playerConnection.player.toggleRepeatMode() },
                    tint = CapsuleInk,
                    contentAlpha = if (repeatMode != Player.REPEAT_MODE_OFF) 1f else 0.6f,
                    contentDescription = "Repeat",
                )
            }

            Spacer(Modifier.height(4.dp))

            CapsuleBottomRow(
                lyricsActive = showLyrics,
                onToggleLyrics = { showLyrics = true },
                onOpenQueue = onOpenQueue,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Lyrics and Queue, as the two quiet icons across the Capsule's bottom row. */
@Composable
private fun CapsuleBottomRow(
    lyricsActive: Boolean,
    onToggleLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        // 118 dp of box gap puts the two 24 dp glyphs 138 dp apart, which is
        // what the reference row measures.
        horizontalArrangement = Arrangement.spacedBy(118.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StyleIconButton(
            icon = R.drawable.lyrics,
            onClick = onToggleLyrics,
            tint = Color(0xFFFFF2EE),
            contentAlpha = if (lyricsActive) 1f else 0.84f,
            contentDescription = "Lyrics",
        )
        StyleIconButton(
            icon = R.drawable.queue_music,
            onClick = onOpenQueue,
            tint = Color(0xFFFFF2EE),
            contentAlpha = 0.84f,
            contentDescription = "Queue",
        )
    }
}

// ---------------------------------------------------------------------------
// Cinematic
// ---------------------------------------------------------------------------

/** The three glass discs' fill: black at 34% over the artwork. */
private val CinematicGlass = Color(0x57000000)
private val CinematicRim = Color.White.copy(alpha = 0.07f)

/**
 * Cinematic: the cover fills the screen behind black scrims, with a chevron and
 * three glass discs up top, and a five-up transport led by a white play disc.
 *
 * Self-contained like [VinylNowPlaying] — it paints its own background, header,
 * artwork and transport, so it opts out of the shared thumbnail, controls and
 * queue peek.
 */
@Composable
fun CinematicNowPlaying(
    bottomSheetState: BottomSheetState,
    /** Live playback position — a provider, so the ticking clock stays a leaf. */
    position: () -> Long,
    duration: Long,
    onOpenQueue: () -> Unit,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val shuffleEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val flags = rememberPlayerFlags()
    val canvasEnabled = rememberCanvasEnabled()
    var canvasReady by remember { mutableStateOf(false) }
    // The still cover stays until a canvas frame is really up.
    val coverAlpha by animateFloatAsState(
        targetValue = if (canvasReady) 0f else 1f,
        animationSpec = tween(250),
        label = "cinematicCoverAlpha",
    )
    var showSleepTimer by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    val showLyricsOnPlayer by rememberPreference(ShowLyricsOnPlayerKey, defaultValue = false)
    val (miniLyricsStyle) = rememberEnumPreference(MiniLyricsStyleKey, defaultValue = MiniLyricsStyle.CLASSIC)

    if (showSleepTimer) {
        SleepTimerPrompt(onDismiss = { showSleepTimer = false })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (canvasEnabled) coverAlpha else 1f },
        ) {
            StyleArtwork(
                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (canvasEnabled) {
            // Full-bleed canvas: the animated artwork is the whole backdrop, with
            // the scrims and controls drawn on top of it.
            PlayerCanvasArtwork(
                mediaId = mediaMetadata?.id,
                title = mediaMetadata?.title.orEmpty(),
                artist = mediaMetadata?.artists?.joinToArtistString(", ") { it.name }.orEmpty(),
                album = mediaMetadata?.album?.title.orEmpty(),
                modifier = Modifier.fillMaxSize(),
                onVideoReady = { canvasReady = it },
            )
        }
        // Legibility scrims: one over the header, one under the transport.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        // Nearly solid at the very top, so the strip the sheet
                        // paints behind the status bar meets the artwork without
                        // a visible seam.
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(430.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 6.dp)
                .padding(bottom = bottomInset),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 21.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleIconButton(
                    icon = R.drawable.expand_more,
                    onClick = { bottomSheetState.collapseSoft() },
                    tint = Color(0xFFFFFAF5),
                    iconSize = 26.dp,
                    contentDescription = "Collapse",
                )
                Spacer(Modifier.weight(1f))
                StyleIconButton(
                    icon = R.drawable.bedtime,
                    onClick = { showSleepTimer = true },
                    container = CinematicGlass,
                    rim = CinematicRim,
                    buttonSize = 40.dp,
                    iconSize = 22.dp,
                    contentDescription = "Sleep timer",
                )
                Spacer(Modifier.width(10.dp))
                StyleIconButton(
                    icon = R.drawable.lyrics,
                    onClick = { showLyrics = !showLyrics },
                    // The active disc brightens its glass so the open lyrics panel
                    // is obviously the one on screen.
                    container = if (showLyrics) Color.White.copy(alpha = 0.26f) else CinematicGlass,
                    rim = if (showLyrics) Color.White.copy(alpha = 0.30f) else CinematicRim,
                    buttonSize = 40.dp,
                    iconSize = 22.dp,
                    contentDescription = "Lyrics",
                )
                Spacer(Modifier.width(10.dp))
                StyleIconButton(
                    icon = R.drawable.queue_music,
                    onClick = onOpenQueue,
                    container = CinematicGlass,
                    rim = CinematicRim,
                    buttonSize = 40.dp,
                    iconSize = 22.dp,
                    contentDescription = "Queue",
                )
                Spacer(Modifier.width(10.dp))
                StyleMenuButton(
                    bottomSheetState = bottomSheetState,
                    tint = Color.White,
                    container = CinematicGlass,
                    rim = CinematicRim,
                    buttonSize = 40.dp,
                    iconSize = 24.dp,
                )
            }

            if (showLyrics) {
                // Lyrics take over the empty artwork space above the title, and
                // bring their own frosted backdrop, so the cover stays readable
                // type-on-glass instead of type-on-artwork.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 25.dp, vertical = 10.dp),
                ) {
                    InlineLyricsView(
                        mediaMetadata = mediaMetadata,
                        showLyrics = showLyrics,
                        positionProvider = position,
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Mini lyrics, in the empty artwork space the design already leaves
            // above the title — at the bottom of the canvas either way, so the
            // two styles take the same place and differ only in how they draw.
            // Tapping either opens the full lyrics panel rather than a second,
            // different lyrics screen.
            if (showLyricsOnPlayer && !showLyrics) {
                Spacer(Modifier.height(18.dp))
                if (miniLyricsStyle == MiniLyricsStyle.CANVAS_GLOW) {
                    // Drawn on the full-bleed canvas itself: the backdrop is
                    // painted edge to edge, so this block costs the design
                    // nothing — it only fills space the spacer above gave up.
                    PlayerCanvasGlowLyrics(
                        mediaMetadata = mediaMetadata,
                        positionProvider = position,
                        accent = Color.White,
                        textSize = 28.sp,
                        onExpand = { showLyrics = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    PlayerSyncedLyricsView(
                        mediaMetadata = mediaMetadata,
                        positionProvider = position,
                        accent = Color.White,
                        onExpand = { showLyrics = true },
                        modifier = Modifier.fillMaxWidth(),
                        horizontalPadding = 25.dp,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata?.title.orEmpty(),
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = mediaMetadata?.artists?.joinToArtistString(", ") { it.name }.orEmpty(),
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                }
                Spacer(Modifier.width(4.dp))
                StyleLikeButton(
                    tint = Color.White,
                    iconSize = 20.dp,
                    contentAlpha = 0.94f,
                )
                StyleIconButton(
                    icon = R.drawable.share,
                    iconSize = 19.dp,
                    tint = Color.White,
                    contentAlpha = 0.94f,
                    contentDescription = "Share",
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://music.youtube.com/watch?v=${mediaMetadata?.id}",
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                )
            }

            Spacer(Modifier.height(18.dp))

            StyleSeekBlock(
                position = position,
                duration = duration,
                trackHeight = 16.dp,
                cornerRadius = 8.dp,
                trackColor = Color(0xFF52595F),
                playedColor = Color.White,
                labelColor = Color.White.copy(alpha = 0.76f),
                canvasHeight = 40.dp,
                labelSize = 11.5.sp,
                labelHorizontalPadding = 25.dp,
                tickThumb = true,
                endDot = true,
                modifier = Modifier.padding(horizontal = 25.dp),
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 41.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleIconButton(
                    icon = if (shuffleEnabled) R.drawable.shuffle_on else R.drawable.shuffle,
                    onClick = { playerConnection.player.shuffleModeEnabled = !shuffleEnabled },
                    iconSize = 20.dp,
                    tint = Color.White,
                    enabled = !flags.isGuest,
                    contentDescription = "Shuffle",
                )
                StyleIconButton(
                    icon = R.drawable.skip_previous,
                    onClick = playerConnection::seekToPrevious,
                    iconSize = 20.dp,
                    tint = Color.White,
                    enabled = flags.canSkipPrevious && !flags.isGuest,
                    contentDescription = "Previous",
                )
                StylePlayDisc(
                    flags = flags,
                    connection = playerConnection,
                    size = 61.dp,
                    iconSize = 26.dp,
                    surface = Color.White,
                    ink = Color.Black,
                )
                StyleIconButton(
                    icon = R.drawable.skip_next,
                    onClick = playerConnection::seekToNext,
                    iconSize = 20.dp,
                    tint = Color.White,
                    enabled = flags.canSkipNext && !flags.isGuest,
                    contentDescription = "Next",
                )
                StyleIconButton(
                    icon = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        R.drawable.repeat_one
                    } else {
                        R.drawable.repeat
                    },
                    onClick = { playerConnection.player.toggleRepeatMode() },
                    iconSize = 20.dp,
                    tint = Color.White,
                    contentAlpha = if (repeatMode != Player.REPEAT_MODE_OFF) 1f else 0.7f,
                    contentDescription = "Repeat",
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}
