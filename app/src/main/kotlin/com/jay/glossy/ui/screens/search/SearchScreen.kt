/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.search

import com.jay.glossy.R

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import com.jay.glossy.LocalNavController
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.jay.glossy.LocalDatabase
import com.jay.glossy.LocalIsPlayerExpanded
import com.jay.glossy.LocalPlayerConnection
import com.jay.glossy.constants.PauseSearchHistoryKey
import com.jay.glossy.constants.SearchSource
import com.jay.glossy.constants.SearchSourceKey
import com.jay.glossy.db.entities.SearchHistory
import com.jay.glossy.playback.queues.YouTubeQueue
import com.jay.glossy.ui.component.HideOnScrollFAB
import com.jay.glossy.ui.component.TiledListRows
import com.jay.glossy.utils.SearchRoutes
import com.jay.glossy.utils.rememberEnumPreference
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    pureBlack: Boolean,
    savedStateHandle: SavedStateHandle,
) {
    val navController = LocalNavController.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val playerConnection = LocalPlayerConnection.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lazyListState = rememberLazyListState()
    var isHandlingScrollToTop by remember { mutableStateOf(false) }

    val scrollToTopCount by savedStateHandle.getStateFlow("scrollToTopCount", 0).collectAsStateWithLifecycle(initialValue = 0)

    var lastHandledCount by rememberSaveable { mutableIntStateOf(0) }
    // The discovery page opens with its title, field and shelves visible, so the
    // keyboard waits until a gesture asks for it — opening it here would cover
    // the page the user came to look at.
    LaunchedEffect(Unit) {
        if (!isPlayerExpanded) {
            kotlinx.coroutines.delay(100)
            try {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            } catch (e: Exception) {
            }
        }
    }
    LaunchedEffect(scrollToTopCount) {
        if (scrollToTopCount > lastHandledCount) {
            lastHandledCount = scrollToTopCount
            isHandlingScrollToTop = true

            kotlinx.coroutines.delay(100)

            if (!isPlayerExpanded) {
                focusManager.clearFocus(force = true)
                kotlinx.coroutines.delay(50)
                try {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                } catch (e: Exception) {
                }
            }

            kotlinx.coroutines.delay(500)
            isHandlingScrollToTop = false
        }
    }

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    fun handleSearch(searchQuery: String) {
        if (searchQuery.isEmpty()) {
            return
        }

        focusManager.clearFocus()

        when (val parsedUrl = YouTubeUrlParser.parse(searchQuery)) {
            is YouTubeUrlParser.ParsedUrl.Video -> {
                playerConnection?.playQueue(
                    YouTubeQueue(
                        WatchEndpoint(videoId = parsedUrl.id),
                    ),
                )
            }

            is YouTubeUrlParser.ParsedUrl.Playlist -> {
                navController.navigate("online_playlist/${parsedUrl.id}")
            }

            is YouTubeUrlParser.ParsedUrl.Album -> {
                navController.navigate("album/MPREb_${parsedUrl.id}")
            }

            is YouTubeUrlParser.ParsedUrl.Artist -> {
                navController.navigate("artist/${parsedUrl.id}")
            }

            null -> {
                navController.navigate(SearchRoutes.resultRoute(searchQuery))
            }
        }

        if (!pauseSearchHistory) {
            coroutineScope.launch(Dispatchers.IO) {
                database.query {
                    insert(SearchHistory(query = searchQuery))
                }
            }
        }
    }

    val onSearch: (String) -> Unit = { searchQuery -> handleSearch(searchQuery) }

    val onSearchFromSuggestion: (String) -> Unit = { searchQuery -> handleSearch(searchQuery) }

    Scaffold(
        topBar = {
            // One field for the whole life of the screen: it is drawn here,
            // outside the switching body, so typing the first character no
            // longer tears down the focused field. See SearchHeaderBar.
            SearchHeaderBar(
                query = query,
                onQueryChange = { query = it },
                focusRequester = focusRequester,
                active = query.text.isNotEmpty(),
                searchSource = searchSource,
                pureBlack = pureBlack,
                onBack = { navController.navigateUp() },
                onClear = { query = TextFieldValue("") },
                onToggleSource = { searchSource = searchSource.toggle() },
                onVoice = { navController.navigate("recognition") },
                onSearch = onSearch,
            )
        },
        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .padding(top = paddingValues.calculateTopPadding())
                    .fillMaxSize(),
        ) {
            // Both search surfaces draw their rows as tiles; no other list in
            // the app does (see TiledListRows).
            when {
                // Nothing typed yet: the design's landing page, built from the
                // mood shelf and the last few searches, takes the whole body.
                query.text.isEmpty() ->
                    SearchDiscoverRoute(
                        onSearch = onSearchFromSuggestion,
                        onSeeAllRecent = { navController.navigate("history") },
                    )

                searchSource == SearchSource.LOCAL -> {
                    TiledListRows {
                        LocalSearchScreen(
                            query = query.text,
                            onDismiss = { navController.navigateUp() },
                            pureBlack = pureBlack,
                        )
                    }
                }

                else -> {
                    TiledListRows {
                        OnlineSearchScreen(
                            query = query.text,
                            onQueryChange = { query = it },
                            onSearch = onSearchFromSuggestion,
                            onDismiss = { /* Don't dismiss when searching from suggestions */ },
                            pureBlack = pureBlack,
                        )
                    }
                }
            }

            // The discovery page keeps the voice shortcut inside its field, so
            // the floating one only belongs to the results states.
            if (query.text.isNotEmpty()) {
                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.mic,
                    onClick = { navController.navigate("recognition") },
                )
            }
        }
    }

    // Handle lifecycle events to manage keyboard visibility
    DisposableEffect(lifecycleOwner, isPlayerExpanded) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        if (isHandlingScrollToTop) return@LifecycleEventObserver
                        // Always hide keyboard when resuming if player is expanded
                        if (isPlayerExpanded) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        if (isHandlingScrollToTop) return@LifecycleEventObserver
                        // Clear focus when pausing to prevent keyboard from showing on resume
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }

                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Initial check - hide keyboard if player is expanded
        if (isPlayerExpanded) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}