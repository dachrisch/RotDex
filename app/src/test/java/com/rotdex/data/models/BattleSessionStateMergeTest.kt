package com.rotdex.data.models

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for BattleSessionState merge logic
 *
 * Phase 4 state synchronization requires merging states when reconnection is detected.
 * These tests verify the host-authoritative merge strategy works correctly.
 *
 * Merge Rules:
 * 1. Host's state wins for conflicts (host-authoritative)
 * 2. Both players' card selections are preserved
 * 3. Take most advanced phase
 * 4. New version = max(local, opponent) + 1
 */
class BattleSessionStateMergeTest {

    private fun createCardForTesting(id: Long, name: String): Card {
        return Card(
            id = id,
            name = name,
            imageUrl = "/test/image.jpg",
            rarity = CardRarity.COMMON,
            prompt = "test prompt",
            biography = "test bio",
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createBattleCardForTesting(id: Long, name: String): BattleCard {
        val card = createCardForTesting(id, name)
        return BattleCard.fromCard(card)
    }

    @Test
    fun `host merge - host's phase wins on conflict`() {
        // Arrange
        val localState = BattleSessionState(
            sessionId = "session-1",
            version = 3,
            phase = BattlePhase.CARD_SELECTION,
            localPlayer = PlayerState(hasSelectedCard = true)
        )

        val opponentState = BattleSessionState(
            sessionId = "session-2",
            version = 2,
            phase = BattlePhase.WAITING_FOR_OPPONENT,
            localPlayer = PlayerState(hasSelectedCard = true)  // Opponent's view of themselves
        )

        // Act
        val merged = localState.merge(opponentState, isHost = true)

        // Assert
        assertEquals("Host's phase should win", BattlePhase.CARD_SELECTION, merged.phase)
        assertEquals("Version should be max + 1", 4, merged.version)
        assertTrue("Local player state preserved", merged.localPlayer.hasSelectedCard)
        assertTrue("Opponent player state synced", merged.opponentPlayer.hasSelectedCard)
    }

    @Test
    fun `non-host merge - accepts host's phase`() {
        // Arrange
        val localState = BattleSessionState(
            sessionId = "session-1",
            version = 2,
            phase = BattlePhase.WAITING_FOR_OPPONENT,
            localPlayer = PlayerState(hasSelectedCard = true)
        )

        val hostState = BattleSessionState(
            sessionId = "session-2",
            version = 3,
            phase = BattlePhase.CARD_SELECTION,
            localPlayer = PlayerState(hasSelectedCard = true)  // Host's view of themselves
        )

        // Act
        val merged = localState.merge(hostState, isHost = false)

        // Assert
        assertEquals("Should accept host's phase", BattlePhase.CARD_SELECTION, merged.phase)
        assertEquals("Version should be max + 1", 4, merged.version)
        assertTrue("Local player state preserved", merged.localPlayer.hasSelectedCard)
        assertTrue("Opponent player state synced from host", merged.opponentPlayer.hasSelectedCard)
    }

    @Test
    fun `both players card selections preserved through merge`() {
        // Arrange
        val localCard = createBattleCardForTesting(1L, "Local Card")
        val opponentCard = createBattleCardForTesting(2L, "Opponent Card")

        val localState = BattleSessionState(
            sessionId = "session-1",
            version = 5,
            localPlayer = PlayerState(
                hasSelectedCard = true,
                card = localCard,
                isReady = true
            )
        )

        val opponentState = BattleSessionState(
            sessionId = "session-2",
            version = 6,
            localPlayer = PlayerState(  // Opponent's view of themselves
                hasSelectedCard = true,
                card = opponentCard,
                isReady = false
            )
        )

        // Act (as host)
        val merged = localState.merge(opponentState, isHost = true)

        // Assert
        assertEquals("Local card preserved", "Local Card", merged.localPlayer.card?.card?.name)
        assertEquals("Opponent card synced", "Opponent Card", merged.opponentPlayer.card?.card?.name)
        assertTrue("Local ready state preserved", merged.localPlayer.isReady)
        assertFalse("Opponent ready state synced", merged.opponentPlayer.isReady)
    }

    @Test
    fun `version increments after merge`() {
        // Arrange
        val state1 = BattleSessionState(version = 10)
        val state2 = BattleSessionState(version = 15)

        // Act
        val merged = state1.merge(state2, isHost = true)

        // Assert
        assertEquals("Version should be max(10, 15) + 1", 16, merged.version)
    }

    @Test
    fun `takes most advanced phase when host`() {
        // Arrange - local (host) is ahead
        val localState = BattleSessionState(
            version = 1,
            phase = BattlePhase.REVEALING
        )
        val opponentState = BattleSessionState(
            version = 1,
            phase = BattlePhase.CARD_SELECTION
        )

        // Act
        val merged = localState.merge(opponentState, isHost = true)

        // Assert
        assertEquals("Should take most advanced phase", BattlePhase.REVEALING, merged.phase)
    }

    @Test
    fun `takes most advanced phase when non-host`() {
        // Arrange - host (opponent) is ahead
        val localState = BattleSessionState(
            version = 1,
            phase = BattlePhase.CARD_SELECTION
        )
        val hostState = BattleSessionState(
            version = 1,
            phase = BattlePhase.REVEALING
        )

        // Act
        val merged = localState.merge(hostState, isHost = false)

        // Assert
        assertEquals("Should accept host's advanced phase", BattlePhase.REVEALING, merged.phase)
    }

    @Test
    fun `reveal state merged correctly when host has reveal`() {
        // Arrange
        val localState = BattleSessionState(
            version = 1,
            phase = BattlePhase.REVEALING,
            reveal = RevealState(
                initiatedBy = "host",
                startedAt = 1000L,
                cardsRevealed = true,
                statsRevealed = false
            )
        )
        val opponentState = BattleSessionState(
            version = 1,
            phase = BattlePhase.CARD_SELECTION,
            reveal = null
        )

        // Act
        val merged = localState.merge(opponentState, isHost = true)

        // Assert
        assertNotNull("Reveal state should be preserved", merged.reveal)
        assertEquals("Reveal initiatedBy preserved", "host", merged.reveal?.initiatedBy)
        assertTrue("Cards revealed status preserved", merged.reveal?.cardsRevealed ?: false)
    }

    @Test
    fun `session ID uses local session ID after merge`() {
        // Arrange
        val localState = BattleSessionState(sessionId = "local-session-123")
        val opponentState = BattleSessionState(sessionId = "opponent-session-456")

        // Act
        val merged = localState.merge(opponentState, isHost = true)

        // Assert
        assertEquals("Session ID should be from local state", "local-session-123", merged.sessionId)
    }

    @Test
    fun `UI state correctly merged`() {
        // Arrange
        val localState = BattleSessionState(
            version = 1,
            canClickReady = true,
            waitingForOpponentReady = false
        )
        val opponentState = BattleSessionState(
            version = 1,
            canClickReady = false,
            waitingForOpponentReady = true
        )

        // Act (as host)
        val merged = localState.merge(opponentState, isHost = true)

        // Assert
        // Host's UI state should be preserved
        assertTrue("Host's canClickReady preserved", merged.canClickReady)
        assertFalse("Host's waitingForOpponentReady preserved", merged.waitingForOpponentReady)
    }

    @Test
    fun `battle execution state merged when present`() {
        // Arrange
        val storySegments = listOf(
            BattleStorySegment("First attack!", isLocalAction = true, damageDealt = 50)
        )
        val localState = BattleSessionState(
            version = 1,
            phase = BattlePhase.BATTLE_ANIMATING,
            battle = BattleExecutionState(
                storySegments = storySegments,
                result = null
            )
        )
        val opponentState = BattleSessionState(
            version = 1,
            phase = BattlePhase.READY_SYNC,
            battle = null
        )

        // Act (as host)
        val merged = localState.merge(opponentState, isHost = true)

        // Assert
        assertNotNull("Battle state should be preserved", merged.battle)
        assertEquals("Story segments preserved", 1, merged.battle?.storySegments?.size)
        assertEquals("Story text preserved", "First attack!", merged.battle?.storySegments?.get(0)?.text)
    }
}
