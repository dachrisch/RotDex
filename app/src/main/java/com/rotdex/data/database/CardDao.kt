package com.rotdex.data.database

import androidx.room.*
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Card entity
 */
@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Long): Card?

    @Query("SELECT * FROM cards WHERE rarity = :rarity ORDER BY createdAt DESC")
    fun getCardsByRarity(rarity: CardRarity): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteCards(): Flow<List<Card>>

    @Query("SELECT COUNT(*) FROM cards")
    fun getTotalCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE rarity = :rarity")
    suspend fun getCardCountByRarity(rarity: CardRarity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card): Long

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("UPDATE cards SET isFavorite = :isFavorite WHERE id = :cardId")
    suspend fun toggleFavorite(cardId: Long, isFavorite: Boolean)

    @Query("UPDATE cards SET shareCount = shareCount + 1 WHERE id = :cardId")
    suspend fun incrementShareCount(cardId: Long)

    @Query("UPDATE cards SET viewCount = viewCount + 1 WHERE id = :cardId")
    suspend fun incrementViewCount(cardId: Long)
}
