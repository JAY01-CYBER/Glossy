/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jay.glossy.R
import com.jay.glossy.constants.AppFont
import com.jay.glossy.constants.SelectedFontKey
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.utils.rememberPreference

// FONT IMPORTS
import com.jay.glossy.ui.theme.GoogleSansFontFamily
import com.jay.glossy.ui.theme.GoogleSansFlexFontFamily
import com.jay.glossy.ui.theme.InterFontFamily
import com.jay.glossy.ui.theme.ManropeFontFamily
import com.jay.glossy.ui.theme.OutfitFontFamily
import com.jay.glossy.ui.theme.PlusJakartaSansFontFamily
import com.jay.glossy.ui.theme.PoppinsFontFamily
import com.jay.glossy.ui.theme.RoundexFontFamily

// HELPER FUNCTION: Maps Enum to Compose FontFamily
fun getComposeFontFamily(appFont: AppFont): FontFamily {
    return when (appFont) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.GOOGLE_SANS -> GoogleSansFontFamily
        AppFont.GOOGLE_SANS_FLEX -> GoogleSansFlexFontFamily
        AppFont.INTER -> InterFontFamily
        AppFont.MANROPE -> ManropeFontFamily
        AppFont.OUTFIT -> OutfitFontFamily
        AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
        AppFont.POPPINS -> PoppinsFontFamily
        AppFont.ROUNDEX -> RoundexFontFamily
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFontSettingsScreen(
    navController: NavController,
) {
    val (selectedFontValue, onSelectedFontChange) = rememberPreference(SelectedFontKey, AppFont.SYSTEM.value)
    val currentFont = AppFont.fromValue(selectedFontValue)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Font") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "TYPOGRAPHY PREVIEW",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "\"Where words fail, music speaks.\"",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp, lineHeight = 36.sp),
                        fontFamily = getComposeFontFamily(currentFont), // ERROR FIXED HERE
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Expressive typeface is applied to display, headlines, and titles. Body copy and labels remain in the system font for maximum readability.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Default, 
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "Font Selection",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            AppFont.entries.forEach { fontOption ->
                Surface(
                    onClick = { onSelectedFontChange(fontOption.value) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFont == fontOption,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fontOption.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = fontOption.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = getComposeFontFamily(fontOption) // ERROR FIXED HERE
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
