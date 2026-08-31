/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.PlaylistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                var foundMixes = false

                // 1. Liked Music hamesha top par
                accountPlaylists?.find {
                    it.id == "LM" || it.title.equals("Liked Music", ignoreCase = true)
                }?.let { fetchedMixes.add(it) }

                // Helper Function: Safely extract mixes from both PlaylistItem & AlbumItem formats
                fun extractMixes(items: List<Any>?) {
                    items?.forEach { item ->
                        var id: String? = null
                        var title = ""
                        var thumbnail = ""
                        var author = ""

                        if (item is PlaylistItem) {
                            id = item.id
                            title = item.title
                            thumbnail = item.thumbnail ?: ""
                            author = item.author?.name ?: "Auto playlist"
                        } else if (item is AlbumItem) {
                            id = item.playlistId ?: item.browseId
                            title = item.title
                            thumbnail = item.thumbnail
                            author = item.artists?.joinToString { it.name } ?: "Auto playlist"
                        }

                        if (id != null && id != "LM") {
                            // Check keywords in multiple languages and Universal Mix IDs
                            val isMix = id == "RDMM" || id.startsWith("RDTMAK") || id.startsWith("RDAMPL") ||
                                        title.contains("mix", true) || title.contains("मिक्स", true) || title.contains("મિક્સ", true)
                            if (isMix) {
                                fetchedMixes.add(
                                    PlaylistItem(
                                        id = id,
                                        title = title,
                                        author = Artist(name = author, id = null),
                                        songCountText = null,
                                        thumbnail = thumbnail,
                                        playEndpoint = null,
                                        shuffleEndpoint = null,
                                        radioEndpoint = null
                                    )
                                )
                                foundMixes = true
                            }
                        }
                    }
                }

                // 2. Scan Home Page directly
                val homePage = YouTube.home().getOrNull()
                homePage?.sections?.forEach { extractMixes(it.items) }

                // 3. Scan 'Mixes' Chip using Local Variables (Bypasses Smart Cast Error!)
                if (!foundMixes) {
                    val mixChip = homePage?.chips?.find {
                        val t = it.title.lowercase()
                        t.contains("mix") || t.contains("मिक्स") || t.contains("મિક્સ")
                    }
                    
                    // SMART CAST FIX: Assigned to local val first
                    val targetEndpoint = mixChip?.endpoint
                    val targetParams = targetEndpoint?.params
                    
                    if (targetParams != null) {
                        val chipPage = YouTube.home(params = targetParams).getOrNull()
                        chipPage?.sections?.forEach { extractMixes(it.items) }
                    }
                }

                // 4. Deep Scan Pagination (Up to 3 pages)
                if (!foundMixes) {
                    var continuation = homePage?.continuation
                    for (i in 1..3) {
                        if (continuation == null) break
                        val nextPage = YouTube.home(continuation = continuation).getOrNull()
                        nextPage?.sections?.forEach { extractMixes(it.items) }
                        if (foundMixes) break
                        continuation = nextPage?.continuation
                    }
                }

                // 5. Ultimate Fallback (Never show empty screen)
                if (!foundMixes) {
                    fetchedMixes.add(PlaylistItem("RDMM", "My Supermix", Artist(name = "Auto playlist", id = null), null, "https://www.gstatic.com/youtube/media/ytm/images/pbg/supermix-light-v2-active.png", null, null, null))
                    fetchedMixes.add(PlaylistItem("RDAMPLw", "Discover Mix", Artist(name = "Auto playlist", id = null), null, "https://www.gstatic.com/youtube/media/ytm/images/pbg/discover-mix-light-v2-active.png", null, null, null))
                    fetchedMixes.add(PlaylistItem("RDATW", "New Release Mix", Artist(name = "Auto playlist", id = null), null, "https://www.gstatic.com/youtube/media/ytm/images/pbg/new-release-mix-light-v2-active.png", null, null, null))
                }

                // Push clean data to UI
                _mixPlaylists.value = fetchedMixes.distinctBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
