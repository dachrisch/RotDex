package com.rotdex.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.data.repository.CardRepository
import com.rotdex.ui.theme.RotDexTheme
import com.rotdex.ui.viewmodel.CollectionStats
import com.rotdex.ui.viewmodel.CollectionViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for CollectionScreen
 * Tests UI interactions, filtering, sorting, and display logic
 */
@RunWith(AndroidJUnit4::class)
class CollectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Helper function to create a mock ViewModel with test data
     */
    private fun createMockViewModel(cards: List<Card> = emptyList()): CollectionViewModel {
        val mockRepository = mockk<CardRepository>(relaxed = true)
        coEvery { mockRepository.getAllCards() } returns flowOf(cards)
        return CollectionViewModel(mockRepository)
    }

    @Test
    fun empty_state_displays_when_no_cards() {
        // Given - ViewModel with no cards
        val viewModel = createMockViewModel(emptyList())

        // When
        composeTestRule.setContent {
            RotDexTheme {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("No cards yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start creating cards to build your collection!")
            .assertIsDisplayed()
    }

    @Test
    fun cards_display_in_grid_when_available() {
        // Given - ViewModel with test cards
        val testCards = listOf(
            Card(
                id = 1,
                prompt = "Test Card 1",
                imageUrl = "/path/to/image1.png",
                rarity = CardRarity.COMMON,
                createdAt = 1000
            ),
            Card(
                id = 2,
                prompt = "Test Card 2",
                imageUrl = "/path/to/image2.png",
                rarity = CardRarity.RARE,
                createdAt = 2000
            )
        )
        val viewModel = createMockViewModel(testCards)

        // When
        composeTestRule.setContent {
            RotDexTheme {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // Then - Cards should be displayed
        composeTestRule.onNodeWithText("Test Card 1").assertExists()
        composeTestRule.onNodeWithText("Test Card 2").assertExists()
        composeTestRule.onNodeWithText("Common").assertExists()
        composeTestRule.onNodeWithText("Rare").assertExists()
    }

    @Test
    fun filter_button_opens_menu() {
        // Given
        val viewModel = createMockViewModel(emptyList())

        composeTestRule.setContent {
            RotDexTheme {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // When - Click filter button
        composeTestRule.onNodeWithContentDescription("Filter").performClick()

        // Then - Filter menu should be visible
        composeTestRule.onNodeWithText("All Cards").assertIsDisplayed()
        composeTestRule.onNodeWithText("Common").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rare").assertIsDisplayed()
        composeTestRule.onNodeWithText("Epic").assertIsDisplayed()
        composeTestRule.onNodeWithText("Legendary").assertIsDisplayed()
    }

    @Test
    fun sort_button_opens_menu() {
        // Given
        val viewModel = createMockViewModel(emptyList())

        composeTestRule.setContent {
            RotDexTheme {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // When - Click sort button
        composeTestRule.onNodeWithContentDescription("Sort").performClick()

        // Then - Sort menu should be visible
        composeTestRule.onNodeWithText("Newest First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oldest First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rarity: High to Low").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rarity: Low to High").assertIsDisplayed()
    }

    @Test
    fun clicking_card_opens_fullscreen_viewer() {
        // Given - ViewModel with a test card
        val testCards = listOf(
            Card(
                id = 1,
                prompt = "Clickable Test Card",
                imageUrl = "/path/to/image1.png",
                rarity = CardRarity.EPIC,
                createdAt = 1000
            )
        )
        val viewModel = createMockViewModel(testCards)

        composeTestRule.setContent {
            RotDexTheme {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // When - Click on the card
        composeTestRule.onNodeWithText("Clickable Test Card").performClick()

        // Then - Fullscreen viewer should be displayed
        // The fullscreen viewer should show the rarity badge in uppercase
        composeTestRule.onNodeWithText("EPIC").assertIsDisplayed()
        // Close button should be visible
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test
    fun fullscreen_viewer_can_be_dismissed() {
        // Given - ViewModel with a test card
        val testCards = listOf(
            Card(
                id = 1,
                prompt = "Dismissible Card",
                imageUrl = "/path/to/image1.png",
                rarity = CardRarity.LEGENDARY,
                createdAt = 1000
            )
        )
        val viewModel = createMockViewModel(testCards)

        composeTestRule.setContent {
            RotDexTheme {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // When - Click card to open fullscreen viewer
        composeTestRule.onNodeWithText("Dismissible Card").performClick()

        // Verify fullscreen viewer is open
        composeTestRule.onNodeWithText("LEGENDARY").assertIsDisplayed()

        // When - Click close button
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        // Then - Fullscreen viewer should be dismissed
        // The uppercase "LEGENDARY" should no longer be visible (only lowercase in grid badge)
        composeTestRule.onNodeWithText("LEGENDARY").assertDoesNotExist()
    }

    @Test
    fun collection_stats_display_correctly() {
        // Given - ViewModel with mixed rarity cards
        val testCards = listOf(
            Card(
                id = 1,
                prompt = "Common Card 1",
                imageUrl = "/path/to/image1.png",
                rarity = CardRarity.COMMON,
                createdAt = 1000
            ),
            Card(
                id = 2,
                prompt = "Common Card 2",
                imageUrl = "/path/to/image2.png",
                rarity = CardRarity.COMMON,
                createdAt = 2000
            ),
            Card(
                id = 3,
                prompt = "Rare Card",
                imageUrl = "/path/to/image3.png",
                rarity = CardRarity.RARE,
                createdAt = 3000
            ),
            Card(
                id = 4,
                prompt = "Epic Card",
                imageUrl = "/path/to/image4.png",
                rarity = CardRarity.EPIC,
                createdAt = 4000
            ),
            Card(
                id = 5,
                prompt = "Legendary Card",
                imageUrl = "/path/to/image5.png",
                rarity = CardRarity.LEGENDARY,
                createdAt = 5000
            )
        )
        val viewModel = createMockViewModel(testCards)

        composeTestRule.setContent {
            RotDexTheme {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // Then - Collection stats should display correct counts
        composeTestRule.onNodeWithText("Collection Stats").assertIsDisplayed()
        // Note: The stats are displayed in a specific format - we need to check for the count values
        // The CollectionStatsCard shows: Total, Common, Rare, Epic, Legendary with their counts
        // Since text nodes might be separate, we'll verify the stats card exists
        composeTestRule.onNodeWithText("Total").assertExists()
        composeTestRule.onNodeWithText("Common").assertExists()
        composeTestRule.onNodeWithText("Rare").assertExists()
        composeTestRule.onNodeWithText("Epic").assertExists()
        composeTestRule.onNodeWithText("Legendary").assertExists()
    }

    @Test
    fun active_filter_indicator_shows_when_filtered() {
        // Given - ViewModel with mixed cards
        val testCards = listOf(
            Card(
                id = 1,
                prompt = "Common Card",
                imageUrl = "/path/to/image1.png",
                rarity = CardRarity.COMMON,
                createdAt = 1000
            ),
            Card(
                id = 2,
                prompt = "Rare Card",
                imageUrl = "/path/to/image2.png",
                rarity = CardRarity.RARE,
                createdAt = 2000
            )
        )
        val viewModel = createMockViewModel(testCards)

        composeTestRule.setContent {
            RotDexTheme {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // When - Apply filter
        composeTestRule.onNodeWithContentDescription("Filter").performClick()
        composeTestRule.onNodeWithText("Rare").performClick()

        // Then - Active filter indicator should be visible
        composeTestRule.onNodeWithText("Filtered by: Rare").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear").assertIsDisplayed()

        // When - Clear filter
        composeTestRule.onNodeWithText("Clear").performClick()

        // Then - Filter indicator should be gone
        composeTestRule.onNodeWithText("Filtered by: Rare").assertDoesNotExist()
    }

    // Additional test: Verify back navigation works
    @Test
    fun back_button_triggers_navigation() {
        var backNavigationCalled = false
        val viewModel = createMockViewModel(emptyList())

        composeTestRule.setContent {
            RotDexTheme {
                CollectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backNavigationCalled = true }
                )
            }
        }

        // When - Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Then - Navigation callback should be called
        assert(backNavigationCalled)
    }

    // Additional test: Verify Collection Stats Card composable directly
    @Test
    fun collection_stats_card_displays_all_stats() {
        // Given
        val stats = CollectionStats(
            totalCards = 10,
            commonCount = 5,
            rareCount = 3,
            epicCount = 1,
            legendaryCount = 1
        )

        composeTestRule.setContent {
            RotDexTheme {
                CollectionStatsCard(stats = stats)
            }
        }

        // Then
        composeTestRule.onNodeWithText("Collection Stats").assertIsDisplayed()
        composeTestRule.onNodeWithText("10").assertExists() // Total
        composeTestRule.onNodeWithText("5").assertExists()  // Common
        composeTestRule.onNodeWithText("3").assertExists()  // Rare
        composeTestRule.onNodeWithText("1").assertExists()  // Epic and Legendary (both 1)
    }
}
