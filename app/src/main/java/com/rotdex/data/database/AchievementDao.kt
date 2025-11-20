package com.rotdex.data.database

import androidx.room.*
import com.rotdex.data.models.AchievementProgress
import kotlinx.coroutines.flow.Flow

/**
 * DAO for achievement progress tracking
 */
@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievement_progress WHERE achievementId = :achievementId")
    suspend fun getProgress(achievementId: String): AchievementProgress?

    @Query("SELECT * FROM achievement_progress")
    fun getAllProgress(): Flow<List<AchievementProgress>>

    @Query("SELECT * FROM achievement_progress WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementProgress>>

    @Query("SELECT COUNT(*) FROM achievement_progress WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: AchievementProgress)

    @Query("UPDATE achievement_progress SET currentProgress = :progress WHERE achievementId = :achievementId")
    suspend fun updateProgress(achievementId: String, progress: Int)

    @Query("UPDATE achievement_progress SET isUnlocked = 1, unlockedAt = :timestamp WHERE achievementId = :achievementId")
    suspend fun unlockAchievement(achievementId: String, timestamp: Long)

    @Query("DELETE FROM achievement_progress")
    suspend fun clearAll()
}
