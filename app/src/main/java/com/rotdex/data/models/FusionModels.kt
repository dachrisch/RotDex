package com.rotdex.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fusion recipe for special card combinations
 */
data class FusionRecipe(
    val id: String,
    val name: String,
    val description: String,
    val requiredCards: List<FusionCardRequirement>,
    val guaranteedRarity: CardRarity,
    val guaranteedTags: List<String> = emptyList(),
    val isSecret: Boolean = false // Discovered through experimentation
)

/**
 * Requirement for cards in a fusion recipe
 */
data class FusionCardRequirement(
    val rarity: CardRarity,
    val count: Int,
    val tagRequired: String? = null // Optional tag requirement
)

/**
 * History of fusion attempts
 */
@Entity(tableName = "fusion_history")
data class FusionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inputCardIds: List<Long>, // IDs of cards used
    val inputRarities: List<CardRarity>,
    val resultCardId: Long,
    val resultRarity: CardRarity,
    val fusionType: FusionType,
    val wasSuccessful: Boolean, // True if rarity upgraded
    val recipeUsed: String? = null, // Recipe ID if special recipe
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Type of fusion performed
 */
enum class FusionType {
    STANDARD,        // Normal fusion
    RECIPE,          // Special recipe
    SUPER_FUSION     // 5+ cards
}

/**
 * Result of a fusion attempt
 */
data class FusionResult(
    val success: Boolean,
    val resultCard: Card,
    val rarityUpgraded: Boolean,
    val bonusApplied: String? = null,
    val recipeDiscovered: FusionRecipe? = null
)

/**
 * Validation result for fusion attempt
 */
sealed class FusionValidation {
    data class Valid(val successRate: Float) : FusionValidation()
    data class Error(val message: String) : FusionValidation()
}
