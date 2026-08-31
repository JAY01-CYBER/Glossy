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

    fun loadMixes() {
        // Agar pehle se loading ho rahi hai ya data hai, toh wapas call mat karo
        if (_mixPlaylists.value.isNotEmpty() || _isLoading.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // 1. Home Page fetch karo taaki "Mixes" wala chip mil sake
                val homePage = YouTube.home().getOrNull()
                val mixChip = homePage?.chips?.find { it.title.contains("mix", ignoreCase = true) }

                // 2. Agar "Mixes" chip mil gaya, toh exact uski list fetch karo, warna home sections use karo
                val mixSections = if (mixChip?.endpoint?.params != null) {
                    YouTube.home(params = mixChip.endpoint.params).getOrNull()?.sections ?: homePage?.sections
                } else {
                    homePage?.sections
                }

                val fetchedMixes = mutableListOf<PlaylistItem>()

                // 3. Exact mixes filter karo (RD prefix aur mix names check karke)
                mixSections?.forEach { section ->
                    section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                        val title = playlist.title ?: ""
                        val isMix = playlist.id.startsWith("RD") && 
                                    (title.contains("mix", ignoreCase = true) || 
                                     playlist.id == "RDMM" || 
                                     playlist.id.startsWith("RDTMAK") ||
                                     playlist.id.startsWith("RDAMPL"))
                                     
                        if (isMix) {
                            fetchedMixes.add(playlist)
                        }
                    }
                }
                
                // Duplicates hata kar State update kar do
                _mixPlaylists.value = fetchedMixes.distinctBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
