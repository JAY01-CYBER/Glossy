/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The sign-in landing screen: a deep violet radial gradient carrying the brand
 * block (a big glowing Glossy mark, tagline pill, title, subtitle), the four
 * holographic key-art props scattered across the whole backdrop the way the
 * brand key art places them, and the white card itself with the three ways in,
 * followed by the legal footnote and the credits.
 *
 * Structure: colours live as named constants below, the backdrop is a single
 * drawBehind, the props are [HoloProp]s scattered by [HoloPropsBackdrop] from
 * the [HoloProps] spec list, and the card is [LoginCard]. The screen owns no
 * state and no navigation — every decision it offers leaves through
 * onGoogleClick / onGuestClick / onTokenClick, so whichever route hosts the
 * screen owns the flow.
 *
 * The props sit in a layer of their own behind the content, not in the column:
 * a prop has to be able to float above the brand block or tuck behind the
 * action card, which a row in the flow could never do.
 *
 * The props are the raster key art (yu/spo/ca/tu), which ships with a solid
 * black background; they are drawn with BlendMode.Screen so the black drops
 * away to the gradient while the holographic highlights lift it, and the paint
 * alpha dims them into the backdrop without turning that black back into a
 * visible box (Screen of black is the gradient either way). Each one also
 * drifts slowly on its own cycle — translate on both axes plus tilt, never
 * scale, so the blend stays against the gradient rather than an isolated layer.
 * The vector props in ui/component/GlassArt.kt remain the drifting art used by
 * WelcomeScreen and the Settings header.
 */
package com.jay.glossy.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.R
import kotlin.math.roundToInt

// ============================================================================
// Palette
// ============================================================================

/** Backdrop: lighter violet at the focus, dark purple into black at the edges. */
private val BackdropFocus = Color(0xFF6A3FBF)
private val BackdropMid = Color(0xFF3B1466)
private val BackdropEdge = Color(0xFF12061E)

/** Brand block on the backdrop. */
private val HeadingWhite = Color(0xFFFFFFFF)
private val LogoGlow = Color(0x59C09BFF)
private val SubtextGray = Color(0xFFD7CFE8)
private val PillFill = Color(0x33FFFFFF)
private val PillBorder = Color(0x4DFFFFFF)
private val PillText = Color(0xFFF3EDFF)

/** White action card. */
private val CardWhite = Color(0xFFFFFFFF)
private val GoogleButton = Color(0xFF7C3AED)
private val OnGoogleButton = Color(0xFFFFFFFF)
private val GuestButton = Color(0xFFE8E8ED)
private val OnGuestButton = Color(0xFF1A1226)
private val CookieLink = Color(0xFF4A4358)

/** Footnote and credits, small type sitting on the gradient below the card. */
private val FootnoteWhite = Color(0xF0FFFFFF)
private val CreditWhite = Color(0xA6FFFFFF)

/**
 * How strongly the key art reads. The props are background texture, not the
 * subject, so they sit under the brand block rather than competing with it.
 */
private const val PropFade = 0.78f

// ============================================================================
// Screen
// ============================================================================

/**
 * Glossy sign-in landing screen.
 *
 * @param onGoogleClick starts the Google sign-in flow.
 * @param onGuestClick continues without an account.
 * @param onTokenClick opens the cookie / token entry.
 */
@Composable
fun GlossyLoginScreen(
    onGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
    onTokenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The backdrop breathes. The focus of the radial gradient drifts a few
    // percent and the radius opens a little over a 14 second cycle, which is
    // slow enough to read as light rather than as motion — and because it is
    // read inside drawBehind it only ever costs a repaint, never a recompose or
    // a re-layout. A symmetric in-out curve is what keeps the turnarounds from
    // ticking.
    val backdropTransition = rememberInfiniteTransition(label = "backdrop")
    val backdropBreath by backdropTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 14_000,
                easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f),
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "backdropBreath",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val breath = backdropBreath - 0.5f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(BackdropFocus, BackdropMid, BackdropEdge),
                        center = Offset(
                            size.width * (0.5f + breath * 0.06f),
                            size.height * (0.42f + breath * 0.05f),
                        ),
                        radius = size.maxDimension * (0.92f + breath * 0.08f),
                    ),
                )
            },
    ) {
        // Key art first, so it sits under everything the column draws — the
        // brand block over it, the card over its feet.
        HoloPropsBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // Everything below rises into place, top piece first, so the screen
            // assembles rather than appearing fully formed the instant it opens.
            // The delays are the running order of reading it: mark, tagline,
            // promise, the card, the small print.
            Entrance(delayMillis = 0) { LogoMark() }

            Spacer(Modifier.height(16.dp))

            Entrance(delayMillis = 90) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(PillFill)
                        .border(1.dp, PillBorder, RoundedCornerShape(percent = 50))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = stringResource(R.string.glossy_tagline),
                        color = PillText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Entrance(delayMillis = 170) {
                Text(
                    text = stringResource(R.string.glossy_welcome_title),
                    color = HeadingWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(10.dp))

            Entrance(delayMillis = 240) {
                Text(
                    text = stringResource(R.string.glossy_welcome_subtitle),
                    color = SubtextGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }

            // The gap the free key art floats in; it is deliberately the
            // smaller half of the slack so the card lands low on the screen.
            Spacer(Modifier.weight(0.34f))

            Entrance(delayMillis = 320) {
                LoginCard(
                    onGoogleClick = onGoogleClick,
                    onGuestClick = onGuestClick,
                    onTokenClick = onTokenClick,
                )
            }

            Spacer(Modifier.height(12.dp))

            Entrance(delayMillis = 430) {
                Text(
                    text = stringResource(R.string.glossy_legal_footnote),
                    color = FootnoteWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(6.dp))

            Entrance(delayMillis = 480) {
                Text(
                    text = stringResource(R.string.glossy_credits),
                    color = CreditWhite,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

// ============================================================================
// Pieces
// ============================================================================

/**
 * Rises [content] into place: a fade plus a 44dp lift, on [delayMillis]'s
 * schedule.
 *
 * The flag flips once from a [LaunchedEffect], so the animation runs on the
 * first frame the screen is composed and never again — the screen is not
 * re-entered while it is open, and a re-entry should not replay the arrival.
 */
@Composable
private fun Entrance(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(
            durationMillis = 620,
            delayMillis = delayMillis,
            // Long tail, quick start: the piece is there almost immediately and
            // settles, instead of crawling in and stopping.
            easing = CubicBezierEasing(0.16f, 0.8f, 0.24f, 1f),
        ),
        label = "entrance",
    )

    Box(
        modifier =
            modifier.graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 44f
            },
    ) {
        content()
    }
}

/**
 * The app mark at display size, with its own soft glow behind it.
 *
 * The glow pulses — radius only, inside the draw phase, so it animates at frame
 * rate without recomposing anything.
 */
@Composable
private fun LogoMark(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "logoGlow")
    val glowPulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4_400,
                // Mirrors the backdrop: a symmetric in-out curve, so the halo
                // swells and settles rather than snapping between sizes.
                easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f),
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    Box(
        modifier = modifier
            .size(184.dp)
            .drawBehind {
                val glowRadius = size.minDimension * 0.5f * glowPulse
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(LogoGlow, Color.Transparent),
                        center = center,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = center,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            tint = HeadingWhite,
            modifier = Modifier.size(148.dp),
        )
    }
}

/**
 * One glass prop: where it sits on the backdrop, how big it is, how it drifts.
 *
 * [periodMillis], [startPhase] and [sway] all differ per prop on purpose — four
 * props sharing one cycle and one phase would read as a single mechanical block
 * instead of objects floating on their own.
 */
private data class HoloPropSpec(
    @DrawableRes val resId: Int,
    val width: Dp,
    val height: Dp,
    /** Centre of the prop across the screen: 0 is the left edge, 1 the right. */
    val xFraction: Float,
    /** Centre of the prop down the screen: 0 is the top edge, 1 the bottom. */
    val yFraction: Float,
    /** Resting tilt, in degrees. */
    val rotation: Float,
    /** How far the prop drifts vertically from its resting place. */
    val floatDistance: Dp,
    /** How far the prop drifts horizontally from its resting place. */
    val driftX: Dp,
    /** Extra tilt the drift adds on either side of [rotation]. */
    val sway: Float,
    /** One half of the drift cycle. */
    val periodMillis: Int,
    /** Where in that cycle the prop starts, so the four break phase at once. */
    val startPhase: Float,
)

/**
 * The four props, placed as the brand key art places them: the box upper left
 * beside the brand block, the lens upper right, the cassette lower left with
 * its feet tucked behind the action card, and the bolt alongside the card on
 * the right. Fractions, not offsets, so they hold their composition on any
 * screen — with the two lower props kept high enough that the card, which is
 * drawn over them, can only ever swallow their feet.
 */
private val HoloProps = listOf(
    HoloPropSpec(
        resId = R.drawable.yu,
        width = 132.dp,
        height = 90.dp,
        xFraction = 0.30f,
        yFraction = 0.20f,
        rotation = -10f,
        floatDistance = 12.dp,
        driftX = 5.dp,
        sway = 4f,
        periodMillis = 6200,
        startPhase = 0.15f,
    ),
    HoloPropSpec(
        resId = R.drawable.spo,
        width = 104.dp,
        height = 104.dp,
        xFraction = 0.77f,
        yFraction = 0.29f,
        rotation = 8f,
        floatDistance = 10.dp,
        driftX = 4.dp,
        sway = 5f,
        periodMillis = 7400,
        startPhase = 0.55f,
    ),
    HoloPropSpec(
        resId = R.drawable.ca,
        width = 148.dp,
        height = 132.dp,
        xFraction = 0.29f,
        yFraction = 0.645f,
        rotation = 6f,
        floatDistance = 11.dp,
        driftX = 6.dp,
        sway = 3f,
        periodMillis = 8300,
        startPhase = 0.35f,
    ),
    HoloPropSpec(
        resId = R.drawable.tu,
        width = 84.dp,
        height = 100.dp,
        xFraction = 0.78f,
        yFraction = 0.615f,
        rotation = -8f,
        floatDistance = 9.dp,
        driftX = 3.dp,
        sway = 4f,
        periodMillis = 5600,
        startPhase = 0.75f,
    ),
)

/**
 * The key art as a full-screen backdrop.
 *
 * Each prop is placed by fraction of the screen rather than by a layout, so it
 * can land where the key art puts it — over the brand block, out at an edge, or
 * half behind the action card. The layer is drawn behind the content column, so
 * the column needs to know nothing about it.
 */
@Composable
private fun HoloPropsBackdrop(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        HoloProps.forEach { spec ->
            HoloProp(
                spec = spec,
                placement = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = screenWidth * spec.xFraction - spec.width / 2,
                        y = screenHeight * spec.yFraction - spec.height / 2,
                    ),
            )
        }
    }
}

/**
 * Draws one prop at [placement], tilted by its spec and drifting on its own
 * slow cycle.
 *
 * Only translate and rotate animate — never scale or a layer of its own —
 * because the prop blends with BlendMode.Screen against the gradient: an
 * isolated layer would blend it against transparency instead. The dimming is
 * the paint's own alpha, which rides along with that same blend, so the black
 * background still drops away to the gradient rather than coming back as a box.
 */
@Composable
private fun HoloProp(spec: HoloPropSpec, placement: Modifier = Modifier) {
    val image = ImageBitmap.imageResource(spec.resId)
    val transition = rememberInfiniteTransition(label = "holoArt")
    // Ping-pongs its own phase → 1 over this prop's cycle; `drift` maps that to
    // a signed -1..1 offset from the resting position. A symmetric in-out curve
    // gives both turnarounds the same shape: the prop slows into the top of its
    // arc and eases out of it instead of reversing on a hard corner.
    val cycle by transition.animateFloat(
        initialValue = spec.startPhase,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = spec.periodMillis,
                easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f),
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "holoDrift",
    )
    val drift = cycle * 2f - 1f

    // A second cycle on the same prop, at the golden ratio to the first and
    // starting from the far end, so the two never line up: the path stops
    // repeating and the prop reads as floating on its own current rather than
    // sliding back and forth on a track. Still translate and rotate only — no
    // layer and no scale — so the Screen blend stays against the gradient.
    val crossCycle by transition.animateFloat(
        initialValue = 1f - spec.startPhase,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (spec.periodMillis * 1.618f).toInt(),
                easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f),
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "holoCrossDrift",
    )
    val cross = crossCycle * 2f - 1f

    Box(
        modifier = placement
            .offset(
                x = spec.driftX * drift + spec.driftX * 0.55f * cross,
                y = spec.floatDistance * drift + spec.floatDistance * 0.3f * cross,
            )
            .size(width = spec.width, height = spec.height)
            .rotate(spec.rotation + spec.sway * drift + spec.sway * 0.4f * cross)
            .drawWithCache {
                val dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                onDrawBehind {
                    drawImage(
                        image = image,
                        dstSize = dstSize,
                        alpha = PropFade,
                        blendMode = BlendMode.Screen,
                    )
                }
            },
    )
}

/** The white action card: Google, guest, and the cookie / token link. */
@Composable
private fun LoginCard(
    onGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
    onTokenClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The primary way in carries the sheen; the secondary one is left
            // quiet, so the eye is told which of the two to reach for before
            // either is read.
            LoginActionButton(
                fill = GoogleButton,
                sheen = true,
                onClick = onGoogleClick,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    tint = OnGoogleButton,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.glossy_continue_google),
                    color = OnGoogleButton,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(10.dp))

            LoginActionButton(
                fill = GuestButton,
                onClick = onGuestClick,
            ) {
                Text(
                    text = stringResource(R.string.glossy_continue_guest),
                    color = OnGuestButton,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTokenClick)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.glossy_cookie_login),
                    color = CookieLink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * One pill inside the action card: [fill] behind centred [content], with press
 * feedback of its own and, when [sheen] is set, a slow highlight crossing it.
 *
 * The press answers on the spring's scale rather than on a ripple, because the
 * pill is small and its own colour is the whole point: shrinking it 3% says
 * "that landed" without washing the colour out.
 */
@Composable
private fun LoginActionButton(
    fill: Color,
    onClick: () -> Unit,
    sheen: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )

    // Restarts rather than ping-pongs: the highlight has to leave the pill
    // entirely before the next one starts, or it looks like a stuck band.
    val sheenOffset by rememberInfiniteTransition(label = "sheen").animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheenSweep",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(percent = 50))
            .background(fill)
            .clickable(
                interactionSource = interactionSource,
                // No ripple: the pill is the feedback.
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )

        if (sheen) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = sheenOffset * size.width
                        // Brightest as it crosses the middle of the pill, gone
                        // by the time it reaches either edge.
                        alpha = (1f - kotlin.math.abs(sheenOffset)).coerceIn(0f, 1f)
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                        ),
                    ),
            )
        }
    }
}

// ============================================================================
// Preview
// ============================================================================

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun GlossyLoginScreenPreview() {
    GlossyLoginScreen(
        onGoogleClick = {},
        onGuestClick = {},
        onTokenClick = {},
    )
}
