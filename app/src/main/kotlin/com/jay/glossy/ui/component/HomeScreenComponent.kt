package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.jay.glossy.R
import com.jay.glossy.LocalNavController

@Composable
fun GlossyCustomHeader(userName: String) {
    val navController = LocalNavController.current 
    
    val vibeText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Enjoy the Morning Vibes"
            in 12..16 -> "Enjoy the Afternoon Vibes"
            else -> "Enjoy the Evening Vibes"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Left Side: Vibe Text & Greeting ---
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = vibeText,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Hi, ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = userName.ifEmpty { "Guest" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- Right Side: Compact Pill (History, Stat, Listen Together, Profile) ---
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. History
            Icon(
                painter = painterResource(id = R.drawable.history),
                contentDescription = "History",
                modifier = Modifier
                    .size(18.dp)
                    .clickable { navController.navigate("history") },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 2. Stat
            Icon(
                painter = painterResource(id = R.drawable.stats),
                contentDescription = "Stats",
                modifier = Modifier
                    .size(18.dp)
                    .clickable { navController.navigate("stats") },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 3. Listen Together
            Icon(
                painter = painterResource(id = R.drawable.listen_together),
                contentDescription = "Listen Together",
                modifier = Modifier
                    .size(18.dp)
                    .clickable { navController.navigate("listen_together_from_topbar") },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 4. Profile
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFF8D6E63), CircleShape)
                    .clickable { navController.navigate("account") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "G",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
