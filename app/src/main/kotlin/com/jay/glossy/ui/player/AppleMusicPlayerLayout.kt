package com.jay.glossy.ui.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.jay.glossy.extensions.togglePlayPause
import com.jay.glossy.utils.makeTimeString
import com.metrolist.models.MediaMetadata
import com.jay.glossy.ui.component.Lyrics
import com.jay.glossy.ui.component.PlayerSliderTrack

enum class AppleMusicView { MAIN, LYRICS, QUEUE }

@Composable
fun AppleMusicPlayerLayout(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    onClose: () -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(null)
    
    var viewState by remember { mutableStateOf(AppleMusicView.MAIN) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    val currentPosition = sliderPosition ?: position

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Frosted Background
        AsyncImage(
            model = mediaMetadata.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .alpha(0.5f) 
        )

        Crossfade(targetState = viewState, label = "AppleMusicTabs") { view ->
            when (view) {
                AppleMusicView.MAIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp))
                        
                        // Top Drag Handle
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(36.dp, 5.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.35f)))
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Artwork
                        AsyncImage(
                            model = mediaMetadata.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Title & Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mediaMetadata.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = mediaMetadata.artists.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            val isFavorite = currentSong?.song?.liked == true
                            IconButton(onClick = { playerConnection.toggleLike() }) {
                                Icon(
                                    painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                                    contentDescription = "Like",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Bottom Controls
                        AppleMusicBottomClusterGlossy(
                            position = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            onSeek = { sliderPosition = it },
                            onSeekFinished = {
                                sliderPosition?.let { playerConnection.player.seekTo(it) }
                                sliderPosition = null
                            },
                            onPlayPause = { playerConnection.togglePlayPause() },
                            onNext = { playerConnection.seekToNext() },
                            onPrev = { playerConnection.seekToPrevious() },
                            viewState = viewState,
                            onTabSelect = { viewState = it }
                        )
                    }
                }
                AppleMusicView.LYRICS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp))
                        AppleMusicCompactHeader(mediaMetadata = mediaMetadata, onClose = { viewState = AppleMusicView.MAIN })
                        
                        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            Lyrics(
                                sliderPositionProvider = { currentPosition },
                                modifier = Modifier.fillMaxSize(),
                                showLyrics = true
                            )
                        }
                        
                        AppleMusicBottomClusterGlossy(
                            position = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            onSeek = { sliderPosition = it },
                            onSeekFinished = {
                                sliderPosition?.let { playerConnection.player.seekTo(it) }
                                sliderPosition = null
                            },
                            onPlayPause = { playerConnection.togglePlayPause() },
                            onNext = { playerConnection.seekToNext() },
                            onPrev = { playerConnection.seekToPrevious() },
                            viewState = viewState,
                            onTabSelect = { viewState = it },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                AppleMusicView.QUEUE -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp))
                        AppleMusicCompactHeader(mediaMetadata = mediaMetadata, onClose = { viewState = AppleMusicView.MAIN })
                        
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("Queue (Tap out of bottom sheet to use Glossy's Queue)", color = Color.White.copy(0.7f))
                        }
                        
                        AppleMusicBottomClusterGlossy(
                            position = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            onSeek = { sliderPosition = it },
                            onSeekFinished = {
                                sliderPosition?.let { playerConnection.player.seekTo(it) }
                                sliderPosition = null
                            },
                            onPlayPause = { playerConnection.togglePlayPause() },
                            onNext = { playerConnection.seekToNext() },
                            onPrev = { playerConnection.seekToPrevious() },
                            viewState = viewState,
                            onTabSelect = { viewState = it },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicBottomClusterGlossy(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    viewState: AppleMusicView,
    onTabSelect: (AppleMusicView) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Slim Slider
        Slider(
            value = position.toFloat(),
            valueRange = 0f..(if (duration <= 0) 0f else duration.toFloat()),
            onValueChange = { onSeek(it.toLong()) },
            onValueChangeFinished = onSeekFinished,
            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
            track = { sliderState ->
                PlayerSliderTrack(
                    sliderState = sliderState,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White.copy(alpha = 0.9f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    trackHeight = 7.dp
                )
            },
            modifier = Modifier.height(24.dp)
        )
        
        // Times Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(makeTimeString(position), color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelMedium)
            Text(makeTimeString(duration), color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Transport Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.skip_previous), contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(66.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.skip_next), contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Dock Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DockIcon(
                icon = R.drawable.lyrics,
                isActive = viewState == AppleMusicView.LYRICS,
                onClick = { onTabSelect(if (viewState == AppleMusicView.LYRICS) AppleMusicView.MAIN else AppleMusicView.LYRICS) }
            )
            DockIcon(
                icon = R.drawable.queue_music,
                isActive = viewState == AppleMusicView.QUEUE,
                onClick = { onTabSelect(if (viewState == AppleMusicView.QUEUE) AppleMusicView.MAIN else AppleMusicView.QUEUE) }
            )
        }
    }
}

@Composable
fun DockIcon(icon: Int, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isActive) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun AppleMusicCompactHeader(mediaMetadata: MediaMetadata, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).clickable { onClose() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = mediaMetadata.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mediaMetadata.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
