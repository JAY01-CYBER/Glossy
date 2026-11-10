/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.jay.glossy.ui.component.liquidGlassSurface
import com.kyant.backdrop.Backdrop
import com.metrolist.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where a row in the title menu leads, and the line printed under its label. */
@Immutable
data class TitleActionTarget(
    val route: String,
    val subtitle: String,
)

/** The destinations available for the track that is playing right now. */
@Immutable
data class TitleActionTargets(
    val playlist: TitleActionTarget? = null,
    val album: TitleActionTarget? = null,
    val artist: TitleActionTarget? = null,
)

/**
 * Works out what "Go to playlist / album / artist" should open for the current
 * track.
 *
 * The queue's own route is authoritative when it has one, because that is the
 * playlist the listener actually started. Queues restored from disk or started
 * from a song's menu carry no route, so the track's library membership is
 * consulted next: the first playlist holding it, then Liked Music and
 * Downloads. That is deliberately not the playback queue — a row that promises
 * a playlist must never open the queue instead.
 */
@Composable
fun rememberTitleActionTargets(
    mediaMetadata: MediaMetadata?,
    queueTitle: String?,
    queueSourceRoute: String?,
): TitleActionTargets {
    val playerConnection = LocalPlayerConnection.current
    val queueLabel = stringResource(R.string.playback_queue)
    val likedLabel = stringResource(R.string.liked_songs)
    val downloadedLabel = stringResource(R.string.downloaded_songs)

    var playlistTarget by remember { mutableStateOf<TitleActionTarget?>(null) }

    LaunchedEffect(mediaMetadata?.id, queueTitle, queueSourceRoute) {
        // Clear first: while the library lookup runs, the previous track's
        // playlist must not stay on screen as a tappable destination.
        playlistTarget = null

        // Only playlist routes qualify. A queue that somehow carries its own
        // screen route must never make this row open the playback queue.
        val route =
            queueSourceRoute
                ?.takeIf { it.isNotBlank() }
                ?.takeIf {
                    it.startsWith("local_playlist/") ||
                        it.startsWith("online_playlist/") ||
                        it.startsWith("auto_playlist/")
                }
        if (route != null) {
            playlistTarget = TitleActionTarget(
                route = route,
                subtitle = queueTitle?.takeIf { it.isNotBlank() } ?: queueLabel,
            )
            return@LaunchedEffect
        }

        val songId = mediaMetadata?.id
        if (songId == null || playerConnection == null) {
            playlistTarget = null
            return@LaunchedEffect
        }

        playlistTarget = withContext(Dispatchers.IO) {
            val database = playerConnection.database
            val owning = database.playlistSongMaps(songId)
                .asSequence()
                .mapNotNull { map -> database.playlistBlocking(map.playlistId)?.playlist }
                .firstOrNull()
            if (owning != null) {
                // A saved online playlist routes by its YouTube id; a local one
                // has only the generated row id, which is its route argument.
                val owningRoute = owning.browseId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { "online_playlist/$it" }
                    ?: "local_playlist/${owning.id}"
                return@withContext TitleActionTarget(owningRoute, owning.name)
            }

            val song = database.getSongByIdBlocking(songId)?.song
            when {
                song?.liked == true -> TitleActionTarget("auto_playlist/liked", likedLabel)
                song?.isDownloaded == true -> TitleActionTarget("auto_playlist/downloaded", downloadedLabel)
                else -> null
            }
        }
    }

    val album = mediaMetadata?.album
        ?.takeIf { it.id.isNotBlank() }
        ?.let { currentAlbum ->
            val route =
                if (currentAlbum.id.startsWith("MPREb_")) {
                    "album/${currentAlbum.id}"
                } else {
                    // Podcast shows reuse the album slot but live on their own page.
                    "online_podcast/${currentAlbum.id}"
                }
            TitleActionTarget(route, currentAlbum.title)
        }

    // Captured here because the smart cast on the caller's nullable argument
    // does not reach into the lambda below.
    val artists = mediaMetadata?.artists.orEmpty()
    val artist = artists.firstNotNullOfOrNull { primary ->
        val id = primary.id?.takeIf { it.isNotBlank() } ?: return@firstNotNullOfOrNull null
        TitleActionTarget(
            route = "artist/$id",
            subtitle = artists.joinToString { it.name },
        )
    }

    return TitleActionTargets(
        playlist = playlistTarget,
        album = album,
        artist = artist,
    )
}

/**
 * The title-tap menu, floating over the player the way the Apple Music player
 * does it: a rounded glass sheet with the playlists, albums and artists the
 * current track belongs to.
 *
 * It is drawn as an overlay in the player's own Box rather than a popup window
 * so the liquid-glass modifier can sample the same composition's backdrop.
 * Tapping anywhere outside the card dismisses it; taps on the card are
 * swallowed so they do not fall through to the controls behind it.
 *
 * Every player design draws its own controls, so each one hosts this overlay
 * and supplies the inset that parks the card just above its title row.
 */
@Composable
fun TitleActionsOverlay(
    visible: Boolean,
    targets: TitleActionTargets,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
    bottomInset: Dp,
    glassBackdrop: Backdrop?,
    modifier: Modifier = Modifier,
    gutter: Dp = 20.dp,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(140)) +
            scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(180),
                transformOrigin = TransformOrigin(0f, 1f),
            ),
        exit = fadeOut(animationSpec = tween(120)) +
            scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(140),
                transformOrigin = TransformOrigin(0f, 1f),
            ),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            val shape = RoundedCornerShape(28.dp)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = gutter, end = 48.dp, bottom = bottomInset)
                    .widthIn(min = 240.dp, max = 300.dp)
                    .clip(shape)
                    .then(
                        if (glassBackdrop != null) {
                            Modifier.liquidGlassSurface(
                                backdrop = glassBackdrop,
                                shape = shape,
                                tint = Color.Black.copy(alpha = 0.34f),
                            )
                        } else {
                            Modifier
                                .background(Color(0xF21C1C1E))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(vertical = 6.dp),
            ) {
                targets.playlist?.let { target ->
                    TitleActionRow(
                        icon = R.drawable.queue_music,
                        title = stringResource(R.string.go_to_playlist),
                        subtitle = target.subtitle,
                        onClick = { onNavigate(target.route) },
                    )
                }
                targets.album?.let { target ->
                    TitleActionRow(
                        icon = R.drawable.album,
                        title = stringResource(R.string.go_to_album),
                        subtitle = target.subtitle,
                        onClick = { onNavigate(target.route) },
                    )
                }
                targets.artist?.let { target ->
                    TitleActionRow(
                        icon = R.drawable.artist,
                        title = stringResource(R.string.go_to_artist),
                        subtitle = target.subtitle,
                        onClick = { onNavigate(target.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleActionRow(
    icon: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
