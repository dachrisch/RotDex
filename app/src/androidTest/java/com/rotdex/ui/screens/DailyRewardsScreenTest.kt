package com.rotdex.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotdex.ui.theme.RotDexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test for DailyRewardsScreen
 * Tests the UI rendering and interactions without requiring a full ViewModel
 */
@RunWith(AndroidJUnit4::class)
class DailyRewardsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dailyRewardsScreenRendersWithoutCrash() {
        // This test verifies the screen can render without throwing runtime errors
        // (e.g., NoSuchMethodError from missing Compose Animation dependencies)
        composeTestRule.setContent {
            RotDexTheme {
                DailyRewardsScreen(
                    onNavigateBack = {}
                )
            }
        }

        // Wait for initial composition to complete
        composeTestRule.waitForIdle()

        // Verify the screen title is present
        composeTestRule.onNodeWithText("Daily Rewards").assertExists()

        // Wait a bit longer to allow async operations (ViewModel initialization, etc.)
        // This ensures all UI components including animated ones are rendered
        composeTestRule.mainClock.advanceTimeBy(1000L)
        composeTestRule.waitForIdle()

        // The test will fail with NoSuchMethodError if:
        // - CircularProgressIndicator tries to use incompatible animation APIs
        // - Keyframes animation methods are missing
        // - Any other runtime linkage errors occur
    }

    @Test
    fun dailyRewardsScreenShowsMainSections() {
        composeTestRule.setContent {
            RotDexTheme {
                DailyRewardsScreen(
                    onNavigateBack = {}
                )
            }
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // Verify main sections are present (these appear after ViewModel loads data)
        // The screen should at least show the loading state or content without crashing
        composeTestRule.onNodeWithText("Daily Rewards").assertExists()
    }

    @Test
    fun dailyRewardsScreenBackButtonWorks() {
        var backPressed = false

        composeTestRule.setContent {
            RotDexTheme {
                DailyRewardsScreen(
                    onNavigateBack = { backPressed = true }
                )
            }
        }

        // Find and click the back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verify callback was triggered
        assert(backPressed)
    }

    @Test
    fun dailyRewardsScreenAnimationsRenderCorrectly() {
        // This test specifically verifies that animated components (CircularProgressIndicator)
        // can render without NoSuchMethodError from keyframes animations.
        // This catches Compose BOM version incompatibilities.
        composeTestRule.setContent {
            RotDexTheme {
                DailyRewardsScreen(
                    onNavigateBack = {}
                )
            }
        }

        // Wait for initial composition
        composeTestRule.waitForIdle()

        // Advance time to trigger animations
        // CircularProgressIndicator uses keyframes animations internally
        // If the animation library is incompatible, this will throw NoSuchMethodError
        composeTestRule.mainClock.advanceTimeBy(2000L)
        composeTestRule.waitForIdle()

        // Advance time multiple times to ensure animations are running
        repeat(5) {
            composeTestRule.mainClock.advanceTimeBy(500L)
            composeTestRule.waitForIdle()
        }

        // If we reach here without crash, animations are working correctly
        // with compatible Compose library versions
        composeTestRule.onNodeWithText("Daily Rewards").assertExists()
    }
}
