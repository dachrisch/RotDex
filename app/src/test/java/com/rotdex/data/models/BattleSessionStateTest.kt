package com.rotdex.data.models

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for BattleSessionState - single source of truth for battle state
 *
 * Tests the unified state model that replaces 13 separate StateFlows in BattleManager.
 * Tests verify:
 * - State transitions follow correct phase progression
 * - Version increments on every state change
 * - Immutability (copy creates new instances)
 * - Default initialization provides sensible values
 * - Player state updates work correctly
 */
class BattleSessionStateTest {

    @Test
    fun `default initialization creates WAITING_FOR_OPPONENT state`() {
        // Arrange & Act
        val state = BattleSessionState()

        // Assert
        assertEquals(BattlePhase.WAITING_FOR_OPPONENT, state.phase)
        assertEquals(0, state.version)
        assertNotNull(state.sessionId)
        assertFalse(state.localPlayer.hasSelectedCard)
        assertFalse(state.opponentPlayer.hasSelectedCard)
        assertNull(state.localPlayer.card)
        assertNull(state.opponentPlayer.card)
        assertFalse(state.canClickReady)
        assertFalse(state.waitingForOpponentReady)
    }

    @Test
    fun `nextVersion increments version number`() {
        // Arrange
        val state = BattleSessionState(version = 5)

        // Act
        val newState = state.nextVersion()

        // Assert
        assertEquals(6, newState.version)
        assertEquals(5, state.version) // Original unchanged
    }

    @Test
    fun `transition from WAITING_FOR_OPPONENT to CARD_SELECTION`() {
        // Arrange
        val state = BattleSessionState(phase = BattlePhase.WAITING_FOR_OPPONENT)

        // Act
        val newState = state.copy(phase = BattlePhase.CARD_SELECTION).nextVersion()

        // Assert
        assertEquals(BattlePhase.CARD_SELECTION, newState.phase)
        assertEquals(1, newState.version)
    }

    @Test
    fun `local player selects card updates state correctly`() {
        // Arrange
        val state = BattleSessionState(phase = BattlePhase.CARD_SELECTION)
        val testCard = createTestBattleCard(id = 1, name = "Test Card")

        // Act
        val newState = state.copy(
            localPlayer = state.localPlayer.copy(
                hasSelectedCard = true,
                card = testCard
            )
        ).nextVersion()

        // Assert
        assertTrue(newState.localPlayer.hasSelectedCard)
        assertEquals(testCard, newState.localPlayer.card)
        assertEquals(1, newState.version)
        assertFalse(state.localPlayer.hasSelectedCard) // Original unchanged
    }

    @Test
    fun `opponent player selects card updates state correctly`() {
        // Arrange
        val state = BattleSessionState(phase = BattlePhase.CARD_SELECTION)
        val testCard = createTestBattleCard(id = 2, name = "Opponent Card")

        // Act
        val newState = state.copy(
            opponentPlayer = state.opponentPlayer.copy(
                hasSelectedCard = true,
                card = testCard
            )
        ).nextVersion()

        // Assert
        assertTrue(newState.opponentPlayer.hasSelectedCard)
        assertEquals(testCard, newState.opponentPlayer.card)
        assertEquals(1, newState.version)
    }

    @Test
    fun `both players select cards increments version twice`() {
        // Arrange
        val state = BattleSessionState(phase = BattlePhase.CARD_SELECTION)
        val localCard = createTestBattleCard(id = 1, name = "Local Card")
        val opponentCard = createTestBattleCard(id = 2, name = "Opponent Card")

        // Act - Simulate two sequential updates
        val afterLocalSelect = state.copy(
            localPlayer = state.localPlayer.copy(
                hasSelectedCard = true,
                card = localCard
            )
        ).nextVersion()

        val afterBothSelect = afterLocalSelect.copy(
            opponentPlayer = afterLocalSelect.opponentPlayer.copy(
                hasSelectedCard = true,
                card = opponentCard
            )
        ).nextVersion()

        // Assert
        assertEquals(2, afterBothSelect.version)
        assertTrue(afterBothSelect.localPlayer.hasSelectedCard)
        assertTrue(afterBothSelect.opponentPlayer.hasSelectedCard)
    }

    @Test
    fun `local player ready updates state`() {
        // Arrange
        val state = BattleSessionState(
            phase = BattlePhase.CARD_SELECTION,
            localPlayer = PlayerState(
                hasSelectedCard = true,
                card = createTestBattleCard(id = 1, name = "Local Card")
            )
        )

        // Act
        val newState = state.copy(
            localPlayer = state.localPlayer.copy(isReady = true)
        ).nextVersion()

        // Assert
        assertTrue(newState.localPlayer.isReady)
        assertEquals(1, newState.version)
    }

    @Test
    fun `both players ready transitions to READY_SYNC phase`() {
        // Arrange
        val state = BattleSessionState(
            phase = BattlePhase.CARD_SELECTION,
            localPlayer = PlayerState(
                hasSelectedCard = true,
                card = createTestBattleCard(id = 1, name = "Local Card"),
                isReady = true
            ),
            opponentPlayer = PlayerState(
                hasSelectedCard = true,
                card = createTestBattleCard(id = 2, name = "Opponent Card")
            )
        )

        // Act - Opponent becomes ready
        val afterOpponentReady = state.copy(
            opponentPlayer = state.opponentPlayer.copy(isReady = true)
        ).nextVersion()

        // Act - Transition to READY_SYNC
        val readyToReveal = afterOpponentReady.copy(
            phase = BattlePhase.READY_SYNC
        ).nextVersion()

        // Assert
        assertEquals(BattlePhase.READY_SYNC, readyToReveal.phase)
        assertTrue(readyToReveal.localPlayer.isReady)
        assertTrue(readyToReveal.opponentPlayer.isReady)
        assertEquals(2, readyToReveal.version)
    }

    @Test
    fun `reveal sequence progresses through phases`() {
        // Arrange
        val state = BattleSessionState(phase = BattlePhase.READY_SYNC)

        // Act & Assert - REVEALING phase
        val revealing = state.copy(
            phase = BattlePhase.REVEALING,
            reveal = RevealState(initiatedBy = "host")
        ).nextVersion()

        assertEquals(BattlePhase.REVEALING, revealing.phase)
        assertNotNull(revealing.reveal)
        assertEquals("host", revealing.reveal?.initiatedBy)
        assertEquals(1, revealing.version)

        // Act & Assert - BATTLE_ANIMATING phase
        val animating = revealing.copy(
            phase = BattlePhase.BATTLE_ANIMATING,
            reveal = revealing.reveal?.copy(
                cardsRevealed = true,
                statsRevealed = true
            )
        ).nextVersion()

        assertEquals(BattlePhase.BATTLE_ANIMATING, animating.phase)
        assertTrue(animating.reveal?.cardsRevealed == true)
        assertTrue(animating.reveal?.statsRevealed == true)
        assertEquals(2, animating.version)

        // Act & Assert - BATTLE_COMPLETE phase
        val complete = animating.copy(
            phase = BattlePhase.BATTLE_COMPLETE,
            battle = BattleExecutionState(
                storySegments = listOf(
                    BattleStorySegment("Test battle", isLocalAction = true, damageDealt = 10)
                ),
                result = BattleResult(
                    isDraw = false,
                    winnerIsLocal = true,
                    winnerCardName = "Winner",
                    loserCardName = "Loser",
                    localCardFinalHealth = 50,
                    opponentCardFinalHealth = 0,
                    battleStory = "Test battle",
                    cardWon = null
                )
            )
        ).nextVersion()

        assertEquals(BattlePhase.BATTLE_COMPLETE, complete.phase)
        assertNotNull(complete.battle)
        assertEquals(1, complete.battle?.storySegments?.size)
        assertNotNull(complete.battle?.result)
        assertEquals(3, complete.version)
    }

    @Test
    fun `disconnection transitions to DISCONNECTED phase`() {
        // Arrange
        val state = BattleSessionState(phase = BattlePhase.CARD_SELECTION)

        // Act
        val disconnected = state.copy(phase = BattlePhase.DISCONNECTED).nextVersion()

        // Assert
        assertEquals(BattlePhase.DISCONNECTED, disconnected.phase)
        assertEquals(1, disconnected.version)
    }

    @Test
    fun `image transfer completion updates player state`() {
        // Arrange
        val state = BattleSessionState(
            phase = BattlePhase.CARD_SELECTION,
            opponentPlayer = PlayerState(
                hasSelectedCard = true,
                card = createTestBattleCard(id = 1, name = "Test Card")
            )
        )

        // Act
        val newState = state.copy(
            opponentPlayer = state.opponentPlayer.copy(
                imageTransferStatus = ImageTransferStatus.COMPLETE
            )
        ).nextVersion()

        // Assert
        assertTrue(newState.opponentPlayer.imageTransferComplete)
        assertEquals(1, newState.version)
    }

    @Test
    fun `data received from opponent flag updates correctly`() {
        // Arrange
        val state = BattleSessionState(phase = BattlePhase.CARD_SELECTION)

        // Act
        val newState = state.copy(
            opponentPlayer = state.opponentPlayer.copy(
                dataReceivedFromOpponent = true
            )
        ).nextVersion()

        // Assert
        assertTrue(newState.opponentPlayer.dataReceivedFromOpponent)
        assertEquals(1, newState.version)
    }

    @Test
    fun `canClickReady and waitingForOpponentReady update correctly`() {
        // Arrange
        val state = BattleSessionState(phase = BattlePhase.CARD_SELECTION)

        // Act - Enable ready button
        val readyEnabled = state.copy(canClickReady = true).nextVersion()

        // Assert
        assertTrue(readyEnabled.canClickReady)
        assertEquals(1, readyEnabled.version)

        // Act - Click ready, start waiting
        val waiting = readyEnabled.copy(
            waitingForOpponentReady = true,
            canClickReady = false
        ).nextVersion()

        // Assert
        assertTrue(waiting.waitingForOpponentReady)
        assertFalse(waiting.canClickReady)
        assertEquals(2, waiting.version)
    }

    @Test
    fun `copy creates new instance with same session ID`() {
        // Arrange
        val state = BattleSessionState()

        // Act
        val newState = state.copy(phase = BattlePhase.CARD_SELECTION)

        // Assert
        assertEquals(state.sessionId, newState.sessionId)
        assertNotSame(state, newState) // Different instances
        assertNotEquals(state.phase, newState.phase)
    }

    @Test
    fun `version tracking survives multiple updates`() {
        // Arrange
        var state = BattleSessionState()

        // Act - Simulate 10 state changes
        repeat(10) {
            state = state.copy(version = state.version + 1)
        }

        // Assert
        assertEquals(10, state.version)
    }

    @Test
    fun `full card data is stored correctly`() {
        // Arrange
        val state = BattleSessionState(phase = BattlePhase.CARD_SELECTION)
        val fullCard = createTestCard(id = 1, name = "Full Card")

        // Act
        val newState = state.copy(
            localPlayer = state.localPlayer.copy(
                fullCard = fullCard
            )
        ).nextVersion()

        // Assert
        assertEquals(fullCard, newState.localPlayer.fullCard)
        assertEquals(1, newState.version)
    }

    @Test
    fun `RevealState tracks reveal progress correctly`() {
        // Arrange
        val revealState = RevealState(
            initiatedBy = "host",
            cardsRevealed = false,
            statsRevealed = false
        )

        // Act - Reveal cards
        val cardsRevealed = revealState.copy(cardsRevealed = true)

        // Assert
        assertTrue(cardsRevealed.cardsRevealed)
        assertFalse(cardsRevealed.statsRevealed)

        // Act - Reveal stats
        val allRevealed = cardsRevealed.copy(statsRevealed = true)

        // Assert
        assertTrue(allRevealed.cardsRevealed)
        assertTrue(allRevealed.statsRevealed)
    }

    @Test
    fun `BattleExecutionState stores story segments and result`() {
        // Arrange
        val segments = listOf(
            BattleStorySegment("First attack", isLocalAction = true, damageDealt = 10),
            BattleStorySegment("Counter attack", isLocalAction = false, damageDealt = 15)
        )
        val result = BattleResult(
            isDraw = false,
            winnerIsLocal = true,
            winnerCardName = "Winner",
            loserCardName = "Loser",
            localCardFinalHealth = 50,
            opponentCardFinalHealth = 0,
            battleStory = "Battle story",
            cardWon = null
        )

        // Act
        val battleState = BattleExecutionState(
            storySegments = segments,
            result = result
        )

        // Assert
        assertEquals(2, battleState.storySegments.size)
        assertEquals(result, battleState.result)
        assertEquals("First attack", battleState.storySegments[0].text)
        assertEquals("Counter attack", battleState.storySegments[1].text)
    }

    // Helper functions to create test data
    private fun createTestBattleCard(id: Long, name: String): BattleCard {
        return BattleCard(
            card = createTestCard(id, name),
            effectiveAttack = 50,
            effectiveHealth = 100,
            currentHealth = 100
        )
    }

    private fun createTestCard(id: Long, name: String): Card {
        return Card(
            id = id,
            name = name,
            imageUrl = "/test/image.jpg",
            rarity = CardRarity.COMMON,
            prompt = "Test prompt",
            biography = "Test biography",
            createdAt = System.currentTimeMillis()
        )
    }
}
