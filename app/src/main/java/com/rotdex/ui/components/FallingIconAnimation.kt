package com.rotdex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * A falling icon animation component that shows an icon and amount falling from a starting position
 * Used for visual feedback when spending resources (coins, energy, etc.)
 *
 * @param icon The emoji/text to animate
 * @param amount The amount to display (e.g., -1, -50)
 * @param startOffset The starting position offset from the top-right of the screen
 * @param startX Optional horizontal start position
 * @param onAnimationComplete Callback when animation finishes
 * @param modifier Modifier for the animation container
 */
@Composable
fun FallingIconAnimation(
    icon: String,
    amount: Int,
    startOffset: Dp = 0.dp,
    startX: Dp? = null,
    onAnimationComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    val density = LocalDensity.current

    // Random horizontal drift for variety
    val horizontalDrift = remember { Random.nextInt(-30, 30).dp }

    // Falling animation (from top to bottom) - short distance near stats
    val infiniteTransition = rememberInfiniteTransition(label = "falling")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = with(density) { 80.dp.toPx() },  // Reduced from 400dp to 80dp
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "y-offset"
    )

    // Fade out animation
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // Scale animation (slight shrink)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    // Auto-dismiss after animation
    LaunchedEffect(Unit) {
        delay(1500)
        isVisible = false
        onAnimationComplete()
    }

    if (isVisible) {
        Box(
            modifier = modifier
                .offset(
                    x = (startX ?: 0.dp) + horizontalDrift,
                    y = with(density) { offsetY.toDp() } + startOffset
                )
                .alpha(alpha)
                .scale(scale)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp
                )
                Text(
                    text = if (amount < 0) "$amount" else "+$amount",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Container for managing multiple falling icon animations
 * Automatically handles spawning and cleanup of animations
 */
@Composable
fun FallingIconsContainer(
    animations: List<FallingIconData>,
    onAnimationComplete: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        animations.forEach { data ->
            key(data.id) {
                FallingIconAnimation(
                    icon = data.icon,
                    amount = data.amount,
                    startOffset = data.startOffset,
                    startX = data.startX,
                    onAnimationComplete = { onAnimationComplete(data.id) }
                )
            }
        }
    }
}

/**
 * Data class representing a single falling icon animation
 */
data class FallingIconData(
    val id: String,
    val icon: String,
    val amount: Int,                    // Amount to display (e.g., -1, -50)
    val startOffset: Dp = 0.dp,
    val startX: Dp? = null              // Optional horizontal start position
)
