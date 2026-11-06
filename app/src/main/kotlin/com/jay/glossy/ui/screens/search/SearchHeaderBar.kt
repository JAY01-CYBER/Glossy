/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The search field, at both of the sizes the search screen uses it.
 */

package com.jay.glossy.ui.screens.search

import com.jay.glossy.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.constants.SearchSource
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette

/**
 * The search field, at both of the sizes this screen uses it.
 *
 * Everything that differs between the discovery header (title, tall pill, mic)
 * and the results toolbar (back arrow, clear, source toggle) is animated with
 * one morph, because the field itself stays the same node across the switch:
 * it keeps focus, so the IME is never asked to close while the listener is
 * still typing. The bar paints the page background on the discovery page and
 * the toolbar surface once there is a query, and the two fade into each other.
 */
@Composable
internal fun SearchHeaderBar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    active: Boolean,
    searchSource: SearchSource,
    pureBlack: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onToggleSource: () -> Unit,
    onVoice: () -> Unit,
    onSearch: (String) -> Unit,
) {
    // Padding, height and colour all ride springs: toggling search and then
    // deleting the character again bends the curve from wherever it currently
    // is instead of restarting a tween, which is what made the bar feel stiff
    // when the state flipped twice in quick succession.
    val morph =
        spring<Dp>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = 0.5.dp,
        )
    val sizeMorph =
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntSize(1, 1),
        )
    val colorMorph =
        spring<Color>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Color(0.01f, 0.01f, 0.01f, 0.01f),
        )
    // Departures clear quickly and arrivals ease in, so the two halves of the
    // cross-fade no longer sit on top of each other in the middle of the morph.
    val fadeOutSpec = tween<Float>(durationMillis = 120, easing = FastOutSlowInEasing)
    val fadeInSpec = tween<Float>(durationMillis = 200, easing = LinearOutSlowInEasing)

    val topPadding by animateDpAsState(if (active) 6.dp else 12.dp, morph, label = "searchHeaderTop")
    val bottomPadding by animateDpAsState(if (active) 8.dp else 18.dp, morph, label = "searchHeaderBottom")
    val sidePadding by animateDpAsState(if (active) 4.dp else GlossyDimens.ScreenPadding, morph, label = "searchHeaderSide")
    val fieldHeight by animateDpAsState(if (active) 48.dp else 56.dp, morph, label = "searchHeaderField")

    val containerColor by animateColorAsState(
        targetValue =
            if (active) {
                if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.background
            },
        animationSpec = colorMorph,
        label = "searchHeaderContainer",
    )
    val fieldColor by animateColorAsState(
        targetValue =
            if (active) {
                if (pureBlack) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                GlossyPalette.Chip
            },
        animationSpec = colorMorph,
        label = "searchHeaderFieldColor",
    )
    // These two used to snap to the new palette on the first frame while the
    // pill behind them was still cross-fading, which read as a flash; now they
    // travel with the background instead of ahead of it.
    val fieldTextColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.onSurface else GlossyPalette.TextPrimary,
        animationSpec = colorMorph,
        label = "searchHeaderTextColor",
    )
    val fieldHintColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.onSurfaceVariant else GlossyPalette.TextSecondary,
        animationSpec = colorMorph,
        label = "searchHeaderHintColor",
    )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(containerColor)
                .statusBarsPadding()
                .padding(top = topPadding, bottom = bottomPadding),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sidePadding)
                    .height(fieldHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = active,
                // Anchored to the leading edge in both directions: the arrow
                // holds still at the left and the space opens to its right.
                enter = fadeIn(fadeInSpec) + expandHorizontally(sizeMorph, expandFrom = Alignment.Start),
                exit = fadeOut(fadeOutSpec) + shrinkHorizontally(sizeMorph, shrinkTowards = Alignment.Start),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.dismiss),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(GlossyDimens.CornerPill))
                        .background(fieldColor)
                        .padding(start = 18.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    tint = fieldHintColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                    textStyle =
                        TextStyle(
                            color = fieldTextColor,
                            fontSize = 16.sp,
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
                    decorationBox = { innerTextField ->
                        if (query.text.isEmpty()) {
                            Text(
                                text =
                                    stringResource(
                                        if (active) {
                                            when (searchSource) {
                                                SearchSource.LOCAL -> R.string.search_library
                                                SearchSource.ONLINE -> R.string.search_yt_music
                                            }
                                        } else {
                                            R.string.search_yt_music
                                        },
                                    ),
                                style =
                                    TextStyle(
                                        color = fieldHintColor,
                                        fontSize = 16.sp,
                                    ),
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    },
                )
                // The floating mic takes over once a search is live, so the
                // pill keeps its own shortcut only on the discovery page.
                AnimatedVisibility(
                    visible = !active,
                    enter = fadeIn(fadeInSpec),
                    exit = fadeOut(fadeOutSpec),
                ) {
                    IconButton(onClick = onVoice, modifier = Modifier.size(44.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.mic),
                            contentDescription = stringResource(R.string.glossy_search_voice),
                            tint = fieldTextColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = active,
                // Anchored to the trailing edge, matching the shrink: the
                // clear button sits on the screen edge and the pill gives up
                // width to its left, instead of the icons sliding across.
                enter = fadeIn(fadeInSpec) + expandHorizontally(sizeMorph, expandFrom = Alignment.End),
                exit = fadeOut(fadeOutSpec) + shrinkHorizontally(sizeMorph, shrinkTowards = Alignment.End),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClear, modifier = Modifier.size(44.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onToggleSource, modifier = Modifier.size(44.dp)) {
                        Icon(
                            painter =
                                painterResource(
                                    when (searchSource) {
                                        SearchSource.LOCAL -> R.drawable.library_music
                                        SearchSource.ONLINE -> R.drawable.language
                                    },
                                ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
