package com.rotdex.data.manager

import com.rotdex.data.database.CardDao
import com.rotdex.data.database.FusionHistoryDao
import com.rotdex.data.models.*
import kotlin.random.Random

/**
 * Manager for card fusion operations
 */
class FusionManager(
    private val cardDao: CardDao,
    private val fusionHistoryDao: FusionHistoryDao,
    private val achievementManager: AchievementManager,
    private val generateFusionCard: suspend (String) -> Result<Pair<String, Int>>,  // (imageUrl, rarity) from AI generation
    private val generateRpgAttributes: (String, CardRarity, Long) -> Triple<String, Int, Int>,  // (name, health, attack)
    private val generateBiography: (String, String, CardRarity) -> String  // (prompt, name, rarity) -> biography
) {

    /**
     * Validate if fusion can be performed
     */
    fun validateFusion(cards: List<Card>): FusionValidation {
        return FusionRules.canFuse(cards)
    }

    /**
     * Perform card fusion
     * @param inputCards Cards to fuse (will be deleted if successful)
     * @return FusionResult with the result card and details, or null if fusion failed
     */
    suspend fun performFusion(inputCards: List<Card>): FusionResult? {
        // Validate fusion
        val validation = validateFusion(inputCards)
        if (validation is FusionValidation.Error) {
            throw IllegalArgumentException(validation.message)
        }

        // Check for matching recipe
        val matchingRecipe = FusionRecipes.findMatchingRecipe(inputCards)

        // Calculate success rate
        val baseSuccessRate = (validation as FusionValidation.Valid).successRate
        val recipeBonus = if (matchingRecipe != null) 0.2f else 0f // +20% for recipe match
        val finalSuccessRate = minOf(1f, baseSuccessRate + recipeBonus)

        // Determine if fusion succeeded BEFORE generating AI card (to save API tokens)
        val randomValue = Random.nextFloat()
        val wasSuccessful = randomValue < finalSuccessRate

        // If fusion failed, return null without creating card or consuming inputs
        if (!wasSuccessful) {
            return null
        }

        // Fusion succeeded - generate result card with AI
        val resultCard = generateResultCard(inputCards, matchingRecipe)

        // Save result card to database
        val resultCardId = cardDao.insertCard(resultCard)
        val savedResultCard = resultCard.copy(id = resultCardId)

        // Determine fusion type
        val fusionType = FusionRules.getFusionType(
            cardCount = inputCards.size,
            hasRecipe = matchingRecipe != null
        )

        // Record fusion in history
        val fusionHistory = FusionHistory(
            inputCardIds = inputCards.map { it.id },
            inputRarities = inputCards.map { it.rarity },
            resultCardId = resultCardId,
            resultRarity = savedResultCard.rarity,
            fusionType = fusionType,
            wasSuccessful = true, // Always true here since we only reach this if successful
            recipeUsed = matchingRecipe?.id
        )
        fusionHistoryDao.insertFusion(fusionHistory)

        // Delete input cards (only on success)
        inputCards.forEach { card ->
            cardDao.deleteCard(card)
        }

        // Check if this is a new recipe discovery
        val recipeDiscovered = if (matchingRecipe != null && matchingRecipe.isSecret) {
            val previouslyDiscovered = fusionHistoryDao.isRecipeDiscovered(matchingRecipe.id) > 1 // >1 because we just inserted
            if (!previouslyDiscovered) matchingRecipe else null
        } else null

        // Check achievements after successful fusion (ignoring unlocked list for now - will be handled by ViewModel)
        achievementManager.checkFusionAchievements(isFirstRecipe = recipeDiscovered != null)
        achievementManager.checkCollectionAchievements()
        achievementManager.checkRarityAchievements(savedResultCard)

        return FusionResult(
            success = true,
            resultCard = savedResultCard,
            rarityUpgraded = savedResultCard.rarity != inputCards.first().rarity,
            bonusApplied = matchingRecipe?.name,
            recipeDiscovered = recipeDiscovered
        )
    }

    /**
     * Generate the result card from fusion
     * Creates a new AI-generated character that combines traits from parent cards
     * Only called when fusion is successful
     */
    private suspend fun generateResultCard(
        inputCards: List<Card>,
        recipe: FusionRecipe?
    ): Card {
        val inputRarity = inputCards.first().rarity

        // Determine result rarity (always upgraded since fusion succeeded)
        val resultRarity = when {
            recipe != null -> {
                // Recipe with guaranteed rarity, upgrade it further on success
                FusionRules.getNextRarity(recipe.guaranteedRarity)
            }
            else -> FusionRules.getNextRarity(inputRarity)
        }

        // Create fusion prompt that combines parent characters
        val fusionPrompt = createFusionPrompt(inputCards, recipe)

        // Generate AI card using the fusion prompt
        val aiResult = generateFusionCard(fusionPrompt)
        val (imageUrl, _) = aiResult.getOrElse {
            // If AI generation fails, use empty image URL as fallback
            "" to resultRarity.ordinal
        }

        // Combine tags
        val allTags = inputCards.flatMap { it.tags }.distinct().toMutableList()
        allTags.add("fusion")
        if (recipe != null) {
            allTags.addAll(recipe.guaranteedTags)
        }

        // Generate RPG attributes for fusion character
        val timestamp = System.currentTimeMillis()
        val (characterName, health, attack) = generateRpgAttributes(fusionPrompt, resultRarity, timestamp)
        val biography = generateBiography(fusionPrompt, characterName, resultRarity)

        return Card(
            prompt = fusionPrompt,
            imageUrl = imageUrl,
            rarity = resultRarity,
            tags = allTags.distinct(),
            createdAt = timestamp,
            name = characterName,
            health = health,
            attack = attack,
            biography = biography
        )
    }

    /**
     * Creates a fusion prompt that describes a hybrid character combining traits from parent cards
     * Uses brainrot Gen Z language for maximum chaos
     */
    private fun createFusionPrompt(inputCards: List<Card>, recipe: FusionRecipe?): String {
        val parentPrompts = inputCards.map { it.prompt }

        // Brainrot fusion descriptors
        val fusionTerms = listOf(
            "absolutely unhinged baby of",
            "cursed offspring spawned from",
            "chaotic love child between",
            "reality-breaking fusion of",
            "no cap literal fusion baby of",
            "sus hybrid created when",
            "demonic mashup of",
            "big yikes combination of"
        ).random()

        val actionTerms = listOf(
            "had a baby",
            "got freaky in the multiverse",
            "collided at max aura",
            "merged in the shadow realm",
            "fused in the backrooms",
            "combined their sigma energy",
            "clashed in Ohio",
            "united their gyatt powers"
        ).random()

        return when {
            recipe != null -> {
                // Recipe fusion: themed but still brainrot
                "Absolutely unhinged ${recipe.name} fusion - what if ${parentPrompts.take(2).joinToString(" and ")} $actionTerms fr fr"
            }
            parentPrompts.size >= 2 -> {
                // Two-card fusion: peak brainrot
                "The $fusionTerms (${parentPrompts[0]}) and (${parentPrompts[1]}) - " +
                "literally what happens when they $actionTerms, pure brainrot energy, mutated chaos character"
            }
            else -> {
                // Multiple cards: maximum chaos
                "Cursed fusion entity that spawned when ${parentPrompts.joinToString(", ")} all $actionTerms at the same time"
            }
        }
    }

    /**
     * Get discovered recipes
     */
    suspend fun getDiscoveredRecipes(): List<FusionRecipe> {
        val discoveredIds = fusionHistoryDao.getDiscoveredRecipes()
        return discoveredIds.mapNotNull { FusionRecipes.getRecipeById(it) }
    }

    /**
     * Get fusion statistics
     */
    suspend fun getFusionStats(): FusionStats {
        val totalFusions = fusionHistoryDao.getTotalFusionCount()
        val successfulFusions = fusionHistoryDao.getSuccessfulFusionCount()
        val successRate = fusionHistoryDao.getSuccessRate() ?: 0f

        val fusionsByRarity = mapOf(
            CardRarity.COMMON to fusionHistoryDao.getFusionCountByRarity(CardRarity.COMMON),
            CardRarity.RARE to fusionHistoryDao.getFusionCountByRarity(CardRarity.RARE),
            CardRarity.EPIC to fusionHistoryDao.getFusionCountByRarity(CardRarity.EPIC),
            CardRarity.LEGENDARY to fusionHistoryDao.getFusionCountByRarity(CardRarity.LEGENDARY)
        )

        return FusionStats(
            totalFusions = totalFusions,
            successfulFusions = successfulFusions,
            successRate = successRate,
            fusionsByRarity = fusionsByRarity
        )
    }
}

/**
 * Fusion statistics
 */
data class FusionStats(
    val totalFusions: Int,
    val successfulFusions: Int,
    val successRate: Float,
    val fusionsByRarity: Map<CardRarity, Int>
)
