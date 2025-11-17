package com.rotdex.data.models

/**
 * Configuration for all rewards in the game
 */
object RewardConfig {
    /**
     * All possible rewards from the spin wheel
     * Weights determine probability (higher weight = more common)
     */
    val SPIN_REWARDS = listOf(
        SpinReward(
            type = SpinRewardType.ENERGY,
            amount = 1,
            displayName = "+1 Energy",
            description = "Instant energy refill",
            weight = 30f
        ),
        SpinReward(
            type = SpinRewardType.ENERGY,
            amount = 3,
            displayName = "+3 Energy",
            description = "Major energy boost!",
            weight = 10f
        ),
        SpinReward(
            type = SpinRewardType.COINS,
            amount = 50,
            displayName = "50 Coins",
            description = "Some pocket change",
            weight = 25f
        ),
        SpinReward(
            type = SpinRewardType.COINS,
            amount = 200,
            displayName = "200 Coins",
            description = "Nice chunk of coins!",
            weight = 15f
        ),
        SpinReward(
            type = SpinRewardType.COINS,
            amount = 500,
            displayName = "500 Coins",
            description = "Big money!",
            weight = 5f
        ),
        SpinReward(
            type = SpinRewardType.GEMS,
            amount = 1,
            displayName = "+1 Gem",
            description = "Premium currency",
            weight = 10f
        ),
        SpinReward(
            type = SpinRewardType.GEMS,
            amount = 5,
            displayName = "+5 Gems",
            description = "Rare premium drop!",
            weight = 3f
        ),
        SpinReward(
            type = SpinRewardType.STREAK_PROTECTION,
            amount = 1,
            displayName = "Streak Shield",
            description = "Protect your streak once",
            weight = 8f
        ),
        SpinReward(
            type = SpinRewardType.RARITY_BOOST,
            amount = 1,
            displayName = "Legendary Luck",
            description = "+20% Legendary chance next gen",
            weight = 7f
        ),
        SpinReward(
            type = SpinRewardType.FREE_PACK,
            amount = 1,
            displayName = "Free Card Pack",
            description = "3 random cards!",
            weight = 12f
        ),
        SpinReward(
            type = SpinRewardType.JACKPOT,
            amount = 1,
            displayName = "JACKPOT!",
            description = "1000 coins + 20 gems!",
            weight = 1f
        )
    )

    /**
     * Streak milestone rewards
     * Players receive these bonuses when reaching specific streak days
     */
    val STREAK_MILESTONES = listOf(
        // Early milestones (Day 1-7)
        StreakMilestone(
            day = 1,
            rewardType = StreakRewardType.ENERGY,
            amount = 2,
            displayName = "First Day!",
            description = "+2 Energy"
        ),
        StreakMilestone(
            day = 3,
            rewardType = StreakRewardType.COINS,
            amount = 100,
            displayName = "3 Day Streak",
            description = "+100 Coins"
        ),
        StreakMilestone(
            day = 7,
            rewardType = StreakRewardType.RARE_PACK,
            amount = 1,
            displayName = "Week Warrior",
            description = "Free Rare Pack (3 cards, 1 guaranteed Rare+)"
        ),

        // Mid milestones (Day 8-21)
        StreakMilestone(
            day = 14,
            rewardType = StreakRewardType.EPIC_PACK,
            amount = 1,
            displayName = "Two Weeks Strong",
            description = "Free Epic Pack (5 cards, 1 guaranteed Epic+)"
        ),
        StreakMilestone(
            day = 21,
            rewardType = StreakRewardType.STREAK_PROTECTION,
            amount = 2,
            displayName = "Triple Week",
            description = "+2 Streak Protections"
        ),

        // Major milestones (Day 30+)
        StreakMilestone(
            day = 30,
            rewardType = StreakRewardType.CUSTOM_LEGENDARY,
            amount = 1,
            displayName = "Month Master",
            description = "Create your own Legendary card!"
        ),
        StreakMilestone(
            day = 60,
            rewardType = StreakRewardType.LEGENDARY_PACK,
            amount = 1,
            displayName = "Two Month Titan",
            description = "Legendary Pack (10 cards, 1 guaranteed Legendary)"
        ),
        StreakMilestone(
            day = 100,
            rewardType = StreakRewardType.CUSTOM_LEGENDARY,
            amount = 3,
            displayName = "Century Club",
            description = "3 Custom Legendary cards + Special badge"
        )
    )

    /**
     * Get milestone for a specific day, if one exists
     */
    fun getMilestoneForDay(day: Int): StreakMilestone? {
        return STREAK_MILESTONES.find { it.day == day }
    }

    /**
     * Get next milestone after current streak
     */
    fun getNextMilestone(currentStreak: Int): StreakMilestone? {
        return STREAK_MILESTONES.firstOrNull { it.day > currentStreak }
    }
}
