/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The playlist reel: the artwork as a full-bleed collage, the title and owner
 * centred on the page below it, then Play flanked by shuffle and download.
 *
 * The local playlist and the Spotify import both draw this header, so the two
 * screens cannot drift apart. Anything the header cannot decide alone — what
 * the pencil edits, what the download button shows, whether the owner opens an
 * artist — arrives as a caller's lambda.
 */
package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.toBitmap
import com.jay.glossy.R

/** Height of the reel itself, before the title block. */
private val ReelHeight = 286.dp

/** Smoke on the artwork, so the floating controls keep their contrast. */
private val ReelGlass = Color.Black.copy(alpha = 0.34f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistReelHeader(
    title: String,
    owner: String,
    subtitle: String,
    thumbnails: List<String?>,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOverflow: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
    downloadButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    onEditCover: (() -> Unit)? = null,
    onOwnerClick: (() -> Unit)? = null,
    downloadActive: Boolean = false,
    statsText: String? = null,
    extraToolbarActions: (@Composable () -> Unit)? = null,
    onArtworkLoaded: ((ImageBitmap) -> Unit)? = null,
) {
    val typography = MaterialTheme.typography
    val titleStyle = remember(typography) {
        typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    }
    val ownerStyle = remember(typography) {
        typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
    }
    val playStyle = remember(typography) {
        typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    }
    // Read the colour here: MaterialTheme is only readable from a composable
    // context, not from inside the remember calculation.
    val background = MaterialTheme.colorScheme.background
    // The scrim sits under the scrolling list, so it must not be rebuilt on
    // every recomposition the player state triggers.
    val scrim = remember(background) {
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.38f),
            0.55f to Color.Black.copy(alpha = 0.08f),
            1f to background.copy(alpha = 0.62f),
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(ReelHeight),
        ) {
            PlaylistReelArtwork(
                thumbnails = thumbnails,
                onArtworkLoaded = onArtworkLoaded,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    color = ReelGlass,
                    contentColor = Color.White,
                    modifier = Modifier.size(width = 58.dp, height = 44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Surface(shape = CircleShape, color = ReelGlass, contentColor = Color.White) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        extraToolbarActions?.invoke()
                        ReelToolbarButton(onClick = onSearch) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        ReelToolbarButton(onClick = onOverflow) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            if (editable && onEditCover != null) {
                // Half above the seam, half below it: the pencil marks the edge
                // of the reel instead of covering the artwork in its middle.
                Surface(
                    onClick = onEditCover,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 19.dp)
                        .size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = owner,
                style = ownerStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onOwnerClick != null) { onOwnerClick?.invoke() },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReelRoundButton(onClick = onShuffle) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }

            Surface(
                onClick = onPlay,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f).height(50.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.play),
                        style = playStyle,
                    )
                }
            }

            Surface(
                onClick = onDownload,
                shape = CircleShape,
                color = if (downloadActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (downloadActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(50.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { downloadButton() }
            }
        }

        if (!statsText.isNullOrBlank()) {
            Text(
                text = statsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp),
            )
        }
    }
}

@Composable
private fun ReelToolbarButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelRoundButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(50.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * Up to four covers, one pane each. A single-cover playlist keeps the same
 * reel shape as a wide image rather than a framed square, so both playlist
 * kinds start on the same line.
 */
@Composable
private fun PlaylistReelArtwork(
    thumbnails: List<String?>,
    modifier: Modifier = Modifier,
    onArtworkLoaded: ((ImageBitmap) -> Unit)? = null,
) {
    val images = remember(thumbnails) { thumbnails.filterNotNull().distinct().take(4) }

    if (images.isEmpty()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
        }
        return
    }

    if (images.size == 1) {
        AsyncImage(
            model = images.first(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onSuccess = { state -> onArtworkLoaded?.invoke(state.result.image.toBitmap().asImageBitmap()) },
            modifier = modifier,
        )
        return
    }

    val rows = if (images.size <= 2) {
        listOf(images)
    } else {
        listOf(images.take(2), listOfNotNull(images[2], images.getOrNull(3) ?: images[0]))
    }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                row.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onSuccess = { state ->
                            onArtworkLoaded?.invoke(state.result.image.toBitmap().asImageBitmap())
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}
