/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.lossless

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build

/**
 * What the phone is plugged into, and whether that path can carry a lossless
 * file without something along the way re-encoding or resampling it.
 *
 * The point is not to grade the listener's gear but to stop the app claiming
 * something it cannot deliver: a 24/96 FLAC played to Bluetooth earbuds is
 * lossless only as far as the phone's encoder, and an honest settings row is
 * worth more than a "Hi-Res" badge. The direct-path query is Android 13's own
 * (`AudioManager.getDirectPlaybackSupport`), so the answer comes from the
 * audio HAL rather than from a guess about the device.
 */
object LosslessOutput {
    data class Report(
        /** "USB audio", "Wired output", "Bluetooth", "Speaker" … */
        val route: String,
        /** Whether that route can carry the samples through unmangled. */
        val carriesLossless: Boolean,
        /** True/false where Android 13+ can answer, null below that. */
        val directSupported: Boolean?,
        /** One sentence a listener can act on. */
        val note: String,
    )

    fun report(context: Context): Report {
        val manager = context.getSystemService(AudioManager::class.java)
            ?: return Report(
                route = "Unknown",
                carriesLossless = false,
                directSupported = null,
                note = "The audio service is not available.",
            )

        val outputs =
            runCatching { manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { it.isSink } }
                .getOrDefault(emptyList())
        val (route, carries) = classify(outputs)
        val direct = directSupport(manager)

        val note =
            when {
                !carries && route == BLUETOOTH_ROUTE ->
                    "Bluetooth re-encodes what it sends, so a FLAC played to these stays lossy on the way out."
                !carries ->
                    "This output has no lossless path: Android resamples to its own rate, usually 48 kHz."
                direct == true ->
                    "A direct path is available at 24-bit/96 kHz, so these samples can reach the converter without the mixer's resample."
                direct == false ->
                    "No direct path is reported: Android's mixer will resample to its own rate, usually 48 kHz."
                else ->
                    "Direct-path reporting needs Android 13 or newer; below that the mixer decides."
            }

        return Report(route = route, carriesLossless = carries, directSupported = direct, note = note)
    }

    private val BLUETOOTH_ROUTE = "Bluetooth"

    /** The first route in priority order: what is actually playing out now. */
    private fun classify(outputs: List<AudioDeviceInfo>): Pair<String, Boolean> {
        fun anyOf(types: Set<Int>) = outputs.any { it.type in types }
        return when {
            anyOf(USB) -> "USB audio" to true
            anyOf(WIRED) -> "Wired output" to true
            anyOf(HDMI) -> "HDMI" to true
            anyOf(DOCK) -> "Dock" to true
            anyOf(BLUETOOTH) -> BLUETOOTH_ROUTE to false
            else -> "Speaker" to false
        }
    }

    private fun directSupport(manager: AudioManager): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val format =
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_24BIT_PACKED)
                .setSampleRate(96_000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
        val attributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        // A bitfield of the direct forms this format gets, so zero —
        // DIRECT_PLAYBACK_NOT_SUPPORTED — is the only "no" answer.
        val modes = manager.getDirectPlaybackSupport(format, attributes)
        return modes != AudioManager.DIRECT_PLAYBACK_NOT_SUPPORTED
    }

    // Device types as plain ints: the newer ones (eARC, BLE, dock analog) are
    // compile-time constants that older phones simply never report, and naming
    // them by value keeps this readable without an API guard per set.
    private val USB =
        setOf(
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
        )
    private val WIRED =
        setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_AUX_LINE,
        )
    private val HDMI = setOf(AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, 29)
    private val DOCK = setOf(AudioDeviceInfo.TYPE_DOCK, 31)
    private val BLUETOOTH =
        setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            26, // TYPE_BLE_HEADSET, API 31
            27, // TYPE_BLE_SPEAKER, API 31
        )
}
