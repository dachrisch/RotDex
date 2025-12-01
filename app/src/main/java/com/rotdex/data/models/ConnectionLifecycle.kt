package com.rotdex.data.models

/**
 * Connection lifecycle state - tracks connection history and transitions
 *
 * This sealed class represents the complete lifecycle of a Nearby Connections session,
 * enabling detection of reconnections after collision and proper state synchronization.
 *
 * **Connection States:**
 * 1. `Idle` - No connection activity
 * 2. `Discovering` - Actively searching for opponents
 * 3. `Connecting` - Connection request in progress (with retry tracking)
 * 4. `Connected` - Successfully connected (tracks if this is a reconnection)
 * 5. `Reconnecting` - Attempting to reconnect after failure
 * 6. `Disconnected` - Connection lost
 *
 * **Reconnection Detection:**
 * When a connection collision occurs:
 * 1. Both devices connect simultaneously → sockets close with IO errors
 * 2. Retry logic establishes NEW connection with DIFFERENT endpoint ID
 * 3. `Connected.isReconnection` property detects endpoint change
 * 4. Phase 4 will use this to trigger state resynchronization
 *
 * @see ConnectionEvent for connection event tracking
 */
sealed class ConnectionLifecycle {
    /**
     * No connection activity - initial state
     */
    object Idle : ConnectionLifecycle()

    /**
     * Discovery in progress - searching for nearby devices
     *
     * @property startedAt Timestamp when discovery started (for timeout tracking)
     */
    data class Discovering(
        val startedAt: Long = System.currentTimeMillis()
    ) : ConnectionLifecycle()

    /**
     * Connection attempt in progress
     *
     * @property endpointId Remote endpoint being connected to
     * @property attempt Retry attempt number (1 = first attempt, 2+ = retries)
     * @property startedAt Timestamp when connection attempt started
     */
    data class Connecting(
        val endpointId: String,
        val attempt: Int = 1,
        val startedAt: Long = System.currentTimeMillis()
    ) : ConnectionLifecycle()

    /**
     * Successfully connected to remote device
     *
     * **Reconnection Detection:**
     * - `isReconnection = true` when `previousEndpointId != endpointId`
     * - This indicates the connection was re-established with a different endpoint
     *   (typically after collision recovery)
     * - Phase 4 will use this to trigger state resynchronization
     *
     * @property endpointId Current connected endpoint ID
     * @property connectionNumber Sequential connection counter (1st, 2nd, 3rd, etc.)
     * @property connectedAt Timestamp when connection was established
     * @property previousEndpointId Previous endpoint ID (null for initial connection)
     */
    data class Connected(
        val endpointId: String,
        val connectionNumber: Int,
        val connectedAt: Long,
        val previousEndpointId: String? = null
    ) : ConnectionLifecycle() {
        /**
         * True if this is a reconnection with a different endpoint ID
         *
         * **Use cases:**
         * - Connection collision recovery (endpoint changed)
         * - Manual reconnect after disconnect (endpoint may change)
         * - Network handoff scenarios
         *
         * **State sync trigger (Phase 4):**
         * When `isReconnection == true`, BattleManager should:
         * 1. Send current battle state to opponent
         * 2. Request opponent's current state
         * 3. Reconcile any differences
         */
        val isReconnection: Boolean
            get() = previousEndpointId != null && previousEndpointId != endpointId
    }

    /**
     * Reconnection attempt in progress after failure/disconnect
     *
     * @property reason Reason for reconnection ("collision", "disconnect", "timeout", etc.)
     * @property previousEndpointId Endpoint we were previously connected to
     * @property startedAt Timestamp when reconnection attempt started
     */
    data class Reconnecting(
        val reason: String,
        val previousEndpointId: String,
        val startedAt: Long = System.currentTimeMillis()
    ) : ConnectionLifecycle()

    /**
     * Disconnected - connection lost or manually terminated
     */
    object Disconnected : ConnectionLifecycle()
}

/**
 * Connection events for logging, debugging, and analytics
 *
 * These events track all connection lifecycle transitions for:
 * - Debugging connection issues in production
 * - Analytics on connection success rates
 * - Reconnection pattern analysis
 *
 * Events are appended to a mutable list in BattleManager for inspection.
 */
sealed class ConnectionEvent {
    /**
     * Discovery started - device is searching for opponents
     *
     * @property timestamp When discovery started
     */
    data class DiscoveryStarted(val timestamp: Long) : ConnectionEvent()

    /**
     * Remote endpoint discovered
     *
     * @property endpointId Discovered endpoint's ID
     * @property name Discovered endpoint's display name
     */
    data class EndpointFound(val endpointId: String, val name: String) : ConnectionEvent()

    /**
     * Connection attempt initiated
     *
     * @property endpointId Endpoint being connected to
     * @property attempt Attempt number (1 = first, 2+ = retry)
     */
    data class ConnectionAttempt(val endpointId: String, val attempt: Int) : ConnectionEvent()

    /**
     * Connection established successfully
     *
     * @property endpointId Connected endpoint ID
     * @property isReconnection True if this is a reconnection with different endpoint
     */
    data class ConnectionSuccess(val endpointId: String, val isReconnection: Boolean) : ConnectionEvent()

    /**
     * Connection attempt failed
     *
     * @property endpointId Endpoint that failed to connect
     * @property reason Failure reason ("rejected", "timeout", "io_error", etc.)
     */
    data class ConnectionFailed(val endpointId: String, val reason: String) : ConnectionEvent()

    /**
     * Connection lost
     *
     * @property endpointId Endpoint that disconnected
     * @property reason Disconnect reason ("user_initiated", "network_error", etc.)
     */
    data class Disconnected(val endpointId: String, val reason: String) : ConnectionEvent()

    /**
     * Reconnection detected - endpoint ID changed
     *
     * **Critical event for Phase 4:**
     * This event indicates that state resynchronization should occur.
     *
     * @property oldEndpoint Previous endpoint ID
     * @property newEndpoint New endpoint ID
     */
    data class ReconnectionDetected(val oldEndpoint: String, val newEndpoint: String) : ConnectionEvent()
}
