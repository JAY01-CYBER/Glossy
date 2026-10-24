/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Home header: one rose card holding the time-based greeting — "Good morning" /
 * "Good afternoon" / "Good evening", picked from the device clock — above the
 * listener's first name.
 *
 * The card is the whole header. The app's own top bar already draws the Glossy
 * wordmark and its actions, so this section adds no second brand row of its own:
 * no wordmark, no action pill, no ghosted app name and no per-user controls
 * inside the card. It is the greeting and the name, nothing else.
 *
 * Optionally the card carries a loop of video behind that type — picked in
 * Settings › Appearance › Home Screen Layout, where the slider beside it sets
 * how strongly the clip shows (two thirds by default). The clip is decoration:
 * silent, unclickable, and finished with a light scrim, so the greeting and the
 * name keep the contrast they have on the plain card. With Reduce animation on,
 * or the system's animations off, the plain card simply draws.
 *
 * Colours come from [GlossyPalette], so the card follows the skin: the rose
 * gradient of the design is the Materialistic theme's `primaryContainer` band,
 * and the ink on top is the matching `onPrimaryContainer`.
 */
package com.jay.glossy.ui.component

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.jay.glossy.R
import com.jay.glossy.constants.DefaultGreetingAnimationOpacity
import com.jay.glossy.constants.GreetingAnimation
import com.jay.glossy.constants.GreetingAnimationKey
import com.jay.glossy.constants.GreetingAnimationOpacityKey
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.ui.theme.OutfitFontFamily
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference

/** Home header: the greeting card, and nothing else. */
@Composable
fun GreetingSection(userName: String) {
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = stringResource(
        when (currentHour) {
            in 5..11 -> R.string.glossy_good_morning
            in 12..16 -> R.string.glossy_good_afternoon
            else -> R.string.glossy_good_evening
        }
    )
    val firstName = remember(userName) {
        userName.trim().substringBefore(' ').takeIf { it.isNotBlank() } ?: userName
    }

    // A filled band means the skin wants a tonal header — Material 3's
    // primaryContainer — and the ink has to come from onPrimaryContainer rather
    // than the page's own onSurface. A skin without a band falls back to its
    // accent soft surface so the card still reads as a card.
    val band = GlossyPalette.HeaderBand
    val tonal = band.alpha > 0f
    val fill = if (tonal) band else GlossyPalette.AccentSoft
    val ink = if (tonal) GlossyPalette.OnHeaderBand else GlossyPalette.TextPrimary

    val cardShape = RoundedCornerShape(22.dp)

    val (greetingAnimation) = rememberEnumPreference(
        GreetingAnimationKey,
        defaultValue = GreetingAnimation.OFF,
    )
    // The listener's own strength for the clip, chosen in Settings › Appearance.
    // Applied to the video alone so the gradient, the wash and the type keep the
    // contrast they were tuned for.
    val (greetingOpacity) = rememberPreference(
        GreetingAnimationOpacityKey,
        defaultValue = DefaultGreetingAnimationOpacity,
    )
    // Reduce animation is a promise, not a suggestion: the clip is not composed
    // at all when it is on, so the card keeps its still gradient.
    val motionAllowed = rememberAmbientMotionEnabled()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = GlossyDimens.ScreenPadding,
                end = GlossyDimens.ScreenPadding,
                top = 10.dp,
                bottom = 4.dp,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(cardShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(fill, lerp(fill, Color.White, 0.12f)),
                    ),
                ),
        ) {
            if (greetingAnimation != GreetingAnimation.OFF && motionAllowed) {
                // The clip is a mood, not a picture: it is drawn bright enough
                // to read as motion, then the card's own colour is washed back
                // over it so the type never has to compete with a bright frame.
                GreetingVideoLoop(
                    animation = greetingAnimation,
                    opacity = greetingOpacity,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    fill.copy(alpha = 0.45f),
                                    fill.copy(alpha = 0.20f),
                                ),
                            ),
                        ),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 18.dp, vertical = 15.dp),
            ) {
                Text(
                    text = greeting,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ink.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = firstName,
                    fontFamily = OutfitFontFamily,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * One muted, looping clip behind the greeting.
 *
 * The player lives as long as the card is composed: scrolled away, it is
 * released, and started over when the card comes back. It also follows the
 * window — a home screen left behind must not keep decoding frames — and it
 * never touches audio.
 */
@Composable
private fun GreetingVideoLoop(
    animation: GreetingAnimation,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val rawRes = when (animation) {
        GreetingAnimation.NEON_GLOW -> R.raw.greeting_neon
        GreetingAnimation.SUNSET_TRAIN -> R.raw.greeting_sunset
        GreetingAnimation.BOHO_HOME -> R.raw.greeting_boho
        GreetingAnimation.HEMMING -> R.raw.greeting_hemming
        GreetingAnimation.STREET_MARKET -> R.raw.greeting_market
        GreetingAnimation.OFF -> return
    }

    val exoPlayer = remember(rawRes) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(rawRes)))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                playWhenReady = true
                prepare()
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> exoPlayer.playWhenReady = true
                Lifecycle.Event.ON_STOP -> exoPlayer.playWhenReady = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            (LayoutInflater.from(viewContext)
                .inflate(R.layout.view_greeting_video, null) as PlayerView).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                // A still frame is shown by the gradient underneath if the clip
                // has not decoded yet, so there is no black flash to cover up.
                player = exoPlayer
            }
        },
        // Set on every frame of the preference flow rather than once in the
        // factory: dragging the slider has to move the clip immediately.
        update = { view -> view.alpha = opacity.coerceIn(0.05f, 1f) },
    )
}
