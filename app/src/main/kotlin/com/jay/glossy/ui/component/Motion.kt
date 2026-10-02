/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Motion — and when not to spend it.
 *
 * Glossy is full of small ambient animations: the mini player's drifting glow,
 * the equalizer next to the current track, the wash behind the home header, the
 * notes over the vinyl. They are what makes the app feel alive, and every one
 * of them is a redraw that a slow phone has to pay for on every single frame,
 * whether or not anyone is looking at it.
 *
 * [rememberAmbientMotionEnabled] is the single answer to "may we animate right
 * now", so no surface has to invent its own rule:
 *
 *  - Android's animation scale is at zero — the "Remove animations" switch in
 *    developer options, which a lot of people on budget phones keep on because
 *    the whole system feels faster with it.
 *  - The user turned on "Reduce animation" in Appearance.
 *  - The device is a low-RAM (go-edition class) phone. Those start with the
 *    Appearance switch already on: a cheap phone gets a calm app out of the
 *    box, and the switch is right there if they want the motion back.
 *
 * Surfaces that obey it draw one static frame instead of scheduling work, so
 * "off" costs nothing rather than costing less.
 */

package com.jay.glossy.ui.component

import android.app.ActivityManager
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jay.glossy.constants.ReduceMotionKey
import com.jay.glossy.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.cos

/** One full turn of a bar's rise and fall. */
private const val TWO_PI = 2f * PI.toFloat()

/** Where each bar sits in the cycle, so the row never moves in lockstep. */
private const val BarPhaseStep = 0.23f

/** Shortest the bars get. Zero would make the row look like it vanished. */
private const val MinBarScale = 0.28f

/** How solid a frozen (paused) indicator is. */
private const val PausedBarAlpha = 0.45f

/** The resting heights a paused row settles on. */
private const val RestingPhase = 0.36f

/** Whether this device is a low-RAM (go-edition class) phone. */
fun Context.isLowRamDevice(): Boolean =
    (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

/** Android's own "Remove animations" switch: an animation scale of zero. */
private fun animationsAllowedBySystem(context: Context): Boolean =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) != 0f

/**
 * The value the Appearance switch starts at on this device — see the file
 * header. Both the switch and [rememberAmbientMotionEnabled] read it from here
 * so the screen can never disagree with what the app actually does.
 */
@Composable
fun rememberDefaultReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isLowRamDevice() }
}

/**
 * Whether the app's ambient animations may run right now. See the file header
 * for what says no, and expect `false` to mean "draw a still frame", not
 * "draw the same thing slower".
 */
@Composable
fun rememberAmbientMotionEnabled(): Boolean {
    val context = LocalContext.current
    val (reduceMotion) = rememberPreference(
        ReduceMotionKey,
        defaultValue = rememberDefaultReduceMotion(),
    )
    var systemAllows by remember { mutableStateOf(animationsAllowedBySystem(context)) }

    // The developer switch can be flipped while the app is open, and it is
    // worth following live: someone turning "Remove animations" on is telling
    // us to stop, not to stop next launch.
    DisposableEffect(context) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    systemAllows = animationsAllowedBySystem(context)
                }
            }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    return systemAllows && !reduceMotion
}

/**
 * The "now playing" equalizer: a few bars that rise and fall while music plays.
 *
 * Built for slow phones on purpose:
 *
 *  - The phase is read *inside* each bar's `graphicsLayer` block, so a frame
 *    costs a handful of layer transforms and triggers no recomposition and no
 *    relayout. The visualizer this replaces animated `fillMaxHeight` on four
 *    boxes, which did both, on the one surface that is on screen for the whole
 *    song.
 *  - Nothing is scheduled while playback is paused: the bars settle at their
 *    resting heights and the animation loop is not even created.
 *  - With ambient motion off the same still frame is drawn and the frame clock
 *    is left alone entirely.
 */
@Composable
fun PlayingBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    barCount: Int = 4,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.5.dp,
    barHeight: Dp = 14.dp,
) {
    val animating = isPlaying && rememberAmbientMotionEnabled()

    val phase: State<Float> =
        if (animating) {
            val transition = rememberInfiniteTransition(label = "playingBars")
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "playingBarsPhase",
            )
        } else {
            remember { mutableStateOf(RestingPhase) }
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(barCount) { index ->
            Box(
                modifier =
                    Modifier
                        .size(width = barWidth, height = barHeight)
                        .graphicsLayer {
                            // Read through the State here, in the layer block:
                            // this is the only code that re-runs per frame.
                            val step = (phase.value + index * BarPhaseStep) % 1f
                            val wave = 0.5f - 0.5f * cos(TWO_PI * step)
                            scaleY = MinBarScale + (1f - MinBarScale) * wave
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            alpha = if (isPlaying) 1f else PausedBarAlpha
                        }
                        .clip(RoundedCornerShape(barWidth / 2))
                        .background(color),
            )
        }
    }
}
