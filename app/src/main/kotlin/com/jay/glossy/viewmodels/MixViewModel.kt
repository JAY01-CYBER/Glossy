/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MixViewModel @Inject constructor() : ViewModel() {

    private val _mixPlaylists = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val mixPlaylists = _mixPlaylists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadMixes(accountPlaylists: List<PlaylistItem>? = null) {
        // Prevent duplicate API calls if already loaded or currently loading
        if (_mixPlaylists.value.isNotEmpty() || _isLoading.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                YouTube.mixedForYou().onSuccess { mixes ->
                    val finalMixes = mutableListOf<PlaylistItem>()

                    // 1. Keep 'Liked Music' pinned at the top
                    accountPlaylists?.find { 
                        it.id == "LM" || it.title.equals("Liked Music", ignoreCase = true) 
                    }?.let {
                        finalMixes.add(it)
                    }

                    // 2. Add the actual YouTube Mixes
                    finalMixes.addAll(mixes)
                    
                    // Update state after removing any potential duplicates
                    _mixPlaylists.value = finalMixes.distinctBy { it.id }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
