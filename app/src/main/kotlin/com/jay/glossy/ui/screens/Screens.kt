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

    object Explore : Screens(
        titleId = R.string.glossy_explore,
        iconIdInactive = R.drawable.explore_outlined,
        iconIdActive = R.drawable.album,
        route = "explore"
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
         * The bottom bar of the new design: Home · Explore · Mix · Library.
         * Search moved into the Home header, and Listen Together stays in the
         * top bar; both screens are still reachable through their routes.
         */
        val MainScreens = listOf(Home, Explore, Mix, Library)
    }
}
