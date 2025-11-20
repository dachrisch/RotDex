package com.rotdex.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.data.repository.CardRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for CollectionViewModel
 * Tests filtering, sorting, and statistics functionality
 * Target: >70% code coverage
 */
@ExperimentalCoroutinesApi
class CollectionViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CollectionViewModel
    private lateinit var cardRepository: CardRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        cardRepository = mockk()

        // Default: empty list of cards
        coEvery { cardRepository.getAllCards() } returns flowOf(emptyList())

        viewModel = CollectionViewModel(cardRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Helper function to collect StateFlow values in tests
     * Needed because WhileSubscribed sharing strategy requires active subscription
     */
    private suspend fun <T> kotlinx.coroutines.flow.StateFlow<T>.testValue(): T {
        // Use first() to collect the current value, which activates the subscription
        return this.first()
    }

    @Test
    fun `initial state has empty cards list`() = runTest {
        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val cards = viewModel.cards.testValue()
        assertTrue(cards.isEmpty())
    }

    @Test
    fun `cards flow emits repository data`() = runTest {
        // Given
        val card1 = Card(
            id = 1,
            prompt = "test card 1",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000
        )
        val card2 = Card(
            id = 2,
            prompt = "test card 2",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.RARE,
            createdAt = 2000
        )
        val testCards = listOf(card1, card2)

        coEvery { cardRepository.getAllCards() } returns flowOf(testCards)

        // When - Create new ViewModel to trigger init
        val testViewModel = CollectionViewModel(cardRepository)

        // Collect first emission to trigger the flow
        val cards = testViewModel.cards.testValue()

        // Then
        assertEquals(2, cards.size)
        // Default sort is NEWEST_FIRST, so card2 (createdAt=2000) comes before card1 (createdAt=1000)
        assertEquals(card2.id, cards[0].id)
        assertEquals(card1.id, cards[1].id)
    }

    @Test
    fun `filterByRarity with COMMON shows only common cards`() = runTest {
        // Given
        val commonCard1 = Card(
            id = 1,
            prompt = "common test 1",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000
        )
        val rareCard = Card(
            id = 2,
            prompt = "rare test",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.RARE,
            createdAt = 2000
        )
        val commonCard2 = Card(
            id = 3,
            prompt = "common test 2",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.COMMON,
            createdAt = 3000
        )

        val allCards = listOf(commonCard1, rareCard, commonCard2)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test data
        val testViewModel = CollectionViewModel(cardRepository)

        // When
        testViewModel.filterByRarity(CardRarity.COMMON)

        // Then
        val filteredCards = testViewModel.cards.testValue()
        assertEquals(2, filteredCards.size)
        assertTrue(filteredCards.all { it.rarity == CardRarity.COMMON })
        // Default sort is NEWEST_FIRST, so commonCard2 (3000) comes before commonCard1 (1000)
        assertEquals(commonCard2.id, filteredCards[0].id)
        assertEquals(commonCard1.id, filteredCards[1].id)
    }

    @Test
    fun `filterByRarity with RARE shows only rare cards`() = runTest {
        // Given
        val commonCard = Card(
            id = 1,
            prompt = "common test",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000
        )
        val rareCard1 = Card(
            id = 2,
            prompt = "rare test 1",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.RARE,
            createdAt = 2000
        )
        val rareCard2 = Card(
            id = 3,
            prompt = "rare test 2",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.RARE,
            createdAt = 3000
        )

        val allCards = listOf(commonCard, rareCard1, rareCard2)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test data
        val testViewModel = CollectionViewModel(cardRepository)

        // When
        testViewModel.filterByRarity(CardRarity.RARE)

        // Then
        val filteredCards = testViewModel.cards.testValue()
        assertEquals(2, filteredCards.size)
        assertTrue(filteredCards.all { it.rarity == CardRarity.RARE })
    }

    @Test
    fun `filterByRarity with null shows all cards`() = runTest {
        // Given
        val commonCard = Card(
            id = 1,
            prompt = "common test",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000
        )
        val rareCard = Card(
            id = 2,
            prompt = "rare test",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.RARE,
            createdAt = 2000
        )
        val epicCard = Card(
            id = 3,
            prompt = "epic test",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.EPIC,
            createdAt = 3000
        )

        val allCards = listOf(commonCard, rareCard, epicCard)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test data
        val testViewModel = CollectionViewModel(cardRepository)

        // When
        testViewModel.filterByRarity(null)

        // Then
        val filteredCards = testViewModel.cards.testValue()
        assertEquals(3, filteredCards.size)
    }

    @Test
    fun `setSortOrder NEWEST_FIRST sorts correctly`() = runTest {
        // Given
        val oldCard = Card(
            id = 1,
            prompt = "old card",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000
        )
        val newerCard = Card(
            id = 2,
            prompt = "newer card",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.COMMON,
            createdAt = 2000
        )
        val newestCard = Card(
            id = 3,
            prompt = "newest card",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.COMMON,
            createdAt = 3000
        )

        val allCards = listOf(oldCard, newestCard, newerCard)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test data
        val testViewModel = CollectionViewModel(cardRepository)

        // When
        testViewModel.setSortOrder(SortOrder.NEWEST_FIRST)

        // Then
        val sortedCards = testViewModel.cards.testValue()
        assertEquals(3, sortedCards.size)
        assertEquals(newestCard.id, sortedCards[0].id)
        assertEquals(newerCard.id, sortedCards[1].id)
        assertEquals(oldCard.id, sortedCards[2].id)
    }

    @Test
    fun `setSortOrder OLDEST_FIRST sorts correctly`() = runTest {
        // Given
        val oldCard = Card(
            id = 1,
            prompt = "old card",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000
        )
        val newerCard = Card(
            id = 2,
            prompt = "newer card",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.COMMON,
            createdAt = 2000
        )
        val newestCard = Card(
            id = 3,
            prompt = "newest card",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.COMMON,
            createdAt = 3000
        )

        val allCards = listOf(newestCard, oldCard, newerCard)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test data
        val testViewModel = CollectionViewModel(cardRepository)

        // When
        testViewModel.setSortOrder(SortOrder.OLDEST_FIRST)

        // Then
        val sortedCards = testViewModel.cards.testValue()
        assertEquals(3, sortedCards.size)
        assertEquals(oldCard.id, sortedCards[0].id)
        assertEquals(newerCard.id, sortedCards[1].id)
        assertEquals(newestCard.id, sortedCards[2].id)
    }

    @Test
    fun `setSortOrder RARITY_HIGH_TO_LOW sorts correctly`() = runTest {
        // Given
        val commonCard = Card(
            id = 1,
            prompt = "common",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000
        )
        val rareCard = Card(
            id = 2,
            prompt = "rare",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.RARE,
            createdAt = 2000
        )
        val legendaryCard = Card(
            id = 3,
            prompt = "legendary",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.LEGENDARY,
            createdAt = 3000
        )
        val epicCard = Card(
            id = 4,
            prompt = "epic",
            imageUrl = "/path/to/image4.png",
            rarity = CardRarity.EPIC,
            createdAt = 4000
        )

        val allCards = listOf(commonCard, rareCard, legendaryCard, epicCard)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test data
        val testViewModel = CollectionViewModel(cardRepository)

        // When
        testViewModel.setSortOrder(SortOrder.RARITY_HIGH_TO_LOW)

        // Then
        val sortedCards = testViewModel.cards.testValue()
        assertEquals(4, sortedCards.size)
        assertEquals(CardRarity.LEGENDARY, sortedCards[0].rarity)
        assertEquals(CardRarity.EPIC, sortedCards[1].rarity)
        assertEquals(CardRarity.RARE, sortedCards[2].rarity)
        assertEquals(CardRarity.COMMON, sortedCards[3].rarity)
    }

    @Test
    fun `setSortOrder RARITY_LOW_TO_HIGH sorts correctly`() = runTest {
        // Given
        val commonCard = Card(
            id = 1,
            prompt = "common",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000
        )
        val rareCard = Card(
            id = 2,
            prompt = "rare",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.RARE,
            createdAt = 2000
        )
        val legendaryCard = Card(
            id = 3,
            prompt = "legendary",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.LEGENDARY,
            createdAt = 3000
        )
        val epicCard = Card(
            id = 4,
            prompt = "epic",
            imageUrl = "/path/to/image4.png",
            rarity = CardRarity.EPIC,
            createdAt = 4000
        )

        val allCards = listOf(legendaryCard, epicCard, commonCard, rareCard)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test data
        val testViewModel = CollectionViewModel(cardRepository)

        // When
        testViewModel.setSortOrder(SortOrder.RARITY_LOW_TO_HIGH)

        // Then
        val sortedCards = testViewModel.cards.testValue()
        assertEquals(4, sortedCards.size)
        assertEquals(CardRarity.COMMON, sortedCards[0].rarity)
        assertEquals(CardRarity.RARE, sortedCards[1].rarity)
        assertEquals(CardRarity.EPIC, sortedCards[2].rarity)
        assertEquals(CardRarity.LEGENDARY, sortedCards[3].rarity)
    }

    @Test
    fun `filter and sort work together correctly`() = runTest {
        // Given
        val commonCard1 = Card(
            id = 1,
            prompt = "common 1",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 3000  // Newest
        )
        val commonCard2 = Card(
            id = 2,
            prompt = "common 2",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000  // Oldest
        )
        val commonCard3 = Card(
            id = 3,
            prompt = "common 3",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.COMMON,
            createdAt = 2000  // Middle
        )
        val rareCard = Card(
            id = 4,
            prompt = "rare",
            imageUrl = "/path/to/image4.png",
            rarity = CardRarity.RARE,
            createdAt = 4000
        )

        val allCards = listOf(commonCard1, commonCard2, commonCard3, rareCard)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test data
        val testViewModel = CollectionViewModel(cardRepository)

        // When - Filter by COMMON and sort by OLDEST_FIRST
        testViewModel.filterByRarity(CardRarity.COMMON)
        testViewModel.setSortOrder(SortOrder.OLDEST_FIRST)

        // Then - Should only have common cards, sorted oldest to newest
        val result = testViewModel.cards.testValue()
        assertEquals(3, result.size)
        assertTrue(result.all { it.rarity == CardRarity.COMMON })
        assertEquals(commonCard2.id, result[0].id) // Oldest (1000)
        assertEquals(commonCard3.id, result[1].id) // Middle (2000)
        assertEquals(commonCard1.id, result[2].id) // Newest (3000)
    }

    @Test
    fun `getCollectionStats with empty list returns zeros`() = runTest {
        // Given - empty list (already set in setup)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val stats = viewModel.stats.testValue()

        // Then
        assertEquals(0, stats.totalCards)
        assertEquals(0, stats.commonCount)
        assertEquals(0, stats.rareCount)
        assertEquals(0, stats.epicCount)
        assertEquals(0, stats.legendaryCount)
    }

    @Test
    fun `getCollectionStats with mixed cards calculates correctly`() = runTest {
        // Given
        val commonCard1 = Card(
            id = 1,
            prompt = "common 1",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.COMMON,
            createdAt = 1000
        )
        val commonCard2 = Card(
            id = 2,
            prompt = "common 2",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.COMMON,
            createdAt = 2000
        )
        val rareCard = Card(
            id = 3,
            prompt = "rare",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.RARE,
            createdAt = 3000
        )
        val epicCard = Card(
            id = 4,
            prompt = "epic",
            imageUrl = "/path/to/image4.png",
            rarity = CardRarity.EPIC,
            createdAt = 4000
        )
        val legendaryCard = Card(
            id = 5,
            prompt = "legendary",
            imageUrl = "/path/to/image5.png",
            rarity = CardRarity.LEGENDARY,
            createdAt = 5000
        )

        val allCards = listOf(commonCard1, commonCard2, rareCard, epicCard, legendaryCard)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test cards
        val testViewModel = CollectionViewModel(cardRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val stats = testViewModel.stats.testValue()

        // Then
        assertEquals(5, stats.totalCards)
        assertEquals(2, stats.commonCount)
        assertEquals(1, stats.rareCount)
        assertEquals(1, stats.epicCount)
        assertEquals(1, stats.legendaryCount)
    }

    @Test
    fun `getCollectionStats counts each rarity correctly`() = runTest {
        // Given - Only legendary cards
        val legendaryCard1 = Card(
            id = 1,
            prompt = "legendary 1",
            imageUrl = "/path/to/image1.png",
            rarity = CardRarity.LEGENDARY,
            createdAt = 1000
        )
        val legendaryCard2 = Card(
            id = 2,
            prompt = "legendary 2",
            imageUrl = "/path/to/image2.png",
            rarity = CardRarity.LEGENDARY,
            createdAt = 2000
        )
        val legendaryCard3 = Card(
            id = 3,
            prompt = "legendary 3",
            imageUrl = "/path/to/image3.png",
            rarity = CardRarity.LEGENDARY,
            createdAt = 3000
        )

        val allCards = listOf(legendaryCard1, legendaryCard2, legendaryCard3)
        coEvery { cardRepository.getAllCards() } returns flowOf(allCards)

        // Create new ViewModel with test cards
        val testViewModel = CollectionViewModel(cardRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val stats = testViewModel.stats.testValue()

        // Then
        assertEquals(3, stats.totalCards)
        assertEquals(0, stats.commonCount)
        assertEquals(0, stats.rareCount)
        assertEquals(0, stats.epicCount)
        assertEquals(3, stats.legendaryCount)
    }
}
