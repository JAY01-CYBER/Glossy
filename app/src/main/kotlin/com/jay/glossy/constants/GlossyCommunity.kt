/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.constants

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.jay.glossy.R

/**
 * The project's public channels.
 *
 * One list, used by the onboarding "community" page and the Settings footer, so
 * a link only ever has to be corrected in one place. Labels are the platforms'
 * own names and stay untranslated; only the descriptions are localised.
 */
enum class CommunityChannel(
    val label: String,
    @StringRes val descriptionRes: Int,
    val url: String,
    @DrawableRes val iconRes: Int,
    /** The platform's own brand colour, used for the icon tile. */
    val brandColor: Color,
) {
    INSTAGRAM(
        label = "Instagram",
        descriptionRes = R.string.glossy_community_instagram_desc,
        url = "https://www.instagram.com/glossyplayer/",
        iconRes = R.drawable.instagram,
        brandColor = Color(0xFFE1306C),
    ),
    DISCORD(
        label = "Discord",
        descriptionRes = R.string.glossy_community_discord_desc,
        url = "https://discord.com/invite/ZzSkcVxuGX",
        iconRes = R.drawable.discord,
        brandColor = Color(0xFF5865F2),
    ),
    TELEGRAM(
        label = "Telegram",
        descriptionRes = R.string.glossy_community_telegram_desc,
        url = "https://t.me/glossyplayer",
        iconRes = R.drawable.telegram,
        brandColor = Color(0xFF2CA5E0),
    ),
}
