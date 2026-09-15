/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player.applemusic

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jay.glossy.R
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.CropAlbumArtKey
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.menu.PlayerMenu
import com.jay.glossy.ui.utils.ShowMediaInfo
import com.jay.glossy.utils.rememberPreference

@Composable
fun NowPlayingContentAppleMusic(
    bottomSheetState: BottomSheetState,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    
    var viewState by rememberSaveable { mutableStateOf(AppleMusicView.MAIN) }
    val typography = rememberAppleMusicTypography()
    val localDensity = LocalDensity.current
    
    val seedColor = MaterialTheme.colorScheme.primary
    val activePillContainer = remember(seedColor) { Color.White.copy(alpha = 0.2f) }
    val activePillContent = remember(seedColor) { Color.White }

    val backdropBrush = remember(seedColor) {
        Brush.verticalGradient(
            0f to appleMusicGradientColorAt(seedColor, 0f),
            0.48f to appleMusicGradientColorAt(seedColor, 0.48f),
            1f to appleMusicGradientColorAt(seedColor, 1f),
        )
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.matchParentSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaMetadata?.thumbnailUrl)
                    .crossfade(500)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(80.dp)
            )
            Box(modifier = Modifier.fillMaxSize().alpha(0.62f).background(backdropBrush))
        }

        Crossfade(targetState = viewState, animationSpec = tween(300), label = "AppleMusicView") { view ->
            when (view) {
                AppleMusicView.MAIN -> AppleMusicMainView(
                    viewState = view,
                    onSelectView = { viewState = it },
                    activePillContainer = activePillContainer,
                    activePillContent = activePillContent,
                    typography = typography,
                    bottomSheetState = bottomSheetState,
                    position = position,
                    duration = duration
                )
                AppleMusicView.LYRICS -> AppleMusicLyricsView(
                    viewState = view,
                    onSelectView = { viewState = it },
                    activePillContainer = activePillContainer,
                    activePillContent = activePillContent,
                    typography = typography,
                    position = position,
                    duration = duration
                )
                AppleMusicView.QUEUE -> AppleMusicQueueView(
                    viewState = view,
                    onSelectView = { viewState = it },
                    activePillContainer = activePillContainer,
                    activePillContent = activePillContent,
                    typography = typography,
                    bottomSheetState = bottomSheetState,
                    position = position,
                    duration = duration
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() })
                .size(width = 64.dp, height = 28.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { bottomSheetState.collapseSoft() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun AppleMusicMainView(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    typography: AppleMusicTypography,
    bottomSheetState: BottomSheetState,
    position: Long,
    duration: Long
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 32.dp, start = 32.dp, end = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaMetadata?.thumbnailUrl)
                    .crossfade(550)
                    .build(),
                contentDescription = null,
                contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .appleMusicVerticalFadeEdges(topFade = 0.dp, bottomFade = 30.dp)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(20.dp))
            AppleMusicMainTitleRow(typography = typography, bottomSheetState = bottomSheetState)
            Spacer(modifier = Modifier.height(16.dp))
            AppleMusicBottomCluster(
                viewState = viewState,
                onSelectView = onSelectView,
                lyricsAvailable = true, 
                activeColor = activePillContainer,
                activeContentColor = activePillContent,
                position = position,
                duration = duration
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppleMusicMainTitleRow(
    typography: AppleMusicTypography,
    bottomSheetState: BottomSheetState
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata?.title ?: "",
                style = typography.mainTitle,
                maxLines = 1,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mediaMetadata?.explicit == true) {
                    Icon(
                        painter = painterResource(R.drawable.explicit), 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(20.dp).padding(end = 4.dp)
                    )
                }
                Text(
                    text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                    style = typography.mainArtist,
                    maxLines = 1,
                    color = AppleMusicTextSecondary,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AppleMusicHeaderActions(bottomSheetState = bottomSheetState)
    }
}

@Composable
internal fun AppleMusicHeaderActions(
    bottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    
    val isEpisode = currentSong?.song?.isEpisode == true
    val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .appleMusicPressInflate()
                .size(32.dp)
                .clip(CircleShape)
                .clickable { playerConnection.toggleLike() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                contentDescription = null,
                tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        
        AppleMusicGlyphButton(
            icon = R.drawable.more_vert, 
            onClick = {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = mediaMetadata,
                        playerBottomSheetState = bottomSheetState,
                        onShowDetailsDialog = {
                            mediaMetadata?.id?.let {
                                bottomSheetPageState.show { ShowMediaInfo(it) }
                            }
                        },
                        onDismiss = menuState::dismiss
                    )
                }
            }
        )
    }
}

@Composable
internal fun AppleMusicCompactHeader(
    typography: AppleMusicTypography,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(mediaMetadata?.thumbnailUrl)
                .crossfade(300)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(55.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata?.title ?: "",
                style = typography.compactTitle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (mediaMetadata?.explicit == true) {
                    Icon(
                        painter = painterResource(R.drawable.explicit), 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                }
                Text(
                    text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                    style = typography.compactArtist,
                    color = AppleMusicTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
