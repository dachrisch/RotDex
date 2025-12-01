package com.rotdex.data.manager

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Minimal manager for testing Nearby Connections API
 * This is a proof-of-concept to verify Bluetooth connectivity works
 */
class ConnectionTestManager(private val context: Context) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.rotdex.battle.test"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<String>>(emptyList())
    val discoveredDevices: StateFlow<List<String>> = _discoveredDevices.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    private var currentEndpointId: String? = null

    // Connection lifecycle callbacks
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with: ${info.endpointName}")
            _connectionState.value = ConnectionState.ConnectionInitiated(info.endpointName)

            // Auto-accept connection for testing
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connection successful!")
                    currentEndpointId = endpointId
                    _connectionState.value = ConnectionState.Connected(endpointId)
                    addMessage("✅ Connected successfully!")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected")
                    _connectionState.value = ConnectionState.Error("Connection rejected")
                    addMessage("❌ Connection rejected")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG, "Connection failed")
                    _connectionState.value = ConnectionState.Error("Connection failed")
                    addMessage("❌ Connection failed")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from: $endpointId")
            _connectionState.value = ConnectionState.Disconnected
            currentEndpointId = null
            addMessage("🔌 Disconnected")
        }
    }

    // Payload (message) callback
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val message = String(bytes, Charsets.UTF_8)
                Log.d(TAG, "Received message: $message")
                addMessage("📩 Received: $message")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Not needed for simple text messages
        }
    }

    // Endpoint discovery callback
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: ${info.endpointName}")
            val devices = _discoveredDevices.value.toMutableList()
            devices.add("${info.endpointName} ($endpointId)")
            _discoveredDevices.value = devices
            addMessage("👀 Found: ${info.endpointName}")
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            val devices = _discoveredDevices.value.filterNot { it.contains(endpointId) }
            _discoveredDevices.value = devices
        }
    }

    /**
     * Start advertising as a host (Player 1)
     */
    fun startAdvertising(playerName: String) {
        Log.d(TAG, "Starting advertising as: $playerName")
        _connectionState.value = ConnectionState.Advertising
        _messages.value = emptyList()
        addMessage("📡 Advertising as: $playerName")

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startAdvertising(
            playerName,
            serviceId,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully")
            addMessage("✅ Waiting for opponent...")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Advertising failed", exception)
            _connectionState.value = ConnectionState.Error("Advertising failed: ${exception.message}")
            addMessage("❌ Advertising failed")
        }
    }

    /**
     * Start discovering hosts (Player 2)
     */
    fun startDiscovery(playerName: String) {
        Log.d(TAG, "Starting discovery as: $playerName")
        _connectionState.value = ConnectionState.Discovering
        _messages.value = emptyList()
        _discoveredDevices.value = emptyList()
        addMessage("🔍 Scanning for hosts...")

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started successfully")
            addMessage("✅ Scanning...")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Discovery failed", exception)
            _connectionState.value = ConnectionState.Error("Discovery failed: ${exception.message}")
            addMessage("❌ Discovery failed")
        }
    }

    /**
     * Connect to a discovered endpoint
     */
    fun connectToEndpoint(endpointId: String, playerName: String) {
        Log.d(TAG, "Requesting connection to: $endpointId")
        // Look up device name from discovered devices (format: "name (endpointId)")
        val deviceEntry = _discoveredDevices.value.find { it.contains(endpointId) }
        val targetName = deviceEntry?.substringBefore(" (") ?: "Opponent"
        _connectionState.value = ConnectionState.Connecting(targetName)
        addMessage("🤝 Connecting...")

        connectionsClient.requestConnection(
            playerName,
            endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            Log.d(TAG, "Connection requested")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Connection request failed", exception)
            _connectionState.value = ConnectionState.Error("Connection failed: ${exception.message}")
            addMessage("❌ Connection request failed")
        }
    }

    /**
     * Send a test message to connected endpoint
     */
    fun sendMessage(message: String) {
        currentEndpointId?.let { endpointId ->
            val payload = Payload.fromBytes(message.toByteArray(Charsets.UTF_8))
            connectionsClient.sendPayload(endpointId, payload)
            Log.d(TAG, "Sent message: $message")
            addMessage("📤 Sent: $message")
        } ?: run {
            Log.w(TAG, "Not connected, cannot send message")
            addMessage("⚠️ Not connected")
        }
    }

    /**
     * Stop all connections and discovery
     */
    fun stopAll() {
        Log.d(TAG, "Stopping all connections")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _connectionState.value = ConnectionState.Idle
        _discoveredDevices.value = emptyList()
        currentEndpointId = null
    }

    private fun addMessage(message: String) {
        val messages = _messages.value.toMutableList()
        messages.add(0, message) // Add to top
        _messages.value = messages.take(20) // Keep last 20 messages
    }

    companion object {
        private const val TAG = "ConnectionTest"
    }
}

/**
 * Connection states
 */
sealed class ConnectionState {
    object Idle : ConnectionState()
    object Advertising : ConnectionState()
    object Discovering : ConnectionState()
    data class AutoDiscovering(val playerName: String) : ConnectionState()
    data class Connecting(val targetName: String) : ConnectionState()
    data class ConnectionInitiated(val opponentName: String) : ConnectionState()
    data class Connected(val endpointId: String) : ConnectionState()
    object Disconnected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
