/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jay.glossy.R
import com.jay.glossy.constants.AppFont
import com.jay.glossy.constants.CustomFontNameKey
import com.jay.glossy.constants.CustomFontPathKey
import com.jay.glossy.constants.SelectedFontKey
import com.jay.glossy.ui.component.IconButton
import com.jay.glossy.ui.theme.appFontFamily
import com.jay.glossy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Where an imported font is kept inside the app's own storage.
 *
 * One slot, not a library: a font choice is a single answer, so a second import
 * replaces the first rather than leaving files nobody can reach.
 */
private const val ImportedFontBaseName = "imported_font"

/** The extensions the text stack can load; anything else is refused before it is copied. */
private val ImportedFontExtensions = setOf("ttf", "otf", "ttc")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFontSettingsScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (selectedFontValue, onSelectedFontChange) = rememberPreference(SelectedFontKey, AppFont.SYSTEM.value)
    val (customFontPath, onCustomFontPathChange) = rememberPreference(CustomFontPathKey, "")
    val (customFontName, onCustomFontNameChange) = rememberPreference(CustomFontNameKey, "")
    val currentFont = AppFont.fromValue(selectedFontValue)

    // Importing copies the file instead of keeping the picker's URI: that grant
    // lasts only as long as the process does, and a font has to survive the next
    // launch. The copy is the app's own file from then on.
    val importFont =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val originalName = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
            val extension = originalName.substringAfterLast('.', "").lowercase()
            if (extension !in ImportedFontExtensions) {
                Toast.makeText(context, R.string.font_import_unsupported, Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                val imported =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val target = File(context.filesDir, "$ImportedFontBaseName.$extension")
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                target.outputStream().use { output -> input.copyTo(output) }
                            } ?: error("The picker handed back a font that cannot be opened")
                            target
                        }.getOrNull()
                    }
                if (imported == null) {
                    Toast.makeText(context, R.string.font_import_failed, Toast.LENGTH_LONG).show()
                } else {
                    onCustomFontPathChange(imported.absolutePath)
                    onCustomFontNameChange(originalName.ifBlank { imported.name })
                    // Importing is the whole intent of the tap, so the font it
                    // brought becomes the chosen one rather than waiting for a
                    // second tap on its own row.
                    onSelectedFontChange(AppFont.CUSTOM.value)
                }
            }
        }

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
                    // Drawn with the imported font too: a preview that fell back
                    // to the system typeface would be showing something other
                    // than what the app is about to draw with.
                    Text(
                        text = "\"Where words fail, music speaks.\"",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp, lineHeight = 36.sp),
                        fontFamily = appFontFamily(currentFont, customFontPath),
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

            // The import sits above the list rather than at the end of it: it adds
            // an option to the list, so it reads as the way in, not as one more
            // typeface to scroll past.
            Surface(
                onClick = {
                    // The picker is asked for anything at all: plenty of devices
                    // label a .ttf as application/octet-stream and would grey it
                    // out under a font-only filter, so the extension check above
                    // is what actually decides.
                    importFont.launch("*/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.upload),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.font_import_action),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.font_import_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            )

            AppFont.entries.forEach { fontOption ->
                val isImported = fontOption == AppFont.CUSTOM
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
                                // The imported row names the file it came from,
                                // which is the only thing that tells two imported
                                // fonts apart; with nothing imported it says so
                                // instead of offering a choice that cannot be made.
                                text = when {
                                    !isImported -> fontOption.description
                                    customFontName.isNotBlank() -> customFontName
                                    else -> stringResource(R.string.font_import_none)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = appFontFamily(fontOption, customFontPath)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
