package com.jay.glossy.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.delay
import com.jay.glossy.utils.safeDataStoreEdit
import com.jay.glossy.R 

enum class WelcomeState {
    INTRO, GUEST_INPUT, LOADING
}

@Composable
fun GlossyWelcomeScreen(
    onSetupComplete: (String) -> Unit,
    onGoogleLoginClick: () -> Unit
) {
    var currentState by remember { mutableStateOf(WelcomeState.INTRO) }
    var guestName by remember { mutableStateOf("") }
    
    // Theme check karna taaki UI uske hisaab se change ho
    val isDark = isSystemInDarkTheme()
    
    // Theme ke hisaab se background image select karna
    val bgImage = if (isDark) R.drawable.welcome_bg_dark else R.drawable.welcome_bg_light

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // NAYA WALLPAPER LOGIC
        Image(
            painter = painterResource(id = bgImage),
            contentDescription = "Welcome Background",
            contentScale = ContentScale.Crop, // Screen par poora fit aayega
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent overlay agar guest input ya loading screen ho
        if (currentState != WelcomeState.INTRO) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f))
            )
        }

        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
            },
            label = "WelcomeTransition"
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
                        isDark = isDark,
                        name = guestName,
                        onNameChange = { guestName = it },
                        onContinue = {
                            if (guestName.isNotBlank()) {
                                currentState = WelcomeState.LOADING
                            }
                        }
                    )
                }
                WelcomeState.LOADING -> {
                    LoadingSection(isDark = isDark, name = guestName)
                    
                    val context = LocalContext.current
                    
                    LaunchedEffect(Unit) {
                        delay(3000)
                        
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

// --- SCREEN 1: Intro Screen ---
@Composable
fun IntroSection(isDark: Boolean, onGuestClick: () -> Unit, onGoogleClick: () -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color.LightGray else Color.DarkGray
    val btnBgColor = if (isDark) Color.White else Color.Black
    val btnTextColor = if (isDark) Color.Black else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // LOGO SIZE BADI KI HAI (84.dp)
        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = "Glossy Logo",
            tint = textColor,
            modifier = Modifier.size(84.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Glossy",
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "A Clean and Minimal Music Streaming app which\nis totally Free Without any subscription or\nAdvertisement",
            color = subTextColor,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGoogleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = btnBgColor,
                contentColor = btnTextColor
            ),
            shape = CircleShape
        ) {
            Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Continue as a guest",
            color = subTextColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { onGuestClick() }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.weight(0.5f))

        Text(
            text = "By tapping Get Started, I agree with the Terms of\nService and Privacy Policy.",
            color = if (isDark) Color.DarkGray else Color.Gray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

// --- SCREEN 2: Guest Input Screen ---
@Composable
fun GuestInputSection(isDark: Boolean, name: String, onNameChange: (String) -> Unit, onContinue: () -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    val inputBgColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.8f) else Color(0xFFE0E0E0).copy(alpha = 0.8f)
    val btnBgColor = if (isDark) Color.White else Color.Black
    val btnTextColor = if (isDark) Color.Black else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        // LOGO SIZE BADI KI HAI (84.dp)
        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = "Glossy Logo",
            tint = textColor,
            modifier = Modifier.size(84.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sign in as a Guest",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            textStyle = TextStyle(color = textColor, fontSize = 16.sp, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(textColor),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(inputBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (name.isEmpty()) {
                        Text("Please Enter Your Name", color = Color.Gray, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = btnBgColor,
                contentColor = btnTextColor
            ),
            shape = CircleShape
        ) {
            Text("Continue as a Guest", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "By tapping Get Started, I agree with the Terms of\nService and Privacy Policy.",
            color = if (isDark) Color.DarkGray else Color.Gray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

// --- SCREEN 3: Loading Screen ---
@Composable
fun LoadingSection(isDark: Boolean, name: String) {
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color.LightGray else Color.DarkGray

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // LOGO SIZE BADI KI HAI (84.dp)
        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = "Glossy Logo",
            tint = textColor,
            modifier = Modifier.size(84.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Glossy",
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Welcome Back,\n$name",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))

        CircularProgressIndicator(
            color = textColor,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "\"Life buffering ho sakti hai, music nahi.\"",
            color = subTextColor,
            fontSize = 12.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "FLUXXLEUX & M4TRX",
            color = if (isDark) Color.DarkGray else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
