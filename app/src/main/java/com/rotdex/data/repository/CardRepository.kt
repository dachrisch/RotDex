package com.rotdex.data.repository

import com.rotdex.data.api.AiApiService
import com.rotdex.data.api.ImageGenerationRequest
import com.rotdex.data.database.CardDao
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.data.models.CollectionStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * Repository for managing card data
 * Coordinates between local database and remote AI API
 */
class CardRepository(
    private val cardDao: CardDao,
    private val aiApiService: AiApiService
) {

    // MARK: - Card Collection Operations

    fun getAllCards(): Flow<List<Card>> = cardDao.getAllCards()

    fun getCardsByRarity(rarity: CardRarity): Flow<List<Card>> =
        cardDao.getCardsByRarity(rarity)

    fun getFavoriteCards(): Flow<List<Card>> = cardDao.getFavoriteCards()

    suspend fun getCardById(cardId: Long): Card? = cardDao.getCardById(cardId)

    suspend fun deleteCard(card: Card) = cardDao.deleteCard(card)

    suspend fun toggleFavorite(cardId: Long, isFavorite: Boolean) =
        cardDao.toggleFavorite(cardId, isFavorite)

    suspend fun incrementShareCount(cardId: Long) =
        cardDao.incrementShareCount(cardId)

    // MARK: - AI Card Generation

    /**
     * Generates a new brainrot card using AI
     * @param prompt User's input describing the card
     * @return Result with the generated Card or an error
     */
    suspend fun generateCard(prompt: String): Result<Card> {
        return try {
            // Call AI API to generate image
            val request = ImageGenerationRequest(
                prompt = enhancePrompt(prompt),
                size = "1024x1024",
                quality = "standard"
            )

            val response = aiApiService.generateImage(request)

            if (response.isSuccessful && response.body() != null) {
                val imageUrl = response.body()!!.data.firstOrNull()?.url
                    ?: return Result.failure(Exception("No image generated"))

                // Determine random rarity
                val rarity = determineRarity()

                // Create and save card
                val card = Card(
                    prompt = prompt,
                    imageUrl = imageUrl,
                    rarity = rarity,
                    createdAt = System.currentTimeMillis()
                )

                val cardId = cardDao.insertCard(card)
                val savedCard = cardDao.getCardById(cardId)

                if (savedCard != null) {
                    Result.success(savedCard)
                } else {
                    Result.failure(Exception("Failed to save card"))
                }
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // MARK: - Collection Statistics

    suspend fun getCollectionStats(): CollectionStats {
        return CollectionStats(
            totalCards = cardDao.getTotalCardCount().first(),
            commonCount = cardDao.getCardCountByRarity(CardRarity.COMMON),
            rareCount = cardDao.getCardCountByRarity(CardRarity.RARE),
            epicCount = cardDao.getCardCountByRarity(CardRarity.EPIC),
            legendaryCount = cardDao.getCardCountByRarity(CardRarity.LEGENDARY),
            totalShareCount = 0, // Calculate from all cards
            favoriteCount = cardDao.getFavoriteCards().first().size
        )
    }

    // MARK: - Helper Functions

    /**
     * Enhances user prompt for better AI generation
     */
    private fun enhancePrompt(userPrompt: String): String {
        return """
            Create a vibrant trading card style image featuring: $userPrompt
            Art style: digital art, colorful, meme culture aesthetic, internet culture
            Format: portrait orientation, clear focal point, suitable for a collectible card
            Quality: high detail, eye-catching
        """.trimIndent()
    }

    /**
     * Determines card rarity using weighted random selection
     */
    private fun determineRarity(): CardRarity {
        val random = Random.nextFloat()
        var cumulative = 0f

        CardRarity.values().forEach { rarity ->
            cumulative += rarity.dropRate
            if (random <= cumulative) {
                return rarity
            }
        }

        return CardRarity.COMMON // Fallback
    }
}
