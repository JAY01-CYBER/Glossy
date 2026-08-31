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
        if (_mixPlaylists.value.isNotEmpty() || _isLoading.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val fetchedMixes = mutableListOf<PlaylistItem>()

                // 1. Liked Music ko sabse upar add karo
                accountPlaylists?.find { 
                    it.id == "LM" || it.title.equals("Liked Music", ignoreCase = true) 
                }?.let {
                    fetchedMixes.add(it)
                }

                // 2. SimpMusic Approach: Seedha "Mixes" wale chip ka data fetch karo
                val homePage = YouTube.home().getOrNull()
                val mixChip = homePage?.chips?.find { it.title.contains("mix", ignoreCase = true) }
                
                // Kotlin Smart Cast Fix (params ko local variable me liya)
                val params = mixChip?.endpoint?.params

                // Agar Mixes chip mil gaya toh direct uski list mangwao
                val mixSections = if (params != null) {
                    YouTube.home(params = params).getOrNull()?.sections ?: homePage?.sections
                } else {
                    homePage?.sections
                }

                // 3. Exact mixes filter karo
                mixSections?.forEach { section ->
                    section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                        val title = playlist.title ?: ""
                        val isMix = playlist.id.startsWith("RD") && 
                                    (title.contains("mix", ignoreCase = true) || 
                                     playlist.id == "RDMM" || 
                                     playlist.id.startsWith("RDTMAK") ||
                                     playlist.id.startsWith("RDAMPL"))
                                     
                        if (isMix && playlist.id != "LM") {
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
