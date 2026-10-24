/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.ChartsPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel @Inject constructor() : ViewModel() {
    private val _chartsPage = MutableStateFlow<ChartsPage?>(null)
    val chartsPage = _chartsPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /**
     * Songs that stand in for the chart when the chart request does not answer
     * with any. The charts endpoint is the one part of the app that is
     * country-gated and renderer-sensitive, and when it fails the Suggestions
     * tab has nothing else to list — a filtered song search answers the same
     * question ("what is popular right now") through an endpoint the rest of
     * the app already relies on.
     */
    private val _suggestedSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val suggestedSongs = _suggestedSongs.asStateFlow()

    fun loadCharts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // The chart request comes back two ways that both look like "no
            // trending": a failure, and a page whose section list parsed to
            // nothing. Neither is worth a dead tab on the first try, so an
            // empty answer is retried once before the fallback is used. An
            // empty page is deliberately not stored, so reopening the search
            // page tries again instead of pinning the empty result for the
            // session.
            var lastFailure: Throwable? = null
            repeat(2) { attempt ->
                if (attempt > 0) delay(400)
                val result = YouTube.getChartsPage()
                val page = result.getOrNull()
                if (page != null && page.sections.isNotEmpty()) {
                    _chartsPage.value = page
                    // The page can answer without a single song — its other
                    // sections are albums and playlists — and the ranked list
                    // only knows how to draw songs, so fill it from search
                    // instead of leaving the tab on an empty notice.
                    _suggestedSongs.value =
                        if (page.sections.any { section -> section.items.any { it is SongItem } }) {
                            emptyList()
                        } else {
                            loadTrendingSuggestions()
                        }
                    _isLoading.value = false
                    return@launch
                }
                lastFailure = result.exceptionOrNull()
            }

            val suggestions = loadTrendingSuggestions()
            _suggestedSongs.value = suggestions

            _error.value =
                if (suggestions.isNotEmpty()) {
                    // Something to show is not an error, even when the chart
                    // itself never arrived.
                    null
                } else {
                    "Failed to load charts: ${lastFailure?.message ?: "the response had no sections"}"
                }
            _isLoading.value = false
        }
    }

    /**
     * Runs the stand-in search, newest query first, and stops at the first one
     * that returns songs. Empty when every query answers with nothing, which is
     * the only case that still ends on the page's error notice.
     */
    private suspend fun loadTrendingSuggestions(): List<SongItem> {
        for (query in TrendingQueries) {
            val songs =
                YouTube
                    .search(query, YouTube.SearchFilter.FILTER_SONG)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<SongItem>()
                    ?.distinctBy { it.id }
                    ?.take(SuggestionCount)
                    .orEmpty()
            if (songs.isNotEmpty()) return songs
        }
        return emptyList()
    }

    fun loadMore() {
        viewModelScope.launch {
            _chartsPage.value?.continuation?.let { continuation ->
                _isLoading.value = true
                YouTube.getChartsPage(continuation)
                    .onSuccess { newPage ->
                        _chartsPage.value = _chartsPage.value?.copy(
                            sections = _chartsPage.value?.sections.orEmpty() + newPage.sections,
                            continuation = newPage.continuation
                        )
                    }
                    .onFailure { e ->
                        _error.value = "Failed to load more: ${e.message}"
                    }
                _isLoading.value = false
            }
        }
    }

    private companion object {
        /** How many rows the Suggestions tab lists. */
        const val SuggestionCount = 5

        /**
         * What to search when the charts have nothing. Deliberately generic: the
         * search is region-aware on its own, so the popular songs it returns are
         * the local ones.
         */
        val TrendingQueries = listOf("trending songs", "top hits")
    }
}
