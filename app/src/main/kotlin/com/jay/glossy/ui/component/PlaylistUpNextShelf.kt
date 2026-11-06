/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The sideways shelf under a playlist header: the tracks that follow whatever
 * is playing, one row tall. Used by the local playlist and by the Spotify
 * import, so both kinds of playlist answer "what next" the same way.
 */
package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jay.glossy.R

/**
 * One card in the shelf, whichever order it was read from.
 *
 * A queued track carries [queueItemIndex], so tapping it jumps inside the
 * player's own queue (which is where a shuffle order exists). A track read
 * from the playlist instead carries [playlistIndex], so tapping it starts the
 * playlist from that position.
 */
data class PlaylistUpNextCard(
    val key: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val queueItemIndex: Int? = null,
    val playlistIndex: Int? = null,
)

@Composable
fun PlaylistUpNextShelf(
    cards: List<PlaylistUpNextCard>,
    onPlayAt: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSeekTo: ((Int) -> Unit)? = null,
) {
    if (cards.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.queue_playing_next).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(cards, key = { it.key }) { card ->
                Column(
                    modifier = Modifier
                        .width(96.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            card.queueItemIndex?.let { index -> onSeekTo?.invoke(index) }
                                ?: card.playlistIndex?.let(onPlayAt)
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        AsyncImage(
                            model = card.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = card.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
