/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.extensions

import com.jay.glossy.db.entities.FormatEntity

/**
 * Whether this row describes a master handed over by the lossless source,
 * rather than one of YouTube's Opus/AAC streams.
 *
 * The MIME type is the primary answer; the codec string covers a module that
 * reported itself in either field. WAV and PCM count because an addon may
 * serve an uncompressed master.
 */
fun FormatEntity.isLosslessMaster(): Boolean =
    listOf(mimeType, codecs).any { value ->
        value.contains("flac", ignoreCase = true) ||
            value.contains("alac", ignoreCase = true) ||
            value.contains("wav", ignoreCase = true) ||
            value.contains("pcm", ignoreCase = true)
    }
