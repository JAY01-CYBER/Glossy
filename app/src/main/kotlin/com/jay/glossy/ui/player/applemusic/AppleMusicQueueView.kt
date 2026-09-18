/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player.applemusic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.jay.glossy.R
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.extensions.metadata
import com.jay.glossy.extensions.move
import com.jay.glossy.extensions.toggleRepeatMode
import com.jay.glossy.listentogether.RoomRole
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.LocalBottomSheetPageState
import com.jay.glossy.ui.component.LocalMenuState
import com.jay.glossy.ui.component.MediaMetadataListItem
import com.jay.glossy.ui.utils.ShowMediaInfo
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppleMusicQueueView(
    viewState: AppleMusicView,
    onSelectView: (AppleMusicView) -> Unit,
    activePillContainer: Color,
    activePillContent: Color,
    typography: AppleMusicTypography,
    bottomSheetState: BottomSheetState,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val localDensity = LocalDensity.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)?.value == RoomRole.GUEST

    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    
    val safeCurrentIndex = maxOf(0, currentWindowIndex)
    val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
    
    val lazyListState = rememberLazyListState()
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        val currentDragInfo = dragInfo
        dragInfo = if (currentDragInfo == null) {
            from.index to to.index
        } else {
            currentDragInfo.first to to.index
        }
        val safeFrom = from.index.coerceIn(0, mutableQueueWindows.lastIndex)
        val safeTo = to.index.coerceIn(0, mutableQueueWindows.lastIndex)
        mutableQueueWindows.move(safeFrom, safeTo)
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                val actualFrom = (from + safeCurrentIndex).coerceIn(0, queueWindows.lastIndex)
                val actualTo = (to + safeCurrentIndex).coerceIn(0, queueWindows.lastIndex)

                if (!playerConnection.player.shuffleModeEnabled) {
                    playerConnection.player.moveMediaItem(actualFrom, actualTo)
                } else {
                    playerConnection.player.setShuffleOrder(
                        DefaultShuffleOrder(
                            queueWindows.map { it.firstPeriodIndex }
                                .toMutableList()
                                .move(actualFrom, actualTo)
                                .toIntArray(),
                            System.currentTimeMillis(),
                        ),
                    )
                }
                dragInfo = null
            }
        }
    }

    LaunchedEffect(queueWindows, safeCurrentIndex) {
        mutableQueueWindows.apply {
            clear()
            addAll(queueWindows.drop(safeCurrentIndex))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier.height(
                with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() } + 20.dp,
            ),
        )
        
        AppleMusicCompactHeader(typography = typography)
        
        AppleMusicQueuePillsRow(
            activePillContainer = activePillContainer,
            activePillContent = activePillContent,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Box(modifier = Modifier.weight(1f).appleMusicVerticalFadeEdges(topFade = 24.dp, bottomFade = 48.dp)) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = mutableQueueWindows,
                    key = { _, item -> item.uid.hashCode() },
                ) { index, window ->
                    ReorderableItem(
                        state = reorderableState,
                        key = window.uid.hashCode(),
                    ) {
                        val isActive = window.uid == queueWindows.getOrNull(currentWindowIndex)?.uid
                        
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.animateItem(),
                        ) {
                            val trackMeta = window.mediaItem.metadata
                            if (trackMeta != null) {
                                MediaMetadataListItem(
                                    mediaMetadata = trackMeta,
                                    isActive = isActive,
                                    isPlaying = isActive && playerConnection.player.isPlaying,
                                    trailingContent = {
                                        if (!isListenTogetherGuest) {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        com.jay.glossy.ui.menu.QueueMenu(
                                                            mediaMetadata = trackMeta,
                                                            playerBottomSheetState = bottomSheetState,
                                                            onShowDetailsDialog = {
                                                                trackMeta.id.let {
                                                                    bottomSheetPageState.show {
                                                                        ShowMediaInfo(it)
                                                                    }
                                                                }
                                                            },
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = "More",
                                                    tint = Color.White
                                                )
                                            }

                                            IconButton(
                                                onClick = { },
                                                modifier = Modifier.draggableHandle()
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.drag_handle),
                                                    contentDescription = "Drag",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!isListenTogetherGuest) {
                                                if (!isActive) {
                                                    playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                    playerConnection.player.playWhenReady = true
                                                } else {
                                                    playerConnection.togglePlayPause()
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

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

@Composable
private fun AppleMusicQueuePillsRow(
    activePillContainer: Color,
    activePillContent: Color,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppleMusicQueuePill(
            icon = R.drawable.shuffle,
            active = shuffleModeEnabled,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
            modifier = Modifier.weight(1f),
        )
        AppleMusicQueuePill(
            icon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                else -> R.drawable.repeat
            },
            active = repeatMode != Player.REPEAT_MODE_OFF,
            activeContainer = activePillContainer,
            activeContent = activePillContent,
            onClick = { playerConnection.player.toggleRepeatMode() },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AppleMusicQueuePill(
    icon: Int,
    active: Boolean,
    activeContainer: Color,
    activeContent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .appleMusicPressInflate(pressedScale = 1.08f)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) activeContainer else AppleMusicPillInactive)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (active) activeContent else Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}
