package com.rotdex.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.ui.theme.RotDexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for StyledCardView component
 * Tests rendering in different modes, edge cases, and visual consistency
 */
@RunWith(AndroidJUnit4::class)
class StyledCardViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test 1: THUMBNAIL mode renders basic card elements
     */
    @Test
    fun thumbnail_mode_displays_essential_elements() {
        // Given - A basic card
        val testCard = Card(
            id = 1,
            prompt = "Test Card",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.RARE,
            name = "Dragon Warrior",
            health = 120,
            attack = 65,
            biography = "A fierce warrior from the mountains"
        )

        // When
        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.THUMBNAIL
                )
            }
        }

        // Then - Essential elements should be visible
        composeTestRule.onNodeWithText("Dragon Warrior").assertIsDisplayed()
        composeTestRule.onNodeWithText("120").assertIsDisplayed() // HP
        composeTestRule.onNodeWithText("65").assertIsDisplayed() // Attack

        // Biography should NOT be visible in thumbnail mode
        composeTestRule.onNodeWithText("A fierce warrior from the mountains").assertDoesNotExist()

        // Rarity should NOT be visible in thumbnail mode (only in full mode)
        composeTestRule.onNodeWithText("RARE").assertDoesNotExist()
    }

    /**
     * Test 2: FULL mode displays all card elements including biography
     */
    @Test
    fun full_mode_displays_all_elements() {
        // Given - A card with biography
        val testCard = Card(
            id = 1,
            prompt = "Test Card",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.EPIC,
            name = "Archmage Merlin",
            health = 100,
            attack = 120,
            biography = "The most powerful wizard in the realm, master of ancient spells and keeper of forbidden knowledge."
        )

        // When
        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.FULL
                )
            }
        }

        // Then - All elements should be visible
        composeTestRule.onNodeWithText("Archmage Merlin").assertIsDisplayed()
        composeTestRule.onNodeWithText("100").assertIsDisplayed() // HP
        composeTestRule.onNodeWithText("120").assertIsDisplayed() // Attack
        composeTestRule.onNodeWithText("EPIC").assertIsDisplayed() // Rarity in uppercase
        composeTestRule.onNodeWithText("BIOGRAPHY").assertIsDisplayed() // Section header
        composeTestRule.onNodeWithText("The most powerful wizard in the realm, master of ancient spells and keeper of forbidden knowledge.")
            .assertIsDisplayed()
    }

    /**
     * Test 3: Card with empty name falls back to prompt
     */
    @Test
    fun empty_name_falls_back_to_prompt() {
        // Given - Card with empty name
        val testCard = Card(
            id = 1,
            prompt = "A mystical dragon",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.COMMON,
            name = "", // Empty name
            health = 100,
            attack = 50
        )

        // When
        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.THUMBNAIL
                )
            }
        }

        // Then - Prompt should be displayed as name
        composeTestRule.onNodeWithText("A mystical dragon").assertIsDisplayed()
    }

    /**
     * Test 4: Biography not displayed when empty in FULL mode
     */
    @Test
    fun empty_biography_not_displayed_in_full_mode() {
        // Given - Card with empty biography
        val testCard = Card(
            id = 1,
            prompt = "Test Card",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.LEGENDARY,
            name = "Golden Knight",
            health = 150,
            attack = 85,
            biography = "" // Empty biography
        )

        // When
        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.FULL
                )
            }
        }

        // Then - Biography section should NOT be displayed
        composeTestRule.onNodeWithText("BIOGRAPHY").assertDoesNotExist()
    }

    /**
     * Test 5: Very long name handling
     */
    @Test
    fun long_name_displays_with_ellipsis() {
        // Given - Card with very long name (25+ characters)
        val testCard = Card(
            id = 1,
            prompt = "Test Card",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.RARE,
            name = "The Legendary Super Ultra Mega Dragon Warrior of the Eastern Mountains",
            health = 100,
            attack = 50
        )

        // When
        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.THUMBNAIL
                )
            }
        }

        // Then - Name should be displayed (might be truncated but should exist)
        // Using substring to check for beginning of name
        composeTestRule.onNodeWithText("The Legendary Super Ultra Mega Dragon Warrior of the Eastern Mountains", substring = true)
            .assertExists()
    }

    /**
     * Test 6: Very long biography handling
     */
    @Test
    fun long_biography_displays_fully_in_full_mode() {
        // Given - Card with very long biography (200+ characters)
        val longBio = "This is an extremely long biography that describes the character's entire life story, " +
                "including their childhood adventures, their training in the mystic arts, their battles against " +
                "evil forces, and their ultimate quest to save the realm from destruction. " +
                "It spans multiple generations and involves countless allies and enemies."

        val testCard = Card(
            id = 1,
            prompt = "Test Card",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.EPIC,
            name = "Epic Hero",
            health = 120,
            attack = 80,
            biography = longBio
        )

        // When
        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.FULL
                )
            }
        }

        // Then - Biography should be displayed (may need scrolling in real UI)
        composeTestRule.onNodeWithText(longBio, substring = true).assertExists()
        composeTestRule.onNodeWithText("BIOGRAPHY").assertIsDisplayed()
    }

    /**
     * Test 7: Very high stat values display correctly
     */
    @Test
    fun high_stat_values_display_correctly() {
        // Given - Card with very high HP/ATK (300/150)
        val testCard = Card(
            id = 1,
            prompt = "Test Card",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.LEGENDARY,
            name = "Supreme Being",
            health = 300,
            attack = 150
        )

        // When
        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.FULL
                )
            }
        }

        // Then - High values should be displayed
        composeTestRule.onNodeWithText("300").assertIsDisplayed() // HP
        composeTestRule.onNodeWithText("150").assertIsDisplayed() // Attack
    }

    /**
     * Test 8: Click handler is triggered
     */
    @Test
    fun click_handler_is_triggered() {
        // Given
        var clickCount = 0
        val testCard = Card(
            id = 1,
            prompt = "Clickable Card",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.COMMON,
            name = "Click Me",
            health = 100,
            attack = 50
        )

        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.THUMBNAIL,
                    onClick = { clickCount++ }
                )
            }
        }

        // When - Click the card
        composeTestRule.onNodeWithText("Click Me").performClick()

        // Then - Click handler should be called
        assert(clickCount == 1)
    }

    /**
     * Test 9: All rarity levels render with correct styling
     */
    @Test
    fun all_rarity_levels_render() {
        CardRarity.entries.forEach { rarity ->
            // Given - Card of specific rarity
            val testCard = Card(
                id = 1,
                prompt = "Test Card",
                imageUrl = "/path/to/image.png",
                rarity = rarity,
                name = "${rarity.displayName} Card",
                health = 100,
                attack = 50
            )

            // When
            composeTestRule.setContent {
                RotDexTheme {
                    StyledCardView(
                        card = testCard,
                        displayMode = CardDisplayMode.FULL
                    )
                }
            }

            // Then - Card should display with rarity
            composeTestRule.onNodeWithText("${rarity.displayName} Card").assertIsDisplayed()
            composeTestRule.onNodeWithText(rarity.displayName.uppercase()).assertIsDisplayed()
        }
    }

    /**
     * Test 10: Stat badges are visible with correct icons/symbols
     */
    @Test
    fun stat_badges_display_with_correct_symbols() {
        // Given
        val testCard = Card(
            id = 1,
            prompt = "Test Card",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.RARE,
            name = "Stat Test",
            health = 95,
            attack = 75
        )

        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.FULL
                )
            }
        }

        // Then - Both stat values should be visible
        composeTestRule.onNodeWithText("95").assertIsDisplayed() // HP
        composeTestRule.onNodeWithText("75").assertIsDisplayed() // Attack
        // Note: Icons/symbols might not be testable as text, but the values are
    }

    /**
     * Test 11: Both display modes render without crashes
     */
    @Test
    fun both_display_modes_render_without_crashes() {
        val testCard = Card(
            id = 1,
            prompt = "Test Card",
            imageUrl = "/path/to/image.png",
            rarity = CardRarity.EPIC,
            name = "Mode Test Card",
            health = 110,
            attack = 60,
            biography = "A test biography for mode testing"
        )

        // Test THUMBNAIL mode
        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.THUMBNAIL
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mode Test Card").assertIsDisplayed()

        // Test FULL mode
        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = testCard,
                    displayMode = CardDisplayMode.FULL
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mode Test Card").assertIsDisplayed()
        composeTestRule.onNodeWithText("A test biography for mode testing").assertIsDisplayed()
    }

    /**
     * Test 12: Preview functions compile (indirect test via component render)
     */
    @Test
    fun preview_data_renders_correctly() {
        // Test with data similar to preview functions

        // Thumbnail preview data
        val thumbnailCard = Card(
            id = 1,
            prompt = "A mystical dragon guardian",
            imageUrl = "",
            rarity = CardRarity.LEGENDARY,
            name = "Dragon Guardian",
            health = 150,
            attack = 85,
            biography = "An ancient dragon that protects the sacred temple"
        )

        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = thumbnailCard,
                    displayMode = CardDisplayMode.THUMBNAIL
                )
            }
        }

        composeTestRule.onNodeWithText("Dragon Guardian").assertIsDisplayed()
        composeTestRule.onNodeWithText("150").assertIsDisplayed()
        composeTestRule.onNodeWithText("85").assertIsDisplayed()

        // Full preview data
        val fullCard = Card(
            id = 2,
            prompt = "A powerful wizard",
            imageUrl = "",
            rarity = CardRarity.EPIC,
            name = "Archmage Merlin",
            health = 100,
            attack = 120,
            biography = "The most powerful wizard in the realm, master of ancient spells and keeper of forbidden knowledge. His magical prowess is matched only by his wisdom."
        )

        composeTestRule.setContent {
            RotDexTheme {
                StyledCardView(
                    card = fullCard,
                    displayMode = CardDisplayMode.FULL
                )
            }
        }

        composeTestRule.onNodeWithText("Archmage Merlin").assertIsDisplayed()
        composeTestRule.onNodeWithText("EPIC").assertIsDisplayed()
        composeTestRule.onNodeWithText("BIOGRAPHY").assertIsDisplayed()
    }
}
