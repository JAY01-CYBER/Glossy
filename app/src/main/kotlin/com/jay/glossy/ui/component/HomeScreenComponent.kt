package com.jay.glossy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import java.util.Calendar
import com.jay.glossy.BuildConfig
import com.jay.glossy.R
import com.jay.glossy.LocalNavController
import com.jay.glossy.viewmodels.HomeViewModel

@Composable
fun GlossyCustomHeader(userName: String) {
    val navController = LocalNavController.current 
    
    val viewModel: HomeViewModel = hiltViewModel()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    var showAccountDialog by remember { mutableStateOf(false) }

    if (showAccountDialog) {
        AccountSettingsDialog(
            onDismiss = {
                showAccountDialog = false
                viewModel.refresh()
            },
            latestVersionName = BuildConfig.VERSION_NAME
        )
    }
    
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
            Icon(
                painter = painterResource(id = R.drawable.history),
                contentDescription = "History",
                modifier = Modifier
                    .size(20.dp)
                    .clickable { navController.navigate("history") },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Icon(
                painter = painterResource(id = R.drawable.stats),
                contentDescription = "Stats",
                modifier = Modifier
                    .size(20.dp)
                    .clickable { navController.navigate("stats") },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Icon(
                painter = painterResource(id = R.drawable.group_outlined),
                contentDescription = "Listen Together",
                modifier = Modifier
                    .size(20.dp)
                    .clickable { navController.navigate("listen_together_from_topbar") },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { showAccountDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (accountImageUrl != null) {
                    AsyncImage(
                        model = accountImageUrl,
                        contentDescription = "Account",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFF8D6E63), CircleShape),
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
    }
}
