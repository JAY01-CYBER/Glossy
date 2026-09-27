/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import com.jay.glossy.R

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jay.glossy.ui.screens.OptionStats

@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Spacer(Modifier.width(12.dp))

        chips.forEach { (value, label) ->
            val isSelected = currentValue == value
            val emoji = getEmojiForLabel(label)
            val displayText = if (emoji.isNotEmpty()) "$emoji $label" else label

            // Bouncy Spring animation for Corner Radius
            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 20.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "corner_radius"
            )

            // FIX: Explicitly typed to let compiler know it's a Composable Lambda
            val leadingIconComposable: @Composable (() -> Unit)? = if (isSelected) {
                {
                    Icon(
                        painter = painterResource(R.drawable.check), 
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null

            FilterChip(
                label = { Text(displayText) },
                selected = isSelected,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                onClick = { onValueUpdate(value) },
                leadingIcon = leadingIconComposable,
                shape = RoundedCornerShape(cornerRadius),
                border = null,
                modifier = Modifier
                    .height(35.dp)
                    .animateContentSize( // Smooth bouncy text shift
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            )

            Spacer(Modifier.width(8.dp))
        }
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun <Int> ChoiceChipsRow(
    chips: List<Pair<Int, String>>,
    options: List<Pair<OptionStats, String>>,
    selectedOption: OptionStats,
    onSelectionChange: (OptionStats) -> Unit,
    currentValue: Int,
    onValueUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    var expandIconDegree by remember { mutableFloatStateOf(0f) }
    val rotationAnimation by animateFloatAsState(
        targetValue = expandIconDegree,
        animationSpec = tween(durationMillis = 400),
        label = "",
    )

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column {
            AssistChip(
                onClick = {
                    expanded = !expanded
                    expandIconDegree -= 180
                },
                label = {
                    Text(
                        text =
                        when (selectedOption) {
                            OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                            OptionStats.MONTHS -> stringResource(id = R.string.months)
                            OptionStats.YEARS -> stringResource(id = R.string.years)
                            OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                        },
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                    )
                },
                shape = RoundedCornerShape(16.dp),
                border = null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = containerColor,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandIn() + fadeIn(),
                exit = shrinkOut() + fadeOut(),
            ) {
                DropdownMenu(
                    modifier = Modifier.padding(start = 12.dp),
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                        expandIconDegree -= 180
                    },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.second) },
                            onClick = {
                                onSelectionChange(option.first)
                                expandIconDegree -= 180
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedOption,
            transitionSpec = { slideInHorizontally() + fadeIn() togetherWith slideOutHorizontally() + fadeOut() },
            label = "",
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
            ) {
                chips.forEach { (value, label) ->
                    Spacer(Modifier.width(8.dp))
                    
                    val isSelected = currentValue == value
                    val emoji = getEmojiForLabel(label)
                    val displayText = if (emoji.isNotEmpty()) "$emoji $label" else label

                    val cornerRadius by animateDpAsState(
                        targetValue = if (isSelected) 20.dp else 8.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "corner_radius"
                    )

                    // FIX: Explicitly typed to let compiler know it's a Composable Lambda
                    val leadingIconComposable: @Composable (() -> Unit)? = if (isSelected) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = "Selected",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null

                    FilterChip(
                        label = { Text(displayText) },
                        selected = isSelected,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = containerColor,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        onClick = { onValueUpdate(value) },
                        leadingIcon = leadingIconComposable,
                        shape = RoundedCornerShape(cornerRadius),
                        border = null,
                        modifier = Modifier
                            .height(35.dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                    )
                }
            }
        }
    }
}

// Emoji Helper function
private fun getEmojiForLabel(label: String): String {
    val lower = label.lowercase()
    return when {
        lower.contains("workout") || lower.contains("cardio") -> "🏋️"
        lower.contains("feel good") || lower.contains("feel-good") || lower.contains("happy") -> "☀️"
        lower.contains("energize") || lower.contains("energise") || lower.contains("power") || lower.contains("boost") -> "⚡"
        lower.contains("podcast") -> "🎙️"
        lower.contains("relax") || lower.contains("calm") || lower.contains("unwind") -> "🧘"
        lower.contains("sleep") || lower.contains("night") || lower.contains("lullaby") -> "🌙"
        lower.contains("commute") || lower.contains("drive") || lower.contains("trip") -> "🚗"
        lower.contains("focus") || lower.contains("study") || lower.contains("work") -> "🧠"
        lower.contains("party") || lower.contains("dance") || lower.contains("club") -> "🎉"
        lower.contains("romance") || lower.contains("love") -> "❤️"
        lower.contains("sad") || lower.contains("heartbreak") || lower.contains("cry") -> "🌧️"
        lower.contains("chill") || lower.contains("acoustic") -> "☕"
        lower.contains("gaming") || lower.contains("game") -> "🎮"
        lower.contains("throwback") || lower.contains("retro") || lower.contains("classic") || lower.contains("90s") || lower.contains("80s") -> "📻"
        lower.contains("bollywood") || lower.contains("desi") -> "💃"
        lower.contains("trending") || lower.contains("top") || lower.contains("hits") -> "🔥"
        lower.contains("new") || lower.contains("latest") || lower.contains("release") -> "🆕"
        else -> "" 
    }
}
