/*
 * Sound FX repository ported from ArchiveTune (2026) © Rukamori — github.com/rukamori (GPL-3.0).
 * Persists the system-equalizer settings and custom profiles in DataStore.
 */

package com.jay.glossy.eq.soundfx

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import com.jay.glossy.constants.SoundFxBandLevelsMbKey
import com.jay.glossy.constants.SoundFxBassBoostEnabledKey
import com.jay.glossy.constants.SoundFxBassBoostStrengthKey
import com.jay.glossy.constants.SoundFxControlModeKey
import com.jay.glossy.constants.SoundFxEnabledKey
import com.jay.glossy.constants.SoundFxOutputGainEnabledKey
import com.jay.glossy.constants.SoundFxOutputGainMbKey
import com.jay.glossy.constants.SoundFxAutoHeadroomKey
import com.jay.glossy.constants.SoundFxProfilesJsonKey
import com.jay.glossy.constants.SoundFxSelectedProfileIdKey
import com.jay.glossy.constants.SoundFxVirtualizerEnabledKey
import com.jay.glossy.constants.SoundFxVirtualizerStrengthKey
import com.jay.glossy.utils.dataStore
import com.jay.glossy.utils.safeDataStoreEdit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundFxRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val playbackController: PlaybackSoundFxController,
    ) {
        fun observe(): Flow<SoundFxConfiguration> =
            combine(
                context.dataStore.data,
                playbackController.capabilities,
            ) { prefs, capabilities ->
                val settings = SoundFxSettings.fromPreferences(prefs)
                val normalizedLevels =
                    resampleLevels(
                        levelsMb = settings.bandLevelsMb,
                        targetCount = capabilities?.bandCount ?: settings.bandLevelsMb.size,
                    )
                SoundFxConfiguration(
                    controlMode = SoundFxControlMode.fromStorage(prefs[SoundFxControlModeKey]),
                    selectedProfileId = prefs[SoundFxSelectedProfileIdKey] ?: FLAT_PROFILE_ID,
                    settings = settings.copy(bandLevelsMb = normalizedLevels),
                    capabilities = capabilities,
                    profiles = decodeProfiles(prefs[SoundFxProfilesJsonKey]),
                )
            }.flowOn(Dispatchers.IO)

        suspend fun setEnabled(enabled: Boolean) = edit { it[SoundFxEnabledKey] = enabled }

        suspend fun setControlMode(mode: SoundFxControlMode) = edit { it[SoundFxControlModeKey] = mode.storageValue }

        suspend fun updateBandLevels(levelsMb: List<Int>) =
            edit {
                it[SoundFxBandLevelsMbKey] = SoundFxJson.encodeToString(levelsMb)
                it[SoundFxSelectedProfileIdKey] = MANUAL_PROFILE_ID
            }

        suspend fun setOutputGainEnabled(enabled: Boolean) = editManual { it[SoundFxOutputGainEnabledKey] = enabled }

        suspend fun setOutputGainMb(gainMb: Int) =
            editManual { it[SoundFxOutputGainMbKey] = gainMb.coerceIn(SoundFxSettings.MIN_OUTPUT_GAIN_MB, SoundFxSettings.MAX_OUTPUT_GAIN_MB) }

        suspend fun setBassBoostEnabled(enabled: Boolean) = editManual { it[SoundFxBassBoostEnabledKey] = enabled }

        suspend fun setBassBoostStrength(strength: Int) =
            editManual { it[SoundFxBassBoostStrengthKey] = strength.coerceIn(0, SoundFxSettings.MAX_EFFECT_STRENGTH) }

        suspend fun setVirtualizerEnabled(enabled: Boolean) = editManual { it[SoundFxVirtualizerEnabledKey] = enabled }

        suspend fun setVirtualizerStrength(strength: Int) =
            editManual { it[SoundFxVirtualizerStrengthKey] = strength.coerceIn(0, SoundFxSettings.MAX_EFFECT_STRENGTH) }

        suspend fun setAutoHeadroomEnabled(enabled: Boolean) = editManual { it[SoundFxAutoHeadroomKey] = enabled }

        suspend fun applyProfile(profile: SoundFxProfile) {
            context.safeDataStoreEdit { prefs ->
                writeProfileSettings(prefs, profile)
                prefs[SoundFxSelectedProfileIdKey] = "$PROFILE_PREFIX${profile.id}"
            }
        }

        suspend fun saveProfile(
            name: String,
            configuration: SoundFxConfiguration,
        ): SoundFxProfile {
            val profile =
                SoundFxProfile(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    bandCenterFreqHz = configuration.capabilities?.bandCenterFreqHz.orEmpty(),
                    bandLevelsMb = configuration.settings.bandLevelsMb,
                    outputGainMb = configuration.settings.outputGainMb,
                    outputGainEnabled = configuration.settings.outputGainEnabled,
                    bassBoostStrength = configuration.settings.bassBoostStrength,
                    bassBoostEnabled = configuration.settings.bassBoostEnabled,
                    virtualizerStrength = configuration.settings.virtualizerStrength,
                    virtualizerEnabled = configuration.settings.virtualizerEnabled,
                    autoHeadroomEnabled = configuration.settings.autoHeadroomEnabled,
                )
            context.dataStore.edit { prefs ->
                val profiles = decodeProfiles(prefs[SoundFxProfilesJsonKey])
                prefs[SoundFxProfilesJsonKey] = encodeProfiles(profiles + profile)
                prefs[SoundFxSelectedProfileIdKey] = "$PROFILE_PREFIX${profile.id}"
            }
            return profile
        }

        suspend fun deleteProfile(profileId: String) {
            context.dataStore.edit { prefs ->
                val profiles = decodeProfiles(prefs[SoundFxProfilesJsonKey])
                prefs[SoundFxProfilesJsonKey] = encodeProfiles(profiles.filterNot { it.id == profileId })
                if (prefs[SoundFxSelectedProfileIdKey] == "$PROFILE_PREFIX$profileId") {
                    prefs[SoundFxSelectedProfileIdKey] = MANUAL_PROFILE_ID
                }
            }
        }

        /** Imports ArchiveTune-format profile JSON. Returns the number of imported profiles. */
        suspend fun importProfiles(uri: Uri): Int =
            withContext(Dispatchers.IO) {
                val raw =
                    context.contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error("Unable to open sound fx profile")
                val imported = decodeImport(raw)
                require(imported.isNotEmpty())
                context.safeDataStoreEdit { prefs ->
                    val existing = decodeProfiles(prefs[SoundFxProfilesJsonKey])
                    val existingIds = existing.mapTo(mutableSetOf()) { it.id }
                    val normalized =
                        imported.map { profile ->
                            val id = profile.id.takeIf { it.isNotBlank() && existingIds.add(it) } ?: uniqueId(existingIds)
                            profile.copy(id = id, name = profile.name.trim())
                        }
                    prefs[SoundFxProfilesJsonKey] = encodeProfiles(existing + normalized)
                    writeProfileSettings(prefs, normalized.first())
                    prefs[SoundFxSelectedProfileIdKey] = "$PROFILE_PREFIX${normalized.first().id}"
                }
                imported.size
            }

        suspend fun exportProfile(
            uri: Uri,
            profile: SoundFxProfile,
        ) = withContext(Dispatchers.IO) {
            val raw = SoundFxJson.encodeToString(SoundFxProfilesPayload(listOf(profile)))
            val output = context.contentResolver.openOutputStream(uri) ?: error("Unable to open sound fx profile")
            output.bufferedWriter().use { it.write(raw) }
        }

        /** Applies the all-flat baseline. */
        suspend fun applyFlat() {
            context.dataStore.edit { prefs ->
                prefs[SoundFxEnabledKey] = true
                prefs[SoundFxBandLevelsMbKey] = SoundFxJson.encodeToString(emptyList<Int>())
                prefs[SoundFxOutputGainMbKey] = 0
                prefs[SoundFxOutputGainEnabledKey] = false
                prefs[SoundFxBassBoostStrengthKey] = 0
                prefs[SoundFxBassBoostEnabledKey] = false
                prefs[SoundFxVirtualizerStrengthKey] = 0
                prefs[SoundFxVirtualizerEnabledKey] = false
                prefs[SoundFxSelectedProfileIdKey] = FLAT_PROFILE_ID
            }
        }

        /** Applies a device system preset by index and stores the resulting band levels. */
        suspend fun applySystemPreset(index: Int): Boolean {
            if (!playbackController.usePreset(index)) return false
            // Read back what the device actually applied so the sliders stay in
            // sync; fails cleanly when the equalizer is not attached yet.
            val levels = playbackController.readCurrentBandLevels() ?: return false
            context.dataStore.edit { prefs ->
                prefs[SoundFxBandLevelsMbKey] = SoundFxJson.encodeToString(levels)
                prefs[SoundFxSelectedProfileIdKey] = "$SYSTEM_PROFILE_PREFIX$index"
            }
            return true
        }

        private suspend fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
            context.safeDataStoreEdit(block)
        }

        /** Edit that marks the current selection as manual (a custom tweak, not a profile). */
        private suspend fun editManual(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
            edit { prefs ->
                block(prefs)
                prefs[SoundFxSelectedProfileIdKey] = MANUAL_PROFILE_ID
            }
        }

        private fun writeProfileSettings(
            prefs: androidx.datastore.preferences.core.MutablePreferences,
            profile: SoundFxProfile,
        ) {
            prefs[SoundFxEnabledKey] = true
            prefs[SoundFxBandLevelsMbKey] = SoundFxJson.encodeToString(profile.bandLevelsMb)
            prefs[SoundFxOutputGainMbKey] = profile.outputGainMb.coerceIn(SoundFxSettings.MIN_OUTPUT_GAIN_MB, SoundFxSettings.MAX_OUTPUT_GAIN_MB)
            prefs[SoundFxOutputGainEnabledKey] = profile.outputGainEnabled ?: (profile.outputGainMb != 0)
            prefs[SoundFxBassBoostStrengthKey] = profile.bassBoostStrength.coerceIn(0, SoundFxSettings.MAX_EFFECT_STRENGTH)
            prefs[SoundFxBassBoostEnabledKey] = profile.bassBoostEnabled ?: (profile.bassBoostStrength != 0)
            prefs[SoundFxVirtualizerStrengthKey] = profile.virtualizerStrength.coerceIn(0, SoundFxSettings.MAX_EFFECT_STRENGTH)
            prefs[SoundFxVirtualizerEnabledKey] = profile.virtualizerEnabled ?: (profile.virtualizerStrength != 0)
            prefs[SoundFxAutoHeadroomKey] = profile.autoHeadroomEnabled
        }

        private fun decodeProfiles(raw: String?): List<SoundFxProfile> =
            raw
                ?.takeIf(String::isNotBlank)
                ?.let { runCatching { SoundFxJson.decodeFromString<SoundFxProfilesPayload>(it).profiles }.getOrNull() }
                .orEmpty()

        /** Accepts both the payload-wrapped and the bare-list JSON formats. */
        private fun decodeImport(raw: String): List<SoundFxProfile> =
            runCatching { SoundFxJson.decodeFromString<SoundFxProfilesPayload>(raw).profiles }.getOrNull()
                ?: runCatching { SoundFxJson.decodeFromString<List<SoundFxProfile>>(raw) }.getOrDefault(emptyList())

        private fun encodeProfiles(profiles: List<SoundFxProfile>): String =
            SoundFxJson.encodeToString(SoundFxProfilesPayload(profiles.distinctBy(SoundFxProfile::id).sortedBy { it.name.lowercase() }))

        private fun uniqueId(existingIds: MutableSet<String>): String =
            generateSequence { UUID.randomUUID().toString() }.first(existingIds::add)

        private companion object {
            const val FLAT_PROFILE_ID = "flat"
            const val MANUAL_PROFILE_ID = "manual"
            const val PROFILE_PREFIX = "profile:"
            const val SYSTEM_PROFILE_PREFIX = "system:"
        }
    }
