/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import com.jay.glossy.R

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jay.glossy.constants.ExperimentalLyricsKey
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.viewmodels.LyricsViewModel

@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
    onUserInteract: () -> Unit = {} 
) {
    val (experimentalLyrics, _) = rememberPreference(key = ExperimentalLyricsKey, defaultValue = true)

    val interactiveModifier = modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                if (downEvent.changes.any { it.pressed }) {
                    var isDrag = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        // FIX: Changed to direct position subtraction to avoid missing extension imports
                        if (event.changes.any { (it.position - it.previousPosition).getDistance() > 3f }) {
                            isDrag = true
                            onUserInteract()
                        }
                        if (event.changes.all { !it.pressed }) {
                            break
                        }
                    }
                    if (!isDrag) {
                        onUserInteract()
                    }
                }
            }
        }
    }

    if (experimentalLyrics) {
        ExperimentalLyrics(
            sliderPositionProvider = sliderPositionProvider,
            modifier = interactiveModifier,
            showLyrics = showLyrics,
            lyricsViewModel = lyricsViewModel
        )
    } else {
        OriginalLyrics(
            sliderPositionProvider = sliderPositionProvider,
            modifier = interactiveModifier,
            showLyrics = showLyrics
        )
    }
}
