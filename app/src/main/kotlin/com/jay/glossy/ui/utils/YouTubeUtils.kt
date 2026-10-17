/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:Suppress("LocalVariableName")

package com.jay.glossy.ui.utils

import com.jay.glossy.R

/**
 * Hosts that serve YouTube *video stills* — the frames on a watch page rather
 * than a track's cover.
 *
 * Matched as a family rather than by the one host seen in the wild
 * (`i.ytimg.com`), because YouTube has moved these before and a missed host is
 * silent: the URL is simply not recognised as a frame, and it gets drawn as if
 * it were square cover art.
 */
private val VideoStillHost = Regex("""(?:https?:)?//[a-z0-9-]*ytimg\.com/""")

/**
 * Whether this URL is a YouTube video frame instead of album art.
 *
 * The two are not the same shape and cannot be drawn the same way. Album art is
 * square, and the image service behind it crops to whatever size the app asks
 * for — see [resize], which is why a cover fills a square tile exactly. A video
 * frame is 16:9 and comes from a different service that [resize] cannot rewrite
 * at all (an `=w-h` suffix on `i.ytimg.com` answers 404 — verified live).
 *
 * Measured against the live API: the music endpoints hand out
 * `i.ytimg.com/vi/<id>/hqdefault.jpg` at 400x225 and `hq720.jpg` at 1280x720,
 * both 16:9 and neither carrying baked-in bars.
 */
fun isYouTubeVideoStill(url: String?): Boolean =
    url != null && VideoStillHost.containsMatchIn(url)

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    // Match BOTH lh3 and yt3 googleusercontent: YouTube migrated music/album art from
    // lh3.googleusercontent.com to yt3.googleusercontent.com. Both serve the same =wW-hH resize
    // params; matching only lh3 silently no-ops on the new host, so the player upscales the raw
    // ~60px thumbnail (blurry). Verified live: a yt3 URL + =w544-h544 returns a sharp full-size image.
    "https://(?:lh3|yt3)\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    // Deliberately not a dead end for video frames: there is no size this can
    // ask `i.ytimg.com` for that it will honour, so the frame is left exactly as
    // the API gave it and it is the caller's job to draw it as a frame — that
    // is what [isYouTubeVideoStill] is for.
    return this
}
