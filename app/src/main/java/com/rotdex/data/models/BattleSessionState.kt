package com.rotdex.data.models

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Complete battle session state - single source of truth
 *
 * Replaces the 13 separate StateFlows in BattleManager with one consolidated state object.
 * Version tracking enables state synchronization on reconnection (Phase 4).
 *
 * This unified state model solves critical issues:
 * - Provides atomic snapshot for synchronization
 * - Single source of truth eliminates inconsistencies
 * - Version tracking enables conflict resolution
 * - Immutable updates prevent race conditions
 *
 * @property sessionId Unique identifier for this battle session
 * @property version Incremented on every state change (for sync/conflict resolution)
 * @property phase Current battle phase in the state machine
 * @property localPlayer State of the local player
 * @property opponentPlayer State of the opponent player
 * @property reveal State of the reveal sequence (null if not revealing)
 * @property battle State of battle execution (null if not battling)
 * @property canClickReady Whether the ready button should be enabled
 * @property waitingForOpponentReady Whether we're waiting for opponent to be ready
 */
data class BattleSessionState(
    val sessionId: String = generateSessionId(),
    val version: Int = 0,
    val phase: BattlePhase = BattlePhase.WAITING_FOR_OPPONENT,

    // Player states
    val localPlayer: PlayerState = PlayerState(),
    val opponentPlayer: PlayerState = PlayerState(),

    // Battle execution
    val reveal: RevealState? = null,
    val battle: BattleExecutionState? = null,

    // UI state
    val canClickReady: Boolean = false,
    val waitingForOpponentReady: Boolean = false
) {
    /**
     * Increment version - call this on EVERY state change
     * Returns a new state with version incremented by 1
     */
    fun nextVersion() = copy(version = version + 1)

    /**
     * Merge two battle states after reconnection
     *
     * Implements host-authoritative conflict resolution:
     * - Host's state wins for conflicts
     * - Both players' card selections are preserved
     * - Phase transitions use most advanced phase
     * - Version becomes max(local, opponent) + 1
     *
     * @param opponent The opponent's battle state
     * @param isHost Whether this device is the host
     * @return Merged state with conflicts resolved
     */
    fun merge(opponent: BattleSessionState, isHost: Boolean): BattleSessionState {
        // Calculate new version (max + 1)
        val newVersion = maxOf(this.version, opponent.version) + 1

        // Determine which phase to use (most advanced, with host authority)
        val mergedPhase = if (isHost) {
            // Host: prefer local phase if more advanced, otherwise keep local
            if (this.phase.ordinal >= opponent.phase.ordinal) this.phase else opponent.phase
        } else {
            // Non-host: accept opponent (host) phase if more advanced
            if (opponent.phase.ordinal >= this.phase.ordinal) opponent.phase else this.phase
        }

        return if (isHost) {
            // Host merge: keep local state, sync opponent's player data
            this.copy(
                version = newVersion,
                phase = mergedPhase,
                // Sync opponent's data but preserve local file paths
                opponentPlayer = opponent.localPlayer.copy(
                    imageFilePath = this.opponentPlayer.imageFilePath  // Keep our local file path
                ),
                // Preserve local reveal/battle if more advanced
                reveal = this.reveal ?: opponent.reveal,
                battle = this.battle ?: opponent.battle
            )
        } else {
            // Non-host merge: accept host's state structure, preserve local player data
            opponent.copy(
                sessionId = this.sessionId,  // Keep local session ID
                version = newVersion,
                phase = mergedPhase,
                localPlayer = this.localPlayer,  // Preserve our card selection
                // Sync host's data but preserve local file paths
                opponentPlayer = opponent.localPlayer.copy(
                    imageFilePath = this.opponentPlayer.imageFilePath  // Keep our local file path
                ),
                // Accept host's UI state and battle progression
                reveal = opponent.reveal ?: this.reveal,
                battle = opponent.battle ?: this.battle
            )
        }
    }

    /**
     * Detect missing images after state synchronization
     *
     * PHASE 5: Checks if opponent has completed image transfer but we're missing the file.
     * This happens when:
     * 1. Connection dropped during file transfer
     * 2. FILE payload was lost
     * 3. Image transfer completed but file wasn't saved
     *
     * @return True if opponent's image is marked COMPLETE but we don't have the file
     */
    fun hasMissingOpponentImage(): Boolean {
        return opponentPlayer.imageTransferStatus == ImageTransferStatus.COMPLETE &&
                opponentPlayer.imageFilePath == null
    }

    /**
     * Detect if our local image needs to be re-sent
     *
     * PHASE 5: Checks if we've marked transfer as complete but opponent doesn't have it.
     * Used after state sync to detect if opponent is missing our image.
     *
     * @return True if our image is COMPLETE but opponent doesn't show it as received
     */
    fun shouldResendLocalImage(): Boolean {
        return localPlayer.imageTransferStatus == ImageTransferStatus.COMPLETE &&
                opponentPlayer.imageTransferStatus != ImageTransferStatus.COMPLETE
    }

    /**
     * Serialize this state to JSON for network transmission
     *
     * Used in Phase 4 state synchronization to send state to opponent
     * when reconnection is detected.
     *
     * @return JSON string representation of this state
     */
    fun toJson(): String {
        return try {
            gson.toJson(this)
        } catch (e: Exception) {
            // Return empty object on error
            // In production, this should be logged
            "{}"
        }
    }

    companion object {
        /**
         * Gson instance for JSON serialization/deserialization
         */
        private val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

        /**
         * Deserialize state from JSON
         *
         * Used in Phase 4 state synchronization to receive state from opponent
         * when reconnection is detected.
         *
         * @param jsonString JSON string to deserialize
         * @return BattleSessionState or null if deserialization fails
         */
        fun fromJson(jsonString: String): BattleSessionState? {
            return try {
                if (jsonString.isEmpty()) return null
                gson.fromJson(jsonString, BattleSessionState::class.java)
            } catch (e: Exception) {
                // Return null on error
                // In production, this should be logged
                null
            }
        }
    }
}

/**
 * Image transfer status tracking
 *
 * PHASE 5: Enhanced status tracking for resilient image transfers
 */
enum class ImageTransferStatus {
    /**
     * No transfer initiated yet
     */
    NOT_STARTED,

    /**
     * Metadata sent, waiting for file payload
     */
    PENDING,

    /**
     * File transfer in progress
     */
    IN_PROGRESS,

    /**
     * Transfer completed successfully
     */
    COMPLETE,

    /**
     * Transfer failed, needs retry
     */
    FAILED
}

/**
 * State of a player in the battle
 *
 * Tracks card selection, data transfer, and ready status.
 *
 * @property hasSelectedCard Whether player has selected a card for battle
 * @property card The battle card selected (with calculated stats)
 * @property fullCard The full card data (for image transfer and details)
 * @property imageTransferStatus Current status of image transfer (PHASE 5)
 * @property imageFilePath Local file path after successful transfer (PHASE 5)
 * @property imageHash SHA-256 hash for verification (PHASE 5, optional)
 * @property isReady Whether player has clicked ready button
 * @property dataReceivedFromOpponent Whether we've received opponent's data
 */
data class PlayerState(
    val hasSelectedCard: Boolean = false,
    val card: BattleCard? = null,
    val fullCard: Card? = null,  // For image transfer

    // PHASE 5: Enhanced image transfer tracking
    val imageTransferStatus: ImageTransferStatus = ImageTransferStatus.NOT_STARTED,
    val imageFilePath: String? = null,       // Actual file path on device
    val imageHash: String? = null,           // For verification (optional)

    val isReady: Boolean = false,
    val dataReceivedFromOpponent: Boolean = false
) {
    /**
     * Backward compatibility: Legacy code checks imageTransferComplete
     * Maps to new imageTransferStatus field
     */
    val imageTransferComplete: Boolean
        get() = imageTransferStatus == ImageTransferStatus.COMPLETE
}

/**
 * Battle phase state machine
 *
 * Defines the progression of a battle session:
 * 1. WAITING_FOR_OPPONENT - Not connected
 * 2. CARD_SELECTION - Connected, selecting cards
 * 3. READY_SYNC - Both ready, syncing data
 * 4. REVEALING - Reveal animation in progress
 * 5. BATTLE_ANIMATING - Battle executing
 * 6. BATTLE_COMPLETE - Battle finished
 * 7. DISCONNECTED - Connection lost
 */
enum class BattlePhase {
    /**
     * Waiting for opponent to connect
     */
    WAITING_FOR_OPPONENT,

    /**
     * Connected, both players selecting cards
     */
    CARD_SELECTION,

    /**
     * Both players ready, syncing final data
     */
    READY_SYNC,

    /**
     * Reveal animation in progress (blur removal, stats reveal)
     */
    REVEALING,

    /**
     * Battle animation and story playing
     */
    BATTLE_ANIMATING,

    /**
     * Battle finished, showing results
     */
    BATTLE_COMPLETE,

    /**
     * Connection lost during battle
     */
    DISCONNECTED
}

/**
 * State of the reveal sequence
 *
 * Tracks the dramatic reveal animation before battle starts.
 *
 * @property initiatedBy Who initiated the reveal ("host" or "client")
 * @property startedAt Timestamp when reveal started
 * @property cardsRevealed Whether blur animation has completed
 * @property statsRevealed Whether stats have been shown
 */
data class RevealState(
    val initiatedBy: String,
    val startedAt: Long = System.currentTimeMillis(),
    val cardsRevealed: Boolean = false,
    val statsRevealed: Boolean = false
)

/**
 * State of battle execution
 *
 * Tracks the battle animation, story segments, and final result.
 *
 * @property storySegments List of story segments for progressive animation
 * @property result Final battle result (null until battle completes)
 */
data class BattleExecutionState(
    val storySegments: List<BattleStorySegment> = emptyList(),
    val result: BattleResult? = null
)

/**
 * Generate a unique session ID
 * Uses timestamp + random to ensure uniqueness
 */
private fun generateSessionId(): String {
    return "session-${System.currentTimeMillis()}-${(0..9999).random()}"
}
