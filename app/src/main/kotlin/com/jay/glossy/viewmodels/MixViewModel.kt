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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI Item class
data class MixUiItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnail: String
)

@HiltViewModel
class MixViewModel @Inject constructor() : ViewModel() {

    private val _mixPlaylists = MutableStateFlow<List<MixUiItem>>(emptyList())
    val mixPlaylists = _mixPlaylists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadMixes() {
        if (_mixPlaylists.value.isNotEmpty() || _isLoading.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val fetchedMixes = mutableListOf<MixUiItem>()

                // 1. Home Page fetch karo 'Mixes' chip ka params nikalne ke liye
                val homePage = YouTube.home().getOrNull()
                val mixChip = homePage?.chips?.find { 
                    val t = it.title.lowercase()
                    t.contains("mix") || t.contains("मिक्स") || t.contains("મિક્સ")
                }
                
                val params = mixChip?.endpoint?.params

                // 2. Agar params mil gaye toh browse() call karo jo GridRenderer ko perfectly parse karta hai
                if (params != null) {
                    val browseResult = YouTube.browse("FEmusic_home", params).getOrNull()
                    
                    browseResult?.items?.forEach { browseGroup ->
                        browseGroup.items.forEach { item ->
                            if (item is PlaylistItem) {
                                fetchedMixes.add(MixUiItem(item.id, item.title, item.author?.name ?: "Auto playlist", item.thumbnail ?: ""))
                            } else if (item is AlbumItem) {
                                val id = item.playlistId ?: item.browseId
                                if (id != null) {
                                    fetchedMixes.add(MixUiItem(id, item.title, item.artists?.joinToString { it.name } ?: "Auto playlist", item.thumbnail))
                                }
                            }
                        }
                    }
                }

                // 3. Fallback: Agar upar grid nahi mila, toh Home page ke general carousels check karo
                if (fetchedMixes.isEmpty()) {
                    homePage?.sections?.forEach { section ->
                        section.items.forEach { item ->
                            var id: String? = null
                            var title = ""
                            var thumb = ""
                            var author = ""
                            
                            if (item is PlaylistItem) {
                                id = item.id; title = item.title; thumb = item.thumbnail ?: ""; author = item.author?.name ?: "Auto playlist"
                            } else if (item is AlbumItem) {
                                id = item.playlistId ?: item.browseId; title = item.title; thumb = item.thumbnail; author = item.artists?.joinToString { it.name } ?: "Auto playlist"
                            }

                            if (id != null) {
                                val isMix = id == "RDMM" || id.startsWith("RDTMAK") || id.startsWith("RDAMPL") || title.contains("mix", true)
                                if (isMix) fetchedMixes.add(MixUiItem(id, title, author, thumb))
                            }
                        }
                    }
                }

                // 4. Default Fallback
                if (fetchedMixes.isEmpty()) {
                    fetchedMixes.add(MixUiItem("RDMM", "My Supermix", "Auto playlist", "https://www.gstatic.com/youtube/media/ytm/images/pbg/supermix-light-v2-active.png"))
                    fetchedMixes.add(MixUiItem("RDAMPLw", "Discover Mix", "Auto playlist", "https://www.gstatic.com/youtube/media/ytm/images/pbg/discover-mix-light-v2-active.png"))
                    fetchedMixes.add(MixUiItem("RDATW", "New Release Mix", "Auto playlist", "https://www.gstatic.com/youtube/media/ytm/images/pbg/new-release-mix-light-v2-active.png"))
                }

                // Sirf pure Mixes ko aage bhejo (Duplicates hata kar)
                _mixPlaylists.value = fetchedMixes.filter { it.id != "LM" }.distinctBy { it.id }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
