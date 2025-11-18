package com.rotdex.data.models

import kotlin.math.min

/**
 * Rules and algorithms for card fusion system
 */
object FusionRules {
    // Base success rates for rarity upgrade
    private val BASE_SUCCESS_RATES = mapOf(
        CardRarity.COMMON to 0.30f,      // 30% → Rare
        CardRarity.RARE to 0.20f,        // 20% → Epic
        CardRarity.EPIC to 0.10f,        // 10% → Legendary
        CardRarity.LEGENDARY to 0.0f     // Cannot upgrade Legendary
    )

    // Bonus per additional card (after 2 cards minimum)
    private const val BONUS_PER_CARD = 0.05f // +5% per card

    // Maximum cards that can be fused
    const val MAX_FUSION_CARDS = 5

    // Minimum cards required for fusion
    const val MIN_FUSION_CARDS = 2

    /**
     * Calculate success rate for fusion
     */
    fun calculateSuccessRate(
        cards: List<Card>,
        recipeBonus: Float = 0f
    ): Float {
        if (cards.isEmpty()) return 0f

        // Must be same rarity
        val rarity = cards.first().rarity
        if (!cards.all { it.rarity == rarity }) return 0f

        val baseRate = BASE_SUCCESS_RATES[rarity] ?: 0f
        val cardCountBonus = kotlin.math.maxOf(0, cards.size - 2) * BONUS_PER_CARD

        return min(1f, baseRate + cardCountBonus + recipeBonus)
    }

    /**
     * Get the next rarity level
     */
    fun getNextRarity(current: CardRarity): CardRarity {
        return when (current) {
            CardRarity.COMMON -> CardRarity.RARE
            CardRarity.RARE -> CardRarity.EPIC
            CardRarity.EPIC -> CardRarity.LEGENDARY
            CardRarity.LEGENDARY -> CardRarity.LEGENDARY
        }
    }

    /**
     * Check if cards can be fused
     */
    fun canFuse(cards: List<Card>): FusionValidation {
        return when {
            cards.size < MIN_FUSION_CARDS ->
                FusionValidation.Error("Need at least $MIN_FUSION_CARDS cards")
            cards.size > MAX_FUSION_CARDS ->
                FusionValidation.Error("Maximum $MAX_FUSION_CARDS cards")
            !cards.all { it.rarity == cards.first().rarity } ->
                FusionValidation.Error("All cards must be same rarity")
            cards.first().rarity == CardRarity.LEGENDARY && cards.size < 3 ->
                FusionValidation.Error("Legendary fusion requires 3+ cards")
            else -> FusionValidation.Valid(calculateSuccessRate(cards))
        }
    }

    /**
     * Get fusion type based on card count and recipe
     */
    fun getFusionType(cardCount: Int, hasRecipe: Boolean): FusionType {
        return when {
            hasRecipe -> FusionType.RECIPE
            cardCount >= 5 -> FusionType.SUPER_FUSION
            else -> FusionType.STANDARD
        }
    }
}
