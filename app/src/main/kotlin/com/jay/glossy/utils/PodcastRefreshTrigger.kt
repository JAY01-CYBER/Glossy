package com.jay.glossy.utils
import com.jay.glossy.R

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Simple singleton to trigger podcast library refresh from anywhere.
 * Used when subscribing/unsubscribing from channels.
 */
object PodcastRefreshTrigger {
    private val _refreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshFlow = _refreshFlow.asSharedFlow()

    fun triggerRefresh() {
        _refreshFlow.tryEmit(Unit)
    }
}
