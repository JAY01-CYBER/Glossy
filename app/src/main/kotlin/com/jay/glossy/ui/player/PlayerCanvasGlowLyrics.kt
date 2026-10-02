/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The "Canvas glow" mini lyrics: the line being sung, drawn on the artwork
 * itself — over the animated canvas — at the bottom of it, with a halo in the
 * design's accent colour.
 *
 * This is the alternative to the frosted strip (`PlayerSyncedLyricsView`), and
 * it is chosen in Appearance. The difference is not only cosmetic: the strip is
 * a sibling of the artwork, so every line it draws takes height from the design
 * and the artwork resizes around it. This one is an overlay, sitting in space
 * the design already leaves empty, so the artwork never moves. It sits low —
 * just above the song's title — so it reads as the bottom of the canvas rather
 * than floating in the middle of the artwork.
 *
 * The line itself comes from `rememberPlayerLyricsPreview`, the same resolution
 * the strip uses — one fetch, one parse, one ticking clock, one answer about
 * which line is current — so the two styles can never disagree about the song.
 */

package com.jay.glossy.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.models.MediaMetadata

/**
 * How many lines after the active one are shown, dimmed and small. Zero is a
 * legitimate choice: one glowing line, no context.
 */
private const val CANVAS_GLOW_LOOKAHEAD_LINES = 1

/**
 * Scale of the translucent copy that sits directly behind the solid line. It is
 * the innermost halo — the two blurred passes do the soft work, this one makes
 * sure the glow reads even on a device whose text shadow passes are weak.
 */
private const val GLOW_ENVELOPE_SCALE = 1.035f

/**
 * How the dark pocket behind the words is sized: a multiple of the block's
 * longest side, so a single long line and a wrapping pair of lines both get a
 * pocket that follows them instead of one flat rectangle.
 */
private const val GLOW_POCKET_SPREAD = 0.62f

/**
 * Breathing room around the words inside their pocket. Generous by default: the
 * words usually float over busy artwork, and the pocket needs somewhere to fade
 * out to. A design that sets the line flush against its song title overrides it
 * with [contentPadding] so the block does not push the words away from the name.
 */
private val GlowContentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)

/**
 * The active lyric, glowing, over whatever artwork the caller has already
 * drawn.
 *
 * Place it inside the same Box as the canvas so it lands on the animation
 * rather than next to it — and pass [alignment] = `BottomCenter` (with the
 * caller's own bottom padding) to park it at the bottom of the canvas, above
 * the song info. Tapping the words opens the full lyrics view, exactly like
 * tapping the strip does: the two styles differ in looks, not in what they do.
 *
 * A song with nothing to show — no lyrics found, still loading, or a timed
 * entry that has not reached its first line — draws nothing at all. Not an
 * empty block, and in particular not the dark pocket that sits under the words:
 * the canvas is the whole of what the listener sees until there is a line to
 * put on it.
 */
@Composable
fun PlayerCanvasGlowLyrics(
    mediaMetadata: MediaMetadata?,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier,
    accent: Color = Color.White,
    /** Size of the active line; the lookahead lines are a fraction of it. */
    textSize: TextUnit = 28.sp,
    lookaheadLines: Int = CANVAS_GLOW_LOOKAHEAD_LINES,
    alignment: Alignment = Alignment.Center,
    /**
     * Room between the words and the edge of their pocket. The default suits a
     * line floating in the middle of an artwork; a design that sets the line
     * right above its song title passes a slimmer one, so the words sit close
     * to the name instead of hovering a pocket's width away from it.
     */
    contentPadding: PaddingValues = GlowContentPadding,
    /**
     * True when the design this line belongs to left-aligns its song title. The
     * words then share that edge, so the pair reads as one block, and the pocket
     * follows the text's own width rather than the full column.
     */
    alignToStart: Boolean = false,
    onExpand: (() -> Unit)? = null,
) {
    val preview =
        rememberPlayerLyricsPreview(
            mediaMetadata = mediaMetadata,
            positionProvider = positionProvider,
        )
    val interaction = remember { MutableInteractionSource() }

    // Is there a line to paint? Deliberately not the same question as "did the
    // song have lyrics". A timed entry can be sitting in an intro with nothing
    // lit yet and nothing queued behind it, and an entry can carry blank text.
    //
    // It gates the whole block, pocket included, because the legibility pocket
    // belongs to the block. With no words the block was nothing but its own
    // padding — a few dp of nothing — and the pocket still painted around it,
    // which on an artwork whose lyrics had not been found came out as a small
    // dark circle sitting on the cover: a black dot with no words under it. A
    // block with nothing to say is not composed at all, so there is no pocket,
    // no words and no tap target where the lyrics would have gone.
    val hasLineToDraw =
        when {
            !preview.hasLyrics -> false
            // Plain-text lyrics can only ever show their opening line.
            !preview.synced -> preview.plainLine.isNotBlank()
            else ->
                preview.lines.getOrNull(preview.activeIndex)?.text?.isNotBlank() == true ||
                    (1..lookaheadLines).any {
                        preview.lines.getOrNull(preview.activeIndex + it)?.text?.isNotBlank() == true
                    }
        }

    Box(
        modifier = modifier,
        contentAlignment = alignment,
    ) {
        if (hasLineToDraw) {
            Column(
                horizontalAlignment =
                    if (alignToStart) Alignment.Start else Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier =
                    Modifier
                        // A lyric is read on top of moving artwork, so it brings
                        // its own legibility: a soft dark pocket under the words,
                        // drawn as a fading circle around them rather than a
                        // rectangle, so it hugs the text wherever the text sits.
                        // Because it is drawn here, on the words, it can only
                        // ever exist around words.
                        .drawBehind {
                            val radius = maxOf(size.width, size.height) * GLOW_POCKET_SPREAD
                            drawCircle(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                Color.Black.copy(alpha = 0.55f),
                                                Color.Black.copy(alpha = 0.22f),
                                                Color.Transparent,
                                            ),
                                        center = center,
                                        radius = radius,
                                    ),
                                radius = radius,
                                center = center,
                            )
                        }
                        .then(
                            if (onExpand != null) {
                                Modifier.clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    onClick = onExpand,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .padding(contentPadding),
            ) {
                when {
                    // Untimed lyrics cannot be followed, so the opening line is
                    // shown quiet and hint-free — the full lyrics view is a tap
                    // away.
                    !preview.synced ->
                        GlowingLyricLine(
                            text = preview.plainLine,
                            accent = accent,
                            textSize = textSize * 0.78f,
                            quiet = true,
                            textAlign = if (alignToStart) TextAlign.Start else TextAlign.Center,
                        )

                    else ->
                        AnimatedContent(
                            targetState = preview.activeIndex,
                            contentAlignment =
                                if (alignToStart) Alignment.TopStart else Alignment.Center,
                            transitionSpec = {
                                (fadeIn(tween(420, easing = FastOutSlowInEasing)) +
                                    scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.94f))
                                    .togetherWith(fadeOut(tween(200)))
                            },
                            label = "canvasGlowLyrics",
                        ) { index ->
                            // -1 means nothing is being sung yet — an intro, or
                            // the gap before the first timed line. Nothing glows
                            // then: the opening line waits in the dimmed "up
                            // next" slot below and lights up when it actually
                            // starts, instead of burning through a twelve-second
                            // intro as if it were the current line. (Clamping to
                            // 0 here was exactly that lie.)
                            val active = index
                            Column(
                                horizontalAlignment =
                                    if (alignToStart) Alignment.Start else Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                GlowingLyricLine(
                                    text = preview.lines.getOrNull(active)?.text.orEmpty(),
                                    accent = accent,
                                    textSize = textSize,
                                    textAlign = if (alignToStart) TextAlign.Start else TextAlign.Center,
                                )
                                for (offset in 1..lookaheadLines) {
                                    val next = preview.lines.getOrNull(active + offset) ?: continue
                                    Text(
                                        text = next.text,
                                        color = Color.White.copy(alpha = 0.42f),
                                        fontSize = textSize * 0.58f,
                                        lineHeight = textSize * 0.72f,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = if (alignToStart) TextAlign.Start else TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}

/**
 * One line, lit: a wide soft halo, a tighter brighter one, a translucent copy
 * hugging the glyphs, and the solid white line on top.
 *
 * The halos are drawn with a *transparent* fill and only a [Shadow], so what
 * glows is the letterform itself — a coloured copy of the text would thicken
 * the glyphs and a background would glow as a rectangle.
 */
@Composable
private fun GlowingLyricLine(
    text: String,
    accent: Color,
    textSize: TextUnit,
    modifier: Modifier = Modifier,
    /** Dimmer, smaller glow for the untimed "opening line" case. */
    quiet: Boolean = false,
    textAlign: TextAlign = TextAlign.Center,
) {
    if (text.isBlank()) return

    val glowAlpha = if (quiet) 0.30f else 0.62f

    Box(
        modifier = modifier,
        contentAlignment = if (textAlign == TextAlign.Start) Alignment.CenterStart else Alignment.Center,
    ) {
        GlowPass(text = text, color = accent.copy(alpha = glowAlpha * 0.8f), blurRadius = 34f, textSize = textSize, textAlign = textAlign)
        GlowPass(text = text, color = accent.copy(alpha = glowAlpha), blurRadius = 14f, textSize = textSize, textAlign = textAlign)
        Text(
            text = text,
            style =
                DefaultGlowTextStyle(
                    textSize = textSize,
                    color = accent.copy(alpha = 0.22f),
                    textAlign = textAlign,
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.graphicsLayer {
                    scaleX = GLOW_ENVELOPE_SCALE
                    scaleY = GLOW_ENVELOPE_SCALE
                },
        )
        Text(
            text = text,
            style = DefaultGlowTextStyle(textSize = textSize, color = Color.White, textAlign = textAlign),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One blurred pass of the glyphs, drawn as shadow only. */
@Composable
private fun GlowPass(
    text: String,
    color: Color,
    blurRadius: Float,
    textSize: TextUnit,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        style =
            DefaultGlowTextStyle(
                textSize = textSize,
                color = Color.Transparent,
                shadow = Shadow(color = color, blurRadius = blurRadius),
                textAlign = textAlign,
            ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The metrics every pass shares. They have to match exactly, or the halo would
 * sit a pixel off the word it belongs to.
 */
private fun DefaultGlowTextStyle(
    textSize: TextUnit,
    color: Color,
    shadow: Shadow? = null,
    textAlign: TextAlign = TextAlign.Center,
): TextStyle =
    TextStyle(
        color = color,
        fontSize = textSize,
        lineHeight = textSize * 1.18f,
        fontWeight = FontWeight.Bold,
        textAlign = textAlign,
        shadow = shadow,
    )
