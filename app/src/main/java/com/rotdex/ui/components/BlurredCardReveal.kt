package com.rotdex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rotdex.data.models.BattleCard
import com.rotdex.utils.BlurUtils

/**
 * Displays a battle card with blur reveal animation.
 *
 * When [isRevealed] is false:
 * - Card image is blurred (on API 31+) or shows frosted glass overlay (API < 31)
 *
 * When [isRevealed] changes to true:
 * - Triggers 500ms animation to gradually remove blur
 * - Reveals the clear card image
 *
 * @param battleCard The battle card to display
 * @param isRevealed Whether the card should be revealed (true) or blurred (false)
 * @param modifier Optional modifier for the container
 */
@Composable
fun BlurredCardReveal(
    battleCard: BattleCard,
    isRevealed: Boolean,
    modifier: Modifier = Modifier
) {
    var revealProgress by remember { mutableFloatStateOf(0f) }

    // Animate reveal progress from 0f (blurred) to 1f (revealed)
    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = EaseInOut)
            ) { value, _ ->
                revealProgress = value
            }
        } else {
            // Reset progress when blurred again
            revealProgress = 0f
        }
    }

    Box(modifier = modifier) {
        // Card image with dynamic blur
        AsyncImage(
            model = battleCard.card.imageUrl,
            contentDescription = battleCard.card.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (BlurUtils.supportsNativeBlur && !isRevealed) {
                        // Fully blurred on devices with native blur support
                        Modifier.blur(radius = 20.dp)
                    } else if (BlurUtils.supportsNativeBlur && isRevealed) {
                        // Animate blur reduction from 20dp to 0dp
                        val blurRadius = 20.dp * (1 - revealProgress)
                        Modifier.blur(radius = blurRadius)
                    } else {
                        // No blur modifier for older devices (use overlay instead)
                        Modifier
                    }
                )
        )

        // Fallback: Frosted glass overlay for older devices (API < 31)
        if (!BlurUtils.supportsNativeBlur && !isRevealed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
        }

        // Animate out overlay on reveal for older devices
        if (!BlurUtils.supportsNativeBlur && isRevealed && revealProgress < 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f * (1 - revealProgress)),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f * (1 - revealProgress))
                            )
                        )
                    )
            )
        }

        // Question mark removed per user request - card will just be blurred
    }
}
