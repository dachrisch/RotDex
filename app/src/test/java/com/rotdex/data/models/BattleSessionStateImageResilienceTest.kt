package com.rotdex.data.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Phase 5 image transfer resilience
 *
 * Verifies:
 * - Missing image detection after state sync
 * - Image re-request logic
 * - ImageTransferStatus tracking
 */
class BattleSessionStateImageResilienceTest {

    // ========== hasMissingOpponentImage Tests ==========

    @Test
    fun `hasMissingOpponentImage returns true when opponent status is COMPLETE but no file path`() {
        // Arrange
        val state = BattleSessionState(
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE,
                imageFilePath = null  // Missing file!
            )
        )

        // Act & Assert
        assertTrue(state.hasMissingOpponentImage())
    }

    @Test
    fun `hasMissingOpponentImage returns false when opponent status is COMPLETE with file path`() {
        // Arrange
        val state = BattleSessionState(
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE,
                imageFilePath = "/path/to/image.jpg"  // File exists
            )
        )

        // Act & Assert
        assertFalse(state.hasMissingOpponentImage())
    }

    @Test
    fun `hasMissingOpponentImage returns false when opponent status is NOT_STARTED`() {
        // Arrange
        val state = BattleSessionState(
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.NOT_STARTED,
                imageFilePath = null
            )
        )

        // Act & Assert
        assertFalse(state.hasMissingOpponentImage())
    }

    @Test
    fun `hasMissingOpponentImage returns false when opponent status is PENDING`() {
        // Arrange
        val state = BattleSessionState(
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.PENDING,
                imageFilePath = null
            )
        )

        // Act & Assert
        assertFalse(state.hasMissingOpponentImage())
    }

    @Test
    fun `hasMissingOpponentImage returns false when opponent status is IN_PROGRESS`() {
        // Arrange
        val state = BattleSessionState(
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.IN_PROGRESS,
                imageFilePath = null
            )
        )

        // Act & Assert
        assertFalse(state.hasMissingOpponentImage())
    }

    @Test
    fun `hasMissingOpponentImage returns false when opponent status is FAILED`() {
        // Arrange
        val state = BattleSessionState(
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.FAILED,
                imageFilePath = null
            )
        )

        // Act & Assert
        assertFalse(state.hasMissingOpponentImage())
    }

    // ========== shouldResendLocalImage Tests ==========

    @Test
    fun `shouldResendLocalImage returns true when local is COMPLETE but opponent is NOT_STARTED`() {
        // Arrange
        val state = BattleSessionState(
            localPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE
            ),
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.NOT_STARTED
            )
        )

        // Act & Assert
        assertTrue(state.shouldResendLocalImage())
    }

    @Test
    fun `shouldResendLocalImage returns true when local is COMPLETE but opponent is PENDING`() {
        // Arrange
        val state = BattleSessionState(
            localPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE
            ),
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.PENDING
            )
        )

        // Act & Assert
        assertTrue(state.shouldResendLocalImage())
    }

    @Test
    fun `shouldResendLocalImage returns true when local is COMPLETE but opponent is FAILED`() {
        // Arrange
        val state = BattleSessionState(
            localPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE
            ),
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.FAILED
            )
        )

        // Act & Assert
        assertTrue(state.shouldResendLocalImage())
    }

    @Test
    fun `shouldResendLocalImage returns false when both local and opponent are COMPLETE`() {
        // Arrange
        val state = BattleSessionState(
            localPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE
            ),
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE
            )
        )

        // Act & Assert
        assertFalse(state.shouldResendLocalImage())
    }

    @Test
    fun `shouldResendLocalImage returns false when local is NOT_STARTED`() {
        // Arrange
        val state = BattleSessionState(
            localPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.NOT_STARTED
            ),
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.NOT_STARTED
            )
        )

        // Act & Assert
        assertFalse(state.shouldResendLocalImage())
    }

    @Test
    fun `shouldResendLocalImage returns false when local is PENDING`() {
        // Arrange
        val state = BattleSessionState(
            localPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.PENDING
            ),
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.NOT_STARTED
            )
        )

        // Act & Assert
        assertFalse(state.shouldResendLocalImage())
    }

    // ========== Backward Compatibility Tests ==========

    @Test
    fun `imageTransferComplete returns true when status is COMPLETE`() {
        // Arrange
        val playerState = PlayerState(
            imageTransferStatus = ImageTransferStatus.COMPLETE
        )

        // Act & Assert
        assertTrue(playerState.imageTransferComplete)
    }

    @Test
    fun `imageTransferComplete returns false when status is NOT_STARTED`() {
        // Arrange
        val playerState = PlayerState(
            imageTransferStatus = ImageTransferStatus.NOT_STARTED
        )

        // Act & Assert
        assertFalse(playerState.imageTransferComplete)
    }

    @Test
    fun `imageTransferComplete returns false when status is PENDING`() {
        // Arrange
        val playerState = PlayerState(
            imageTransferStatus = ImageTransferStatus.PENDING
        )

        // Act & Assert
        assertFalse(playerState.imageTransferComplete)
    }

    @Test
    fun `imageTransferComplete returns false when status is IN_PROGRESS`() {
        // Arrange
        val playerState = PlayerState(
            imageTransferStatus = ImageTransferStatus.IN_PROGRESS
        )

        // Act & Assert
        assertFalse(playerState.imageTransferComplete)
    }

    @Test
    fun `imageTransferComplete returns false when status is FAILED`() {
        // Arrange
        val playerState = PlayerState(
            imageTransferStatus = ImageTransferStatus.FAILED
        )

        // Act & Assert
        assertFalse(playerState.imageTransferComplete)
    }

    // ========== Integration Scenarios ==========

    @Test
    fun `merge preserves image transfer status from both players`() {
        // Arrange
        val localState = BattleSessionState(
            localPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE,
                imageFilePath = "/local/image.jpg"
            ),
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.PENDING
            )
        )

        val opponentState = BattleSessionState(
            localPlayer = PlayerState(  // This becomes opponentPlayer in merge
                imageTransferStatus = ImageTransferStatus.COMPLETE,
                imageFilePath = "/opponent/image.jpg"
            ),
            opponentPlayer = PlayerState(  // This becomes localPlayer in merge (from opponent's perspective)
                imageTransferStatus = ImageTransferStatus.PENDING
            )
        )

        // Act - Host merge
        val mergedState = localState.merge(opponentState, isHost = true)

        // Assert
        assertEquals(ImageTransferStatus.COMPLETE, mergedState.localPlayer.imageTransferStatus)
        assertEquals("/local/image.jpg", mergedState.localPlayer.imageFilePath)

        // Opponent's status should be synced but file path stays local
        assertEquals(ImageTransferStatus.COMPLETE, mergedState.opponentPlayer.imageTransferStatus)
        assertNull("File path should not be synced - it's device-local", mergedState.opponentPlayer.imageFilePath)
    }

    @Test
    fun `reconnection scenario - both players have COMPLETE but one missing file`() {
        // Arrange - Simulates reconnection where opponent shows COMPLETE but we lost the file
        val localState = BattleSessionState(
            sessionId = "local-session",
            version = 5,
            localPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE,
                imageFilePath = "/local/card.jpg"
            ),
            opponentPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE,
                imageFilePath = null  // Lost during connection drop!
            )
        )

        val opponentState = BattleSessionState(
            sessionId = "opponent-session",
            version = 5,
            localPlayer = PlayerState(
                imageTransferStatus = ImageTransferStatus.COMPLETE,
                imageFilePath = "/opponent/card.jpg"
            )
        )

        // Act
        val mergedState = localState.merge(opponentState, isHost = true)

        // Assert - Should detect missing file
        assertTrue("Should detect missing opponent image", mergedState.hasMissingOpponentImage())
    }
}
