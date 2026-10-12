package com.jay.glossy.canvas

/**
 * A canvas provider could not answer the question at all — the network was
 * down, the request was rate limited, the service returned an error — as
 * opposed to answering that this song has no canvas.
 *
 * The two look identical to a caller holding only a null, and they call for
 * opposite reactions. "No canvas" is the truth about the song and asking again
 * changes nothing; "could not ask" means the question was never answered and a
 * retry is worthwhile. Providers used to swallow every failure into a null, so
 * every lookup that fell over was recorded as a song with no canvas and
 * written off ([com.jay.glossy.ui.player.CanvasResolver.wasDefinitiveMiss]).
 *
 * A provider that returns null without throwing is saying something true: it
 * asked, and there is nothing for this song.
 */
class CanvasLookupUnavailable(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
