package com.rotdex.ui

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotdex.ui.screens.CardCreateScreen
import com.rotdex.ui.screens.CollectionScreen
import com.rotdex.ui.screens.DailyRewardsScreen
import com.rotdex.ui.screens.FusionScreen
import com.rotdex.ui.screens.HomeScreen
import com.rotdex.ui.theme.RotDexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive runtime error detection tests for all screens.
 *
 * These tests catch runtime errors that compilation can't detect, including:
 * - NoSuchMethodError from library version incompatibilities
 * - NoClassDefFoundError from missing dependencies
 * - Animation API incompatibilities
 * - Compose library version mismatches
 *
 * HOW IT WORKS:
 * 1. Each test renders a complete screen
 * 2. Waits for all async operations to complete
 * 3. Advances animation time to trigger animated components
 * 4. If any runtime errors occur, the test fails immediately
 *
 * WHAT IT CATCHES:
 * - The Compose BOM 2024.01.00 animation incompatibility we just fixed
 * - Future library version conflicts
 * - Missing dependencies that compile but fail at runtime
 * - Component initialization errors
 */
@RunWith(AndroidJUnit4::class)
class ComposeRuntimeErrorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_rendersWithoutRuntimeErrors() {
        testScreenRendersWithoutErrors(
            screenName = "HomeScreen"
        ) {
            HomeScreen(
                onNavigateToDailyRewards = {},
                onNavigateToCollection = {},
                onNavigateToCardCreate = {},
                onNavigateToFusion = {}
            )
        }
    }

    @Test
    fun dailyRewardsScreen_rendersWithoutRuntimeErrors() {
        testScreenRendersWithoutErrors(
            screenName = "DailyRewardsScreen"
        ) {
            DailyRewardsScreen(onNavigateBack = {})
        }
    }

    @Test
    fun fusionScreen_rendersWithoutRuntimeErrors() {
        testScreenRendersWithoutErrors(
            screenName = "FusionScreen"
        ) {
            FusionScreen(onNavigateBack = {})
        }
    }

    @Test
    fun cardCreateScreen_rendersWithoutRuntimeErrors() {
        testScreenRendersWithoutErrors(
            screenName = "CardCreateScreen"
        ) {
            CardCreateScreen(
                onNavigateBack = {},
                onNavigateToCollection = {}
            )
        }
    }

    @Test
    fun collectionScreen_rendersWithoutRuntimeErrors() {
        testScreenRendersWithoutErrors(
            screenName = "CollectionScreen"
        ) {
            CollectionScreen(onNavigateBack = {})
        }
    }

    /**
     * Helper function to test screen rendering without runtime errors.
     *
     * This function:
     * 1. Sets the content with theme wrapper
     * 2. Waits for initial composition
     * 3. Advances animation clock to trigger all animations
     * 4. Catches any runtime exceptions that occur
     *
     * @param screenName Name of the screen for error messages
     * @param content Composable content to test
     */
    private fun testScreenRendersWithoutErrors(
        screenName: String,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        try {
            composeTestRule.setContent {
                RotDexTheme {
                    content()
                }
            }

            // Wait for initial composition to complete
            composeTestRule.waitForIdle()

            // Advance time to trigger animations and async operations
            // This would catch NoSuchMethodError from CircularProgressIndicator keyframes
            composeTestRule.mainClock.advanceTimeBy(3000L)
            composeTestRule.waitForIdle()

            // Advance time multiple times to ensure all animations run
            repeat(10) {
                composeTestRule.mainClock.advanceTimeBy(500L)
                composeTestRule.waitForIdle()
            }

            // If we reach here, no runtime errors occurred
            // Success!
        } catch (e: NoSuchMethodError) {
            throw AssertionError(
                "$screenName failed with NoSuchMethodError. " +
                "This usually indicates library version incompatibility. " +
                "Check Compose BOM version and animation library versions. " +
                "Error: ${e.message}",
                e
            )
        } catch (e: NoClassDefFoundError) {
            throw AssertionError(
                "$screenName failed with NoClassDefFoundError. " +
                "This usually indicates a missing dependency. " +
                "Error: ${e.message}",
                e
            )
        }
    }
}
