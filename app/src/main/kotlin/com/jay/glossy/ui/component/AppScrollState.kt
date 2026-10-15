/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * Whether the screen under the app chrome has been scrolled away from its top.
 *
 * The chrome needs one answer to one question — is the listener reading down the
 * page, or back at the top? — and no screen was answering it, because every
 * screen owns a different list. So it is answered once, at the root, from the
 * nested scroll every scrolling container already reports through, and read back
 * through [LocalAppScrolledToTop].
 *
 * A threshold rather than a sign check: a scroll of a few pixels should not be
 * enough to start an animation, and scrolling back up has to be enough to end
 * one. Reversing direction starts a fresh run instead of cancelling the last
 * one, so a wobble at the top cannot leave the chrome collapsed.
 */
class AppScrollState {
    var isScrolledToTop by mutableStateOf(true)
        private set

    private var accumulated = 0f

    val connection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                track(available.y)
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                track(consumed.y)
                return Offset.Zero
            }
        }

    private fun track(deltaY: Float) {
        if (deltaY == 0f) return
        accumulated = if (accumulated * deltaY < 0f) deltaY else accumulated + deltaY
        when {
            accumulated <= -CollapseThreshold && isScrolledToTop -> isScrolledToTop = false
            accumulated >= CollapseThreshold && !isScrolledToTop -> isScrolledToTop = true
        }
    }

    private companion object {
        /** About one deliberate swipe of a finger, which is what the gesture is. */
        const val CollapseThreshold = 24f
    }
}

/**
 * Read by the chrome that reacts to scrolling — see [AppScrollState].
 *
 * Defaults to `true`, so anything composed outside the app root (a preview, a
 * test) keeps the expanded shape rather than collapsing around a signal it
 * never received.
 */
val LocalAppScrolledToTop = compositionLocalOf { true }
