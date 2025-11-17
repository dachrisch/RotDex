package com.rotdex.data.manager

import com.rotdex.data.database.UserProfileDao
import com.rotdex.data.models.RewardConfig
import com.rotdex.data.models.StreakMilestone
import com.rotdex.data.models.UserProfile
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Manages daily streak logic and calculations
 */
class StreakManager(
    private val userProfileDao: UserProfileDao
) {
    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd"
    }

    /**
     * Check and update the user's login streak
     * Should be called when the app starts or becomes active
     */
    suspend fun checkAndUpdateStreak(): StreakUpdateResult {
        val profile = userProfileDao.getUserProfileOnce() ?: createDefaultProfile()
        val today = getTodayDate()
        val lastLogin = profile.lastLoginDate

        return when {
            lastLogin == today -> {
                // Already logged in today
                StreakUpdateResult.AlreadyLoggedIn(profile.currentStreak)
            }
            isConsecutiveDay(lastLogin, today) -> {
                // Streak continues
                val newStreak = profile.currentStreak + 1
                val newLongest = maxOf(newStreak, profile.longestStreak)

                userProfileDao.updateStreak(newStreak, newLongest, today)

                // Check for milestone rewards
                val milestone = RewardConfig.getMilestoneForDay(newStreak)
                StreakUpdateResult.StreakIncreased(newStreak, milestone)
            }
            else -> {
                // Streak broken - check for protection
                if (profile.streakProtections > 0) {
                    StreakUpdateResult.ProtectionAvailable(profile.currentStreak)
                } else {
                    userProfileDao.updateStreak(1, profile.longestStreak, today)
                    StreakUpdateResult.StreakBroken(profile.currentStreak)
                }
            }
        }
    }

    /**
     * Use a streak protection to save a broken streak
     */
    suspend fun useStreakProtection(): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        if (profile.streakProtections <= 0) return false

        val today = getTodayDate()
        val rowsAffected = userProfileDao.useStreakProtection()

        if (rowsAffected > 0) {
            // Update last login date to today without breaking streak
            userProfileDao.updateProfile(
                profile.copy(
                    lastLoginDate = today
                )
            )
            return true
        }
        return false
    }

    /**
     * Get the next milestone the user is working towards
     */
    suspend fun getNextMilestone(): StreakMilestone? {
        val profile = userProfileDao.getUserProfileOnce() ?: return null
        return RewardConfig.getNextMilestone(profile.currentStreak)
    }

    /**
     * Check if two dates are consecutive days
     */
    private fun isConsecutiveDay(lastDate: String, currentDate: String): Boolean {
        if (lastDate.isEmpty()) return false

        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.US)
        val last = formatter.parse(lastDate) ?: return false
        val current = formatter.parse(currentDate) ?: return false

        val diffInMillis = current.time - last.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return diffInDays == 1L
    }

    /**
     * Get today's date in the standard format
     */
    private fun getTodayDate(): String {
        return SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
    }

    /**
     * Create a default user profile with starting bonuses
     */
    private suspend fun createDefaultProfile(): UserProfile {
        val profile = UserProfile(
            currentEnergy = 5,
            brainrotCoins = 100, // Starting bonus
            gems = 5, // Starting gems
            lastLoginDate = getTodayDate(),
            currentStreak = 1
        )
        userProfileDao.insertProfile(profile)
        return profile
    }
}

/**
 * Result of checking/updating the user's streak
 */
sealed class StreakUpdateResult {
    data class AlreadyLoggedIn(val currentStreak: Int) : StreakUpdateResult()
    data class StreakIncreased(val newStreak: Int, val milestone: StreakMilestone?) : StreakUpdateResult()
    data class StreakBroken(val previousStreak: Int) : StreakUpdateResult()
    data class ProtectionAvailable(val currentStreak: Int) : StreakUpdateResult()
}
