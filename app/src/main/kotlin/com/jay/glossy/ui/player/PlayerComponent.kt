/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.jay.glossy.R
import com.jay.glossy.ui.component.ResizableIconButton

// ==========================================
// 1. WAVY PLAYER STYLE (Same to same as 82147.jpg)
// ==========================================
@Composable
fun WavyPlayerStyle(
    textButtonColor: Color,
    iconButtonColor: Color,
    sideButtonContainerColor: Color,
    sideButtonContentColor: Color,
    focusRequester: FocusRequester,
    isListenTogetherGuest: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    effectiveIsPlaying: Boolean,
    isMuted: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleRepeat: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Controls: Prev, Play, Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onPreviousClick,
                enabled = canSkipPrevious && !isListenTogetherGuest,
                shape = RoundedCornerShape(24.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = sideButtonContainerColor,
                    contentColor = sideButtonContentColor
                ),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp))
            }

            FilledIconButton(
                onClick = onPlayPauseClick,
                shape = RoundedCornerShape(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = textButtonColor,
                    contentColor = iconButtonColor
                ),
                modifier = Modifier.size(96.dp).focusRequester(focusRequester)
            ) {
                Icon(
                    painter = painterResource(
                        if (isListenTogetherGuest) {
                            if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        } else {
                            if (effectiveIsPlaying) R.drawable.pause else R.drawable.play
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }

            FilledIconButton(
                onClick = onNextClick,
                enabled = canSkipNext && !isListenTogetherGuest,
                shape = RoundedCornerShape(24.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = sideButtonContainerColor,
                    contentColor = sideButtonContentColor
                ),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Row 1: Heart, Download, Repeat
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButtonPill(
                icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                text = null,
                containerColor = sideButtonContainerColor,
                contentColor = if (isFavorite) MaterialTheme.colorScheme.error else sideButtonContentColor,
                modifier = Modifier.weight(1f),
                onClick = onToggleLike
            )
            ActionButtonPill(
                icon = R.drawable.offline, 
                text = "Download",
                containerColor = sideButtonContainerColor,
                contentColor = sideButtonContentColor,
                modifier = Modifier.weight(1.5f),
                onClick = onDownloadClick
            )
            ActionButtonPill(
                icon = when (repeatMode) {
                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                    else -> R.drawable.repeat
                },
                text = "Repeat",
                containerColor = sideButtonContainerColor,
                contentColor = if (repeatMode != Player.REPEAT_MODE_OFF) textButtonColor else sideButtonContentColor,
                modifier = Modifier.weight(1.5f),
                onClick = onToggleRepeat
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Lyrics, Queue
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButtonPill(
                icon = R.drawable.lyrics, text = "Lyrics",
                containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor,
                modifier = Modifier.weight(1f), onClick = onLyricsClick
            )
            ActionButtonPill(
                icon = R.drawable.queue_music, text = "Queue",
                containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor,
                modifier = Modifier.weight(1f), onClick = onQueueClick
            )
        }
    }
}

@Composable
private fun ActionButtonPill(
    icon: Int, text: String?, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp), // Perfect pill shape
        color = containerColor, contentColor = contentColor,
        modifier = modifier.height(48.dp), onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(painter = painterResource(id = icon), contentDescription = text, modifier = Modifier.size(20.dp))
            if (text != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ==========================================
// 2. MODERN PLAYER STYLE 
// ==========================================
@Composable
fun ModernPlayerStyle(
    textButtonColor: Color,
    iconButtonColor: Color,
    sideButtonContainerColor: Color,
    sideButtonContentColor: Color,
    focusRequester: FocusRequester,
    isListenTogetherGuest: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    effectiveIsPlaying: Boolean,
    isMuted: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        val backInteractionSource = remember { MutableInteractionSource() }
        val nextInteractionSource = remember { MutableInteractionSource() }
        val playPauseInteractionSource = remember { MutableInteractionSource() }

        val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
        val isBackPressed by backInteractionSource.collectIsPressedAsState()
        val isNextPressed by nextInteractionSource.collectIsPressedAsState()

        val playPauseWeight by animateFloatAsState(
            targetValue = if (isPlayPausePressed) 1.9f else if (isBackPressed || isNextPressed) 1.1f else 1.3f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f), label = "playPauseWeight"
        )
        val backButtonWeight by animateFloatAsState(
            targetValue = if (isBackPressed) 0.65f else if (isPlayPausePressed) 0.35f else 0.45f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f), label = "backButtonWeight"
        )
        val nextButtonWeight by animateFloatAsState(
            targetValue = if (isNextPressed) 0.65f else if (isPlayPausePressed) 0.35f else 0.45f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f), label = "nextButtonWeight"
        )

        FilledIconButton(
            onClick = onPreviousClick, enabled = canSkipPrevious && !isListenTogetherGuest,
            shape = RoundedCornerShape(50), interactionSource = backInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor),
            modifier = Modifier.height(68.dp).weight(backButtonWeight)
        ) {
            Icon(painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = onPlayPauseClick, shape = RoundedCornerShape(50), interactionSource = playPauseInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor),
            modifier = Modifier.height(68.dp).weight(playPauseWeight).focusRequester(focusRequester)
        ) {
            Icon(
                painterResource(
                    if (isListenTogetherGuest) {
                        if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                    } else {
                        if (effectiveIsPlaying) R.drawable.pause else R.drawable.play
                    }
                ),
                contentDescription = null, modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledIconButton(
            onClick = onNextClick, enabled = canSkipNext && !isListenTogetherGuest,
            shape = RoundedCornerShape(50), interactionSource = nextInteractionSource,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor),
            modifier = Modifier.height(68.dp).weight(nextButtonWeight)
        ) {
            Icon(painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

// ==========================================
// 3. CLASSIC PLAYER STYLE 
// ==========================================
@Composable
fun ClassicPlayerStyle(
    textButtonColor: Color,
    iconButtonColor: Color,
    TextBackgroundColor: Color,
    focusRequester: FocusRequester,
    isListenTogetherGuest: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    effectiveIsPlaying: Boolean,
    isMuted: Boolean,
    playbackState: Int,
    repeatMode: Int,
    isFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    val playPauseRoundness by animateDpAsState(
        targetValue = if (effectiveIsPlaying) 24.dp else 36.dp,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "playPauseRoundness",
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f)) {
            ResizableIconButton(
                icon = when (repeatMode) {
                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                    else -> throw IllegalStateException()
                },
                color = TextBackgroundColor,
                modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest || repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                enabled = !isListenTogetherGuest, onClick = onToggleRepeat,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            ResizableIconButton(
                icon = R.drawable.skip_previous, enabled = canSkipPrevious && !isListenTogetherGuest, color = TextBackgroundColor,
                modifier = Modifier.size(32.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest) 0.5f else 1f),
                onClick = onPreviousClick,
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(playPauseRoundness)).background(textButtonColor)
                .clickable { onPlayPauseClick() }.focusRequester(focusRequester),
        ) {
            Image(
                painter = painterResource(
                    if (isListenTogetherGuest) { if (isMuted) R.drawable.volume_off else R.drawable.volume_up } 
                    else if (playbackState == Player.STATE_ENDED) R.drawable.replay 
                    else if (effectiveIsPlaying) R.drawable.pause else R.drawable.play
                ),
                contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor), modifier = Modifier.align(Alignment.Center).size(36.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            ResizableIconButton(
                icon = R.drawable.skip_next, enabled = canSkipNext && !isListenTogetherGuest, color = TextBackgroundColor,
                modifier = Modifier.size(32.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest) 0.5f else 1f),
                onClick = onNextClick,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            ResizableIconButton(
                icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                color = if (isFavorite) MaterialTheme.colorScheme.error else TextBackgroundColor,
                modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center),
                onClick = onToggleLike,
            )
        }
    }
}
