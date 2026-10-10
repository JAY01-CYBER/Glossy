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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.glossy.R
import com.jay.glossy.LocalDatabase
import com.jay.glossy.ui.player.applemusic.toHighRes
import com.jay.glossy.ui.component.GlassBackdrop
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.PlayerHorizontalPadding
import com.jay.glossy.constants.MiniLyricsAnimationStyle
import com.jay.glossy.constants.MiniLyricsAnimationStyleKey
import com.jay.glossy.db.entities.LyricsEntity
import com.jay.glossy.lyrics.LyricsEntry
import com.jay.glossy.lyrics.LyricsUtils
import com.jay.glossy.lyrics.lyricsTextLooksSynced
import com.jay.glossy.ui.component.applyWordAnimation
import com.jay.glossy.utils.rememberEnumPreference
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
 * Height of the strip's content box — deliberately constant.
 *
 * The strip is a sibling of the artwork in every player design, so whatever
 * height it asks for is height the artwork does not get. That made the artwork
 * slide up and down the whole time lyrics were on: a line that wrapped onto a
 * second row grew the strip, [AnimatedContent] animated its own size on every
 * line change, and the loading / "no lyrics" states were yet another height.
 * Every state is now centred inside this one height, so the artwork above keeps
 * the exact same size from the first lyric of a song to the last.
 */
private val LYRICS_STRIP_CONTENT_HEIGHT = 52.dp

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

/**
 * How often the active line is re-resolved while playback runs.
 *
 * Halved from 100 ms for the same reason the clock below is smoothed: at 100 ms
 * the strip could sit on the old line for a whole tick after the buffer that
 * proved the line had changed. Resolving an index is a couple of comparisons
 * and only writes the state when the answer actually differs, so the extra
 * polls cost nothing.
 */
private const val LYRICS_TICK_MS = 50L

/**
 * Longest the last published position may be extrapolated before it is treated
 * as frozen rather than merely late.
 *
 * A player that has stalled (a buffer underrun, a decoder being rebuilt for the
 * canvas) stops publishing; without a ceiling the clock would keep walking away
 * from the audio and the lyrics would drift further out of sync the longer the
 * stall lasted.
 */
private const val LYRICS_CLOCK_MAX_EXTRAPOLATION_MS = 1_000L


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
    val preview =
        rememberPlayerLyricsPreview(
            mediaMetadata = mediaMetadata,
            positionProvider = positionProvider,
        )

    val miniLyricsAnimationStyle by rememberEnumPreference(
        MiniLyricsAnimationStyleKey,
        defaultValue = MiniLyricsAnimationStyle.FADE,
    )

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
        if (preview.hasLyrics) {
            GlassBackdrop(
                thumbnailUrl = mediaMetadata?.thumbnailUrl?.toHighRes(),
                shape = RoundedCornerShape(18.dp),
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
            modifier = Modifier.fillMaxWidth().height(LYRICS_STRIP_CONTENT_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
        when {
            preview.isLoading -> LyricsLinePlaceholder()

            !preview.hasLyrics -> LyricsNotFoundLine()

            !preview.synced -> PlainLyricsPreview(line = preview.plainLine, accent = accent)

            else -> {
                AnimatedContent(
                    targetState = preview.activeIndex,
                    contentAlignment = Alignment.Center,
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
                            val line = preview.lines.getOrNull(index + offset) ?: continue
                            val isActive = offset == 0
                            // Word-by-word animation, when one was chosen and the
                            // provider timed the words. The styles drive alpha,
                            // weight and glow only, so they are applied in the
                            // strip's own white: the accent the canvas glow uses
                            // would repaint a line that has always been white.
                            val animated =
                                if (isActive &&
                                    line.words?.isNotEmpty() == true &&
                                    miniLyricsAnimationStyle != MiniLyricsAnimationStyle.NONE
                                ) {
                                    applyWordAnimation(
                                        item = line,
                                        animationStyle = miniLyricsAnimationStyle,
                                        isActiveLine = true,
                                        // Reading the clock here keeps each tick's
                                        // recomposition inside this one text.
                                        effectivePlaybackPosition = preview.effectivePlaybackPosition,
                                        accent = Color.White,
                                    )
                                } else {
                                    null
                                }
                            // One line per row, with the line height pinned: a
                            // wrapped row would change the row's height, and
                            // rows of different heights are exactly what used
                            // to push the artwork around mid-song.
                            Text(
                                text = animated ?: AnnotatedString(line.text),
                                color = when {
                                    isActive -> Color.White
                                    else -> Color.White.copy(alpha = 0.5f)
                                },
                                fontSize = if (isActive) 19.sp else 15.sp,
                                lineHeight = if (isActive) 24.sp else 19.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
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
 * Lyrics as every on-screen player surface needs them: the lines themselves,
 * which of them is being sung, and the "still loading" / "no lyrics" / "no
 * timings" answers.
 *
 * Two quite different surfaces draw this — the frosted strip under the artwork
 * ([PlayerSyncedLyricsView]) and the glow painted on the artwork itself
 * ([PlayerCanvasGlowLyrics]) — and they must agree, to the line, on what is
 * being sung. Resolution therefore lives here rather than being written twice:
 * the on-demand fetch, the untimed-entry upgrade, the parse and the ticking
 * clock are all one implementation, and what a surface chooses to draw is its
 * own business.
 */
internal class PlayerLyricsPreview(
    val isLoading: Boolean,
    val hasLyrics: Boolean,
    /** True when the entry carries timings and can actually be followed. */
    val synced: Boolean,
    val lines: List<LyricsEntry>,
    val activeIndex: Int,
    val plainLine: String,
    /**
     * The ticking playback clock, with the song's lyrics offset already applied.
     *
     * It is exposed through [effectivePlaybackPosition] rather than kept as a
     * plain value so that a tick recomposes only the text that follows it: a
     * word-by-word animation needs the clock to move between line changes, and
     * holding it as state keeps the strip's card, glow and pocket from being
     * recomposed (in the worst case, repainted) at the clock's rate.
     */
    private val positionState: State<Long> = mutableStateOf(0L),
) {
    /** Playback position in milliseconds; reading it subscribes to the clock. */
    val effectivePlaybackPosition: Long
        get() = positionState.value
}

@Composable
internal fun rememberPlayerLyricsPreview(
    mediaMetadata: MediaMetadata?,
    positionProvider: () -> Long,
): PlayerLyricsPreview {
    val playerConnection = LocalPlayerConnection.current
    // Nothing is bound yet, so there is no song to follow: both surfaces read
    // this as "loading" and stay empty rather than showing a stale line.
    if (playerConnection == null) {
        return remember { PlayerLyricsPreview(true, false, false, emptyList(), -1, "") }
    }

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
                // An upgrade has to bypass the caches: they hold the untimed
                // text this pass exists to replace, so a cached answer would be
                // handed straight back and nothing would ever change.
                val fetched = entryPoint.lyricsHelper().getLyrics(metadata, forceRefresh = upgradeUntimed)
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
    // strip; show the opening line with an honest hint instead. Just the one
    // line: the strip is a fixed height (see [LYRICS_STRIP_CONTENT_HEIGHT]) and
    // the hint has to fit in it next to the lyric.
    val plainLine = remember(lyricsText, synced) {
        if (!synced && hasLyrics && lyricsText != null) {
            lyricsText.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()
        } else {
            ""
        }
    }

    val lyricsOffset = (currentSong?.song?.lyricsOffset ?: 0).toLong()
    // Kept in a state holder so the ticking effect below is never restarted by
    // a recomposition that recreates the lambda.
    val currentPositionProvider by rememberUpdatedState(positionProvider)

    var currentIndex by remember(timedLines) { mutableIntStateOf(-1) }
    // The clock the word-by-word animations follow, published as state so that
    // only the text reading it recomposes on each tick.
    val playbackPositionState = remember { mutableLongStateOf(0L) }
    LaunchedEffect(timedLines, lyricsOffset) {
        if (timedLines.isEmpty()) {
            currentIndex = -1
            return@LaunchedEffect
        }
        // The clock is re-extrapolated here rather than read raw, and this is
        // what puts the line on the word being sung.
        //
        // A player does not report a continuously moving position. It publishes
        // a fresh, authoritative one every audio buffer or so — a few hundred
        // milliseconds apart — and between publications the value the app holds
        // is simply frozen at the last one. Following that number directly made
        // every line change *after* its word: the offset was whatever slice of a
        // buffer the tick happened to land on. The full lyrics view has always
        // compensated for this; the mini lyrics did not, so the two disagreed by
        // up to a buffer and the strip drifted late against the audio.
        //
        // So: keep the last published position and the moment it arrived, and
        // every tick move forward from there by the wall time since. A genuinely
        // new position re-anchors the pair, which also covers seeks and skips
        // for free. Reading the raw value at each tick costs nothing on top of
        // the poll the caller's provider already does.
        var anchor = currentPositionProvider()
        var anchorAt = System.currentTimeMillis()
        while (isActive) {
            val now = System.currentTimeMillis()
            val published = currentPositionProvider()
            if (published != anchor) {
                anchor = published
                anchorAt = now
            }
            // Extrapolation only helps while the audio is actually running: a
            // paused player has published its final position and must stay on
            // it, and a stalled one must not be walked away from.
            val elapsed =
                if (playerConnection.player.isPlaying) {
                    (now - anchorAt).coerceIn(0L, LYRICS_CLOCK_MAX_EXTRAPOLATION_MS)
                } else {
                    0L
                }
            val position = anchor + elapsed + lyricsOffset
            playbackPositionState.longValue = position
            val index = LyricsUtils.findCurrentLineIndex(timedLines, position)
            if (index != currentIndex) currentIndex = index
            delay(LYRICS_TICK_MS)
        }
    }

    val preview =
        remember(lyricsText, hasLyrics, synced, timedLines, currentIndex, plainLine) {
            PlayerLyricsPreview(
                isLoading = lyricsText == null,
                hasLyrics = hasLyrics,
                synced = synced,
                lines = timedLines,
                activeIndex = currentIndex,
                plainLine = plainLine,
                positionState = playbackPositionState,
            )
        }
    return preview
}

/**
 * Untimed lyrics: the opening line plus a hint saying why it does not move.
 * Better than a block that silently sits on the first line forever.
 *
 * Two rows only, because the strip's box is a fixed height: a five-line block
 * used to add a visible slab of height the moment a song's lyrics turned out
 * to be untimed, which moved the artwork above it.
 */
@Composable
private fun PlainLyricsPreview(
    line: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = line,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.lyrics_not_time_synced),
            color = accent.copy(alpha = 0.85f),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
        )
    }
}

/**
 * Shown when a song has no lyrics at all. The strip keeps its height rather
 * than collapsing, so the artwork does not grow back into the space mid-song;
 * a muted line explains the empty room instead of leaving a blank band.
 */
@Composable
private fun LyricsNotFoundLine(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.lyrics_not_found),
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 13.sp,
        lineHeight = 17.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
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
