package com.rotdex.data.manager

import com.rotdex.data.database.SpinHistoryDao
import com.rotdex.data.database.UserProfileDao
import com.rotdex.data.models.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Manages spin wheel logic and reward distribution
 */
class SpinWheelManager(
    private val userProfileDao: UserProfileDao,
    private val spinHistoryDao: SpinHistoryDao
) {
    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd"
    }

    /**
     * Check if the user can spin the wheel today
     */
    suspend fun canSpinToday(): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        val today = getTodayDate()
        return profile.lastSpinDate != today || !profile.hasUsedSpinToday
    }

    /**
     * Perform a daily spin and return the result
     */
    suspend fun performSpin(): SpinResult {
        if (!canSpinToday()) {
            return SpinResult.AlreadySpunToday
        }

        val profile = userProfileDao.getUserProfileOnce()
            ?: return SpinResult.Error("Profile not found")

        try {
            // Weighted random selection
            val reward = selectRandomReward(profile.currentStreak)

            // Apply reward
            applyReward(reward, profile)

            // Record spin
            val today = getTodayDate()
            userProfileDao.updateSpinStatus(true, today)
            spinHistoryDao.insertSpin(
                SpinHistory(
                    rewardType = reward.type,
                    amount = reward.amount,
                    streakDayAtSpin = profile.currentStreak
                )
            )

            return SpinResult.Success(reward)
        } catch (e: Exception) {
            return SpinResult.Error("Failed to perform spin: ${e.message}")
        }
    }

    /**
     * Select a random reward based on weights
     * Higher streaks get bonus weights for better rewards
     */
    private fun selectRandomReward(streakDay: Int): SpinReward {
        // Bonus weights for higher streaks (+10% better rewards per week)
        val streakBonus = (streakDay / 7) * 0.1f

        val rewards = RewardConfig.SPIN_REWARDS.map {
            it.copy(weight = it.weight * (1f + streakBonus))
        }

        val totalWeight = rewards.sumOf { it.weight.toDouble() }.toFloat()
        val random = Random.nextFloat() * totalWeight

        var currentWeight = 0f
        for (reward in rewards) {
            currentWeight += reward.weight
            if (random <= currentWeight) {
                return reward
            }
        }

        // Fallback to last reward
        return rewards.last()
    }

    /**
     * Apply the reward to the user's profile
     */
    private suspend fun applyReward(reward: SpinReward, profile: UserProfile) {
        when (reward.type) {
            SpinRewardType.ENERGY -> {
                val newEnergy = minOf(
                    profile.currentEnergy + reward.amount,
                    profile.maxEnergy + reward.amount // Allow temporary overflow
                )
                userProfileDao.updateEnergy(newEnergy, System.currentTimeMillis())
            }
            SpinRewardType.COINS -> {
                userProfileDao.addCoins(reward.amount)
            }
            SpinRewardType.GEMS -> {
                userProfileDao.addGems(reward.amount)
            }
            SpinRewardType.STREAK_PROTECTION -> {
                userProfileDao.addStreakProtections(reward.amount)
            }
            SpinRewardType.JACKPOT -> {
                userProfileDao.addCoins(1000)
                userProfileDao.addGems(20)
            }
            // FREE_PACK and RARITY_BOOST are handled in the UI/Repository layer
            SpinRewardType.FREE_PACK,
            SpinRewardType.RARITY_BOOST -> {
                // These are handled elsewhere
            }
        }
    }

    /**
     * Get today's date in the standard format
     */
    private fun getTodayDate(): String {
        return SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
    }

    /**
     * Reset the daily spin (for testing or admin purposes)
     */
    suspend fun resetDailySpin() {
        val profile = userProfileDao.getUserProfileOnce() ?: return
        userProfileDao.updateProfile(
            profile.copy(
                hasUsedSpinToday = false,
                lastSpinDate = ""
            )
        )
    }
}

/**
 * Result of performing a spin
 */
sealed class SpinResult {
    data class Success(val reward: SpinReward) : SpinResult()
    object AlreadySpunToday : SpinResult()
    data class Error(val message: String) : SpinResult()
}
