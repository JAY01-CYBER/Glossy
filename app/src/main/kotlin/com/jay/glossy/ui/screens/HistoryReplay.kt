/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jay.glossy.R
import com.jay.glossy.db.entities.Album
import com.jay.glossy.db.entities.Artist
import com.jay.glossy.db.entities.Song
import com.jay.glossy.viewmodels.ReplayMonth
import com.jay.glossy.viewmodels.ReplaySong
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * The monthly Replay that opens the local history.
 *
 * It deliberately reads as one self-contained block rather than another
 * filter row: a single listening number, then the artists, songs and albums
 * that made that month. The day-by-day log continues underneath.
 */
@Composable
fun HistoryReplaySection(
    state: ReplayMonth,
    years: List<Int>,
    onMonthSelected: (Month) -> Unit,
    onYearSelected: (Int) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSongClick: (index: Int) -> Unit,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ReplayHero(
            state = state,
            years = years,
            onMonthSelected = onMonthSelected,
            onYearSelected = onYearSelected,
        )

        if (state.topArtists.isNotEmpty()) {
            ReplaySectionTitle(R.string.replay_top_artists)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.topArtists,
                    key = { artist -> "replay_artist_${artist.id}" },
                ) { artist ->
                    TopArtistCard(
                        rank = state.topArtists.indexOf(artist) + 1,
                        artist = artist,
                        onClick = { onArtistClick(artist) },
                    )
                }
            }
        }

        if (state.topSongs.isNotEmpty()) {
            ReplaySectionTitle(R.string.replay_top_songs)
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                state.topSongs.forEachIndexed { index, replaySong ->
                    TopSongRow(
                        rank = index + 1,
                        replaySong = replaySong,
                        onClick = { onSongClick(index) },
                        onLongClick = { onSongLongClick(replaySong.song) },
                    )
                }
            }
        }

        if (state.topAlbums.isNotEmpty()) {
            ReplaySectionTitle(R.string.replay_top_albums)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.topAlbums,
                    key = { album -> "replay_album_${album.id}" },
                ) { album ->
                    TopAlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplayHero(
    state: ReplayMonth,
    years: List<Int>,
    onMonthSelected: (Month) -> Unit,
    onYearSelected: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val artwork =
        remember(state.topArtists, state.topSongs, state.topAlbums) {
            state.topArtists
                .firstOrNull()
                ?.thumbnailUrl
                ?.takeIf { it.isNotBlank() }
                ?: state.topSongs
                    .firstOrNull()
                    ?.song
                    ?.thumbnailUrl
                    ?.takeIf { it.isNotBlank() }
                ?: state.topAlbums
                    .firstOrNull()
                    ?.thumbnailUrl
                    ?.takeIf { it.isNotBlank() }
        }
    val monthName = state.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = shape,
                ),
    ) {
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .matchParentSize()
                        .blur(38.dp)
                        .alpha(0.55f),
            )
        }

        // The artwork is only a tint; this wash keeps the text legible on both
        // dark and light themes and lets the card dissolve into the page.
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops =
                                arrayOf(
                                    0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
                                    0.52f to MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                    1f to MaterialTheme.colorScheme.surfaceContainer,
                                ),
                        ),
                    ),
        )

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 14.dp, top = 18.dp),
            ) {
                Text(
                    text = stringResource(R.string.replay),
                    color = MaterialTheme.colorScheme.onSurface,
                    style =
                        MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.6).sp,
                        ),
                )
                Spacer(Modifier.weight(1f))
                ReplayYearPicker(
                    selectedYear = state.yearMonth.year,
                    years = years,
                    onYearSelected = onYearSelected,
                )
            }

            Spacer(Modifier.height(18.dp))

            ReplayMonthRail(
                selectedMonth = state.yearMonth,
                onMonthSelected = onMonthSelected,
            )

            Spacer(Modifier.height(18.dp))

            if (state.hasPlays) {
                ReplayStatBlock(
                    state = state,
                    monthName = monthName,
                )
            } else {
                Text(
                    text = stringResource(R.string.replay_no_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                )
            }

            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun ReplayMonthRail(
    selectedMonth: java.time.YearMonth,
    onMonthSelected: (Month) -> Unit,
) {
    val today = remember { LocalDate.now() }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(
            items = Month.entries,
            key = { month -> "replay_month_${month.value}" },
        ) { month ->
            val enabled =
                selectedMonth.year != today.year ||
                    month.value <= today.monthValue
            val isSelected = selectedMonth.month == month

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .alpha(if (enabled) 1f else 0.35f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = enabled) { onMonthSelected(month) }
                        .padding(vertical = 3.dp),
            ) {
                Text(
                    text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier =
                        Modifier
                            .height(3.dp)
                            .width(16.dp)
                            .background(
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                shape = CircleShape,
                            ),
                )
            }
        }
    }
}

@Composable
private fun ReplayYearPicker(
    selectedYear: Int,
    years: List<Int>,
    onYearSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = CircleShape,
                    ).clickable { expanded = true }
                    .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Text(
                text = selectedYear.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(5.dp))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = year.toString(),
                            fontWeight =
                                if (year == selectedYear) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                            color =
                                if (year == selectedYear) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    },
                    onClick = {
                        expanded = false
                        onYearSelected(year)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReplayStatBlock(
    state: ReplayMonth,
    monthName: String,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val songs = pluralStringResource(R.plurals.n_song, state.uniqueSongs, state.uniqueSongs)
    val artists = pluralStringResource(R.plurals.n_artist, state.uniqueArtists, state.uniqueArtists)
    val albums = pluralStringResource(R.plurals.n_album, state.uniqueAlbums, state.uniqueAlbums)

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.replay_listened_for),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = numberFormat.format(state.totalMinutes),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
            )
        }
        Text(
            text = stringResource(R.string.replay_minutes_in_month, monthName),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "$songs · $artists · $albums",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ReplaySectionTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
    )
}

@Composable
private fun TopArtistCard(
    rank: Int,
    artist: Artist,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val minutes = ((artist.timeListened ?: 0) / 60_000).toLong()

    Box(
        modifier =
            Modifier
                .width(164.dp)
                .height(224.dp)
                .clip(shape)
                .clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = artist.artist.name.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops =
                                arrayOf(
                                    0f to Color.Black.copy(alpha = 0.26f),
                                    0.42f to Color.Transparent,
                                    0.62f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.78f),
                                ),
                        ),
                    ),
        )
        Text(
            text = rank.toString(),
            color = Color.White,
            style =
                MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                    shadow =
                        Shadow(
                            color = Color.Black.copy(alpha = 0.45f),
                            blurRadius = 14f,
                        ),
                ),
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 13.dp, top = 6.dp),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
        ) {
            Text(
                text = artist.artist.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.minute, minutes.toInt(), minutes),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopSongRow(
    rank: Int,
    replaySong: ReplaySong,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val song = replaySong.song
    val minutes = replaySong.playTimeMs / 60_000L

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Text(
            text = rank.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.song.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString { it.name },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.replay_min_short, minutes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TopAlbumCard(
    album: Album,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(124.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .size(124.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = album.album.title.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            AsyncImage(
                model = album.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Text(
            text = album.album.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
        )
        Text(
            text = album.artists.joinToString { it.name },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp, bottom = 4.dp),
        )
    }
}

/**
 * Marks where the Replay block ends and the untouched day-by-day history
 * begins, so the two scales of listening never read as one list.
 */
@Composable
fun RecentlyPlayedLabel() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.replay_recently_played).uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f),
        )
    }
}
