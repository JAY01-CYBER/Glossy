/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Mix, on the design's terms: the "Your sound, reimagined / Mix for you"
 * lockup, the three filters, the featured Liked music card and the
 * artwork-led personalised mixes below it.
 *
 * The screen is a single grid: the heading, the filters, the featured card and
 * the closing line each span both columns, so the page scrolls as one sheet and
 * the floating player and navigation never sit on top of a section of their
 * own. Every card is built from the app's real YouTube data — the names and
 * counts on the Figma frames are examples, not values to hard-code.
 */

package com.jay.glossy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.R
import com.jay.glossy.ui.component.GlossyChipRow
import com.jay.glossy.ui.component.GlossyEyebrow
import com.jay.glossy.ui.component.GlossyMixCard
import com.jay.glossy.ui.component.GlossyThumb
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.menu.YouTubeAlbumMenu
import com.jay.glossy.ui.menu.YouTubePlaylistMenu
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.viewmodels.HomeViewModel
import com.jay.glossy.viewmodels.MixViewModel
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import kotlinx.coroutines.launch

/** The three filters of the design's Mix page. */
private enum class MixFilter { FOR_YOU, FAVOURITES, DISCOVER }

private val MixFilter.labelRes: Int
    get() = when (this) {
        MixFilter.FOR_YOU -> R.string.glossy_mix_for_you
        MixFilter.FAVOURITES -> R.string.glossy_mix_favourites
        MixFilter.DISCOVER -> R.string.glossy_mix_discover
    }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MixScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    mixViewModel: MixViewModel = hiltViewModel()
) {
    val accountPlaylists by homeViewModel.accountPlaylists.collectAsStateWithLifecycle()
    val ytMixes by mixViewModel.mixPlaylists.collectAsStateWithLifecycle()
    val isLoading by mixViewModel.isLoading.collectAsStateWithLifecycle()

    val insetsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        homeViewModel.loadHomeData()
        mixViewModel.loadMixes()
    }

    var filter by rememberSaveable { mutableStateOf(MixFilter.FOR_YOU) }

    // The Liked music playlist is the design's featured card. It comes from the
    // account feed, so a signed-in listener sees their own collection; a guest
    // simply has no featured card and the page starts on the mixes.
    val likedMusic = remember(accountPlaylists) {
        accountPlaylists?.firstOrNull { item ->
            item.id == "LM" || item.title.equals("Liked Music", ignoreCase = true)
        }
    }
    val libraryMixes = remember(accountPlaylists, likedMusic) {
        accountPlaylists.orEmpty()
            .filterNot { it === likedMusic }
            .distinctBy { it.id }
    }
    val personalMixes = remember(ytMixes) { ytMixes.distinctBy { it.id } }

    val visibleMixes = when (filter) {
        MixFilter.FOR_YOU -> (personalMixes + libraryMixes).distinctBy { it.id }
        MixFilter.FAVOURITES -> libraryMixes
        MixFilter.DISCOVER -> personalMixes
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = GlossyDimens.ScreenPadding,
                end = GlossyDimens.ScreenPadding,
                top = insetsPadding.calculateTopPadding() + 8.dp,
                bottom = insetsPadding.calculateBottomPadding() + GlossyDimens.Section,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "mix_header", span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    GlossyEyebrow(stringResource(R.string.glossy_mix_eyebrow))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.mix_for_you_title),
                        fontSize = 30.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.8).sp,
                        color = GlossyPalette.TextPrimary,
                    )
                }
            }

            item(key = "mix_filters", span = { GridItemSpan(2) }) {
                GlossyChipRow(
                    items = MixFilter.entries.map { it to stringResource(it.labelRes) },
                    currentValue = filter,
                    onValueUpdate = { filter = it },
                    contentPadding = 0.dp,
                )
            }

            val featured = likedMusic?.takeIf { filter != MixFilter.DISCOVER }
            if (featured != null) {
                item(key = "mix_featured", span = { GridItemSpan(2) }) {
                    FeaturedMixCard(
                        item = featured,
                        onClick = { navController.navigate(featured.destinationRoute()) },
                    )
                }
            }

            items(
                items = visibleMixes,
                key = { "mix_${it.id}" },
                span = { GridItemSpan(1) },
            ) { item ->
                GlossyMixCard(
                    title = item.title,
                    subtitle = item.subtitle(),
                    seed = item.id,
                    imageUrl = item.thumbnail,
                    label = stringResource(R.string.glossy_made_for_label),
                    onClick = { navController.navigate(item.destinationRoute()) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    onDismiss = menuState::dismiss,
                                )
                                else -> menuState.dismiss()
                            }
                        }
                    },
                )
            }

            if (visibleMixes.isEmpty() && featured == null) {
                item(key = "mix_empty", span = { GridItemSpan(2) }) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = GlossyPalette.Accent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.glossy_mix_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlossyPalette.TextSecondary,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        )
                    }
                }
            }

            item(key = "mix_footer", span = { GridItemSpan(2) }) {
                Text(
                    text = stringResource(R.string.glossy_mix_evolving),
                    style = MaterialTheme.typography.bodySmall,
                    color = GlossyPalette.TextMuted,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
                )
            }
        }
    }
}

/** Route for a mix card, matching the destinations the rest of the app uses. */
private fun YTItem.destinationRoute(): String = when (this) {
    is PlaylistItem -> if (id == "LM") "auto_playlist/liked" else "online_playlist/$id"
    is AlbumItem -> "album/$browseId"
    else -> "mix"
}

/** The caption under a mix's title: its author/artist, when the feed has one. */
private fun YTItem.subtitle(): String? = when (this) {
    is PlaylistItem -> author?.name
    is AlbumItem -> artists?.joinToString(" · ") { it.name }
    else -> null
}

/**
 * The featured Liked music card: one cover across both columns, a scrim to keep
 * the copy legible and the rose play button of the design.
 */
@Composable
private fun FeaturedMixCard(
    item: YTItem,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(GlossyDimens.CornerHero)
    val accent = GlossyPalette.Accent
    val onAccent = GlossyPalette.OnAccent
    val songCount = (item as? PlaylistItem)?.songCountText

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(shape)
            .background(GlossyPalette.Row)
            .clickable(onClick = onClick),
    ) {
        if (!item.thumbnail.isNullOrBlank()) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            GlossyThumb(
                seed = item.id,
                size = 196.dp,
                corner = GlossyDimens.CornerHero,
                imageUrl = null,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.86f),
                        )
                    )
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 88.dp, bottom = 18.dp),
        ) {
            Text(
                text = stringResource(R.string.glossy_mix_featured),
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(GlossyDimens.CornerPill))
                    .background(Color.Black.copy(alpha = 0.34f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.title,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!songCount.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = songCount,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
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
