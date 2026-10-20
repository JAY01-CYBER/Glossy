/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The Search screen before anything is typed: the design's "Discover something
 * new." page.
 *
 * A search field that type-ahead searches as you write, three tabs that reshape
 * the page (Explore / Suggestions / Albums), the mood shelves that stand in for
 * browsing, the listener's recent searches and the trending shelves. The
 * artwork and ranks are real chart data — [SearchDiscoverRoute] feeds them in
 * from the charts view model, [SearchDiscoverPage] only draws them, so the
 * layout can be reasoned about (and previewed) without a network.
 */

package com.jay.glossy.ui.screens.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalNavController
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.metrolist.models.MediaMetadata
import com.jay.glossy.playback.queues.YouTubeQueue
import com.jay.glossy.ui.component.GlossyPillButton
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.ui.theme.glossySwatchFor
import com.jay.glossy.viewmodels.ChartsViewModel
import com.jay.glossy.viewmodels.ExploreViewModel
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.models.toMediaMetadata
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Connects the discovery page to the app: chart data, the recent-search
 * history, playback and navigation. Everything it hands [SearchDiscoverPage] is
 * already flattened, so the page itself has no idea a network exists.
 */
@Composable
fun SearchDiscoverRoute(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onSeeAllRecent: () -> Unit,
    modifier: Modifier = Modifier,
    chartsViewModel: ChartsViewModel = hiltViewModel(),
    exploreViewModel: ExploreViewModel = hiltViewModel(),
) {
    val chartsPage by chartsViewModel.chartsPage.collectAsStateWithLifecycle()
    val isLoading by chartsViewModel.isLoading.collectAsStateWithLifecycle()
    val error by chartsViewModel.error.collectAsStateWithLifecycle()
    // The explore browse is the fallback source: it returns the new-release
    // albums in every region, where the charts response can come back empty.
    val explorePage by exploreViewModel.explorePage.collectAsStateWithLifecycle()

    val database = LocalDatabase.current
    val history by database.searchHistory().collectAsState(initial = emptyList())
    val playerConnection = LocalPlayerConnection.current
    val navController = LocalNavController.current
    val metadataFlow =
        remember(playerConnection) {
            playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
        }
    val mediaMetadata by metadataFlow.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(DiscoverTab.Explore) }

    // The charts cache is shared with the Charts screen's view model, so this
    // only fires the first time the page is opened in a session.
    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            chartsViewModel.loadCharts()
        }
    }

    SearchDiscoverPage(
        query = query,
        onQueryChange = onQueryChange,
        focusRequester = focusRequester,
        tab = tab,
        onTabChange = { tab = it },
        data = remember(chartsPage, explorePage) {
            val fromCharts = chartsPage?.toDiscover() ?: SearchDiscoverData()
            fromCharts.copy(
                albums = fromCharts.albums
                    .ifEmpty { explorePage?.newReleaseAlbums.orEmpty() }
                    .distinctBy { it.id }
                    .take(8),
            )
        },
        isLoading = isLoading && chartsPage == null && explorePage == null,
        errorMessage = if (chartsPage == null && explorePage == null) error else null,
        recentSearches = remember(history) { history.take(3).map { it.query } },
        nowPlayingId = mediaMetadata?.id,
        onSearch = onSearch,
        onVoiceSearch = onVoiceSearch,
        onSeeAllRecent = onSeeAllRecent,
        onPlaySong = { song ->
            playerConnection?.playQueue(
                YouTubeQueue(
                    endpoint = WatchEndpoint(videoId = song.id),
                    preloadItem = song.toMediaMetadata(),
                ),
            )
        },
        onOpenAlbum = { album -> navController.navigate("album/${album.id}") },
        onRetry = {
            chartsViewModel.loadCharts()
            exploreViewModel.refresh()
        },
        modifier = modifier,
    )
}

/** The page itself: header, tabs, then whichever shelf the tab selected. */
@Composable
fun SearchDiscoverPage(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    tab: DiscoverTab,
    onTabChange: (DiscoverTab) -> Unit,
    data: SearchDiscoverData,
    isLoading: Boolean,
    errorMessage: String?,
    recentSearches: List<String>,
    nowPlayingId: String?,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onSeeAllRecent: () -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onOpenAlbum: (AlbumItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The floating mini player and navigation pill sit over the list, so the
    // last row has to scroll clear of them.
    val bottomInset =
        LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()

    // Read here, not inside the lazy content lambda: [LazyColumn]'s scope is not
    // composable, so the shelf states take their copy as plain strings.
    val retryLabel = stringResource(R.string.glossy_search_retry)
    val emptyText = stringResource(R.string.glossy_search_empty)
    val shelfMessage = errorMessage?.takeIf { it.isNotBlank() } ?: emptyText

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomInset + 28.dp),
    ) {
        item(key = "discover_header") {
            DiscoverHeader(
                query = query,
                onQueryChange = onQueryChange,
                focusRequester = focusRequester,
                onSearch = onSearch,
                onVoiceSearch = onVoiceSearch,
            )
        }

        item(key = "discover_tabs") {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = GlossyDimens.ScreenPadding, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DiscoverTab.entries.forEach { entry ->
                    DiscoverTabChip(
                        tab = entry,
                        active = entry == tab,
                        onClick = { onTabChange(entry) },
                    )
                }
            }
        }

        // The header and the tabs always draw, and so does everything the page
        // can show without a network: the mood grid and the recent searches are
        // local. A slow, empty or failed chart response must never blank the
        // whole page — each shelf that needs it reports itself instead.

        when (tab) {
            DiscoverTab.Explore -> {
                item(key = "discover_moods_title") {
                    DiscoverSectionTitle(stringResource(R.string.glossy_search_shelf))
                }
                item(key = "discover_moods") {
                    DiscoverMoodGrid(onSearch = onSearch)
                }
                if (recentSearches.isNotEmpty()) {
                    item(key = "discover_recent_title") {
                        DiscoverSectionTitle(
                            title = stringResource(R.string.glossy_search_recent),
                            action = stringResource(R.string.glossy_search_see_all),
                            onAction = onSeeAllRecent,
                        )
                    }
                    item(key = "discover_recent") {
                        RecentSearches(
                            queries = recentSearches,
                            onSearch = onSearch,
                        )
                    }
                }
                item(key = "discover_trending_title") {
                    DiscoverSectionTitle(stringResource(R.string.glossy_search_trending))
                }
                when {
                    data.albums.isNotEmpty() ->
                        item(key = "discover_trending") {
                            TrendingShelf(albums = data.albums, onOpenAlbum = onOpenAlbum)
                        }

                    isLoading ->
                        item(key = "discover_trending_loading") { DiscoverSkeleton() }

                    else ->
                        item(key = "discover_trending_notice") {
                            DiscoverNotice(
                                message = shelfMessage,
                                action = retryLabel,
                                onAction = onRetry,
                            )
                        }
                }
            }

            DiscoverTab.Suggestions -> {
                when {
                    data.ranked.isNotEmpty() -> {
                        item(key = "discover_ranked_title") {
                            DiscoverSectionTitle(stringResource(R.string.glossy_search_trending))
                        }
                        itemsIndexed(
                            items = data.ranked,
                            key = { _, song -> "discover_rank_${song.id}" },
                        ) { index, song ->
                            RankedSongRow(
                                rank = song.chartPosition ?: (index + 1),
                                song = song,
                                isActive = song.id == nowPlayingId,
                                onClick = { onPlaySong(song) },
                            )
                        }
                    }

                    isLoading ->
                        item(key = "discover_ranked_loading") { DiscoverSkeleton() }

                    else ->
                        item(key = "discover_ranked_notice") {
                            DiscoverNotice(
                                message = shelfMessage,
                                action = retryLabel,
                                onAction = onRetry,
                            )
                        }
                }
                if (data.albums.isNotEmpty()) {
                    item(key = "discover_suggestions_albums_title") {
                        DiscoverSectionTitle(stringResource(R.string.albums))
                    }
                    item(key = "discover_suggestions_albums") {
                        TrendingShelf(albums = data.albums, onOpenAlbum = onOpenAlbum)
                    }
                }
            }

            DiscoverTab.Albums -> {
                when {
                    data.albums.isNotEmpty() -> {
                        item(key = "discover_albums_title") {
                            DiscoverSectionTitle(stringResource(R.string.albums))
                        }
                        item(key = "discover_albums") {
                            AlbumGrid(albums = data.albums, onOpenAlbum = onOpenAlbum)
                        }
                    }

                    isLoading ->
                        item(key = "discover_albums_loading") { DiscoverSkeleton() }

                    else ->
                        item(key = "discover_albums_notice") {
                            DiscoverNotice(
                                message = shelfMessage,
                                action = retryLabel,
                                onAction = onRetry,
                            )
                        }
                }
            }
        }
    }
}

// ============================================================================
// Header
// ============================================================================

/**
 * Title, search field and the voice shortcut. The field writes straight into
 * the screen's query, so typing here is the same as typing in the toolbar the
 * results state shows.
 */
@Composable
private fun DiscoverHeader(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = GlossyDimens.ScreenPadding)) {
        Text(
            text = stringResource(R.string.glossy_search_discover),
            fontSize = 27.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
            color = GlossyPalette.Accent,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(GlossyDimens.CornerPill))
                    .background(GlossyPalette.Chip)
                    .padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = GlossyPalette.TextPrimary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                textStyle =
                    TextStyle(
                        color = GlossyPalette.TextPrimary,
                        fontSize = 16.sp,
                    ),
                cursorBrush = SolidColor(GlossyPalette.Accent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
                decorationBox = { innerTextField ->
                    if (query.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_yt_music),
                            fontSize = 16.sp,
                            color = GlossyPalette.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                },
            )
            IconButton(
                onClick = onVoiceSearch,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = stringResource(R.string.glossy_search_voice),
                    tint = GlossyPalette.TextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** One tab of the discovery page: an outlined pill, filled accent when active. */
@Composable
private fun DiscoverTabChip(
    tab: DiscoverTab,
    active: Boolean,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (active) GlossyPalette.Accent else GlossyPalette.Chip,
        label = "discoverTabContainer",
    )
    val content = if (active) GlossyPalette.OnAccent else GlossyPalette.TextSecondary
    Row(
        modifier =
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(GlossyDimens.CornerPill))
                .background(container)
                .clickable(role = Role.Tab, onClick = onClick)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(tab.icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(tab.label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            maxLines = 1,
        )
    }
}

// ============================================================================
// Sections
// ============================================================================

/** A section heading, optionally with the "See all" affordance on the right. */
@Composable
private fun DiscoverSectionTitle(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = GlossyDimens.ScreenPadding, end = GlossyDimens.ScreenPadding, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            color = GlossyPalette.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelMedium,
                color = GlossyPalette.TextSecondary,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(GlossyDimens.CornerSmall))
                        .clickable(role = Role.Button, onClick = onAction)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * The "Go somewhere new" shelf: four moods, two across. Each tile runs its own
 * search, so the page doubles as a way into the catalogue for listeners who do
 * not know what to type.
 */
@Composable
private fun DiscoverMoodGrid(onSearch: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = GlossyDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DiscoverMoods.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { mood ->
                    DiscoverMoodTile(
                        mood = mood,
                        onClick = { onSearch(mood.query) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DiscoverMoodTile(
    mood: DiscoverMood,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(118.dp)
                .clip(RoundedCornerShape(GlossyDimens.CornerCard))
                .background(Brush.verticalGradient(mood.gradient))
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        ) {
            Text(
                text = stringResource(mood.title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(mood.subtitle),
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A ranked chart row: the song, its artist, its position, and its artwork. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankedSongRow(
    rank: Int,
    song: SongItem,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = GlossyDimens.ScreenPadding, vertical = 2.5.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GlossyPalette.Row)
                .combinedClickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) GlossyPalette.Accent else GlossyPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = GlossyPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.glossy_search_rank, rank),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = GlossyPalette.Accent,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(14.dp))
        DiscoverArtwork(
            imageUrl = song.thumbnail,
            seed = song.id,
            corner = 8.dp,
            modifier = Modifier.size(56.dp),
        )
    }
}

/** The horizontal shelf of albums that closes Explore and Suggestions. */
@Composable
private fun TrendingShelf(
    albums: List<AlbumItem>,
    onOpenAlbum: (AlbumItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = GlossyDimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items = albums, key = { "discover_album_${it.id}" }) { album ->
            Column(
                modifier =
                    Modifier
                        .width(132.dp)
                        .clip(RoundedCornerShape(GlossyDimens.CornerCard))
                        .clickable(role = Role.Button) { onOpenAlbum(album) },
            ) {
                DiscoverArtwork(
                    imageUrl = album.thumbnail,
                    seed = album.id,
                    corner = GlossyDimens.CornerCard,
                    modifier = Modifier.size(132.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = album.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GlossyPalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val artists = album.artists?.joinToString(", ") { it.name }.orEmpty()
                if (artists.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = artists,
                        fontSize = 11.5.sp,
                        color = GlossyPalette.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The Albums tab: the same shelf data, two across. */
@Composable
private fun AlbumGrid(
    albums: List<AlbumItem>,
    onOpenAlbum: (AlbumItem) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = GlossyDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        albums.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { album ->
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(GlossyDimens.CornerCard))
                                .clickable(role = Role.Button) { onOpenAlbum(album) },
                    ) {
                        DiscoverArtwork(
                            imageUrl = album.thumbnail,
                            seed = album.id,
                            corner = GlossyDimens.CornerCard,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = album.title,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlossyPalette.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val artists = album.artists?.joinToString(", ") { it.name }.orEmpty()
                        if (artists.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = artists,
                                fontSize = 11.5.sp,
                                color = GlossyPalette.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** The last few queries, each one a shortcut back into the results. */
@Composable
private fun RecentSearches(
    queries: List<String>,
    onSearch: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = GlossyDimens.ScreenPadding)) {
        queries.forEach { entry ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlossyPalette.Row)
                        .clickable(role = Role.Button) { onSearch(entry) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.history),
                    contentDescription = null,
                    tint = GlossyPalette.TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GlossyPalette.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.glossy_search_recent_caption),
                        fontSize = 11.5.sp,
                        color = GlossyPalette.TextSecondary,
                    )
                }
            }
        }
    }
}

// ============================================================================
// States
// ============================================================================

/** Placeholder shelf while the charts are still on their way. */
@Composable
private fun DiscoverSkeleton() {
    Column(modifier = Modifier.padding(horizontal = GlossyDimens.ScreenPadding)) {
        Box(
            modifier =
                Modifier
                    .padding(top = 22.dp, bottom = 10.dp)
                    .height(18.dp)
                    .width(160.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GlossyPalette.Row),
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(2) {
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(118.dp)
                                    .clip(RoundedCornerShape(GlossyDimens.CornerCard))
                                    .background(GlossyPalette.Row),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A shelf that has nothing to show yet: one line of copy and a compact retry
 * pill, drawn in place of that shelf rather than over the whole page.
 */
@Composable
private fun DiscoverNotice(
    message: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = GlossyDimens.ScreenPadding, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = GlossyPalette.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        GlossyPillButton(text = action, onClick = onAction)
    }
}

// ============================================================================
// Artwork
// ============================================================================

/**
 * Artwork that always has somewhere to land: the category swatch shows until
 * the image resolves, and stays for items the catalogue has no cover for.
 */
@Composable
private fun DiscoverArtwork(
    imageUrl: String?,
    seed: String,
    corner: Dp,
    modifier: Modifier = Modifier,
) {
    val swatch = remember(seed) { glossySwatchFor(seed) }
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(corner))
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
    }
}
