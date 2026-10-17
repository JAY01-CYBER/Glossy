/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.seaglass.compose.SeaGlassContentRole
import dev.seaglass.compose.seaGlass
import dev.seaglass.core.SeaGlassMaterial

/**
 * Whether this device can run the real SeaGlass pass.
 *
 * The frost is a `RuntimeShader`, and `RuntimeShader` only exists from API 33.
 * Below that the same surfaces are drawn by [seaGlassFallback] instead, which
 * keeps the look — tint, highlight, chalky rim — without the shader, so a pane
 * of sea glass is still *visible* on an older phone rather than collapsing into
 * a flat fill.
 *
 * This was previously pinned off outright, so every call site took its flat
 * branch on every device and the glass never shipped at all. The glass is the
 * whole point of these surfaces, so the tier check is back, and the fallback it
 * leaves for older devices is now a painted surface rather than a fill.
 */
val seaGlassSurfacesAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * The app's sea glass, defined once.
 *
 * Every surface in the app — the sign-in pills, the onboarding button, the
 * section panels, the chips, the search dock, the floating nav bar — is cut from
 * these values, so the material is the same material everywhere and a change
 * here changes all of it. Before this there were five separate tunings
 * (0.44/0.70 on the login pills, 0.38/0.66 on the guest pill, 0.40/0.72 on the
 * panels, 0.62/0.45 on onboarding, 0.44/0.70 on the hero), so the same glass read
 * at a different strength on each screen.
 *
 * The strengths are the denser end of that range on purpose: glass you can read
 * the wallpaper through stopped reading as glass at all, and the tint has to hold
 * on its own on the devices where the body is painted rather than sampled. If
 * the look ever wants to be lighter or heavier, this object is the one place to
 * turn the dial.
 */
object SeaGlassLook {
    /** How much of [SeaGlassMaterial.tintColorArgb] the body absorbs. */
    const val TintDensity = 0.62f

    /** How uneven the frost is: higher reads as more stone-like. */
    const val Coarseness = 0.28f

    /** How far the pass looks behind the surface, in dp. */
    const val FrostRadiusDp = 26f

    /** How far the body's edge thickness runs, which is what lights the rim. */
    const val ThicknessDp = 15f

    /** How chalky the rim is, against how much of the backdrop it picks up. */
    const val RimChalkiness = 0.55f

    /** How much of the backdrop survives the body. Lower is denser. */
    const val Transparency = 0.45f

    // --- The painted body ([seaGlassFallback]). Kept as alphas of the surface's
    // own tint rather than of white, so the drawn pane sits at the same strength
    // as the sampled one and a phone below API 33 sees the same material.

    /** The tint at the top lip, where the light lands. */
    const val BodyTopAlpha = 0.58f

    /** The tint at the bottom lip, where the pane has thinned out. */
    const val BodyBottomAlpha = 0.34f

    /** The highlight the frost throws back at the light. */
    const val HighlightAlpha = 0.22f

    /** A little bounce along the bottom lip, so the body is not just a gradient. */
    const val BounceAlpha = 0.08f

    /** The chalky rim around the outline. */
    const val RimAlpha = 0.32f

    /** The rim's width. */
    val RimWidth = 1.dp
}

/**
 * The glass body for [fill] at the app's one set of values.
 *
 * [cornerRadiusDp] is the corner the surface is cut to: half a pill's height for
 * a pill, the panel's corner for a panel. It has to be the real corner, because
 * the pass rounds its own contour to it.
 */
fun seaGlassMaterial(fill: Color, cornerRadiusDp: Float): SeaGlassMaterial =
    SeaGlassMaterial(
        tintColorArgb = fill.toArgb(),
        tintDensity = SeaGlassLook.TintDensity,
        coarseness = SeaGlassLook.Coarseness,
        frostRadiusDp = SeaGlassLook.FrostRadiusDp,
        thicknessDp = SeaGlassLook.ThicknessDp,
        rimChalkiness = SeaGlassLook.RimChalkiness,
        transparency = SeaGlassLook.Transparency,
        cornerRadiusDp = cornerRadiusDp,
    )

/**
 * The app's sea-glass surface: the sampled pane on a device that can run the
 * pass, the painted pane ([seaGlassFallback]) everywhere else.
 *
 * This is the only way a surface should be made into glass. Going through one
 * entry point is what keeps the material consistent — a caller that builds its
 * own [SeaGlassMaterial] is a caller that has its own opinion about how glassy
 * the glass is, and the app had five of those.
 *
 * [cornerRadius] is both the fallback's clipping corner (through [shape]) and the
 * shader's contour radius, so the two paths cover the same area and nothing is
 * cut off on either.
 */
@Composable
fun seaGlassSurface(
    fill: Color,
    shape: Shape,
    cornerRadius: Dp,
): Modifier =
    if (LocalInspectionMode.current || !seaGlassSurfacesAvailable) {
        Modifier.seaGlassFallback(fill = fill, shape = shape)
    } else {
        Modifier.seaGlass(
            material = remember(fill, cornerRadius) { seaGlassMaterial(fill, cornerRadius.value) },
            contentRole = SeaGlassContentRole.TEXT_AND_ICONS,
        )
    }

/**
 * The sea-glass body for a device with no shader to run one on.
 *
 * A real pane is a tinted, frosted body with light caught on its lip, so that
 * is what is painted here: the tint densest at the top edge, a highlight thrown
 * back at the light, and a chalky rim around the outline. It is deliberately
 * *not* a flat fill — a flat fill is what made the glass invisible below API 33
 * in the first place.
 *
 * Draw it in place of the shader, not underneath it: it clips itself to [shape]
 * and paints its own rim, so a fill behind it would only flatten it back out.
 *
 * The three bodies are remembered against [fill] rather than rebuilt into the
 * modifier chain. A `Brush` is the thing that owns the Skia shader behind a
 * gradient, and it builds that shader on first draw and then keeps it — so a
 * remembered brush means one shader reused for the life of the surface, where a
 * rebuilt one asks Skia for a new shader the next time it is drawn. Below API 33
 * this is the only body glass has, which makes it the whole cost of the material
 * on those devices, and it is paid by every chip, tile, dock and panel that
 * comes back onto the screen while a list scrolls.
 */
@Composable
fun Modifier.seaGlassFallback(
    fill: Color,
    shape: Shape,
): Modifier {
    // The body: denser where the light lands, thinning downwards the way a
    // frosted pane does.
    val body = remember(fill) {
        Brush.verticalGradient(
            colors = listOf(
                fill.copy(alpha = SeaGlassLook.BodyTopAlpha),
                fill.copy(alpha = SeaGlassLook.BodyBottomAlpha),
            ),
        )
    }
    // The highlight the frost throws back, and a little bounce along the bottom
    // lip so the body reads as glass rather than as a gradient. Not keyed on the
    // fill: it is white against transparent, at the material's own strengths.
    val sheen = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = SeaGlassLook.HighlightAlpha),
                Color.Transparent,
                Color.White.copy(alpha = SeaGlassLook.BounceAlpha),
            ),
        )
    }
    val rim = remember { Color.White.copy(alpha = SeaGlassLook.RimAlpha) }

    return this
        .clip(shape)
        .background(body)
        .background(sheen)
        .border(SeaGlassLook.RimWidth, rim, shape)
}
