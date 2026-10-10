/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.menu

import com.jay.glossy.R

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.jay.glossy.LocalNavController
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalDownloadUtil
import com.jay.glossy.LocalListenTogetherManager
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.ListItemHeight
import com.jay.glossy.constants.ShowLyricsOnPlayerKey
import com.jay.glossy.constants.VarispeedKey
import com.jay.glossy.listentogether.ConnectionState
import com.jay.glossy.listentogether.ListenTogetherEvent
import com.metrolist.models.MediaMetadata
import com.jay.glossy.playback.ExoDownloadService
import com.jay.glossy.db.entities.Song
import com.jay.glossy.db.entities.SpeedDialItem
import com.jay.glossy.ui.component.BottomSheetState
import com.jay.glossy.ui.component.ListDialog
import com.jay.glossy.ui.component.Material3MenuGroup
import com.jay.glossy.ui.component.Material3MenuItemData
import com.jay.glossy.ui.component.NewAction
import com.jay.glossy.ui.component.NewActionGrid
import com.jay.glossy.ui.component.getAudioBoostLabel
import com.jay.glossy.ui.player.SleepTimerPrompt
import com.jay.glossy.ui.utils.listenTogetherConnectionErrorText
import com.jay.glossy.ui.utils.listenTogetherServerErrorText
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.jayaudioutils.AudioDeviceBottomSheet
import com.jay.glossy.jayaudioutils.getConnectedBluetoothDeviceName
import com.jay.glossy.jayaudioutils.isWiredHeadphoneConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val navController = LocalNavController.current
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsStateWithLifecycle()

    val castHandler =
        remember(playerConnection) {
            try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsStateWithLifecycle() ?: remember { mutableFloatStateOf(1f) }
    val castDeviceName by castHandler?.castDeviceName?.collectAsStateWithLifecycle() ?: remember { mutableStateOf<String?>(null) }

    val varispeedMode by rememberPreference(VarispeedKey, defaultValue = false)
    val librarySong by database.song(mediaMetadata.id).collectAsStateWithLifecycle(initialValue = null)
    val coroutineScope = rememberCoroutineScope()
    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id).collectAsStateWithLifecycle(initialValue = null)
    val isPinned by database.speedDialDao.isPinned(mediaMetadata.id).collectAsStateWithLifecycle(initialValue = false)

    // The queue-peek strip is the only place Shuffle and the sleep timer used to
    // live on a full-screen player design, and that strip is gone on the custom
    // designs — so surface both here instead of dropping them.
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val sleepTimerEnabled = remember(playerConnection.service.sleepTimer?.triggerTime, playerConnection.service.sleepTimer?.pauseWhenSongEnd) {
        playerConnection.service.sleepTimer?.isActive ?: false
    }
    var showSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    val (showLyricsOnPlayer, onShowLyricsOnPlayerChange) = rememberPreference(ShowLyricsOnPlayerKey, defaultValue = false)

    // The one-tap loudness boost, reachable without a trip to Settings.
    val (audioBoost, onAudioBoostChange) = rememberEnumPreference(
        com.jay.glossy.constants.AudioBoostLevelKey,
        defaultValue = com.jay.glossy.eq.soundfx.AudioBoostLevel.OFF,
    )
    var showAudioBoostDialog by rememberSaveable { mutableStateOf(false) }

    if (showAudioBoostDialog) {
        com.jay.glossy.ui.component.AudioBoostDialog(
            current = audioBoost,
            onDismiss = { showAudioBoostDialog = false },
            onSelect = {
                onAudioBoostChange(it)
                showAudioBoostDialog = false
            },
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerPrompt(onDismiss = { showSleepTimerDialog = false })
    }

    val artists = remember(mediaMetadata.artists) { mediaMetadata.artists.filter { it.id != null } }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showListenTogetherDialog by rememberSaveable { mutableStateOf(false) }
    var showAudioDeviceBottomSheet by rememberSaveable { mutableStateOf(false) }

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = com.jay.glossy.listentogether.RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == com.jay.glossy.listentogether.RoomRole.GUEST
    
    val pendingSuggestions by listenTogetherManager?.pendingSuggestions?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    val systemEqLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.withTransaction { insert(mediaMetadata) }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onGetSongIds = { listOf(mediaMetadata.id) },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    ListenTogetherDialog(
        visible = showListenTogetherDialog,
        mediaMetadata = mediaMetadata,
        onDismiss = { showListenTogetherDialog = false },
    )

    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }

    if (showSelectArtistDialog) {
        ListDialog(onDismiss = { showSelectArtistDialog = false }) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            playerBottomSheetState.collapseSoft()
                            onDismiss()
                        }.padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable { mutableStateOf(false) }
    if (showPitchTempoDialog) TempoPitchDialog(onDismiss = { showPitchTempoDialog = false })

    var showSpeedDialog by rememberSaveable { mutableStateOf(false) }
    if (showSpeedDialog) SpeedDialog(onDismiss = { showSpeedDialog = false })
    
    // Live Active Audio Device Name tracking logic
    var activeDeviceName by remember { mutableStateOf(context.getString(R.string.phone_speaker)) }
    var isBluetoothActive by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val btName = getConnectedBluetoothDeviceName(context)
                if (btName != null) {
                    activeDeviceName = btName
                    isBluetoothActive = true
                } else if (isWiredHeadphoneConnected(context)) {
                    activeDeviceName = context.getString(R.string.wired_headphones)
                    isBluetoothActive = false
                } else {
                    activeDeviceName = context.getString(R.string.phone_speaker)
                    isBluetoothActive = false
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.media.AUDIO_BECOMING_NOISY")
        }
        context.registerReceiver(receiver, filter)
        
        val btName = getConnectedBluetoothDeviceName(context)
        if (btName != null) {
            activeDeviceName = btName
            isBluetoothActive = true
        } else if (isWiredHeadphoneConnected(context)) {
            activeDeviceName = context.getString(R.string.wired_headphones)
            isBluetoothActive = false
        } else {
            activeDeviceName = context.getString(R.string.phone_speaker)
            isBluetoothActive = false
        }

        onDispose { context.unregisterReceiver(receiver) }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, 
            top = 16.dp,
            end = 16.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp), 
        modifier = Modifier.animateContentSize().fillMaxSize()
    ) {
        if (isQueueTrigger != true) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PlayerMenuNowPlayingHeader(
                        mediaMetadata = mediaMetadata,
                        castDeviceName = if (isCasting) castDeviceName else null,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PlayerMenuAudioCard(
                        deviceName = if (isCasting) castDeviceName ?: stringResource(R.string.cast_device) else activeDeviceName,
                        isCasting = isCasting,
                        isBluetoothActive = isBluetoothActive,
                        onDeviceClick = { showAudioDeviceBottomSheet = true },
                        volume = if (isCasting) castVolume else playerVolume.value,
                        onVolumeChange = { volume ->
                            if (isCasting) castHandler?.setVolume(volume)
                            else playerConnection.service.playerVolume.value = volume
                        },
                    )
                }
            }
        }

        item {
            // Quick actions, laid out with the same Material 3 action grid the
            // rest of the app's menus use.
            NewActionGrid(
                actions =
                    listOfNotNull(
                        if (isListenTogetherGuest) {
                            null
                        } else {
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.radio),
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                },
                                text = stringResource(R.string.start_radio),
                                onClick = {
                                    Toast.makeText(context, context.getString(R.string.starting_radio), Toast.LENGTH_SHORT).show()
                                    playerConnection.startRadioSeamlessly()
                                    onDismiss()
                                },
                                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        },
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.playlist_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.add_to_playlist),
                            onClick = { showChoosePlaylistDialog = true },
                            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.link),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.copy_link),
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Song Link", "https://music.youtube.com/watch?v=${mediaMetadata.id}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ),
                modifier = Modifier.padding(horizontal = 4.dp),
                columns = if (isListenTogetherGuest) 2 else 3,
            )
        }

        item {
            // Song: everything that acts on the track itself.
            val isPodcast = mediaMetadata.album?.let { !it.id.startsWith("MPREb_") } ?: false
            val isInLibrary = librarySong?.song?.inLibrary != null

            PlayerMenuSectionLabel(text = stringResource(R.string.menu_section_song))

            Material3MenuGroup(
                items =
                    listOfNotNull(
                        if (artists.isNotEmpty() && !isPodcast) {
                            Material3MenuItemData(
                                icon = { PlayerMenuTonalTile(iconRes = R.drawable.artist) },
                                title = { Text(text = stringResource(R.string.view_artist)) },
                                description = { Text(text = mediaMetadata.artists.joinToString { it.name }) },
                                cardColors = menuRowColors(),
                                onClick = {
                                    if (mediaMetadata.artists.size == 1) {
                                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    } else {
                                        showSelectArtistDialog = true
                                    }
                                },
                            )
                        } else {
                            null
                        },
                        if (mediaMetadata.album != null) {
                            Material3MenuItemData(
                                icon = { PlayerMenuTonalTile(iconRes = if (isPodcast) R.drawable.mic else R.drawable.album) },
                                title = { Text(text = stringResource(if (isPodcast) R.string.view_podcast else R.string.view_album)) },
                                description = { Text(text = mediaMetadata.album.title) },
                                cardColors = menuRowColors(),
                                onClick = {
                                    if (isPodcast) navController.navigate("online_podcast/${mediaMetadata.album.id}")
                                    else navController.navigate("album/${mediaMetadata.album.id}")
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                },
                            )
                        } else {
                            null
                        },
                        Material3MenuItemData(
                            icon = { PlayerMenuTonalTile(iconRes = if (isInLibrary) R.drawable.library_add_check else R.drawable.library_add) },
                            title = { Text(text = stringResource(if (isInLibrary) R.string.remove_from_library else R.string.add_to_library)) },
                            cardColors = menuRowColors(),
                            onClick = {
                                playerConnection.toggleLibrary()
                                onDismiss()
                            },
                        ),
                        Material3MenuItemData(
                            icon = { PlayerMenuTonalTile(iconRes = if (isPinned) R.drawable.remove else R.drawable.add) },
                            title = { Text(text = if (isPinned) stringResource(R.string.unpin_from_speed_dial) else stringResource(R.string.pin_to_speed_dial)) },
                            cardColors = menuRowColors(),
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (isPinned) database.speedDialDao.delete(mediaMetadata.id)
                                    else database.speedDialDao.insert(SpeedDialItem.fromYTItem(mediaMetadata.toYTItem()))
                                }
                                onDismiss()
                            },
                        ),
                        when (download?.state) {
                            Download.STATE_COMPLETED ->
                                Material3MenuItemData(
                                    icon = { PlayerMenuTonalTile(iconRes = R.drawable.offline) },
                                    title = { Text(text = stringResource(R.string.remove_download)) },
                                    cardColors = menuRowColors(),
                                    onClick = { DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, mediaMetadata.id, false) },
                                )
                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING ->
                                Material3MenuItemData(
                                    icon = { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) },
                                    title = { Text(text = stringResource(R.string.downloading)) },
                                    cardColors = menuRowColors(),
                                    onClick = { DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, mediaMetadata.id, false) },
                                )
                            else ->
                                Material3MenuItemData(
                                    icon = { PlayerMenuTonalTile(iconRes = R.drawable.download) },
                                    title = { Text(text = stringResource(R.string.action_download)) },
                                    cardColors = menuRowColors(),
                                    onClick = {
                                        database.transaction { insert(mediaMetadata) }
                                        val downloadRequest = DownloadRequest.Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                            .setCustomCacheKey(mediaMetadata.id)
                                            .setData(mediaMetadata.title.toByteArray())
                                            .build()
                                        DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                    },
                                )
                        },
                    ),
            )
        }

        item {
            // Playback: toggles that change how the queue plays.
            PlayerMenuSectionLabel(text = stringResource(R.string.menu_section_playback))

            Material3MenuGroup(
                items =
                    listOf(
                        Material3MenuItemData(
                            icon = {
                                if (shuffleModeEnabled) PlayerMenuActiveTile(iconRes = R.drawable.shuffle)
                                else PlayerMenuTonalTile(iconRes = R.drawable.shuffle)
                            },
                            title = { Text(text = stringResource(R.string.shuffle)) },
                            cardColors = if (shuffleModeEnabled) menuActiveRowColors() else menuRowColors(),
                            trailingContent = if (shuffleModeEnabled) {
                                { PlayerMenuActiveCheck() }
                            } else {
                                null
                            },
                            onClick = {
                                playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                                onDismiss()
                            },
                        ),
                        // Lyrics on the player, the same control the reference menu
                        // shows as "Hide lyrics". The choice is remembered, so the
                        // lyrics stay on for every player design until turned off.
                        Material3MenuItemData(
                            icon = {
                                if (showLyricsOnPlayer) PlayerMenuActiveTile(iconRes = R.drawable.lyrics)
                                else PlayerMenuTonalTile(iconRes = R.drawable.lyrics)
                            },
                            title = {
                                Text(
                                    text = stringResource(
                                        if (showLyricsOnPlayer) R.string.hide_lyrics else R.string.show_lyrics
                                    )
                                )
                            },
                            cardColors = if (showLyricsOnPlayer) menuActiveRowColors() else menuRowColors(),
                            trailingContent = if (showLyricsOnPlayer) {
                                { PlayerMenuActiveCheck() }
                            } else {
                                null
                            },
                            onClick = {
                                onShowLyricsOnPlayerChange(!showLyricsOnPlayer)
                                onDismiss()
                            },
                        ),
                        Material3MenuItemData(
                            icon = {
                                if (sleepTimerEnabled) PlayerMenuActiveTile(iconRes = R.drawable.bedtime)
                                else PlayerMenuTonalTile(iconRes = R.drawable.bedtime)
                            },
                            title = { Text(text = stringResource(R.string.sleep_timer)) },
                            cardColors = if (sleepTimerEnabled) menuActiveRowColors() else menuRowColors(),
                            trailingContent = if (sleepTimerEnabled) {
                                { PlayerMenuActiveCheck() }
                            } else {
                                null
                            },
                            onClick = {
                                if (sleepTimerEnabled) {
                                    playerConnection.service.sleepTimer?.clear()
                                    onDismiss()
                                } else {
                                    showSleepTimerDialog = true
                                }
                            },
                        ),
                    ),
            )
        }

        item {
            // Room: the shared-listening controls.
            val pendingCount = pendingSuggestions.size

            PlayerMenuSectionLabel(text = stringResource(R.string.menu_section_room))

            Material3MenuGroup(
                items =
                    listOfNotNull(
                        Material3MenuItemData(
                            icon = { PlayerMenuTonalTile(iconRes = R.drawable.group) },
                            title = { Text(text = stringResource(R.string.listen_together)) },
                            cardColors = menuRowColors(),
                            trailingContent = if (pendingCount > 0) {
                                {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = pendingCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                }
                            } else null,
                            onClick = { showListenTogetherDialog = true }
                        ),
                        if (isListenTogetherGuest) {
                            Material3MenuItemData(
                                icon = { PlayerMenuTonalTile(iconRes = R.drawable.replay) },
                                title = { Text(text = stringResource(R.string.resync)) },
                                cardColors = menuRowColors(),
                                onClick = {
                                    listenTogetherManager?.requestSync()
                                    onDismiss()
                                },
                            )
                        } else {
                            null
                        },
                    ),
            )
        }

        if (isQueueTrigger != true) {
            item {
                // Audio: sound shaping and the system effect panel.
                PlayerMenuSectionLabel(text = stringResource(R.string.menu_section_audio))

                Material3MenuGroup(
                    items =
                        listOf(
                            Material3MenuItemData(
                                icon = { PlayerMenuTonalTile(iconRes = R.drawable.volume_up) },
                                title = { Text(text = stringResource(R.string.audio_boost)) },
                                description = {
                                    Text(
                                        if (audioBoost.isOn) getAudioBoostLabel(audioBoost)
                                        else stringResource(R.string.audio_boost_desc)
                                    )
                                },
                                cardColors = if (audioBoost.isOn) menuActiveRowColors() else menuRowColors(),
                                onClick = { showAudioBoostDialog = true },
                            ),
                            Material3MenuItemData(
                                icon = { PlayerMenuTonalTile(iconRes = R.drawable.equalizer) },
                                title = { Text(text = stringResource(R.string.equalizer)) },
                                description = { Text(text = stringResource(R.string.equalizer_desc)) },
                                cardColors = menuRowColors(),
                                onClick = {
                                    navController.navigate("equalizer")
                                    onDismiss()
                                },
                            ),
                            Material3MenuItemData(
                                icon = { PlayerMenuTonalTile(iconRes = R.drawable.graphic_eq) },
                                title = { Text(text = stringResource(R.string.system_equalizer)) },
                                description = { Text(text = stringResource(R.string.system_equalizer_desc)) },
                                cardColors = menuRowColors(),
                                onClick = {
                                    val audioSessionId = playerConnection.player.audioSessionId
                                    if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                                        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                                        }
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            systemEqLauncher.launch(intent)
                                        }
                                    }
                                    onDismiss()
                                },
                            ),
                            Material3MenuItemData(
                                icon = { PlayerMenuTonalTile(iconRes = R.drawable.tune) },
                                title = { Text(text = stringResource(R.string.advanced)) },
                                description = { Text(text = stringResource(R.string.advanced_desc)) },
                                cardColors = menuRowColors(),
                                onClick = {
                                    if (!varispeedMode) showPitchTempoDialog = true
                                    else showSpeedDialog = true
                                },
                            ),
                        ),
                )
            }
        }

        item {
            // More: the track's technical details.
            PlayerMenuSectionLabel(text = stringResource(R.string.menu_section_more))

            Material3MenuGroup(
                items =
                    listOf(
                        Material3MenuItemData(
                            icon = { PlayerMenuTonalTile(iconRes = R.drawable.info) },
                            title = { Text(text = stringResource(R.string.details)) },
                            description = { Text(text = stringResource(R.string.details_desc)) },
                            cardColors = menuRowColors(),
                            onClick = {
                                onShowDetailsDialog()
                                onDismiss()
                            },
                        ),
                    ),
            )
        }
    }
    
    // Bottom Sheet yahan pe open hogi
    if (showAudioDeviceBottomSheet) {
        AudioDeviceBottomSheet(
            onDismiss = { showAudioDeviceBottomSheet = false }
        )
    }
}

// ============================================================================
// MATERIAL 3 MENU PIECES
// ============================================================================

@Composable
private fun menuRowColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)

@Composable
private fun menuActiveRowColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)

@Composable
private fun PlayerMenuSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun PlayerMenuIconTile(
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PlayerMenuTonalTile(iconRes: Int) {
    PlayerMenuIconTile(
        iconRes = iconRes,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun PlayerMenuActiveTile(iconRes: Int) {
    PlayerMenuIconTile(
        iconRes = iconRes,
        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun PlayerMenuActiveCheck() {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun PlayerMenuNowPlayingHeader(
    mediaMetadata: MediaMetadata,
    castDeviceName: String?,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                AsyncImage(
                    model = mediaMetadata.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (castDeviceName != null) {
                        stringResource(R.string.casting_to, castDeviceName)
                    } else {
                        stringResource(R.string.now_playing)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val artistNames = mediaMetadata.artists.joinToString { it.name }
                if (artistNames.isNotBlank()) {
                    Text(
                        text = artistNames,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerMenuAudioCard(
    deviceName: String,
    isCasting: Boolean,
    isBluetoothActive: Boolean,
    onDeviceClick: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Surface(
                onClick = onDeviceClick,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerMenuIconTile(
                        iconRes = when {
                            isCasting -> R.drawable.cast_connected
                            isBluetoothActive -> R.drawable.headset_applemusic
                            else -> R.drawable.speaker_apple
                        },
                        containerColor = if (isCasting) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        },
                        contentColor = if (isCasting) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.audio_output),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            PlayerMenuVolumeRow(volume = volume, onVolumeChange = onVolumeChange)
        }
    }
}

@Composable
private fun PlayerMenuVolumeRow(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    val safeVolume = volume.coerceIn(0f, 1f)
    var lastAudibleVolume by remember { mutableFloatStateOf(if (safeVolume > 0.01f) safeVolume else 1f) }
    LaunchedEffect(safeVolume) {
        if (safeVolume > 0.01f) lastAudibleVolume = safeVolume
    }
    val isMuted = safeVolume <= 0.01f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                onVolumeChange(if (isMuted) lastAudibleVolume.coerceAtLeast(0.05f) else 0f)
            },
        ) {
            Icon(
                painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
                contentDescription = stringResource(if (isMuted) R.string.unmute else R.string.mute),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = safeVolume,
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(safeVolume * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp),
        )
    }
}

// ============================================================================
// EXISTING DIALOG COMPONENTS
// ============================================================================

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }
    val listenTogetherManager = com.jay.glossy.LocalListenTogetherManager.current
    val isInRoom = listenTogetherManager?.isInRoom ?: false

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                if (!isInRoom) {
                    ValueAdjuster(
                        icon = R.drawable.speed,
                        currentValue = tempo,
                        values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                        onValueUpdate = {
                            tempo = it
                            updatePlaybackParameters()
                        },
                        valueText = { "x$it" },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun SpeedDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var speed by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(speed, speed)
    }
    val listenTogetherManager = com.jay.glossy.LocalListenTogetherManager.current

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.speed))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    speed = 1f
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                ValueAdjuster(
                    icon = R.drawable.speed,
                    currentValue = speed,
                    values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                    onValueUpdate = {
                        speed = it
                        updatePlaybackParameters()
                    },
                    valueText = { "x$it" },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        },
    )
}

@Composable
fun <T> ValueAdjuster(
    @DrawableRes icon: Int,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null,
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
            )
        }
    }
}

@Composable
fun ListenTogetherDialog(
    visible: Boolean,
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    val listenTogetherManager = com.jay.glossy.LocalListenTogetherManager.current
    val joiningRoomTemplate = stringResource(R.string.joining_room)

    if (listenTogetherManager == null) {
        ListDialog(onDismiss = onDismiss) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.group),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.listen_together),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.listen_together_not_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
        return
    }

    val connectionState by listenTogetherManager.connectionState.collectAsStateWithLifecycle()
    val roomState by listenTogetherManager.roomState.collectAsStateWithLifecycle()
    val userId by listenTogetherManager.userId.collectAsStateWithLifecycle()
    val pendingJoinRequests by listenTogetherManager.pendingJoinRequests.collectAsStateWithLifecycle()
    val pendingSuggestions by listenTogetherManager.pendingSuggestions.collectAsStateWithLifecycle()

    var savedUsername by rememberPreference(com.jay.glossy.constants.ListenTogetherUsernameKey, "")
    var roomCodeInput by rememberSaveable { mutableStateOf("") }
    var usernameInput by rememberSaveable { mutableStateOf(savedUsername) }

    var isCreatingRoom by rememberSaveable { mutableStateOf(false) }
    var isJoiningRoom by rememberSaveable { mutableStateOf(false) }
    var joinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedUserForMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUsername by rememberSaveable { mutableStateOf<String?>(null) }

    val waitingForApprovalText = stringResource(R.string.waiting_for_approval)
    val invalidRoomCodeText = stringResource(R.string.invalid_room_code)
    val joinRequestDeniedText = stringResource(R.string.join_request_denied)

    if (selectedUserForMenu != null && selectedUsername != null) {
        ListDialog(
            onDismiss = {
                selectedUserForMenu = null
                selectedUsername = null
            },
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.group),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.manage_user),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = selectedUsername ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable {
                        selectedUserForMenu?.let { listenTogetherManager.kickUser(it, "Removed by host") }
                        selectedUserForMenu = null
                        selectedUsername = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        Icon(painter = painterResource(R.drawable.close), contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.kick_user), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            Text(text = stringResource(R.string.kick_user_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable {
                        selectedUserForMenu?.let { uid ->
                            selectedUsername?.let { uname ->
                                listenTogetherManager.blockUser(uname)
                                listenTogetherManager.kickUser(uid, R.string.user_blocked_by_host.toString())
                            }
                        }
                        selectedUserForMenu = null
                        selectedUsername = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        Icon(painter = painterResource(R.drawable.close), contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.permanently_kick_user), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = stringResource(R.string.permanently_kick_user_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable {
                        selectedUserForMenu?.let { listenTogetherManager.transferHost(it) }
                        selectedUserForMenu = null
                        selectedUsername = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        Icon(painter = painterResource(R.drawable.crown), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.transfer_ownership), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(text = stringResource(R.string.transfer_ownership_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        return
    }

    LaunchedEffect(savedUsername) {
        if (usernameInput.isBlank() && savedUsername.isNotBlank()) {
            usernameInput = savedUsername
        }
    }

    LaunchedEffect(listenTogetherManager) {
        listenTogetherManager.events.collect { event ->
            when (event) {
                is ListenTogetherEvent.JoinRejected -> {
                    val reason = event.reason
                    joinErrorMessage = when {
                        reason.isNullOrBlank() -> joinRequestDeniedText
                        reason.contains("invalid", ignoreCase = true) == true -> invalidRoomCodeText
                        else -> "$joinRequestDeniedText: $reason"
                    }
                    isJoiningRoom = false
                    isCreatingRoom = false
                }
                is ListenTogetherEvent.JoinApproved -> {
                    isJoiningRoom = false
                    joinErrorMessage = null
                }
                is ListenTogetherEvent.RoomCreated -> {
                    isCreatingRoom = false
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("ListenTogetherRoom", event.roomCode)
                    clipboard.setPrimaryClip(clip)
                }
                // Same as the Listen Together screen: a rejected or unreachable
                // server has to clear the pending state and say why, instead of
                // leaving the sheet on a spinner for a room that never arrives.
                is ListenTogetherEvent.ServerError -> {
                    isCreatingRoom = false
                    isJoiningRoom = false
                    joinErrorMessage = listenTogetherServerErrorText(context, event.code, event.message)
                }
                is ListenTogetherEvent.ConnectionError -> {
                    isCreatingRoom = false
                    isJoiningRoom = false
                    joinErrorMessage = listenTogetherConnectionErrorText(context)
                }
                else -> { }
            }
        }
    }

    val isInRoom = listenTogetherManager.isInRoom
    val isHost = roomState?.hostId == userId

    ListDialog(onDismiss = onDismiss) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Icon(painter = painterResource(R.drawable.group), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (isInRoom) { if (isHost) stringResource(R.string.hosting_room) else stringResource(R.string.in_room) } else { stringResource(R.string.listen_together) },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = when (connectionState) {
                    ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ConnectionState.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Box(
                            modifier = Modifier.size(10.dp).background(
                                color = when (connectionState) {
                                    ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondary
                                    ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                    ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
                                },
                                shape = RoundedCornerShape(50),
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> stringResource(R.string.listen_together_connected)
                                ConnectionState.CONNECTING -> stringResource(R.string.listen_together_connecting)
                                ConnectionState.RECONNECTING -> stringResource(R.string.listen_together_reconnecting)
                                ConnectionState.ERROR -> stringResource(R.string.listen_together_error)
                                ConnectionState.DISCONNECTED -> stringResource(R.string.listen_together_disconnected)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondary
                                ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }

                    if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                            Button(
                                onClick = { listenTogetherManager.connect() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            ) {
                                Text(stringResource(R.string.connect), fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = { listenTogetherManager.disconnect() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            ) {
                                Text(stringResource(R.string.disconnect), fontWeight = FontWeight.SemiBold)
                            }
                            FilledTonalButton(
                                onClick = { listenTogetherManager.forceReconnect() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Reconnect", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        if (connectionState == ConnectionState.CONNECTED && !isInRoom) {
            item {
                Text(
                    text = stringResource(R.string.listen_together_background_disconnect_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (isInRoom) {
            roomState?.let { room ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(R.string.room_code), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text(text = room.roomCode, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
                            }
                            if (isHost) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val inviteLink = remember(room.roomCode) { "https://metrolist.cc/listen?code=${room.roomCode}" }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    FilledTonalButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Listen Together Link", inviteLink)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        },
                                    ) {
                                        Icon(painter = painterResource(R.drawable.link), contentDescription = stringResource(R.string.copy_link), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.copy_link))
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledTonalButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Room Code", room.roomCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        },
                                    ) {
                                        Icon(painter = painterResource(R.drawable.content_copy), contentDescription = stringResource(R.string.copy_code), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.copy_code))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                val connectedUsers = room.users.filter { it.isConnected }

                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text(
                            text = stringResource(R.string.connected_users, connectedUsers.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            connectedUsers.forEach { user ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(72.dp).clickable(
                                        enabled = isHost && user.userId != userId,
                                        onClick = {
                                            selectedUserForMenu = user.userId
                                            selectedUsername = user.username
                                        },
                                    ),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            modifier = Modifier.size(52.dp),
                                            shape = RoundedCornerShape(50),
                                            color = if (user.isHost) MaterialTheme.colorScheme.primary else if (user.userId == userId) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Text(
                                                    text = user.username.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (user.isHost) MaterialTheme.colorScheme.onPrimary else if (user.userId == userId) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        if (user.isHost || user.userId == userId) {
                                            Surface(
                                                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(18.dp),
                                                shape = RoundedCornerShape(50),
                                                color = if (user.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Icon(painter = painterResource(if (user.isHost) R.drawable.crown else R.drawable.person), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = user.username,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (user.userId == userId) FontWeight.Bold else FontWeight.Medium,
                                        color = if (user.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )

                                    if (user.isHost) {
                                        Text(text = stringResource(R.string.host_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                    } else if (user.userId == userId) {
                                        Text(text = stringResource(R.string.you_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (isHost && pendingJoinRequests.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = stringResource(R.string.pending_requests), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(pendingJoinRequests) { request ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                    Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondary) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(text = request.username.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                                        }
                                    }
                                    Text(text = request.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { listenTogetherManager.approveJoin(request.userId) }) {
                                        Icon(painter = painterResource(R.drawable.check), contentDescription = stringResource(R.string.approve), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    }
                                    IconButton(onClick = { listenTogetherManager.rejectJoin(request.userId, "Rejected by host") }) {
                                        Icon(painter = painterResource(R.drawable.close), contentDescription = stringResource(R.string.reject), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (isHost && pendingSuggestions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = stringResource(R.string.pending_suggestions), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(pendingSuggestions) { suggestion ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                    Icon(painter = painterResource(R.drawable.queue_music), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = suggestion.trackInfo.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(text = suggestion.fromUsername, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { listenTogetherManager.approveSuggestion(suggestion.suggestionId) }) {
                                        Icon(painter = painterResource(R.drawable.check), contentDescription = stringResource(R.string.approve), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    }
                                    IconButton(onClick = { listenTogetherManager.rejectSuggestion(suggestion.suggestionId, "Rejected by host") }) {
                                        Icon(painter = painterResource(R.drawable.close), contentDescription = stringResource(R.string.reject), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.cancel), fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = {
                                listenTogetherManager.leaveRoom()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(painter = painterResource(R.drawable.logout), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.leave_room), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = stringResource(R.string.listen_together_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text(stringResource(R.string.username)) },
                            placeholder = { Text(stringResource(R.string.enter_username)) },
                            leadingIcon = { Icon(painterResource(R.drawable.person), null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (usernameInput.isNotBlank()) {
                                    IconButton(onClick = { usernameInput = "" }) { Icon(painterResource(R.drawable.close), null) }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedLabelColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        HorizontalDivider()

                        Text(text = stringResource(R.string.join_existing_room), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) },
                            label = { Text(stringResource(R.string.room_code)) },
                            placeholder = { Text("ABCD1234") },
                            supportingText = { Text(text = "${roomCodeInput.length}/8", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(painterResource(R.drawable.token), null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline, focusedLabelColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (isCreatingRoom) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.creating_room), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (isJoiningRoom) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = waitingForApprovalText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }
                        }

                        joinErrorMessage?.let { msg ->
                            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(12.dp)) {
                                    Icon(painterResource(R.drawable.error), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                                val finalUsername = username.trim()
                                if (finalUsername.isNotBlank()) {
                                    savedUsername = finalUsername
                                    Toast.makeText(context, R.string.creating_room, Toast.LENGTH_SHORT).show()
                                    isCreatingRoom = true
                                    isJoiningRoom = false
                                    joinErrorMessage = null
                                    listenTogetherManager.connect()
                                    listenTogetherManager.createRoom(finalUsername)
                                } else {
                                    Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = (usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(painter = painterResource(R.drawable.add), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.create_room), fontWeight = FontWeight.SemiBold)
                        }

                        if (roomCodeInput.length == 8) {
                            Button(
                                onClick = {
                                    val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                                    val finalUsername = username.trim()
                                    if (finalUsername.isNotBlank()) {
                                        savedUsername = finalUsername
                                        Toast.makeText(context, String.format(joiningRoomTemplate, roomCodeInput), Toast.LENGTH_SHORT).show()
                                        isJoiningRoom = true
                                        isCreatingRoom = false
                                        joinErrorMessage = null
                                        listenTogetherManager.connect()
                                        listenTogetherManager.joinRoom(roomCodeInput, finalUsername)
                                    } else {
                                        Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = (usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            ) {
                                Icon(painter = painterResource(R.drawable.login), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.join_room), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
