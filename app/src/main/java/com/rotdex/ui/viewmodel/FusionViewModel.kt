package com.rotdex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotdex.data.manager.FusionStats
import com.rotdex.data.models.*
import com.rotdex.data.repository.CardRepository
import com.rotdex.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for card fusion operations
 */
@HiltViewModel
class FusionViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // All available cards for fusion
    val allCards: StateFlow<List<Card>> = cardRepository.getAllCards()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected cards for fusion
    private val _selectedCards = MutableStateFlow<List<Card>>(emptyList())
    val selectedCards: StateFlow<List<Card>> = _selectedCards.asStateFlow()

    // Current fusion state
    private val _fusionState = MutableStateFlow<FusionState>(FusionState.Idle)
    val fusionState: StateFlow<FusionState> = _fusionState.asStateFlow()

    // Fusion validation result
    private val _validation = MutableStateFlow<FusionValidation?>(null)
    val validation: StateFlow<FusionValidation?> = _validation.asStateFlow()

    // Matching recipe for selected cards
    private val _matchingRecipe = MutableStateFlow<FusionRecipe?>(null)
    val matchingRecipe: StateFlow<FusionRecipe?> = _matchingRecipe.asStateFlow()

    // Public recipes (always visible)
    private val _publicRecipes = MutableStateFlow<List<FusionRecipe>>(emptyList())
    val publicRecipes: StateFlow<List<FusionRecipe>> = _publicRecipes.asStateFlow()

    // Discovered recipes (including secret ones)
    private val _discoveredRecipes = MutableStateFlow<List<FusionRecipe>>(emptyList())
    val discoveredRecipes: StateFlow<List<FusionRecipe>> = _discoveredRecipes.asStateFlow()

    // Fusion history
    val fusionHistory: StateFlow<List<FusionHistory>> = cardRepository.getFusionHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Fusion statistics
    private val _fusionStats = MutableStateFlow<FusionStats?>(null)
    val fusionStats: StateFlow<FusionStats?> = _fusionStats.asStateFlow()

    init {
        loadRecipes()
        loadFusionStats()
    }

    /**
     * Toggle card selection for fusion
     */
    fun toggleCardSelection(card: Card) {
        val currentSelection = _selectedCards.value.toMutableList()

        if (currentSelection.contains(card)) {
            currentSelection.remove(card)
        } else {
            // Check if we can add more cards
            if (currentSelection.size < FusionRules.MAX_FUSION_CARDS) {
                currentSelection.add(card)
            }
        }

        _selectedCards.value = currentSelection
        validateSelection()
        checkForMatchingRecipe()
    }

    /**
     * Clear selected cards
     */
    fun clearSelection() {
        _selectedCards.value = emptyList()
        _validation.value = null
        _matchingRecipe.value = null
    }

    /**
     * Validate current selection
     */
    private fun validateSelection() {
        val cards = _selectedCards.value
        _validation.value = if (cards.isNotEmpty()) {
            cardRepository.validateFusion(cards)
        } else {
            null
        }
    }

    /**
     * Check if selected cards match any recipe
     */
    private fun checkForMatchingRecipe() {
        val cards = _selectedCards.value
        _matchingRecipe.value = if (cards.size >= FusionRules.MIN_FUSION_CARDS) {
            cardRepository.findMatchingRecipe(cards)
        } else {
            null
        }
    }

    /**
     * Perform fusion with selected cards
     */
    fun performFusion() {
        viewModelScope.launch {
            try {
                val cards = _selectedCards.value

                // Validate before fusion
                val validation = cardRepository.validateFusion(cards)
                if (validation is FusionValidation.Error) {
                    _fusionState.value = FusionState.Error(validation.message)
                    return@launch
                }

                // Spend coins before starting
                val coinSpent = userRepository.spendCoins(GameConfig.FUSION_COIN_COST)
                if (!coinSpent) {
                    _fusionState.value = FusionState.Error("Not enough coins for fusion")
                    return@launch
                }

                // Start fusion animation
                _fusionState.value = FusionState.Fusing

                // Simulate fusion animation duration (initial delay)
                delay(2500)

                // Perform actual fusion (checks success internally)
                val result = cardRepository.performFusion(cards)

                if (result != null) {
                    // Fusion succeeded - show result
                    _fusionState.value = FusionState.Result(result)

                    // Clear selection
                    clearSelection()

                    // Reload stats and recipes
                    loadFusionStats()
                    loadDiscoveredRecipes()
                } else {
                    // Fusion failed - add random delay (10-30s) then show failure
                    val randomDelay = (10000..30000).random().toLong()
                    delay(randomDelay)

                    _fusionState.value = FusionState.Failed

                    // Don't clear selection - cards weren't consumed
                    // Don't reload stats - nothing changed
                }

            } catch (e: Exception) {
                _fusionState.value = FusionState.Error(e.message ?: "Fusion failed")
            }
        }
    }

    /**
     * Reset fusion state to idle
     */
    fun resetFusionState() {
        _fusionState.value = FusionState.Idle
    }

    /**
     * Load recipes
     */
    private fun loadRecipes() {
        viewModelScope.launch {
            _publicRecipes.value = cardRepository.getPublicRecipes()
            loadDiscoveredRecipes()
        }
    }

    /**
     * Load discovered recipes
     */
    private fun loadDiscoveredRecipes() {
        viewModelScope.launch {
            _discoveredRecipes.value = cardRepository.getDiscoveredRecipes()
        }
    }

    /**
     * Load fusion statistics
     */
    private fun loadFusionStats() {
        viewModelScope.launch {
            _fusionStats.value = cardRepository.getFusionStats()
        }
    }

    /**
     * Select cards to match a specific recipe
     */
    fun selectCardsForRecipe(recipe: FusionRecipe) {
        viewModelScope.launch {
            val cards = allCards.value
            val selectedForRecipe = mutableListOf<Card>()

            // Try to find cards that match recipe requirements
            recipe.requiredCards.forEach { requirement ->
                val matchingCards = cards.filter { card ->
                    card.rarity == requirement.rarity &&
                            !selectedForRecipe.contains(card) &&
                            (requirement.tagRequired == null || card.tags.contains(requirement.tagRequired))
                }

                selectedForRecipe.addAll(matchingCards.take(requirement.count))
            }

            if (selectedForRecipe.size >= FusionRules.MIN_FUSION_CARDS) {
                _selectedCards.value = selectedForRecipe
                validateSelection()
                checkForMatchingRecipe()
            }
        }
    }

    /**
     * Get cards grouped by rarity for easier selection
     */
    fun getCardsByRarity(rarity: CardRarity): List<Card> {
        return allCards.value.filter { it.rarity == rarity }
    }
}

/**
 * UI state for fusion screen
 */
sealed class FusionState {
    object Idle : FusionState()
    object Fusing : FusionState()
    data class Result(val fusionResult: FusionResult) : FusionState()
    object Failed : FusionState() // Fusion failed due to chance, cards not consumed
    data class Error(val message: String) : FusionState()
}
