/*
 * Sound FX tab ported from ArchiveTune (2026) © Rukamori — github.com/rukamori (GPL-3.0).
 * ArchiveTune-style equalizer UI: presets, tone/band sliders, output gain,
 * bass boost, virtualizer, auto headroom and profile management.
 * Restyled with Material You tonal cards and section headers.
 */

package com.jay.glossy.ui.screens.equalizer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jay.glossy.LocalPlayerAwareWindowInsets
import com.jay.glossy.R
import com.jay.glossy.eq.soundfx.SoundFxConfiguration
import com.jay.glossy.eq.soundfx.SoundFxControlMode
import com.jay.glossy.eq.soundfx.SoundFxProfile
import com.jay.glossy.eq.soundfx.SoundFxSettings
import com.jay.glossy.eq.soundfx.SoundFxTone
import com.jay.glossy.eq.soundfx.resampleLevels
import com.jay.glossy.eq.soundfx.soundFxToneIndices
import java.util.Locale

private const val FLAT_PRESET_ID = "flat"
private const val SYSTEM_PRESET_PREFIX = "system:"

/** Card corner radius used across this screen (Material You "large" scale). */
private val SectionCardShape = RoundedCornerShape(24.dp)

@Composable
fun SoundFxTab(
    viewModel: SoundFxViewModel = hiltViewModel(),
) {
    val configuration by viewModel.configuration.collectAsStateWithLifecycle()
    val normalizationEnabled by viewModel.normalizationEnabled.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var profileToExport by remember { mutableStateOf<SoundFxProfile?>(null) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let(viewModel::importProfiles)
        }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val target = profileToExport
            if (uri != null && target != null) viewModel.exportProfile(uri, target)
        }

    when (val config = configuration) {
        null -> {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.sound_fx_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Master enable
                item {
                    SectionCard {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.sound_fx_enable), fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = { Text(stringResource(R.string.sound_fx_enable_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = config.settings.enabled,
                                    onCheckedChange = { viewModel.setEnabled(it) },
                                )
                            },
                        )
                    }
                }

                // Capabilities notice until playback attaches the device equalizer
                if (config.capabilities == null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SectionCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.sound_fx_needs_playback),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                }

                // Basic / Advanced mode selector + presets
                item {
                    SectionCard {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                SoundFxControlMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = config.controlMode == mode,
                                        onClick = { viewModel.setControlMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = SoundFxControlMode.entries.size,
                                        ),
                                        label = {
                                            Text(
                                                when (mode) {
                                                    SoundFxControlMode.BASIC -> stringResource(R.string.sound_fx_mode_basic)
                                                    SoundFxControlMode.ADVANCED -> stringResource(R.string.sound_fx_mode_advanced)
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.sound_fx_presets),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                            ) {
                                item {
                                    PresetChip(
                                        label = stringResource(R.string.preset_flat),
                                        selected = config.selectedProfileId == FLAT_PRESET_ID,
                                        enabled = config.capabilities != null,
                                        onClick = { viewModel.applyPreset(FLAT_PRESET_ID) },
                                    )
                                }
                                items(config.capabilities?.presetNames.orEmpty().size) { index ->
                                    val name = config.capabilities?.presetNames?.getOrNull(index) ?: return@items
                                    PresetChip(
                                        label = name,
                                        selected = config.selectedProfileId == "$SYSTEM_PRESET_PREFIX$index",
                                        enabled = true,
                                        onClick = { viewModel.applyPreset("$SYSTEM_PRESET_PREFIX$index") },
                                    )
                                }
                            }
                        }
                    }
                }

                // Tone sliders (basic mode)
                if (config.controlMode == SoundFxControlMode.BASIC) {
                    item {
                        val capabilities = config.capabilities
                        if (capabilities != null) {
                            SectionCard(title = stringResource(R.string.equalizer)) {
                                SoundFxTone.entries.forEach { tone ->
                                    val current = toneAverageMb(tone, config)
                                    ToneSliderRow(
                                        title = stringResource(tone.labelRes()),
                                        valueMb = current,
                                        minMb = capabilities.minBandLevelMb,
                                        maxMb = capabilities.maxBandLevelMb,
                                        enabled = config.settings.enabled,
                                        onCommit = { viewModel.setTone(tone, it) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Per-band sliders (advanced mode)
                if (config.controlMode == SoundFxControlMode.ADVANCED) {
                    item {
                        val capabilities = config.capabilities
                        if (capabilities != null && capabilities.bandCount > 0) {
                            SectionCard(title = stringResource(R.string.equalizer)) {
                                val levels = resampleLevels(config.settings.bandLevelsMb, capabilities.bandCount)
                                levels.forEachIndexed { index, levelMb ->
                                    BandSliderRow(
                                        frequencyHz = capabilities.bandCenterFreqHz.getOrNull(index) ?: 0,
                                        valueMb = levelMb,
                                        minMb = capabilities.minBandLevelMb,
                                        maxMb = capabilities.maxBandLevelMb,
                                        enabled = config.settings.enabled,
                                        onCommit = { viewModel.setBand(index, it) },
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.resetBands() },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(stringResource(R.string.sound_fx_reset_bands))
                                }
                            }
                        }
                    }
                }

                // Output gain
                item {
                    SectionCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SwitchHeaderRow(
                                title = stringResource(R.string.sound_fx_output_gain),
                                description = stringResource(R.string.sound_fx_output_gain_desc),
                                checked = config.settings.outputGainEnabled,
                                onCheckedChange = { viewModel.setOutputGainEnabled(it) },
                            )
                            GainSliderRow(
                                title = stringResource(
                                    R.string.sound_fx_gain_value,
                                    formatDb(config.settings.outputGainMb),
                                ),
                                valueMb = config.settings.outputGainMb,
                                enabled = config.settings.outputGainEnabled,
                                onCommit = { viewModel.setOutputGainMb(it) },
                            )
                            Button(
                                onClick = { viewModel.maximizeBoost() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.sound_fx_max_boost),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (normalizationEnabled && config.settings.outputGainEnabled && config.settings.outputGainMb > 0) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                    ),
                                ) {
                                    Text(
                                        text = stringResource(R.string.sound_fx_normalization_warning),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Bass boost
                item {
                    SectionCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SwitchHeaderRow(
                                title = stringResource(R.string.sound_fx_bass_boost),
                                checked = config.settings.bassBoostEnabled,
                                onCheckedChange = { viewModel.setBassBoostEnabled(it) },
                            )
                            StrengthSliderRow(
                                value = config.settings.bassBoostStrength,
                                enabled = config.settings.bassBoostEnabled,
                                onCommit = { viewModel.setBassBoostStrength(it) },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }

                // Virtualizer
                item {
                    SectionCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SwitchHeaderRow(
                                title = stringResource(R.string.sound_fx_virtualizer),
                                checked = config.settings.virtualizerEnabled,
                                onCheckedChange = { viewModel.setVirtualizerEnabled(it) },
                            )
                            StrengthSliderRow(
                                value = config.settings.virtualizerStrength,
                                enabled = config.settings.virtualizerEnabled,
                                onCommit = { viewModel.setVirtualizerStrength(it) },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }

                // Auto headroom
                item {
                    SectionCard {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.sound_fx_auto_headroom)) },
                            supportingContent = { Text(stringResource(R.string.sound_fx_auto_headroom_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = config.settings.autoHeadroomEnabled,
                                    onCheckedChange = { viewModel.setAutoHeadroomEnabled(it) },
                                )
                            },
                        )
                    }
                }

                // Profiles
                item {
                    SectionCard(title = stringResource(R.string.sound_fx_profiles)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilledTonalButton(
                                    onClick = { showSaveDialog = true },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.sound_fx_save_profile), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                FilledTonalButton(
                                    onClick = { importLauncher.launch("*/*") },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.sound_fx_import), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (config.profiles.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.sound_fx_no_profiles),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    config.profiles.forEach { profile ->
                                        SoundFxProfileItem(
                                            profile = profile,
                                            isSelected = config.selectedProfileId == "profile:${profile.id}",
                                            onApply = { viewModel.applyProfile(profile) },
                                            onDelete = { viewModel.deleteProfile(profile.id) },
                                            onExport = {
                                                profileToExport = profile
                                                exportLauncher.launch("${profile.name}.json")
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save profile dialog
    if (showSaveDialog) {
        SaveProfileDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                viewModel.saveProfile(name)
                showSaveDialog = false
            },
        )
    }

    // Error dialog
    error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text(stringResource(R.string.error_title)) },
            text = {
                Text(
                    if (errorMessage == "preset_failed") {
                        stringResource(R.string.sound_fx_error_preset)
                    } else {
                        stringResource(R.string.sound_fx_error, errorMessage)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

/**
 * Material You section container: rounded tonal card with an optional
 * Material header (small primary-colored label) above the content.
 */@Composable
private fun SectionCard(
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = SectionCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            content()
        }
    }
}

/** Title (and optional description) with a trailing switch, padded for card use. */
@Composable
private fun SwitchHeaderRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) })
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun ToneSliderRow(
    title: String,
    valueMb: Int,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    onCommit: (Int) -> Unit,
) {
    var dragValue by remember(valueMb) { mutableStateOf<Float?>(null) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatDb(dragValue?.toInt() ?: valueMb),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = dragValue ?: valueMb.toFloat(),
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onCommit(it.toInt()) }
                dragValue = null
            },
            valueRange = minMb.toFloat()..maxMb.toFloat(),
            enabled = enabled,
        )
    }
}

@Composable
private fun BandSliderRow(
    frequencyHz: Int,
    valueMb: Int,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    onCommit: (Int) -> Unit,
) {
    var dragValue by remember(valueMb) { mutableStateOf<Float?>(null) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatFrequency(frequencyHz),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(64.dp),
        )
        Slider(
            value = dragValue ?: valueMb.toFloat(),
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onCommit(it.toInt()) }
                dragValue = null
            },
            valueRange = minMb.toFloat()..maxMb.toFloat(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatDb(dragValue?.toInt() ?: valueMb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun GainSliderRow(
    title: String,
    valueMb: Int,
    enabled: Boolean,
    onCommit: (Int) -> Unit,
) {
    var dragValue by remember(valueMb) { mutableStateOf<Float?>(null) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
        Slider(
            value = dragValue ?: valueMb.toFloat(),
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onCommit(it.toInt()) }
                dragValue = null
            },
            valueRange = SoundFxSettings.MIN_OUTPUT_GAIN_MB.toFloat()..SoundFxSettings.MAX_OUTPUT_GAIN_MB.toFloat(),
            enabled = enabled,
        )
    }
}

@Composable
private fun StrengthSliderRow(
    value: Int,
    enabled: Boolean,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragValue by remember(value) { mutableStateOf<Float?>(null) }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slider(
            value = dragValue ?: value.toFloat(),
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onCommit(it.toInt()) }
                dragValue = null
            },
            valueRange = 0f..SoundFxSettings.MAX_EFFECT_STRENGTH.toFloat(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.sound_fx_strength, dragValue?.toInt() ?: value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SoundFxProfileItem(
    profile: SoundFxProfile,
    isSelected: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onApply),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = profile.name,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            },
            supportingContent = {
                val parts = buildList {
                    if (profile.outputGainMb != 0) add(formatDb(profile.outputGainMb))
                    if (profile.bassBoostStrength > 0) add(stringResource(R.string.sound_fx_bass_boost))
                    if (profile.virtualizerStrength > 0) add(stringResource(R.string.sound_fx_virtualizer))
                }
                Text(parts.joinToString(" · ").ifEmpty { stringResource(R.string.preset_flat) })
            },
            trailingContent = {
                Row {
                    TextButton(onClick = onExport) {
                        Text(stringResource(R.string.sound_fx_export))
                    }
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Text(
                            stringResource(R.string.sound_fx_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.sound_fx_delete)) },
            text = { Text(stringResource(R.string.delete_profile_confirmation, profile.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                ) {
                    Text(stringResource(R.string.sound_fx_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SaveProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sound_fx_save_profile)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.sound_fx_profile_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.sound_fx_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

private fun toneAverageMb(
    tone: SoundFxTone,
    configuration: SoundFxConfiguration,
): Int {
    val capabilities = configuration.capabilities ?: return 0
    val levels = resampleLevels(configuration.settings.bandLevelsMb, capabilities.bandCount)
    val indices = soundFxToneIndices(tone, capabilities.bandCenterFreqHz, capabilities.bandCount)
    if (indices.isEmpty()) return 0
    return indices.sumOf { levels.getOrNull(it) ?: 0 } / indices.size
}

private fun SoundFxTone.labelRes(): Int =
    when (this) {
        SoundFxTone.BASS -> R.string.sound_fx_tone_bass
        SoundFxTone.MIDRANGE -> R.string.sound_fx_tone_midrange
        SoundFxTone.TREBLE -> R.string.sound_fx_tone_treble
    }

private fun formatDb(millibels: Int): String {
    val sign = if (millibels > 0) "+" else ""
    return String.format(Locale.getDefault(), "%s%.1f dB", sign, millibels / 100.0)
}

private fun formatFrequency(hz: Int): String =
    when {
        hz <= 0 -> "—"
        hz >= 1000 -> String.format(Locale.getDefault(), "%.1f kHz", hz / 1000f)
        else -> "$hz Hz"
    }
