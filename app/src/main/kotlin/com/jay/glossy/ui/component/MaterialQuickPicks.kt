/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The home feed's "Quick picks" shelf, drawn art-forward: a large square of
 * artwork with the title and the artist captioned underneath it.
 *
 * The shelf has been through a card stack and a compact icon list. This is the
 * third shape and the one that was picked: the artwork is the shelf — as big as
 * a column of the feed allows — and everything else is a caption under it. It
 * carries no container of its own, no card fill, no border and no divider,
 * because a box around every pick is a box drawn inside a section drawn inside
 * the feed, which is what made the old shelf read as a separate widget.
 *
 * Two tiles share a row, so the artwork is a little under half the screen wide
 * and square; the shelf is two rows deep because that is all the height an
 * artwork this size can ask for before it becomes a screen of its own. The rest
 * of the picks are a swipe away — the grid scrolls horizontally and snaps to a
 * page.
 *
 * There is deliberately no overflow button: at half the screen a ⋮ costs the
 * title a third of its width, so the whole tile plays and the menu is on
 * long-press — the same gesture the tiles around it use.
 */

package com.jay.glossy.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jay.glossy.constants.QuickPickCaptionHeight
import com.jay.glossy.db.entities.Song
import com.jay.glossy.ui.utils.resize
import com.jay.glossy.utils.joinToArtistString

/**
 * Corners on the artwork. Soft enough to read as a sleeve, tight enough that
 * the caption underneath still lines up with it rather than drifting.
 */
private val QuickPickArtShape = RoundedCornerShape(16.dp)

/**
 * One quick pick: the artwork, then the title over the artist underneath it.
 *
 * The tile is exactly `width + QuickPickCaptionHeight` tall — the caller sizes
 * its grid rows from the same two numbers — so the artwork stays square and the
 * captions of every pick in a row sit on one line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaterialQuickPickTile(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Square, and as wide as the column the shelf gives it: the
                // artwork is the shelf, so it takes the whole tile width rather
                // than being inset into it.
                .aspectRatio(1f),
        ) {
            ItemThumbnail(
                // Two hundred was right for a 46dp icon; at this size it was
                // being upscaled, which is exactly the softness a shelf this
                // large makes obvious.
                thumbnailUrl = song.song.thumbnailUrl?.resize(400, 400),
                isActive = isActive,
                isPlaying = isPlaying,
                shape = QuickPickArtShape,
            )
        }
        // The caption block is a fixed height — the caller sizes the grid rows
        // with it — so a long title stays on one line and the artist keeps its
        // own row instead of one pick in a row drifting out of line.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(QuickPickCaptionHeight)
                .padding(top = 8.dp),
        ) {
            Text(
                text = song.song.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.orderedArtists.joinToArtistString(" & ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
