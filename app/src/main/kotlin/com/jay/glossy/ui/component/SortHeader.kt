/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.component

import com.jay.glossy.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.constants.PlaylistSongSortType

@Composable
inline fun <reified T : Enum<T>> SortHeader(
    sortType: T,
    sortDescending: Boolean,
    crossinline onSortTypeChange: (T) -> Unit,
    crossinline onSortDescendingChange: (Boolean) -> Unit,
    crossinline sortTypeText: (T) -> Int,
    modifier: Modifier = Modifier,
    showDescending: Boolean? = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Row mein 4.dp ka gap daala hai "split" (alag-alag box) design ke liye
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp), 
        modifier = modifier.padding(vertical = 8.dp),
    ) {
        
        // 1. Pehla Box: Date Added Text ke liye
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    menuExpanded = !menuExpanded
                }
        ) {
            Text(
                text = stringResource(sortTypeText(sortType)),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.widthIn(min = 172.dp),
            ) {
                enumValues<T>().forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(sortTypeText(type)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                painter =
                                painterResource(
                                    if (sortType == type) {
                                        R.drawable.radio_button_checked
                                    } else {
                                        R.drawable.radio_button_unchecked
                                    },
                                ),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            onSortTypeChange(type)
                            menuExpanded = false
                        },
                    )
                }
            }
        }

        // 2. Dusra Box: Arrow Up/Down icon ke liye
        if (sortType != PlaylistSongSortType.CUSTOM && showDescending == true) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { onSortDescendingChange(!sortDescending) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (sortDescending) R.drawable.arrow_downward else R.drawable.arrow_upward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(6.dp)
                )
            }
        }
    }
}
