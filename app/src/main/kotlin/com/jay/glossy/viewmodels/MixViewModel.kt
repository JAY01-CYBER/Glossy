/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MixViewModel @Inject constructor() : ViewModel() {

    private val _mixPlaylists = MutableStateFlow<List<YTItem>>(emptyList())
    val mixPlaylists = _mixPlaylists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadMixes() {
        if (_mixPlaylists.value.isNotEmpty() || _isLoading.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Seedha naya YouTube API function hit karo
                val mixes = YouTube.getMixedForYou().getOrNull() ?: emptyList()
                
                // 'VL' prefix hatao taaki playlist properly open ho
                val cleanMixes = mixes.map { item ->
                    when (item) {
                        is PlaylistItem -> item.copy(id = item.id.removePrefix("VL"))
                        is AlbumItem -> item.copy(browseId = item.browseId.removePrefix("VL"))
                        else -> item
                    }
                }
                
                _mixPlaylists.value = cleanMixes
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
