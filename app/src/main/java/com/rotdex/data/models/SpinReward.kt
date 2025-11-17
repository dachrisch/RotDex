package com.rotdex.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Types of rewards available from the spin wheel
 */
enum class SpinRewardType {
    ENERGY,           // +1-3 Energy
    COINS,            // +50-500 Brainrot Coins
    GEMS,             // +1-10 Gems
    FREE_PACK,        // 1 Free Card Pack
    RARITY_BOOST,     // Next generation has +20% Legendary chance
    STREAK_PROTECTION, // +1 Streak Protection
    JACKPOT           // 1000 coins + 20 gems
}

/**
 * Represents a reward that can be won from the spin wheel
 */
data class SpinReward(
    val type: SpinRewardType,
    val amount: Int = 1,
    val displayName: String,
    val description: String,
    val weight: Float // Probability weight
)

/**
 * History of spin wheel results
 */
@Entity(tableName = "spin_history")
data class SpinHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rewardType: SpinRewardType,
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val streakDayAtSpin: Int
)

/**
 * Streak milestone rewards
 */
data class StreakMilestone(
    val day: Int,
    val rewardType: StreakRewardType,
    val amount: Int,
    val displayName: String,
    val description: String
)

/**
 * Types of streak milestone rewards
 */
enum class StreakRewardType {
    COINS,
    GEMS,
    ENERGY,
    FREE_GENERATION,
    RARE_PACK,
    EPIC_PACK,
    LEGENDARY_PACK,
    CUSTOM_LEGENDARY, // User picks prompt for guaranteed Legendary
    STREAK_PROTECTION
}
