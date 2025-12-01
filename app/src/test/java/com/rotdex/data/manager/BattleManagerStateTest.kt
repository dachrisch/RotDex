package com.rotdex.data.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rotdex.data.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for BattleManager unified state updates (Phase 2)
 *
 * Tests verify:
 * - BattleSessionState is updated correctly by selectCard() and setReady()
 * - Version increments on every state change
 * - Legacy StateFlows are synced correctly (backward compatibility)
 * - Immutability is maintained (old state != new state)
 *
 * These tests ensure Phase 2 doesn't break existing functionality while
 * adding the unified state foundation for future phases.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BattleManagerStateTest {

    private lateinit var battleManager: BattleManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        battleManager = BattleManager(context)
    }

    @Test
    fun `initial state has version 0 and WAITING_FOR_OPPONENT phase`() {
        // Act
        val state = battleManager.battleSessionState.value

        // Assert
        assertEquals(0, state.version)
        assertEquals(BattlePhase.WAITING_FOR_OPPONENT, state.phase)
        assertFalse(state.localPlayer.hasSelectedCard)
        assertFalse(state.opponentPlayer.hasSelectedCard)
        assertNull(state.localPlayer.card)
        assertNull(state.opponentPlayer.card)
    }

    @Test
    fun `selectCard updates unified state correctly`() {
        // Arrange
        val testCard = createTestCard(id = 1, name = "Test Card", attack = 50, health = 100)
        val initialVersion = battleManager.battleSessionState.value.version

        // Act
        battleManager.selectCard(testCard)

        // Assert - Unified state updated
        val newState = battleManager.battleSessionState.value
        assertTrue(newState.localPlayer.hasSelectedCard)
        assertNotNull(newState.localPlayer.card)
        assertEquals("Test Card", newState.localPlayer.card?.card?.name)
        assertEquals(testCard, newState.localPlayer.fullCard)
        assertTrue(newState.canClickReady)

        // Assert - Version incremented
        assertEquals(initialVersion + 1, newState.version)
    }

    @Test
    fun `selectCard syncs to legacy StateFlows`() {
        // Arrange
        val testCard = createTestCard(id = 1, name = "Test Card", attack = 50, health = 100)

        // Act
        battleManager.selectCard(testCard)

        // Assert - Legacy StateFlows synchronized
        assertNotNull(battleManager.localCard.value)
        assertEquals("Test Card", battleManager.localCard.value?.card?.name)
        assertTrue(battleManager.canClickReady.value)
    }

    @Test
    fun `selectCard maintains immutability`() {
        // Arrange
        val testCard = createTestCard(id = 1, name = "Test Card")
        val oldState = battleManager.battleSessionState.value

        // Act
        battleManager.selectCard(testCard)

        // Assert - New state instance created
        val newState = battleManager.battleSessionState.value
        assertNotSame(oldState, newState)

        // Assert - Old state unchanged
        assertFalse(oldState.localPlayer.hasSelectedCard)
        assertNull(oldState.localPlayer.card)
    }

    @Test
    fun `setReady updates unified state correctly`() {
        // Arrange
        val testCard = createTestCard(id = 1, name = "Test Card")
        battleManager.selectCard(testCard)
        val versionAfterSelect = battleManager.battleSessionState.value.version

        // Act
        battleManager.setReady()

        // Assert - Unified state updated
        val newState = battleManager.battleSessionState.value
        assertTrue(newState.localPlayer.isReady)
        assertFalse(newState.canClickReady)  // Button disabled after click
        assertTrue(newState.waitingForOpponentReady)

        // Assert - Version incremented again
        assertEquals(versionAfterSelect + 1, newState.version)
    }

    @Test
    fun `setReady syncs to legacy StateFlows`() {
        // Arrange
        val testCard = createTestCard(id = 1, name = "Test Card")
        battleManager.selectCard(testCard)

        // Act
        battleManager.setReady()

        // Assert - Legacy StateFlows synchronized
        assertTrue(battleManager.localReady.value)
        assertFalse(battleManager.canClickReady.value)
        assertTrue(battleManager.waitingForOpponentReady.value)
    }

    @Test
    fun `multiple state updates increment version correctly`() {
        // Arrange
        val testCard = createTestCard(id = 1, name = "Test Card")
        val initialVersion = battleManager.battleSessionState.value.version

        // Act - Multiple state changes
        battleManager.selectCard(testCard)
        val v1 = battleManager.battleSessionState.value.version

        battleManager.setReady()
        val v2 = battleManager.battleSessionState.value.version

        // Assert - Version increments progressively
        assertEquals(initialVersion + 1, v1)
        assertEquals(initialVersion + 2, v2)
    }

    @Test
    fun `state updates maintain session ID consistency`() {
        // Arrange
        val testCard = createTestCard(id = 1, name = "Test Card")
        val originalSessionId = battleManager.battleSessionState.value.sessionId

        // Act
        battleManager.selectCard(testCard)
        battleManager.setReady()

        // Assert - Session ID never changes
        assertEquals(originalSessionId, battleManager.battleSessionState.value.sessionId)
    }

    @Test
    fun `canClickReady follows correct state transition`() {
        // Arrange
        val testCard = createTestCard(id = 1, name = "Test Card")

        // Initial state - ready button disabled
        assertFalse(battleManager.battleSessionState.value.canClickReady)

        // After card selection - ready button enabled
        battleManager.selectCard(testCard)
        assertTrue(battleManager.battleSessionState.value.canClickReady)

        // After clicking ready - ready button disabled
        battleManager.setReady()
        assertFalse(battleManager.battleSessionState.value.canClickReady)
    }

    @Test
    fun `fullCard is stored in unified state`() {
        // Arrange
        val testCard = createTestCard(
            id = 1,
            name = "Full Card Test",
            attack = 75,
            health = 150,
            prompt = "Test prompt",
            biography = "Test bio"
        )

        // Act
        battleManager.selectCard(testCard)

        // Assert
        val storedCard = battleManager.battleSessionState.value.localPlayer.fullCard
        assertNotNull(storedCard)
        assertEquals(testCard.id, storedCard?.id)
        assertEquals(testCard.name, storedCard?.name)
        assertEquals(testCard.prompt, storedCard?.prompt)
        assertEquals(testCard.biography, storedCard?.biography)
    }

    @Test
    fun `BattleCard stats are calculated correctly in state`() {
        // Arrange
        val testCard = createTestCard(
            id = 1,
            name = "Rare Card",
            attack = 50,
            health = 100,
            rarity = CardRarity.RARE
        )

        // Act
        battleManager.selectCard(testCard)

        // Assert - Rare bonus: +5% attack, +5 health
        val battleCard = battleManager.battleSessionState.value.localPlayer.card
        assertNotNull(battleCard)
        assertEquals(52, battleCard?.effectiveAttack)  // 50 * 1.05 = 52
        assertEquals(105, battleCard?.effectiveHealth)  // 100 + 5 = 105
    }

    @Test
    fun `state version survives multiple operations`() {
        // Arrange
        val card1 = createTestCard(id = 1, name = "Card 1")
        val card2 = createTestCard(id = 2, name = "Card 2")

        // Act - Simulate realistic flow
        battleManager.selectCard(card1)
        val v1 = battleManager.battleSessionState.value.version

        battleManager.setReady()
        val v2 = battleManager.battleSessionState.value.version

        // Simulate reset and new selection
        battleManager.stopAll()
        val v3 = battleManager.battleSessionState.value.version

        // Assert - Version keeps incrementing or resets to 0
        // (Implementation detail: stopAll() creates new state with version 0)
        assertTrue(v1 > 0)
        assertTrue(v2 > v1)
        // After stopAll, new state created
        assertEquals(0, v3)
    }

    // Helper function to create test cards
    private fun createTestCard(
        id: Long,
        name: String,
        attack: Int = 50,
        health: Int = 100,
        rarity: CardRarity = CardRarity.COMMON,
        prompt: String = "Test prompt",
        biography: String = "Test biography"
    ): Card {
        return Card(
            id = id,
            name = name,
            imageUrl = "/test/image.jpg",
            rarity = rarity,
            prompt = prompt,
            biography = biography,
            attack = attack,
            health = health,
            createdAt = System.currentTimeMillis()
        )
    }
}
