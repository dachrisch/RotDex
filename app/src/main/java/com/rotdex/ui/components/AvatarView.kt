package com.rotdex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rotdex.utils.AvatarUtils
import java.io.File

/**
 * Reusable avatar component that displays either:
 * - Custom avatar image from file path
 * - Default initials avatar with hash-based color
 *
 * The avatar is always circular and can optionally be clickable.
 *
 * @param playerName Player name used for generating initials and color
 * @param avatarImagePath Optional file path to custom avatar image
 * @param size Diameter of the circular avatar (default 40.dp)
 * @param onClick Optional click handler
 * @param modifier Additional modifiers to apply
 */
@Composable
fun AvatarView(
    playerName: String,
    avatarImagePath: String?,
    size: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (avatarImagePath != null && File(avatarImagePath).exists()) {
            // Custom avatar
            AsyncImage(
                model = avatarImagePath,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Default initials avatar
            val initials = AvatarUtils.getInitials(playerName)
            val backgroundColor = AvatarUtils.getColorFromName(playerName)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
