package com.jay.glossy.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.LocalNavController
import com.jay.glossy.R
import java.util.Calendar

@Composable
fun GreetingSection(userName: String) {
    val navController = LocalNavController.current

    // 1. Dynamic Salutation (Hi, Hello, Hey)
    val salutations = listOf("Hi,", "Hello,", "Hey,")
    val salutation = remember { salutations.random() }

    // 2. Dynamic Greeting Lines
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    
    // Time ke hisaab se lines
    val timeBasedPhrases = when (currentHour) {
        in 5..11 -> listOf("Morning vibes are best served loud. ☕", "Wake up and smell the music. 🌅")
        in 12..16 -> listOf("Midday reset? Press play. ☀️", "Keep the energy up! ⚡")
        in 17..20 -> listOf("Enjoy the evening vibes. 🌆", "Sunset tunes loaded up. 🌇")
        else -> listOf("Late night, great music. 🦉", "Wind down with some good tunes. 🌙")
    }

    // Aapki di hui custom lines
    val customPhrases = listOf(
        "Haalooooo 👋",
        "What are we feeling today? 🎧",
        "Let’s give today a soundtrack. 💽",
        "Your mood called. It wants music. 🎼",
        "Another day, another soundtrack. 💽",
        "Whatever the day brings, bring the music. 🎵",
        "Ready to get lost in a song? 👀",
        "Your vibe is here. Let’s press play. 💖"
    )

    // Dono list ko mix karke koi ek random line uthana
    val subtitlePhrase = remember { (timeBasedPhrases + customPhrases).random() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LEFT SIDE: Texts (Hi, Username, Line) ---
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = salutation,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                // Primary color se Flux Leox jaisa highlight aayega
                color = MaterialTheme.colorScheme.primary, 
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitlePhrase,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- RIGHT SIDE: Buttons (Download & Liked) ---
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.width(IntrinsicSize.Max) // Dono buttons ko same size ka banane ke liye
        ) {
            // Download Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth() // Parent column ki max width lega
                    .clickable {
                        // Seedha downloaded playlist par le jayega
                        navController.navigate("auto_playlist/downloaded")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.download), // Agar icon name alag ho toh change kar lena
                        contentDescription = "Downloads",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Liked Button (Trending ki jagah)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth() // Parent column ki max width lega
                    .clickable {
                        // Seedha Liked songs wali playlist par le jayega
                        navController.navigate("auto_playlist/liked")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.favorite), // Agar icon name alag ho (jaise ic_favorite) toh update kar lena
                        contentDescription = "Liked",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Liked",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
