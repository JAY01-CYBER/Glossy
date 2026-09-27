/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * A full-screen now-playing design:
 *
 *  - [VinylNowPlaying]      Warm turntable: a spinning vinyl record with the
 *    artwork printed on the disc, swinging tonearm and a transport +
 *    Lyrics / Cast / Queue dock.
 *
 * The design paints no animated canvas: the record shows plain artwork. The
 * canvas engines stay exclusive to the standard player designs and the app
 * backdrop, all behind the single "Canvas Background" switch, so this screen
 * looks identical whether it is on or off.
 */

package com.jay.glossy.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    AnimatedVisibility(
        visible = isPlaying,
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
            )

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // fill = true takes the whole leftover height and centres the
                    // square inside it, so spare space is shared above and below
                    // the record instead of pooling under the dock.
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
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

            if (showLyricsOnPlayer) {
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

            VinylTransportRow(connection = playerConnection, flags = flags, colors = colors)

            Spacer(Modifier.height(14.dp))

            GlossyActionPills(connection = playerConnection, colors = colors)

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
 * The vinyl disc: the track's artwork pressed onto a grooved black record,
 * spinning while playing. It deliberately never draws an animated canvas — the
 * record shows plain artwork, and the canvas engines stay exclusive to the
 * other player designs.
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
        modifier = Modifier
            // 0.90 keeps the disc radius at 0.45 × the parent square, which is
            // the figure VinylTonearm derives its pivot geometry from.
            .fillMaxSize(0.90f)
            .rotate(displayAngle)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1D1D1D), Color(0xFF090909)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Grooves across the playable surface.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2f
            val grooveStroke = 1.dp.toPx()
            var position = 0.70f
            while (position <= 0.98f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.07f),
                    radius = outerRadius * position,
                    style = Stroke(width = grooveStroke),
                )
                position += 0.03f
            }
            // Edge catch-light so the disc separates from the dark backdrop.
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = outerRadius - grooveStroke,
                style = Stroke(width = grooveStroke * 1.5f),
            )
        }

        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(400)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize(0.68f)
                    .clip(CircleShape),
            )
        }

        // Centre label with a spindle hole, sized like a real record's.
        Box(
            modifier = Modifier
                .fillMaxSize(0.22f)
                .clip(CircleShape)
                .background(Color(0xFF140F09))
                .border(1.dp, VinylAccent.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE9E4D8)),
            )
        }

        // Faint sheen for depth.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                ),
        )
    }
}

/**
 * The tonearm, drawn rather than assembled from rectangles so the pivot, tube
 * and headshell always line up with the disc: the stylus rests over the outer
 * grooves while playing and swings clear when paused. The canvas spans the
 * whole square, so the geometry follows the disc's radius at any screen size.
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
        val discRadius = size.width * 0.45f
        val pivot = Offset(size.width * 0.90f, size.height * 0.07f)

        val towardsPivot = pivot - discCentre
        val stylusRest = discCentre + towardsPivot / towardsPivot.getDistance() * (discRadius * 0.78f)
        val arm = stylusRest - pivot
        val armLength = arm.getDistance()
        val armAngle = atan2(arm.y, arm.x) * 180f / PI.toFloat()

        rotate(degrees = armAngle + lift, pivot = pivot) {
            val tube = 5.dp.toPx()

            // Counterweight behind the pivot.
            drawRoundRect(
                color = Color(0xFF8E8779),
                topLeft = Offset(pivot.x - 24.dp.toPx(), pivot.y - tube * 1.6f),
                size = Size(24.dp.toPx(), tube * 3.2f),
                cornerRadius = CornerRadius(tube * 1.6f),
            )

            // Tube from the pivot out to the record.
            drawRoundRect(
                color = Color(0xFFDDD7C7),
                topLeft = Offset(pivot.x, pivot.y - tube / 2f),
                size = Size(armLength, tube),
                cornerRadius = CornerRadius(tube / 2f),
            )

            // Headshell over the grooves.
            drawRoundRect(
                color = VinylAccent,
                topLeft = Offset(pivot.x + armLength - 20.dp.toPx(), pivot.y - 7.dp.toPx()),
                size = Size(20.dp.toPx(), 14.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )

            // Pivot housing.
            drawCircle(color = Color(0xFF6F6858), radius = 12.dp.toPx(), center = pivot)
            drawCircle(color = VinylAccent.copy(alpha = 0.85f), radius = 5.dp.toPx(), center = pivot)
        }
    }
}

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun TrackMenuButton(bottomSheetState: BottomSheetState) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    GlossyIconButton(
        icon = R.drawable.more_vert,
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
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIconButton(icon = R.drawable.arrow_back, onClick = onCollapse, tint = VinylAccent)
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
        TrackMenuButton(bottomSheetState = bottomSheetState)
    }
}

@Composable
private fun GlossyIconButton(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
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
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun VinylTransportRow(
    connection: PlayerConnection,
    flags: PlayerFlags,
    colors: ArtworkColors,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton(
            icon = R.drawable.skip_previous,
            enabled = flags.canSkipPrevious && !flags.isGuest,
            container = colors.side,
            content = colors.onSide,
            onClick = connection::seekToPrevious,
        )
        TransportButton(
            icon = transportIcon(flags),
            enabled = true,
            container = colors.accent,
            content = colors.onAccent,
            large = true,
            onClick = playPauseAction(connection, flags),
        )
        TransportButton(
            icon = R.drawable.skip_next,
            enabled = flags.canSkipNext && !flags.isGuest,
            container = colors.side,
            content = colors.onSide,
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

@Composable
private fun TransportButton(
    icon: Int,
    enabled: Boolean,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    large: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "transportScale",
    )
    Box(
        modifier = Modifier
            .size(if (large) 74.dp else 58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(if (large) 24.dp else 20.dp))
            .background(container.copy(alpha = if (enabled) 1f else 0.4f))
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
            tint = content,
            modifier = Modifier.size(if (large) 34.dp else 28.dp),
        )
    }
}

@Composable
private fun GlossyActionPills(
    connection: PlayerConnection,
    colors: ArtworkColors,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val mediaMetadata by connection.mediaMetadata.collectAsStateWithLifecycle()
    val currentSong by connection.currentSong.collectAsStateWithLifecycle(initialValue = null)
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
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon-only heart, like the reference design.
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

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (showLabel) Modifier.height(44.dp) else Modifier.size(44.dp))
            .clip(if (showLabel) RoundedCornerShape(50) else CircleShape)
            .background(container)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = if (showLabel) 18.dp else 0.dp),
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
        horizontalArrangement = Arrangement.SpaceBetween,
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
            modifier = Modifier.size(36.dp),
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
