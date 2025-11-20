package com.rotdex.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotdex.ui.theme.RotDexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test for FusionScreen
 */
@RunWith(AndroidJUnit4::class)
class FusionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fusionScreenRendersWithoutCrash() {
        composeTestRule.setContent {
            RotDexTheme {
                FusionScreen(
                    onNavigateBack = {},
                    onNavigateToCollection = {}
                )
            }
        }

        // Verify the screen renders with title
        composeTestRule.onNodeWithText("Card Fusion ⚗️").assertExists()
    }

    @Test
    fun fusionScreenShowsMainUI() {
        composeTestRule.setContent {
            RotDexTheme {
                FusionScreen(
                    onNavigateBack = {},
                    onNavigateToCollection = {}
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Verify main UI elements are present
        composeTestRule.onNodeWithText("Card Fusion ⚗️").assertExists()

        // The screen should show either the card selection UI or empty state
        // without crashing due to animation or other runtime errors
    }

    @Test
    fun fusionScreenBackButtonWorks() {
        var backPressed = false

        composeTestRule.setContent {
            RotDexTheme {
                FusionScreen(
                    onNavigateBack = { backPressed = true },
                    onNavigateToCollection = {}
                )
            }
        }

        // Find and click the back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verify callback was triggered
        assert(backPressed)
    }
}
