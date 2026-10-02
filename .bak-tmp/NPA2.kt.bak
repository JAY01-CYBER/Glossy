/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The mini player's "this is the song that is playing" marker, in the two shapes
 * Glossy offers: [PlayingBars], the four-bar equalizer, and [PlayingNotes], three
 * small notes that drift up out of the song row. The user picks between them in
 * Appearance, and [NowPlayingAnimationIndicator] is the one place that maps that
 * choice to a shape — so the mini player and the settings preview can never
 * disagree about what was picked.
 *
 * Both shapes are built for slow phones, like the rest of the app's ambience:
 *
 *  - One infinite transition drives every element, and the per-element transform
 *    is read *inside* its own `graphicsLayer` block. A frame therefore costs a
 *    handful of layer transforms and triggers no recomposition and no relayout,
 *    on the one surface that is on screen for a whole song.
 *  - Nothing is scheduled while playback is paused, or with ambient motion off:
 *    the same still frame is drawn and the frame clock is left alone.
 */

package com.jay.glossy.ui.component

import com.jay.glossy.R

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jay.glossy.constants.MiniPlayerPlayingAnimation
import kotlin.math.PI
import kotlin.math.sin

/** One full trip of a note: in under the row, out over the top of it. */
private const val NoteCycleMillis = 2600

/** Where each note sits in the cycle, so the three never move in lockstep. */
private const val NotePhaseStep = 0.34f

/** The heights a frozen (paused) row settles on, and the gap between them. */
private const val RestingNotePhase = 0.3f
private const val RestingNoteStep = 0.18f

/** How solid a paused row is. */
private const val PausedNoteAlpha = 0.45f

/** A note's peak opacity: full-white tiny glyphs read as specks, not as notes. */
private const val NotePeakAlpha = 0.92f

/** The cycle fraction a note spends fading in, and the point it starts leaving. */
private const val NoteFadeInEnd = 0.18f
private const val NoteFadeOutStart = 0.68f

/** One full wave, for the sideways sway. */
private const val TWO_PI = 2f * PI.toFloat()

/**
 * The indicator's own footprint. A fixed size is the point: switching between
 * the bars and the notes must not resize — and therefore must not nudge — the
 * song row it sits in.
 */
private val NotesIndicatorWidth = 15.dp
private val NotesIndicatorHeight = 16.dp

/** How far a note travels, measured out from the centre of the indicator. */
private val NoteTravel = 8.dp

/** Gap between the three note columns, and how far a note sways sideways. */
private val NoteColumnGap = 5.dp
private val NoteSway = 1.dp

/** The chip Appearance uses to preview a shape. */
private val PreviewWidth = 40.dp
private val PreviewHeight = 26.dp

/**
 * Three small music notes that float up out of the indicator while [isPlaying],
 * fading in below the row and out above it.
 *
 * Like [PlayingBars], a pause does not empty the indicator: the notes settle at
 * their resting heights at [PausedNoteAlpha], so the row keeps its shape.
 */
@Composable
fun PlayingNotes(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    noteCount: Int = 3,
    noteSize: Dp = 8.dp,
) {
    val animating = isPlaying && rememberAmbientMotionEnabled()

    // Only the phase lives in state: every note reads it inside its own layer
    // block, which is what keeps an animating row from recomposing per frame.
    val phase: State<Float> =
        if (animating) {
            val transition = rememberInfiniteTransition(label = "playingNotes")
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(NoteCycleMillis, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "playingNotesPhase",
            )
        } else {
            remember { mutableStateOf(0f) }
        }

    Box(
        modifier =
            modifier
                .size(width = NotesIndicatorWidth, height = NotesIndicatorHeight)
                // The notes are born under the row and die over it; clipping
                // keeps that inside the indicator's own little window instead of
                // over the artwork and the buttons next to it.
                .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        repeat(noteCount) { index ->
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = color,
                modifier =
                    Modifier
                        .size(noteSize)
                        .graphicsLayer {
                            // The only code that re-runs per frame.
                            val t =
                                if (animating) {
                                    (phase.value + index * NotePhaseStep) % 1f
                                } else {
                                    (RestingNotePhase + index * RestingNoteStep).coerceIn(0f, 1f)
                                }
                            val column = (index - (noteCount - 1) / 2f) * NoteColumnGap.toPx()

                            translationY = NoteTravel.toPx() * (1f - 2f * t)
                            translationX = column + sin(t * TWO_PI + index) * NoteSway.toPx()
                            alpha = if (isPlaying) noteFade(t) * NotePeakAlpha else PausedNoteAlpha

                            val scale = 0.72f + t * 0.32f
                            scaleX = scale
                            scaleY = scale
                            rotationZ = -10f + t * 20f
                        },
            )
        }
    }
}

/**
 * Draws the playing indicator in the shape picked in Appearance.
 *
 * The bars keep [PlayingBars]' footprint and the notes keep theirs; the two are
 * within a couple of dp of each other, so switching between them never moves the
 * row's baseline.
 */
@Composable
fun NowPlayingAnimationIndicator(
    animation: MiniPlayerPlayingAnimation,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    when (animation) {
        MiniPlayerPlayingAnimation.BARS ->
            PlayingBars(
                isPlaying = isPlaying,
                modifier = modifier,
                color = color,
            )

        MiniPlayerPlayingAnimation.NOTES ->
            PlayingNotes(
                isPlaying = isPlaying,
                modifier = modifier,
                color = color,
            )
    }
}

/**
 * The Appearance preview: one shape, in a small tonal chip, at the size it
 * occupies in the song row.
 *
 * It asks for [isPlaying] = true, but it still goes through the ambient motion
 * gate, so someone who has turned animation off is shown the still frame the app
 * will actually give them rather than a promise it will not keep.
 */
@Composable
fun NowPlayingAnimationPreview(
    animation: MiniPlayerPlayingAnimation,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier =
            modifier
                .size(width = PreviewWidth, height = PreviewHeight)
                .clip(RoundedCornerShape(PreviewHeight / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        if (animation == MiniPlayerPlayingAnimation.NOTES) {
            // A dot of artwork with the notes rising off it, like the bar.
            Box(
                modifier = Modifier.size(width = 40.dp, height = PreviewHeight),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.75f)),
                )
                ArtworkNotesOverlay(
                    isPlaying = true,
                    color = color,
                    modifier = Modifier.size(width = 34.dp, height = PreviewHeight),
                )
            }
        } else {
            NowPlayingAnimationIndicator(
                animation = animation,
                isPlaying = true,
                color = color,
            )
        }
    }
}

/**
 * A note's opacity over its trip: in at the bottom, whole in the middle, gone by
 * the top. Without this the notes would pop into and out of the clipped window.
 */
private fun noteFade(t: Float): Float =
    when {
        t < NoteFadeInEnd -> t / NoteFadeInEnd
        t > NoteFadeOutStart -> ((1f - t) / (1f - NoteFadeOutStart)).coerceIn(0f, 1f)
        else -> 1f
    }

/** Sizes for the notes rising off the artwork: the middle note leads biggest. */
private val ArtworkNoteSizes = listOf(10.dp, 13.dp, 9.dp)

/** How far the outer notes sit from the centre column as they rise. */
private val ArtworkNoteSpread = 10.dp

/** Where a note is born above the overlay bottom, and how far it climbs. */
private val ArtworkRiseStart = 2.dp
private val ArtworkRiseDistance = 34.dp

/**
 * Music notes that rise off the mini player's artwork while a song plays.
 *
 * This is the NOTES shape of the Appearance playing-animation setting. It is a
 * transparent overlay stacked on top of the artwork thumbnail: notes spawn near
 * the bottom of the artwork, float upward across it, and fade out near its top.
 *
 * It is intentionally unclipped, so callers add it as a sibling AFTER the
 * clipped artwork image (inside an unclipped container). That way the notes can
 * drift a little past the artwork edge instead of being cut at its bounds.
 *
 * Like everything here, one infinite transition drives every note, each note
 * reads the phase inside its own graphicsLayer block (no recomposition per
 * frame), and a pause draws one still frame without scheduling anything.
 */
@Composable
fun ArtworkNotesOverlay(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    noteCount: Int = 3,
) {
    val animating = isPlaying && rememberAmbientMotionEnabled()

    val phase: State<Float> =
        if (animating) {
            val transition = rememberInfiniteTransition(label = "artworkNotes")
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(NoteCycleMillis, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "artworkNotesPhase",
            )
        } else {
            remember { mutableStateOf(RestingNotePhase) }
        }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        repeat(noteCount) { index ->
            val noteSize = ArtworkNoteSizes[index % ArtworkNoteSizes.size]
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = color,
                modifier =
                    Modifier
                        .size(noteSize)
                        .graphicsLayer {
                            // The only code that re-runs per frame.
                            val t =
                                if (animating) {
                                    (phase.value + index * NotePhaseStep) % 1f
                                } else {
                                    (RestingNotePhase + index * RestingNoteStep).coerceIn(0f, 1f)
                                }
                            translationY = -(ArtworkRiseStart.toPx() + t * ArtworkRiseDistance.toPx())
                            val spread = (index - (noteCount - 1) / 2f) * ArtworkNoteSpread.toPx() * (0.35f + t)
                            translationX = spread + sin(t * TWO_PI + index) * NoteSway.toPx()
                            alpha = if (isPlaying) noteFade(t) * NotePeakAlpha else PausedNoteAlpha

                            val scale = 0.6f + t * 0.55f
                            scaleX = scale
                            scaleY = scale
                            rotationZ = -12f + t * 24f
                        },
            )
        }
    }
}
