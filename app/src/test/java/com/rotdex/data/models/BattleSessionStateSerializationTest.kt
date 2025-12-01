package com.rotdex.data.models

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for BattleSessionState serialization and deserialization
 *
 * These tests verify that battle state can be reliably serialized to JSON
 * and deserialized back without data loss. This is critical for Phase 4
 * state synchronization when reconnection is detected.
 *
 * Test scenarios:
 * 1. Empty state serialization round-trip
 * 2. State with selected cards serialization round-trip
 * 3. State in different phases serialization round-trip
 * 4. Invalid JSON handling (returns null gracefully)
 * 5. Version number preservation
 * 6. Session ID preservation
 */
class BattleSessionStateSerializationTest {

    @Test
    fun `serialize empty state and deserialize equals original`() {
        // Arrange
        val originalState = BattleSessionState()

        // Act
        val json = originalState.toJson()
        val deserializedState = BattleSessionState.fromJson(json)

        // Assert
        assertNotNull("Deserialized state should not be null", deserializedState)
        assertEquals("Session ID should be preserved", originalState.sessionId, deserializedState!!.sessionId)
        assertEquals("Version should be preserved", originalState.version, deserializedState.version)
        assertEquals("Phase should be preserved", originalState.phase, deserializedState.phase)
        assertEquals("Local player state should match", originalState.localPlayer, deserializedState.localPlayer)
        assertEquals("Opponent player state should match", originalState.opponentPlayer, deserializedState.opponentPlayer)
        assertEquals("Reveal state should be null", originalState.reveal, deserializedState.reveal)
        assertEquals("Battle state should be null", originalState.battle, deserializedState.battle)
        assertEquals("Can click ready should match", originalState.canClickReady, deserializedState.canClickReady)
        assertEquals("Waiting for opponent ready should match", originalState.waitingForOpponentReady, deserializedState.waitingForOpponentReady)
    }

    @Test
    fun `serialize state with selected cards and deserialize preserves all data`() {
        // Arrange
        val card = Card(
            id = 123L,
            name = "Test Card",
            imageUrl = "/path/to/image.jpg",
            rarity = CardRarity.EPIC,
            prompt = "A powerful test card",
            biography = "This card was created for testing",
            createdAt = 1234567890L
        )

        val battleCard = BattleCard(
            card = card,
            effectiveAttack = 85,
            effectiveHealth = 120,
            currentHealth = 120
        )

        val originalState = BattleSessionState(
            sessionId = "test-session-123",
            version = 5,
            phase = BattlePhase.CARD_SELECTION,
            localPlayer = PlayerState(
                hasSelectedCard = true,
                card = battleCard,
                fullCard = card,
                imageTransferStatus = ImageTransferStatus.COMPLETE,
                isReady = false,
                dataReceivedFromOpponent = true
            ),
            opponentPlayer = PlayerState(
                hasSelectedCard = true,
                card = battleCard.copy(card = card.copy(id = 456L, name = "Opponent Card")),
                fullCard = card.copy(id = 456L, name = "Opponent Card"),
                imageTransferStatus = ImageTransferStatus.NOT_STARTED,
                isReady = false,
                dataReceivedFromOpponent = false
            ),
            canClickReady = true,
            waitingForOpponentReady = false
        )

        // Act
        val json = originalState.toJson()
        val deserializedState = BattleSessionState.fromJson(json)

        // Assert
        assertNotNull("Deserialized state should not be null", deserializedState)
        assertEquals("Session ID should be preserved", originalState.sessionId, deserializedState!!.sessionId)
        assertEquals("Version should be preserved", originalState.version, deserializedState.version)
        assertEquals("Phase should be preserved", originalState.phase, deserializedState.phase)

        // Check local player state
        assertEquals("Local player has selected card", originalState.localPlayer.hasSelectedCard, deserializedState.localPlayer.hasSelectedCard)
        assertEquals("Local player card name", originalState.localPlayer.card?.card?.name, deserializedState.localPlayer.card?.card?.name)
        assertEquals("Local player card attack", originalState.localPlayer.card?.effectiveAttack, deserializedState.localPlayer.card?.effectiveAttack)
        assertEquals("Local player card health", originalState.localPlayer.card?.effectiveHealth, deserializedState.localPlayer.card?.effectiveHealth)
        assertEquals("Local player image transfer complete", originalState.localPlayer.imageTransferComplete, deserializedState.localPlayer.imageTransferComplete)

        // Check opponent player state
        assertEquals("Opponent player has selected card", originalState.opponentPlayer.hasSelectedCard, deserializedState.opponentPlayer.hasSelectedCard)
        assertEquals("Opponent player card name", originalState.opponentPlayer.card?.card?.name, deserializedState.opponentPlayer.card?.card?.name)
    }

    @Test
    fun `serialize state in BATTLE_ANIMATING phase and deserialize preserves phase`() {
        // Arrange
        val originalState = BattleSessionState(
            sessionId = "battle-session",
            version = 10,
            phase = BattlePhase.BATTLE_ANIMATING,
            battle = BattleExecutionState(
                storySegments = listOf(
                    BattleStorySegment("First attack!", isLocalAction = true, damageDealt = 50),
                    BattleStorySegment("Counter attack!", isLocalAction = false, damageDealt = 30)
                ),
                result = null
            )
        )

        // Act
        val json = originalState.toJson()
        val deserializedState = BattleSessionState.fromJson(json)

        // Assert
        assertNotNull("Deserialized state should not be null", deserializedState)
        assertEquals("Phase should be BATTLE_ANIMATING", BattlePhase.BATTLE_ANIMATING, deserializedState!!.phase)
        assertEquals("Battle state should be preserved", originalState.battle?.storySegments?.size, deserializedState.battle?.storySegments?.size)
        assertEquals("First story segment text", originalState.battle?.storySegments?.get(0)?.text, deserializedState.battle?.storySegments?.get(0)?.text)
        assertEquals("First story segment damage", originalState.battle?.storySegments?.get(0)?.damageDealt, deserializedState.battle?.storySegments?.get(0)?.damageDealt)
    }

    @Test
    fun `serialize state with reveal in progress and deserialize preserves reveal state`() {
        // Arrange
        val originalState = BattleSessionState(
            sessionId = "reveal-session",
            version = 7,
            phase = BattlePhase.REVEALING,
            reveal = RevealState(
                initiatedBy = "host",
                startedAt = 1234567890L,
                cardsRevealed = true,
                statsRevealed = false
            )
        )

        // Act
        val json = originalState.toJson()
        val deserializedState = BattleSessionState.fromJson(json)

        // Assert
        assertNotNull("Deserialized state should not be null", deserializedState)
        assertEquals("Phase should be REVEALING", BattlePhase.REVEALING, deserializedState!!.phase)
        assertNotNull("Reveal state should not be null", deserializedState.reveal)
        assertEquals("Reveal initiated by", originalState.reveal?.initiatedBy, deserializedState.reveal?.initiatedBy)
        assertEquals("Reveal started at", originalState.reveal?.startedAt, deserializedState.reveal?.startedAt)
        assertEquals("Cards revealed", originalState.reveal?.cardsRevealed, deserializedState.reveal?.cardsRevealed)
        assertEquals("Stats revealed", originalState.reveal?.statsRevealed, deserializedState.reveal?.statsRevealed)
    }

    @Test
    fun `invalid JSON returns null gracefully without crash`() {
        // Arrange
        val invalidJson = "{ this is not valid json }"

        // Act
        val deserializedState = BattleSessionState.fromJson(invalidJson)

        // Assert
        assertNull("Invalid JSON should return null", deserializedState)
    }

    @Test
    fun `empty string returns null gracefully`() {
        // Arrange
        val emptyJson = ""

        // Act
        val deserializedState = BattleSessionState.fromJson(emptyJson)

        // Assert
        assertNull("Empty JSON should return null", deserializedState)
    }

    @Test
    fun `version number preserved through serialization`() {
        // Arrange
        val originalState = BattleSessionState(version = 42)

        // Act
        val json = originalState.toJson()
        val deserializedState = BattleSessionState.fromJson(json)

        // Assert
        assertNotNull("Deserialized state should not be null", deserializedState)
        assertEquals("Version should be 42", 42, deserializedState!!.version)
    }

    @Test
    fun `session ID preserved through serialization`() {
        // Arrange
        val customSessionId = "custom-session-id-12345"
        val originalState = BattleSessionState(sessionId = customSessionId)

        // Act
        val json = originalState.toJson()
        val deserializedState = BattleSessionState.fromJson(json)

        // Assert
        assertNotNull("Deserialized state should not be null", deserializedState)
        assertEquals("Session ID should match", customSessionId, deserializedState!!.sessionId)
    }

    @Test
    fun `serialize state in DISCONNECTED phase preserves phase`() {
        // Arrange
        val originalState = BattleSessionState(
            phase = BattlePhase.DISCONNECTED
        )

        // Act
        val json = originalState.toJson()
        val deserializedState = BattleSessionState.fromJson(json)

        // Assert
        assertNotNull("Deserialized state should not be null", deserializedState)
        assertEquals("Phase should be DISCONNECTED", BattlePhase.DISCONNECTED, deserializedState!!.phase)
    }

    @Test
    fun `serialized JSON is not empty`() {
        // Arrange
        val state = BattleSessionState()

        // Act
        val json = state.toJson()

        // Assert
        println("DEBUG: Serialized JSON = '$json'")  // DEBUG output
        assertTrue("JSON should not be empty (got: '$json')", json.isNotEmpty())
        assertTrue("JSON should contain sessionId (got: '$json')", json.contains("sessionId"))
        assertTrue("JSON should contain version (got: '$json')", json.contains("version"))
        assertTrue("JSON should contain phase (got: '$json')", json.contains("phase"))
    }
}
