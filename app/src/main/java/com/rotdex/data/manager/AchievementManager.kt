package com.rotdex.data.manager

import com.rotdex.data.database.AchievementDao
import com.rotdex.data.database.CardDao
import com.rotdex.data.database.FusionHistoryDao
import com.rotdex.data.database.UserProfileDao
import com.rotdex.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Manages achievement tracking, progress, and unlocking
 */
class AchievementManager(
    private val achievementDao: AchievementDao,
    private val cardDao: CardDao,
    private val fusionHistoryDao: FusionHistoryDao,
    private val userProfileDao: UserProfileDao
) {

    /**
     * Initialize achievement tracking for new users
     */
    suspend fun initializeAchievements() {
        Achievements.ALL_ACHIEVEMENTS.forEach { achievement ->
            val existing = achievementDao.getProgress(achievement.id)
            if (existing == null) {
                achievementDao.upsertProgress(
                    AchievementProgress(
                        achievementId = achievement.id,
                        currentProgress = 0,
                        isUnlocked = false
                    )
                )
            }
        }
    }

    /**
     * Check and update collection achievements based on current card count
     */
    suspend fun checkCollectionAchievements() {
        val totalCards = cardDao.getTotalCardCount().first()

        val collectionAchievements = listOf(
            Achievements.COLLECTOR_10,
            Achievements.COLLECTOR_25,
            Achievements.COLLECTOR_50,
            Achievements.COLLECTOR_100,
            Achievements.COLLECTOR_250
        )

        collectionAchievements.forEach { achievement ->
            updateAchievementProgress(achievement, totalCards)
        }
    }

    /**
     * Check and update rarity achievements
     */
    suspend fun checkRarityAchievements(newCard: Card) {
        when (newCard.rarity) {
            CardRarity.RARE -> {
                updateAchievementProgress(Achievements.FIRST_RARE, 1, true)
                val rareCount = cardDao.getCardCountByRarity(CardRarity.RARE)
                updateAchievementProgress(Achievements.RARE_COLLECTOR, rareCount)
            }
            CardRarity.EPIC -> {
                updateAchievementProgress(Achievements.FIRST_EPIC, 1, true)
                val epicCount = cardDao.getCardCountByRarity(CardRarity.EPIC)
                updateAchievementProgress(Achievements.EPIC_COLLECTOR, epicCount)
            }
            CardRarity.LEGENDARY -> {
                updateAchievementProgress(Achievements.FIRST_LEGENDARY, 1, true)
                val legendaryCount = cardDao.getCardCountByRarity(CardRarity.LEGENDARY)
                updateAchievementProgress(Achievements.LEGENDARY_COLLECTOR, legendaryCount)
            }
            else -> {}
        }
    }

    /**
     * Check and update fusion achievements
     */
    suspend fun checkFusionAchievements(isFirstRecipe: Boolean = false) {
        val successfulFusions = fusionHistoryDao.getSuccessfulFusionCount()

        updateAchievementProgress(Achievements.FIRST_FUSION, successfulFusions, true)
        updateAchievementProgress(Achievements.FUSION_10, successfulFusions)
        updateAchievementProgress(Achievements.FUSION_50, successfulFusions)

        if (isFirstRecipe) {
            updateAchievementProgress(Achievements.FIRST_RECIPE, 1, true)
        }
    }

    /**
     * Check and update generation achievements
     */
    suspend fun checkGenerationAchievements() {
        val totalCards = cardDao.getTotalCardCount().first()

        updateAchievementProgress(Achievements.GENERATOR_10, totalCards)
        updateAchievementProgress(Achievements.GENERATOR_50, totalCards)
        updateAchievementProgress(Achievements.GENERATOR_100, totalCards)
    }

    /**
     * Update achievement progress and unlock if requirement met
     * @param achievement The achievement to update
     * @param currentValue Current progress value
     * @param isIncremental If true, only unlock if this specific increment matters (for "first" achievements)
     */
    private suspend fun updateAchievementProgress(
        achievement: Achievement,
        currentValue: Int,
        isIncremental: Boolean = false
    ) {
        val progress = achievementDao.getProgress(achievement.id) ?: return

        // Skip if already unlocked
        if (progress.isUnlocked) return

        // For incremental achievements (like "first rare"), only unlock if value == requirement
        if (isIncremental && currentValue != achievement.requirement) return

        // Update progress
        achievementDao.updateProgress(achievement.id, currentValue)

        // Check if requirement met
        if (currentValue >= achievement.requirement) {
            unlockAchievement(achievement)
        }
    }

    /**
     * Unlock an achievement and grant rewards
     */
    private suspend fun unlockAchievement(achievement: Achievement) {
        val timestamp = System.currentTimeMillis()
        achievementDao.unlockAchievement(achievement.id, timestamp)

        // Grant rewards
        if (achievement.coinReward > 0) {
            userProfileDao.addCoins(achievement.coinReward)
        }
        if (achievement.gemReward > 0) {
            userProfileDao.addGems(achievement.gemReward)
        }
        if (achievement.energyReward > 0) {
            userProfileDao.addEnergy(achievement.energyReward)
        }
    }

    /**
     * Get all unlocked achievements
     */
    fun getUnlockedAchievements(): Flow<List<AchievementProgress>> {
        return achievementDao.getUnlockedAchievements()
    }

    /**
     * Get all achievement progress
     */
    fun getAllProgress(): Flow<List<AchievementProgress>> {
        return achievementDao.getAllProgress()
    }

    /**
     * Get achievement details with progress
     */
    suspend fun getAchievementWithProgress(achievementId: String): Pair<Achievement, AchievementProgress>? {
        val achievement = Achievements.ALL_ACHIEVEMENTS.find { it.id == achievementId } ?: return null
        val progress = achievementDao.getProgress(achievementId) ?: return null
        return achievement to progress
    }

    /**
     * Get all achievements with their progress
     */
    suspend fun getAllAchievementsWithProgress(): List<Pair<Achievement, AchievementProgress>> {
        val allProgress = achievementDao.getAllProgress().first()
        return Achievements.ALL_ACHIEVEMENTS.mapNotNull { achievement ->
            val progress = allProgress.find { it.achievementId == achievement.id }
            if (progress != null) achievement to progress else null
        }
    }
}
