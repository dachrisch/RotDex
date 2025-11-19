package com.rotdex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the card collection screen
 */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards.asStateFlow()

    private val _selectedRarity = MutableStateFlow<CardRarity?>(null)
    val selectedRarity: StateFlow<CardRarity?> = _selectedRarity.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    init {
        loadCards()
    }

    /**
     * Load all cards from the repository
     */
    private fun loadCards() {
        viewModelScope.launch {
            cardRepository.getAllCards().collect { allCards ->
                _cards.value = filterAndSortCards(allCards)
            }
        }
    }

    /**
     * Filter cards by rarity
     */
    fun filterByRarity(rarity: CardRarity?) {
        _selectedRarity.value = rarity
        viewModelScope.launch {
            cardRepository.getAllCards().collect { allCards ->
                _cards.value = filterAndSortCards(allCards)
            }
        }
    }

    /**
     * Change sort order
     */
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            cardRepository.getAllCards().collect { allCards ->
                _cards.value = filterAndSortCards(allCards)
            }
        }
    }

    /**
     * Apply filters and sorting to cards
     */
    private fun filterAndSortCards(allCards: List<Card>): List<Card> {
        var result = allCards

        // Filter by rarity if selected
        _selectedRarity.value?.let { rarity ->
            result = result.filter { it.rarity == rarity }
        }

        // Sort
        result = when (_sortOrder.value) {
            SortOrder.NEWEST_FIRST -> result.sortedByDescending { it.createdAt }
            SortOrder.OLDEST_FIRST -> result.sortedBy { it.createdAt }
            SortOrder.RARITY_HIGH_TO_LOW -> result.sortedByDescending { it.rarity.ordinal }
            SortOrder.RARITY_LOW_TO_HIGH -> result.sortedBy { it.rarity.ordinal }
        }

        return result
    }

    /**
     * Get collection statistics
     */
    fun getCollectionStats(): CollectionStats {
        val allCards = _cards.value
        return CollectionStats(
            totalCards = allCards.size,
            commonCount = allCards.count { it.rarity == CardRarity.COMMON },
            rareCount = allCards.count { it.rarity == CardRarity.RARE },
            epicCount = allCards.count { it.rarity == CardRarity.EPIC },
            legendaryCount = allCards.count { it.rarity == CardRarity.LEGENDARY }
        )
    }
}

/**
 * Sort order options for cards
 */
enum class SortOrder(val displayName: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    RARITY_HIGH_TO_LOW("Rarity: High to Low"),
    RARITY_LOW_TO_HIGH("Rarity: Low to High")
}

/**
 * Collection statistics
 */
data class CollectionStats(
    val totalCards: Int,
    val commonCount: Int,
    val rareCount: Int,
    val epicCount: Int,
    val legendaryCount: Int
)
