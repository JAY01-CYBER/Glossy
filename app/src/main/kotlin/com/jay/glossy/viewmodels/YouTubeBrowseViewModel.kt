/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.viewmodels

import com.jay.glossy.R

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.filterYoutubeShorts
<<<<<<< HEAD
import com.metrolist.innertube.models.YouTubeLocale
=======
>>>>>>> origin/main
import com.metrolist.innertube.pages.BrowseResult
import com.jay.glossy.constants.HideExplicitKey
import com.jay.glossy.constants.HideVideoSongsKey
import com.jay.glossy.constants.HideYoutubeShortsKey
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.get
import com.jay.glossy.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")
<<<<<<< HEAD
    private val region = savedStateHandle.get<String>("region")
=======
>>>>>>> origin/main

    val result = MutableStateFlow<BrowseResult?>(null)

    init {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
<<<<<<< HEAD
            
            // For charts, temporarily set locale based on selected region
            val originalLocale = YouTube.locale
            if (browseId == "FEmusic_charts" && region != null) {
                val regionLanguage = when (region) {
                    "US", "GB" -> "en"
                    "IN" -> "hi"
                    "JP" -> "ja"
                    "KR" -> "ko"
                    "ZZ" -> "en"
                    else -> originalLocale.hl
                }
                YouTube.locale = YouTubeLocale(
                    gl = region,
                    hl = regionLanguage
                )
            }
            
=======
>>>>>>> origin/main
            YouTube
                .browse(browseId, params)
                .onSuccess {
                    result.value = it
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                }.onFailure {
                    reportException(it)
                }
<<<<<<< HEAD
            
            // Restore original locale
            if (browseId == "FEmusic_charts" && region != null) {
                YouTube.locale = originalLocale
            }
=======
>>>>>>> origin/main
        }
    }
}
