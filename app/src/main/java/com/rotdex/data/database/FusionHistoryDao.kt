package com.rotdex.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rotdex.data.models.CardRarity
import com.rotdex.data.models.FusionHistory
import kotlinx.coroutines.flow.Flow

/**
 * DAO for fusion history operations
 */
@Dao
interface FusionHistoryDao {

    /**
     * Get recent fusions
     */
    @Query("SELECT * FROM fusion_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentFusions(limit: Int = 50): Flow<List<FusionHistory>>

    /**
     * Get all successful fusions
     */
    @Query("SELECT * FROM fusion_history WHERE wasSuccessful = 1 ORDER BY timestamp DESC")
    fun getSuccessfulFusions(): Flow<List<FusionHistory>>

    /**
     * Insert a fusion record
     */
    @Insert
    suspend fun insertFusion(fusion: FusionHistory)

    /**
     * Get count of successful fusions
     */
    @Query("SELECT COUNT(*) FROM fusion_history WHERE wasSuccessful = 1")
    suspend fun getSuccessfulFusionCount(): Int

    /**
     * Get total fusion count
     */
    @Query("SELECT COUNT(*) FROM fusion_history")
    suspend fun getTotalFusionCount(): Int

    /**
     * Get fusion count by result rarity
     */
    @Query("SELECT COUNT(*) FROM fusion_history WHERE resultRarity = :rarity")
    suspend fun getFusionCountByRarity(rarity: CardRarity): Int

    /**
     * Get all discovered recipe IDs
     */
    @Query("SELECT DISTINCT recipeUsed FROM fusion_history WHERE recipeUsed IS NOT NULL")
    suspend fun getDiscoveredRecipes(): List<String>

    /**
     * Check if a recipe has been discovered
     */
    @Query("SELECT COUNT(*) FROM fusion_history WHERE recipeUsed = :recipeId LIMIT 1")
    suspend fun isRecipeDiscovered(recipeId: String): Int

    /**
     * Get fusion success rate (percentage of successful fusions)
     */
    @Query("""
        SELECT CAST(SUM(CASE WHEN wasSuccessful = 1 THEN 1 ELSE 0 END) AS REAL) /
               CAST(COUNT(*) AS REAL) * 100
        FROM fusion_history
    """)
    suspend fun getSuccessRate(): Float?
}
