/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player.applemusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jay.glossy.LocalDatabase
import com.jay.glossy.R
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.db.entities.LyricsEntity
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.Lyrics
import com.jay.glossy.ui.component.PlayStoreRefreshIndicator
import com.jay.glossy.ui.utils.ShowOffsetDialog
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppleMusicLyricsView(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    typography: AppleMusicTypography,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val localDensity = LocalDensity.current
    
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }

    var showCluster by rememberSaveable { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    
    val refreshState = rememberPullToRefreshState()
    
    // Auto-hide after 5 seconds
    LaunchedEffect(showCluster, interactionTick) {
        if (showCluster) {
            delay(5000L)
            showCluster = false
        }
    }

    // LYRICS FETCH LOGIC
    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.jay.glossy.di.LyricsHelperEntryPoint::class.java,
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata!!)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata!!.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (e: Exception) {
                    // Handle error silently
                }
            }
        }
    }

    // Scroll direction: scroll down -> hide controls, swipe up -> show controls
    val scrollWakesControls = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < 0f) {
                    showCluster = false
                } else if (available.y > 0f) {
                    showCluster = true
                    interactionTick++
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier.height(
                with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() } + 20.dp,
            ),
        )
        
        // Compact header with the three dots menu button on the right
        AppleMusicCompactHeader(
            typography = typography,
            trailingContent = {
                IconButton(
                    onClick = {
                        menuState.show {
                            com.jay.glossy.ui.menu.LyricsMenu(
                                lyricsProvider = { currentLyrics },
                                songProvider = { currentSong?.song },
                                mediaMetadataProvider = { mediaMetadata!! },
                                onDismiss = menuState::dismiss,
                                onShowOffsetDialog = {
                                    bottomSheetPageState.show {
                                        ShowOffsetDialog(songProvider = { currentSong?.song })
                                    }
                                },
                            )
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = "Lyrics Menu",
                        tint = Color.White
                    )
                }
            },
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                showCluster = !showCluster
                interactionTick++
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            // Premium frosted-glass lyrics panel: blurred artwork backdrop + dark scrim.
            val lyricsArtworkUrl = remember(mediaMetadata?.thumbnailUrl) {
                mediaMetadata?.thumbnailUrl?.toHighRes()
            }
            val lyricsPanelShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(lyricsPanelShape)
            ) {
                if (lyricsArtworkUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(lyricsArtworkUrl)
                            .crossfade(400)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                scaleX = 1.5f
                                scaleY = 1.5f
                            }
                            .blur(72.dp)
                            .alpha(0.6f),
                    )
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.55f),
                                ),
                            ),
                        ),
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .border(1.dp, Color.White.copy(alpha = 0.10f), lyricsPanelShape),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollWakesControls)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showCluster = !showCluster
                        interactionTick++
                    },
                contentAlignment = Alignment.Center
            ) {
            when {
                lyrics == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PlayStoreRefreshIndicator(
                            isRefreshing = true,
                            state = refreshState,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.lyrics_not_found),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {
                    val positionProvider = remember { { playerConnection.player.currentPosition } }
                    ProvideTextStyle(
                        value = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    ) {
                        Lyrics(
                            sliderPositionProvider = positionProvider,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                                .appleMusicVerticalFadeEdges(topFade = 28.dp, bottomFade = 18.dp),
                            showLyrics = true,
                        )
                    }
                }
            }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showCluster,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
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
