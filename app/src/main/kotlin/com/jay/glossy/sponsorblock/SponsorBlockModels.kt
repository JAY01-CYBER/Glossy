/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * SponsorBlock support ported from ArchiveTune
 * (https://github.com/rukamori/ArchiveTune), GPL-3.0.
 */

package com.jay.glossy.sponsorblock

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val DEFAULT_SPONSOR_BLOCK_API_URL = "https://sponsor.ajay.app"

/**
 * The segment kinds the public SponsorBlock database knows about.
 *
 * [apiValue] is the name the API accepts and returns — the API's spelling, not
 * ours, and the reason these are not stored by enum name: reordering or renaming
 * this list must not silently reinterpret what a listener has already chosen.
 */
enum class SponsorBlockCategory(val apiValue: String) {
    MUSIC_OFF_TOPIC("music_offtopic"),
    SPONSOR("sponsor"),
    INTRO("intro"),
    OUTRO("outro"),
    SELF_PROMOTION("selfpromo"),
    PREVIEW("preview"),
    FILLER("filler"),
    INTERACTION("interaction"),
    HOOK("hook"),
    ;

    companion object {
        fun fromApiValue(value: String): SponsorBlockCategory? =
            entries.firstOrNull { it.apiValue == value }
    }
}

data class SponsorBlockSettings(
    val enabled: Boolean,
    val categories: Set<SponsorBlockCategory>,
)

data class SponsorBlockSegment(
    val startMs: Long,
    val endMs: Long,
)

internal data class SponsorBlockRawSegment(
    val segment: List<Double>,
    val category: String,
    val actionType: String,
)

@Serializable
internal data class SponsorBlockVideoResponse(
    @SerialName("videoID") val videoId: String,
    val segments: List<SponsorBlockSegmentResponse> = emptyList(),
)

@Serializable
internal data class SponsorBlockSegmentResponse(
    val segment: List<Double> = emptyList(),
    val category: String = "",
    val actionType: String = "",
)
