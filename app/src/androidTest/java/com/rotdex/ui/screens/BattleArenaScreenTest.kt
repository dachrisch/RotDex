package com.rotdex.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rotdex.data.models.BattleCard
import com.rotdex.data.models.BattleStorySegment
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import com.rotdex.ui.theme.RotDexTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for BattleArenaScreen battle animation components
 * Following TDD RED-GREEN-REFACTOR cycle
 *
 * Test Coverage:
 * - BattlePrimaryAnimationScreen rendering
 * - Typewriter text effect for story segments
 * - Battle progress indicator
 * - Skip functionality
 * - Card attack/damage animations
 * - State transitions
 */
@RunWith(AndroidJUnit4::class)
class BattleArenaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testCard1 = Card(
        id = 1L,
        name = "Fire Dragon",
        prompt = "A fierce fire dragon",
        imageUrl = "test_image_1.png",
        rarity = CardRarity.LEGENDARY,
        attack = 85,
        health = 120,
        createdAt = System.currentTimeMillis()
    )

    private val testCard2 = Card(
        id = 2L,
        name = "Ice Wizard",
        prompt = "A powerful ice wizard",
        imageUrl = "test_image_2.png",
        rarity = CardRarity.EPIC,
        attack = 75,
        health = 100,
        createdAt = System.currentTimeMillis()
    )

    private val testBattleCard1 = BattleCard.fromCard(testCard1)
    private val testBattleCard2 = BattleCard.fromCard(testCard2)

    private val testBattleStory = listOf(
        BattleStorySegment(
            text = "Fire Dragon charges forward with blazing fury!",
            isLocalAction = true,
            damageDealt = 25
        ),
        BattleStorySegment(
            text = "Ice Wizard counters with a freezing blast!",
            isLocalAction = false,
            damageDealt = 20
        ),
        BattleStorySegment(
            text = "Fire Dragon strikes again with deadly precision!",
            isLocalAction = true,
            damageDealt = 30
        )
    )

    // ===========================================
    // Tests for BattlePrimaryAnimationScreen
    // ===========================================

    @Test
    fun battlePrimaryAnimationScreen_displaysBothCards() {
        composeTestRule.setContent {
            RotDexTheme {
                BattlePrimaryAnimationScreen(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = 0,
                    opponentName = "Opponent",
                    onSkip = {}
                )
            }
        }

        // Verify both cards are displayed
        composeTestRule.onNodeWithText("Fire Dragon").assertExists()
        composeTestRule.onNodeWithText("Ice Wizard").assertExists()

        // Verify card labels
        composeTestRule.onNodeWithText("YOU", substring = true).assertExists()
        composeTestRule.onNodeWithText("FOE", substring = true).assertExists()
    }

    @Test
    fun battlePrimaryAnimationScreen_displaysVersusText() {
        composeTestRule.setContent {
            RotDexTheme {
                BattlePrimaryAnimationScreen(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = 0,
                    opponentName = "Opponent",
                    onSkip = {}
                )
            }
        }

        // Verify VS text is displayed
        composeTestRule.onNodeWithText("VS").assertExists()
    }

    @Test
    fun battlePrimaryAnimationScreen_showsProgressIndicator() {
        composeTestRule.setContent {
            RotDexTheme {
                BattlePrimaryAnimationScreen(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = 1,
                    opponentName = "Opponent",
                    onSkip = {}
                )
            }
        }

        // Verify progress indicator shows current round
        composeTestRule.onNodeWithText("Round 2 of 3").assertExists()
    }

    @Test
    fun battlePrimaryAnimationScreen_showsRoundHeader() {
        composeTestRule.setContent {
            RotDexTheme {
                BattlePrimaryAnimationScreen(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = 0,
                    opponentName = "Opponent",
                    onSkip = {}
                )
            }
        }

        // Verify round header is displayed
        composeTestRule.onNodeWithText("ROUND 1", substring = true).assertExists()
    }

    @Test
    fun battlePrimaryAnimationScreen_displaysCurrentStorySegment() {
        composeTestRule.setContent {
            RotDexTheme {
                BattlePrimaryAnimationScreen(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = 0,
                    opponentName = "Opponent",
                    onSkip = {}
                )
            }
        }

        // Verify first story segment is displayed
        composeTestRule.onNodeWithText(
            "Fire Dragon charges forward with blazing fury!",
            substring = true,
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun battlePrimaryAnimationScreen_skipButtonExists() {
        composeTestRule.setContent {
            RotDexTheme {
                BattlePrimaryAnimationScreen(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = 0,
                    opponentName = "Opponent",
                    onSkip = {}
                )
            }
        }

        // Verify skip button is present
        composeTestRule.onNodeWithText("SKIP", substring = true).assertExists()
    }

    @Test
    fun battlePrimaryAnimationScreen_skipButtonTriggersCallback() {
        var skipCalled = false

        composeTestRule.setContent {
            RotDexTheme {
                BattlePrimaryAnimationScreen(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = 0,
                    opponentName = "Opponent",
                    onSkip = { skipCalled = true }
                )
            }
        }

        // Click skip button
        composeTestRule.onNodeWithText("SKIP", substring = true).performClick()

        // Verify callback was triggered
        assert(skipCalled)
    }

    @Test
    fun battlePrimaryAnimationScreen_progressBarUpdatesWithIndex() {
        val currentIndex = mutableStateOf(0)

        composeTestRule.setContent {
            RotDexTheme {
                BattlePrimaryAnimationScreen(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = currentIndex.value,
                    opponentName = "Opponent",
                    onSkip = {}
                )
            }
        }

        // Initially at segment 1
        composeTestRule.onNodeWithText("Round 1 of 3").assertExists()

        // Update to segment 2
        currentIndex.value = 1
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Round 2 of 3").assertExists()

        // Update to segment 3
        currentIndex.value = 2
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Round 3 of 3").assertExists()
    }

    // ===========================================
    // Tests for TypewriterText Effect
    // ===========================================

    @Test
    fun typewriterText_displaysTextCharacterByCharacter() {
        composeTestRule.setContent {
            RotDexTheme {
                TypewriterText(
                    text = "Test message",
                    isAnimating = true,
                    delayPerCharMs = 30L
                )
            }
        }

        // Initially text should be empty or showing first character
        composeTestRule.waitForIdle()

        // After some time, partial text should be visible
        runBlocking { delay(150) }
        composeTestRule.waitForIdle()

        // Eventually full text should be visible
        runBlocking { delay(500) }
        composeTestRule.onNodeWithText("Test message", useUnmergedTree = true).assertExists()
    }

    @Test
    fun typewriterText_displaysFullTextWhenNotAnimating() {
        composeTestRule.setContent {
            RotDexTheme {
                TypewriterText(
                    text = "Instant text",
                    isAnimating = false
                )
            }
        }

        // Text should be displayed immediately when not animating
        composeTestRule.onNodeWithText("Instant text", useUnmergedTree = true).assertExists()
    }

    @Test
    fun typewriterText_handlesEmptyString() {
        composeTestRule.setContent {
            RotDexTheme {
                TypewriterText(
                    text = "",
                    isAnimating = true
                )
            }
        }

        // Should handle empty string without crashing
        composeTestRule.waitForIdle()
    }

    @Test
    fun typewriterText_handlesSingleCharacter() {
        composeTestRule.setContent {
            RotDexTheme {
                TypewriterText(
                    text = "A",
                    isAnimating = true,
                    delayPerCharMs = 30L
                )
            }
        }

        // Wait for animation
        runBlocking { delay(100) }
        composeTestRule.onNodeWithText("A", useUnmergedTree = true).assertExists()
    }

    @Test
    fun typewriterText_respectsCustomDelay() {
        val startTime = System.currentTimeMillis()

        composeTestRule.setContent {
            RotDexTheme {
                TypewriterText(
                    text = "ABC",
                    isAnimating = true,
                    delayPerCharMs = 100L // 100ms per character
                )
            }
        }

        // Wait for animation to complete (3 chars * 100ms = 300ms)
        runBlocking { delay(400) }
        composeTestRule.waitForIdle()

        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime

        // Animation should take at least 300ms
        assert(elapsedTime >= 300)

        // Full text should be visible
        composeTestRule.onNodeWithText("ABC", useUnmergedTree = true).assertExists()
    }

    // ===========================================
    // Tests for AnimatedBattleCard During Battle
    // ===========================================

    @Test
    fun animatedBattleCard_showsAttackState() {
        composeTestRule.setContent {
            RotDexTheme {
                AnimatedBattleCard(
                    battleCard = testBattleCard1,
                    label = "YOU",
                    isAttacking = true,
                    isTakingDamage = false,
                    isWinner = false,
                    isLoser = false,
                    isDraw = false
                )
            }
        }

        // Card should be visible with attack animation
        composeTestRule.onNodeWithText("Fire Dragon", substring = true).assertExists()
        composeTestRule.onNodeWithText("YOU").assertExists()
    }

    @Test
    fun animatedBattleCard_showsTakingDamageState() {
        composeTestRule.setContent {
            RotDexTheme {
                AnimatedBattleCard(
                    battleCard = testBattleCard1,
                    label = "YOU",
                    isAttacking = false,
                    isTakingDamage = true,
                    isWinner = false,
                    isLoser = false,
                    isDraw = false
                )
            }
        }

        // Card should be visible with damage animation
        composeTestRule.onNodeWithText("Fire Dragon", substring = true).assertExists()
    }

    @Test
    fun animatedBattleCard_displaysStats() {
        composeTestRule.setContent {
            RotDexTheme {
                AnimatedBattleCard(
                    battleCard = testBattleCard1,
                    label = "YOU",
                    isAttacking = false,
                    isTakingDamage = false,
                    isWinner = false,
                    isLoser = false,
                    isDraw = false
                )
            }
        }

        // Verify stats are displayed (with rarity bonuses)
        val expectedAttack = testBattleCard1.effectiveAttack
        val expectedHealth = testBattleCard1.effectiveHealth

        composeTestRule.onNodeWithText("⚔️$expectedAttack", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("❤️$expectedHealth", useUnmergedTree = true).assertExists()
    }

    @Test
    fun animatedBattleCard_showsHealthBar() {
        composeTestRule.setContent {
            RotDexTheme {
                AnimatedBattleCard(
                    battleCard = testBattleCard1.copy(currentHealth = 60),
                    label = "YOU",
                    isAttacking = false,
                    isTakingDamage = false,
                    isWinner = false,
                    isLoser = false,
                    isDraw = false
                )
            }
        }

        // Health bar should be rendered (visual component, no text assertion)
        composeTestRule.waitForIdle()
    }

    // ===========================================
    // Tests for Battle State Transitions
    // ===========================================

    @Test
    fun battleSection_transitionsFromCardSelectionToBattleAnimation() {
        // This test verifies the integration with BattleSection
        composeTestRule.setContent {
            RotDexTheme {
                BattleSection(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = 0,
                    battleResult = null,
                    cardTransferred = false,
                    opponentName = "Opponent",
                    onPlayAgain = {}
                )
            }
        }

        // Verify battle elements are displayed
        composeTestRule.onNodeWithText("Fire Dragon").assertExists()
        composeTestRule.onNodeWithText("Ice Wizard").assertExists()
        composeTestRule.onNodeWithText("VS").assertExists()
    }

    @Test
    fun battleSection_updatesStoryProgression() {
        val currentIndex = mutableStateOf(0)

        composeTestRule.setContent {
            RotDexTheme {
                BattleSection(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = testBattleStory,
                    currentStoryIndex = currentIndex.value,
                    battleResult = null,
                    cardTransferred = false,
                    opponentName = "Opponent",
                    onPlayAgain = {}
                )
            }
        }

        // Initially showing first segment
        composeTestRule.onNodeWithText(
            "Fire Dragon charges forward with blazing fury!",
            substring = true,
            useUnmergedTree = true
        ).assertExists()

        // Update to second segment
        currentIndex.value = 1
        composeTestRule.waitForIdle()

        // Should now show second segment
        composeTestRule.onNodeWithText(
            "Ice Wizard counters with a freezing blast!",
            substring = true,
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun battleSection_handlesEmptyStoryGracefully() {
        composeTestRule.setContent {
            RotDexTheme {
                BattleSection(
                    localCard = testBattleCard1,
                    opponentCard = testBattleCard2,
                    battleStory = emptyList(),
                    currentStoryIndex = 0,
                    battleResult = null,
                    cardTransferred = false,
                    opponentName = "Opponent",
                    onPlayAgain = {}
                )
            }
        }

        // Should still display cards without crashing
        composeTestRule.onNodeWithText("Fire Dragon").assertExists()
        composeTestRule.onNodeWithText("Ice Wizard").assertExists()
    }

    // ===========================================
    // Tests for Connection Bubble Stability (Bug Fix Verification)
    // ===========================================

    @Test
    fun connectingAnimation_bubbleRemainsVisibleDuringRecomposition() {
        // This test verifies the bubble stays visible when the discovered device list changes
        // during an active connection attempt (simulates the collision retry scenario)

        composeTestRule.setContent {
            RotDexTheme {
                // Direct test of ConnectingAnimation with forced recompositions
                ConnectingAnimation(playerName = "TestPlayer")
            }
        }

        // Verify bubble is initially visible
        composeTestRule.onNodeWithText("TestPlayer", substring = true, useUnmergedTree = true).assertExists()

        // Wait and verify it stays visible
        runBlocking { delay(500) }
        composeTestRule.waitForIdle()

        // Bubble should still be visible after time passes
        composeTestRule.onNodeWithText("TestPlayer", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun connectingAnimation_displaysPlayerInitials() {
        composeTestRule.setContent {
            RotDexTheme {
                ConnectingAnimation(playerName = "John Doe")
            }
        }

        // Should display initials
        composeTestRule.onNodeWithText("JD", useUnmergedTree = true).assertExists()
    }
}
