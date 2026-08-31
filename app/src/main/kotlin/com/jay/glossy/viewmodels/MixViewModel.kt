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
        // Agar pehle se load ho gaya hai toh dubara network call mat karo
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

                // 2. YouTube API me 4 pages deep tak search karenge "Mixed for you" ke liye
                var continuation: String? = null
                
                for (i in 0..3) { // Page 0 se 3 tak check karega
                    val homePage = YouTube.home(continuation = continuation).getOrNull()
                    
                    // Aisa section dhoondho jisme "mix" word ho (Jaise: "Mixed for you", "Your mixes")
                    val mixSection = homePage?.sections?.find { section ->
                        section.title.contains("mix", ignoreCase = true)
                    }

                    if (mixSection != null) {
                        // Mixes mil gaye! Inko list me daalo
                        mixSection.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                            if (playlist.id.startsWith("RD") && playlist.id != "LM") {
                                fetchedMixes.add(playlist)
                            }
                        }
                        // Mixes milne ke baad aur pages load karne ki zarurat nahi
                        break 
                    }

                    // Agar nahi mila toh next page ka token lo
                    continuation = homePage?.continuation
                    if (continuation == null) break // Agar aur pages nahi hain toh ruk jao
                }
                
                // State update karo (duplicates hata kar)
                _mixPlaylists.value = fetchedMixes.distinctBy { it.id }
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
