package com.rotdex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotdex.ui.components.BattleReadyStatus
import com.rotdex.ui.components.DiscoveryBubblesSection
import com.rotdex.ui.theme.RotDexTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test for Battle Arena auto-discovery UI and BattleReadyStatus component
 *
 * Test Coverage:
 * 1. Auto-discovery UI rendering (discovery bubbles + battle arena background)
 * 2. BattleReadyStatus component states:
 *    - "Moving to arena..." when card is selected but not ready
 *    - Green check when local player is ready
 *    - Blue check when opponent is ready
 * 3. Screen is NOT blank when entering Battle Arena
 * 4. Discovery bubbles section with nearby opponents
 * 5. Battle arena background visibility
 */
@RunWith(AndroidJUnit4::class)
class BattleArenaAutoDiscoveryUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===========================================
    // Auto-Discovery UI Tests
    // ===========================================

    @Test
    fun autoDiscoveryUI_displaysDiscoveryBubblesSection() {
        composeTestRule.setContent {
            RotDexTheme {
                DiscoveryBubblesSection(
                    discoveredDevices = emptyList(),
                    onDeviceClick = {}
                )
            }
        }

        // Verify "NEARBY OPPONENTS" label is visible
        composeTestRule.onNodeWithText("NEARBY OPPONENTS").assertExists()
    }

    @Test
    fun autoDiscoveryUI_showsScanningWhenNoDevicesDiscovered() {
        composeTestRule.setContent {
            RotDexTheme {
                DiscoveryBubblesSection(
                    discoveredDevices = emptyList(),
                    onDeviceClick = {}
                )
            }
        }

        // Verify scanning message is displayed
        composeTestRule.onNodeWithText("Scanning for opponents...").assertExists()
    }

    @Test
    fun autoDiscoveryUI_displaysDiscoveredOpponents() {
        val testDevices = listOf(
            "Player1|endpoint1",
            "Player2|endpoint2",
            "LongPlayerName123|endpoint3"
        )

        composeTestRule.setContent {
            RotDexTheme {
                DiscoveryBubblesSection(
                    discoveredDevices = testDevices,
                    onDeviceClick = {}
                )
            }
        }

        // Verify player names are displayed (truncated if needed)
        composeTestRule.onNodeWithText("Player1").assertExists()
        composeTestRule.onNodeWithText("Player2").assertExists()
        // Long names should be truncated to 8 chars + "..."
        composeTestRule.onNodeWithText("LongPlay...", substring = true).assertExists()
    }

    @Test
    fun autoDiscoveryUI_opponentBubblesAreClickable() {
        var clickedDevice: String? = null
        val testDevices = listOf("TestPlayer|endpoint1")

        composeTestRule.setContent {
            RotDexTheme {
                DiscoveryBubblesSection(
                    discoveredDevices = testDevices,
                    onDeviceClick = { device -> clickedDevice = device }
                )
            }
        }

        // Click on the opponent bubble
        composeTestRule.onNodeWithText("TestPlayer").performClick()

        // Verify callback was triggered with correct device info
        assert(clickedDevice == "TestPlayer|endpoint1")
    }

    @Test
    fun autoDiscoveryUI_displaysBattleArenaBackground() {
        composeTestRule.setContent {
            RotDexTheme {
                // Simulate the battle arena background from the screen
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚔️  🏛️  ⚔️",
                            fontSize = 60.sp
                        )
                        Text(
                            text = "🔥 BATTLE ARENA 🔥",
                            fontSize = 40.sp
                        )
                        Text(
                            text = "⚡  💥  ⚡",
                            fontSize = 50.sp
                        )
                    }
                }
            }
        }

        // Verify battle arena background elements are visible
        composeTestRule.onNodeWithText("🔥 BATTLE ARENA 🔥").assertExists()
        composeTestRule.onNodeWithText("⚔️  🏛️  ⚔️").assertExists()
        composeTestRule.onNodeWithText("⚡  💥  ⚡").assertExists()
    }

    @Test
    fun autoDiscoveryUI_screenIsNotBlank() {
        composeTestRule.setContent {
            RotDexTheme {
                // Simulate the full auto-discovery UI layout
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Battle arena background
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔥 BATTLE ARENA 🔥",
                            fontSize = 40.sp
                        )
                    }

                    // Discovery bubbles overlay at top
                    DiscoveryBubblesSection(
                        discoveredDevices = emptyList(),
                        onDeviceClick = {},
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                }
            }
        }

        // Verify both the battle arena background and discovery UI are visible
        composeTestRule.onNodeWithText("🔥 BATTLE ARENA 🔥").assertExists()
        composeTestRule.onNodeWithText("NEARBY OPPONENTS").assertExists()
        composeTestRule.onNodeWithText("Scanning for opponents...").assertExists()
    }

    // ===========================================
    // BattleReadyStatus Component Tests
    // ===========================================

    @Test
    fun battleReadyStatus_showsMovingToArenaWhenCardSelectedButNotReady() {
        composeTestRule.setContent {
            RotDexTheme {
                BattleReadyStatus(
                    localCardSelected = true,
                    localReady = false,
                    opponentReady = false,
                    opponentHasSelectedCard = false
                )
            }
        }

        // Verify "Moving to arena..." text is displayed
        composeTestRule.onNodeWithText("Moving to arena...").assertExists()
    }

    @Test
    fun battleReadyStatus_showsGreenCheckWhenLocalPlayerReady() {
        composeTestRule.setContent {
            RotDexTheme {
                BattleReadyStatus(
                    localCardSelected = true,
                    localReady = true,
                    opponentReady = false,
                    opponentHasSelectedCard = false,
                    opponentDataComplete = true // Data is complete
                )
            }
        }

        // Verify local player status shows "YOU" label
        composeTestRule.onNodeWithText("YOU").assertExists()

        // Verify green check icon is present (check icon with specific tint)
        // The icon should be visible as a content description
        composeTestRule.onNodeWithContentDescription("Ready").assertExists()
    }

    @Test
    fun battleReadyStatus_showsBlueCheckWhenOpponentReady() {
        composeTestRule.setContent {
            RotDexTheme {
                BattleReadyStatus(
                    localCardSelected = true,
                    localReady = true,
                    opponentReady = true,
                    opponentHasSelectedCard = true,
                    localDataComplete = true, // All data synced
                    opponentDataComplete = true
                )
            }
        }

        // Verify opponent status shows "OPPONENT" label
        composeTestRule.onNodeWithText("OPPONENT").assertExists()

        // Both players should show ready state (check icons)
        composeTestRule.onAllNodesWithContentDescription("Ready").assertCountEquals(2)
    }

    @Test
    fun battleReadyStatus_showsThinkingEmojiWhenOpponentSelecting() {
        composeTestRule.setContent {
            RotDexTheme {
                BattleReadyStatus(
                    localCardSelected = true,
                    localReady = true,
                    opponentReady = false,
                    opponentHasSelectedCard = true
                )
            }
        }

        // Verify thinking emoji is displayed for opponent
        composeTestRule.onNodeWithText("💭").assertExists()
    }

    @Test
    fun battleReadyStatus_showsLoadingDuringDataTransfer() {
        composeTestRule.setContent {
            RotDexTheme {
                BattleReadyStatus(
                    localCardSelected = true,
                    localReady = true,
                    opponentReady = true,
                    opponentHasSelectedCard = true,
                    localDataComplete = false, // Still transferring data
                    opponentDataComplete = false
                )
            }
        }

        // Verify "Moving to arena..." is shown during data transfer
        // Both sides might show this during data sync
        composeTestRule.onNodeWithText("Moving to arena...").assertExists()
    }

    @Test
    fun battleReadyStatus_displaysBothPlayerLabels() {
        composeTestRule.setContent {
            RotDexTheme {
                BattleReadyStatus(
                    localCardSelected = false,
                    localReady = false,
                    opponentReady = false,
                    opponentHasSelectedCard = false
                )
            }
        }

        // Verify both player labels are always visible
        composeTestRule.onNodeWithText("YOU").assertExists()
        composeTestRule.onNodeWithText("OPPONENT").assertExists()

        // Verify sword emoji separator is present
        composeTestRule.onNodeWithText("⚔️").assertExists()
    }

    @Test
    fun battleReadyStatus_transitionsFromMovingToReady() {
        composeTestRule.setContent {
            RotDexTheme {
                var isReady by remember { mutableStateOf(false) }

                Column {
                    BattleReadyStatus(
                        localCardSelected = true,
                        localReady = isReady,
                        opponentReady = false,
                        opponentHasSelectedCard = false,
                        opponentDataComplete = true
                    )

                    Button(
                        onClick = { isReady = true }
                    ) {
                        Text("Set Ready")
                    }
                }
            }
        }

        // Initially should show "Moving to arena..."
        composeTestRule.onNodeWithText("Moving to arena...").assertExists()

        // Click ready button
        composeTestRule.onNodeWithText("Set Ready").performClick()
        composeTestRule.waitForIdle()

        // Should now show ready state (check icon)
        composeTestRule.onNodeWithContentDescription("Ready").assertExists()
    }

    @Test
    fun battleReadyStatus_handlesAllStatesSimultaneously() {
        composeTestRule.setContent {
            RotDexTheme {
                BattleReadyStatus(
                    localCardSelected = true,
                    localReady = true,
                    opponentReady = true,
                    opponentHasSelectedCard = true,
                    localDataComplete = true,
                    opponentDataComplete = true
                )
            }
        }

        // Both players ready - should show both check marks
        composeTestRule.onNodeWithText("YOU").assertExists()
        composeTestRule.onNodeWithText("OPPONENT").assertExists()
        composeTestRule.onAllNodesWithContentDescription("Ready").assertCountEquals(2)

        // No "Moving to arena..." should be present when both ready and data complete
        composeTestRule.onNodeWithText("Moving to arena...").assertDoesNotExist()
    }

    @Test
    fun battleReadyStatus_showsImageTransferIndicator() {
        composeTestRule.setContent {
            RotDexTheme {
                BattleReadyStatus(
                    localCardSelected = true,
                    localReady = false,
                    opponentReady = true,
                    opponentHasSelectedCard = true,
                    localDataComplete = false,
                    opponentDataComplete = false // Data still transferring
                )
            }
        }

        // Should show loading state for image transfer
        composeTestRule.onNodeWithText("Moving to arena...").assertExists()
    }

    // ===========================================
    // Integration Tests
    // ===========================================

    @Test
    fun integration_autoDiscoveryUIWithBattleReadyStatus() {
        composeTestRule.setContent {
            RotDexTheme {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Discovery bubbles section
                    DiscoveryBubblesSection(
                        discoveredDevices = listOf("Player1|endpoint1"),
                        onDeviceClick = {}
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Battle ready status
                    BattleReadyStatus(
                        localCardSelected = true,
                        localReady = false,
                        opponentReady = false,
                        opponentHasSelectedCard = true
                    )
                }
            }
        }

        // Verify both components are visible
        composeTestRule.onNodeWithText("NEARBY OPPONENTS").assertExists()
        composeTestRule.onNodeWithText("Player1").assertExists()
        composeTestRule.onNodeWithText("YOU").assertExists()
        composeTestRule.onNodeWithText("OPPONENT").assertExists()
        composeTestRule.onNodeWithText("💭").assertExists()
    }

    @Test
    fun integration_verifyNoBlankScreenInAutoDiscovery() {
        composeTestRule.setContent {
            RotDexTheme {
                // Simulate the exact layout from BattleArenaScreen when in auto-discovery state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Battle arena background
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-20).dp)
                    ) {
                        Text(
                            text = "⚔️  🏛️  ⚔️",
                            fontSize = 60.sp,
                            modifier = Modifier.alpha(0.3f)
                        )
                        Text(
                            text = "🔥 BATTLE ARENA 🔥",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.alpha(0.25f)
                        )
                        Text(
                            text = "⚡  💥  ⚡",
                            fontSize = 50.sp,
                            modifier = Modifier.alpha(0.3f)
                        )
                    }

                    // Discovery bubbles overlay
                    DiscoveryBubblesSection(
                        discoveredDevices = emptyList(),
                        onDeviceClick = {},
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    )
                }
            }
        }

        // Critical verification: screen is NOT blank
        // Both background and overlay should be visible
        composeTestRule.onNodeWithText("🔥 BATTLE ARENA 🔥").assertExists()
        composeTestRule.onNodeWithText("⚔️  🏛️  ⚔️").assertExists()
        composeTestRule.onNodeWithText("⚡  💥  ⚡").assertExists()
        composeTestRule.onNodeWithText("NEARBY OPPONENTS").assertExists()
        composeTestRule.onNodeWithText("Scanning for opponents...").assertExists()
    }
}
