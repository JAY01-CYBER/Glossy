/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player.applemusic

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.glossy.R
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.db.entities.LyricsEntity
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.Lyrics
import com.jay.glossy.ui.component.LyricsColorPickerDialog
import com.jay.glossy.ui.component.LyricsShareDialog
import com.jay.glossy.ui.component.PlayStoreRefreshIndicator
import com.jay.glossy.ui.screens.settings.LyricsPosition
import com.jay.glossy.ui.utils.ShowOffsetDialog
import kotlinx.coroutines.delay

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

    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }

    var showCluster by rememberSaveable { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    
    var showShareDialog by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    
    // For PlayStoreRefreshIndicator
    val refreshState = rememberPullToRefreshState()
    
    LaunchedEffect(showCluster, interactionTick) {
        if (showCluster) {
            delay(8000L)
            showCluster = false
        }
    }

    val scrollWakesControls = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y != 0f) {
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
        
        AppleMusicCompactHeader(
            typography = typography,
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
                    // NEW PLAY STORE REFRESH INDICATOR IN CENTER
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PlayStoreRefreshIndicator(
                            isRefreshing = true,
                            state = refreshState,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                    Text(
                        text = stringResource(R.string.lyrics_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
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

                    Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                        AnimatedVisibility(
                            visible = showCluster,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(end = 20.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AppleMusicFloatingCircleButton(
                                    icon = R.drawable.more_horiz,
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
                                    }
                                )

                                AppleMusicFloatingCircleButton(
                                    icon = R.drawable.share,
                                    onClick = { showShareDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
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

    if (showShareDialog) {
        LyricsShareDialog(
            txt = lyrics ?: "",
            title = mediaMetadata?.title ?: "",
            arts = mediaMetadata?.artists?.joinToString { it.name } ?: "",
            songId = mediaMetadata?.id ?: "",
            onDismiss = { showShareDialog = false },
            onShareAsImage = {
                showShareDialog = false
                showColorPicker = true
            }
        )
    }

    if (showColorPicker) {
        LyricsColorPickerDialog(
            txt = lyrics ?: "",
            title = mediaMetadata?.title ?: "",
            arts = mediaMetadata?.artists?.joinToString { it.name } ?: "",
            thumbnailUrl = mediaMetadata?.thumbnailUrl,
            lyricsTextPosition = LyricsPosition.CENTER,
            onDismiss = { showColorPicker = false },
            onShare = { backgroundColor, textColor, secondaryTextColor, style ->
                showColorPicker = false
            }
        )
    }
}

@Composable
private fun AppleMusicFloatingCircleButton(
    icon: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .appleMusicPressInflate()
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.24f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}
