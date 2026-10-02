/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Network policy for animated canvases.
 *
 * A canvas is a looping video, so on mobile data it costs real data every time
 * a track starts. [rememberCanvasEnabled] is the single place that answers "may
 * this screen fetch and play a canvas right now?", combining the master "Canvas
 * Background" switch with [CanvasOnMobileDataKey]:
 *
 *  - Canvas Background off            -> never.
 *  - Canvas Background on, option on  -> always, Wi-Fi or mobile data. This is
 *    the default: a switch the user turned on to get animated artwork should
 *    not silently do nothing because the phone is off Wi-Fi.
 *  - Canvas Background on, option off -> only while the connection is not
 *    mobile data; on cellular the static artwork simply stays.
 *
 * "Is this mobile data?" is answered by the connection's *transport*, not by
 * [ConnectivityManager.isActiveNetworkMetered] — see
 * [isMobileDataConnection], which is where the canvas that loaded on one song
 * and not on the next used to come from.
 *
 * [rememberSpotlightCanvasEnabled] answers the same question for the home
 * screen's Featured Spotlight carousel, which has its own switch (on by
 * default) but shares this one's metered-connection rule.
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jay.glossy.constants.CanvasCornerSizeKey
import com.jay.glossy.constants.CanvasOnMobileDataKey
import com.jay.glossy.constants.CanvasPreloadKey
import com.jay.glossy.constants.DefaultCanvasCornerSize
import com.jay.glossy.constants.MaxCanvasCornerSize
import com.jay.glossy.constants.MinCanvasCornerSize
import com.jay.glossy.constants.CanvasThumbnailAnimationKey
import com.jay.glossy.constants.SpotlightCanvasKey
import com.jay.glossy.utils.rememberPreference

/**
 * True when the active connection is mobile data: a cellular transport that
 * Wi-Fi or ethernet is not carrying.
 *
 * Deliberately not [ConnectivityManager.isActiveNetworkMetered]. That answers a
 * broader question, and two of its answers are wrong for this switch:
 *
 *  - It reports "metered" for Wi-Fi the system has flagged as metered (a phone
 *    hotspot, a captive portal) and for VPNs — so "Wi-Fi only" would refuse to
 *    load a canvas while on Wi-Fi.
 *  - With **no active network** — the second or two while a connection is being
 *    established, revalidated, or after a blip — it presumes the worst and
 *    answers "metered". Read there it blocks canvases on a phone that is on
 *    Wi-Fi, which is exactly the canvas that appears on one song and silently
 *    does not on the next.
 *
 * Asking about the transport instead poses the question the switch actually
 * asks: is this mobile data or Wi-Fi? Unknown (no active network) answers
 * false, so a canvas is never refused because the radio happened to be
 * mid-handshake; the download simply fails and is retried as usual.
 *
 * Used off the composition too, e.g. by the service that prefetches canvases
 * for the next track.
 */
fun Context.isMobileDataConnection(): Boolean =
    try {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityManager
            ?.let { manager -> manager.getNetworkCapabilities(manager.activeNetwork) }
            .isMobileDataTransport()
    } catch (e: Exception) {
        false
    }

/**
 * Cellular that nothing else is carrying. Also used for a connectivity
 * callback's capabilities, which describe one network rather than "active".
 */
private fun NetworkCapabilities?.isMobileDataTransport(): Boolean =
    this != null &&
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
        !hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
        !hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

/**
 * The same check as state that recomposes when the connection changes, so a
 * canvas starts as soon as the phone lands on Wi-Fi and stops when it drops
 * back to mobile data.
 */
@Composable
fun rememberIsMobileDataConnection(): Boolean {
    val context = LocalContext.current
    val connectivityManager =
        remember(context) {
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        }
    var mobileData by remember(connectivityManager) { mutableStateOf(context.isMobileDataConnection()) }

    DisposableEffect(connectivityManager) {
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    mobileData = context.isMobileDataConnection()
                }

                override fun onLost(network: Network) {
                    mobileData = context.isMobileDataConnection()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    mobileData = networkCapabilities.isMobileDataTransport()
                }
            }
        try {
            connectivityManager?.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            // No callback registered: the synchronous read below still gives a
            // sane answer, it just will not follow a network switch live.
        }
        mobileData = context.isMobileDataConnection()
        onDispose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
            }
        }
    }

    return mobileData
}

/**
 * Whether animated canvases may load on this screen right now. Replaces a
 * direct read of [CanvasThumbnailAnimationKey] on every canvas surface so the
 * player artwork, the Apple Music design and the Featured Spotlight carousel
 * all obey the same rule.
 *
 * Note what is *not* on that list: the frosted app backdrop. A canvas is
 * artwork you look at; a backdrop is a pane the app is read through, so the
 * backdrop stays on the blurred still artwork and never resolves or decodes a
 * clip (see `BackgroundBlurBackdrop`).
 */
@Composable
fun rememberCanvasEnabled(): Boolean {
    val (canvasEnabled) = rememberPreference(CanvasThumbnailAnimationKey, defaultValue = false)
    val (canvasOnMobileData) = rememberPreference(CanvasOnMobileDataKey, defaultValue = true)
    // With the feature off there is nothing to decide, and the connectivity
    // listener is not needed either — skip it so an idle player never holds a
    // network callback.
    if (!canvasEnabled) return false
    // "Allow mobile data" (on by default) makes the connection type
    // irrelevant.
    if (canvasOnMobileData) return true
    return !rememberIsMobileDataConnection()
}

/**
 * The same rule for the Featured Spotlight carousel, whose master switch is
 * [SpotlightCanvasKey] instead of the player's "Canvas Background". The
 * mobile-data policy is shared: one "load canvases on mobile data" switch
 * governs every surface a canvas can appear on, so a user on a data cap does
 * not have to police two of them.
 */
@Composable
fun rememberSpotlightCanvasEnabled(): Boolean {
    val (spotlightCanvas) = rememberPreference(SpotlightCanvasKey, defaultValue = true)
    // Off means nothing is resolved, downloaded or decoded for the carousel;
    // skip the connectivity listener too.
    if (!spotlightCanvas) return false
    val (canvasOnMobileData) = rememberPreference(CanvasOnMobileDataKey, defaultValue = true)
    if (canvasOnMobileData) return true
    return !rememberIsMobileDataConnection()
}

/**
 * Whether canvases may be prepared ahead of being shown: the provider lookup
 * and the download of an upcoming clip's opening seconds both happen early, so
 * the card (or the next track) starts on its first frame instead of buffering.
 *
 * Read here rather than at each prefetch site so the carousel, the player and
 * the service all obey one switch.
 */
@Composable
fun rememberCanvasPreloadEnabled(): Boolean = rememberPreference(CanvasPreloadKey, defaultValue = true).value

/**
 * The user's "allow canvases on mobile data" answer, for callers that have to
 * make a metered-connection decision of their own — the preloader, which would
 * otherwise spend the same data a download does, just earlier.
 */
@Composable
fun rememberCanvasOnMobileData(): Boolean = rememberPreference(CanvasOnMobileDataKey, defaultValue = true).value

/**
 * Corner radius, in dp, of every surface a canvas is drawn on — the player
 * artwork while its canvas is showing, and the Featured Spotlight cards.
 *
 * Read here rather than at each call site so the Appearance slider, the player
 * and the carousel can never disagree, and clamped so a bad stored value cannot
 * turn the card into a circle or a negative radius.
 */
@Composable
fun rememberCanvasCornerSize(): Dp =
    rememberPreference(CanvasCornerSizeKey, defaultValue = DefaultCanvasCornerSize)
        .value
        .coerceIn(MinCanvasCornerSize, MaxCanvasCornerSize)
        .dp
