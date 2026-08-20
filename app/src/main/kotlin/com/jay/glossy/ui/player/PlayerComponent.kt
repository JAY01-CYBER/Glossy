/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.jay.glossy.R

// ==========================================
// 1. WAVY PLAYER STYLE (Perfect 82142.jpg Design)
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        shape = RoundedCornerShape(24.dp),
        color = containerColor, contentColor = contentColor,
        modifier = modifier.height(48.dp), onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(painter = painterResource(id = icon), contentDescription = text, modifier = Modifier.size(20.dp))
            if (text != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
