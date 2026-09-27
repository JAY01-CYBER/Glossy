/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Network policy for animated canvases.
 *
 * A canvas is a looping video, so on a metered connection it costs real data
 * every time a track starts. [rememberCanvasEnabled] is the single place that
 * answers "may this screen fetch and play a canvas right now?", combining the
 * master "Canvas Background" switch with [CanvasOnMobileDataKey]:
 *
 *  - Canvas Background off            -> never.
 *  - Canvas Background on, option on  -> always, Wi-Fi or mobile data.
 *  - Canvas Background on, option off -> only while the connection is unmetered
 *    (Wi-Fi/ethernet); on mobile data the static artwork simply stays.
 *
 * Wi-Fi that is explicitly marked metered (a phone hotspot) counts as metered,
 * which is exactly the billing semantic the user is opting into.
 */

package com.jay.glossy.ui.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.jay.glossy.constants.CanvasOnMobileDataKey
import com.jay.glossy.constants.CanvasThumbnailAnimationKey
import com.jay.glossy.utils.rememberPreference

/**
 * Synchronous check: true when the active connection is billed per byte
 * (mobile data, or a metered hotspot). Used off the composition, e.g. by the
 * service that prefetches canvases for the next track.
 */
fun Context.isMeteredConnection(): Boolean =
    try {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityManager?.isActiveNetworkMetered ?: false
    } catch (e: Exception) {
        false
    }

/**
 * The same check as state that recomposes when the connection changes, so a
 * canvas starts as soon as the phone lands on Wi-Fi and stops when it drops
 * back to mobile data.
 */
@Composable
fun rememberIsMeteredConnection(): Boolean {
    val context = LocalContext.current
    val connectivityManager =
        remember(context) {
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        }
    var metered by remember(connectivityManager) { mutableStateOf(context.isMeteredConnection()) }

    DisposableEffect(connectivityManager) {
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    metered = context.isMeteredConnection()
                }

                override fun onLost(network: Network) {
                    metered = context.isMeteredConnection()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    metered = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                }
            }
        try {
            connectivityManager?.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            // No callback registered: the synchronous read below still gives a
            // sane answer, it just will not follow a network switch live.
        }
        metered = context.isMeteredConnection()
        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
            }
        }
    }

    return metered
}

/**
 * Whether animated canvases may load on this screen right now. Replaces a
 * direct read of [CanvasThumbnailAnimationKey] on every canvas surface so the
 * player artwork, the Apple Music design and the blurred app backdrop all obey
 * the same rule.
 */
@Composable
fun rememberCanvasEnabled(): Boolean {
    val (canvasEnabled) = rememberPreference(CanvasThumbnailAnimationKey, defaultValue = false)
    val (canvasOnMobileData) = rememberPreference(CanvasOnMobileDataKey, defaultValue = false)
    // With the feature off there is nothing to decide, and the connectivity
    // listener is not needed either — skip it so an idle player never holds a
    // network callback.
    if (!canvasEnabled) return false
    // "Allow mobile data" makes the connection type irrelevant.
    if (canvasOnMobileData) return true
    return !rememberIsMeteredConnection()
}
