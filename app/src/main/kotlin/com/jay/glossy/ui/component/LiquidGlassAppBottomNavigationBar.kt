/**
 * Glossy Project (C) 2026
 * Liquid Glass Navigation Bar
 */
package com.jay.glossy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.ui.player.MiniPlayer

// Generic Screens for Glossy
sealed class BottomNavScreen(val ordinal: Int, val route: String, val icon: @Composable () -> Unit) {
    data object Home : BottomNavScreen(0, "home", { Icon(Icons.Default.Home, contentDescription = null) })
    data object Search : BottomNavScreen(1, "search", { Icon(Icons.Default.Search, contentDescription = null) })
    data object Library : BottomNavScreen(2, "library", { Icon(Icons.Default.LibraryMusic, contentDescription = null) })
}

@Composable
fun LiquidGlassAppBottomNavigationBar(
    navController: NavController,
    backdrop: com.kyant.backdrop.Backdrop,
    isScrolledToTop: Boolean = true,
    onOpenNowPlaying: () -> Unit,
    positionState: MutableLongState,
    durationState: MutableLongState,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    
    // Check if song is playing to show MiniPlayer
    val isShowMiniPlayer = mediaMetadata != null

    val layer = rememberGraphicsLayer()
    val toolbarInteraction = rememberGlassInteraction()
    val searchFabInteraction = rememberGlassInteraction()
    val luminanceAnimation = remember { Animatable(0.5f) } // Default luminance

    val bottomNavScreens = listOf(BottomNavScreen.Home, BottomNavScreen.Library, BottomNavScreen.Search)
    val barTabs = listOf(BottomNavScreen.Home, BottomNavScreen.Library)

    var selectedIndex by remember { mutableIntStateOf(0) }
    var isExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(isScrolledToTop) {
        isExpanded = isScrolledToTop
    }

    // Standard Compose Column (No ConstraintLayout needed)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(bottom = 8.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. MiniPlayer Attached on Top ---
        AnimatedVisibility(visible = isShowMiniPlayer) {
            MiniPlayer(
                positionState = positionState,
                durationState = durationState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
                    .liquidGlass(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(32.dp)
                    ),
                onClick = onOpenNowPlaying
            )
        }

        // --- 2. Liquid Glass Bottom Bar ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isExpanded) 16.dp else 0.dp)
        ) {
            if (isExpanded) {
                BoxWithConstraints(Modifier.weight(1f, fill = false)) {
                    LiquidGlassTabBar(
                        tabs = barTabs,
                        selectedTab = barTabs.indexOfFirst { it.ordinal == selectedIndex }.coerceAtLeast(0),
                        backdrop = backdrop,
                        layer = layer,
                        luminance = luminanceAnimation.value,
                        availableWidth = maxWidth,
                        onTabSelected = { position ->
                            selectedIndex = barTabs[position].ordinal
                            try { navController.navigate(barTabs[position].route) } catch (e: Exception) { }
                        },
                    )
                }
                Spacer(Modifier.size(12.dp))
                // Search FAB
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .drawInteractiveGlass(
                            isDark = true,
                            backdrop = backdrop,
                            layer = layer,
                            luminanceAnimation = luminanceAnimation.value,
                            shape = CircleShape,
                            interaction = searchFabInteraction
                        )
                        .clickable {
                            selectedIndex = BottomNavScreen.Search.ordinal
                            try { navController.navigate(BottomNavScreen.Search.route) } catch (e: Exception) { }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BottomNavScreen.Search.icon()
                }
            } else {
                val selectedScreen = bottomNavScreens.find { it.ordinal == selectedIndex } ?: BottomNavScreen.Home
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .drawInteractiveGlass(
                            isDark = true,
                            backdrop = backdrop,
                            layer = layer,
                            luminanceAnimation = luminanceAnimation.value,
                            shape = CircleShape,
                            interaction = toolbarInteraction
                        )
                        .clickable { isExpanded = true },
                    contentAlignment = Alignment.Center,
                ) {
                    selectedScreen.icon()
                }
            }
        }
    }
}
