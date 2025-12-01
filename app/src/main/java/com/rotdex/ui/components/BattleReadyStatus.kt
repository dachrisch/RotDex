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
 * Player status enum representing the current state of a player in battle
 */
enum class PlayerStatus {
    WAITING,           // Connected, no card selected (💭 thinking)
    MOVING_TO_ARENA,   // Card selected, not ready yet (⏳ orange)
    READY,             // Ready, waiting for opponent (✓ checkmark)
    BOTH_READY         // Both players ready (✓ green/blue)
}

/**
 * Battle ready status indicator showing both players' ready states
 *
 * Displays two status chips with simplified state management using PlayerStatus enum
 *
 * @param localCardSelected Whether the local player has selected a card
 * @param localReady Whether the local player is ready
 * @param opponentReady Whether the opponent is ready
 * @param opponentHasSelectedCard Play state: whether opponent has selected a card (separate from technical data status)
 * @param localDataComplete Whether local device has received all opponent data
 * @param opponentDataComplete Whether opponent has received all our data
 * @param modifier Modifier for the component
 */
@Composable
fun BattleReadyStatus(
    localCardSelected: Boolean,
    localReady: Boolean,
    opponentReady: Boolean,
    opponentHasSelectedCard: Boolean = false,
    localDataComplete: Boolean = false,
    opponentDataComplete: Boolean = false,
    opponentName: String = "Opponent",
    modifier: Modifier = Modifier
) {
    // Derive status from minimal state - single source of truth
    val localStatus = when {
        localReady && opponentReady -> PlayerStatus.BOTH_READY
        localReady -> PlayerStatus.READY
        localCardSelected -> PlayerStatus.MOVING_TO_ARENA
        else -> PlayerStatus.WAITING
    }

    val opponentStatus = when {
        opponentReady && localReady -> PlayerStatus.BOTH_READY
        opponentReady -> PlayerStatus.READY
        opponentHasSelectedCard -> PlayerStatus.MOVING_TO_ARENA  // Play state: opponent has selected a card
        else -> PlayerStatus.WAITING
    }

    // Debug logging for state diagnosis
    android.util.Log.d("BattleReadyStatus", """
        🎯 Status calculation:
          Opponent state: opponentReady=$opponentReady, opponentHasSelectedCard=$opponentHasSelectedCard
          Local state: localReady=$localReady, localCardSelected=$localCardSelected
          Calculated opponent status: $opponentStatus
          Calculated local status: $localStatus
    """.trimIndent())

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(
            label = "YOU",
            status = localStatus,
            isLocalPlayer = true,
            showDataTransfer = localReady && !opponentDataComplete,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "⚔️",
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        StatusChip(
            label = opponentName.uppercase(),
            status = opponentStatus,
            isLocalPlayer = false,
            showDataTransfer = opponentReady && !localDataComplete,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual status chip for a player using clean state enum
 *
 * @param label Player label text (YOU/OPPONENT)
 * @param status Current player status
 * @param isLocalPlayer Whether this is the local player (affects color)
 * @param showDataTransfer Whether to show data transfer indicator
 * @param modifier Modifier for the chip
 */
@Composable
private fun StatusChip(
    label: String,
    status: PlayerStatus,
    isLocalPlayer: Boolean,
    showDataTransfer: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Determine visuals based on status
    val (icon, text, backgroundColor) = when (status) {
        PlayerStatus.WAITING -> Triple(
            "💭",
            null,
            MaterialTheme.colorScheme.surfaceVariant
        )
        PlayerStatus.MOVING_TO_ARENA -> Triple(
            "⏳",
            "Moving to arena...",
            Color(0xFFFFA726).copy(alpha = 0.3f)  // Orange
        )
        PlayerStatus.READY -> Triple(
            "✓",
            if (showDataTransfer) "Syncing..." else "Waiting for opponent...",
            Color(0xFF2196F3).copy(alpha = 0.3f)  // Blue
        )
        PlayerStatus.BOTH_READY -> Triple(
            "✓",
            "Battle starting!",
            (if (isLocalPlayer) Color(0xFF4CAF50) else Color(0xFF2196F3)).copy(alpha = 0.3f)
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))

                // Render icon based on status
                when (icon) {
                    "✓" -> {
                        val checkColor = if (isLocalPlayer) Color(0xFF4CAF50) else Color(0xFF2196F3)
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Ready",
                            tint = checkColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    "⏳" -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFA726)
                        )
                    }
                    "💭" -> {
                        Text("💭", fontSize = 16.sp)
                    }
                }
            }

            // Show status text
            text?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status == PlayerStatus.MOVING_TO_ARENA) Color(0xFFFFA726) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp
                )
            }
        }
    }
}
