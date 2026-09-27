/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The reusable building blocks of the new design system: chips, text tabs,
 * pill toggles, grouped settings rows, list rows, swatch thumbnails, the disc
 * glyph placeholder, dotted progress indicators and the player slider.
 *
 * Every screen of the redesign is assembled from these, so spacing, corner
 * radii and colours stay identical everywhere instead of being re-invented per
 * screen.
 */

package com.jay.glossy.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jay.glossy.R
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.ui.theme.glossyOnSwatchFor
import com.jay.glossy.ui.theme.glossySwatchFor

// ============================================================================
// Text
// ============================================================================

/** Tiny, letter-spaced, muted uppercase label — the design's "eyebrow". */
@Composable
fun GlossyEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp,
        color = color ?: GlossyPalette.TextMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Sentence-case bold section title, optionally preceded by an eyebrow. */
@Composable
fun GlossySectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
) {
    Column(modifier = modifier) {
        if (!eyebrow.isNullOrBlank()) {
            GlossyEyebrow(eyebrow)
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = GlossyPalette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ============================================================================
// Chips and tabs
// ============================================================================

/** Full pill filter chip: teal when selected, dark neutral when not. */
@Composable
fun GlossyChip(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        targetValue = if (active) GlossyPalette.Accent else GlossyPalette.Chip,
        label = "glossyChipContainer",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GlossyDimens.CornerPill))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) GlossyPalette.OnAccent else GlossyPalette.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Horizontally scrolling row of [GlossyChip]s. */
@Composable
fun <T> GlossyChipRow(
    items: List<Pair<T, String>>,
    currentValue: T?,
    onValueUpdate: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = GlossyDimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (value, label) ->
            GlossyChip(
                text = label,
                active = value == currentValue,
                onClick = { onValueUpdate(value) },
            )
        }
    }
}

/** Plain text tab with a teal underline on the active entry. */
@Composable
fun GlossyTextTab(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Captured before the draw scope: the palette cannot be read from there.
    val underlineColor = GlossyPalette.Accent
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = GlossyPalette.Accent),
                onClick = onClick,
            )
            .padding(bottom = 10.dp)
            .drawBehind {
                if (active) {
                    val y = size.height - 2.dp.toPx()
                    drawLine(
                        color = underlineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) GlossyPalette.TextPrimary else GlossyPalette.TextSecondary,
            maxLines = 1,
        )
    }
}

/** Scrolling row of [GlossyTextTab]s. */
@Composable
fun <T> GlossyTextTabs(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = GlossyDimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (value, label) ->
            GlossyTextTab(
                text = label,
                active = value == selected,
                onClick = { onSelect(value) },
            )
        }
    }
}

// ============================================================================
// Controls
// ============================================================================

/**
 * Small circular icon action used in headers and rows. Passing [background]
 * turns it into a raised, filled button — the form the library dock uses.
 */
@Composable
fun GlossyIconAction(
    icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    background: Color? = null,
    elevation: Dp = 0.dp,
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation = elevation, shape = CircleShape, clip = false)
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .then(
                if (background != null) Modifier.background(background) else Modifier,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = buttonSize / 2),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint ?: GlossyPalette.TextSecondary,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * The library's floating dock: a raised search pill with the two collections
 * people reach for most — Liked and Downloads — beside it.
 *
 * The pill carries its own shadow and fill so it reads as a control hovering
 * over the list rather than as the first row of it; the buttons reuse
 * [GlossyIconAction]'s filled form so all three sit on the same plane.
 */
@Composable
fun GlossySearchDock(
    hint: String,
    onSearchClick: () -> Unit,
    onLikedClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    modifier: Modifier = Modifier,
    likedLabel: String? = null,
    downloadsLabel: String? = null,
) {
    val pillShape = RoundedCornerShape(GlossyDimens.CornerPill)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GlossyDimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .shadow(elevation = 6.dp, shape = pillShape, clip = false)
                .clip(pillShape)
                .background(GlossyPalette.Row)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = GlossyPalette.Accent),
                    onClick = onSearchClick,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = GlossyPalette.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = GlossyPalette.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        GlossyIconAction(
            icon = R.drawable.favorite,
            contentDescription = likedLabel ?: stringResource(R.string.liked),
            tint = GlossyPalette.TextPrimary,
            buttonSize = 48.dp,
            iconSize = 22.dp,
            background = GlossyPalette.Row,
            elevation = 6.dp,
            onClick = onLikedClick,
        )

        GlossyIconAction(
            icon = R.drawable.download,
            contentDescription = downloadsLabel ?: stringResource(R.string.downloaded_songs),
            tint = GlossyPalette.TextPrimary,
            buttonSize = 48.dp,
            iconSize = 22.dp,
            background = GlossyPalette.Row,
            elevation = 6.dp,
            onClick = onDownloadsClick,
        )
    }
}

/** Full-width pill button: light fill for the primary action, outline for the rest. */
@Composable
fun GlossyPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val container = if (filled) GlossyPalette.TextPrimary else Color.Transparent
    val content = if (filled) GlossyPalette.Page else GlossyPalette.TextPrimary
    // Captured before the draw scope: the palette cannot be read from there.
    val outlineColor = GlossyPalette.Border
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(GlossyDimens.CornerPill))
            .background(container)
            .then(
                if (filled) {
                    Modifier
                } else {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = outlineColor,
                            style = Stroke(width = 2.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                        )
                    }
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
        }
    }
}

/** Three-dot indicator; the middle dot is accented. */
@Composable
fun GlossyDotsIndicator(
    count: Int = 3,
    activeIndex: Int = 1,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == activeIndex) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (index == activeIndex) GlossyPalette.Accent else GlossyPalette.Chip),
            )
        }
    }
}

// ============================================================================
// Rows and groups
// ============================================================================

/**
 * Continuous-list row: 46dp leading slot, bold title, secondary subtitle and a
 * trailing slot. The optional hairline divider is inset so it starts under the
 * text, matching the design's list treatment.
 */
@Composable
fun GlossyListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = GlossyPalette.Accent),
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(
                    start = GlossyDimens.ScreenPadding,
                    end = GlossyDimens.ScreenPadding,
                    top = 10.dp,
                    bottom = 10.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(GlossyDimens.Related))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = GlossyPalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = GlossyPalette.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = GlossyDimens.ScreenPadding +
                            (if (leading != null) 46.dp + GlossyDimens.Related else 0.dp),
                        end = GlossyDimens.ScreenPadding,
                    )
                    .height(GlossyDimens.Hairline)
                    .background(GlossyPalette.Border),
            )
        }
    }
}

// ============================================================================
// Profile, brand and progress pieces
// ============================================================================

/** Round platform tile: the brand's colour with its white glyph on top. */
@Composable
fun GlossyBrandTile(
    color: Color,
    icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

// ============================================================================
// Artwork, avatars and placeholders
// ============================================================================

/**
 * Simple outline disc used as the placeholder artwork glyph: an outer ring with
 * a centre hole, drawn in the dark accent so it sits on a category swatch.
 */
@Composable
fun GlossyDiscGlyph(
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    // Captured before the draw scope: the palette cannot be read from there.
    val glyphColor = color ?: GlossyPalette.Player
    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        drawCircle(
            color = glyphColor.copy(alpha = 0.85f),
            radius = diameter * 0.30f,
            style = Stroke(width = diameter * 0.06f),
        )
        drawCircle(
            color = glyphColor,
            radius = diameter * 0.075f,
        )
    }
}

/** Square artwork placeholder filled with the item's category colour. */
@Composable
fun GlossyThumb(
    seed: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
    imageUrl: String? = null,
    glyph: Boolean = true,
) {
    val swatch = remember(seed) { glossySwatchFor(seed) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(swatch),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (glyph) {
            GlossyDiscGlyph(modifier = Modifier.fillMaxSize(0.72f))
        }
    }
}

/** Circular avatar showing the user's initial (or their profile picture). */
@Composable
fun GlossyInitialAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    imageUrl: String? = null,
) {
    val swatch = remember(name) { glossySwatchFor(name) }
    val initial = remember(name) {
        name.trim().firstOrNull()?.uppercase() ?: "G"
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(swatch),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = initial,
                color = glossyOnSwatchFor(swatch),
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.42f).sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Dashed outline tile with a plus — used for "New playlist". */
@Composable
fun GlossyDashedTile(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    corner: Dp = 12.dp,
) {
    // Captured before the draw scope: the palette cannot be read from there.
    val dashColor = GlossyPalette.TextLow
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = GlossyPalette.Accent),
                onClick = onClick,
            )
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.add),
            contentDescription = contentDescription,
            tint = GlossyPalette.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ============================================================================
// Player pieces
// ============================================================================

/**
 * Thin player progress bar: neutral track, teal fill and a round knob. Handles
 * both tap-to-seek and drag-to-seek in one gesture detector.
 */
@Composable
fun GlossyProgressSlider(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shown = (dragFraction ?: progress).coerceIn(0f, 1f)
    // Captured before the draw scope: the palette cannot be read from there.
    val trackColor = GlossyPalette.Border
    val fillColor = GlossyPalette.Accent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    dragFraction = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    down.consume()
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            change.consume()
                        } else {
                            pressed = false
                        }
                    }
                    dragFraction?.let(onSeek)
                    dragFraction = null
                }
            }
            .drawBehind {
                val centerY = size.height / 2f
                val trackWidth = 4.dp.toPx()
                drawLine(
                    color = trackColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round,
                )
                val headX = size.width * shown
                if (headX > 0f) {
                    drawLine(
                        color = fillColor,
                        start = Offset(0f, centerY),
                        end = Offset(headX, centerY),
                        strokeWidth = trackWidth,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(headX.coerceIn(6.dp.toPx(), size.width - 6.dp.toPx()), centerY),
                )
            },
    )
}

/** Icon (and optional label) used in the player's utility row. */
@Composable
fun GlossyIconTile(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    active: Boolean = false,
    iconSize: Dp = 22.dp,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GlossyDimens.CornerSmall))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = GlossyPalette.Accent),
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (active) GlossyPalette.Accent else GlossyPalette.TextSecondary,
            modifier = Modifier.size(iconSize),
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) GlossyPalette.Accent else GlossyPalette.TextSecondary,
                maxLines = 1,
            )
        }
    }
}

// ============================================================================
// Home pieces
// ============================================================================

/** Square album card with the title caption under the art ("Listen again"). */
@Composable
fun GlossyAlbumCard(
    title: String,
    seed: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    imageUrl: String? = null,
) {
    Column(
        modifier = modifier
            .width(96.dp)
            .clip(RoundedCornerShape(GlossyDimens.CornerSmall))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = GlossyPalette.Accent),
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        GlossyThumb(
            seed = seed,
            size = 96.dp,
            corner = 12.dp,
            imageUrl = imageUrl,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = GlossyPalette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GlossyPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Compact song row of the "Quick picks" grid: a 44dp swatch with the title and
 * artist stacked next to it, on a subtle card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlossyCompactSongRow(
    title: String,
    subtitle: String?,
    seed: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    isActive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlossyDimens.CornerSmall))
            .background(GlossyPalette.Row)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyThumb(
            seed = seed,
            size = 44.dp,
            corner = 10.dp,
            imageUrl = imageUrl,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) GlossyPalette.Accent else GlossyPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = GlossyPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}
