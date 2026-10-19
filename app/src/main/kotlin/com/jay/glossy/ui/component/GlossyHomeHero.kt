/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The two pieces that open the home feed in the design:
 *
 *  - [GlossyDailyHero], the full-bleed editorial card: an eyebrow on a dark
 *    pill at the top, the track's name set large over a scrim at the bottom,
 *    and one round play button in the corner.
 *  - [GlossyMixCard], the two-across mix tile of the "Made for …" shelf: cover
 *    with its own label, title and caption underneath.
 *
 * Both paint from [GlossyPalette], so they follow the scheme the theme resolved
 * — the design's own dark colours are not baked in.
 */

package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jay.glossy.R
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.ui.theme.glossySwatchFor
import com.jay.glossy.utils.joinToArtistString
import com.jay.glossy.viewmodels.DailyDiscoverItem
import com.metrolist.innertube.models.SongItem

/**
 * The home's opening card: one recommendation, given the whole width.
 *
 * It replaces the carousel tile that used to hold the same data — same
 * [item], same [onClick] — with the single-card treatment of the design, so
 * the page starts on one strong image instead of a row of half-visible ones.
 */
@Composable
fun GlossyDailyHero(
    item: DailyDiscoverItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = item.recommendation as? SongItem
    val artists = remember(item.recommendation.id) {
        song?.artists?.joinToArtistString(" · ") { it.name }.orEmpty()
    }

    // Captured here rather than inside the draw lambdas: palette reads are
    // composition-only.
    val accent = GlossyPalette.Accent
    val onAccent = GlossyPalette.OnAccent
    val shape = RoundedCornerShape(GlossyDimens.CornerHero)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clip(shape)
                .background(GlossyPalette.Row)
                .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.recommendation.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Two washes, as in the design: a light one under the eyebrow so the
        // pill never floats on a bright sleeve, and a heavy one under the
        // title so the words stay white whatever the artwork does.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.42f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.80f),
                                ),
                        ),
                    ),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.42f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.glossy_hero_eyebrow),
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.4.sp,
                    color = Color.White,
                    maxLines = 1,
                )
            }

            Column(modifier = Modifier.padding(end = 62.dp)) {
                // The design's editorial headline: 35 with a 108% line box and
                // slightly tight tracking — the one headline at that scale, so
                // it stays the loudest thing under the eyebrow.
                Text(
                    text = item.recommendation.title,
                    fontSize = 35.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.6).sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artists.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = artists,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = stringResource(R.string.play),
                tint = onAccent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * One mix on the "Made for …" shelf: the cover with its label in the corner,
 * then the title and the caption under it.
 *
 * [label] is the small caps line over the artwork ("MADE FOR YOU"); [subtitle]
 * is the caption under the title, which for an account playlist is its author.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlossyMixCard(
    title: String,
    seed: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    imageUrl: String? = null,
    label: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val swatch = remember(seed) { glossySwatchFor(seed) }
    val coverShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.52f)
                    .clip(coverShape)
                    .background(swatch),
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Keeps the label legible on a pale cover without darkening the
            // whole image the way an even overlay would.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.38f), Color.Transparent),
                            ),
                        ),
            )
            if (!label.isNullOrBlank()) {
                Text(
                    text = label.uppercase(),
                    fontSize = 8.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                    color = Color.White,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.30f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = GlossyPalette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GlossyPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}
