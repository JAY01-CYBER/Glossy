/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.artist

import com.jay.glossy.R

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jay.glossy.upcoming.ReleaseType
import com.jay.glossy.upcoming.UpcomingRelease
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The announced-release card that sits above Top Songs.
 *
 * The card is deliberately not a shelf: there is at most one next release, so
 * it reads as a single statement with an action, and disappears entirely when
 * the artist has nothing announced.
 */
@Composable
fun UpcomingReleaseCard(
    release: UpcomingRelease,
    fallbackArtworkUrl: String?,
    isReminded: Boolean,
    onOpen: () -> Unit,
    onToggleReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)
    val date = release.releaseDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    val typeLabel = stringResource(release.type.labelRes())
    val subtitle =
        release.trackCount?.let { count ->
            "${pluralStringResource(R.plurals.upcoming_release_song_count, count, count)} · $typeLabel"
        } ?: typeLabel

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .background(
                    Brush.linearGradient(
                        colors = listOf(accent.copy(alpha = 0.075f), Color.Transparent),
                    ),
                )
                .border(1.dp, accent.copy(alpha = 0.16f), shape)
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            // The archive cover is drawn over the artist image, so a release
            // without artwork still gets a sensible square while it loads.
            if (!fallbackArtworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = fallbackArtworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            AsyncImage(
                model = release.coverArtUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(5.dp)
                            .background(accent, CircleShape),
                )
                Text(
                    text = stringResource(R.string.upcoming_release_coming, date).uppercase(Locale.getDefault()),
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.35.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = release.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isReminded) accent else accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.3f), CircleShape)
                    .clickable(role = Role.Button, onClick = onToggleReminder),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isReminded) R.drawable.check else R.drawable.add),
                contentDescription =
                    stringResource(
                        if (isReminded) {
                            R.string.upcoming_release_remove_reminder
                        } else {
                            R.string.upcoming_release_save
                        },
                    ),
                tint = if (isReminded) MaterialTheme.colorScheme.onPrimary else accent,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@StringRes
private fun ReleaseType.labelRes(): Int =
    when (this) {
        ReleaseType.ALBUM -> R.string.upcoming_release_type_album
        ReleaseType.EP -> R.string.upcoming_release_type_ep
        ReleaseType.SINGLE -> R.string.upcoming_release_type_single
    }
