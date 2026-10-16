/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.seaglass.compose.SeaGlassContentRole
import dev.seaglass.compose.seaGlass
import dev.seaglass.core.SeaGlassMaterial

/** The corner the section panels are cut to, in dp. */
private val SectionPanelCorner = 20.dp

/**
 * A panel cut from sea glass, used to seat a section lockup on the browsable
 * screens.
 *
 * The body is the real frosted pane where the device can run the shader, and
 * the painted equivalent ([seaGlassFallback]) everywhere else — a phone below
 * API 33, or a tooling preview. Either way the panel is glass, which is the
 * point: the fallback used to be a flat fill, and a flat fill is invisible as
 * glass.
 *
 * [fill] tints the body and is taken from the scheme by default, so the panel
 * repaints from the seed colour and follows Material You like the surface it
 * sits on.
 */
@Composable
fun SeaGlassPanel(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape: Shape = RoundedCornerShape(SectionPanelCorner),
    corner: Dp = SectionPanelCorner,
    content: @Composable () -> Unit,
) {
    val glass =
        if (LocalInspectionMode.current || !seaGlassSurfacesAvailable) {
            Modifier.seaGlassFallback(fill = fill, shape = shape)
        } else {
            Modifier.seaGlass(
                material = remember(fill, corner) { seaGlassBody(fill, corner.value) },
                contentRole = SeaGlassContentRole.TEXT_AND_ICONS,
            )
        }

    Box(modifier = modifier.clip(shape).then(glass)) { content() }
}

/**
 * The glass body: the pane's tint rides inside the glass as Beer-Lambert body
 * absorption, so the colour sits *inside* the surface and the rim picks up
 * whatever is behind it rather than drawing a colour on top. Values are tuned
 * for a wide, fairly flat panel rather than the login screen's small pills.
 */
private fun seaGlassBody(fill: Color, corner: Float): SeaGlassMaterial =
    SeaGlassMaterial(
        tintColorArgb = fill.toArgb(),
        tintDensity = 0.40f,
        coarseness = 0.28f,
        frostRadiusDp = 26f,
        thicknessDp = 14f,
        rimChalkiness = 0.55f,
        transparency = 0.72f,
        cornerRadiusDp = corner,
    )

/**
 * A section lockup — the eyebrow over the title — on a sea-glass panel.
 *
 * The library and mix tabs both draw their section breaks through this, so the
 * two read as one page, and it is the surface the app's sea glass is most
 * likely to be seen on: browsing, not the player.
 */
@Composable
fun SeaGlassSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    onClick: (() -> Unit)? = null,
) {
    SeaGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}
