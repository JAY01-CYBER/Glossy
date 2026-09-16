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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.jay.glossy.R
import com.jay.glossy.ui.component.Lyrics
import kotlinx.coroutines.delay

// Note: Replace these import paths if NowPlayingContentState/Actions are in a different package in Glossy.
import com.jay.glossy.ui.player.content.NowPlayingContentActions
import com.jay.glossy.ui.player.content.NowPlayingContentState

private const val CLUSTER_AUTO_HIDE_MS = 8_000L

@Composable
internal fun AppleMusicLyricsView(
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    typography: AppleMusicTypography,
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    modifier: Modifier = Modifier,
) {
    val localDensity = LocalDensity.current
    val lyricsData = state.screenData.lyricsData

    // Apple hands the whole page to the lyrics once you stop touching it, and brings the transport
    // back the moment you touch it again.
    var showCluster by rememberSaveable { mutableStateOf(true) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    
    var interactionTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(showCluster, interactionTick) {
        if (showCluster) {
            delay(CLUSTER_AUTO_HIDE_MS)
            showCluster = false
        }
    }

    val scrollWakesControls = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y != 0f) {
                    showCluster = true
                    interactionTick++
                }
                return Offset.Zero
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier.height(
                with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() } + 20.dp,
            ),
        )
        AppleMusicCompactHeader(state = state, actions = actions, typography = typography)

        Box(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(scrollWakesControls)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    showCluster = !showCluster
                    interactionTick++
                },
        ) {
            if (lyricsData != null) {
                // Assuming Glossy's Lyrics view requires sliderPositionProvider
                Lyrics(
                    sliderPositionProvider = { state.timelineState.current },
                    showLyrics = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .appleMusicVerticalFadeEdges(topFade = 28.dp, bottomFade = 18.dp)
                        .padding(horizontal = 20.dp),
                )
                
                // FIX: Wrapped AnimatedVisibility inside a Box to prevent ColumnScope implicit receiver error
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
                                icon = R.drawable.favorite, // Assuming you don't have thumbs_up_down, using favorite
                                onClick = { actions.onShowVoteDialog() }
                            )
                            AppleMusicFloatingCircleButton(
                                icon = R.drawable.share, 
                                onClick = { showShareSheet = true }
                            )
                            AppleMusicFloatingCircleButton(
                                icon = R.drawable.fullscreen, 
                                onClick = { actions.onShowFullscreenLyrics() }
                            )
                        }
                    }
                }
            }
        }

        // AnimatedVisibility for the bottom cluster
        AnimatedVisibility(
            visible = showCluster,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            AppleMusicBottomCluster(
                state = state,
                actions = actions,
                typography = typography,
                viewState = viewState,
                onSelectView = onSelectView,
                activePillContainer = activePillContainer,
                activePillContent = activePillContent,
                deviceVolumeController = null, 
            )
        }
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
