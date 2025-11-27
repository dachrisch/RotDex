package com.rotdex.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotdex.data.models.UserProfile

/**
 * Reusable RotDex logo component for consistent branding
 *
 * Displays either:
 * - User avatar (custom or initials-based) when userProfile is provided
 * - Brain emoji as fallback
 *
 * The avatar is clickable if onAvatarClick is provided.
 */
@Composable
fun RotDexLogo(
    modifier: Modifier = Modifier,
    userProfile: UserProfile? = null,
    onAvatarClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (userProfile != null) {
            AvatarView(
                playerName = userProfile.playerName,
                avatarImagePath = userProfile.avatarImagePath,
                size = 32.dp,
                onClick = onAvatarClick
            )
        } else {
            Text("🧠", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "RotDex",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
