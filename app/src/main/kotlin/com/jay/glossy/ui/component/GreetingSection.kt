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
 * no wordmark, no action pill and no per-user controls inside the card. The one
 * brand note it does carry is the ghosted "Glossy Nightly" set beside the name —
 * a watermark on the greaseproof card, not a heading, so it sits at low opacity
 * in the accent ink and is clipped by the card's rounded edge.
 *
 * Colours come from [GlossyPalette], so the card follows the skin: the rose
 * gradient of the design is the Materialistic theme's `primaryContainer` band,
 * and the ink on top is the matching `onPrimaryContainer`.
 */
package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.R
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import com.jay.glossy.ui.theme.OutfitFontFamily
import java.util.Calendar

/** Home header: the greeting card, and nothing else. */
@Composable
fun GreetingSection(userName: String) {
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

    // A filled band means the skin wants a tonal header — Material 3's
    // primaryContainer — and the ink has to come from onPrimaryContainer rather
    // than the page's own onSurface. A skin without a band falls back to its
    // accent soft surface so the card still reads as a card.
    val band = GlossyPalette.HeaderBand
    val tonal = band.alpha > 0f
    val fill = if (tonal) band else GlossyPalette.AccentSoft
    val ink = if (tonal) GlossyPalette.OnHeaderBand else GlossyPalette.TextPrimary

    val cardShape = RoundedCornerShape(22.dp)

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
                Row(verticalAlignment = Alignment.Bottom) {
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
                    // The watermark: set beside the name, on its baseline, and
                    // allowed to run under the card's rounded edge instead of
                    // wrapping. It is decoration only — nothing taps it.
                    Text(
                        text = stringResource(R.string.glossy_nightly),
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.5).sp,
                        color = ink.copy(alpha = 0.18f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}
