package com.rotdex.data.repository

import com.rotdex.data.database.SpinHistoryDao
import com.rotdex.data.database.UserProfileDao
import com.rotdex.data.manager.SpinResult
import com.rotdex.data.manager.SpinWheelManager
import com.rotdex.data.manager.StreakManager
import com.rotdex.data.manager.StreakUpdateResult
import com.rotdex.data.models.SpinHistory
import com.rotdex.data.models.StreakMilestone
import com.rotdex.data.models.UserProfile
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Repository for managing user profile, progression, and rewards
 */
class UserRepository(
    private val userProfileDao: UserProfileDao,
    private val spinHistoryDao: SpinHistoryDao
) {
    private val streakManager = StreakManager(userProfileDao)
    private val spinWheelManager = SpinWheelManager(userProfileDao, spinHistoryDao)

    // MARK: - User Profile

    val userProfile: Flow<UserProfile> = userProfileDao.getUserProfile()

    suspend fun getUserProfileOnce(): UserProfile? = userProfileDao.getUserProfileOnce()

    /**
     * Initialize user profile with starting bonuses
     * Should be called when app first launches
     */
    suspend fun initializeUser() {
        val existing = userProfileDao.getUserProfileOnce()
        if (existing == null) {
            userProfileDao.insertProfile(
                UserProfile(
                    currentEnergy = 5,
                    brainrotCoins = 100, // Starting bonus
                    gems = 5 // Starting gems
                )
            )
        }
    }

    // MARK: - Streak Operations

    suspend fun updateDailyStreak(): StreakUpdateResult = streakManager.checkAndUpdateStreak()

    suspend fun useStreakProtection(): Boolean = streakManager.useStreakProtection()

    suspend fun getNextMilestone(): StreakMilestone? = streakManager.getNextMilestone()

    // MARK: - Spin Operations

    suspend fun canSpinToday(): Boolean = spinWheelManager.canSpinToday()

    suspend fun performDailySpin(): SpinResult = spinWheelManager.performSpin()

    fun getRecentSpins(limit: Int = 50): Flow<List<SpinHistory>> =
        spinHistoryDao.getRecentSpins(limit)

    suspend fun getLastSpinReward() = spinWheelManager.getLastSpinReward()

    // MARK: - Energy Operations

    /**
     * Regenerate energy based on time elapsed
     * Call this when app starts or becomes active
     * @return Amount of energy added
     */
    suspend fun regenerateEnergy(): Int {
        val profile = userProfileDao.getUserProfileOnce() ?: return 0
        val now = System.currentTimeMillis()
        val elapsed = now - profile.lastEnergyRefresh
        val hoursElapsed = TimeUnit.MILLISECONDS.toHours(elapsed)

        if (hoursElapsed >= 4) {
            val energyToAdd = (hoursElapsed / 4).toInt()
            val newEnergy = minOf(profile.currentEnergy + energyToAdd, profile.maxEnergy)
            val actualAdded = newEnergy - profile.currentEnergy

            if (actualAdded > 0) {
                userProfileDao.updateEnergy(newEnergy, now)
            }
            return actualAdded
        }
        return 0
    }

    /**
     * Spend energy for card generation
     * @param amount Amount of energy to spend
     * @return true if successful, false if not enough energy
     */
    suspend fun spendEnergy(amount: Int): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        if (profile.currentEnergy < amount) return false

        userProfileDao.updateEnergy(
            profile.currentEnergy - amount,
            System.currentTimeMillis()
        )
        return true
    }

    /**
     * Add energy (from rewards)
     */
    suspend fun addEnergy(amount: Int) {
        val profile = userProfileDao.getUserProfileOnce() ?: return
        val newEnergy = minOf(profile.currentEnergy + amount, profile.maxEnergy)
        userProfileDao.updateEnergy(newEnergy, System.currentTimeMillis())
    }

    /**
     * Get time until next energy regeneration in milliseconds
     */
    suspend fun getTimeUntilNextEnergy(): Long {
        val profile = userProfileDao.getUserProfileOnce() ?: return 0L
        if (profile.currentEnergy >= profile.maxEnergy) return 0L

        val elapsed = System.currentTimeMillis() - profile.lastEnergyRefresh
        val fourHoursInMillis = TimeUnit.HOURS.toMillis(4)
        val timeSinceLastRegen = elapsed % fourHoursInMillis

        return fourHoursInMillis - timeSinceLastRegen
    }

    // MARK: - Currency Operations

    /**
     * Spend brainrot coins
     * @return true if successful, false if not enough coins
     */
    suspend fun spendCoins(amount: Int): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        if (profile.brainrotCoins < amount) return false

        userProfileDao.addCoins(-amount)
        return true
    }

    /**
     * Add brainrot coins (from rewards)
     */
    suspend fun addCoins(amount: Int) {
        userProfileDao.addCoins(amount)
    }

    /**
     * Spend gems
     * @return true if successful, false if not enough gems
     */
    suspend fun spendGems(amount: Int): Boolean {
        val profile = userProfileDao.getUserProfileOnce() ?: return false
        if (profile.gems < amount) return false

        userProfileDao.addGems(-amount)
        return true
    }

    /**
     * Add gems (from rewards or purchases)
     */
    suspend fun addGems(amount: Int) {
        userProfileDao.addGems(amount)
    }

    // MARK: - Statistics

    suspend fun getTotalSpins(): Int = userProfileDao.getTotalSpins()

    suspend fun getTotalLoginDays(): Int = userProfileDao.getTotalLoginDays()

    suspend fun getLongestStreak(): Int = userProfileDao.getLongestStreak()

    // MARK: - Player Identity (Battle Arena UX)

    /**
     * Update player name
     * Trims whitespace and ignores empty/whitespace-only strings
     */
    suspend fun updatePlayerName(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isNotEmpty()) {
            userProfileDao.updatePlayerName(trimmedName)
        }
    }

    /**
     * Update avatar image path
     * Can be null to remove avatar
     */
    suspend fun updateAvatarImage(imagePath: String?) {
        userProfileDao.updateAvatarImagePath(imagePath)
    }

    /**
     * Get current player name
     * Returns generated default if profile doesn't exist
     */
    suspend fun getPlayerName(): String {
        return userProfileDao.getPlayerName() ?: UserProfile().playerName
    }

    /**
     * Get current avatar image path
     * Returns null if no avatar is set or profile doesn't exist
     */
    suspend fun getAvatarImagePath(): String? {
        return userProfileDao.getAvatarImagePath()
    }
}
