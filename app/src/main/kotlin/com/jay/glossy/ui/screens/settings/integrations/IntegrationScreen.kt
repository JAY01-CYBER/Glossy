/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.settings.integrations

import com.jay.glossy.R

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.spotify.SpotifyLoginPrewarm
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.ui.component.IntegrationCard
import com.jay.glossy.ui.component.IntegrationCardItem
import com.jay.glossy.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    navController: NavController
) {
    val context = LocalContext.current

    // Start loading the Spotify login page right away: by the time the user
    // taps Spotify Integration and the login screen opens, the multi-second
    // page fetch is already well underway (or done).
    LaunchedEffect(Unit) {
        SpotifyLoginPrewarm.warm(context)
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        IntegrationCard(
            title = stringResource(R.string.general),
            items = listOf(
                IntegrationCardItem(
                    icon = painterResource(R.drawable.discord),
                    title = { Text(stringResource(R.string.discord_integration)) },
                    onClick = { navController.navigate("settings/integrations/discord") }
                ),
                IntegrationCardItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.lastfm_integration)) },
                    onClick = {
                        navController.navigate("settings/integrations/lastfm")
                    }
                ),
                IntegrationCardItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.spotify_integration)) },
                    onClick = { navController.navigate("settings/integrations/spotify") }
                ),
                IntegrationCardItem(
                    icon = painterResource(R.drawable.key),
                    title = { Text("Spotify Token Login") },
                    description = { Text("Paste your sp_dc token to enable Canvas & playlist import") },
                    onClick = { navController.navigate("settings/integrations/spotify_token") }
                )
            )
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.integrations)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}
