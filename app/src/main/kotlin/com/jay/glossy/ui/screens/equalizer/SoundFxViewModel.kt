/*
 * Sound FX ViewModel ported from ArchiveTune (2026) © Rukamori — github.com/rukamori (GPL-3.0).
 * Exposes the system equalizer, output gain, bass boost, virtualizer and
 * profile management to the Sound FX tab.
 */

package com.jay.glossy.ui.screens.equalizer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jay.glossy.constants.AudioNormalizationKey
import com.jay.glossy.eq.soundfx.SoundFxConfiguration
import com.jay.glossy.eq.soundfx.SoundFxControlMode
import com.jay.glossy.eq.soundfx.SoundFxProfile
import com.jay.glossy.eq.soundfx.SoundFxRepository
import com.jay.glossy.eq.soundfx.SoundFxTone
import com.jay.glossy.eq.soundfx.resampleLevels
import com.jay.glossy.eq.soundfx.soundFxToneIndices
import com.jay.glossy.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoundFxViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val repository: SoundFxRepository,
    ) : ViewModel() {
        val configuration: StateFlow<SoundFxConfiguration?> =
            repository
                .observe()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        /** Whether volume normalization is on, so the UI can warn about boost conflicts. */
        val normalizationEnabled: StateFlow<Boolean> =
            context.dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error.asStateFlow()

        fun clearError() = _error.update { null }

        fun setEnabled(enabled: Boolean) = launch { repository.setEnabled(enabled) }

        fun setControlMode(mode: SoundFxControlMode) = launch { repository.setControlMode(mode) }

        fun setBand(
            index: Int,
            levelMb: Int,
        ) {
            val config = configuration.value ?: return
            val capabilities = config.capabilities ?: return
            if (index !in 0 until capabilities.bandCount) return
            val levels =
                resampleLevels(config.settings.bandLevelsMb, capabilities.bandCount)
                    .toMutableList()
                    .also { it[index] = levelMb.coerceIn(capabilities.minBandLevelMb, capabilities.maxBandLevelMb) }
            launch { repository.updateBandLevels(levels) }
        }

        fun setTone(
            tone: SoundFxTone,
            targetLevelMb: Int,
        ) {
            val config = configuration.value ?: return
            launch { repository.updateBandLevels(adjustToneBands(tone, targetLevelMb, config)) }
        }

        fun resetBands() {
            val capabilities = configuration.value?.capabilities ?: return
            launch { repository.updateBandLevels(List(capabilities.bandCount) { 0 }) }
        }

        /** Applies "flat" or a device preset ("system:<index>"). Returns whether it succeeded. */
        fun applyPreset(presetId: String): Boolean =
            when {
                presetId == FLAT_PRESET_ID -> {
                    launch { repository.applyFlat() }
                    true
                }

                presetId.startsWith(SYSTEM_PRESET_PREFIX) -> {
                    val index = presetId.removePrefix(SYSTEM_PRESET_PREFIX).toIntOrNull() ?: return false
                    launch {
                        val ok = repository.applySystemPreset(index)
                        if (!ok) _error.update { it ?: "preset_failed" }
                    }
                    true
                }

                else -> false
            }

        fun setOutputGainEnabled(enabled: Boolean) = launch { repository.setOutputGainEnabled(enabled) }

        fun setOutputGainMb(gainMb: Int) = launch { repository.setOutputGainMb(gainMb) }

        fun setBassBoostEnabled(enabled: Boolean) = launch { repository.setBassBoostEnabled(enabled) }

        fun setBassBoostStrength(strength: Int) = launch { repository.setBassBoostStrength(strength) }

        fun setVirtualizerEnabled(enabled: Boolean) = launch { repository.setVirtualizerEnabled(enabled) }

        fun setVirtualizerStrength(strength: Int) = launch { repository.setVirtualizerStrength(strength) }

        fun setAutoHeadroomEnabled(enabled: Boolean) = launch { repository.setAutoHeadroomEnabled(enabled) }

        /**
         * One-tap maximum sound boost: enables everything and pins output gain,
         * bass boost and virtualizer to their maximum (ArchiveTune-style).
         */
        fun maximizeBoost() {
            launch {
                repository.setEnabled(true)
                repository.setOutputGainEnabled(true)
                repository.setOutputGainMb(com.jay.glossy.eq.soundfx.SoundFxSettings.MAX_OUTPUT_GAIN_MB)
                repository.setBassBoostEnabled(true)
                repository.setBassBoostStrength(com.jay.glossy.eq.soundfx.SoundFxSettings.MAX_EFFECT_STRENGTH)
                repository.setVirtualizerEnabled(true)
                repository.setVirtualizerStrength(com.jay.glossy.eq.soundfx.SoundFxSettings.MAX_EFFECT_STRENGTH)
            }
        }

        fun saveProfile(name: String) {
            val config = configuration.value ?: return
            if (name.isBlank()) return
            launch {
                repository.saveProfile(name, config)
            }
        }

        fun applyProfile(profile: SoundFxProfile) = launch { repository.applyProfile(profile) }

        fun deleteProfile(profileId: String) = launch { repository.deleteProfile(profileId) }

        fun importProfiles(uri: Uri) {
            launch {
                runCatching { repository.importProfiles(uri) }
                    .onFailure { e ->
                        _error.update { e.message ?: "import_failed" }
                    }
            }
        }

        fun exportProfile(
            uri: Uri,
            profile: SoundFxProfile,
        ) {
            launch {
                runCatching { repository.exportProfile(uri, profile) }
                    .onFailure { e ->
                        _error.update { e.message ?: "export_failed" }
                    }
            }
        }

        private fun launch(block: suspend () -> Unit) {
            viewModelScope.launch { runCatching { block() }.onFailure { e -> _error.update { e.message ?: "error" } } }
        }

        /** Ported from ArchiveTune's UpdateEqualizerUseCase.adjustToneBands. */
        private fun adjustToneBands(
            tone: SoundFxTone,
            targetLevelMb: Int,
            configuration: SoundFxConfiguration,
        ): List<Int> {
            val capabilities = configuration.capabilities ?: return configuration.settings.bandLevelsMb
            val current =
                resampleLevels(configuration.settings.bandLevelsMb, capabilities.bandCount).toMutableList()
            val indices = soundFxToneIndices(tone, capabilities.bandCenterFreqHz, capabilities.bandCount)
            if (indices.isEmpty()) return current
            val average = indices.sumOf { current[it] } / indices.size
            val delta = targetLevelMb - average
            indices.forEach { index ->
                current[index] =
                    (current[index] + delta).coerceIn(capabilities.minBandLevelMb, capabilities.maxBandLevelMb)
            }
            return current
        }

        private companion object {
            const val FLAT_PRESET_ID = "flat"
            const val SYSTEM_PRESET_PREFIX = "system:"
        }
    }
