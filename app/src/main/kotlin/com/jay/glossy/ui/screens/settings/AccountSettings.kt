/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.ui.screens.settings

import com.jay.glossy.R

import androidx.compose.foundation.background
<<<<<<< HEAD
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
=======
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
>>>>>>> origin/main
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
<<<<<<< HEAD
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
=======
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
>>>>>>> origin/main
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import timber.log.Timber
import com.jay.glossy.utils.reportException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
<<<<<<< HEAD
import androidx.compose.ui.draw.alpha
=======
>>>>>>> origin/main
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
<<<<<<< HEAD
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
=======
import androidx.compose.ui.unit.dp
>>>>>>> origin/main
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.jay.glossy.BuildConfig
import com.jay.glossy.constants.AccountChannelHandleKey
import com.jay.glossy.constants.AccountEmailKey
import com.jay.glossy.constants.AccountNameKey
import com.jay.glossy.constants.DataSyncIdKey
import com.jay.glossy.constants.InnerTubeCookieKey
import com.jay.glossy.constants.UseLoginForBrowse
import com.jay.glossy.constants.VisitorDataKey
import com.jay.glossy.constants.YtmSyncKey
import com.jay.glossy.ui.component.DefaultDialog
import com.jay.glossy.ui.component.InfoLabel
<<<<<<< HEAD
=======
import com.jay.glossy.ui.component.Material3SettingsGroup
import com.jay.glossy.ui.component.Material3SettingsItem
import com.jay.glossy.ui.component.PreferenceEntry
>>>>>>> origin/main
import com.jay.glossy.ui.component.TextFieldDialog
import com.jay.glossy.utils.Updater
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.viewmodels.AccountSettingsViewModel
import com.jay.glossy.viewmodels.HomeViewModel

@Composable
fun AccountSettings(
    navController: NavController,
    onClose: () -> Unit,
    latestVersionName: String
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
<<<<<<< HEAD
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Grab handle first: the sheet is draggable, and this is the only thing
        // that says so.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp, bottom = 12.dp)
                .size(width = 34.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            // A close button that is visibly a button, not a floating glyph.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
=======
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.close), contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))
>>>>>>> origin/main

        // Logout confirmation dialog
        if (showLogoutDialog) {
            DefaultDialog(
                onDismiss = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.logout_dialog_title)) },
                content = {
                    Text(
                        text = stringResource(R.string.logout_dialog_message),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                },
                buttons = {
                    TextButton(
                        onClick = {
                            Timber.d("[LOGOUT_CLEAR] User chose to clear data")
                            scope.launch {
                                try {
                                    Timber.d("[LOGOUT_CLEAR] Starting clear and logout process")
                                    // Forget account first (stops all sync), then clear data.
                                    // This prevents background syncs from re-adding songs.
                                    accountSettingsViewModel.logoutAndClearLibraryData(context)
                                    Timber.d("[LOGOUT_CLEAR] Library data cleared and account forgotten")
                                } catch (e: Exception) {
                                    Timber.e(e, "[LOGOUT_CLEAR] Error clearing library data, proceeding with logout")
                                    reportException(e)
                                }
                                onInnerTubeCookieChange("")
                                Timber.d("[LOGOUT_CLEAR] Logout complete")
                                showLogoutDialog = false
                                onClose()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.logout_clear))
                    }
                    TextButton(
                        onClick = {
                            Timber.d("[LOGOUT_KEEP] User chose to keep data")
                            scope.launch {
                                Timber.d("[LOGOUT_KEEP] Starting logout process (keeping data)")
                                accountSettingsViewModel.logoutKeepData(context, onInnerTubeCookieChange)
                                Timber.d("[LOGOUT_KEEP] Logout complete")
                                showLogoutDialog = false
                                onClose()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.logout_keep))
                    }
                }
            )
        }

        if (showTokenEditor) {
            val text = """
                ***INNERTUBE COOKIE*** =$innerTubeCookie
                ***VISITOR DATA*** =$visitorData
                ***DATASYNC ID*** =$dataSyncId
                ***ACCOUNT NAME*** =$accountNamePref
                ***ACCOUNT EMAIL*** =$accountEmail
                ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
            """.trimIndent()

            TextFieldDialog(
                initialTextFieldValue = TextFieldValue(text),
                onDone = { data ->
                    var cookie = ""
                    var visitorDataValue = ""
                    var dataSyncIdValue = ""
                    var accountNameValue = ""
                    var accountEmailValue = ""
                    var accountChannelHandleValue = ""

                    data.split("\n").forEach {
                        when {
                            it.startsWith("***INNERTUBE COOKIE*** =") -> cookie = it.substringAfter("=")
                            it.startsWith("***VISITOR DATA*** =") -> visitorDataValue = it.substringAfter("=")
                            it.startsWith("***DATASYNC ID*** =") -> dataSyncIdValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT NAME*** =") -> accountNameValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT EMAIL*** =") -> accountEmailValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> accountChannelHandleValue = it.substringAfter("=")
                        }
                    }
                    // Write all credentials atomically to DataStore and wait for completion
                    // before restarting, preventing the race condition where the process
                    // would be killed before async DataStore coroutines finished writing.
                    accountSettingsViewModel.saveTokenAndRestart(
                        context = context,
                        cookie = cookie,
                        visitorData = visitorDataValue,
                        dataSyncId = dataSyncIdValue,
                        accountName = accountNameValue,
                        accountEmail = accountEmailValue,
                        accountChannelHandle = accountChannelHandleValue,
                    )
                },
                onDismiss = { showTokenEditor = false },
                singleLine = false,
                maxLines = 20,
                isInputValid = { fullText ->
                    // Extract the cookie value from the formatted template line,
                    // then validate it separately — avoids the bug where parseCookieString
                    // received the entire multi-line template and failed to find "SAPISID"
                    // as a key because the "***INNERTUBE COOKIE*** =" prefix shadowed it.
                    val cookieLine = fullText.lines()
                        .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                    val cookieValue = cookieLine?.substringAfter("***INNERTUBE COOKIE*** =")?.trim() ?: ""
                    cookieValue.isNotEmpty() && "SAPISID" in parseCookieString(cookieValue)
                },
                extraContent = {
                    Spacer(Modifier.height(8.dp))
                    InfoLabel(text = stringResource(R.string.token_adv_login_description))
                }
            )
        }

<<<<<<< HEAD
        AccountHeroCard(
            isLoggedIn = isLoggedIn,
            accountName = accountName,
            subtitle = accountEmail.takeIf { it.isNotBlank() }
                ?: accountChannelHandle.takeIf { it.isNotBlank() },
            accountImageUrl = accountImageUrl,
            loginLabel = stringResource(R.string.login),
            onLogout = {
                Timber.d("[LOGOUT] User clicked logout button, showing dialog")
                showLogoutDialog = true
            },
            onClick = {
                onClose()
                if (isLoggedIn) {
                    navController.navigate("account")
                } else {
                    navController.navigate("login")
                }
            },
        )

        Spacer(Modifier.height(18.dp))

        SheetSectionLabel(stringResource(R.string.account))

        val tokenLabel = when {
            !isLoggedIn -> stringResource(R.string.advanced_login)
            showToken -> stringResource(R.string.token_shown)
            else -> stringResource(R.string.token_hidden)
        }

        SheetCard {
            SheetRow(
                title = tokenLabel,
                icon = R.drawable.token,
                showChevron = true,
                onClick = {
                    if (!isLoggedIn) showTokenEditor = true
                    else if (!showToken) showToken = true
                    else showTokenEditor = true
                },
            )

            SheetDivider()

            SheetRow(
                title = stringResource(R.string.more_content),
                icon = R.drawable.explore_outlined,
                enabled = isLoggedIn,
                // The whole row is the target, not just the switch: a 40dp
                // thumb is a poor thing to ask a thumb to hit.
                onClick = {
                    val next = !useLoginForBrowse
                    YouTube.useLoginForBrowse = next
                    onUseLoginForBrowseChange(next)
                },
                trailing = {
                    Switch(
                        enabled = isLoggedIn,
                        checked = useLoginForBrowse,
                        onCheckedChange = {
                            YouTube.useLoginForBrowse = it
                            onUseLoginForBrowseChange(it)
                        },
                        thumbContent = {
                            Icon(
                                painter = painterResource(
                                    id = if (useLoginForBrowse) R.drawable.check else R.drawable.close
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    )
                },
            )

            SheetDivider()

            SheetRow(
                title = stringResource(R.string.yt_sync),
                icon = R.drawable.sync,
                enabled = isLoggedIn,
                onClick = { onYtmSyncChange(!ytmSync) },
                trailing = {
                    Switch(
                        enabled = isLoggedIn,
                        checked = ytmSync,
                        onCheckedChange = onYtmSyncChange,
                        thumbContent = {
                            Icon(
                                painter = painterResource(
                                    id = if (ytmSync) R.drawable.check else R.drawable.close
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    )
                },
            )
        }

        Spacer(Modifier.height(18.dp))

        val updatePending =
            BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.VERSION_NAME
        val downloadUrl =
            if (updatePending) {
                // The update row only exists when there is somewhere to send the
                // user, which is also why it is not a badge on Settings alone.
                Updater.getCachedLatestRelease()?.let { Updater.getDownloadUrlForCurrentVariant(it) }
            } else {
                null
            }

        SheetSectionLabel(stringResource(R.string.glossy_section_app))

        SheetCard {
            SheetRow(
                title = stringResource(R.string.integrations),
                icon = R.drawable.integration,
                showChevron = true,
=======
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLoggedIn && accountImageUrl != null) {
                                AsyncImage(
                                    model = accountImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )

                                Spacer(Modifier.width(12.dp))
                            }

                            Text(
                                text = if (isLoggedIn) accountName else stringResource(R.string.login),
                            )
                        }
                    },
                    icon = if (!isLoggedIn) painterResource(R.drawable.login) else null,
                    trailingContent = {
                        if (isLoggedIn) {
                            OutlinedButton(
                                onClick = {
                                    Timber.d("[LOGOUT] User clicked logout button, showing dialog")
                                    showLogoutDialog = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(stringResource(R.string.action_logout))
                            }
                        }
                    },
                    onClick = {
                        onClose()
                        if (isLoggedIn) {
                            navController.navigate("account")
                        } else {
                            navController.navigate("login")
                        }
                    }
                )
            ),
            useLowContrast = true
        )

        Spacer(Modifier.height(8.dp))

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    title = {
                        Text(
                            when {
                                !isLoggedIn -> stringResource(R.string.advanced_login)
                                showToken -> stringResource(R.string.token_shown)
                                else -> stringResource(R.string.token_hidden)
                            }
                        )
                    },
                    icon = painterResource(R.drawable.token),
                    onClick = {
                        if (!isLoggedIn) showTokenEditor = true
                        else if (!showToken) showToken = true
                        else showTokenEditor = true
                    }
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.more_content)) },
                    icon = painterResource(R.drawable.cached),
                    trailingContent = {
                        Switch(
                            enabled = isLoggedIn,
                            checked = useLoginForBrowse,
                            onCheckedChange = {
                                YouTube.useLoginForBrowse = it
                                onUseLoginForBrowseChange(it)
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (useLoginForBrowse) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    enabled = isLoggedIn
                ),
                Material3SettingsItem(
                    title = { Text(stringResource(R.string.yt_sync)) },
                    icon = painterResource(R.drawable.cached),
                    trailingContent = {
                        Switch(
                            enabled = isLoggedIn,
                            checked = ytmSync,
                            onCheckedChange = onYtmSyncChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (ytmSync) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    enabled = isLoggedIn
                )
            ),
            useLowContrast = true
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.integrations)) },
                icon = { Icon(painterResource(R.drawable.integration), null) },
>>>>>>> origin/main
                onClick = {
                    onClose()
                    navController.navigate("settings/integrations")
                },
<<<<<<< HEAD
            )

            SheetDivider()

            SheetRow(
                title = stringResource(R.string.settings),
                icon = R.drawable.settings,
                badge = updatePending,
                showChevron = true,
=======
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )

            Spacer(Modifier.height(4.dp))

            PreferenceEntry(
                title = { Text(stringResource(R.string.settings)) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.VERSION_NAME) {
                                Badge()
                            }
                        }
                    ) {
                        Icon(painterResource(R.drawable.settings), contentDescription = null)
                    }
                },
>>>>>>> origin/main
                onClick = {
                    onClose()
                    navController.navigate("settings")
                },
<<<<<<< HEAD
            )

            if (downloadUrl != null) {
                SheetDivider()

                SheetRow(
                    title = stringResource(R.string.new_version_available),
                    caption = latestVersionName,
                    icon = R.drawable.update,
                    badge = true,
                    trailingLabel = stringResource(R.string.glossy_update_action),
                    showChevron = true,
                    onClick = { uriHandler.openUri(downloadUrl) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Sheet pieces
// ---------------------------------------------------------------------------

/** Accent eyebrow above a group of rows, so the sheet reads in sections. */
@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
    )
}

/**
 * One grouped settings card: a tonal fill, a hairline rim, and rows divided
 * inside it. The sheet previously stacked one bordered card per row for some
 * entries and a plain list for others, which is what made it look assembled
 * from two different screens.
 */
@Composable
private fun SheetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape),
        content = content,
    )
}

/** Hairline between two rows, inset to the row's text column. */
@Composable
private fun SheetDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

/**
 * One row of a settings card: a tonal icon tile, the title, an optional caption
 * and whatever the row needs on the right (switch, button, chevron, label).
 * 40dp tiles on a 40dp grid keep every group's icons on the same column.
 */
@Composable
private fun SheetRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    caption: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = false,
    trailingLabel: String? = null,
    badge: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = if (enabled) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                if (badge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(scheme.primary),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = scheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        trailing?.invoke()

        if (trailingLabel != null) {
            Text(
                text = trailingLabel,
                color = scheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp),
            )
        }
    }
}

/**
 * The account row, promoted to a header: avatar, name, the address the account
 * is signed in with, and the log-out affordance on the card instead of a button
 * competing with the row's own tap target. Signed out it becomes the sign-in
 * row it always effectively was.
 */
@Composable
private fun AccountHeroCard(
    isLoggedIn: Boolean,
    accountName: String,
    subtitle: String?,
    accountImageUrl: String?,
    loginLabel: String,
    onLogout: () -> Unit,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainerHigh)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoggedIn && accountImageUrl != null) {
                AsyncImage(
                    model = accountImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isLoggedIn) accountName.ifBlank { loginLabel } else loginLabel,
                color = scheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isLoggedIn && subtitle != null) {
                Text(
                    text = subtitle,
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (isLoggedIn) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(scheme.surfaceContainerHighest)
                    .clickable(onClick = onLogout)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.action_logout),
                    color = scheme.onSurface,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = null,
            tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(start = 8.dp)
                .size(18.dp),
        )
    }
}
=======
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )

            Spacer(Modifier.height(4.dp))

            if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.VERSION_NAME) {
                val releaseInfo = Updater.getCachedLatestRelease()
                val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }
                
                if (downloadUrl != null) {
                    PreferenceEntry(
                        title = {
                            Text(text = stringResource(R.string.new_version_available))
                        },
                        description = latestVersionName,
                        icon = {
                            BadgedBox(badge = { Badge() }) {
                                Icon(painterResource(R.drawable.update), null)
                            }
                        },
                        onClick = {
                            uriHandler.openUri(downloadUrl)
                        }
                    )
                }
            }
        }
    }
}
>>>>>>> origin/main
