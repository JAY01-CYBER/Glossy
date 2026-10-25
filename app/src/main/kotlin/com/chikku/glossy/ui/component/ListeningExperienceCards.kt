/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.chikku.glossy.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.R
import com.jay.glossy.ui.theme.GlossyDimens
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One fact from the listener's history, as a card you could keep in a wallet.
 *
 * The stats screen's charts say *which* songs, artists and albums came first;
 * these say how the period added up, and they say it as four objects rather
 * than as a row of numbers. A card is a thing people already know how to read:
 * one fact on the front, a name at the foot so it is obviously yours, and a
 * shape — 85.6 × 54mm, 1.586:1 — that does most of the recognising on its own.
 *
 * The background is the card's own gradient, mixed from the fact it carries:
 * warm for time, violet for a song, teal for an artist, amber for an album.
 * Artwork would put a thumbnail of the song behind the numbers, which is the
 * one thing these cards are not — they are about the listening, not the cover.
 */
data class ListeningExperienceCard(
    /** What the figure is, uppercase in the picker: "Top artist". */
    val label: String,
    /** The figure itself: a name, a title, a count. */
    val value: String,
    /** The supporting line, already formatted: "46 min · 11 plays". */
    val detail: String?,
)

/**
 * The four cards in a row, one per headline figure.
 *
 * A sideways rail rather than a stack: the cards are one story read left to
 * right, and the next one peeking in says there is more without spending a
 * screen of height on it.
 */
@Composable
fun ListeningExperienceRail(
    cards: List<ListeningExperienceCard>,
    /** Whose cards these are. Blank falls back to a neutral name. */
    holder: String,
    /** `MM/YY` of the account's first play, or null when there is none. */
    memberSince: String?,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = GlossyDimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = cards, key = { it.label }) { card ->
            ListeningExperienceCardFace(
                card = card,
                holder = holder,
                memberSince = memberSince,
                // Most of the screen wide, so the next card is always peeking in
                // and the row reads as something you can move.
                modifier = Modifier.fillParentMaxWidth(0.86f),
            )
        }
    }
}

@Composable
private fun ListeningExperienceCardFace(
    card: ListeningExperienceCard,
    holder: String,
    memberSince: String?,
    modifier: Modifier = Modifier,
) {
    val palette = remember(card.label) { paletteFor(card.label) }
    Box(
        modifier =
            modifier
                .aspectRatio(CARD_RATIO)
                .shadow(elevation = 16.dp, shape = CardShape, clip = false)
                .clip(CardShape),
    ) {
        CardWash(palette = palette)
        // Deepened towards the foot, where the pale embossed lines are, so they
        // never land on whichever bright patch of the gradient drifted under them.
        Box(Modifier.fillMaxSize().background(CardScrim))
        // A wide diagonal wash and a slow breathing glow, which is what makes
        // the flat gradient read as lit glass rather than as a coloured box.
        Box(Modifier.fillMaxSize().background(CardSheen))
        CardGlow(accent = palette.accent)

        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
            // What the card is on the left, whose it is on the right — the two
            // corners a real card says them from.
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "Your listening experience".uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 1.4.sp,
                    lineHeight = 13.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                // The Glossy mark, not a generic disc: the notification icon is
                // already the one-colour form of the brand, so the card borrows
                // it rather than shipping a second copy of the curves.
                Icon(
                    painter = painterResource(R.drawable.small_icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = card.value,
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 27.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.W800,
                        brush = PolishedInk,
                        shadow = EmbossShadow,
                    ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.W700,
                letterSpacing = 1.8.sp,
                color = Color.White.copy(alpha = 0.65f),
            )

            // Weighted rather than pinned to the foot, so the figure sits where
            // a card's number sits — across the middle.
            Spacer(Modifier.weight(0.85f))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Embossed(text = holder.ifBlank { "Listener" }.uppercase(Locale.getDefault()), size = 13.sp)
                    if (!card.detail.isNullOrBlank()) {
                        Text(
                            text = card.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (memberSince != null) {
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Member since".uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            letterSpacing = 0.8.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            textAlign = TextAlign.End,
                        )
                        Spacer(Modifier.height(1.dp))
                        Embossed(text = memberSince, size = 12.sp)
                    }
                }
            }
        }
    }
}

/** The card's colour: a three-stop diagonal, lit from the top-left. */
@Composable
private fun CardWash(palette: CardPalette) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = palette.stops)),
    )
}

/**
 * The card's slow light: a broad highlight in the card's own accent, plus a
 * vignette that keeps the corners from competing with the type.
 *
 * The alpha is animated rather than the colours, so the effect is one draw per
 * frame and costs nothing as the rail scrolls.
 */
@Composable
private fun CardGlow(accent: Color) {
    val transition = rememberInfiniteTransition(label = "listening-card-glow")
    val glow by
        transition.animateFloat(
            initialValue = 0.05f,
            targetValue = 0.14f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 5_600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glow-alpha",
        )
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    accent.copy(alpha = glow + 0.10f),
                                    accent.copy(alpha = 0f),
                                ),
                            center = Offset(size.width * 0.18f, size.height * 0.04f),
                            radius = size.maxDimension * 0.95f,
                        ),
                )
                drawRect(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                            center = Offset(size.width * 0.5f, size.height * 0.45f),
                            radius = size.maxDimension * 0.8f,
                        ),
                )
            },
    )
}

/** A line pressed into the card rather than printed on it. */
@Composable
private fun Embossed(text: String, size: TextUnit) {
    Text(
        text = text,
        style =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.W600,
                fontSize = size,
                letterSpacing = 1.6.sp,
                brush = PolishedInk,
                shadow = EmbossShadow,
            ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** How long a card is summarising: "17 min", or "2 h 04 min" past the hour. */
fun formatListeningSpan(millis: Long?): String {
    val minutes = ((millis ?: 0L) / 60_000L).coerceAtLeast(0L)
    return if (minutes >= 60) "%d h %02d min".format(minutes / 60, minutes % 60) else "$minutes min"
}

/** `MM/YY`, the date a card is dated from. */
fun memberSinceLabel(timestamp: LocalDateTime?): String? =
    timestamp?.format(DateTimeFormatter.ofPattern("MM/yy"))

/**
 * One gradient per fact, so the rail reads as four related cards without four
 * copies of the same picture. The labels are the four the stats screen builds;
 * anything else falls back to the app's own plum so a new card still looks
 * deliberate.
 */
private data class CardPalette(val stops: List<Color>, val accent: Color)

private fun paletteFor(label: String): CardPalette =
    when (label.lowercase(Locale.ROOT)) {
        "minutes listened" ->
            CardPalette(
                stops = listOf(Color(0xFF8C3B57), Color(0xFF3B1B2C), Color(0xFF150E14)),
                accent = Color(0xFFF0B7C8),
            )
        "top song" ->
            CardPalette(
                stops = listOf(Color(0xFF4E3A86), Color(0xFF241C44), Color(0xFF110F1D)),
                accent = Color(0xFFC9BCFF),
            )
        "top artist" ->
            CardPalette(
                stops = listOf(Color(0xFF1E5C5B), Color(0xFF122B30), Color(0xFF0B1519)),
                accent = Color(0xFF9BE6DA),
            )
        "top album" ->
            CardPalette(
                stops = listOf(Color(0xFF7A4A20), Color(0xFF32200F), Color(0xFF171008)),
                accent = Color(0xFFFFD2A1),
            )
        else ->
            CardPalette(
                stops = listOf(Color(0xFF3F3450), Color(0xFF1B1724), Color(0xFF111014)),
                accent = Color(0xFFE5D6FF),
            )
    }

/** The fill on every raised line: bright along the top edge, cooler below. */
private val PolishedInk =
    Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8F9FC), Color(0xFFDDE1E8)))

/** What makes the fill above read as raised rather than merely pale. */
private val EmbossShadow =
    Shadow(color = Color(0x99000000), offset = Offset(0f, 2.5f), blurRadius = 4f)

private val CardScrim =
    Brush.verticalGradient(
        0.0f to Color.Black.copy(alpha = 0.22f),
        0.55f to Color.Black.copy(alpha = 0.34f),
        1.0f to Color.Black.copy(alpha = 0.58f),
    )

/** Light from the top-left, shade into the bottom-right, like the cards in hand. */
private val CardSheen =
    Brush.linearGradient(
        0.0f to Color.White.copy(alpha = 0.08f),
        0.36f to Color.White.copy(alpha = 0f),
        0.72f to Color.White.copy(alpha = 0f),
        1.0f to Color.Black.copy(alpha = 0.18f),
    )

private val CardShape = RoundedCornerShape(20.dp)

/** 85.6mm × 54mm, which is what makes the shape read as a card. */
private const val CARD_RATIO = 1.586f
