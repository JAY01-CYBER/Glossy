package com.jay.glossy.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jay.glossy.LocalNavController
import com.jay.glossy.R
import com.jay.glossy.ui.theme.GlossyDimens
import com.jay.glossy.ui.theme.GlossyPalette
import java.util.Calendar

/**
 * Home header: a muted "Good morning / afternoon / evening" line, the user's
 * first name in bold under it, then the search action and the avatar.
 */
@Composable
fun GreetingSection(userName: String) {
    val navController = LocalNavController.current

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = stringResource(
        when (currentHour) {
            in 5..11 -> R.string.glossy_good_morning
            in 12..16 -> R.string.glossy_good_afternoon
            else -> R.string.glossy_good_evening
        }
    )
    val firstName = remember(userName) {
        userName.trim().substringBefore(' ').takeIf { it.isNotBlank() } ?: userName
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = GlossyDimens.ScreenPadding,
                end = 12.dp,
                top = 12.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodySmall,
                color = GlossyPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = firstName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = GlossyPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        GlossyIconAction(
            icon = R.drawable.search,
            contentDescription = stringResource(R.string.search),
            tint = GlossyPalette.TextSecondary,
            onClick = { navController.navigate("search_input") },
        )

        Spacer(Modifier.width(6.dp))

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 20.dp),
                    onClick = { navController.navigate("account") },
                ),
        ) {
            GlossyInitialAvatar(
                name = userName,
                size = 34.dp,
            )
        }
    }
}
