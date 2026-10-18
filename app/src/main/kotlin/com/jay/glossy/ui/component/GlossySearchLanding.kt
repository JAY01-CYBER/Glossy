/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The search screen's landing, before a query is typed: the design's
 * "Find your sound" page. An eyebrow over the title, a four-tile shelf of moods
 * to browse into, and the searches the listener made most recently.
 *
 * The tiles are shortcuts, not decoration: each one runs a search, so the page
 * doubles as a way into the catalogue for people who do not know what to type.
 */

package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.LocalDatabase
import com.jay.glossy.R
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.ui.theme.glossySwatchFor

/** One mood tile: the words on it, what it searches for, and its two hues. */
private data class SearchMood(
    val title: Int,
    val subtitle: Int,
    val swatch: String,
)

/**
 * The four shelves of the landing page, in the order the design lists them.
 * Each one searches its own genre name, which is what the catalogue's search
 * answers best.
 */
private val SearchMoods = listOf(
    SearchMood(R.string.glossy_search_mood_indie, R.string.glossy_search_mood_indie_sub, "indie soul"),
    SearchMood(R.string.glossy_search_mood_after_dark, R.string.glossy_search_mood_after_dark_sub, "late night"),
    SearchMood(R.string.glossy_search_mood_easy, R.string.glossy_search_mood_easy_sub, "easy listening"),
    SearchMood(R.string.glossy_search_mood_desi, R.string.glossy_search_mood_desi_sub, "desi hits"),
)

/**
 * The landing page itself. [onSearch] runs a query (the tiles and the recent
 * rows both use it); [onSeeAll] opens the full history screen and may be null.
 */
@Composable
fun GlossySearchLanding(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
) {
    val database = LocalDatabase.current
    val history by database.searchHistory().collectAsState(initial = emptyList())
    val recent = remember(history) { history.take(3) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 18.dp, bottom = 32.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = GlossyDimens.ScreenPadding)) {
            GlossyEyebrow(stringResource(R.string.glossy_search_eyebrow))
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.glossy_search_title),
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.6).sp,
                color = GlossyPalette.TextPrimary,
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.glossy_search_shelf),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = GlossyPalette.TextPrimary,
            modifier = Modifier.padding(horizontal = GlossyDimens.ScreenPadding),
        )

        Spacer(Modifier.height(12.dp))

        // Two across, two down — the shelf the design shows. The rows are built
        // by hand rather than by a lazy grid because the page is short enough to
        // scroll as one column.
        Column(
            modifier = Modifier.padding(horizontal = GlossyDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchMoods.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { mood ->
                        val title = stringResource(mood.title)
                        SearchMoodTile(
                            title = title,
                            subtitle = stringResource(mood.subtitle),
                            swatch = mood.swatch,
                            onClick = { onSearch(title) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GlossyDimens.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.glossy_search_recent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GlossyPalette.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (onSeeAll != null) {
                    Text(
                        text = stringResource(R.string.glossy_search_see_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = GlossyPalette.TextSecondary,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onSeeAll)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            recent.forEach { entry ->
                GlossyListRow(
                    title = entry.query,
                    subtitle = stringResource(R.string.glossy_search_recent_caption),
                    onClick = { onSearch(entry.query) },
                    leading = {
                        GlossyThumb(
                            seed = entry.query,
                            size = 44.dp,
                            corner = 10.dp,
                        )
                    },
                    trailing = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint = GlossyPalette.TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}

/**
 * A mood tile: the artwork stand-in is a two-hue wash from the item's swatch,
 * which keeps the shelf photographic in rhythm without shipping four images.
 */
@Composable
private fun SearchMoodTile(
    title: String,
    subtitle: String,
    swatch: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = remember(swatch) { glossySwatchFor(swatch) }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier =
            modifier
                .height(118.dp)
                .clip(shape)
                .background(base)
                .clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.55f),
                                ),
                        ),
                    ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
