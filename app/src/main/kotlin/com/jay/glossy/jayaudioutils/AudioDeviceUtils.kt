/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// ============================================================================
// BLUETOOTH & DEVICE DETECTION UTILS
// ============================================================================

fun isBluetoothHeadphoneConnected(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        audioDevices.any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    } else {
        @Suppress("DEPRECATION")
        audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
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
// CUSTOM AUDIO DEVICE BOTTOM SHEET (VIVI DESIGN)
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadDevices(context, null, onSuccess = { devices ->
                audioDevices = devices
                isLoading = false
            }, onError = { error ->
                errorMessage = error
                isLoading = false
            })
        } else {
            errorMessage = "Bluetooth permission required"
            isLoading = false
        }
    }

    fun refreshDevices() {
        loadDevices(context, null, onSuccess = { devices ->
            audioDevices = devices
        }, onError = {})
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

        context.registerReceiver(volumeChangeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))

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

        if (checkBluetoothPermission(context)) {
            loadDevices(context, null, onSuccess = { devices ->
                audioDevices = devices
                isLoading = false
            }, onError = { error ->
                errorMessage = error
                isLoading = false
            })
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        val handler = Handler(Looper.getMainLooper())
        val audioDeviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    refreshDevices()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .animateContentSize()
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Error, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            errorMessage = null
                            isLoading = true
                            refreshDevices()
                        }) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                }
                else -> {
                    val activeDevice = audioDevices.firstOrNull { it.isActive }
                    val hasBluetooth = audioDevices.any { it.type == AudioDeviceType.BLUETOOTH }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        activeDevice?.let { device ->
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AudioDeviceRow(
                                        device = device,
                                        currentVolume = currentVolume,
                                        maxVolume = maxVolume,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    AnimatedVisibility(
                                        visible = hasBluetooth,
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = fadeOut() + shrinkHorizontally()
                                    ) {
                                        val chevronRotation by animateFloatAsState(targetValue = if (showDevicePopup) 180f else 0f, label = "chevron")
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 12.dp)
                                        ) {
                                            Surface(
                                                onClick = { showDevicePopup = !showDevicePopup },
                                                shape = CircleShape,
                                                color = if (device.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                tonalElevation = 2.dp,
                                                modifier = Modifier.size(72.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Filled.ExpandMore, contentDescription = null,
                                                        modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = chevronRotation },
                                                        tint = if (device.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = hasBluetooth && showDevicePopup,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {                                 
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(
                                        text = "Audio Devices",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                    )

                                    audioDevices.forEachIndexed { index, dev ->
                                        key(dev.deviceId) {
                                            val isSelected = dev.isActive
                                            val deviceIcon = when (dev.type) {
                                                AudioDeviceType.BLUETOOTH -> Icons.Filled.Bluetooth
                                                AudioDeviceType.WIRED_HEADPHONES -> Icons.Filled.Headphones
                                                AudioDeviceType.USB_HEADSET -> Icons.Filled.Usb
                                                AudioDeviceType.HDMI -> Icons.Filled.Tv
                                                AudioDeviceType.EXTERNAL_SPEAKER -> Icons.Filled.Speaker
                                                AudioDeviceType.PHONE_SPEAKER -> Icons.Filled.PhoneAndroid
                                            }
                                            
                                            val itemShape = remember(index, audioDevices.size) {
                                                when {
                                                    audioDevices.size == 1 -> RoundedCornerShape(24.dp)
                                                    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                                    index == audioDevices.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                                    else -> RoundedCornerShape(4.dp)
                                                }
                                            }

                                            Surface(
                                                onClick = {
                                                    try {
                                                        service?.javaClass?.getMethod("setPreferredAudioDevice", Int::class.javaObjectType)?.invoke(service, dev.deviceId)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                    refreshDevices()
                                                    showDevicePopup = false
                                                },
                                                shape = itemShape,
                                                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    Icon(deviceIcon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        text = if (dev.type == AudioDeviceType.PHONE_SPEAKER) "This Phone" else dev.name,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                                    )
                                                    if (isSelected) {
                                                        Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        VolumeControlRow(
                            label = stringResource(R.string.volume),
                            icon = Icons.Filled.MusicNote,
                            volume = currentVolume,
                            maxVolume = maxVolume,
                            onVolumeChange = { newVolume ->
                                currentVolume = newVolume
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume.toInt(), 0)
                            },
                            onDragStart = { isUserDragging = true },
                            onDragEnd = { isUserDragging = false }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeDevice?.type == AudioDeviceType.BLUETOOTH && activeDevice.batteryLevel != null) {
                                Box(
                                    modifier = Modifier.padding(start = 8.dp).size(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { activeDevice.batteryLevel.toFloat() / 100f },
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                                        strokeWidth = 4.dp,
                                    )
                                    Text(
                                        text = "${activeDevice.batteryLevel}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Button(onClick = onDismiss, shape = RoundedCornerShape(24.dp)) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeControlRow(
    label: String,
    icon: ImageVector,
    volume: Float,
    maxVolume: Int,
    onVolumeChange: (Float) -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var currentValue by rememberSaveable { mutableFloatStateOf(volume) }
    
    LaunchedEffect(volume) {
        currentValue = volume
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            val animatedVolumeFraction by animateFloatAsState(
                targetValue = currentValue / maxVolume.toFloat(),
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
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
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedVolumeFraction)
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
                        imageVector = if (currentValue > 0) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = null,
                        tint = if (currentValue / maxVolume > 0.2f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (currentValue / maxVolume > 0.4f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(6.dp)
                        .background(
                            color = if (currentValue / maxVolume > 0.95f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

private fun loadDevices(context: Context, preferredDeviceId: Int?, onSuccess: (List<AudioDevice>) -> Unit, onError: (String) -> Unit) {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = mutableListOf<AudioDevice>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            audioDevices.forEach { deviceInfo ->
                val device = when (deviceInfo.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                        val batteryLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                                    val bluetoothAdapter = bluetoothManager.adapter
                                    val btDevice = bluetoothAdapter?.bondedDevices?.find { it.name == deviceInfo.productName.toString() }

                                    @SuppressLint("MissingPermission")
                                    val battery = btDevice?.let { dev ->
                                        try {
                                            val method = android.bluetooth.BluetoothDevice::class.java.getMethod("getBatteryLevel")
                                            method.invoke(dev) as? Int
                                        } catch (e: Exception) { null }
                                    }
                                    if (battery != null && battery in 0..100) battery else null
                                } else null
                            } catch (e: Exception) { null }
                        } else null

                        AudioDevice(
                            name = deviceInfo.productName?.toString() ?: "Bluetooth Device",
                            type = AudioDeviceType.BLUETOOTH, isConnected = true, isActive = false, batteryLevel = batteryLevel, deviceId = deviceInfo.id
                        )
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioDevice("Wired Headphones", AudioDeviceType.WIRED_HEADPHONES, true, false, null, deviceInfo.id)
                    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> AudioDevice(deviceInfo.productName?.toString() ?: "USB Audio", AudioDeviceType.USB_HEADSET, true, false, null, deviceInfo.id)
                    AudioDeviceInfo.TYPE_HDMI -> AudioDevice("HDMI", AudioDeviceType.HDMI, true, false, null, deviceInfo.id)
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDevice("Phone Speaker", AudioDeviceType.PHONE_SPEAKER, true, false, null, deviceInfo.id)
                    else -> null
                }
                device?.let { devices.add(it) }
            }

            val activeDevice = determineActiveDevice(audioManager, audioDevices, preferredDeviceId)
            val updatedDevices = devices.map { device -> device.copy(isActive = device.deviceId == activeDevice?.id) }

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
            loadDevicesLegacy(context, onSuccess)
        }
    } catch (e: Exception) {
        onError("Failed to load devices: ${e.message}")
    }
}

private fun determineActiveDevice(audioManager: AudioManager, audioDevices: Array<AudioDeviceInfo>, preferredDeviceId: Int?): AudioDeviceInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val preferred = if (preferredDeviceId != null) audioDevices.find { it.id == preferredDeviceId } else null
        preferred ?: when {
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP } -> audioDevices.find { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET } -> audioDevices.find { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }
            else -> audioDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        }
    } else null

@Suppress("DEPRECATION")
private fun loadDevicesLegacy(context: Context, onSuccess: (List<AudioDevice>) -> Unit) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val devices = mutableListOf<AudioDevice>()
    if (audioManager.isBluetoothA2dpOn) devices.add(AudioDevice("Bluetooth Device", AudioDeviceType.BLUETOOTH, true, true))
    if (audioManager.isWiredHeadsetOn) devices.add(AudioDevice("Wired Headphones", AudioDeviceType.WIRED_HEADPHONES, true, true))
    if (audioManager.isSpeakerphoneOn) devices.add(AudioDevice("External Speaker", AudioDeviceType.EXTERNAL_SPEAKER, true, true))
    if (devices.isEmpty() || !devices.any { it.isActive }) devices.add(AudioDevice("Phone Speaker", AudioDeviceType.PHONE_SPEAKER, true, true))
    onSuccess(devices.filter { it.isActive }.take(1))
}

private fun checkBluetoothPermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
} else true

@Composable
private fun AudioDeviceRow(device: AudioDevice, currentVolume: Float, maxVolume: Int, modifier: Modifier = Modifier) {
    val isActiveDevice = device.isActive
    val containerColor = if (isActiveDevice) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val onContainer = if (isActiveDevice) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val scallopShape = RoundedCornerShape(16.dp)

    val backgroundScale by animateFloatAsState(targetValue = if (isActiveDevice) 1.10f else 1f, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing), label = "activeDeviceScale")

    val deviceIcon = when (device.type) {
        AudioDeviceType.BLUETOOTH -> Icons.Filled.Bluetooth
        AudioDeviceType.WIRED_HEADPHONES -> Icons.Filled.Headphones
        AudioDeviceType.USB_HEADSET -> Icons.Filled.Usb
        AudioDeviceType.HDMI -> Icons.Filled.Tv
        AudioDeviceType.EXTERNAL_SPEAKER -> Icons.Filled.Speaker
        else -> Icons.Filled.Speaker
    }

    Surface(modifier = modifier.clip(CircleShape), color = containerColor, tonalElevation = 2.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(52.dp).padding(start = 4.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.matchParentSize().graphicsLayer(scaleX = backgroundScale, scaleY = backgroundScale).background(color = onContainer.copy(alpha = 0.12f), shape = if (isActiveDevice) scallopShape else CircleShape))
                Icon(imageVector = deviceIcon, contentDescription = null, tint = onContainer, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = onContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                val statusText = if (isActiveDevice) "Connected" else "Available"
                Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(onContainer.copy(alpha = 0.08f)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = statusText, maxLines = 1, style = MaterialTheme.typography.labelMedium, overflow = TextOverflow.Ellipsis, color = onContainer)
                }
            }

            if (isActiveDevice) {
                val value = ((currentVolume / maxVolume) * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 4.dp)) {
                    Icon(imageVector = Icons.Filled.VolumeUp, contentDescription = "Volume level", tint = onContainer, modifier = Modifier.size(14.dp))
                    Text(text = "$value%", style = MaterialTheme.typography.labelSmall, color = onContainer)
                }
            }
        }
    }
}
