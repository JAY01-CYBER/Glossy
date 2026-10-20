/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Home header: the time-based greeting — "Good morning" / "Good afternoon" /
 * "Good evening", picked from the device clock — above the user's name in the
 * display scale.
 *
 * Behind it sits either an aurora wash of the theme accent bleeding in from the
 * top-left corner or, in a tonal theme, a solid header band: the skin decides
 * by filling [GlossyPalette.HeaderBand], and the text follows it.
 *
 * The header deliberately carries no actions of its own: the
 * per-song actions (download, like) live on the song's own menus and in the
 * player, so the top of the home feed stays a heading rather than a toolbar.
 * The category pills that belong with it (Podcasts, Romance, Relax…) are the
 * home feed's own chip row, drawn right below.
 */
package com.jay.glossy.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.R
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import java.util.Calendar

/** How far the wash reaches: strong in the corner, gone by the first section. */
private const val AuroraCore = 0.30f
private const val AuroraMid = 0.10f

/** Home header: greeting and the user's name. */
@Composable
fun GreetingSection(
    userName: String,
    avatarUrl: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    onListenTogetherClick: (() -> Unit)? = null,
) {
    // The wash behind the header drifts instead of sitting still. It is one
    // radial gradient repainted per frame — no blur, no layer, no
    // recomposition — and with ambient motion off the drift pins at zero and no
    // animation is created at all.
    val driftState: State<Float> =
        if (rememberAmbientMotionEnabled()) {
            rememberInfiniteTransition(label = "headerAurora")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(16000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "headerAuroraDrift",
                )
        } else {
            remember { mutableStateOf(0f) }
        }

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
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

    // Captured outside the draw lambda: GlossyPalette reads are composition-only.
    val aurora = MaterialTheme.colorScheme.primary
    val band = GlossyPalette.HeaderBand
    // A filled band means the skin wants a tonal header — Material 3's
    // primaryContainer — so the text has to come from onPrimaryContainer rather
    // than the page's own onSurface.
    val tonalBand = band.alpha > 0f
    // On the page itself the eyebrow is the design's quiet secondary grey and
    // the wordmark is primary text; the rose is reserved for the Listen
    // Together action beside the avatar, not the greeting.
    val greetColor = if (tonalBand) GlossyPalette.OnHeaderBand else GlossyPalette.TextSecondary
    val textPrimary = if (tonalBand) GlossyPalette.OnHeaderBand else GlossyPalette.TextPrimary
    val actionColor = if (tonalBand) GlossyPalette.OnHeaderBand else GlossyPalette.Accent
    // A tonal band is a card of its own — fully rounded and inset from the
    // edges. The wash design runs edge to edge and only rounds its bottom.
    val bandShape = if (tonalBand) {
        RoundedCornerShape(28.dp)
    } else {
        RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (tonalBand) Modifier.padding(horizontal = 12.dp, vertical = 6.dp) else Modifier,
            )
            .clip(bandShape)
            .drawBehind {
                if (tonalBand) {
                    drawRect(color = band)
                } else {
                    // Read in the draw phase: the drift repaints the wash without
                    // recomposing the header or its children.
                    val drift = driftState.value
                    drawRect(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        aurora.copy(alpha = AuroraCore),
                                        aurora.copy(alpha = AuroraMid),
                                        Color.Transparent,
                                    ),
                                center =
                                    Offset(
                                        size.width * (0.06f + drift * 0.06f),
                                        -size.height * (0.30f - drift * 0.08f),
                                    ),
                                radius = size.width * (1.20f + drift * 0.12f),
                            ),
                    )
                }
            }
            .padding(
                start = GlossyDimens.ScreenPadding,
                end = GlossyDimens.ScreenPadding,
                top = 14.dp,
                bottom = 18.dp,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                // The greeting and the listener's name share one uppercase
                // eyebrow line, exactly as the design has them: the hour and
                // the person on one small tracked-out label.
                Text(
                    text = "$greeting, $firstName".uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.3.sp,
                    color = greetColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
            }
            // Listen Together sits in the header beside the listener's own
            // avatar, where the old top bar kept it — one tap from the top of
            // the feed without taking a row of its own.
            if (onListenTogetherClick != null) {
                HeaderTogetherButton(
                    label = stringResource(R.string.together),
                    tint = actionColor,
                    onClick = onListenTogetherClick,
                )
            }
            if (avatarUrl != null || onAvatarClick != null) {
                // The design groups the two header actions with a hand's width
                // between them; the button carries no margin of its own.
                Spacer(Modifier.width(12.dp))
                GlossyInitialAvatar(
                    name = firstName,
                    size = 42.dp,
                    imageUrl = avatarUrl,
                    modifier =
                        Modifier.clip(CircleShape).then(
                            if (onAvatarClick != null) {
                                Modifier.clickable(onClick = onAvatarClick)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

/**
 * The compact "Listen together" button of the design's home header: a people
 * icon and its label in an outlined pill that is only as wide as its content.
 *
 * The pill itself is 36dp tall, but the clickable box around it is 48dp so the
 * touch target still meets the accessibility minimum without making the header
 * look like a toolbar.
 */
@Composable
private fun HeaderTogetherButton(
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val pill = RoundedCornerShape(GlossyDimens.CornerPill)
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(pill)
                .border(GlossyDimens.Hairline, tint.copy(alpha = 0.38f), pill)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.group_outlined),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = tint,
                maxLines = 1,
            )
        }
    }
}
