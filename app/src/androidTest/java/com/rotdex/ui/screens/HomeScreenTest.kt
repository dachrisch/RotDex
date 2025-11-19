package com.rotdex.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotdex.ui.theme.RotDexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test for HomeScreen
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreenRendersWithoutCrash() {
        composeTestRule.setContent {
            RotDexTheme {
                HomeScreen(
                    onNavigateToDailyRewards = {},
                    onNavigateToCollection = {},
                    onNavigateToCardCreate = {},
                    onNavigateToFusion = {}
                )
            }
        }

        // Verify main UI elements are present
        composeTestRule.onNodeWithText("🧠 RotDex").assertExists()
        composeTestRule.onNodeWithText("Collect the Chaos").assertExists()
    }

    @Test
    fun homeScreenShowsNavigationButtons() {
        composeTestRule.setContent {
            RotDexTheme {
                HomeScreen(
                    onNavigateToDailyRewards = {},
                    onNavigateToCollection = {},
                    onNavigateToCardCreate = {},
                    onNavigateToFusion = {}
                )
            }
        }

        // Verify all navigation buttons are present
        composeTestRule.onNodeWithText("Daily Rewards").assertExists()
        composeTestRule.onNodeWithText("Card Collection").assertExists()
        composeTestRule.onNodeWithText("Create Card").assertExists()
        composeTestRule.onNodeWithText("Card Fusion").assertExists()
    }

    @Test
    fun homeScreenNavigationButtonsAreClickable() {
        var dailyRewardsClicked = false
        var collectionClicked = false
        var createCardClicked = false
        var fusionClicked = false

        composeTestRule.setContent {
            RotDexTheme {
                HomeScreen(
                    onNavigateToDailyRewards = { dailyRewardsClicked = true },
                    onNavigateToCollection = { collectionClicked = true },
                    onNavigateToCardCreate = { createCardClicked = true },
                    onNavigateToFusion = { fusionClicked = true }
                )
            }
        }

        // Click each navigation button
        composeTestRule.onNodeWithText("Daily Rewards").performClick()
        assert(dailyRewardsClicked)

        composeTestRule.onNodeWithText("Card Collection").performClick()
        assert(collectionClicked)

        composeTestRule.onNodeWithText("Create Card").performClick()
        assert(createCardClicked)

        composeTestRule.onNodeWithText("Card Fusion").performClick()
        assert(fusionClicked)
    }
}
