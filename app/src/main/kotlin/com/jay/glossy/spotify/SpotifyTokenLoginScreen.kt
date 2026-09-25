package com.jay.glossy.spotify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.R
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.ui.utils.backToMain
import kotlinx.coroutines.launch

/**
 * Spotify token login: paste the sp_dc cookie token directly instead of
 * signing in through the WebView. Once a valid token is saved, the animated
 * Spotify canvas works and Spotify playlists can be imported.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyTokenLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(false) }
    var checkingConnection by remember { mutableStateOf(true) }
    var tokenInput by rememberSaveable { mutableStateOf("") }
    var tokenStatus by remember { mutableStateOf<String?>(null) }
    var checkingToken by remember { mutableStateOf(false) }
    var playlistUrl by rememberSaveable { mutableStateOf("") }
    var playlistName by rememberSaveable { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        connected = SpotifySession.cookie(context).isNotBlank()
        checkingConnection = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Reserve space for the floating TopAppBar above.
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        )

        if (checkingConnection) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        } else if (!connected) {
            Text(
                "Paste your sp_dc token to log in. " +
                    "This enables the animated Spotify canvas and playlist import.",
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("sp_dc token") },
                placeholder = { Text("Paste your sp_dc cookie value") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    enabled = !checkingToken && tokenInput.trim().length > 20,
                    onClick = {
                        checkingToken = true
                        tokenStatus = null
                        scope.launch {
                            tokenStatus = SpotifySession.saveAndValidateToken(context, tokenInput.trim())
                            if (tokenStatus == "Spotify connected") connected = true
                            checkingToken = false
                        }
                    },
                ) {
                    if (checkingToken) CircularProgressIndicator(Modifier.size(20.dp))
                    else Text("Check & log in")
                }
                tokenStatus?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (it == "Spotify connected") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else {
            Text(
                "Spotify connected",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = playlistUrl,
                onValueChange = { playlistUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Spotify playlist URL") },
                singleLine = true,
            )
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Local playlist name") },
                singleLine = true,
            )
            Button(
                enabled = !isImporting && playlistUrl.isNotBlank(),
                onClick = {
                    isImporting = true
                    message = null
                    scope.launch {
                        // Extract playlist ID properly for the new importer
                        val playlistId = Regex("playlist[/:]([A-Za-z0-9]+)").find(playlistUrl)?.groupValues?.get(1) ?: ""
                        message = runCatching {
                            if (playlistId.isBlank()) error("Enter a valid Spotify playlist URL")
                            SpotifyPlaylistImporter.importPlaylist(
                                database = database,
                                playlistId = playlistId,
                                playlistName = playlistName.ifBlank { "Spotify playlist" },
                            )
                        }.fold({ "Imported $it songs" }, { "Import failed: ${it.message ?: "Unknown error"}" })
                        isImporting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isImporting) CircularProgressIndicator(Modifier.size(20.dp))
                else Text("Import playlist")
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        SpotifySession.clear(context)
                        connected = false
                        tokenInput = ""
                        tokenStatus = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Disconnect Spotify") }
        }
    }

    TopAppBar(
        title = { Text("Spotify Token Login") },
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
        },
    )
}
