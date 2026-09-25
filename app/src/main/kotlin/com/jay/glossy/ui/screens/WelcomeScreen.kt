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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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

    Box(
        modifier = Modifier
            .fillMaxSize()
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
        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
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
                    )
                } else {
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
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
                    )
                }
            },
            label = "WelcomeTransition",
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
                }
            }
        }
    }
}

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
        )
    }
}

// --- SCREEN 2: Material 3 Expressive Guest Input Screen ---
@Composable
fun GuestInputSection(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isValid = name.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Navigation Top Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                alpha = if (isDark) 0.7f else 0.9f
            ),
            border = BorderStroke(
                width = if (isValid) 1.8.dp else 1.dp,
                color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                    tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
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
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (name.isEmpty()) {
                            Text(
                                text = "Your Name or Nickname",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        innerTextField()
                    }
                )

                if (name.isNotEmpty()) {
                    IconButton(
                        onClick = { onNameChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

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
        )
    }
}

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

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    )
                }
            }
        }

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
        )
    }
}
