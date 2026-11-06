@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.jay.glossy.ui.component

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.constants.UseFloatingNavBarKey
import com.jay.glossy.ui.screens.Screens
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.utils.rememberPreference
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
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
    onSearchLongClick: (() -> Unit)? = null,
    hazeNav: HazeState? = null,
    liquidGlassBackdrop: Backdrop? = null,
) {
    // One bar for the whole app: the floating pill of the design, painted with
    // whatever scheme the theme resolved. [pureBlack] is still accepted so the
    // callers keep compiling; the bar reads its colours from the palette.
    GlossyBottomNav(
        navigationItems = navigationItems,
        currentRoute = currentRoute,
        onItemClick = onItemClick,
        modifier = modifier,
        slimNav = slimNav,
        onSearchLongClick = onSearchLongClick,
        hazeNav = hazeNav,
        liquidGlassBackdrop = liquidGlassBackdrop,
    )
}

// ----------------------------------------------------
// New design system: the frosted bar and its search orb
// ----------------------------------------------------
/** How much of the wallpaper the navigation bar pulls through its frost. */
private val NavBarBlurRadius = 24.dp

/**
 * The bar from the design: a frosted pill holding Home, Mix and Library, the
 * active tab carried by a solid capsule, and a round search orb floating beside
 * it instead of a fourth tab wedged into the pill.
 *
 * The bar keeps [GlossyDimens.NavPillHeight] for its slot, so the mini player,
 * the sheet player and the window insets keep the geometry they were tuned for;
 * only the paint and the tabs/orb split are new.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlossyBottomNav(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    hazeNav: HazeState? = null,
    liquidGlassBackdrop: Backdrop? = null,
) {
    // The tab set the app passes in is Home, Mix and Library; Search is the
    // orb beside the pill, never one of its tabs.
    val searchItem = navigationItems.find { it == Screens.Search } ?: Screens.Search
    val tabs = navigationItems.filter { it != Screens.Search }
    val pill = RoundedCornerShape(GlossyDimens.CornerPill)
    val tabShape = RoundedCornerShape(GlossyDimens.CornerPill)

    // Captured before the draw phase: the palette reads are composition-only.
    val glass = GlossyPalette.MiniBar
    val rim = GlossyPalette.Border

    // The bar's lava wash. It is the only surface in the app that animates its
    // background: the motion is deliberately scoped to the navigation pill and
    // its search orb, not painted over the screens behind them. Ambient motion
    // is the app's existing reduce-motion switch, so the bar freezes with the
    // rest of the decorations instead of ignoring the setting.
    val motionEnabled = rememberAmbientMotionEnabled()
    val navFluid = rememberNavFluid(motionEnabled)
    val fluidRose = GlossyPalette.Accent
    val fluidViolet = MaterialTheme.colorScheme.tertiary
    val fluidShade = GlossyPalette.AccentSoft

    // A liquid-glass pane takes its colour from the recorded backdrop, so the
    // palette fill is only a translucent tint over it. The fluid wash stays the
    // fallback whenever the caller has no backdrop to sample; below the API
    // levels that support RenderEffect the glass degrades to the same tint,
    // which keeps the bar looking like part of the palette rather than flat.
    val glassTint = glass.copy(alpha = 0.35f)
    val navSurface: (Shape) -> Modifier = { shape ->
        liquidGlassBackdrop?.let { backdrop ->
            Modifier.liquidGlassSurface(
                backdrop = backdrop,
                shape = shape,
                tint = glassTint,
            )
        } ?: Modifier.navFluidWash(
            base = glass,
            rose = fluidRose,
            violet = fluidViolet,
            shade = fluidShade,
            fluid = navFluid,
        )
    }

    // Keep the navigation bar on its palette surface rather than a Haze child:
    // Haze 1.1.1 can throw at draw time when a HazeChild has no backgroundColor
    // configured, and the app has to launch on those devices. The wash below
    // paints [glass] first, so the bar keeps the MiniBar tone it had.

    val barHeight = if (slimNav) 56.dp else GlossyDimens.NavPillHeight
    val pillHeight = if (slimNav) 50.dp else 62.dp
    val orbSize = if (slimNav) 50.dp else 58.dp

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(barHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .weight(1f)
                    .height(pillHeight)
                    // The design's bar carries a shadow *and* a hairline, so it
                    // reads as a control floating over the feed rather than a
                    // strip painted onto it.
                    .shadow(elevation = 12.dp, shape = pill, clip = false)
                    .clip(pill)
                    .then(navSurface(pill))
                    .border(GlossyDimens.Hairline, rim, pill)
                    .padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            // The selection rides on one liquid pane that stretches between
            // tabs instead of each tab painting its own background. The
            // animation is deliberately driven by drag position rather than by
            // the route, so the pane follows a finger with the same squash and
            // stretch the video shows, then settles on the tab it is released
            // over.
            val tabCount = tabs.size.coerceAtLeast(1)
            val tabWidth = maxWidth / tabCount
            val tabWidthPx = with(LocalDensity.current) { tabWidth.toPx() }
            val currentTabWidthPx by rememberUpdatedState(tabWidthPx)
            val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
            val animationScope = rememberCoroutineScope()
            val selectedTabIndex =
                tabs.indexOfFirst { screen ->
                    isRouteSelected(currentRoute, screen.route, navigationItems)
                }
            var currentIndex by remember { mutableIntStateOf(selectedTabIndex.coerceAtLeast(0)) }
            var dragReleaseTarget by remember { mutableIntStateOf(-1) }
            var showPane by remember { mutableStateOf(selectedTabIndex >= 0) }
            val paneAlpha by animateFloatAsState(
                targetValue = if (showPane) 1f else 0f,
                label = "navSelectionPaneAlpha",
            )
            // The drag callbacks outlive the composition that created them, so
            // they read these through updated state instead of capturing the
            // route that happened to be active when the bar first appeared.
            val currentSelectedTabIndex by rememberUpdatedState(selectedTabIndex)
            val draggedFlag = remember { booleanArrayOf(false) }

            val dampedDrag =
                remember(animationScope, tabCount, isLtr) {
                    DampedDragAnimation(
                        animationScope = animationScope,
                        initialValue = selectedTabIndex.coerceAtLeast(0).toFloat(),
                        valueRange = 0f..(tabCount - 1).toFloat(),
                        visibilityThreshold = 0.001f,
                        initialScale = 1f,
                        pressedScale = 1.36f,
                        onDragStarted = { draggedFlag[0] = false },
                        onDragStopped = {
                            if (draggedFlag[0]) {
                                val target = targetValue.roundToInt().coerceIn(0, tabCount - 1)
                                // While Search is open there is no selected
                                // tab, so releasing on the last tab must still
                                // navigate back into the pill.
                                if (target == currentIndex && currentSelectedTabIndex >= 0) {
                                    // Released over the tab it started on, so
                                    // just settle the stretched pane back.
                                    animateToValue(target.toFloat())
                                } else {
                                    currentIndex = target
                                    dragReleaseTarget = target
                                }
                            }
                        },
                        onDrag = { _, dragAmount ->
                            if (dragAmount.x != 0f) draggedFlag[0] = true
                            updateValue(
                                (targetValue +
                                    dragAmount.x / currentTabWidthPx * if (isLtr) 1f else -1f)
                                    .coerceIn(0f, (tabCount - 1).toFloat()),
                            )
                        },
                    )
                }

            // Route changes from anywhere else (back, deep link, shortcut) move
            // the pane too, instead of leaving it behind on the old tab.
            LaunchedEffect(selectedTabIndex) {
                if (selectedTabIndex >= 0) {
                    showPane = true
                    if (selectedTabIndex != currentIndex) {
                        currentIndex = selectedTabIndex
                    }
                } else {
                    showPane = false
                }
            }
            LaunchedEffect(tabCount) {
                currentIndex = currentIndex.coerceIn(0, tabCount - 1)
            }
            LaunchedEffect(dampedDrag) {
                snapshotFlow { currentIndex }
                    .drop(1)
                    .collectLatest { index -> dampedDrag.animateToValue(index.toFloat()) }
            }
            LaunchedEffect(dragReleaseTarget) {
                val target = dragReleaseTarget
                if (target < 0) return@LaunchedEffect
                dragReleaseTarget = -1
                val screen = tabs.getOrNull(target) ?: return@LaunchedEffect
                onItemClick(screen, isRouteSelected(currentRoute, screen.route, navigationItems))
            }

            val glassBackdrop = liquidGlassBackdrop
            // Shared by the glass pane and the tint fallback so the press
            // squash and the velocity stretch are felt with Liquid glass off
            // too, not just by the devices that can blur the backdrop.
            val paneLayer: GraphicsLayerScope.() -> Unit = {
                scaleX = dampedDrag.scaleX
                scaleY = dampedDrag.scaleY
                val velocity = dampedDrag.velocity / 10f
                scaleX /= 1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
                scaleY *= 1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f)
            }
            val blobSurface =
                if (glassBackdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.drawBackdrop(
                        backdrop = glassBackdrop,
                        shape = { tabShape },
                        effects = {
                            val press = dampedDrag.pressProgress
                            vibrancy()
                            colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.5f)
                            blur(18f.dp.toPx() + 8f.dp.toPx() * press)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                lens(
                                    10f.dp.toPx() * press,
                                    14f.dp.toPx() * press,
                                    chromaticAberration = true,
                                )
                            }
                        },
                        highlight = { Highlight.Default.copy(alpha = 0.55f) },
                        shadow = { Shadow(radius = 4f.dp, alpha = 0.35f) },
                        innerShadow = {
                            val press = dampedDrag.pressProgress
                            InnerShadow(radius = 8f.dp * press, alpha = press)
                        },
                        layerBlock = paneLayer,
                        onDrawSurface = {
                            // A frosted wash keeps the pane readable without
                            // turning it into a second opaque fill: the
                            // refracted page stays visible through it.
                            drawRect(Color.White.copy(alpha = 0.12f))
                        },
                    )
                } else {
                    Modifier
                        .graphicsLayer(block = paneLayer)
                        .clip(tabShape)
                        .background(GlossyPalette.AccentSoft)
                }

            Box(
                modifier =
                    Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX =
                                dampedDrag.value *
                                    currentTabWidthPx *
                                    if (isLtr) 1f else -1f
                            // Search is the orb, not one of these tabs, so no
                            // tab pane exists while it is open. Pressing the
                            // pill brings the pane back for the drag.
                            alpha = paneAlpha.coerceAtLeast(dampedDrag.pressProgress)
                        }
                        .then(blobSurface),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(dampedDrag.modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, screen ->
                    val routeSelected =
                        isRouteSelected(currentRoute, screen.route, navigationItems)
                    val highlighted = selectedTabIndex >= 0 && currentIndex == index
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(tabShape)
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        showPane = true
                                        currentIndex = index
                                        onItemClick(screen, routeSelected)
                                    },
                                ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (highlighted) screen.iconIdActive else screen.iconIdInactive
                            ),
                            contentDescription = stringResource(screen.titleId),
                            tint = if (highlighted) GlossyPalette.Accent else GlossyPalette.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(screen.titleId),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (highlighted) GlossyPalette.Accent else GlossyPalette.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (tabs.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))

            val isSelected = isRouteSelected(currentRoute, searchItem.route, navigationItems)
            val currentIsSelected by rememberUpdatedState(isSelected)

            Box(
                modifier =
                    Modifier
                        .size(orbSize)
                        .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                        .clip(CircleShape)
                            .then(navSurface(CircleShape))
                        .border(GlossyDimens.Hairline, rim, CircleShape)
                        // A press opens search; holding it opens recognition.
                        .combinedClickable(
                            onClick = { onItemClick(searchItem, currentIsSelected) },
                            onLongClick = onSearchLongClick,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (isSelected) searchItem.iconIdActive else searchItem.iconIdInactive
                    ),
                    contentDescription = stringResource(searchItem.titleId),
                    tint = if (isSelected) GlossyPalette.Accent else GlossyPalette.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

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
