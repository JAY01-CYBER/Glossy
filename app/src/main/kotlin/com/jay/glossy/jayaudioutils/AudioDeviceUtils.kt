/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.jay.glossy.jayaudioutils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.R
import com.jay.glossy.ui.shapes.RoundedStarShape

// Global state to sync UI instantly without waiting for playback engine
object AudioDeviceState {
    var preferredDeviceId: Int? = null
}

// ============================================================================
// BLUETOOTH & DEVICE DETECTION UTILS
// ============================================================================

fun isBluetoothHeadphoneConnected(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        audioDevices.any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        }
    } else {
        @Suppress("DEPRECATION")
        audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
    }
}

fun isWiredHeadphoneConnected(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        audioDevices.any { device ->
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            device.type == AudioDeviceInfo.TYPE_AUX_LINE
        }
    } else {
        @Suppress("DEPRECATION")
        audioManager.isWiredHeadsetOn
    }
}

fun getConnectedBluetoothDeviceName(context: Context): String? {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val isBluetoothActive = audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
    if (!isBluetoothActive) return null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val activeBluetoothDevice = audioDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: audioDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
        return activeBluetoothDevice?.productName?.toString()
    } else {
        return null
    }
}

// ============================================================================
// CUSTOM AUDIO DEVICE BOTTOM SHEET
// ============================================================================

data class AudioDevice(
    val name: String,
    val type: AudioDeviceType,
    val isConnected: Boolean,
    val isActive: Boolean = false,
    val batteryLevel: Int? = null,
    val deviceId: Int? = null,
)

enum class AudioDeviceType {
    BLUETOOTH, WIRED_HEADPHONES, PHONE_SPEAKER, EXTERNAL_SPEAKER, USB_HEADSET, HDMI
}

@Composable
fun AudioDeviceBottomSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var audioDevices by remember { mutableStateOf<List<AudioDevice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var currentVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var isUserDragging by remember { mutableStateOf(false) }
    var maxVolume by remember { mutableStateOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) }

    val playerConnection = LocalPlayerConnection.current
    val service = playerConnection?.service
    var showDevicePopup by remember { mutableStateOf(false) }

    var selectedDeviceId by rememberSaveable { mutableStateOf(AudioDeviceState.preferredDeviceId) }

    fun refreshDevices() {
        var prefId: Int? = AudioDeviceState.preferredDeviceId
        
        if (prefId == null && service != null) {
            try {
                val actualService = (service as? com.jay.glossy.playback.MusicService) ?: 
                                    (service.javaClass.getMethod("getService").invoke(service) as? com.jay.glossy.playback.MusicService)
                prefId = actualService?.preferredDeviceId
            } catch (e: Exception) {}
        }
        
        loadDevices(context, prefId, onSuccess = { devices ->
            audioDevices = devices
            isLoading = false
        }, onError = { error ->
            errorMessage = error
            isLoading = false
        })
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshDevices()
    }

    DisposableEffect(Unit) {
        val volumeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC && !isUserDragging) {
                        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    }
                }
            }
        }

        val audioDeviceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshDevices()
            }
        }

        context.registerReceiver(
            volumeChangeReceiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.registerReceiver(
                audioDeviceReceiver,
                IntentFilter().apply {
                    addAction(AudioManager.ACTION_HEADSET_PLUG)
                    addAction(AudioManager.ACTION_HDMI_AUDIO_PLUG)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                    }
                }
            )
        }

        refreshDevices()

        if (!checkBluetoothPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } catch (e: Exception) {}
        }

        val handler = Handler(Looper.getMainLooper())
        val audioDeviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    refreshDevices()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    if (removedDevices?.any { it.id == selectedDeviceId } == true) {
                        selectedDeviceId = null
                        AudioDeviceState.preferredDeviceId = null
                        
                        // FIX: Notify MusicService to revert back to default device
                        val intent = Intent(context, com.jay.glossy.playback.MusicService::class.java).apply {
                            action = "SET_AUDIO_DEVICE"
                            putExtra("device_id", -1)
                        }
                        context.startService(intent)
                    }
                    refreshDevices()
                }
            }
        } else null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        }

        val bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshDevices()
                handler.postDelayed({ refreshDevices() }, 1000)
                handler.postDelayed({ refreshDevices() }, 2500)
            }
        }

        context.registerReceiver(
            bluetoothReceiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )

        val batteryPollingRunnable = object : Runnable {
            override fun run() {
                refreshDevices()
                handler.postDelayed(this, 30000)
            }
        }
        handler.postDelayed(batteryPollingRunnable, 30000)

        onDispose {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
                    audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
                }
                context.unregisterReceiver(volumeChangeReceiver)
                context.unregisterReceiver(audioDeviceReceiver)
                context.unregisterReceiver(bluetoothReceiver)
                handler.removeCallbacksAndMessages(null)
            } catch (e: IllegalArgumentException) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .animateContentSize()
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                refreshDevices()
                            }
                        ) {
                            Text(text = "Retry")
                        }
                    }
                }

                else -> {
                    val activeDevice = audioDevices.firstOrNull { it.isActive }
                    val hasMultipleDevices = audioDevices.size > 1

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp) // Exact Vivi spacing
                    ) {
                        activeDevice?.let { device ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // EXACT VIVI: Active Device Card
                                AudioDeviceActiveCard(
                                    device = device,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // EXACT VIVI: Separate Chevron Button
                                AnimatedVisibility(
                                    visible = hasMultipleDevices,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Row {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        val chevronRotation by animateFloatAsState(
                                            targetValue = if (showDevicePopup) 180f else 0f,
                                            animationSpec = tween(durationMillis = 300),
                                            label = "chevron"
                                        )
                                        Surface(
                                            onClick = { showDevicePopup = !showDevicePopup },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(72.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.ExpandMore,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .graphicsLayer { rotationZ = chevronRotation },
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // EXACT VIVI: Animated Pop-up List
                        AnimatedVisibility(
                            visible = hasMultipleDevices && showDevicePopup,
                            enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {                                 
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Audio Devices",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )

                                    audioDevices.forEach { dev ->
                                        key(dev.deviceId) {
                                            val isSelected = dev.isActive
                                            val deviceIcon = when (dev.type) {
                                                AudioDeviceType.PHONE_SPEAKER -> R.drawable.speaker_apple
                                                else -> R.drawable.headset_applemusic
                                            }
                                            
                                            Surface(
                                                onClick = {
                                                    // UPDATE UI STATE
                                                    AudioDeviceState.preferredDeviceId = dev.deviceId
                                                    selectedDeviceId = dev.deviceId
                                                    
                                                    // FIRE INTENT TO MUSIC SERVICE FOR 100% RELIABLE SWITCHING
                                                    val intent = Intent(context, com.jay.glossy.playback.MusicService::class.java).apply {
                                                        action = "SET_AUDIO_DEVICE"
                                                        putExtra("device_id", dev.deviceId ?: -1)
                                                    }
                                                    context.startService(intent)
                                                    
                                                    refreshDevices()
                                                    showDevicePopup = false
                                                },
                                                shape = CircleShape, // Fully rounded rows in expanded list
                                                color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                                modifier = Modifier.fillMaxWidth().height(64.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(horizontal = 16.dp)
                                                        .fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = deviceIcon),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Text(
                                                        text = if (dev.type == AudioDeviceType.PHONE_SPEAKER) "This Phone" else dev.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f) // pushes rest to right edge
                                                    )
                                                    
                                                    // EXACT VIVI UI: Selected Volume Icon OR Unselected Empty Circle
                                                    if (isSelected) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.volume_up),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(20.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .border(
                                                                    width = 2.dp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                                    shape = CircleShape
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // EXACT VIVI: Thick Volume Slider Row
                        VolumeControlRow(
                            label = "Volume",
                            volume = currentVolume,
                            maxVolume = maxVolume,
                            onVolumeChange = { newVolume ->
                                currentVolume = newVolume
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    newVolume.toInt(),
                                    0
                                )
                            },
                            onDragStart = { isUserDragging = true },
                            onDragEnd = { isUserDragging = false }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // EXACT VIVI: Bottom Row (Battery + OK Button)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeDevice?.type == AudioDeviceType.BLUETOOTH && activeDevice.batteryLevel != null) {
                                val density = LocalDensity.current
                                val strokeWidthPx = with(density) { 4.dp.toPx() }
                                val wavyStroke = remember(strokeWidthPx) {
                                    Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                }

                                Box(
                                    modifier = Modifier.size(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Custom Wavy Progress for battery
                                    CircularWavyProgressIndicator(
                                        progress = { activeDevice.batteryLevel.toFloat() / 100f },
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                                        stroke = wavyStroke,
                                        trackStroke = wavyStroke,
                                        gapSize = 3.dp
                                    )
                                    Text(
                                        text = "${activeDevice.batteryLevel}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Button(
                                onClick = onDismiss,
                                shape = CircleShape,
                                modifier = Modifier.height(56.dp).widthIn(min = 100.dp)
                            ) {
                                Text("OK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// EXACT VIVI ACTIVE DEVICE CARD
@Composable
private fun AudioDeviceActiveCard(
    device: AudioDevice,
    modifier: Modifier = Modifier
) {
    val deviceIcon = when (device.type) {
        AudioDeviceType.PHONE_SPEAKER -> R.drawable.speaker_apple
        else -> R.drawable.headset_applemusic
    }

    Surface(
        modifier = modifier.height(72.dp),
        shape = CircleShape, // Pill shape
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Applying your shiny new RoundedStarShape!
            val scallopShape = RoundedStarShape(sides = 8, curve = 0.10, rotation = 0f)
            
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(scallopShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = deviceIcon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (device.type == AudioDeviceType.PHONE_SPEAKER) "Phone Speaker" else device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Connected",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (device.batteryLevel != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            painter = painterResource(R.drawable.volume_up),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${device.batteryLevel}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeControlRow(
    label: String,
    volume: Float,
    maxVolume: Int,
    onVolumeChange: (Float) -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var currentValue by rememberSaveable { mutableFloatStateOf(volume) }

    LaunchedEffect(volume) {
        currentValue = volume
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            val animatedVolumeFraction by animateFloatAsState(
                targetValue = currentValue / maxVolume.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "VolumeFillAnimation"
            )

            val widthState = remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { widthState.floatValue = it.width.toFloat() }
                    .pointerInput(maxVolume) {
                        detectTapGestures { offset ->
                            val percent = (offset.x / widthState.floatValue).coerceIn(0f, 1f)
                            val newValue = percent * maxVolume
                            currentValue = newValue
                            onVolumeChange(newValue)
                        }
                    }
                    .pointerInput(maxVolume) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        ) { change, _ ->
                            change.consume()
                            val percent = (change.position.x / widthState.floatValue).coerceIn(0f, 1f)
                            val newValue = percent * maxVolume
                            currentValue = newValue
                            onVolumeChange(newValue)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedVolumeFraction)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(start = 24.dp)
                ) {
                    Icon(
                        painter = painterResource(if (currentValue > 0) R.drawable.volume_up else R.drawable.volume_off),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

private fun loadDevices(
    context: Context,
    preferredDeviceId: Int?,
    onSuccess: (List<AudioDevice>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = mutableListOf<AudioDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            audioDevices.forEach { deviceInfo ->
                val device = when (deviceInfo.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        val batteryLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val bluetoothManager = context.getSystemService(
                                        Context.BLUETOOTH_SERVICE
                                    ) as BluetoothManager
                                    val bluetoothAdapter = bluetoothManager.adapter
                                    val pairedDevices = bluetoothAdapter?.bondedDevices
                                    val btDevice = pairedDevices?.find {
                                        it.name == deviceInfo.productName.toString()
                                    }

                                    @SuppressLint("MissingPermission")
                                    val battery = btDevice?.let { device ->
                                        try {
                                            val method = android.bluetooth.BluetoothDevice::class.java.getMethod(
                                                "getBatteryLevel"
                                            )
                                            val level = method.invoke(device) as? Int
                                            level
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (battery != null && battery in 0..100) battery else null
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        } else null

                        AudioDevice(
                            name = deviceInfo.productName?.toString() ?: "Bluetooth Device",
                            type = AudioDeviceType.BLUETOOTH,
                            isConnected = true,
                            isActive = false,
                            batteryLevel = batteryLevel,
                            deviceId = deviceInfo.id
                        )
                    }

                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_AUX_LINE -> {
                        AudioDevice(
                            name = "Wired Headphones",
                            type = AudioDeviceType.WIRED_HEADPHONES,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        AudioDevice(
                            name = deviceInfo.productName?.toString() ?: "USB Audio",
                            type = AudioDeviceType.USB_HEADSET,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    AudioDeviceInfo.TYPE_HDMI -> {
                        AudioDevice(
                            name = "HDMI",
                            type = AudioDeviceType.HDMI,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                        AudioDevice(
                            name = "Phone Speaker",
                            type = AudioDeviceType.PHONE_SPEAKER,
                            isConnected = true,
                            isActive = false,
                            deviceId = deviceInfo.id
                        )
                    }
                    else -> null
                }
                device?.let { devices.add(it) }
            }

            val activeDevice = determineActiveDevice(audioManager, audioDevices, preferredDeviceId)
            val updatedDevices = devices.map { device ->
                device.copy(isActive = device.deviceId == activeDevice?.id)
            }

            val sortedDevices = updatedDevices.sortedWith(compareBy<AudioDevice> {
                when (it.type) {
                    AudioDeviceType.PHONE_SPEAKER -> 0
                    AudioDeviceType.WIRED_HEADPHONES -> 1
                    AudioDeviceType.USB_HEADSET -> 2
                    AudioDeviceType.BLUETOOTH -> 3
                    else -> 4
                }
            }.thenBy { it.name })

            onSuccess(sortedDevices.distinctBy { it.name })
        } else {
            loadDevicesLegacy(context, onSuccess, onError)
        }
    } catch (e: Exception) {
        onError("Failed to load devices: ${e.message}")
    }
}

private fun determineActiveDevice(
    audioManager: AudioManager,
    audioDevices: Array<AudioDeviceInfo>,
    preferredDeviceId: Int?
): AudioDeviceInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val preferred = if (preferredDeviceId != null) {
            audioDevices.find { it.id == preferredDeviceId }
        } else null

        preferred ?: when {
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } ->
                audioDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            audioDevices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                        it.type == AudioDeviceInfo.TYPE_AUX_LINE
            } ->
                audioDevices.find {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                            it.type == AudioDeviceInfo.TYPE_AUX_LINE
                }
            else -> audioDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        }
    } else null

@Suppress("DEPRECATION")
private fun loadDevicesLegacy(context: Context, onSuccess: (List<AudioDevice>) -> Unit, onError: (String) -> Unit) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val devices = mutableListOf<AudioDevice>()

    if (audioManager.isBluetoothA2dpOn) {
        devices.add(AudioDevice("Bluetooth Device", AudioDeviceType.BLUETOOTH, true, true))
    }
    if (audioManager.isWiredHeadsetOn) {
        devices.add(AudioDevice("Wired Headphones", AudioDeviceType.WIRED_HEADPHONES, true, true))
    }
    if (audioManager.isSpeakerphoneOn) {
        devices.add(AudioDevice("External Speaker", AudioDeviceType.EXTERNAL_SPEAKER, true, true))
    }
    if (devices.isEmpty() || !devices.any { it.isActive }) {
        devices.add(AudioDevice("Phone Speaker", AudioDeviceType.PHONE_SPEAKER, true, true))
    }
    onSuccess(devices.filter { it.isActive }.take(1))
}

private fun checkBluetoothPermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
} else true
