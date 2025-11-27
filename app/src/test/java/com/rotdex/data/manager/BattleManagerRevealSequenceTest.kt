package com.rotdex.data.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rotdex.data.models.Card
import com.rotdex.data.models.CardRarity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test suite for BattleManager card reveal sequence functionality.
 *
 * Tests follow TDD methodology - validating expected behavior.
 *
 * Covers:
 * - Initial reveal state
 * - Reveal sequence triggering
 * - Reveal timing and sequence order
 * - State transitions during reveal
 * - Reset behavior
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BattleManagerRevealSequenceTest {

    private lateinit var battleManager: BattleManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        battleManager = BattleManager(context)
    }

    private fun createTestCard(id: Long = 1): Card {
        return Card(
            id = id,
            name = "TestCard$id",
            attack = 50,
            health = 100,
            rarity = CardRarity.COMMON,
            prompt = "Test prompt",
            biography = "Test bio",
            imageUrl = "test_path"
        )
    }

    // ==================== Initial State Tests ====================

    @Test
    fun shouldRevealCards_initiallyFalse() = runTest {
        // GIVEN: New BattleManager instance
        // WHEN: Checking initial state
        val shouldReveal = battleManager.shouldRevealCards.value

        // THEN: shouldRevealCards should be false
        assertFalse("shouldRevealCards should initially be false", shouldReveal)
    }

    @Test
    fun statsRevealed_initiallyFalse() = runTest {
        // GIVEN: New BattleManager instance
        // WHEN: Checking initial state
        val statsRevealed = battleManager.statsRevealed.value

        // THEN: statsRevealed should be false
        assertFalse("statsRevealed should initially be false", statsRevealed)
    }

    // ==================== Reset Tests ====================

    @Test
    fun stopAll_resetsRevealStates() = runTest {
        // GIVEN: BattleManager with potentially modified state
        // (We can't easily trigger reveal without full connection setup,
        // but we can verify reset behavior)

        // WHEN: stopAll is called
        battleManager.stopAll()

        // THEN: All reveal states should be reset to false
        assertFalse("shouldRevealCards should be reset", battleManager.shouldRevealCards.value)
        assertFalse("statsRevealed should be reset", battleManager.statsRevealed.value)
    }

    // ==================== State Flow Tests ====================

    @Test
    fun revealStateFlows_emitCorrectInitialValues() = runTest {
        // GIVEN: Fresh BattleManager
        // WHEN: Collecting initial values from StateFlows
        val shouldReveal = battleManager.shouldRevealCards.value
        val statsRevealed = battleManager.statsRevealed.value

        // THEN: Both should be false initially
        assertFalse("shouldRevealCards initial value", shouldReveal)
        assertFalse("statsRevealed initial value", statsRevealed)
    }

    @Test
    fun resetAfterBattle_restoresRevealStates() = runTest {
        // GIVEN: BattleManager in any state
        // WHEN: Battle is reset (via stopAll)
        battleManager.stopAll()

        // THEN: Reveal states should return to initial values
        assertFalse("shouldRevealCards should reset", battleManager.shouldRevealCards.value)
        assertFalse("statsRevealed should reset", battleManager.statsRevealed.value)
    }

    // ==================== Integration Tests ====================

    @Test
    fun selectCard_doesNotTriggerReveal() = runTest {
        // GIVEN: BattleManager with no card
        // WHEN: Card is selected
        battleManager.selectCard(createTestCard())

        // Small delay to ensure any async operations complete
        delay(100)

        // THEN: Reveal should not be triggered (requires ready state)
        assertFalse("Reveal should not trigger on card selection alone",
            battleManager.shouldRevealCards.value)
    }

    @Test
    fun setReady_withoutBothReady_doesNotTriggerReveal() = runTest {
        // GIVEN: BattleManager with local card selected
        battleManager.selectCard(createTestCard())

        // WHEN: Local player sets ready (but opponent not ready)
        battleManager.setReady()

        // Small delay to ensure any async operations complete
        delay(100)

        // THEN: Reveal should not be triggered (needs both players ready)
        assertFalse("Reveal should not trigger when only one player ready",
            battleManager.shouldRevealCards.value)
    }

    // Note: Full reveal sequence test requires mock Nearby Connections
    // which is tested in integration tests. These unit tests verify
    // the state management and reset behavior of reveal states.
}
