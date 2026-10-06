@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.jay.glossy.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jay.glossy.constants.ActiveDesignStyle
import com.jay.glossy.constants.DesignStyle
import com.jay.glossy.constants.UseFloatingNavBarKey
import com.jay.glossy.ui.screens.Screens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@Immutable
private data class NavItemState(
    val isSelected: Boolean,
    val iconRes: Int
)

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

// ----------------------------------------------------
// Navigation Rail (For Tablet/Landscape Mode)
// ----------------------------------------------------
@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val interactionSource = remember { MutableInteractionSource() }

            if (isSearchItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSearchLongClick.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ----------------------------------------------------
// Main Navigation Bar Router
// ----------------------------------------------------
@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null
) {
    val (useFloatingNavBar) = rememberPreference(UseFloatingNavBarKey, defaultValue = true)
    // Pinned to the classic look: the redesigned bar is no longer selectable.
    val newDesign = ActiveDesignStyle == DesignStyle.TEAL

    if (newDesign) {
        GlossyBottomNav(
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            modifier = modifier
        )
    } else if (useFloatingNavBar) {
        FloatingAppNavigationBar(
            // The floating bar draws Search as its own pill beside the tab pill
            // rather than as a tab, so the list it is handed (the tab set,
            // which has no Search in it) is not enough on its own — without
            // this the lookup came back empty and the pill never appeared.
            navigationItems = navigationItems.withSearchPill(),
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            modifier = modifier,
            pureBlack = pureBlack,
            slimNav = slimNav,
            onSearchLongClick = onSearchLongClick
        )
    } else {
        StandardAppNavigationBar(
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onItemClick = onItemClick,
            modifier = modifier,
            pureBlack = pureBlack,
            slimNav = slimNav,
            onSearchLongClick = onSearchLongClick
        )
    }
}

// ----------------------------------------------------
// New design system: flat four-item bar
// ----------------------------------------------------
@Composable
fun GlossyBottomNav(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GlossyPalette.Page)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationItems.forEach { screen ->
            val selected = isRouteSelected(currentRoute, screen.route, navigationItems)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onItemClick(screen, selected) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (selected) screen.iconIdActive else screen.iconIdInactive
                    ),
                    contentDescription = stringResource(screen.titleId),
                    tint = if (selected) GlossyPalette.Accent else GlossyPalette.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(screen.titleId),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) GlossyPalette.Accent else GlossyPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The floating bar renders Search as a separate pill, not a tab, and the tab set
 * the app passes in ([Screens.MainScreens]) does not contain it. Appending it
 * here keeps the floating bar's own split (tabs vs. pill) working while the
 * standard bar keeps showing exactly the tabs it was given.
 */
private fun List<Screens>.withSearchPill(): List<Screens> =
    if (contains(Screens.Search)) this else this + Screens.Search

// ----------------------------------------------------
// Premium MD3 Liquid Navigation Bar (Squash & Stretch)
// ----------------------------------------------------

/** The gap that separates the floating search button from the navigation pill. */
private val SearchPillGap = 12.dp

/** Bounds one tab may occupy inside the navigation pill. */
private val TabMinWidth = 62.dp
private val TabMaxWidth = 80.dp

@Composable
private fun FloatingAppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    val searchItem = navigationItems.find { it == Screens.Search }
    val mainItems = navigationItems.filter { it != Screens.Search }

    val selectedMainIndex = mainItems.indexOfFirst { screen ->
        isRouteSelected(currentRoute, screen.route, navigationItems)
    }
    
    var lastMainIndex by remember { mutableIntStateOf(maxOf(0, selectedMainIndex)) }
    LaunchedEffect(selectedMainIndex) {
        if (selectedMainIndex >= 0) {
            lastMainIndex = selectedMainIndex
        }
    }

    val barHeight = if (slimNav) 48.dp else 56.dp 
    // Deliberately shorter than the tab pill: search is a secondary control
    // sitting beside the bar, not a fifth tab wedged into it, and the smaller
    // footprint hands the tabs their width back.
    val searchSize = if (slimNav) 42.dp else 46.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp), 
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The tabs and the search pill share whatever width the bar is given:
        // the tabs flex down toward icon width so the pill always fits beside
        // the navigation pill, however narrow the screen.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val gap = if (searchItem != null) SearchPillGap else 0.dp
            val tabCount = mainItems.size.coerceAtLeast(1)
            val tabWidth = ((maxWidth - searchSize - gap) / tabCount)
                .coerceIn(TabMinWidth, TabMaxWidth)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
        // 1. Home, Mix and Library, all three inside one pill.
        MaterialLiquidTabBar(
            tabs = mainItems,
            selectedIndex = lastMainIndex,
            isMainTabActive = selectedMainIndex >= 0,
            currentRoute = currentRoute,
            navigationItems = navigationItems,
            pureBlack = pureBlack,
            slimNav = slimNav,
            barHeight = barHeight,
            tabWidth = tabWidth,
            onItemClick = onItemClick
        )

        // 2. Search, floating on its own
        if (searchItem != null) {
            Spacer(modifier = Modifier.width(gap)) 
            
            val isSearchSelected = remember(currentRoute, searchItem.route) {
                isRouteSelected(currentRoute, searchItem.route, navigationItems)
            }
            val currentIsSearchSelected by rememberUpdatedState(isSearchSelected)
            val interactionSource = remember { MutableInteractionSource() }

            if (onSearchLongClick != null) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSearchLongClick.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(searchItem, currentIsSearchSelected)
                                }
                            }
                            is PressInteraction.Cancel -> isLongClick = false
                        }
                    }
                }
            }

            // A circle carrying nothing but the magnifier and its own
            // elevation, so search reads as a control in its own right rather
            // than a fourth tab wedged into the navigation pill. The word was
            // labelling a control nobody needs labelling.
            Surface(
                onClick = {
                    if (onSearchLongClick == null) {
                        onItemClick(searchItem, currentIsSearchSelected)
                    }
                },
                interactionSource = interactionSource,
                shape = RoundedCornerShape(percent = 50),
                // Accent-filled, so the pill reads as the bar's second control
                // instead of a tonal blob that disappears into it.
                color = if (isSearchSelected) floatingToolbarSelectedItemContainerColor(pureBlack) else MaterialTheme.colorScheme.primary,
                contentColor = if (isSearchSelected) floatingToolbarSelectedItemContentColor(pureBlack) else MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 12.dp,
                modifier = Modifier.size(searchSize)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = if (isSearchSelected) searchItem.iconIdActive else searchItem.iconIdInactive),
                        contentDescription = stringResource(searchItem.titleId),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
            }
        }
    }
}

@Composable
private fun MaterialLiquidTabBar(
    tabs: List<Screens>,
    selectedIndex: Int,
    isMainTabActive: Boolean,
    currentRoute: String?,
    navigationItems: List<Screens>,
    pureBlack: Boolean,
    slimNav: Boolean,
    barHeight: androidx.compose.ui.unit.Dp,
    /** Width of one tab — the caller decides how much of the bar the pills may take. */
    tabWidth: androidx.compose.ui.unit.Dp,
    onItemClick: (Screens, Boolean) -> Unit
) {
    val tabsCount = tabs.size
    
    val blobHeight = if (slimNav) 36.dp else 44.dp 
    
    val tabWidthPx = with(LocalDensity.current) { tabWidth.toPx() }
    val totalWidth = tabWidth * tabsCount 
    
    val animationScope = rememberCoroutineScope()
    
    // FIX ADDED HERE: Added totalDragDistance to prevent micro-movements from blocking the tap
    val draggedFlag = remember { booleanArrayOf(false) }
    val totalDragDistance = remember { floatArrayOf(0f) } 
    
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    val currentRouteState by rememberUpdatedState(currentRoute)
    val currentTabs by rememberUpdatedState(tabs)
    val currentNavItems by rememberUpdatedState(navigationItems)
    
    val dampedDrag = remember(animationScope, tabsCount) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.coerceAtLeast(0).toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.15f, 
            onDragStarted = { 
                draggedFlag[0] = false
                totalDragDistance[0] = 0f 
            },
            onDragStopped = {
                if (draggedFlag[0]) {
                    val target = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                    animateToValue(target.toFloat())
                    
                    val screen = currentTabs[target]
                    val isSelected = isRouteSelected(currentRouteState, screen.route, currentNavItems)
                    currentOnItemClick(screen, isSelected)
                }
            },
            onDrag = { _, dragAmount ->
                totalDragDistance[0] += kotlin.math.abs(dragAmount.x)
                
                // 8 pixel ka touch threshold lagaya gaya hai
                if (totalDragDistance[0] > 8f) {
                    draggedFlag[0] = true
                }
                
                updateValue((targetValue + dragAmount.x / tabWidthPx).coerceIn(0f, (tabsCount - 1).toFloat()))
            }
        )
    }

    LaunchedEffect(selectedIndex) {
        dampedDrag.animateToValue(selectedIndex.toFloat())
    }

    Box(
        modifier = Modifier
            .height(barHeight)
            .width(totalWidth)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(50)) 
            .clip(RoundedCornerShape(50))
            .background(floatingToolbarContainerColor(pureBlack)),
        contentAlignment = Alignment.CenterStart
    ) {
        val indicatorOpacity by animateFloatAsState(targetValue = if (isMainTabActive) 1f else 0f, label = "Opacity")
        
        // Active Indicator (Blob)
        Box(
            Modifier
                .graphicsLayer {
                    translationX = dampedDrag.value * tabWidthPx
                    scaleX = dampedDrag.scaleX
                    scaleY = dampedDrag.scaleY
                    
                    val velocity = dampedDrag.velocity / 10f
                    scaleX /= 1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f)
                    alpha = indicatorOpacity
                }
                .width(tabWidth)
                .height(blobHeight)
                .padding(horizontal = 6.dp) 
                .clip(RoundedCornerShape(50))
                .background(floatingToolbarSelectedItemContainerColor(pureBlack))
        )

        // Icons and Labels Row
        Row(
            Modifier
                .fillMaxSize()
                .then(dampedDrag.modifier), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { position, screen ->
                val isSelected = remember(currentRouteState, screen.route) {
                    isRouteSelected(currentRouteState, screen.route, currentNavItems)
                }
                val currentIsSelected by rememberUpdatedState(isSelected)
                
                val iconRes = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                val contentColor = if (isSelected) floatingToolbarSelectedItemContentColor(pureBlack) else floatingToolbarItemContentColor(pureBlack)
                val animatedColor by animateColorAsState(targetValue = contentColor, label = "Color")

                Column(
                    Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = null,
                            indication = null, 
                            role = Role.Tab,
                            onClick = {
                                if (!draggedFlag[0]) {
                                    currentOnItemClick(screen, currentIsSelected)
                                }
                            }
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId),
                        tint = animatedColor,
                        modifier = Modifier.size(24.dp) 
                    )
                    
                    if (!slimNav) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(screen.titleId),
                            style = MaterialTheme.typography.labelMedium,
                            color = animatedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Standard Old Navigation Bar
// ----------------------------------------------------
@Composable
private fun StandardAppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val interactionSource = remember { MutableInteractionSource() }

            if (isSearchItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSearchLongClick.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                },
                label = if (!slimNav) {
                    {
                        Text(
                            text = stringResource(screen.titleId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else null
            )
        }
    }
}

// ----------------------------------------------------
// Color Providers
// ----------------------------------------------------
@Composable
private fun floatingToolbarContainerColor(pureBlack: Boolean): Color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
@Composable
private fun floatingToolbarSelectedItemContainerColor(pureBlack: Boolean): Color = if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer
@Composable
private fun floatingToolbarSelectedItemContentColor(pureBlack: Boolean): Color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
@Composable
private fun floatingToolbarItemContentColor(pureBlack: Boolean): Color = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
