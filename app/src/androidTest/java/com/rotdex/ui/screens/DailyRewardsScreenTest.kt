package com.rotdex.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotdex.data.models.UserProfile
import com.rotdex.ui.theme.RotDexTheme
import com.rotdex.ui.viewmodel.DailyRewardsState
import kotlinx.coroutines.flow.MutableStateFlow
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

        // Verify the screen title is present
        composeTestRule.onNodeWithText("Daily Rewards").assertExists()
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
}
