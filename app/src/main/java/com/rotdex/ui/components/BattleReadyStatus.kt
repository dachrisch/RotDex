package com.rotdex.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Battle ready status indicator showing both players' ready states
 *
 * Displays two status chips:
 * - YOU: Local player's ready state
 * - OPPONENT: Opponent's ready state with thinking indicator
 *
 * Design follows:
 * - Single Responsibility: Displays ready status for both players
 * - Open/Closed: Extensible through parameters without modification
 * - Dependency Inversion: Depends on state primitives
 *
 * @param localReady Whether the local player is ready
 * @param opponentReady Whether the opponent is ready
 * @param opponentIsThinking Whether the opponent is currently selecting a card
 * @param modifier Modifier for the component
 */
@Composable
fun BattleReadyStatus(
    localReady: Boolean,
    opponentReady: Boolean,
    opponentIsThinking: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(
            label = "YOU",
            isReady = localReady,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "⚔️",
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        StatusChip(
            label = "OPPONENT",
            isReady = opponentReady,
            isThinking = opponentIsThinking && !opponentReady,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual status chip for a player
 *
 * Shows:
 * - Player label (YOU/OPPONENT)
 * - Check icon when ready
 * - Thinking emoji when opponent is selecting
 * - Loading spinner otherwise
 *
 * @param label Player label text
 * @param isReady Whether the player is ready
 * @param isThinking Whether the player is currently thinking/selecting
 * @param modifier Modifier for the chip
 */
@Composable
private fun StatusChip(
    label: String,
    isReady: Boolean,
    isThinking: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isReady)
                Color(0xFF4CAF50).copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            if (isReady) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Ready",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            } else if (isThinking) {
                Text("💭", fontSize = 16.sp)
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
