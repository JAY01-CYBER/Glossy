/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import com.jay.glossy.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.EnableGoogleCastKey
import com.jay.glossy.utils.rememberPreference
import timber.log.Timber

/**
 * A Composable Cast button that shows available Cast devices.
 * Uses the app's MenuState to show a styled bottom sheet.
 *
 * The route list comes from [com.jay.glossy.playback.CastConnectionHandler],
 * which owns the discovery callback and starts a scan when the button is shown
 * and again every time the picker opens. Discovering only from this composable
 * made the picker open before mDNS had answered and list no TVs at all.
 */
@Composable
fun CastButton(
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current

    var castAvailable by remember { mutableStateOf(false) }

    val (enableGoogleCast) = rememberPreference(
        key = EnableGoogleCastKey,
        defaultValue = true
    )

    // Get cast state from service. Keep the State objects rather than their
    // current values: the picker reads them inside its own composition so
    // devices found by mDNS after the sheet opens still show up.
    val castHandler = playerConnection?.service?.castConnectionHandler
    val isCasting = castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val isConnecting = castHandler?.isConnecting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val availableRoutes = castHandler?.availableRoutes?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf<List<MediaRouter.RouteInfo>>(emptyList()) }

    // Check if Cast is available and disconnect if disabled while casting
    LaunchedEffect(enableGoogleCast, castHandler) {
        if (!enableGoogleCast) {
            // Disconnect from Cast if currently casting
            if (isCasting.value) {
                castHandler?.disconnect()
            }
            castHandler?.stopDiscovery()
            castAvailable = false
            return@LaunchedEffect
        }
        castAvailable = try {
            CastContext.getSharedInstance(context)
            // initialize() is idempotent, starts discovery, and refreshes the
            // route list if it is already running, so a TV that appeared while
            // the player was closed is still listed.
            castHandler?.initialize() == true
        } catch (e: Exception) {
            Timber.d("Cast not available: ${e.message}")
            false
        }
    }

    // Show the button if Cast is enabled and the SDK is available
    if (enableGoogleCast && castAvailable) {
        Box(
            modifier = modifier
        ) {
            // Shadow background for cast button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Cast button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                    // Ask for a fresh scan before showing the sheet so a TV that
                    // was not in the router's cache still gets a chance to answer.
                    castHandler?.refreshDiscovery()

                    // Show bottom sheet with cast picker
                    menuState.show {
                        CastPickerSheet(
                            routes = availableRoutes.value,
                            isConnecting = isConnecting.value,
                            currentlyConnectedRoute = if (isCasting.value) castHandler?.selectedRoute else null,
                            onRouteSelected = { route ->
                                castHandler?.connectToRoute(route)
                                menuState.dismiss()
                            },
                            onDisconnect = {
                                castHandler?.disconnect()
                                menuState.dismiss()
                            },
                            onRefresh = { castHandler?.refreshDiscovery() }
                        )
                    }
                }
            ) {
                Image(
                    painter = painterResource(
                        if (isCasting.value) R.drawable.cast_connected else R.drawable.cast
                    ),
                    contentDescription = if (isCasting.value) "Stop casting" else "Cast",
                    colorFilter = ColorFilter.tint(
                        if (isCasting.value) MaterialTheme.colorScheme.primary else tintColor
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
