package com.rotdex.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rotdex.data.models.CardRarity

/**
 * Get the color for a card rarity
 * Uses centralized color definitions from Color.kt
 */
@Composable
fun CardRarity.getColor(): Color {
    return when (this) {
        CardRarity.COMMON -> RarityCommon
        CardRarity.RARE -> RarityRare
        CardRarity.EPIC -> RarityEpic
        CardRarity.LEGENDARY -> RarityLegendary
    }
}
