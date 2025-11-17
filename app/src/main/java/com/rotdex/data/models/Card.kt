package com.rotdex.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a brainrot card in the collection
 */
@Entity(tableName = "cards")
data class Card(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val prompt: String,
    val imageUrl: String,
    val rarity: CardRarity,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,

    // Optional metadata
    val tags: List<String> = emptyList(),
    val shareCount: Int = 0,
    val viewCount: Int = 0
)

/**
 * Card rarity levels
 */
enum class CardRarity(val displayName: String, val dropRate: Float) {
    COMMON("Common", 0.60f),      // 60% chance
    RARE("Rare", 0.25f),          // 25% chance
    EPIC("Epic", 0.12f),          // 12% chance
    LEGENDARY("Legendary", 0.03f) // 3% chance
}

/**
 * Statistics for the card collection
 */
data class CollectionStats(
    val totalCards: Int,
    val commonCount: Int,
    val rareCount: Int,
    val epicCount: Int,
    val legendaryCount: Int,
    val totalShareCount: Int,
    val favoriteCount: Int
)
