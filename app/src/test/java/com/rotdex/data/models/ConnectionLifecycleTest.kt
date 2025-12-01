package com.rotdex.data.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Test suite for ConnectionLifecycle state machine
 *
 * Tests connection lifecycle tracking including:
 * - Initial connection establishment
 * - Reconnection detection after endpoint changes
 * - Connection number incrementation
 * - Event tracking
 */
class ConnectionLifecycleTest {

    @Test
    fun `initial connection has connectionNumber 1 and isReconnection false`() {
        // Arrange & Act
        val lifecycle = ConnectionLifecycle.Connected(
            endpointId = "ABCD",
            connectionNumber = 1,
            connectedAt = System.currentTimeMillis(),
            previousEndpointId = null
        )

        // Assert
        assertEquals(1, lifecycle.connectionNumber)
        assertFalse("Initial connection should not be a reconnection", lifecycle.isReconnection)
        assertNull("Initial connection should have no previous endpoint", lifecycle.previousEndpointId)
    }

    @Test
    fun `reconnection with different endpoint has isReconnection true`() {
        // Arrange & Act
        val lifecycle = ConnectionLifecycle.Connected(
            endpointId = "EFGH",
            connectionNumber = 2,
            connectedAt = System.currentTimeMillis(),
            previousEndpointId = "ABCD"
        )

        // Assert
        assertEquals(2, lifecycle.connectionNumber)
        assertTrue("Connection with different endpoint should be reconnection", lifecycle.isReconnection)
        assertEquals("ABCD", lifecycle.previousEndpointId)
        assertEquals("EFGH", lifecycle.endpointId)
    }

    @Test
    fun `reconnection with same endpoint has isReconnection false`() {
        // Arrange & Act - same endpoint ID used twice (e.g., manual reconnect)
        val lifecycle = ConnectionLifecycle.Connected(
            endpointId = "ABCD",
            connectionNumber = 2,
            connectedAt = System.currentTimeMillis(),
            previousEndpointId = "ABCD"
        )

        // Assert
        assertEquals(2, lifecycle.connectionNumber)
        assertFalse("Connection with same endpoint should not be reconnection", lifecycle.isReconnection)
    }

    @Test
    fun `Discovering state tracks start time`() {
        // Arrange
        val startTime = System.currentTimeMillis()

        // Act
        val lifecycle = ConnectionLifecycle.Discovering(startedAt = startTime)

        // Assert
        assertEquals(startTime, lifecycle.startedAt)
    }

    @Test
    fun `Connecting state tracks endpoint and attempt number`() {
        // Arrange & Act
        val lifecycle = ConnectionLifecycle.Connecting(
            endpointId = "WXYZ",
            attempt = 2,
            startedAt = System.currentTimeMillis()
        )

        // Assert
        assertEquals("WXYZ", lifecycle.endpointId)
        assertEquals(2, lifecycle.attempt)
    }

    @Test
    fun `Reconnecting state tracks previous endpoint and reason`() {
        // Arrange & Act
        val lifecycle = ConnectionLifecycle.Reconnecting(
            reason = "collision",
            previousEndpointId = "ABCD",
            startedAt = System.currentTimeMillis()
        )

        // Assert
        assertEquals("collision", lifecycle.reason)
        assertEquals("ABCD", lifecycle.previousEndpointId)
    }

    @Test
    fun `ConnectionEvent DiscoveryStarted has timestamp`() {
        // Arrange
        val timestamp = System.currentTimeMillis()

        // Act
        val event = ConnectionEvent.DiscoveryStarted(timestamp)

        // Assert
        assertEquals(timestamp, event.timestamp)
    }

    @Test
    fun `ConnectionEvent EndpointFound tracks endpoint ID and name`() {
        // Arrange & Act
        val event = ConnectionEvent.EndpointFound(
            endpointId = "TEST123",
            name = "Player-42"
        )

        // Assert
        assertEquals("TEST123", event.endpointId)
        assertEquals("Player-42", event.name)
    }

    @Test
    fun `ConnectionEvent ConnectionSuccess tracks reconnection status`() {
        // Arrange & Act
        val initialConnection = ConnectionEvent.ConnectionSuccess(
            endpointId = "ABCD",
            isReconnection = false
        )
        val reconnection = ConnectionEvent.ConnectionSuccess(
            endpointId = "EFGH",
            isReconnection = true
        )

        // Assert
        assertFalse("Initial connection event should not be reconnection", initialConnection.isReconnection)
        assertTrue("Reconnection event should be marked as reconnection", reconnection.isReconnection)
    }

    @Test
    fun `ConnectionEvent ReconnectionDetected tracks both endpoints`() {
        // Arrange & Act
        val event = ConnectionEvent.ReconnectionDetected(
            oldEndpoint = "ABCD",
            newEndpoint = "EFGH"
        )

        // Assert
        assertEquals("ABCD", event.oldEndpoint)
        assertEquals("EFGH", event.newEndpoint)
    }

    @Test
    fun `ConnectionEvent ConnectionFailed tracks reason`() {
        // Arrange & Act
        val event = ConnectionEvent.ConnectionFailed(
            endpointId = "FAIL",
            reason = "timeout"
        )

        // Assert
        assertEquals("FAIL", event.endpointId)
        assertEquals("timeout", event.reason)
    }

    @Test
    fun `ConnectionEvent Disconnected tracks reason`() {
        // Arrange & Act
        val event = ConnectionEvent.Disconnected(
            endpointId = "GONE",
            reason = "user_initiated"
        )

        // Assert
        assertEquals("GONE", event.endpointId)
        assertEquals("user_initiated", event.reason)
    }

    @Test
    fun `lifecycle states are distinct sealed classes`() {
        // Arrange & Act
        val idle = ConnectionLifecycle.Idle
        val discovering = ConnectionLifecycle.Discovering()
        val connecting = ConnectionLifecycle.Connecting("TEST", 1)
        val connected = ConnectionLifecycle.Connected("TEST", 1, System.currentTimeMillis())
        val reconnecting = ConnectionLifecycle.Reconnecting("test", "OLD")
        val disconnected = ConnectionLifecycle.Disconnected

        // Assert - verify each is a different type
        assertNotEquals(idle, discovering)
        assertNotEquals(discovering, connecting)
        assertNotEquals(connecting, connected)
        assertNotEquals(connected, reconnecting)
        assertNotEquals(reconnecting, disconnected)
    }

    @Test
    fun `connection number increments across multiple connections`() {
        // Arrange - simulate connection history
        val connection1 = ConnectionLifecycle.Connected(
            endpointId = "EP1",
            connectionNumber = 1,
            connectedAt = 1000L,
            previousEndpointId = null
        )

        val connection2 = ConnectionLifecycle.Connected(
            endpointId = "EP2",
            connectionNumber = 2,
            connectedAt = 2000L,
            previousEndpointId = "EP1"
        )

        val connection3 = ConnectionLifecycle.Connected(
            endpointId = "EP3",
            connectionNumber = 3,
            connectedAt = 3000L,
            previousEndpointId = "EP2"
        )

        // Assert
        assertEquals(1, connection1.connectionNumber)
        assertEquals(2, connection2.connectionNumber)
        assertEquals(3, connection3.connectionNumber)

        assertFalse(connection1.isReconnection)
        assertTrue(connection2.isReconnection)
        assertTrue(connection3.isReconnection)
    }
}
