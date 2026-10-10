/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens

import com.jay.glossy.R

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    object Mix : Screens(
        titleId = R.string.mix,
        iconIdInactive = R.drawable.radio, 
        iconIdActive = R.drawable.radio,
        route = "mix"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "search_input"
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        iconIdInactive = R.drawable.group_outlined,
        iconIdActive = R.drawable.group_filled,
        route = "listen_together"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.library_music_outlined,
        iconIdActive = R.drawable.library_music_filled,
        route = "library"
    )

    companion object {
        /**
         * The bottom bar of the new design: Home · Mix · Library. Search rides
         * the floating bar as its own pill, and Listen
         * Together stays in the top bar; all three are still reachable through
         * their routes.
         *
         * Explore is deliberately not here any more: its charts, moods and new
         * releases all live on Home, so the tab only ever opened a second way to
         * the same browse endpoints.
         */
        val MainScreens = listOf(Home, Mix, Library)
    }
}
