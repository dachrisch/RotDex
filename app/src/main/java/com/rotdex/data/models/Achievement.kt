package com.rotdex.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Types of achievements players can unlock
 */
enum class AchievementType {
    COLLECTION,      // Card collection milestones
    RARITY,          // First/multiple of each rarity
    FUSION,          // Fusion-related achievements
    STREAK,          // Login streak milestones
    GENERATION       // Card generation milestones
}

/**
 * Achievement definition
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val type: AchievementType,
    val requirement: Int,           // Number needed to unlock
    val coinReward: Int = 0,
    val gemReward: Int = 0,
    val energyReward: Int = 0,
    val icon: String = "🏆"
)

/**
 * Player's achievement progress and unlocks
 */
@Entity(tableName = "achievement_progress")
data class AchievementProgress(
    @PrimaryKey
    val achievementId: String,
    val currentProgress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L
)

/**
 * Predefined achievements
 */
object Achievements {

    // Collection Achievements
    val COLLECTOR_10 = Achievement(
        id = "collector_10",
        name = "Getting Started",
        description = "Collect 10 cards",
        type = AchievementType.COLLECTION,
        requirement = 10,
        coinReward = 100,
        icon = "📚"
    )

    val COLLECTOR_25 = Achievement(
        id = "collector_25",
        name = "Card Enthusiast",
        description = "Collect 25 cards",
        type = AchievementType.COLLECTION,
        requirement = 25,
        coinReward = 250,
        gemReward = 5,
        icon = "📚"
    )

    val COLLECTOR_50 = Achievement(
        id = "collector_50",
        name = "Serious Collector",
        description = "Collect 50 cards",
        type = AchievementType.COLLECTION,
        requirement = 50,
        coinReward = 500,
        gemReward = 10,
        icon = "📚"
    )

    val COLLECTOR_100 = Achievement(
        id = "collector_100",
        name = "Master Collector",
        description = "Collect 100 cards",
        type = AchievementType.COLLECTION,
        requirement = 100,
        coinReward = 1000,
        gemReward = 25,
        icon = "📚"
    )

    val COLLECTOR_250 = Achievement(
        id = "collector_250",
        name = "Legendary Collector",
        description = "Collect 250 cards",
        type = AchievementType.COLLECTION,
        requirement = 250,
        coinReward = 2500,
        gemReward = 50,
        icon = "📚"
    )

    // Rarity Achievements
    val FIRST_RARE = Achievement(
        id = "first_rare",
        name = "Rare Discovery",
        description = "Obtain your first Rare card",
        type = AchievementType.RARITY,
        requirement = 1,
        coinReward = 50,
        icon = "💎"
    )

    val FIRST_EPIC = Achievement(
        id = "first_epic",
        name = "Epic Find",
        description = "Obtain your first Epic card",
        type = AchievementType.RARITY,
        requirement = 1,
        coinReward = 100,
        gemReward = 5,
        icon = "💜"
    )

    val FIRST_LEGENDARY = Achievement(
        id = "first_legendary",
        name = "Legendary Status",
        description = "Obtain your first Legendary card",
        type = AchievementType.RARITY,
        requirement = 1,
        coinReward = 500,
        gemReward = 25,
        icon = "⭐"
    )

    val RARE_COLLECTOR = Achievement(
        id = "rare_collector_10",
        name = "Rare Specialist",
        description = "Collect 10 Rare cards",
        type = AchievementType.RARITY,
        requirement = 10,
        coinReward = 250,
        gemReward = 10,
        icon = "💎"
    )

    val EPIC_COLLECTOR = Achievement(
        id = "epic_collector_10",
        name = "Epic Specialist",
        description = "Collect 10 Epic cards",
        type = AchievementType.RARITY,
        requirement = 10,
        coinReward = 500,
        gemReward = 20,
        icon = "💜"
    )

    val LEGENDARY_COLLECTOR = Achievement(
        id = "legendary_collector_5",
        name = "Legendary Master",
        description = "Collect 5 Legendary cards",
        type = AchievementType.RARITY,
        requirement = 5,
        coinReward = 1000,
        gemReward = 50,
        icon = "⭐"
    )

    // Fusion Achievements
    val FIRST_FUSION = Achievement(
        id = "first_fusion",
        name = "Fusion Apprentice",
        description = "Perform your first fusion",
        type = AchievementType.FUSION,
        requirement = 1,
        coinReward = 100,
        icon = "⚗️"
    )

    val FUSION_10 = Achievement(
        id = "fusion_10",
        name = "Fusion Adept",
        description = "Perform 10 successful fusions",
        type = AchievementType.FUSION,
        requirement = 10,
        coinReward = 250,
        gemReward = 10,
        icon = "⚗️"
    )

    val FUSION_50 = Achievement(
        id = "fusion_50",
        name = "Fusion Master",
        description = "Perform 50 successful fusions",
        type = AchievementType.FUSION,
        requirement = 50,
        coinReward = 500,
        gemReward = 25,
        icon = "⚗️"
    )

    val FIRST_RECIPE = Achievement(
        id = "first_recipe",
        name = "Recipe Hunter",
        description = "Discover your first fusion recipe",
        type = AchievementType.FUSION,
        requirement = 1,
        coinReward = 200,
        gemReward = 10,
        icon = "📜"
    )

    // Generation Achievements
    val GENERATOR_10 = Achievement(
        id = "generator_10",
        name = "Creative Beginner",
        description = "Generate 10 cards",
        type = AchievementType.GENERATION,
        requirement = 10,
        coinReward = 50,
        icon = "✨"
    )

    val GENERATOR_50 = Achievement(
        id = "generator_50",
        name = "Creative Mind",
        description = "Generate 50 cards",
        type = AchievementType.GENERATION,
        requirement = 50,
        coinReward = 250,
        gemReward = 10,
        icon = "✨"
    )

    val GENERATOR_100 = Achievement(
        id = "generator_100",
        name = "Creation Master",
        description = "Generate 100 cards",
        type = AchievementType.GENERATION,
        requirement = 100,
        coinReward = 500,
        gemReward = 25,
        icon = "✨"
    )

    /**
     * All achievements in the game
     */
    val ALL_ACHIEVEMENTS = listOf(
        // Collection
        COLLECTOR_10, COLLECTOR_25, COLLECTOR_50, COLLECTOR_100, COLLECTOR_250,
        // Rarity
        FIRST_RARE, FIRST_EPIC, FIRST_LEGENDARY,
        RARE_COLLECTOR, EPIC_COLLECTOR, LEGENDARY_COLLECTOR,
        // Fusion
        FIRST_FUSION, FUSION_10, FUSION_50, FIRST_RECIPE,
        // Generation
        GENERATOR_10, GENERATOR_50, GENERATOR_100
    )
}
