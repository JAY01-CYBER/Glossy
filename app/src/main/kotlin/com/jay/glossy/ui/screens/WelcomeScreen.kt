package com.jay.glossy.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    onGoogleLoginClick: () -> Unit // GOOGLE LOGIN KA PARAMETER
) {
    var currentState by remember { mutableStateOf(WelcomeState.INTRO) }
    var guestName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentState != WelcomeState.GUEST_INPUT) {
            AuraBackground()
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
                        onGuestClick = { currentState = WelcomeState.GUEST_INPUT },
                        onGoogleClick = onGoogleLoginClick // YAHAN PARAMETER PASS KIYA
                    )
                }
                WelcomeState.GUEST_INPUT -> {
                    GuestInputSection(
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
                    LoadingSection(name = guestName)
                    
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

@Composable
fun AuraBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2962FF).copy(alpha = 0.4f), 
                    Color(0xFFD7CCC8).copy(alpha = 0.2f), 
                    Color.Transparent
                ),
                center = Offset(size.width / 2, size.height * 0.35f),
                radius = size.width * 0.8f
            ),
            topLeft = Offset(-size.width * 0.2f, size.height * 0.1f),
            size = androidx.compose.ui.geometry.Size(size.width * 1.4f, size.height * 0.5f)
        )
    }
}

// --- SCREEN 1: Intro Screen ---
@Composable
fun IntroSection(onGuestClick: () -> Unit, onGoogleClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = "Glossy Logo",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Glossy",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "A Clean and Minimal Music Streaming app which\nis totally Free Without any subscription or\nAdvertisement",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGoogleClick, // YAHAN GOOGLE BUTTON KA CLICK FIX KIYA
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = CircleShape
        ) {
            Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Continue as a guest",
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable { onGuestClick() }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.weight(0.5f))

        Text(
            text = "By tapping Get Started, I agree with the Terms of\nService and Privacy Policy.",
            color = Color.DarkGray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

// --- SCREEN 2: Guest Input Screen ---
@Composable
fun GuestInputSection(name: String, onNameChange: (String) -> Unit, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = "Glossy Logo",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sign in as a Guest",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Color.White),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF1E1E1E), CircleShape),
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
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = CircleShape
        ) {
            Text("Continue as a Guest", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "By tapping Get Started, I agree with the Terms of\nService and Privacy Policy.",
            color = Color.DarkGray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

// --- SCREEN 3: Loading Screen ---
@Composable
fun LoadingSection(name: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = "Glossy Logo",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Glossy",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Welcome Back,\n$name",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))

        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "\"Life buffering ho sakti hai, music nahi.\"",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "FLUXXLEUX & M4TRX",
            color = Color.DarkGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
