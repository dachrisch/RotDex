package com.rotdex.data.repository

import android.content.Context
import android.util.Base64
import com.rotdex.data.api.AiApiService
import com.rotdex.data.api.ImageGenerationRequest
import com.rotdex.data.database.CardDao
import com.rotdex.data.database.FusionHistoryDao
import com.rotdex.data.manager.FusionManager
import com.rotdex.data.manager.FusionStats
import com.rotdex.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.random.Random

/**
 * Repository for managing card data
 * Coordinates between local database and remote AI API
 */
class CardRepository(
    private val context: Context,
    private val cardDao: CardDao,
    private val fusionHistoryDao: FusionHistoryDao,
    private val aiApiService: AiApiService,
    private val userRepository: UserRepository
) {

    private val fusionManager = FusionManager(cardDao, fusionHistoryDao)

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
     * Costs energy to generate - checks and spends energy before generating
     * @param prompt User's input describing the card
     * @return Result with the generated Card or an error
     */
    suspend fun generateCard(prompt: String): Result<Card> {
        return try {
            // Check and spend energy before generating
            val energySpent = userRepository.spendEnergy(GameConfig.CARD_GENERATION_ENERGY_COST)
            if (!energySpent) {
                return Result.failure(InsufficientEnergyException(
                    "Not enough energy to generate card. Need ${GameConfig.CARD_GENERATION_ENERGY_COST} energy."
                ))
            }

            // Call Freepik API to start image generation
            val request = ImageGenerationRequest.fromPrompt(enhancePrompt(prompt))
            val response = aiApiService.generateImage(request)

            if (response.isSuccessful && response.body() != null) {
                val jobId = response.body()!!.data.id

                // Poll for completion (max 30 seconds, check every 2 seconds)
                var attempts = 0
                val maxAttempts = 15
                var imageUrl: String? = null

                while (attempts < maxAttempts) {
                    delay(2000) // Wait 2 seconds between checks

                    val statusResponse = aiApiService.checkImageStatus(jobId)
                    if (statusResponse.isSuccessful && statusResponse.body() != null) {
                        val result = statusResponse.body()!!.data

                        when (result.status) {
                            "completed" -> {
                                imageUrl = result.image?.url
                                break
                            }
                            "failed" -> {
                                return Result.failure(Exception("Image generation failed"))
                            }
                            // "processing" - continue polling
                        }
                    }
                    attempts++
                }

                if (imageUrl == null) {
                    return Result.failure(Exception("Image generation timed out"))
                }

                // Download and save image from URL
                val imageFile = downloadAndSaveImage(imageUrl)
                    ?: return Result.failure(Exception("Failed to download image"))

                // Determine random rarity
                val rarity = determineRarity()

                // Create and save card with file path
                val card = Card(
                    prompt = prompt,
                    imageUrl = imageFile.absolutePath,  // Store local file path
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
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("API error ${response.code()}: $errorBody"))
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
     * Downloads image from URL and saves to local storage
     * @param imageUrl URL of the image to download
     * @return File object or null if download/save failed
     */
    private fun downloadAndSaveImage(imageUrl: String): File? {
        return try {
            // Download image from URL
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            val imageBytes = inputStream.readBytes()
            inputStream.close()

            // Create unique filename
            val timestamp = System.currentTimeMillis()
            val filename = "card_${timestamp}.png"

            // Get app's private storage directory
            val imagesDir = File(context.filesDir, "card_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            // Save to file
            val imageFile = File(imagesDir, filename)
            FileOutputStream(imageFile).use { outputStream ->
                outputStream.write(imageBytes)
            }

            imageFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves base64-encoded image to a file (legacy method, kept for compatibility)
     * @param base64String Base64-encoded image data
     * @return File object or null if save failed
     */
    private fun saveBase64Image(base64String: String): File? {
        return try {
            // Decode base64 string
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)

            // Create unique filename
            val timestamp = System.currentTimeMillis()
            val filename = "card_${timestamp}.png"

            // Get app's private storage directory
            val imagesDir = File(context.filesDir, "card_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            // Save to file
            val imageFile = File(imagesDir, filename)
            FileOutputStream(imageFile).use { outputStream ->
                outputStream.write(imageBytes)
            }

            imageFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

    // MARK: - Card Fusion Operations

    /**
     * Validate if cards can be fused
     */
    fun validateFusion(cards: List<Card>): FusionValidation {
        return fusionManager.validateFusion(cards)
    }

    /**
     * Perform card fusion
     */
    suspend fun performFusion(cards: List<Card>): FusionResult {
        return fusionManager.performFusion(cards)
    }

    /**
     * Get all fusion recipes (public only)
     */
    fun getPublicRecipes(): List<FusionRecipe> {
        return FusionRecipes.getPublicRecipes()
    }

    /**
     * Get discovered recipes (including secret ones)
     */
    suspend fun getDiscoveredRecipes(): List<FusionRecipe> {
        return fusionManager.getDiscoveredRecipes()
    }

    /**
     * Get fusion history
     */
    fun getFusionHistory(limit: Int = 50): Flow<List<FusionHistory>> {
        return fusionHistoryDao.getRecentFusions(limit)
    }

    /**
     * Get fusion statistics
     */
    suspend fun getFusionStats(): FusionStats {
        return fusionManager.getFusionStats()
    }

    /**
     * Find matching recipe for cards
     */
    fun findMatchingRecipe(cards: List<Card>): FusionRecipe? {
        return FusionRecipes.findMatchingRecipe(cards)
    }
}
