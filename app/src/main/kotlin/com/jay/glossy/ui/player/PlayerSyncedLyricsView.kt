/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The "lyrics on the player" preview: a couple of lines of the song rendered on
 * the now-playing screen itself, the way the reference design shows them. The
 * line being sung is large and bright, the line after it is dimmed, and the
 * block advances as playback moves.
 *
 * The active line is resolved here, on a short timer, from the position the
 * caller hands in — it is deliberately not derived during composition. The
 * full-screen player designs that show this strip no longer read the ticking
 * playback position themselves (that read is what used to recompose the whole
 * screen at 10 Hz), so a strip that waited for its parent to recompose would
 * simply sit on the opening line forever.
 *
 * Lyrics without timestamps cannot be followed at all; those are shown as a
 * static preview with a hint instead of pretending to be synced.
 *
 * Enabled from the player overflow menu ("Show Lyrics" / "Hide Lyrics") and
 * remembered in [com.jay.glossy.constants.ShowLyricsOnPlayerKey].
 */

package com.jay.glossy.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.glossy.LocalDatabase
import com.jay.glossy.ui.player.applemusic.toHighRes
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.PlayerHorizontalPadding
import com.jay.glossy.db.entities.LyricsEntity
import com.jay.glossy.lyrics.LyricsUtils
import com.jay.glossy.lyrics.lyricsTextLooksSynced
import com.metrolist.models.MediaMetadata
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * How many lines follow the active one. The line before it is deliberately not
 * rendered: the strip sits in the tight space above the song title, so it shows
 * the current line and what is coming next instead of a three-line block.
 */
private const val LYRICS_LOOKAHEAD_LINES = 1

/** Outward breathing room the glass card gets around the lyrics text. */
private val GLASS_BLEED = 12.dp

/**
 * Grows a backdrop beyond its parent's bounds. The lyrics text has to keep the
 * design's inset (it is aligned with the song title), so the card expands
 * outward rather than the text taking inner padding — the parent Box does not
 * clip, so the extra margin reaches into the surrounding gap.
 *
 * Vertical bleed stays at zero by default: the strip sits directly above the
 * song title (and above the controls in the full-screen designs), so growing
 * the card up and down would draw it over that text.
 */
private fun Modifier.backdropBleed(
    horizontal: androidx.compose.ui.unit.Dp,
    vertical: androidx.compose.ui.unit.Dp = 0.dp,
): Modifier =
    then(
        Modifier.layout { measurable, constraints ->
            val hpx = horizontal.roundToPx()
            val vpx = vertical.roundToPx()
            val child = measurable.measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + hpx * 2 else constraints.maxWidth,
                    maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight + vpx * 2 else constraints.maxHeight,
                ),
            )
            layout(child.width, child.height) { child.place(-hpx, -vpx) }
        },
    )

/** How often the active line is re-resolved while playback runs. */
private const val LYRICS_TICK_MS = 100L

/** Plain (untimed) lyrics show this many lines before the hint. */
private const val PLAIN_PREVIEW_LINES = 5

@Composable
fun PlayerSyncedLyricsView(
    mediaMetadata: MediaMetadata?,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFB79CFF),
    onExpand: (() -> Unit)? = null,
    /**
     * Side inset applied by this strip itself. Callers whose parent column is
     * already padded by [PlayerHorizontalPadding] (the Vinyl design) pass 0.dp —
     * otherwise the two insets stack and the lyrics sit a full double-padding
     * to the right of the song title below them.
     */
    horizontalPadding: androidx.compose.ui.unit.Dp = PlayerHorizontalPadding,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()

    val lyricsText = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }

    // Same on-demand fetch as the full lyrics view, so switching the option on
    // mid-song still fills the strip in.
    //
    // Cached plain-text lyrics are also re-requested once per song: an untimed
    // entry stored earlier never gets replaced on its own, which left the strip
    // stuck showing the static preview even when a provider could time-sync the
    // song. The guard keeps this to a single attempt so it cannot loop.
    var upgradeAskedFor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        val metadata = mediaMetadata ?: return@LaunchedEffect
        val cachedText = currentLyrics?.lyrics?.trim()
        val missing = currentLyrics == null
        val upgradeUntimed =
            !missing &&
                cachedText != null &&
                cachedText != LyricsEntity.LYRICS_NOT_FOUND &&
                !lyricsTextLooksSynced(cachedText) &&
                upgradeAskedFor != metadata.id
        if (!missing && !upgradeUntimed) return@LaunchedEffect
        upgradeAskedFor = metadata.id
        scope.launch(Dispatchers.IO) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    com.jay.glossy.di.LyricsHelperEntryPoint::class.java,
                )
                val fetched = entryPoint.lyricsHelper().getLyrics(metadata)
                // Only overwrite when the retry actually found something timed;
                // otherwise the existing entry stays and no upgrade happens.
                if (!missing && !lyricsTextLooksSynced(fetched.lyrics)) return@launch
                database.query {
                    upsert(LyricsEntity(metadata.id, fetched.lyrics, fetched.provider))
                }
            } catch (_: Exception) {
            }
        }
    }

    val hasLyrics = lyricsText != null && lyricsText.isNotEmpty() && lyricsText != LyricsEntity.LYRICS_NOT_FOUND
    val parsedLines = remember(lyricsText, hasLyrics) {
        if (hasLyrics && lyricsText != null) {
            LyricsUtils.parseLyrics(lyricsText).filter { it.text.isNotBlank() }
        } else {
            emptyList()
        }
    }
    // Two timed lines are the least that can actually be followed; anything
    // shorter is treated as plain text rather than pretending to be synced.
    val timedLines = remember(parsedLines) { if (parsedLines.size >= 2) parsedLines else emptyList() }
    val synced = timedLines.isNotEmpty()
    // Providers that only return plain text previously rendered as an empty
    // strip; show the opening lines with an honest hint instead.
    val plainLines = remember(lyricsText, synced) {
        if (!synced && hasLyrics && lyricsText != null) {
            lyricsText.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(PLAIN_PREVIEW_LINES)
                .toList()
        } else {
            emptyList()
        }
    }

    val lyricsOffset = (currentSong?.song?.lyricsOffset ?: 0).toLong()
    // Kept in a state holder so the ticking effect below is never restarted by
    // a recomposition that recreates the lambda.
    val currentPositionProvider by rememberUpdatedState(positionProvider)

    var currentIndex by remember(timedLines) { mutableIntStateOf(-1) }
    LaunchedEffect(timedLines, lyricsOffset) {
        if (timedLines.isEmpty()) {
            currentIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            val position = currentPositionProvider() + lyricsOffset
            val index = LyricsUtils.findCurrentLineIndex(timedLines, position)
                .coerceIn(0, timedLines.lastIndex)
            if (index != currentIndex) currentIndex = index
            delay(LYRICS_TICK_MS)
        }
    }

    val expandInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            // Clearance for the glass card's outward bleed: without it the card
            // extends over the song title that sits directly below the strip.
            .padding(bottom = 14.dp)
            .then(
                if (onExpand != null) {
                    Modifier.clickable(
                        interactionSource = expandInteraction,
                        indication = null,
                        onClick = onExpand,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        // Apple Music-style frosted card behind the strip. It bleeds outward
        // instead of the text taking inner padding, so the lines keep the exact
        // left edge they share with the song title below.
        if (hasLyrics) {
            LyricsGlassBackdrop(
                thumbnailUrl = mediaMetadata?.thumbnailUrl?.toHighRes(),
                shape = 18.dp,
                artworkAlpha = 0.35f,
                scrim = Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.45f),
                    ),
                ),
                border = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.matchParentSize().backdropBleed(horizontal = GLASS_BLEED),
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
        when {
            lyricsText == null -> LyricsLinePlaceholder()

            !hasLyrics -> Unit

            !synced -> PlainLyricsPreview(lines = plainLines, accent = accent)

            else -> {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 2 },
                        ) + fadeIn(tween(320)))
                            .togetherWith(
                                slideOutVertically(
                                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                                    targetOffsetY = { -it / 2 },
                                ) + fadeOut(tween(200)),
                            )
                    },
                    label = "playerSyncedLyrics",
                ) { index ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (offset in 0..LYRICS_LOOKAHEAD_LINES) {
                            val line = timedLines.getOrNull(index + offset) ?: continue
                            val isActive = offset == 0
                            Text(
                                text = line.text,
                                color = when {
                                    isActive -> Color.White
                                    else -> Color.White.copy(alpha = 0.5f)
                                },
                                fontSize = if (isActive) 19.sp else 15.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 2,
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
 * Untimed lyrics: the opening lines plus a hint saying why they do not move.
 * Better than a block that silently sits on the first line forever.
 */
@Composable
private fun PlainLyricsPreview(
    lines: List<String>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEachIndexed { index, line ->
            Text(
                text = line,
                color = Color.White.copy(alpha = if (index == 0) 0.95f else 0.45f),
                fontSize = if (index == 0) 17.sp else 14.sp,
                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "Lyrics aren't time-synced",
            color = accent.copy(alpha = 0.85f),
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

/** Skeleton shown while lyrics are still being fetched. */
@Composable
private fun LyricsLinePlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "lyricsLinePlaceholder")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lyricsLinePlaceholderAlpha",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = alpha)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.38f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = alpha * 0.6f)),
        )
    }
}
