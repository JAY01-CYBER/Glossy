package com.jay.glossy.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jay.glossy.R
import com.jay.glossy.spotifycore.models.SpotifyTrack
import com.jay.glossy.spotifycore.SpotifyMapper
import com.jay.glossy.utils.joinByBullet
import com.jay.glossy.utils.makeTimeString

@Composable
fun SpotifyTrackListItem(
    track: SpotifyTrack,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (track.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    showSongIconPlaceholder: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val duration = track.durationMs
        .takeIf { it > 0 }
        ?.toLong()
        ?.let(::makeTimeString)
    val subtitle = joinByBullet(
        track.artists.joinToString { it.name },
        duration,
    )

    ListItem(
        title = track.name,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = SpotifyMapper.getTrackThumbnail(track),
                albumIndex = albumIndex,
                isSelected = isSelected,
                isActive = isActive,
                isPlaying = isPlaying,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(54.dp)
            )
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive,
    )
}
