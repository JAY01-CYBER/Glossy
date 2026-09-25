package com.jay.glossy.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.LocalNavController
import com.jay.glossy.R
import java.util.Calendar

@Composable
fun GreetingSection(userName: String) {
    val navController = LocalNavController.current

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hey night owl"
        }
    }

    val timeBasedPhrases = when (currentHour) {
        in 5..11 -> listOf("Morning vibes are best served loud ☕", "Wake up and smell the music 🌅")
        in 12..16 -> listOf("Midday reset? Press play ☀️", "Keep the energy up ⚡")
        in 17..21 -> listOf("Sunset tunes loaded up 🌇", "Golden hour playlist ready 🌆")
        else -> listOf("Late night, great music 🦉", "Wind down with some good tunes 🌙")
    }

    val customPhrases = listOf(
        "What are we feeling today? 🎧",
        "Let’s give today a soundtrack 💽",
        "Your mood called — it wants music 🎼",
        "Ready to get lost in a song? 👀",
        "Your vibe is here. Let’s press play 💖",
    )

    val subtitlePhrase = remember(currentHour) { (timeBasedPhrases + customPhrases).random() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Small-caps overline, Material 3 style
            Text(
                text = greeting.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Hero name
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitlePhrase,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Quick actions: filled-tonal circular buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GreetingCircleAction(
                icon = R.drawable.download,
                contentDescription = "Downloads",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { navController.navigate("auto_playlist/downloaded") },
            )
            GreetingCircleAction(
                icon = R.drawable.favorite,
                contentDescription = "Liked",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { navController.navigate("auto_playlist/liked") },
            )
        }
    }
}

@Composable
private fun GreetingCircleAction(
    icon: Int,
    contentDescription: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
        modifier = Modifier.size(44.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(0.dp),
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}


