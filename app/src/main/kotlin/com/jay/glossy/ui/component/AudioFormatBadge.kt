/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jay.glossy.db.entities.FormatEntity
import java.util.Locale

/**
 * Names the stream that is actually being fetched, as one short line: the
 * container, the codec and the bitrate — `WebM · Opus · 130 kbps`.
 *
 * This is deliberately not the *setting* (Auto / Low / High / Very high): the
 * setting is what was asked for, and the interesting answer is whether the
 * request was honoured — whether the high tier really landed on a WebM stream,
 * or whether a track only offered AAC and fell back. [AudioQuality] is chosen
 * once in settings; this is per song, so it belongs next to the song.
 *
 * Renders nothing at all without a format: before the first stream is resolved
 * there is no honest answer to give, and an empty chip would only be noise.
 */
@Composable
fun AudioFormatBadge(
    format: FormatEntity?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    if (format == null) return

    Text(
        text = audioFormatSummary(format),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            // A pill rather than a rectangle so it reads as a label on the row
            // it describes, not as another button.
            .background(containerColor.copy(alpha = 0.55f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * The badge's text, built from a resolved [FormatEntity]. Kept separate from
 * the composable so the player menu and any future queue row can print the same
 * words without drawing the chip.
 */
fun audioFormatSummary(format: FormatEntity): String = buildString {
    val lossless = isLossless(format)
    append(containerName(format.mimeType))
    // A lossless master names itself once: "FLAC · FLAC" would repeat the same
    // word, and the number worth printing for it is the rate it runs at rather
    // than a bitrate that only restates it.
    if (!lossless) {
        codecName(format.codecs).takeIf { it.isNotEmpty() }?.let { append(" · ").append(it) }
    }
    if (lossless) {
        format.sampleRate?.takeIf { it > 0 }?.let {
            append(" · ").append(String.format(Locale.ROOT, "%.1f kHz", it / 1000f))
        }
    } else if (format.bitrate > 0) {
        append(" · ").append(format.bitrate / 1000).append(" kbps")
    }
}

/**
 * Whether the row describes a master from the lossless source rather than one
 * of YouTube's streams. The MIME type is the primary answer; the codec string
 * covers a module that reported itself in either field.
 */
private fun isLossless(format: FormatEntity): Boolean =
    listOf(format.mimeType, format.codecs).any { value ->
        value.contains("flac", ignoreCase = true) || value.contains("alac", ignoreCase = true)
    }

/**
 * YouTube serves the same audio in either a WebM (Opus) or an MP4 (AAC)
 * container, and which one arrived is the whole question the badge exists to
 * answer — so the container is named the way a person says it, not the way the
 * MIME type spells it.
 */
private fun containerName(mimeType: String): String = when {
    mimeType.contains("flac", ignoreCase = true) -> "FLAC"
    mimeType.contains("wav", ignoreCase = true) -> "WAV"
    mimeType.contains("webm", ignoreCase = true) -> "WebM"
    mimeType.contains("mp4", ignoreCase = true) -> "M4A"
    mimeType.contains("mpeg", ignoreCase = true) -> "MP3"
    mimeType.contains("ogg", ignoreCase = true) -> "Ogg"
    else -> mimeType.substringAfter('/', mimeType).ifEmpty { "Audio" }
}

/**
 * The codec string arrives in the wild as everything from `opus` to
 * `mp4a.40.2`, so the two families YouTube actually serves are spelled out and
 * anything else is shown as it came.
 */
private fun codecName(codecs: String): String = when {
    codecs.contains("opus", ignoreCase = true) -> "Opus"
    codecs.contains("mp4a", ignoreCase = true) -> "AAC"
    codecs.contains("vorbis", ignoreCase = true) -> "Vorbis"
    else -> codecs.uppercase()
}
