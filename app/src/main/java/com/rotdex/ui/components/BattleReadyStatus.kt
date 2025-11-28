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
 * @param localCardSelected Whether the local player has selected a card
 * @param localReady Whether the local player is ready
 * @param opponentReady Whether the opponent is ready
 * @param opponentIsThinking Whether the opponent is currently selecting a card
 * @param waitingForOpponentReady Whether we're waiting for opponent's ready acknowledgment
 * @param localDataComplete Whether local device has received all opponent data
 * @param opponentDataComplete Whether opponent has received all our data
 * @param opponentImageTransferComplete Whether opponent's image transfer is complete
 * @param modifier Modifier for the component
 */
@Composable
fun BattleReadyStatus(
    localCardSelected: Boolean,
    localReady: Boolean,
    opponentReady: Boolean,
    opponentIsThinking: Boolean,
    waitingForOpponentReady: Boolean = false,
    localDataComplete: Boolean = false,
    opponentDataComplete: Boolean = false,
    opponentImageTransferComplete: Boolean = false,
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
            // Show "Moving to arena..." from card selection until ready
            isMovingToArena = localCardSelected && !localReady,
            isTransferring = localReady && !opponentDataComplete,
            isTransferringImage = false,  // Never show image transfer for local player
            showLoadingSpinner = false,  // Never show loading spinner for local player
            isLocalPlayer = true,  // Use green color scheme for local player
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
            isTransferring = opponentReady && !localDataComplete,
            isTransferringImage = !opponentImageTransferComplete && !opponentIsThinking,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual status chip for a player
 *
 * Shows:
 * - Player label (YOU/OPPONENT)
 * - Green check icon when local player is ready
 * - Regular check icon when opponent is ready and data synced
 * - "Moving to arena..." when card is moving to arena (before ready)
 * - Thinking emoji when opponent is selecting
 * - Loading spinner with "Transferring image..." when image transfer in progress
 * - Loading spinner when transferring data
 * - Loading spinner otherwise (if showLoadingSpinner is true)
 *
 * @param label Player label text
 * @param isReady Whether the player is ready
 * @param isMovingToArena Whether the card is moving to arena (selected but not ready)
 * @param isThinking Whether the player is currently thinking/selecting
 * @param isTransferring Whether data is being transferred
 * @param isTransferringImage Whether image is being transferred
 * @param showLoadingSpinner Whether to show loading spinner in default state
 * @param isLocalPlayer Whether this is the local player (affects color scheme)
 * @param modifier Modifier for the chip
 */
@Composable
private fun StatusChip(
    label: String,
    isReady: Boolean,
    isMovingToArena: Boolean = false,
    isThinking: Boolean = false,
    isTransferring: Boolean = false,
    isTransferringImage: Boolean = false,
    showLoadingSpinner: Boolean = true,
    isLocalPlayer: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Color scheme based on player type
    val readyColor = if (isLocalPlayer) Color(0xFF4CAF50) else Color(0xFF2196F3)  // Green for local, Blue for opponent
    val transferColor = Color(0xFFFFA726)  // Orange for transferring/moving

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isReady && !isTransferring -> readyColor.copy(alpha = 0.3f)
                isMovingToArena || isTransferring || isTransferringImage -> transferColor.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
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
                when {
                    // Always show check when ready (green for local, blue for opponent)
                    isReady -> {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Ready",
                            tint = if (isLocalPlayer) Color(0xFF4CAF50) else Color(0xFF2196F3),  // Green for local, Blue for opponent
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Moving to arena (card selected but not ready yet)
                    isMovingToArena -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFA726)
                        )
                    }
                    // Image transferring (not ready yet)
                    isTransferringImage -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFA726)
                        )
                    }
                    // Data transferring (not ready yet)
                    isTransferring -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFA726)
                        )
                    }
                    // Opponent thinking
                    isThinking -> {
                        Text("💭", fontSize = 16.sp)
                    }
                    // Default loading state (only if showLoadingSpinner is true)
                    showLoadingSpinner -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Show "Moving to arena..." text when:
            // 1. Card is selected but not ready yet (isMovingToArena)
            // 2. Data or image is being transferred (isTransferring || isTransferringImage)
            // Extended to show from card selection until battle starts
            if (isMovingToArena || isTransferring || isTransferringImage) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Moving to arena...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFA726),
                    fontSize = 10.sp
                )
            }
        }
    }
}
