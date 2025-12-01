package com.rotdex.data.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.rotdex.data.models.ConnectionEvent
import com.rotdex.data.models.ConnectionLifecycle
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for BattleManager connection lifecycle tracking
 *
 * Tests that BattleManager correctly:
 * - Tracks connection lifecycle states
 * - Detects reconnections when endpoint ID changes
 * - Increments connection number on each connection
 * - Records connection events for debugging
 * - Maintains connection history
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BattleManagerConnectionLifecycleTest {

    private lateinit var context: Context
    private lateinit var battleManager: BattleManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        battleManager = BattleManager(context)
    }

    /**
     * Helper method to create a mock ConnectionResolution
     * Uses MockK which can mock final classes (unlike Mockito)
     */
    private fun createMockConnectionResult(statusCode: Int): ConnectionResolution {
        val status = mockk<com.google.android.gms.common.api.Status>(relaxed = true) {
            every { this@mockk.statusCode } returns statusCode
        }

        return mockk<ConnectionResolution>(relaxed = true) {
            every { this@mockk.status } returns status
        }
    }

    @Test
    fun `initial state is Idle`() = runBlocking {
        // Assert
        val lifecycle = battleManager.connectionLifecycle.first()
        assertTrue("Initial lifecycle should be Idle", lifecycle is ConnectionLifecycle.Idle)
    }

    @Test
    fun `startAutoDiscovery sets Discovering state`() = runBlocking {
        // Act
        battleManager.startAutoDiscovery("TestPlayer")

        // Assert
        val lifecycle = battleManager.connectionLifecycle.first()
        assertTrue("Lifecycle should be Discovering after startAutoDiscovery", lifecycle is ConnectionLifecycle.Discovering)

        // Verify event was recorded
        val events = battleManager.getConnectionEvents()
        val discoveryEvent = events.find { it is ConnectionEvent.DiscoveryStarted }
        assertNotNull("DiscoveryStarted event should be recorded", discoveryEvent)
    }

    @Test
    fun `first connection has connectionNumber 1 and isReconnection false`() = runBlocking {
        // Arrange
        battleManager.startAutoDiscovery("TestPlayer")

        // Act - simulate successful connection
        val connectionResolution = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("ENDPOINT1", connectionResolution)

        // Assert
        val lifecycle = battleManager.connectionLifecycle.first()
        assertTrue("Lifecycle should be Connected", lifecycle is ConnectionLifecycle.Connected)

        val connected = lifecycle as ConnectionLifecycle.Connected
        assertEquals("First connection should have connectionNumber 1", 1, connected.connectionNumber)
        assertFalse("First connection should not be reconnection", connected.isReconnection)
        assertNull("First connection should have no previous endpoint", connected.previousEndpointId)
        assertEquals("ENDPOINT1", connected.endpointId)

        // Verify event was recorded
        val events = battleManager.getConnectionEvents()
        val successEvent = events.find { it is ConnectionEvent.ConnectionSuccess } as? ConnectionEvent.ConnectionSuccess
        assertNotNull("ConnectionSuccess event should be recorded", successEvent)
        assertFalse("ConnectionSuccess event should not be reconnection", successEvent?.isReconnection ?: true)
    }

    @Test
    fun `reconnection with different endpoint is detected`() = runBlocking {
        // Arrange - establish first connection
        battleManager.startAutoDiscovery("TestPlayer")
        val firstConnection = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("ENDPOINT1", firstConnection)

        // Act - simulate disconnection and reconnection with different endpoint
        battleManager.simulateDisconnection("ENDPOINT1")

        val secondConnection = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("ENDPOINT2", secondConnection)

        // Assert
        val lifecycle = battleManager.connectionLifecycle.first()
        assertTrue("Lifecycle should be Connected", lifecycle is ConnectionLifecycle.Connected)

        val connected = lifecycle as ConnectionLifecycle.Connected
        assertEquals("Second connection should have connectionNumber 2", 2, connected.connectionNumber)
        assertTrue("Connection with different endpoint should be reconnection", connected.isReconnection)
        assertEquals("Previous endpoint should be ENDPOINT1", "ENDPOINT1", connected.previousEndpointId)
        assertEquals("Current endpoint should be ENDPOINT2", "ENDPOINT2", connected.endpointId)

        // Verify reconnection event was recorded
        val events = battleManager.getConnectionEvents()
        val reconnectEvent = events.find { it is ConnectionEvent.ReconnectionDetected } as? ConnectionEvent.ReconnectionDetected
        assertNotNull("ReconnectionDetected event should be recorded", reconnectEvent)
        assertEquals("ENDPOINT1", reconnectEvent?.oldEndpoint)
        assertEquals("ENDPOINT2", reconnectEvent?.newEndpoint)
    }

    @Test
    fun `connection number increments on each connection`() = runBlocking {
        // Arrange & Act - establish multiple connections
        battleManager.startAutoDiscovery("TestPlayer")

        val connection1 = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("EP1", connection1)

        val lifecycle1 = battleManager.connectionLifecycle.first()
        assertEquals(1, (lifecycle1 as ConnectionLifecycle.Connected).connectionNumber)

        battleManager.simulateDisconnection("EP1")

        val connection2 = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("EP2", connection2)

        val lifecycle2 = battleManager.connectionLifecycle.first()
        assertEquals(2, (lifecycle2 as ConnectionLifecycle.Connected).connectionNumber)

        battleManager.simulateDisconnection("EP2")

        val connection3 = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("EP3", connection3)

        val lifecycle3 = battleManager.connectionLifecycle.first()
        assertEquals(3, (lifecycle3 as ConnectionLifecycle.Connected).connectionNumber)

        // Assert - verify history is maintained
        val history = battleManager.getConnectionHistory()
        assertEquals("Should have 3 connection records", 3, history.size)
        assertEquals(1, history[0].connectionNumber)
        assertEquals(2, history[1].connectionNumber)
        assertEquals(3, history[2].connectionNumber)
    }

    @Test
    fun `onDisconnected sets Disconnected state`() = runBlocking {
        // Arrange - establish connection first
        battleManager.startAutoDiscovery("TestPlayer")
        val connection = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("ENDPOINT1", connection)

        // Act
        battleManager.simulateDisconnection("ENDPOINT1")

        // Assert
        val lifecycle = battleManager.connectionLifecycle.first()
        assertTrue("Lifecycle should be Disconnected", lifecycle is ConnectionLifecycle.Disconnected)

        // Verify event was recorded
        val events = battleManager.getConnectionEvents()
        val disconnectEvent = events.find { it is ConnectionEvent.Disconnected }
        assertNotNull("Disconnected event should be recorded", disconnectEvent)
    }

    @Test
    fun `connection events are recorded in order`() = runBlocking {
        // Act - simulate full connection lifecycle
        battleManager.startAutoDiscovery("TestPlayer")

        val connection = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("ENDPOINT1", connection)

        battleManager.simulateDisconnection("ENDPOINT1")

        // Assert
        val events = battleManager.getConnectionEvents()

        // Should have at least 3 events in order
        assertTrue("Should have multiple events", events.size >= 3)

        var foundDiscovery = false
        var foundSuccess = false
        var foundDisconnect = false

        events.forEach { event ->
            when (event) {
                is ConnectionEvent.DiscoveryStarted -> foundDiscovery = true
                is ConnectionEvent.ConnectionSuccess -> {
                    assertTrue("Discovery should come before success", foundDiscovery)
                    foundSuccess = true
                }
                is ConnectionEvent.Disconnected -> {
                    assertTrue("Success should come before disconnect", foundSuccess)
                    foundDisconnect = true
                }
                else -> {}
            }
        }

        assertTrue("Should have all three event types", foundDiscovery && foundSuccess && foundDisconnect)
    }

    @Test
    fun `connection failure event is recorded`() = runBlocking {
        // Arrange
        battleManager.startAutoDiscovery("TestPlayer")

        // Act - simulate connection failure
        val failedConnection = createMockConnectionResult(ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED)
        battleManager.simulateConnectionResult("ENDPOINT1", failedConnection)

        // Assert
        val events = battleManager.getConnectionEvents()
        val failureEvent = events.find { it is ConnectionEvent.ConnectionFailed } as? ConnectionEvent.ConnectionFailed
        assertNotNull("ConnectionFailed event should be recorded", failureEvent)
        assertEquals("ENDPOINT1", failureEvent?.endpointId)
    }

    @Test
    fun `connection history is maintained across multiple connections`() = runBlocking {
        // Arrange & Act - multiple connections
        battleManager.startAutoDiscovery("TestPlayer")

        repeat(3) { index ->
            val connection = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
            battleManager.simulateConnectionResult("EP$index", connection)

            if (index < 2) {
                battleManager.simulateDisconnection("EP$index")
            }
        }

        // Assert
        val history = battleManager.getConnectionHistory()
        assertEquals("Should have 3 connections in history", 3, history.size)

        history.forEachIndexed { index, connected ->
            assertEquals("EP$index", connected.endpointId)
            assertEquals(index + 1, connected.connectionNumber)

            if (index > 0) {
                assertTrue("Connections after first should be reconnections", connected.isReconnection)
                assertEquals("EP${index - 1}", connected.previousEndpointId)
            } else {
                assertFalse("First connection should not be reconnection", connected.isReconnection)
            }
        }
    }

    @Test
    fun `stopAll resets lifecycle to Idle`() = runBlocking {
        // Arrange - establish connection
        battleManager.startAutoDiscovery("TestPlayer")
        val connection = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("ENDPOINT1", connection)

        // Act
        battleManager.stopAll()

        // Assert
        val lifecycle = battleManager.connectionLifecycle.first()
        assertTrue("Lifecycle should be Idle after stopAll", lifecycle is ConnectionLifecycle.Idle)
    }

    @Test
    fun `onReconnectionDetected callback is invoked for reconnections`() = runBlocking {
        // Arrange - track callback invocations
        var callbackInvoked = false
        var oldEndpoint: String? = null
        var newEndpoint: String? = null

        battleManager.setReconnectionCallback { old, new ->
            callbackInvoked = true
            oldEndpoint = old
            newEndpoint = new
        }

        // Establish first connection
        battleManager.startAutoDiscovery("TestPlayer")
        val connection1 = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("ENDPOINT1", connection1)

        // Act - reconnect with different endpoint
        battleManager.simulateDisconnection("ENDPOINT1")
        val connection2 = createMockConnectionResult(ConnectionsStatusCodes.STATUS_OK)
        battleManager.simulateConnectionResult("ENDPOINT2", connection2)

        // Assert
        assertTrue("Reconnection callback should be invoked", callbackInvoked)
        assertEquals("ENDPOINT1", oldEndpoint)
        assertEquals("ENDPOINT2", newEndpoint)
    }
}
