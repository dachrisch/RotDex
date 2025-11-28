package com.rotdex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rotdex.utils.AvatarUtils

/**
 * Displays discovered opponents as pulsing bubbles during auto-discovery
 *
 * Format of deviceInfo: "playerName|endpointId"
 *
 * @param discoveredDevices List of discovered devices in "name|id" format
 * @param onDeviceClick Callback when a device bubble is tapped
 * @param modifier Optional modifier
 */
@Composable
fun DiscoveryBubblesSection(
    discoveredDevices: List<String>,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "NEARBY OPPONENTS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (discoveredDevices.isEmpty()) {
            // Scanning animation
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Scanning for opponents...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(discoveredDevices) { deviceInfo ->
                    OpponentBubble(
                        deviceInfo = deviceInfo,
                        onClick = { onDeviceClick(deviceInfo) }
                    )
                }
            }
        }
    }
}

/**
 * Individual opponent bubble with pulsing animation
 *
 * @param deviceInfo Device info in "playerName|endpointId" format
 * @param onClick Callback when bubble is tapped
 */
@Composable
private fun OpponentBubble(
    deviceInfo: String,
    onClick: () -> Unit
) {
    val playerName = deviceInfo.split("|").firstOrNull() ?: "Unknown"
    val displayName = if (playerName.length > 8) {
        "${playerName.take(8)}..."
    } else {
        playerName
    }

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Get avatar color
    val avatarColor = AvatarUtils.getColorFromName(playerName)
    val initials = AvatarUtils.getInitials(playerName)

    Card(
        modifier = Modifier
            .scale(scale)
            .size(100.dp)
            .clickable(onClick = onClick),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Avatar circle with initials
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            Modifier
                                .padding(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Background circle (using Card for elevation)
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
