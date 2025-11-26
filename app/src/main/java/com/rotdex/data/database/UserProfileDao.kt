package com.rotdex.data.database

import androidx.room.*
import com.rotdex.data.models.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for UserProfile entity
 */
@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    fun getUserProfile(userId: String = "default_user"): Flow<UserProfile>

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    suspend fun getUserProfileOnce(userId: String = "default_user"): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    // Energy operations
    @Query("UPDATE user_profile SET currentEnergy = :energy, lastEnergyRefresh = :timestamp WHERE userId = :userId")
    suspend fun updateEnergy(energy: Int, timestamp: Long, userId: String = "default_user")

    @Query("UPDATE user_profile SET maxEnergy = :maxEnergy WHERE userId = :userId")
    suspend fun updateMaxEnergy(maxEnergy: Int, userId: String = "default_user")

    // Currency operations
    @Query("UPDATE user_profile SET brainrotCoins = brainrotCoins + :amount WHERE userId = :userId")
    suspend fun addCoins(amount: Int, userId: String = "default_user")

    @Query("UPDATE user_profile SET gems = gems + :amount WHERE userId = :userId")
    suspend fun addGems(amount: Int, userId: String = "default_user")

    @Query("UPDATE user_profile SET currentEnergy = currentEnergy + :amount WHERE userId = :userId")
    suspend fun addEnergy(amount: Int, userId: String = "default_user")

    @Query("UPDATE user_profile SET brainrotCoins = :amount WHERE userId = :userId")
    suspend fun setCoins(amount: Int, userId: String = "default_user")

    @Query("UPDATE user_profile SET gems = :amount WHERE userId = :userId")
    suspend fun setGems(amount: Int, userId: String = "default_user")

    // Streak operations
    @Query("UPDATE user_profile SET currentStreak = :streak, longestStreak = :longest, lastLoginDate = :date, totalLoginDays = totalLoginDays + 1 WHERE userId = :userId")
    suspend fun updateStreak(streak: Int, longest: Int, date: String, userId: String = "default_user")

    @Query("UPDATE user_profile SET hasUsedSpinToday = :used, lastSpinDate = :date, totalSpins = totalSpins + 1 WHERE userId = :userId")
    suspend fun updateSpinStatus(used: Boolean, date: String, userId: String = "default_user")

    @Query("UPDATE user_profile SET streakProtections = streakProtections + :amount WHERE userId = :userId")
    suspend fun addStreakProtections(amount: Int, userId: String = "default_user")

    @Query("UPDATE user_profile SET streakProtections = streakProtections - 1 WHERE userId = :userId AND streakProtections > 0")
    suspend fun useStreakProtection(userId: String = "default_user"): Int

    // Statistics queries
    @Query("SELECT totalSpins FROM user_profile WHERE userId = :userId")
    suspend fun getTotalSpins(userId: String = "default_user"): Int

    @Query("SELECT totalLoginDays FROM user_profile WHERE userId = :userId")
    suspend fun getTotalLoginDays(userId: String = "default_user"): Int

    @Query("SELECT longestStreak FROM user_profile WHERE userId = :userId")
    suspend fun getLongestStreak(userId: String = "default_user"): Int

    // Player Identity operations (Battle Arena UX)
    @Query("UPDATE user_profile SET playerName = :name WHERE userId = :userId")
    suspend fun updatePlayerName(name: String, userId: String = "default_user")

    @Query("UPDATE user_profile SET avatarImagePath = :imagePath WHERE userId = :userId")
    suspend fun updateAvatarImagePath(imagePath: String?, userId: String = "default_user")

    @Query("SELECT playerName FROM user_profile WHERE userId = :userId")
    suspend fun getPlayerName(userId: String = "default_user"): String?

    @Query("SELECT avatarImagePath FROM user_profile WHERE userId = :userId")
    suspend fun getAvatarImagePath(userId: String = "default_user"): String?
}
