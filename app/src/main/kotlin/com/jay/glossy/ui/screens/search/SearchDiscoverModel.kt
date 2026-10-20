/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The data behind the Search screen's discovery page: which tabs it offers, the
 * mood shelves that stand in for browsing, and the shape the chart response is
 * mapped into before anything is rendered.
 *
 * Kept apart from the composables so the page itself only knows how to draw a
 * [SearchDiscoverData], never where the numbers came from.
 */

package com.jay.glossy.ui.screens.search

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.jay.glossy.R
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.ChartsPage

/** The three ways the discovery page can be sliced, in the design's order. */
enum class DiscoverTab(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    Explore(R.string.glossy_search_tab_explore, R.drawable.explore_outlined),
    Suggestions(R.string.glossy_search_tab_suggestions, R.drawable.star),
    Albums(R.string.albums, R.drawable.album),
}

/**
 * One mood tile: the words on it, the query it runs, and the two hues its wash
 * fades between. The hues are content colours, not theme tokens — the tiles
 * keep their identity whichever palette the app is running.
 */
data class DiscoverMood(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val query: String,
    val gradient: List<Color>,
)

/** The shelves of "Go somewhere new", in the order the design lists them. */
val DiscoverMoods = listOf(
    DiscoverMood(
        title = R.string.glossy_search_mood_indie,
        subtitle = R.string.glossy_search_mood_indie_sub,
        query = "indie soul",
        gradient = listOf(Color(0xFF37937F), Color(0xFF1F6555)),
    ),
    DiscoverMood(
        title = R.string.glossy_search_mood_after_dark,
        subtitle = R.string.glossy_search_mood_after_dark_sub,
        query = "late night",
        gradient = listOf(Color(0xFF859744), Color(0xFF5D7029)),
    ),
    DiscoverMood(
        title = R.string.glossy_search_mood_easy,
        subtitle = R.string.glossy_search_mood_easy_sub,
        query = "easy listening",
        gradient = listOf(Color(0xFF5578BE), Color(0xFF37558F)),
    ),
    DiscoverMood(
        title = R.string.glossy_search_mood_desi,
        subtitle = R.string.glossy_search_mood_desi_sub,
        query = "desi hits",
        gradient = listOf(Color(0xFFA93E62), Color(0xFF752845)),
    ),
)

/** Everything the discovery page draws, already trimmed to a shelf's worth. */
data class SearchDiscoverData(
    val ranked: List<SongItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
) {
    val isEmpty: Boolean
        get() = ranked.isEmpty() && albums.isEmpty()
}

/** How many rows a shelf shows before the page needs a "see all". */
private const val RankedCount = 5
private const val AlbumCount = 8

/**
 * Maps the chart response onto the page's shelves.
 *
 * The trending chart is what the ranked list wants, so its songs win over any
 * other section's; albums come from wherever the response lists them (new
 * releases first, otherwise the first section that has any). Both are
 * de-duplicated because the same song legitimately appears in several charts.
 */
fun ChartsPage.toDiscover(
    rankedCount: Int = RankedCount,
    albumCount: Int = AlbumCount,
): SearchDiscoverData {
    // Music videos are not what the page's rows want, but a response that holds
    // nothing else still beats an empty page: fall back to the whole sheet
    // rather than dropping every row the chart did return.
    val withoutVideos = sections.filter { it.title != "Top music videos" }
    val usable = withoutVideos.ifEmpty { sections }
    val trending = usable.firstOrNull { it.chartType == ChartsPage.ChartType.TRENDING }
    val ordered = listOfNotNull(trending) + usable.filter { it !== trending }

    val ranked = ordered
        .asSequence()
        .flatMap { it.items.asSequence() }
        .filterIsInstance<SongItem>()
        .distinctBy { it.id }
        .take(rankedCount)
        .toList()

    val albums = (usable.firstOrNull { it.chartType == ChartsPage.ChartType.NEW_RELEASES }
        ?.let { listOf(it) } ?: usable)
        .asSequence()
        .flatMap { it.items.asSequence() }
        .filterIsInstance<AlbumItem>()
        .distinctBy { it.id }
        .take(albumCount)
        .toList()

    return SearchDiscoverData(ranked = ranked, albums = albums)
}
