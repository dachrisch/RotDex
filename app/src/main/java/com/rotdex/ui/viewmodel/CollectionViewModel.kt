package com.rotdex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the card collection screen
 */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _selectedRarity = MutableStateFlow<CardRarity?>(null)
    val selectedRarity: StateFlow<CardRarity?> = _selectedRarity.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    /**
     * Reactive cards flow that automatically updates when repository, filter, or sort changes
     * Uses combine() to avoid multiple Flow collectors
     */
    val cards: StateFlow<List<Card>> = combine(
        cardRepository.getAllCards(),
        _selectedRarity,
        _sortOrder
    ) { allCards, rarity, order ->
        filterAndSortCards(allCards, rarity, order)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Reactive collection statistics that automatically updates when cards change
     */
    val stats: StateFlow<CollectionStats> = cards.map { cardList ->
        CollectionStats(
            totalCards = cardList.size,
            commonCount = cardList.count { it.rarity == CardRarity.COMMON },
            rareCount = cardList.count { it.rarity == CardRarity.RARE },
            epicCount = cardList.count { it.rarity == CardRarity.EPIC },
            legendaryCount = cardList.count { it.rarity == CardRarity.LEGENDARY }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CollectionStats(0, 0, 0, 0, 0)
    )

    /**
     * Filter cards by rarity
     */
    fun filterByRarity(rarity: CardRarity?) {
        _selectedRarity.value = rarity
    }

    /**
     * Change sort order
     */
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    /**
     * Apply filters and sorting to cards
     */
    private fun filterAndSortCards(
        allCards: List<Card>,
        rarity: CardRarity?,
        order: SortOrder
    ): List<Card> {
        var result = allCards

        // Filter by rarity if selected
        rarity?.let {
            result = result.filter { card -> card.rarity == rarity }
        }

        // Sort
        result = when (order) {
            SortOrder.NEWEST_FIRST -> result.sortedByDescending { it.createdAt }
            SortOrder.OLDEST_FIRST -> result.sortedBy { it.createdAt }
            SortOrder.RARITY_HIGH_TO_LOW -> result.sortedByDescending { it.rarity.ordinal }
            SortOrder.RARITY_LOW_TO_HIGH -> result.sortedBy { it.rarity.ordinal }
        }

        return result
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
