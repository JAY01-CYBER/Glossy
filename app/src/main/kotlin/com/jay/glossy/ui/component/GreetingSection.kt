package com.jay.glossy.ui.component

import java.util.Calendar
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun getDynamicGreeting(name: String): Pair<String, String> {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..4 -> "Up late, $name?" to "🦉"
        in 5..7 -> "Up early, $name?" to "🌅"
        in 8..11 -> "Good morning, $name." to "☀️"
        in 12..16 -> "Good afternoon, $name." to "🌤️"
        in 17..20 -> "Good evening, $name." to "🌆"
        else -> "Good night, $name." to "🌙"
    }
}

@Composable
fun GreetingSection(userName: String) {
    val (greetingText, greetingEmoji) = getDynamicGreeting(userName)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = greetingText,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = greetingEmoji,
            fontSize = 38.sp
        )
    }
}
