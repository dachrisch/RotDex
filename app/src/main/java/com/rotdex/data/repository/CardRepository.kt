package com.rotdex.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.rotdex.data.api.AiApiService
import com.rotdex.data.api.ImageGenerationRequest
import com.rotdex.data.api.ImageGenerationResponse
import com.rotdex.data.api.ImageStatusResponse
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

    companion object {
        private const val TAG = "CardRepository"
    }

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
            Log.d(TAG, "Starting card generation for prompt: $prompt")

            // Check and spend energy before generating
            Log.d(TAG, "Checking energy availability (need ${GameConfig.CARD_GENERATION_ENERGY_COST})")
            val energySpent = userRepository.spendEnergy(GameConfig.CARD_GENERATION_ENERGY_COST)
            if (!energySpent) {
                Log.w(TAG, "Insufficient energy for card generation")
                return Result.failure(InsufficientEnergyException(
                    "Not enough energy to generate card. Need ${GameConfig.CARD_GENERATION_ENERGY_COST} energy."
                ))
            }
            Log.d(TAG, "Energy spent successfully")

            // Call Freepik API to start image generation
            val enhancedPrompt = enhancePrompt(prompt)
            Log.d(TAG, "Enhanced prompt: $enhancedPrompt")
            val request = ImageGenerationRequest.fromPrompt(enhancedPrompt)
            Log.d(TAG, "Calling Freepik API to start image generation")
            val response = aiApiService.generateImage(request)

            Log.d(TAG, "API Response - Code: ${response.code()}, Success: ${response.isSuccessful}, Body: ${response.body()}")

            if (response.isSuccessful && response.body() != null) {
                val responseData = response.body()!!.data
                val jobId = responseData.id
                val status = responseData.status
                Log.i(TAG, "Image generation job created - ID: $jobId, Status: $status, Created at: ${responseData.created_at}")

                // Validate job ID
                if (jobId == null || jobId.isEmpty()) {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Received null or empty job ID from Freepik API. Response body: ${response.body()}, Error body: $errorBody")
                    return Result.failure(Exception("API returned null job ID. Unable to track image generation."))
                }

                // Poll for completion (max 30 seconds, check every 2 seconds)
                var attempts = 0
                val maxAttempts = 15
                var imageUrl: String? = null

                Log.d(TAG, "Starting polling loop (max $maxAttempts attempts)")
                while (attempts < maxAttempts) {
                    delay(2000) // Wait 2 seconds between checks
                    attempts++

                    Log.d(TAG, "Polling attempt $attempts/$maxAttempts - Checking status for job $jobId")
                    val statusResponse = aiApiService.checkImageStatus(jobId)

                    if (statusResponse.isSuccessful && statusResponse.body() != null) {
                        val result = statusResponse.body()!!.data
                        Log.d(TAG, "Job $jobId status: ${result.status}")

                        when (result.status) {
                            "completed" -> {
                                imageUrl = result.image?.url
                                Log.i(TAG, "Image generation completed! URL: $imageUrl")
                                break
                            }
                            "failed" -> {
                                Log.e(TAG, "Image generation failed for job $jobId")
                                return Result.failure(Exception("Image generation failed"))
                            }
                            "processing" -> {
                                Log.d(TAG, "Job $jobId still processing... (attempt $attempts/$maxAttempts)")
                            }
                            else -> {
                                Log.w(TAG, "Unknown status '${result.status}' for job $jobId")
                            }
                        }
                    } else {
                        val errorBody = statusResponse.errorBody()?.string()
                        Log.e(TAG, "Failed to check status for job $jobId - Code: ${statusResponse.code()}, Error: $errorBody")
                    }
                }

                if (imageUrl == null) {
                    Log.e(TAG, "Image generation timed out after $attempts attempts (${attempts * 2}s)")
                    return Result.failure(Exception("Image generation timed out"))
                }

                // Download and save image from URL
                Log.d(TAG, "Downloading image from: $imageUrl")
                val imageFile = downloadAndSaveImage(imageUrl)
                if (imageFile == null) {
                    Log.e(TAG, "Failed to download image from $imageUrl")
                    return Result.failure(Exception("Failed to download image"))
                }
                Log.i(TAG, "Image downloaded and saved to: ${imageFile.absolutePath}")

                // Determine random rarity
                val rarity = determineRarity()
                Log.d(TAG, "Assigned rarity: ${rarity.name} (drop rate: ${rarity.dropRate})")

                // Create and save card with file path
                val card = Card(
                    prompt = prompt,
                    imageUrl = imageFile.absolutePath,  // Store local file path
                    rarity = rarity,
                    createdAt = System.currentTimeMillis()
                )
                Log.d(TAG, "Creating card in database with prompt: '$prompt', rarity: ${rarity.name}")

                val cardId = cardDao.insertCard(card)
                Log.d(TAG, "Card inserted with ID: $cardId")
                val savedCard = cardDao.getCardById(cardId)

                if (savedCard != null) {
                    Log.i(TAG, "Card generation completed successfully! Card ID: $cardId, Rarity: ${rarity.name}")
                    Result.success(savedCard)
                } else {
                    Log.e(TAG, "Failed to retrieve saved card from database (ID: $cardId)")
                    Result.failure(Exception("Failed to save card"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Freepik API error - Code: ${response.code()}, Message: $errorBody")
                Result.failure(Exception("API error ${response.code()}: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during card generation: ${e.message}", e)
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
