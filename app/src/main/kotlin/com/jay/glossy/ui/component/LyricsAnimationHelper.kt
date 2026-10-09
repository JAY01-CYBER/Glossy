/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.geometry.Offset
import com.jay.glossy.constants.MiniLyricsAnimationStyle
import com.jay.glossy.lyrics.LyricsEntry

/**
 * Applies one step of a word-by-word animation to a single lyric line.
 *
 * Used by the two mini lyrics surfaces on the player — the frosted strip and the
 * canvas glow. The full lyrics view animates whole lines of its own accord, so
 * it does not share this code.
 *
 * Only paint is animated: colour, alpha and glow. A word's weight, and with it
 * the width of its glyphs, is left to the caller: both surfaces draw exactly
 * one line, and a word that grew bolder as it was sung would re-lay that line
 * out sideways on every word boundary. [MiniLyricsAnimationStyle.SLIDE] is the
 * wipe: it is painted per glyph rather than with a gradient, because a
 * `SpanStyle` brush covers the whole line and would light every word at once.
 *
 * The spacing between words is the provider's own — a word is followed by a
 * space only when it was timed with one — so a word-timed line reads exactly
 * like the same line rendered untimed, including in languages that are not
 * written with spaces.
 */
fun applyWordAnimation(
    item: LyricsEntry,
    animationStyle: MiniLyricsAnimationStyle,
    isActiveLine: Boolean,
    effectivePlaybackPosition: Long,
    accent: Color,
): androidx.compose.ui.text.AnnotatedString {
    val hasWordTimings = item.words?.isNotEmpty() == true

    if (!hasWordTimings) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
                append(item.text)
            }
        }
    }

    return when (animationStyle) {
        MiniLyricsAnimationStyle.NONE -> applyNoneAnimation(item, isActiveLine, effectivePlaybackPosition, accent)
        MiniLyricsAnimationStyle.FADE -> applyFadeAnimation(item, isActiveLine, effectivePlaybackPosition, accent)
        MiniLyricsAnimationStyle.GLOW -> applyGlowAnimation(item, isActiveLine, effectivePlaybackPosition, accent)
        MiniLyricsAnimationStyle.SLIDE -> applySlideAnimation(item, isActiveLine, effectivePlaybackPosition, accent)
        MiniLyricsAnimationStyle.KARAOKE -> applyKaraokeAnimation(item, isActiveLine, effectivePlaybackPosition, accent)
        MiniLyricsAnimationStyle.APPLE -> applyAppleAnimation(item, isActiveLine, effectivePlaybackPosition, accent)
    }
}

/**
 * Shared timing kit for the mini lyrics word animation.
 *
 * Each style file used to inline its own copy of the progress math, and the
 * copies drifted: the easing, the word boundary (inclusive vs. exclusive end)
 * and the alpha of a word that has just been sung all disagreed, so flipping
 * between two styles visibly changed *where* the animation was. Every style
 * below is painted from one [WordTiming], and its only freedom is how it maps
 * that timing to paint.
 *
 * Two behaviours are shared on purpose:
 *
 *  - **A word visibly leads in.** The highlight starts [LeadInMillis] before
 *    the word is actually sung, so short words at speed do not read as a
 *    strobe: the eye is already on the word when its first frame lands.
 *
 *  - **A word settles instead of snapping off.** When a word ends, its glow
 *    fills back down to "sung" over `SETTLE_MILLIS` rather than dropping to
 *    that level in one tick, so a word boundary never reads as a flicker.
 *
 * The data below is relative to the line, not to any style, and none of it is
 * recomputed per style below.
 */
private const val LeadInMillis = 160L

/** How long a sung word takes to settle from "lit" to "sung". */
private const val SettleMillis = 320L

/** Not-sung-yet alpha: a word the line has not reached. */
private const val WordDimAlpha = 0.34f

/** Sung alpha: a word the line has left behind. */
private const val WordSungAlpha = 0.68f

/** The active word never drops below its neighbours. */
private const val WordActiveFloorAlpha = 0.68f

/** Shared ease for every word-by-word ramp. */
private fun easeWord(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

/** Where one word is in the line's sweep, relative to the clock. */
private data class WordTiming(
    val isActive: Boolean,
    val hasPassed: Boolean,
    val isUpcoming: Boolean,
    /** 0 on the first frame of the word, 1 on its last, eased. */
    val progress: Float,
    /** 1 on the frame the word was sung, relaxing back toward 0 after. */
    val settle: Float,
)

/**
 * One word's timing, with [LeadInMillis] of visible lead and a
 * [SettleMillis] settle after it ends.
 */
private fun wordTiming(
    wordStartMs: Long,
    wordEndMs: Long,
    isActiveLine: Boolean,
    effectivePlaybackPosition: Long,
): WordTiming {
    if (!isActiveLine) {
        // Inactive lines never light up; callers keep them dim and still.
        return WordTiming(isActive = false, hasPassed = false, isUpcoming = true, progress = 0f, settle = 0f)
    }
    val startsAt = wordStartMs - LeadInMillis
    val isLeading = effectivePlaybackPosition in startsAt until wordStartMs
    val isActive = effectivePlaybackPosition in wordStartMs..wordEndMs
    val hasPassed = effectivePlaybackPosition > wordEndMs
    val progress =
        when {
            isLeading -> ((effectivePlaybackPosition - startsAt).toFloat() / (LeadInMillis)).coerceIn(0f, 1f) * 0.5f

            isActive && wordEndMs > wordStartMs ->
                0.5f + 0.5f * ((effectivePlaybackPosition - wordStartMs).toFloat() / (wordEndMs - wordStartMs)).coerceIn(0f, 1f)

            hasPassed -> 1f
            else -> 0f
        }
    // The settle is what stops a boundary from snapping: for a short beat
    // after a word ends it still carries some of its light.
    val settle =
        if (hasPassed) {
            1f - ((effectivePlaybackPosition - wordEndMs).toFloat() / SettleMillis).coerceIn(0f, 1f)
        } else {
            0f
        }
    return WordTiming(
        isActive = isActive || isLeading,
        hasPassed = hasPassed,
        isUpcoming = !isActive && !isLeading && !hasPassed,
        progress = progress,
        settle = settle,
    )
}

private fun applyNoneAnimation(
    item: LyricsEntry,
    isActiveLine: Boolean,
    effectivePlaybackPosition: Long,
    accent: Color,
): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    item.words?.forEachIndexed { wordIndex, word ->
        val wordStartMs = (word.startTime * 1000).toLong()
        val wordEndMs = (word.endTime * 1000).toLong()
        val timing = wordTiming(wordStartMs, wordEndMs, isActiveLine, effectivePlaybackPosition)

        // "None" means none: the line sits at one steady level and never
        // repaints word by word. The timing is still read so the words share
        // the same map as every other style — it just maps to one value.
        val wordAlpha = if (isActiveLine) WordSungAlpha + 0.12f else 0.6f
        val wordColor = accent.copy(alpha = wordAlpha)

        withStyle(style = SpanStyle(color = wordColor)) {
            append(word.text)
        }
        if (word.hasTrailingSpace && wordIndex < item.words.size - 1) append(" ")
    }
}

private fun applyFadeAnimation(
    item: LyricsEntry,
    isActiveLine: Boolean,
    effectivePlaybackPosition: Long,
    accent: Color,
): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    item.words?.forEachIndexed { wordIndex, word ->
        val wordStartMs = (word.startTime * 1000).toLong()
        val wordEndMs = (word.endTime * 1000).toLong()
        val timing = wordTiming(wordStartMs, wordEndMs, isActiveLine, effectivePlaybackPosition)

        // The sweep reads dim -> lit -> sung, so a boundary never drops the
        // line back to its floor: when the next word is still at zero light,
        // the word just sung is still carrying nearly all of it.
        val fadeProgress = easeWord(timing.progress)
        val wordAlpha =
            when {
                !isActiveLine -> 0.55f
                timing.hasPassed -> WordSungAlpha + 0.32f * timing.settle
                timing.isActive -> (WordActiveFloorAlpha + (1f - WordActiveFloorAlpha) * fadeProgress).coerceAtLeast(WordDimAlpha + 0.3f * fadeProgress)
                else -> WordDimAlpha
            }
        val wordColor = accent.copy(alpha = wordAlpha)
        val wordShadow =
            when {
                timing.isActive && fadeProgress > 0.2f -> {
                    Shadow(
                        color = accent.copy(alpha = 0.35f * fadeProgress),
                        offset = Offset.Zero,
                        blurRadius = 10f * fadeProgress,
                    )
                }

                timing.hasPassed && timing.settle > 0.05f -> {
                    Shadow(
                        color = accent.copy(alpha = 0.15f * timing.settle),
                        offset = Offset.Zero,
                        blurRadius = 6f * timing.settle,
                    )
                }

                else -> {
                    null
                }
            }

        withStyle(style = SpanStyle(color = wordColor, shadow = wordShadow)) {
            append(word.text)
        }
        if (word.hasTrailingSpace && wordIndex < item.words.size - 1) append(" ")
    }
}

private fun applyGlowAnimation(
    item: LyricsEntry,
    isActiveLine: Boolean,
    effectivePlaybackPosition: Long,
    accent: Color,
): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    item.words?.forEachIndexed { wordIndex, word ->
        val wordStartMs = (word.startTime * 1000).toLong()
        val wordEndMs = (word.endTime * 1000).toLong()
        val timing = wordTiming(wordStartMs, wordEndMs, isActiveLine, effectivePlaybackPosition)

        // The glow is a peak, not a plateau: it blooms while the word is sung
        // and breathes back out after, so the line always reads as one moving
        // highlight rather than a trail that never fades.
        val fillProgress = easeWord(timing.progress)
        val wordAlpha =
            when {
                !isActiveLine -> 0.5f
                timing.hasPassed -> WordSungAlpha + 0.24f * timing.settle
                timing.isActive -> (0.6f + 0.4f * fillProgress).coerceAtLeast(WordDimAlpha + 0.26f * fillProgress)
                else -> 0.5f
            }

        val glowIntensity =
            when {
                timing.isActive -> fillProgress
                timing.hasPassed -> 0.55f * timing.settle
                else -> 0f
            }

        val wordColor = accent.copy(alpha = wordAlpha)
        val wordShadow =
            if (glowIntensity > 0.12f) {
                Shadow(
                    color = accent.copy(alpha = 0.5f * glowIntensity),
                    offset = Offset.Zero,
                    blurRadius = 6f + (12f * glowIntensity),
                )
            } else {
                null
            }

        withStyle(style = SpanStyle(color = wordColor, shadow = wordShadow)) {
            append(word.text)
        }
        if (word.hasTrailingSpace && wordIndex < item.words.size - 1) append(" ")
    }
}

private fun applySlideAnimation(
    item: LyricsEntry,
    isActiveLine: Boolean,
    effectivePlaybackPosition: Long,
    accent: Color,
): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    val words = item.words.orEmpty()
    // The sweep is measured across the whole line: the leading edge starts at
    // the line's first word and reaches its last at the line's end. A
    // `SpanStyle` brush cannot see where one word ends on screen, so the wipe
    // is split per character instead — each glyph is painted by which side of
    // the travelling edge it sits on, which reads as one continuous sweep.
    val lineStartMs = words.minOfOrNull { (it.startTime * 1000).toLong() } ?: 0L
    val lineEndMs = words.maxOfOrNull { (it.endTime * 1000).toLong() } ?: 0L
    val lineDurationMs = (lineEndMs - lineStartMs).coerceAtLeast(1L)
    val lineSweep =
        if (!isActiveLine || lineDurationMs <= 0L) {
            0f
        } else {
            easeWord(
                ((effectivePlaybackPosition - (lineStartMs - LeadInMillis)).toFloat() / (lineDurationMs + LeadInMillis))
                    .coerceIn(0f, 1f),
            )
        }
    var glyphsSeen = 0
    val totalGlyphs = words.sumOf { it.text.length }.coerceAtLeast(1)

    item.words?.forEachIndexed { wordIndex, word ->
        if (isActiveLine) {
            // The word's own light follows the sweep across its own glyphs, so
            // the wipe keeps travelling *inside* the word it is on instead of
            // landing on it all at once.
            val glyphWindow = word.text.length.toFloat() / totalGlyphs
            val wordHead = glyphsSeen.toFloat() / totalGlyphs
            val wordWindowStart = wordHead - glyphWindow * 0.5f
            val local = ((lineSweep - wordWindowStart) / glyphWindow.coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
            val edge = 0.12f
            word.text.forEachIndexed { glyphIndex, glyph ->
                val glyphCentre = (glyphIndex + 0.5f) / word.text.length.coerceAtLeast(1)
                // Wipe across the glyph: fully lit behind the edge, dim ahead,
                // with a soft rim right on it — that rim is the "sweep".
                val litness =
                    when {
                        glyphCentre <= local - edge -> 1f
                        glyphCentre >= local + edge -> 0f
                        else -> 1f - ((glyphCentre - (local - edge)) / (2f * edge)).coerceIn(0f, 1f)
                    }
                val glyphAlpha = (WordDimAlpha + (1f - WordDimAlpha) * easeWord(litness))
                    .coerceIn(0f, 1f)
                val glyphShadow =
                    if (litness in 0.2f..0.95f) {
                        Shadow(color = accent.copy(alpha = 0.35f * (1f - litness)), offset = Offset.Zero, blurRadius = 7f)
                    } else {
                        null
                    }
                withStyle(style = SpanStyle(color = accent.copy(alpha = glyphAlpha), shadow = glyphShadow)) {
                    append(glyph)
                }
            }
            glyphsSeen += word.text.length
        } else {
            // Inactive lines never wipe: they sit at the sweep's dim end.
            withStyle(style = SpanStyle(color = accent.copy(alpha = 0.55f))) {
                append(word.text)
            }
        }
        if (word.hasTrailingSpace && wordIndex < item.words.size - 1) append(" ")
    }
}

private fun applyKaraokeAnimation(
    item: LyricsEntry,
    isActiveLine: Boolean,
    effectivePlaybackPosition: Long,
    accent: Color,
): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    item.words?.forEachIndexed { wordIndex, word ->
        val wordStartMs = (word.startTime * 1000).toLong()
        val wordEndMs = (word.endTime * 1000).toLong()
        val timing = wordTiming(wordStartMs, wordEndMs, isActiveLine, effectivePlaybackPosition)

        // Karaoke is the hard fill: a word is either sung or it is not. The
        // shared timing still gives it the settle, so the fill lands without
        // a flash when the word starts lighting early.
        val wordAlpha =
            when {
                !isActiveLine -> 0.5f
                timing.hasPassed -> 1f
                timing.isActive -> 1f
                else -> 0.34f
            }

        val wordColor = accent.copy(alpha = wordAlpha)

        withStyle(style = SpanStyle(color = wordColor)) {
            append(word.text)
        }
        if (word.hasTrailingSpace && wordIndex < item.words.size - 1) append(" ")
    }
}

private fun applyAppleAnimation(
    item: LyricsEntry,
    isActiveLine: Boolean,
    effectivePlaybackPosition: Long,
    accent: Color,
): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    item.words?.forEachIndexed { wordIndex, word ->
        val wordStartMs = (word.startTime * 1000).toLong()
        val wordEndMs = (word.endTime * 1000).toLong()
        val timing = wordTiming(wordStartMs, wordEndMs, isActiveLine, effectivePlaybackPosition)

        // Apple-style: a fast bloom as the word arrives (faster start than the
        // shared ease gives on its own), then a slow breathing fall back to
        // "sung" on the settle. The bloom colour stays white-hot at the peak
        // and only then relaxes to the accent.
        val bloom =
            if (timing.isActive) {
                val fast = (timing.progress * 1.6f).coerceIn(0f, 1f)
                1f - ((1f - fast) * (1f - fast))
            } else {
                0f
            }
        val wordAlpha =
            when {
                !isActiveLine -> 0.6f
                timing.hasPassed -> 0.82f + 0.18f * timing.settle
                timing.isActive -> 0.55f + 0.45f * bloom
                else -> (0.4f + 0.12f * timing.progress).coerceAtMost(0.5f)
            }

        val wordColor = accent.copy(alpha = wordAlpha)

        val wordShadow =
            if ((timing.isActive && bloom > 0.3f) || timing.settle > 0.25f) {
                val glow = if (timing.isActive) bloom else timing.settle
                Shadow(
                    color = accent.copy(alpha = 0.3f * glow),
                    offset = Offset.Zero,
                    blurRadius = 5f + (7f * glow),
                )
            } else {
                null
            }

        withStyle(style = SpanStyle(color = wordColor, shadow = wordShadow)) {
            append(word.text)
        }
        if (word.hasTrailingSpace && wordIndex < item.words.size - 1) append(" ")
    }
}
