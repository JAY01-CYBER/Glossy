/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.viewmodels

import com.jay.glossy.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.ChartsPage
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun loadCharts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // The chart request comes back two ways that both look like "no
            // trending": a failure, and a page whose section list parsed to
            // nothing. Neither is worth a dead tab on the first try, so an
            // empty answer is retried once before it is reported. An empty page
            // is deliberately not stored, so reopening the search page tries
            // again instead of pinning the empty result for the session.
            var lastFailure: Throwable? = null
            repeat(2) { attempt ->
                if (attempt > 0) kotlinx.coroutines.delay(400)
                val result = YouTube.getChartsPage()
                val page = result.getOrNull()
                if (page != null && page.sections.isNotEmpty()) {
                    _chartsPage.value = page
                    _isLoading.value = false
                    return@launch
                }
                lastFailure = result.exceptionOrNull()
            }

            _error.value =
                "Failed to load charts: ${lastFailure?.message ?: "the response had no sections"}"
            _isLoading.value = false
        }
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
}
