package com.rotdex.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User profile containing progression data, currency, and streak information
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val userId: String = "default_user", // Single user for now

    // Energy System
    val currentEnergy: Int = 5,
    val maxEnergy: Int = 5,
    val lastEnergyRefresh: Long = System.currentTimeMillis(),

    // Currency
    val brainrotCoins: Int = 0,
    val gems: Int = 0,

    // Streak System
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastLoginDate: String = "", // Format: "yyyy-MM-dd"
    val lastSpinDate: String = "", // Format: "yyyy-MM-dd"
    val hasUsedSpinToday: Boolean = false,

    // Streak Protection
    val streakProtections: Int = 0, // Number of protections available

    // Statistics
    val totalSpins: Int = 0,
    val totalLoginDays: Int = 0,
    val accountCreatedAt: Long = System.currentTimeMillis()
)
