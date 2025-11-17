package com.rotdex.data.database

import androidx.room.*
import com.rotdex.data.models.SpinHistory
import com.rotdex.data.models.SpinRewardType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SpinHistory entity
 */
@Dao
interface SpinHistoryDao {

    @Query("SELECT * FROM spin_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSpins(limit: Int = 50): Flow<List<SpinHistory>>

    @Query("SELECT * FROM spin_history WHERE id = :id")
    suspend fun getSpinById(id: Long): SpinHistory?

    @Insert
    suspend fun insertSpin(spin: SpinHistory): Long

    @Query("SELECT COUNT(*) FROM spin_history")
    suspend fun getTotalSpinCount(): Int

    @Query("SELECT COUNT(*) FROM spin_history WHERE rewardType = :type")
    suspend fun getCountByRewardType(type: SpinRewardType): Int

    @Query("SELECT SUM(amount) FROM spin_history WHERE rewardType = :type")
    suspend fun getTotalAmountByType(type: SpinRewardType): Int?

    @Query("SELECT * FROM spin_history WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getSpinsSince(startTime: Long): Flow<List<SpinHistory>>

    @Query("DELETE FROM spin_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldHistory(cutoffTime: Long): Int

    @Query("DELETE FROM spin_history")
    suspend fun deleteAllHistory()
}
