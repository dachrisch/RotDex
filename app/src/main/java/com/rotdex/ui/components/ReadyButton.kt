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
import com.rotdex.data.models.BattleCard

/**
 * Enum representing the different states of the Ready button
 *
 * Following Single Responsibility Principle - encapsulates button state logic
 */
enum class ReadyButtonState {
    /** Card not selected yet - button is disabled */
    DISABLED,
    /** Card selected and ready to click */
    ENABLED,
    /** Waiting for opponent after clicking ready */
    WAITING,
    /** Both players are ready, battle starting */
    BOTH_READY
}

/**
 * Ready button component for Battle Arena card selection phase
 *
 * Displays different states:
 * - DISABLED: No card selected yet
 * - ENABLED: Card selected, ready to confirm
 * - WAITING: Local player ready, waiting for opponent
 * - BOTH_READY: Both players ready, battle starting
 *
 * Design follows:
 * - Single Responsibility: Focuses solely on ready button presentation
 * - Open/Closed: Extensible through parameters without modification
 * - Dependency Inversion: Depends on abstractions (callbacks, data)
 *
 * @param localCard The currently selected card (null if none selected)
 * @param localReady Whether the local player has clicked ready
 * @param opponentReady Whether the opponent has clicked ready
 * @param canClick Whether the button can be clicked (disabled after first click)
 * @param onReady Callback when ready button is clicked
 * @param modifier Modifier for the button
 */
@Composable
fun ReadyButton(
    localCard: BattleCard?,
    localReady: Boolean,
    opponentReady: Boolean,
    canClick: Boolean,
    onReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine button state based on inputs
    val buttonState = when {
        localCard == null -> ReadyButtonState.DISABLED
        !canClick && !localReady -> ReadyButtonState.DISABLED
        localReady && !opponentReady -> ReadyButtonState.WAITING
        localReady && opponentReady -> ReadyButtonState.BOTH_READY
        else -> ReadyButtonState.ENABLED
    }

    Button(
        onClick = onReady,
        enabled = buttonState == ReadyButtonState.ENABLED,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (buttonState) {
                ReadyButtonState.BOTH_READY -> Color(0xFF4CAF50)
                ReadyButtonState.WAITING -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            when (buttonState) {
                ReadyButtonState.DISABLED -> {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("SELECT A CARD FIRST", fontWeight = FontWeight.Bold)
                }
                ReadyButtonState.ENABLED -> {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("READY TO BATTLE!", fontWeight = FontWeight.Bold)
                }
                ReadyButtonState.WAITING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("WAITING FOR OPPONENT...", fontWeight = FontWeight.Bold)
                }
                ReadyButtonState.BOTH_READY -> {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("BATTLE STARTING!", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
