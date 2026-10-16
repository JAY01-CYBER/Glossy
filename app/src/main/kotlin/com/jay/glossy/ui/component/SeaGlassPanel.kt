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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    Box(
        modifier = modifier
            .clip(shape)
            .then(seaGlassSurface(fill = fill, shape = shape, cornerRadius = corner)),
    ) { content() }
}

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
