/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The experimental SeaGlass mini player: a sand-frosted glass bar whose body
 * refracts the current artwork instead of painting a flat themed surface.
 *
 * It is selectable, never automatic — [MiniPlayerStyle.SEAGLASS] is only ever
 * reached because the user picked it in Appearance.
 *
 * The bar has no access to the screen behind it, so it carries its own stage:
 * the artwork is drawn once into a GPU layer by [Modifier.seaGlassBackdrop] and
 * the bar surface refracts that layer. Because the stage lives inside this
 * composable, nothing is captured while any other mini player style is active.
 */
package com.jay.glossy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.jay.glossy.ui.component.GlossyThumb
import com.jay.glossy.utils.joinToArtistString
import dev.seaglass.compose.SeaGlassContentRole
import dev.seaglass.compose.rememberSeaGlassStageState
import dev.seaglass.compose.seaGlass
import dev.seaglass.compose.seaGlassBackdrop
import dev.seaglass.core.SeaGlassMaterial

/** Height of the bar and of the artwork layer captured behind it. */
private val BarHeight = 66.dp

/**
 * Deep smoked slate with a heavy sand frost: the artwork reads through the
 * body as colour, not as detail, so the white title and controls keep their
 * contrast no matter how bright the cover is.
 */
private val MiniBarMaterial = SeaGlassMaterial(
    tintColorArgb = 0xFF10131C.toInt(),
    tintDensity = 0.62f,
    coarseness = 0.34f,
    frostRadiusDp = 30f,
    rimChalkiness = 0.72f,
    thicknessDp = 18f,
    transparency = 0.40f,
    cornerRadiusDp = 18f,
)

/**
 * The SeaGlass variant of the mini player.
 *
 * Keeps the same contract as [NewMiniPlayer] — [ProgressState], a modifier and
 * a click-to-expand callback — so it drops into the same call sites.
 */
@Composable
fun SeaGlassMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()

    val stageState = rememberSeaGlassStageState()
    val artistText = mediaMetadata?.artists?.joinToArtistString(", ") { it.name }.orEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight),
    ) {
        // Recorded into the stage once per frame; the bar renders the frosted
        // copy of it rather than the original.
        Box(
            modifier = Modifier
                .matchParentSize()
                .seaGlassBackdrop(stageState),
        ) {
            val artworkUrl = mediaMetadata?.thumbnailUrl
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                )
            }
        }

        Row(
            modifier = Modifier
                .matchParentSize()
                .seaGlass(
                    stageState = stageState,
                    material = MiniBarMaterial,
                    // Chrome, not text: the bar stretches its top and bottom
                    // edges and holds the interior steady for the labels.
                    contentRole = SeaGlassContentRole.NAVIGATION_CHROME,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlossyThumb(
                seed = mediaMetadata?.id,
                size = 48.dp,
                corner = 12.dp,
                imageUrl = mediaMetadata?.thumbnailUrl,
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mediaMetadata?.title.orEmpty(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artistText.isNotBlank()) {
                    Text(
                        text = artistText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            if (canSkipNext) {
                IconButton(onClick = { playerConnection.seekToNext() }) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            IconButton(onClick = { playerConnection.togglePlayPause() }) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Hairline progress, right-aligned against the transport controls so
            // it reads as a scrub position rather than a separator.
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .padding(vertical = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(progressState.progress)
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.85f)),
                )
            }
        }
    }
}
