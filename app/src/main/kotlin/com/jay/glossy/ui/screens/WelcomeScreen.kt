<<<<<<< HEAD
/**
 * Glossy Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Onboarding. Three steps on one violet backdrop:
 *
 *  1. the sign-in screen — [GlossyLoginScreen]: the full-bleed violet design
 *     with the holographic key art and Google / guest / cookie-token entries,
 *  2. the guest nickname step,
 *  3. the community page — Instagram, Discord and Telegram — which ends with
 *     "Next" and hands the user over to Home.
 *
 * Signed-in users never see step 1: the sign-in flow restarts the app, so the
 * relaunched process lands straight on step 3 (see [PendingCommunityIntroKey]).
 */

package com.jay.glossy.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
=======
package com.jay.glossy.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
>>>>>>> origin/main
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
<<<<<<< HEAD
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
=======
import androidx.compose.foundation.isSystemInDarkTheme
>>>>>>> origin/main
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
<<<<<<< HEAD
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
=======
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
>>>>>>> origin/main
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
<<<<<<< HEAD
import androidx.compose.runtime.rememberCoroutineScope
=======
>>>>>>> origin/main
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
<<<<<<< HEAD
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
=======
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
>>>>>>> origin/main
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
<<<<<<< HEAD
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
=======
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
>>>>>>> origin/main
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
<<<<<<< HEAD
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.R
import com.jay.glossy.constants.CommunityChannel
import com.jay.glossy.constants.GuestNameKey
import com.jay.glossy.constants.InnerTubeCookieKey
import com.jay.glossy.constants.PendingCommunityIntroKey
import com.jay.glossy.ui.component.GlossyDotsIndicator
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyOnboardingPalette
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.safeDataStoreEdit
import kotlinx.coroutines.launch

enum class WelcomeState {
    INTRO,
    GUEST_INPUT,
    COMMUNITY,
}

/** Drawable the project ships its key art as (optional — see [OnboardingBackdrop]). */
private const val KEY_ART_RES_NAME = "login_bg"

@Composable
fun GlossyWelcomeScreen(
    onSetupComplete: (String) -> Unit,
    onGoogleLoginClick: () -> Unit,
    onTokenClick: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val (cookie) = rememberPreference(InnerTubeCookieKey, defaultValue = "")
    val (pendingCommunityIntro, setPendingCommunityIntro) =
        rememberPreference(PendingCommunityIntroKey, defaultValue = false)

    var currentState by rememberSaveable { mutableStateOf(WelcomeState.INTRO) }
    var guestName by rememberSaveable { mutableStateOf("") }
    // The flow can be observed from two places (a session that already exists and
    // the user pressing Next), so make sure Home is entered exactly once.
    var setupCompleted by rememberSaveable { mutableStateOf(false) }
    val completeOnce: (String) -> Unit = { name ->
        if (!setupCompleted) {
            setupCompleted = true
            onSetupComplete(name)
        }
    }

    // A session restored from a backup (or an install that already has a cookie)
    // has nothing to sign in; go to Home. Right after an interactive sign-in the
    // restarted process sets [PendingCommunityIntroKey] and the community page
    // is shown instead, so the flow still ends with an explicit hand-off.
    LaunchedEffect(cookie, pendingCommunityIntro) {
        if (cookie.isBlank()) return@LaunchedEffect
        if (pendingCommunityIntro) {
            if (currentState == WelcomeState.INTRO) currentState = WelcomeState.COMMUNITY
        } else {
            completeOnce("Google User")
        }
    }

    val finishOnboarding: () -> Unit = {
        coroutineScope.launch {
            if (pendingCommunityIntro) setPendingCommunityIntro(false)
            completeOnce(guestName.trim().ifBlank { "Google User" })
        }
    }
=======
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jay.glossy.R
import com.jay.glossy.constants.InnerTubeCookieKey
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.safeDataStoreEdit
import kotlinx.coroutines.delay

enum class WelcomeState {
    INTRO, GUEST_INPUT, LOADING
}

@Composable
fun GlossyWelcomeScreen(
    onSetupComplete: (String) -> Unit,
    onGoogleLoginClick: () -> Unit
) {
    var currentState by rememberSaveable { mutableStateOf(WelcomeState.INTRO) }
    var guestName by rememberSaveable { mutableStateOf("") }

    val isDark = isSystemInDarkTheme()
    val bgImage = if (isDark) R.drawable.welcome_bg_dark else R.drawable.welcome_bg_light

    val (cookie) = rememberPreference(InnerTubeCookieKey, defaultValue = "")

    LaunchedEffect(cookie) {
        if (cookie.isNotBlank()) {
            onSetupComplete("Google User")
        }
    }

    // Android 12+ blur support
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
>>>>>>> origin/main

    Box(
        modifier = Modifier
            .fillMaxSize()
<<<<<<< HEAD
            .background(GlossyOnboardingPalette.BackdropBottom),
    ) {
        OnboardingBackdrop(
            dim = if (currentState == WelcomeState.INTRO) 0f else 0.45f,
        )

=======
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background wallpaper
        Image(
            painter = painterResource(id = bgImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (currentState != WelcomeState.INTRO && supportsBlur) {
                        Modifier.blur(20.dp)
                    } else {
                        Modifier
                    }
                )
        )

        // Material 3 Dynamic Scrim & Ambient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = if (isDark) 0.45f else 0.25f
                            ),
                            MaterialTheme.colorScheme.background.copy(
                                alpha = if (currentState == WelcomeState.INTRO) {
                                    if (isDark) 0.72f else 0.55f
                                } else {
                                    if (isDark) 0.88f else 0.78f
                                }
                            ),
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                alpha = if (isDark) 0.94f else 0.85f
                            )
                        )
                    )
                )
        )

        // Fluid Material 3 Animated Transition between Landing Steps
>>>>>>> origin/main
        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
<<<<<<< HEAD
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) { it / 3 } + fadeIn(tween(350, easing = FastOutSlowInEasing)) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(350),
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutLinearInEasing),
                        ) { -it / 3 } + fadeOut(tween(240)) + scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(280),
                        ),
=======
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { it / 3 } + fadeIn(tween(350, easing = FastOutSlowInEasing)) + scaleIn(
                        initialScale = 0.93f,
                        animationSpec = tween(350)
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutLinearInEasing)
                        ) { -it / 3 } + fadeOut(tween(240)) + scaleOut(
                            targetScale = 0.93f,
                            animationSpec = tween(280)
                        )
>>>>>>> origin/main
                    )
                } else {
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
<<<<<<< HEAD
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) { -it / 3 } + fadeIn(tween(350, easing = FastOutSlowInEasing)) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = tween(350),
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutLinearInEasing),
                        ) { it / 3 } + fadeOut(tween(240)) + scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(280),
                        ),
=======
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { -it / 3 } + fadeIn(tween(350, easing = FastOutSlowInEasing)) + scaleIn(
                        initialScale = 0.93f,
                        animationSpec = tween(350)
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutLinearInEasing)
                        ) { it / 3 } + fadeOut(tween(240)) + scaleOut(
                            targetScale = 0.93f,
                            animationSpec = tween(280)
                        )
>>>>>>> origin/main
                    )
                }
            },
            label = "WelcomeTransition",
<<<<<<< HEAD
            modifier = Modifier.fillMaxSize(),
        ) { state ->
            when (state) {
                WelcomeState.INTRO -> {
                    GlossyLoginScreen(
                        onGoogleClick = onGoogleLoginClick,
                        onGuestClick = { currentState = WelcomeState.GUEST_INPUT },
                        onTokenClick = onTokenClick,
                    )
                }

                WelcomeState.GUEST_INPUT -> {
                    GuestInputSection(
                        name = guestName,
                        activeIndex = 1,
                        onNameChange = { guestName = it },
                        onBack = { currentState = WelcomeState.INTRO },
                        onContinue = {
                            val name = guestName.trim()
                            if (name.isNotBlank()) {
                                coroutineScope.launch {
                                    context.safeDataStoreEdit { prefs ->
                                        prefs[GuestNameKey] = name
                                    }
                                    currentState = WelcomeState.COMMUNITY
                                }
                            }
                        },
                    )
                }

                WelcomeState.COMMUNITY -> {
                    CommunitySection(
                        activeIndex = 2,
                        onNext = finishOnboarding,
                    )
=======
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) { state ->
            when (state) {
                WelcomeState.INTRO -> {
                    IntroSection(
                        isDark = isDark,
                        onGuestClick = { currentState = WelcomeState.GUEST_INPUT },
                        onGoogleClick = onGoogleLoginClick
                    )
                }
                WelcomeState.GUEST_INPUT -> {
                    GuestInputSection(
                        name = guestName,
                        onNameChange = { guestName = it },
                        onBack = { currentState = WelcomeState.INTRO },
                        onContinue = {
                            if (guestName.isNotBlank()) {
                                currentState = WelcomeState.LOADING
                            }
                        }
                    )
                }
                WelcomeState.LOADING -> {
                    LoadingSection(name = guestName)

                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        delay(1400L)

                        context.safeDataStoreEdit { prefs ->
                            prefs[stringPreferencesKey("guest_name")] = guestName
                        }

                        onSetupComplete(guestName)
                    }
>>>>>>> origin/main
                }
            }
        }
    }
}

<<<<<<< HEAD
// ============================================================================
// Backdrop
// ============================================================================

/**
 * The onboarding backdrop: the project's key art when it is present in the
 * drawable resources, over a violet gradient with a soft glow. The art is
 * looked up by name so the flow still renders correctly (and the build still
 * passes) before the asset is added.
 */
@Composable
private fun OnboardingBackdrop(
    modifier: Modifier = Modifier,
    dim: Float = 0f,
) {
    val context = LocalContext.current
    val artworkId =
        remember(context) {
            context.resources.getIdentifier(KEY_ART_RES_NAME, "drawable", context.packageName)
        }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                GlossyOnboardingPalette.BackdropTop,
                                GlossyOnboardingPalette.BackdropMid,
                                GlossyOnboardingPalette.BackdropBottom,
                            ),
                        ),
                    )
                },
        )

        if (artworkId != 0) {
            Image(
                painter = painterResource(artworkId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Ambient glow behind the brand mark, plus the brighter violet corner
        // and the deep shadow at the opposite end that give the backdrop its
        // lit-from-above feel.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val glowCenter = Offset(size.width / 2f, size.height * 0.32f)
                    val glowRadius = size.width * 1.05f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GlossyOnboardingPalette.Glow.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                            center = glowCenter,
                            radius = glowRadius,
                        ),
                        radius = glowRadius,
                        center = glowCenter,
                    )

                    val cornerCenter = Offset(size.width * 1.02f, -size.height * 0.04f)
                    val cornerRadius = size.width * 0.95f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GlossyOnboardingPalette.Glow.copy(alpha = 0.65f),
                                Color.Transparent,
                            ),
                            center = cornerCenter,
                            radius = cornerRadius,
                        ),
                        radius = cornerRadius,
                        center = cornerCenter,
                    )

                    val shadowCenter = Offset(-size.width * 0.1f, size.height)
                    val shadowRadius = size.width * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GlossyOnboardingPalette.BackdropBottom.copy(alpha = 0.75f),
                                Color.Transparent,
                            ),
                            center = shadowCenter,
                            radius = shadowRadius,
                        ),
                        radius = shadowRadius,
                        center = shadowCenter,
                    )
                },
        )

        // Legibility wash. Darkens as the user walks deeper into the flow, so
        // the artwork never competes with the form.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                GlossyOnboardingPalette.BackdropTop.copy(alpha = 0.28f + 0.3f * dim),
                                GlossyOnboardingPalette.BackdropMid.copy(alpha = 0.14f + 0.36f * dim),
                                GlossyOnboardingPalette.BackdropBottom.copy(alpha = 0.72f + 0.2f * dim),
                            ),
                        ),
                    )
                },
        )
    }
}

// ============================================================================
// Shared pieces
// ============================================================================

/** The app mark, white, with its own glow. */
@Composable
private fun GlossyMark(markSize: Dp = 88.dp) {
    Box(
        modifier = Modifier.size(markSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val glowCenter = Offset(this.size.width / 2f, this.size.height / 2f)
                    val glowRadius = this.size.minDimension * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GlossyOnboardingPalette.Glow.copy(alpha = 0.85f),
                                Color.Transparent,
                            ),
                            center = glowCenter,
                            radius = glowRadius,
                        ),
                        radius = glowRadius,
                        center = glowCenter,
                    )
                },
        )
        Icon(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(markSize * 0.74f),
        )
    }
}

/** Filled violet action, used for Primary/Next. */
@Composable
private fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(GlossyDimens.CornerPill))
            .background(
                if (enabled) GlossyOnboardingPalette.Primary
                else GlossyOnboardingPalette.Primary.copy(alpha = 0.35f),
            )
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = GlossyOnboardingPalette.OnPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Small round glassy icon action, used to step back. */
@Composable
private fun OnboardingBackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(GlossyOnboardingPalette.ChipFill),
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_back),
            contentDescription = stringResource(R.string.back),
            tint = GlossyOnboardingPalette.OnBackdrop,
            modifier = Modifier.size(20.dp),
=======
// --- SCREEN 1: Material 3 Expressive Intro Screen ---
@Composable
fun IntroSection(
    isDark: Boolean,
    onGuestClick: () -> Unit,
    onGoogleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1.2f))

        // Expressive App Emblem with subtle luminous glow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(116.dp)
        ) {
            Surface(
                modifier = Modifier.size(108.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.5f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
            ) {}

            Icon(
                painter = painterResource(R.drawable.small_icon),
                contentDescription = "Glossy Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(68.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Headline & Pill Badge
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = "Pure Music • Zero Ads • Free",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Text(
            text = "Welcome to Glossy",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "A beautifully crafted music player tailored for the way you listen. Smooth, ad-free, and open source.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1.0f))

        // Material 3 Expressive Action Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(
                alpha = if (isDark) 0.65f else 0.85f
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            ),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Action: Google Login
                Button(
                    onClick = onGoogleClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.login),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Action: Guest Access
                OutlinedButton(
                    onClick = onGuestClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    border = BorderStroke(
                        1.2.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue as a guest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Crafted with ❤️ by Jay",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
>>>>>>> origin/main
        )
    }
}

<<<<<<< HEAD

// ============================================================================
// Step 2 — guest nickname
// ============================================================================

@Composable
private fun GuestInputSection(
    name: String,
    activeIndex: Int,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
=======
// --- SCREEN 2: Material 3 Expressive Guest Input Screen ---
@Composable
fun GuestInputSection(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
>>>>>>> origin/main
    val isValid = name.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
<<<<<<< HEAD
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = GlossyDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
=======
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Navigation Top Row
>>>>>>> origin/main
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
<<<<<<< HEAD
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OnboardingBackButton(onClick = onBack)
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.weight(0.8f))

        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(GlossyOnboardingPalette.ChipFill)
                .border(1.dp, GlossyOnboardingPalette.ChipBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.person),
                contentDescription = null,
                tint = GlossyOnboardingPalette.OnBackdrop,
                modifier = Modifier.size(44.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "What should we call you?",
            color = GlossyOnboardingPalette.OnBackdrop,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Pick a nickname for your Glossy profile. You can change it later in Settings.",
            color = GlossyOnboardingPalette.OnBackdropMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

=======
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                            alpha = if (isDark) 0.6f else 0.8f
                        ),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(0.8f))

        // Expressive Avatar Badge
        Surface(
            modifier = Modifier.size(92.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
            tonalElevation = 4.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = "Guest",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What should we call you?",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your nickname to personalize your music journey",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Material 3 Expressive Input Container
>>>>>>> origin/main
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
<<<<<<< HEAD
            shape = RoundedCornerShape(20.dp),
            color = GlossyOnboardingPalette.ChipFill,
            border = BorderStroke(
                width = if (isValid) 1.6.dp else 1.dp,
                color = if (isValid) GlossyOnboardingPalette.Primary
                else GlossyOnboardingPalette.ChipBorder,
            ),
=======
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                alpha = if (isDark) 0.7f else 0.9f
            ),
            border = BorderStroke(
                width = if (isValid) 1.8.dp else 1.dp,
                color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            tonalElevation = 1.dp
>>>>>>> origin/main
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
<<<<<<< HEAD
                verticalAlignment = Alignment.CenterVertically,
=======
                verticalAlignment = Alignment.CenterVertically
>>>>>>> origin/main
            ) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
<<<<<<< HEAD
                    tint = if (isValid) GlossyOnboardingPalette.Primary
                    else GlossyOnboardingPalette.OnBackdropMuted,
                    modifier = Modifier.size(20.dp),
                )

                Spacer(Modifier.width(14.dp))
=======
                    tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))
>>>>>>> origin/main

                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
<<<<<<< HEAD
                        color = GlossyOnboardingPalette.OnBackdrop,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                    ),
                    cursorBrush = SolidColor(GlossyOnboardingPalette.Primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (isValid) onContinue() },
=======
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid) onContinue()
                        }
>>>>>>> origin/main
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (name.isEmpty()) {
                            Text(
<<<<<<< HEAD
                                text = "Your name or nickname",
                                style = MaterialTheme.typography.bodyLarge,
                                color = GlossyOnboardingPalette.OnBackdropMuted.copy(alpha = 0.7f),
                            )
                        }
                        innerTextField()
                    },
=======
                                text = "Your Name or Nickname",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        innerTextField()
                    }
>>>>>>> origin/main
                )

                if (name.isNotEmpty()) {
                    IconButton(
                        onClick = { onNameChange("") },
<<<<<<< HEAD
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            tint = GlossyOnboardingPalette.OnBackdropMuted,
                            modifier = Modifier.size(16.dp),
=======
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
>>>>>>> origin/main
                        )
                    }
                }
            }
        }

<<<<<<< HEAD
        Spacer(Modifier.height(20.dp))

        OnboardingPrimaryButton(
            text = "Start listening",
            onClick = onContinue,
            enabled = isValid,
        )

        Spacer(Modifier.weight(1f))

        GlossyDotsIndicator(count = 3, activeIndex = activeIndex)

        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.glossy_credits),
            color = GlossyOnboardingPalette.OnBackdropMuted.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp),
=======
        Spacer(modifier = Modifier.height(20.dp))

        // Continue Button with state elevation and responsive icon
        Button(
            onClick = onContinue,
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            ),
            shape = CircleShape,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isValid) 3.dp else 0.dp,
                pressedElevation = 6.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Start Listening",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1.2f))

        Text(
            text = "Crafted with ❤️ by Jay",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
>>>>>>> origin/main
        )
    }
}

<<<<<<< HEAD
// ============================================================================
// Step 3 — community
// ============================================================================

@Composable
private fun CommunitySection(
    activeIndex: Int,
    onNext: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
=======
// --- SCREEN 3: Material 3 Expressive Loading Screen ---
@Composable
fun LoadingSection(name: String) {
    val isDark = isSystemInDarkTheme()

    // Smooth pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "LoadingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraAlpha"
    )
>>>>>>> origin/main

    Column(
        modifier = Modifier
            .fillMaxSize()
<<<<<<< HEAD
            .systemBarsPadding()
            .padding(horizontal = GlossyDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        GlossyMark(markSize = 72.dp)

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.glossy_community_title),
            color = GlossyOnboardingPalette.OnBackdrop,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.glossy_community_subtitle),
            color = GlossyOnboardingPalette.OnBackdropMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.height(28.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = GlossyOnboardingPalette.Card,
            border = BorderStroke(1.dp, GlossyOnboardingPalette.CardBorder),
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                CommunityChannel.entries.forEachIndexed { index, channel ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 66.dp, end = 16.dp)
                                .height(1.dp)
                                .background(GlossyOnboardingPalette.CardBorder),
                        )
                    }
                    ChannelRow(
                        channel = channel,
                        onClick = {
                            runCatching { uriHandler.openUri(channel.url) }
                                .onFailure { openWithPackageManager(context, channel.url) }
                        },
=======
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1.5f))

        // Pulsing App Emblem with Radiant Glow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(114.dp)
                    .scale(pulseScale)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = auraAlpha * 0.35f),
                        shape = CircleShape
                    )
            )

            Surface(
                modifier = Modifier.size(86.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                tonalElevation = 4.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.small_icon),
                        contentDescription = "Glossy Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
>>>>>>> origin/main
                    )
                }
            }
        }

<<<<<<< HEAD
        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.glossy_community_handle),
            color = GlossyOnboardingPalette.OnBackdropMuted,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 0.6.sp,
        )

        Spacer(Modifier.weight(1f))

        GlossyDotsIndicator(count = 3, activeIndex = activeIndex)

        Spacer(Modifier.height(16.dp))

        OnboardingPrimaryButton(
            text = stringResource(R.string.next),
            onClick = onNext,
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ChannelRow(
    channel: CommunityChannel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(channel.brandColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(channel.iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.label,
                color = GlossyOnboardingPalette.OnCard,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(channel.descriptionRes),
                color = GlossyOnboardingPalette.OnCardMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = null,
            tint = GlossyOnboardingPalette.OnCardMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Last resort for a [CommunityChannel] whose URL no app or browser claims:
 * hand the link to the system so the user at least gets the chooser.
 */
private fun openWithPackageManager(
    context: Context,
    url: String,
) {
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url),
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
=======
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Welcome aboard,",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (name.isNotBlank()) name else "Music Lover",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(44.dp))

        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 3.5.dp,
            modifier = Modifier.size(38.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(
                alpha = if (isDark) 0.6f else 0.8f
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "\"Life buffering ho sakti hai, music nahi.\"",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1.0f))

        Text(
            text = "Jay & M4TRX",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
>>>>>>> origin/main
        )
    }
}
