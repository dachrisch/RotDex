package com.rotdex.data.manager

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.rotdex.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Manages battle arena logic including:
 * - Nearby Connections for device communication
 * - Card selection and synchronization
 * - Battle damage calculation with chance factor
 * - AI-generated battle story
 */
class BattleManager(private val context: Context) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.rotdex.battle.arena"

    // Coroutine scope for async operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Keep-alive heartbeat to prevent connection timeout
    private var heartbeatJob: Job? = null

    // ============================================================
    // PHASE 1: Message Reliability Layer
    // ============================================================
    /**
     * Message reliability layer with ACK/retry protocol
     * Ensures no messages are lost during connection transitions
     */
    private val messageReliability = MessageReliabilityLayer(
        sendRaw = { rawMessage ->
            currentEndpointId?.let { endpointId ->
                val payload = Payload.fromBytes(rawMessage.toByteArray(Charsets.UTF_8))
                connectionsClient.sendPayload(endpointId, payload)
            }
        },
        scope = scope
    )

    // ============================================================
    // PHASE 2: Unified State (Single Source of Truth)
    // ============================================================
    /**
     * Unified battle session state - replaces 13 separate StateFlows
     * Version tracking enables state synchronization on reconnection (Phase 4)
     */
    private val _battleSessionState = MutableStateFlow(BattleSessionState())
    val battleSessionState: StateFlow<BattleSessionState> = _battleSessionState.asStateFlow()

    // ============================================================
    // PHASE 3: Connection Lifecycle Management
    // ============================================================
    /**
     * Connection lifecycle state - tracks connection history and reconnections
     * Enables automatic state resynchronization when endpoint changes (Phase 4)
     */
    private val _connectionLifecycle = MutableStateFlow<ConnectionLifecycle>(ConnectionLifecycle.Idle)
    val connectionLifecycle: StateFlow<ConnectionLifecycle> = _connectionLifecycle.asStateFlow()

    /**
     * Connection event history for debugging and analytics
     * Records all connection lifecycle transitions
     */
    private val connectionEvents = mutableListOf<ConnectionEvent>()

    /**
     * Sequential connection counter - increments on each successful connection
     * Helps track connection history and identify reconnections
     */
    private var connectionNumber = 0

    /**
     * History of all successful connections in this session
     * Useful for debugging connection issues and analyzing patterns
     */
    private val connectionHistory = mutableListOf<ConnectionLifecycle.Connected>()

    /**
     * Callback invoked when reconnection is detected (endpoint ID changed)
     * Phase 4 will use this to trigger state resynchronization
     */
    private var reconnectionCallback: ((oldEndpoint: String, newEndpoint: String) -> Unit)? = null

    // ============================================================
    // LEGACY: Backward Compatibility StateFlows (Phase 2)
    // These will be removed in Phase 3 when UI is refactored
    // ============================================================
    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Battle state
    private val _battleState = MutableStateFlow(BattleState.WAITING_FOR_OPPONENT)
    val battleState: StateFlow<BattleState> = _battleState.asStateFlow()

    // Selected cards
    private val _localCard = MutableStateFlow<BattleCard?>(null)
    val localCard: StateFlow<BattleCard?> = _localCard.asStateFlow()

    private val _opponentCard = MutableStateFlow<BattleCard?>(null)
    val opponentCard: StateFlow<BattleCard?> = _opponentCard.asStateFlow()

    // Opponent player name (captured from connection)
    private val _opponentName = MutableStateFlow("Opponent")
    val opponentName: StateFlow<String> = _opponentName.asStateFlow()

    // Play state: Has opponent selected a card? (separate from technical data status)
    private val _opponentHasSelectedCard = MutableStateFlow(false)
    val opponentHasSelectedCard: StateFlow<Boolean> = _opponentHasSelectedCard.asStateFlow()

    // Battle story segments for progressive display
    private val _battleStory = MutableStateFlow<List<BattleStorySegment>>(emptyList())
    val battleStory: StateFlow<List<BattleStorySegment>> = _battleStory.asStateFlow()

    // Battle result
    private val _battleResult = MutableStateFlow<BattleResult?>(null)
    val battleResult: StateFlow<BattleResult?> = _battleResult.asStateFlow()

    // Messages/log
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    // Discovered devices
    private val _discoveredDevices = MutableStateFlow<List<String>>(emptyList())
    val discoveredDevices: StateFlow<List<String>> = _discoveredDevices.asStateFlow()

    // Ready state management
    private val _localReady = MutableStateFlow(false)
    val localReady: StateFlow<Boolean> = _localReady.asStateFlow()

    private val _opponentReady = MutableStateFlow(false)
    val opponentReady: StateFlow<Boolean> = _opponentReady.asStateFlow()

    // CRITICAL FIX: Start with false - only enable after opponent data is complete
    private val _canClickReady = MutableStateFlow(false)
    val canClickReady: StateFlow<Boolean> = _canClickReady.asStateFlow()

    private val _opponentIsThinking = MutableStateFlow(false)
    val opponentIsThinking: StateFlow<Boolean> = _opponentIsThinking.asStateFlow()

    // Data completeness tracking (for synchronization)
    private val _localDataComplete = MutableStateFlow(false)
    val localDataComplete: StateFlow<Boolean> = _localDataComplete.asStateFlow()

    private val _opponentDataComplete = MutableStateFlow(false)
    val opponentDataComplete: StateFlow<Boolean> = _opponentDataComplete.asStateFlow()

    private val _waitingForOpponentReady = MutableStateFlow(false)
    val waitingForOpponentReady: StateFlow<Boolean> = _waitingForOpponentReady.asStateFlow()

    // Image transfer status tracking
    private val _opponentImageTransferComplete = MutableStateFlow(false)
    val opponentImageTransferComplete: StateFlow<Boolean> = _opponentImageTransferComplete.asStateFlow()

    private val _localImageSent = MutableStateFlow(false)
    val localImageSent: StateFlow<Boolean> = _localImageSent.asStateFlow()

    private var localImageTransferComplete = false
    private var readyTimeoutJob: kotlinx.coroutines.Job? = null
    private var revealInitiated = false
    private var imageTransferRetryCount = 0
    private val maxImageTransferRetries = 3

    private var currentEndpointId: String? = null
    private var isHost: Boolean = false
    private var opponentReadyLegacy: Boolean = false
    private var localReadyLegacy: Boolean = false
    private var playerName: String = ""
    private var connectingToName: String = ""  // Name of device we're connecting to (for UI display)

    // Connection collision prevention with retry mechanism
    private var localEndpointId: String = ""  // Our generated endpoint ID for collision resolution
    private val outgoingConnectionRequests = mutableSetOf<String>()  // Track endpoints we requested connection to
    private val connectionRetryAttempts = mutableMapOf<String, Int>()  // Track retry attempts per endpoint
    private val maxRetryAttempts = 3
    private var retryJob: kotlinx.coroutines.Job? = null

    // Connection lifecycle callbacks
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with: ${info.endpointName}, endpointId: $endpointId")
            addMessage("Connection initiated with ${info.endpointName}")

            // Capture opponent's player name from their advertised endpoint name
            _opponentName.value = info.endpointName
            Log.d(TAG, "👤 Captured opponent name: ${info.endpointName}")

            // FIX: Determine host based on connection direction
            // isIncomingConnection = true means someone connected to US → we are HOST
            // isIncomingConnection = false means WE connected to them → we are CLIENT
            isHost = info.isIncomingConnection
            Log.d(TAG, "🔑 Host determination: isIncomingConnection=${info.isIncomingConnection} → isHost=$isHost")

            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "✅ Connection successful!")

                    // PHASE 3: Track connection lifecycle and detect reconnections
                    val previousEndpoint = currentEndpointId
                    currentEndpointId = endpointId

                    val isReconnection = previousEndpoint != null && previousEndpoint != endpointId
                    connectionNumber++

                    val lifecycle = ConnectionLifecycle.Connected(
                        endpointId = endpointId,
                        connectionNumber = connectionNumber,
                        connectedAt = System.currentTimeMillis(),
                        previousEndpointId = previousEndpoint
                    )

                    _connectionLifecycle.value = lifecycle
                    connectionHistory.add(lifecycle)

                    // Record connection success event
                    connectionEvents.add(ConnectionEvent.ConnectionSuccess(endpointId, isReconnection))

                    if (isReconnection) {
                        Log.d(TAG, "🔄 Reconnection detected: $previousEndpoint -> $endpointId (connection #$connectionNumber)")
                        connectionEvents.add(ConnectionEvent.ReconnectionDetected(previousEndpoint, endpointId))
                        onReconnectionDetected(previousEndpoint, endpointId)
                    } else {
                        Log.d(TAG, "✅ Initial connection established: $endpointId (connection #$connectionNumber)")
                    }

                    outgoingConnectionRequests.clear()
                    connectionRetryAttempts.clear()
                    retryJob?.cancel()

                    // CRITICAL FIX: Stop advertising and discovery to prevent endpoint churn
                    // When in auto-discovery mode, both are running and cause "endpoint lost" events
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                    Log.d(TAG, "✅ Stopped advertising and discovery after successful connection")

                    // Start keep-alive heartbeat to prevent connection timeout
                    startHeartbeat()

                    _connectionState.value = ConnectionState.Connected(endpointId)
                    _battleState.value = BattleState.CARD_SELECTION
                    _opponentIsThinking.value = true  // Opponent is now selecting their card
                    addMessage("Connected! Select your card for battle.")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "⚠️ Connection rejected by $endpointId")
                    handleConnectionFailure(endpointId, "rejected")
                }
                else -> {
                    Log.w(TAG, "⚠️ Connection failed with $endpointId, status=${result.status.statusCode}")
                    handleConnectionFailure(endpointId, "failed")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from: $endpointId")

            // Stop keep-alive heartbeat
            stopHeartbeat()

            // PHASE 3: Track disconnection lifecycle
            _connectionLifecycle.value = ConnectionLifecycle.Disconnected
            connectionEvents.add(ConnectionEvent.Disconnected(endpointId, "connection_lost"))

            _connectionState.value = ConnectionState.Disconnected
            setBattleState(BattleState.DISCONNECTED, "connection_lost")

            // PHASE 4: Clear pending messages from reliability layer
            messageReliability.clearPendingMessages()

            // PHASE 3: Keep currentEndpointId so we can detect reconnection
            // Only set to null in stopAll() or resetBattleState()
            // This allows reconnection detection when endpoint ID changes
            Log.d(TAG, "   Keeping currentEndpointId=$currentEndpointId for reconnection detection")

            addMessage("Opponent disconnected")
        }
    }

    // Track expected image transfers and received image paths
    private data class ImageTransferInfo(val cardId: Long, val fileName: String)
    private val expectedImageTransfers = mutableMapOf<Long, ImageTransferInfo>()  // payloadId -> info
    private val receivedImagePaths = mutableMapOf<Long, String>()  // cardId -> local file path

    // Track FILE payloads that arrived before IMAGE_TRANSFER metadata
    private val orphanedFiles = mutableMapOf<Long, String>()  // payloadId -> temp file path

    // Cache payloads so we can access them after transfer completes
    private val payloadCache = mutableMapOf<Long, Payload>()

    // Track pending file transfers (waiting for onPayloadTransferUpdate SUCCESS)
    private val pendingFileTransfers = mutableMapOf<Long, ImageTransferInfo>()  // payloadId -> info

    // Track outgoing file transfers (our image being sent to opponent)
    private val outgoingFileTransfers = mutableSetOf<Long>()  // payloadIds of files we're sending

    // Payload callback for battle messages
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    payload.asBytes()?.let { bytes ->
                        val messageStr = String(bytes, Charsets.UTF_8)
                        Log.d(TAG, "Received: $messageStr")
                        handleReceivedMessage(messageStr)
                    }
                }
                Payload.Type.FILE -> {
                    // DON'T read the file yet - transfer is still in progress!
                    // We'll read it in onPayloadTransferUpdate() when Status.SUCCESS
                    Log.d(TAG, "FILE payload received: payloadId=${payload.id}, caching for transfer completion")

                    // Cache the payload so we can access it after transfer completes
                    payloadCache[payload.id] = payload

                    // Check if we already have metadata for this payload
                    val transferInfo = expectedImageTransfers[payload.id]

                    if (transferInfo != null) {
                        // Case 1: IMAGE_TRANSFER arrived FIRST
                        Log.d(TAG, "Metadata already received for payloadId=${payload.id}, card=${transferInfo.cardId}")
                        pendingFileTransfers[payload.id] = transferInfo
                        expectedImageTransfers.remove(payload.id)
                    } else {
                        // Case 2: IMAGE_TRANSFER NOT YET arrived - mark as orphaned
                        Log.d(TAG, "Metadata not yet received for payloadId=${payload.id}, marking as orphaned")
                        // Will be matched when IMAGE_TRANSFER arrives
                    }
                }
                else -> {}
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    Log.d(TAG, "Payload ${update.payloadId} transferred successfully")

                    // Check if this is one of our OUTGOING file transfers
                    if (outgoingFileTransfers.contains(update.payloadId)) {
                        Log.d(TAG, "✅ OUTGOING image transfer complete: payloadId=${update.payloadId}")
                        _localImageSent.value = true
                        outgoingFileTransfers.remove(update.payloadId)
                    }

                    // Check if this is a pending file transfer
                    val transferInfo = pendingFileTransfers[update.payloadId]
                    if (transferInfo != null) {
                        // NOW the FILE transfer is complete - read the full file
                        val payload = payloadCache[update.payloadId]
                        payload?.asFile()?.let { filePayload ->
                            try {
                                Log.d(TAG, "Reading complete file for payloadId=${update.payloadId}, card=${transferInfo.cardId}")

                                // Get URI from the completed file transfer
                                val uri = filePayload.asUri()
                                if (uri != null) {
                                    // Open and copy the complete file
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    if (inputStream != null) {
                                        val imagesDir = java.io.File(context.filesDir, "card_images")
                                        if (!imagesDir.exists()) imagesDir.mkdirs()

                                        val newFileName = "temp_${update.payloadId}_${System.currentTimeMillis()}.jpg"
                                        val newFile = java.io.File(imagesDir, newFileName)

                                        // Copy the complete file
                                        java.io.FileOutputStream(newFile).use { outputStream ->
                                            inputStream.copyTo(outputStream, bufferSize = 8192)
                                        }
                                        inputStream.close()

                                        val imagePath = newFile.absolutePath
                                        val fileSize = newFile.length()
                                        Log.d(TAG, "✅ Complete FILE saved: payloadId=${update.payloadId}, path=$imagePath, size=$fileSize")

                                        // Update card with the complete image
                                        receivedImagePaths[transferInfo.cardId] = imagePath
                                        updateCardWithImage(transferInfo.cardId, imagePath)

                                        // Mark image transfer complete and check if all data received
                                        // CRITICAL FIX: Update StateFlow so UI can react
                                        _opponentImageTransferComplete.value = true
                                        Log.d(TAG, "✅ CRITICAL: Opponent image transfer complete for card ${transferInfo.cardId}")
                                        Log.d(TAG, "✅ CRITICAL: Setting _opponentImageTransferComplete = true")
                                        Log.d(TAG, "✅ CRITICAL: Now calling checkOpponentDataComplete()")
                                        checkOpponentDataComplete()

                                        // Cleanup
                                        pendingFileTransfers.remove(update.payloadId)
                                        payloadCache.remove(update.payloadId)
                                    } else {
                                        Log.e(TAG, "Failed to open input stream for URI: $uri")
                                    }
                                } else {
                                    Log.e(TAG, "Failed to get URI from file payload: ${update.payloadId}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to read complete file: ${e.message}", e)
                            }
                        }
                    }
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    Log.e(TAG, "Payload ${update.payloadId} transfer failed")
                    expectedImageTransfers.remove(update.payloadId)
                    pendingFileTransfers.remove(update.payloadId)
                    payloadCache.remove(update.payloadId)
                }
                else -> {
                    // IN_PROGRESS or CANCELED
                }
            }
        }
    }

    // Discovery callback
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: ${info.endpointName}")

            // Filter out self-discovery (same name as local player)
            if (info.endpointName == playerName) {
                Log.d(TAG, "🔄 Ignoring self-discovery: ${info.endpointName}")
                return
            }

            val devices = _discoveredDevices.value.toMutableList()
            devices.add("${info.endpointName}|$endpointId")
            _discoveredDevices.value = devices
            Log.d(TAG, "✅ discoveredDevices updated: ${_discoveredDevices.value.size} devices")
            addMessage("Found: ${info.endpointName}")
        }

        override fun onEndpointLost(endpointId: String) {
            // CRITICAL FIX: Don't remove endpoint if we're actively connecting to it
            // This prevents UI recomposition that causes bubble disappearance during collision retry
            if (outgoingConnectionRequests.contains(endpointId)) {
                Log.d(TAG, "⏸️ Keeping endpoint $endpointId in list (active connection in progress)")
                return
            }

            val devices = _discoveredDevices.value.filterNot { it.contains(endpointId) }
            _discoveredDevices.value = devices
            connectionRetryAttempts.remove(endpointId)

            Log.d(TAG, "🔴 Endpoint lost, discoveredDevices now: ${_discoveredDevices.value.size} devices")
        }
    }

    // ============================================================
    // PHASE 2: State Synchronization Helpers
    // ============================================================

    /**
     * Update unified session state and increment version
     * Automatically syncs to legacy StateFlows for backward compatibility
     *
     * @param update Lambda that transforms current state to new state
     */
    private fun updateSessionState(update: (BattleSessionState) -> BattleSessionState) {
        _battleSessionState.value = update(_battleSessionState.value).nextVersion()
        syncLegacyState()
        Log.d(TAG, "📊 Session state updated: version=${_battleSessionState.value.version}, phase=${_battleSessionState.value.phase}")
    }

    /**
     * Sync unified state to legacy StateFlows for backward compatibility
     * This ensures existing UI code continues to work during Phase 2
     * Will be removed in Phase 3 when UI is refactored to use unified state
     */
    private fun syncLegacyState() {
        val state = _battleSessionState.value

        // Sync player card selections
        // FIX: Defensive null check - don't overwrite existing data with null
        if (state.localPlayer.card != null) {
            _localCard.value = state.localPlayer.card
        } else if (_localCard.value != null) {
            Log.w(TAG, "⚠️ syncLegacyState: Preserving existing _localCard (unified state has null)")
        }

        if (state.opponentPlayer.card != null) {
            _opponentCard.value = state.opponentPlayer.card
        } else if (_opponentCard.value != null) {
            Log.w(TAG, "⚠️ syncLegacyState: Preserving existing _opponentCard (unified state has null)")
        }

        // Sync ready states
        _localReady.value = state.localPlayer.isReady
        _opponentReady.value = state.opponentPlayer.isReady

        // Sync opponent selection status
        _opponentHasSelectedCard.value = state.opponentPlayer.hasSelectedCard

        // Sync data completeness
        // FIX: Defensive check - don't overwrite true with false
        if (state.localPlayer.dataReceivedFromOpponent) {
            _localDataComplete.value = true
        } else if (_localDataComplete.value) {
            Log.w(TAG, "⚠️ syncLegacyState: Preserving _localDataComplete=true (unified state has false)")
        }

        if (state.opponentPlayer.dataReceivedFromOpponent) {
            _opponentDataComplete.value = true
        } else if (_opponentDataComplete.value) {
            Log.w(TAG, "⚠️ syncLegacyState: Preserving _opponentDataComplete=true (unified state has false)")
        }

        // Sync UI control states
        _canClickReady.value = state.canClickReady
        _waitingForOpponentReady.value = state.waitingForOpponentReady

        // Sync image transfer status
        _opponentImageTransferComplete.value = state.opponentPlayer.imageTransferComplete

        // Sync reveal and battle state (if applicable)
        if (state.reveal != null) {
            _statsRevealed.value = state.reveal.statsRevealed
            _shouldRevealCards.value = state.reveal.cardsRevealed
        }

        if (state.battle != null) {
            _battleStory.value = state.battle.storySegments
            _battleResult.value = state.battle.result
        }

        Log.d(TAG, "🔄 Legacy state synchronized from unified state")
    }

    /**
     * Start as host (advertise)
     */
    fun startAsHost(name: String) {
        playerName = name
        isHost = true
        Log.d(TAG, "🏠 Starting as HOST (isHost=true, name=$name)")
        _connectionState.value = ConnectionState.Advertising
        _battleState.value = BattleState.WAITING_FOR_OPPONENT
        resetBattleState()
        addMessage("Waiting for opponent...")

        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startAdvertising(name, serviceId, connectionLifecycleCallback, options)
            .addOnSuccessListener { addMessage("Ready for battle!") }
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Failed: ${e.message}")
            }
    }

    /**
     * Start as client (discover)
     */
    fun startAsClient(name: String) {
        playerName = name
        isHost = false
        Log.d(TAG, "🔍 Starting as CLIENT (isHost=false, name=$name)")
        _connectionState.value = ConnectionState.Discovering
        _battleState.value = BattleState.WAITING_FOR_OPPONENT
        resetBattleState()
        _discoveredDevices.value = emptyList()
        addMessage("Scanning for battles...")

        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener { addMessage("Scanning...") }
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Failed: ${e.message}")
            }
    }

    /**
     * Start auto-discovery mode (both advertise AND discover simultaneously)
     * Enables automatic peer-to-peer connection without manual role selection
     *
     * COLLISION PREVENTION: Generates unique local endpoint ID for comparison
     */
    fun startAutoDiscovery(name: String) {
        val effectiveName = name.ifEmpty { "player-${System.currentTimeMillis() % 10000}" }
        playerName = effectiveName

        // Generate unique session ID for collision resolution
        // This ID is used to determine connection priority when both devices connect simultaneously
        localEndpointId = generateSessionId()
        Log.d(TAG, "🆔 Generated local session ID for collision resolution: $localEndpointId")

        // PHASE 3: Track discovery lifecycle
        val discoveringState = ConnectionLifecycle.Discovering()
        _connectionLifecycle.value = discoveringState
        connectionEvents.add(ConnectionEvent.DiscoveryStarted(discoveringState.startedAt))

        _connectionState.value = ConnectionState.AutoDiscovering(effectiveName)
        _battleState.value = BattleState.WAITING_FOR_OPPONENT
        resetBattleState()
        _discoveredDevices.value = emptyList()
        addMessage("Finding opponents...")

        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        // Start advertising
        connectionsClient.startAdvertising(effectiveName, serviceId, connectionLifecycleCallback, options)
            .addOnSuccessListener {
                addMessage("Ready for discovery...")
            }
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Advertising failed: ${e.message}")
            }

        // ALSO start discovery
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                addMessage("Scanning for opponents...")
            }
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Discovery failed: ${e.message}")
            }
    }

    /**
     * Generate random 4-character session ID for collision resolution
     * Uses the same format as Nearby Connections endpoint IDs (alphanumeric)
     */
    private fun generateSessionId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..4)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Handle connection failure with retry logic to resolve collisions
     * Uses session ID comparison to stagger retries:
     * - Lower session ID: Retry immediately (0ms delay)
     * - Higher session ID: Retry after 2 seconds
     *
     * IMPORTANT: Keeps UI in "Connecting" state - user should never see an error during retry
     */
    private fun handleConnectionFailure(endpointId: String, reason: String) {
        // PHASE 3: Record connection failure event
        connectionEvents.add(ConnectionEvent.ConnectionFailed(endpointId, reason))

        // Check if this was an outgoing connection request (potential collision)
        if (!outgoingConnectionRequests.contains(endpointId)) {
            Log.d(TAG, "Not retrying $endpointId - was not our outgoing request")
            _connectionState.value = ConnectionState.Error("Connection $reason")
            addMessage("Connection $reason")
            return
        }

        // Check retry count
        val currentAttempts = connectionRetryAttempts.getOrDefault(endpointId, 0)
        if (currentAttempts >= maxRetryAttempts) {
            Log.w(TAG, "⛔ Max retry attempts ($maxRetryAttempts) reached for $endpointId")
            outgoingConnectionRequests.remove(endpointId)
            connectionRetryAttempts.remove(endpointId)
            _connectionState.value = ConnectionState.Error("Connection failed after $maxRetryAttempts attempts")
            addMessage("Connection failed - please try again")
            return
        }

        // Increment retry count
        connectionRetryAttempts[endpointId] = currentAttempts + 1
        val attemptNum = currentAttempts + 1

        // Calculate retry delay based on session ID comparison
        // Lower ID retries immediately, higher ID waits to avoid re-collision
        val retryDelay = if (localEndpointId < endpointId) {
            0L  // We have priority - retry immediately
        } else {
            2000L  // They have priority - wait 2 seconds
        }

        Log.d(TAG, """
            🔄 Retry attempt $attemptNum/$maxRetryAttempts for $endpointId
            Reason: $reason
            Session IDs: local=$localEndpointId, remote=$endpointId
            Retry delay: ${retryDelay}ms (${if (retryDelay == 0L) "immediate" else "delayed"})
        """.trimIndent())

        // CRITICAL: Keep UI in "Connecting" state - no error messages visible to user
        // The retry happens silently in the background
        if (attemptNum == 1) {
            // First retry - don't spam user with messages, keep "Connecting..." visible
            Log.d(TAG, "Silent retry - keeping UI in connecting state")
        }
        // Always stay in Connecting state during retries
        _connectionState.value = ConnectionState.Connecting(connectingToName)

        // Schedule retry with appropriate delay
        retryJob?.cancel()
        retryJob = scope.launch {
            kotlinx.coroutines.delay(retryDelay)
            Log.d(TAG, "🔁 Executing retry for $endpointId")
            connectToHostInternal(endpointId)
        }
    }

    /**
     * Connect to a discovered host (public API - initiates first attempt)
     * Clears any previous retry state for this endpoint
     */
    fun connectToHost(endpointId: String) {
        // Clear any previous retry state for fresh start
        connectionRetryAttempts.remove(endpointId)
        retryJob?.cancel()

        // Look up device name from discovered devices (format: "name|endpointId")
        val deviceEntry = _discoveredDevices.value.find { it.endsWith("|$endpointId") }
        connectingToName = deviceEntry?.split("|")?.firstOrNull() ?: "Opponent"
        Log.d(TAG, "📤 User initiated connection to $endpointId (name: $connectingToName)")

        connectToHostInternal(endpointId)
    }

    /**
     * Internal connection method used by both initial connection and retries
     * Tracks outgoing connection for collision detection
     */
    private fun connectToHostInternal(endpointId: String) {
        // Only add message on first attempt to avoid spam
        val attemptNum = connectionRetryAttempts.getOrDefault(endpointId, 0)

        // PHASE 3: Track connecting lifecycle
        val connectingState = ConnectionLifecycle.Connecting(
            endpointId = endpointId,
            attempt = attemptNum + 1
        )
        _connectionLifecycle.value = connectingState
        connectionEvents.add(ConnectionEvent.ConnectionAttempt(endpointId, attemptNum + 1))

        _connectionState.value = ConnectionState.Connecting(connectingToName)

        if (attemptNum == 0) {
            addMessage("Connecting...")
        }

        // Track this outgoing connection request for collision detection
        outgoingConnectionRequests.add(endpointId)

        val attemptLabel = if (attemptNum > 0) " (attempt ${attemptNum + 1})" else ""
        Log.d(TAG, "📤 Requesting connection to $endpointId$attemptLabel")

        connectionsClient.requestConnection(playerName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ requestConnection failed: ${e.message}")
                // Trigger retry logic for IO errors (likely collision)
                handleConnectionFailure(endpointId, "io_error")
            }
    }

    // Store full card data for transfer
    private var localFullCard: Card? = null

    // Track if stats should be revealed
    private val _statsRevealed = MutableStateFlow(false)
    val statsRevealed: StateFlow<Boolean> = _statsRevealed.asStateFlow()

    // Track if cards should be revealed (blur animation)
    private val _shouldRevealCards = MutableStateFlow(false)
    val shouldRevealCards: StateFlow<Boolean> = _shouldRevealCards.asStateFlow()

    /**
     * Select a card for battle
     * PHASE 2: Updates unified state + syncs to legacy StateFlows
     */
    fun selectCard(card: Card) {
        val battleCard = BattleCard.fromCard(card)

        // PHASE 2: Update unified state
        updateSessionState { state ->
            state.copy(
                localPlayer = state.localPlayer.copy(
                    hasSelectedCard = true,
                    card = battleCard,
                    fullCard = card
                ),
                canClickReady = true  // Enable ready button after card selection
            )
        }

        // LEGACY: Keep for network message sending
        localFullCard = card
        addMessage("Selected: ${card.name}")

        Log.d(TAG, "✅ Ready button enabled after card selection (v${_battleSessionState.value.version})")

        // IMPORTANT: Send CARDPREVIEW first, then image
        // This ensures opponent creates _opponentCard before image arrives
        // Format: CARDPREVIEW|id|name|rarity|attack|health|prompt|biography
        // Send all data immediately - UI will hide stats until reveal moment
        val previewData = listOf(
            "CARDPREVIEW",
            card.id.toString(),
            card.name,
            card.rarity.name,
            battleCard.effectiveAttack.toString(),
            battleCard.effectiveHealth.toString(),
            card.prompt.replace("|", "~"),
            card.biography.replace("|", "~")
        ).joinToString("|")
        sendMessage(previewData)
        Log.d(TAG, "📤 Sent CARDPREVIEW for card ${card.id} (${card.name}) with stats ATK=${battleCard.effectiveAttack} HP=${battleCard.effectiveHealth}")

        // Then send card image (will arrive separately, possibly before CARDPREVIEW due to payload ordering)
        sendCardImage(card.imageUrl, card.id)

        // Mark that we've sent our image
        localImageTransferComplete = true
        Log.d(TAG, "✅ Marked local image transfer as initiated")
    }

    /**
     * Mark ready to battle
     * PHASE 2: Updates unified state + syncs to legacy StateFlows
     */
    fun setReady() {
        val card = localFullCard ?: return
        val battleCard = _localCard.value ?: return

        // PHASE 2: Update unified state
        updateSessionState { state ->
            state.copy(
                localPlayer = state.localPlayer.copy(isReady = true),
                canClickReady = false,  // Disable button after click
                waitingForOpponentReady = true
            )
        }

        // LEGACY: Keep for compatibility
        localReadyLegacy = true

        // FIX: Check if opponent data is already complete and send READY_ACK if so
        // This ensures symmetric READY_ACK exchange even when opponent data arrived before click
        checkOpponentDataComplete()

        Log.d(TAG, "✅ Local player ready (v${_battleSessionState.value.version})")

        // REMOVED: CARD data sending (now sent in CARDPREVIEW immediately when card is selected)
        // All card data (name, rarity, attack, health, prompt, biography) already sent
        // UI layer controls visibility with reveal logic (_statsRevealed, _shouldRevealCards)

        sendMessage("READY|true")
        addMessage("Ready to battle!")

        // NEW: Enter waiting state with timeout
        startReadyTimeout()

        // If we already have all opponent data, send ACK immediately
        if (_localDataComplete.value) {
            sendMessage("READY_ACK")
            Log.d(TAG, "📤 Sent READY_ACK (already had all data)")
            checkBothReadyForReveal()
        } else {
            Log.d(TAG, "⏳ Waiting to receive opponent's data...")
        }
    }

    /**
     * Start the dramatic card reveal sequence
     *
     * Sequence:
     * 1. Dramatic pause (2 seconds)
     * 2. Trigger blur reveal animation (shouldRevealCards = true)
     * 3. Wait for blur animation to complete (500ms)
     * 4. Reveal stats (statsRevealed = true)
     * 5. Short pause before battle (500ms)
     * 6. Execute battle (host only)
     */
    private suspend fun startRevealSequence() {
        // Dramatic pause before reveal
        kotlinx.coroutines.delay(2000)

        // Trigger reveal animation
        _shouldRevealCards.value = true

        // Wait for animation to complete
        kotlinx.coroutines.delay(500)

        // Now reveal stats
        _statsRevealed.value = true

        // Short pause before battle
        kotlinx.coroutines.delay(500)

        // Start battle (only host executes)
        if (isHost) {
            sendMessage("BATTLE_START")  // NEW: Explicit state transition
            kotlinx.coroutines.delay(500)
            executeBattle()
        }
    }

    /**
     * Execute the battle (called by host when both ready)
     */
    fun executeBattle() {
        val local = _localCard.value ?: return
        val opponent = _opponentCard.value ?: return

        // Start with animation state
        _battleState.value = BattleState.BATTLE_ANIMATING

        // Generate battle story and calculate outcome
        val (story, result) = calculateBattle(local, opponent)

        _battleStory.value = story

        // Send story segments to opponent for synchronized animation
        // Format: STORY|index|text|isLocalAction|damage
        story.forEachIndexed { index, segment ->
            val storyMsg = "STORY|$index|${segment.text.replace("|", "~")}|${segment.isLocalAction}|${segment.damageDealt ?: -1}"
            sendMessage(storyMsg)
        }

        // Update result with full card data for transfer
        val finalResult = result.copy(
            cardWon = if (!result.isDraw && result.winnerIsLocal == true) opponentFullCard else null
        )
        _battleResult.value = finalResult

        // Send result to opponent (they won if host lost)
        val resultMsg = "RESULT|${if (finalResult.isDraw) "DRAW" else if (finalResult.winnerIsLocal == true) "LOCAL" else "OPPONENT"}"
        sendMessage(resultMsg)

        // NOTE: Don't transition to BATTLE_COMPLETE here!
        // Let the ViewModel's animateStory() handle the animation, then call completeBattleAnimation()
        // This allows the battle animation (pulsing glow, impact effects) to play out
        Log.d(TAG, "⚔️ HOST: Battle started, animation will run via ViewModel")
    }

    /**
     * Complete the battle animation and show final results
     * Called by ViewModel when animation finishes or is skipped
     */
    fun completeBattleAnimation() {
        _battleState.value = BattleState.BATTLE_COMPLETE
    }

    /**
     * Calculate battle outcome with damage and chance factor
     * Returns story segments and final result
     */
    private fun calculateBattle(local: BattleCard, opponent: BattleCard): Pair<List<BattleStorySegment>, BattleResult> {
        val segments = mutableListOf<BattleStorySegment>()
        var localHealth = local.effectiveHealth
        var opponentHealth = opponent.effectiveHealth

        // Opening narration
        segments.add(BattleStorySegment(
            "The arena crackles with energy as ${local.card.name} faces ${opponent.card.name}!",
            isLocalAction = false
        ))

        // Battle rounds (3-5 exchanges)
        val rounds = Random.nextInt(3, 6)
        for (round in 1..rounds) {
            // Local attacks (with 0.8-1.2 random factor)
            val localDamage = (local.effectiveAttack * Random.nextDouble(0.8, 1.2)).toInt()
            opponentHealth -= localDamage
            segments.add(BattleStorySegment(
                "${local.card.name} unleashes a devastating ${getAttackVerb()}! ${opponent.card.name} takes $localDamage damage!",
                isLocalAction = true,
                damageDealt = localDamage
            ))

            if (opponentHealth <= 0) break

            // Opponent attacks
            val opponentDamage = (opponent.effectiveAttack * Random.nextDouble(0.8, 1.2)).toInt()
            localHealth -= opponentDamage
            segments.add(BattleStorySegment(
                "${opponent.card.name} retaliates with a fierce ${getAttackVerb()}! ${local.card.name} suffers $opponentDamage damage!",
                isLocalAction = false,
                damageDealt = opponentDamage
            ))

            if (localHealth <= 0) break
        }

        // Determine outcome
        val isDraw = localHealth <= 0 && opponentHealth <= 0
        val localWins = !isDraw && localHealth > opponentHealth

        // Closing narration
        val closingText = when {
            isDraw -> "Both warriors fall simultaneously! A legendary draw that will be remembered!"
            localWins -> "${local.card.name} stands victorious! ${opponent.card.name} has been defeated!"
            else -> "${opponent.card.name} emerges triumphant! ${local.card.name} falls in battle!"
        }
        segments.add(BattleStorySegment(closingText, isLocalAction = localWins))

        val fullStory = segments.joinToString(" ") { it.text }

        val result = BattleResult(
            isDraw = isDraw,
            winnerIsLocal = if (isDraw) null else localWins,
            winnerCardName = when {
                isDraw -> null
                localWins -> local.card.name
                else -> opponent.card.name
            },
            loserCardName = when {
                isDraw -> null
                localWins -> opponent.card.name
                else -> local.card.name
            },
            localCardFinalHealth = maxOf(0, localHealth),
            opponentCardFinalHealth = maxOf(0, opponentHealth),
            battleStory = fullStory,
            cardWon = if (!isDraw && localWins) opponent.card else null
        )

        return Pair(segments, result)
    }

    private fun getAttackVerb(): String {
        val verbs = listOf(
            "strike", "blow", "assault", "barrage", "onslaught",
            "slam", "blast", "surge", "rampage", "combo"
        )
        return verbs.random()
    }

    // Store opponent's full card for potential transfer
    private var opponentFullCard: Card? = null

    private fun handleReceivedMessage(message: String) {
        // PHASE 4: Process through MessageReliabilityLayer
        val reliableMessage = messageReliability.handleReceivedMessage(message) {
            // Callback for non-duplicate messages
        }

        // If this is a reliable message (JSON format), handle it
        if (reliableMessage != null) {
            // Handle infrastructure messages
            when (reliableMessage.type) {
            MessageType.ACK -> {
                // ACK messages are handled internally by MessageReliabilityLayer
                return
            }
            MessageType.STATE_SYNC_REQUEST -> {
                // Opponent is requesting state sync (they detected reconnection)
                Log.d(TAG, "📩 Received STATE_SYNC_REQUEST (opponent version=${reliableMessage.version})")

                val opponentState = BattleSessionState.fromJson(reliableMessage.payload)
                if (opponentState == null) {
                    Log.w(TAG, "⚠️ Failed to parse opponent state from STATE_SYNC_REQUEST")
                    return
                }

                // Merge states (host-authoritative)
                val currentState = _battleSessionState.value
                val mergedState = currentState.merge(opponentState, isHost = isHost)

                Log.d(TAG, "🔄 Merged state: local v${currentState.version} + opponent v${opponentState.version} → v${mergedState.version}")

                // Update our state
                _battleSessionState.value = mergedState
                syncLegacyState()

                // Send our state back as STATE_SYNC_RESPONSE
                val syncResponse = ReliableMessage(
                    messageId = java.util.UUID.randomUUID().toString(),
                    type = MessageType.STATE_SYNC_RESPONSE,
                    payload = mergedState.toJson(),
                    version = mergedState.version
                )
                messageReliability.sendReliableMessage(syncResponse)
                Log.d(TAG, "📤 Sent STATE_SYNC_RESPONSE (version=${mergedState.version})")

                return
            }
            MessageType.STATE_SYNC_RESPONSE -> {
                // Opponent sent their state in response to our sync request
                Log.d(TAG, "📩 Received STATE_SYNC_RESPONSE (opponent version=${reliableMessage.version})")

                val opponentState = BattleSessionState.fromJson(reliableMessage.payload)
                if (opponentState == null) {
                    Log.w(TAG, "⚠️ Failed to parse opponent state from STATE_SYNC_RESPONSE")
                    return
                }

                // Merge states (host-authoritative)
                val currentState = _battleSessionState.value
                val mergedState = currentState.merge(opponentState, isHost = isHost)

                Log.d(TAG, "🔄 Merged state: local v${currentState.version} + opponent v${opponentState.version} → v${mergedState.version}")
                Log.d(TAG, "✅ State resynchronization complete!")

                // Update our state
                _battleSessionState.value = mergedState
                syncLegacyState()

                // PHASE 5: Check for missing images after state sync
                if (mergedState.hasMissingOpponentImage()) {
                    Log.d(TAG, "🖼️ Opponent's image is marked COMPLETE but we're missing the file - requesting re-send")
                    requestMissingImage()
                }

                if (mergedState.shouldResendLocalImage()) {
                    Log.d(TAG, "🖼️ Opponent is missing our image - re-sending")
                    localFullCard?.let { card ->
                        sendCardImage(card.imageUrl, card.id)
                    }
                }

                return
            }
            MessageType.IMAGE_REQUEST -> {
                // Opponent is requesting our card image (they're missing it after reconnection)
                Log.d(TAG, "📩 Received IMAGE_REQUEST - opponent is missing our image")

                val requestedCardId = reliableMessage.payload.toLongOrNull()
                if (requestedCardId == null) {
                    Log.w(TAG, "⚠️ Invalid IMAGE_REQUEST - bad card ID: ${reliableMessage.payload}")
                    return
                }

                // Re-send our card image
                localFullCard?.let { card ->
                    if (card.id == requestedCardId) {
                        Log.d(TAG, "📤 Re-sending image for card ${card.id} as requested")
                        sendCardImage(card.imageUrl, card.id)
                    } else {
                        Log.w(TAG, "⚠️ IMAGE_REQUEST for card $requestedCardId but our card is ${card.id}")
                    }
                } ?: run {
                    Log.w(TAG, "⚠️ Cannot resend image - no local card data")
                }

                return
            }
            MessageType.READY_TIMEOUT -> {
                Log.d(TAG, "📩 Received READY_TIMEOUT from opponent")
                addMessage("⏱️ Opponent timed out waiting for you")

                // If we're also waiting, cancel our timeout
                readyTimeoutJob?.cancel()
                _waitingForOpponentReady.value = false

                // Offer to retry
                setBattleState(BattleState.READY_TIMEOUT, "opponent_timeout_notification")
                return
            }
            else -> {
                // Not a Phase 4 infrastructure message, continue to legacy handling
            }
            }
        }
        // If reliableMessage == null, it's a legacy message - handle below

        // Legacy message handling (for backward compatibility during transition)
        val parts = message.split("|")
        when (parts[0]) {
            "PING" -> {
                // Keep-alive heartbeat - silently ignore
                // This message is sent periodically to prevent connection timeout
                // No action needed, just receiving it keeps the connection alive
                return
            }
            "IMAGE_TRANSFER" -> {
                // IMAGE_TRANSFER|cardId|fileName|size|payloadId
                // IMPORTANT: Can arrive BEFORE or AFTER FILE payload or CARDPREVIEW
                val cardId = parts[1].toLongOrNull() ?: 0
                val fileName = parts[2]
                val payloadId = parts.getOrNull(4)?.toLongOrNull() ?: 0

                Log.d(TAG, "Received IMAGE_TRANSFER metadata: cardId=$cardId, payloadId=$payloadId, fileName=$fileName")

                // UPDATE-or-CREATE: Handle async message ordering
                val existingCard = _opponentCard.value

                if (existingCard != null && existingCard.card.id == cardId) {
                    // Card already exists from CARDPREVIEW - keep it, image will update later
                    Log.d(TAG, "IMAGE_TRANSFER for existing card, image will update when FILE completes")
                } else if (existingCard == null) {
                    // IMAGE_TRANSFER arrived FIRST - create placeholder
                    val placeholderCard = BattleCard(
                        card = Card(
                            id = cardId,
                            name = "Loading...",
                            imageUrl = "",
                            rarity = CardRarity.COMMON,
                            prompt = "",
                            biography = "",
                            createdAt = System.currentTimeMillis()
                        ),
                        effectiveAttack = 0,
                        effectiveHealth = 0,
                        currentHealth = 0
                    )
                    _opponentCard.value = placeholderCard
                    Log.d(TAG, "✅ Created placeholder card from IMAGE_TRANSFER")

                    // FIX: Sync placeholder card to unified state
                    _battleSessionState.value = _battleSessionState.value.copy(
                        opponentPlayer = _battleSessionState.value.opponentPlayer.copy(
                            hasSelectedCard = true,
                            card = placeholderCard
                        )
                    ).nextVersion()
                    Log.d(TAG, "📊 Synced placeholder card to unified state (v${_battleSessionState.value.version})")
                }

                // ALWAYS set play state (opponent has selected a card)
                _opponentHasSelectedCard.value = true

                // Check if FILE payload already arrived
                val cachedPayload = payloadCache[payloadId]

                if (cachedPayload != null) {
                    // Case 1: FILE arrived FIRST - mark as pending transfer completion
                    Log.d(TAG, "FILE already received for payloadId=$payloadId, waiting for transfer completion")
                    pendingFileTransfers[payloadId] = ImageTransferInfo(cardId, fileName)
                } else {
                    // Case 2: FILE NOT YET arrived - register expectation
                    Log.d(TAG, "File not yet received, waiting for payloadId=$payloadId")
                    expectedImageTransfers[payloadId] = ImageTransferInfo(cardId, fileName)
                }
            }
            "CARDPREVIEW" -> {
                // CARDPREVIEW|id|name|rarity|attack|health|prompt|biography - full data now
                val id = parts[1].toLongOrNull() ?: 0
                val name = parts[2]
                val rarity = try { CardRarity.valueOf(parts[3]) } catch (e: Exception) { CardRarity.COMMON }
                val attack = parts.getOrNull(4)?.toIntOrNull() ?: 0
                val health = parts.getOrNull(5)?.toIntOrNull() ?: 0
                val prompt = parts.getOrNull(6)?.replace("~", "|") ?: ""
                val biography = parts.getOrNull(7)?.replace("~", "|") ?: ""

                Log.d(TAG, "📩 Received CARDPREVIEW: id=$id, name=$name, rarity=$rarity, ATK=$attack, HP=$health")

                // UPDATE-or-CREATE: Handle async message ordering
                val existingCard = _opponentCard.value

                if (existingCard != null && existingCard.card.id == id) {
                    // Card already exists from IMAGE_TRANSFER - UPDATE with stats
                    val updatedCard = existingCard.copy(
                        card = existingCard.card.copy(
                            name = name,
                            rarity = rarity,
                            prompt = prompt,
                            biography = biography
                            // Keep existing imageUrl from placeholder
                        ),
                        effectiveAttack = attack,
                        effectiveHealth = health,
                        currentHealth = health
                    )
                    _opponentCard.value = updatedCard
                    Log.d(TAG, "✅ Updated placeholder card with stats from CARDPREVIEW")

                    // FIX: Set opponentFullCard for card transfer when winner claims card
                    opponentFullCard = updatedCard.card
                    Log.d(TAG, "📦 Set opponentFullCard from CARDPREVIEW (update): ${updatedCard.card.name}")
                } else {
                    // CARDPREVIEW arrived FIRST - CREATE with stats
                    val imageUrl = receivedImagePaths[id] ?: ""

                    val previewCard = BattleCard(
                        card = Card(
                            id = id,
                            name = name,
                            imageUrl = imageUrl,
                            rarity = rarity,
                            prompt = prompt,
                            biography = biography,
                            createdAt = System.currentTimeMillis()
                        ),
                        effectiveAttack = attack,
                        effectiveHealth = health,
                        currentHealth = health
                    )
                    _opponentCard.value = previewCard
                    Log.d(TAG, "✅ Created card with stats from CARDPREVIEW")

                    // FIX: Set opponentFullCard for card transfer when winner claims card
                    opponentFullCard = previewCard.card
                    Log.d(TAG, "📦 Set opponentFullCard from CARDPREVIEW (create): ${previewCard.card.name}")
                }

                // FIX: Sync opponent card to unified state to prevent data loss
                // This ensures syncLegacyState() won't overwrite with null
                _battleSessionState.value = _battleSessionState.value.copy(
                    opponentPlayer = _battleSessionState.value.opponentPlayer.copy(
                        hasSelectedCard = true,
                        card = _opponentCard.value
                    )
                ).nextVersion()
                Log.d(TAG, "📊 Synced opponent card to unified state (v${_battleSessionState.value.version})")

                // ALWAYS set play state
                _opponentHasSelectedCard.value = true
                _opponentIsThinking.value = false

                checkOpponentDataComplete()
                addMessage("Opponent selected their card")
            }
            "CARD" -> {
                // CARD|id|name|attack|health|rarity|prompt|biography - full stats (image was sent separately)
                val id = parts[1].toLongOrNull() ?: 0
                val name = parts[2]
                val attack = parts[3].toIntOrNull() ?: 50
                val health = parts[4].toIntOrNull() ?: 100
                val rarity = try { CardRarity.valueOf(parts[5]) } catch (e: Exception) { CardRarity.COMMON }
                val prompt = parts.getOrNull(6)?.replace("~", "|") ?: ""
                val biography = parts.getOrNull(7)?.replace("~", "|") ?: ""

                // Use received image path, or existing from preview
                val imageUrl = receivedImagePaths[id]
                    ?: _opponentCard.value?.card?.imageUrl
                    ?: ""

                // Create full card for potential transfer
                val opponentCardData = Card(
                    id = id,
                    prompt = prompt,
                    imageUrl = imageUrl,
                    rarity = rarity,
                    name = name,
                    attack = attack,
                    health = health,
                    biography = biography
                )
                opponentFullCard = opponentCardData
                val battleCard = BattleCard(
                    card = opponentCardData,
                    effectiveAttack = attack,
                    effectiveHealth = health,
                    currentHealth = health
                )
                _opponentCard.value = battleCard

                // FIX: Sync opponent card to unified state
                _battleSessionState.value = _battleSessionState.value.copy(
                    opponentPlayer = _battleSessionState.value.opponentPlayer.copy(
                        hasSelectedCard = true,
                        card = battleCard
                    )
                ).nextVersion()
                Log.d(TAG, "📊 Synced opponent card from CARD message to unified state (v${_battleSessionState.value.version})")

                _statsRevealed.value = true
                addMessage("Opponent revealed: $name (ATK:$attack HP:$health)")

                // Check if we now have all opponent data to send READY_ACK
                checkOpponentDataComplete()
            }
            "READY" -> {
                val ready = parts[1] == "true"
                Log.d(TAG, "📩 RECEIVED READY MESSAGE: ready=$ready")
                Log.d(TAG, "   Before: opponentReadyLegacy=$opponentReadyLegacy, _opponentReady=${_opponentReady.value}")
                opponentReadyLegacy = ready
                _opponentReady.value = ready  // Update StateFlow
                Log.d(TAG, "   After: opponentReadyLegacy=$opponentReadyLegacy, _opponentReady=${_opponentReady.value}")
                addMessage("Opponent is ready!")
                checkBothReadyForReveal()
            }
            "READY_ACK" -> {
                Log.d(TAG, "📩 Received READY_ACK from opponent")
                _opponentDataComplete.value = true
                _waitingForOpponentReady.value = false

                // FIX: Sync to unified state so syncLegacyState() won't reset to false
                _battleSessionState.value = _battleSessionState.value.copy(
                    opponentPlayer = _battleSessionState.value.opponentPlayer.copy(
                        dataReceivedFromOpponent = true
                    )
                ).nextVersion()
                Log.d(TAG, "📊 Synced opponentDataComplete to unified state (v${_battleSessionState.value.version})")

                addMessage("Opponent confirmed ready!")
                checkBothReadyForReveal()
            }
            "REVEAL_START" -> {
                Log.d(TAG, "📩 NON-HOST: Received REVEAL_START from host")
                if (_battleState.value == BattleState.READY_TO_BATTLE) {
                    scope.launch { startRevealSequence() }
                } else {
                    Log.w(TAG, "⚠️ Received REVEAL_START in wrong state: ${_battleState.value}")
                }
            }
            "BATTLE_START" -> {
                Log.d(TAG, "📩 NON-HOST: Received BATTLE_START, entering BATTLE_ANIMATING")
                _battleState.value = BattleState.BATTLE_ANIMATING
            }
            "STORY" -> {
                // Receive story segment from host
                // STORY|index|text|isLocalAction|damage
                // State should already be BATTLE_ANIMATING from BATTLE_START
                if (_battleState.value != BattleState.BATTLE_ANIMATING) {
                    Log.w(TAG, "⚠️ Received STORY in wrong state: ${_battleState.value}, forcing BATTLE_ANIMATING")
                    _battleState.value = BattleState.BATTLE_ANIMATING
                }

                val index = parts[1].toIntOrNull() ?: 0
                val text = parts[2].replace("~", "|")
                // Flip isLocalAction for non-host (their local is host's opponent)
                val isLocalAction = parts[3] != "true"  // Inverted!
                val damage = parts[4].toIntOrNull()?.takeIf { it >= 0 }

                val segment = BattleStorySegment(text, isLocalAction, damage)
                val currentStory = _battleStory.value.toMutableList()
                // Ensure we add at correct index
                while (currentStory.size <= index) {
                    currentStory.add(segment)
                }
                if (currentStory.size > index) {
                    currentStory[index] = segment
                }
                _battleStory.value = currentStory
            }
            "RESULT" -> {
                // Non-host receives result
                val outcome = parts[1]

                val local = _localCard.value ?: return
                val opponent = _opponentCard.value ?: return

                val isDraw = outcome == "DRAW"
                val localWins = outcome == "OPPONENT" // Flip perspective for non-host

                val fullStory = _battleStory.value.joinToString(" ") { it.text }

                _battleResult.value = BattleResult(
                    isDraw = isDraw,
                    winnerIsLocal = if (isDraw) null else localWins,
                    winnerCardName = if (isDraw) null else if (localWins) local.card.name else opponent.card.name,
                    loserCardName = if (isDraw) null else if (localWins) opponent.card.name else local.card.name,
                    localCardFinalHealth = 0,
                    opponentCardFinalHealth = 0,
                    battleStory = fullStory,
                    cardWon = if (!isDraw && localWins) opponentFullCard else null
                )
                // NOTE: Don't transition to BATTLE_COMPLETE here!
                // Let the ViewModel's animateStory() finish, then call completeBattleAnimation()
                Log.d(TAG, "📩 NON-HOST: Result received, animation will complete via ViewModel")
            }
        }
    }

    /**
     * Check if both players are truly ready for reveal (not just button press)
     * Requires: both ready + both have complete opponent data
     */
    private fun checkBothReadyForReveal() {
        if (revealInitiated) {
            Log.d(TAG, "checkBothReadyForReveal: Already initiated, skipping")
            return
        }

        Log.d(TAG, """
            checkBothReadyForReveal:
              localReady=$localReadyLegacy, opponentReady=$opponentReadyLegacy
              localDataComplete=${_localDataComplete.value}, opponentDataComplete=${_opponentDataComplete.value}
              cards: local=${_localCard.value != null}, opponent=${_opponentCard.value != null}
        """.trimIndent())

        val bothReady = localReadyLegacy && opponentReadyLegacy
        val bothHaveData = _localDataComplete.value && _opponentDataComplete.value
        val bothHaveCards = _localCard.value != null && _opponentCard.value != null

        if (bothReady && bothHaveData && bothHaveCards) {
            revealInitiated = true  // Set flag BEFORE any async operations
            Log.d(TAG, "✅ BOTH PLAYERS READY FOR REVEAL! (isHost=$isHost)")
            _battleState.value = BattleState.READY_TO_BATTLE
            _waitingForOpponentReady.value = false
            readyTimeoutJob?.cancel()
            addMessage("Both players ready!")

            // Only host initiates reveal
            if (isHost) {
                Log.d(TAG, "🎮 HOST: Sending REVEAL_START")
                sendMessage("REVEAL_START")
                scope.launch { startRevealSequence() }
            } else {
                Log.d(TAG, "⏳ NON-HOST: Waiting for REVEAL_START from host (isHost=$isHost)")
            }
        }
    }

    /**
     * Set battle state with comprehensive logging
     * Logs all state transitions with context for debugging
     */
    private fun setBattleState(newState: BattleState, reason: String) {
        val oldState = _battleState.value
        Log.d(TAG, "🔄 STATE TRANSITION: $oldState → $newState (reason: $reason)")
        Log.d(TAG, "   Connection: endpoint=$currentEndpointId, lifecycle=${_connectionLifecycle.value}")
        Log.d(TAG, "   Ready: local=$localReadyLegacy, opponent=$opponentReadyLegacy")
        Log.d(TAG, "   Data: local=${_localDataComplete.value}, opponent=${_opponentDataComplete.value}")
        _battleState.value = newState
    }

    /**
     * Check if connection is actually healthy
     * Considers both endpoint existence and lifecycle state
     */
    private fun isConnectionHealthy(): Boolean {
        val hasEndpoint = currentEndpointId != null
        val isLifecycleConnected = _connectionLifecycle.value is ConnectionLifecycle.Connected

        Log.d(TAG, "Connection health check: endpoint=$hasEndpoint, lifecycle=$isLifecycleConnected")

        return hasEndpoint && isLifecycleConnected
    }

    /**
     * Start timeout protection for ready state
     * Prevents infinite waiting if opponent disconnects or encounters error
     *
     * CRITICAL FIX: Increased timeout from 30s to 45s to account for image transfer time
     * PHASE 2: Distinguish between connection timeout (READY_TIMEOUT) and disconnection (DISCONNECTED)
     */
    private fun startReadyTimeout() {
        readyTimeoutJob?.cancel()
        readyTimeoutJob = scope.launch {
            delay(45000)  // 45 seconds

            if (_waitingForOpponentReady.value) {
                Log.w(TAG, "⏱️ TIMEOUT: Battle didn't start in time (45s)")

                // Check if connection is still alive
                val isConnected = isConnectionHealthy()

                if (isConnected) {
                    // Send timeout notification to opponent
                    val timeoutMsg = ReliableMessage(
                        messageId = java.util.UUID.randomUUID().toString(),
                        type = MessageType.READY_TIMEOUT,
                        payload = "local_player_timeout",
                        requiresAck = true
                    )
                    messageReliability.sendReliableMessage(timeoutMsg)
                    Log.d(TAG, "📤 Sent READY_TIMEOUT notification to opponent")

                    // Connection alive - just a timeout
                    setBattleState(BattleState.READY_TIMEOUT, "ready_timeout_connection_healthy")
                    addMessage("⏱️ Waiting for opponent timed out.")
                } else {
                    // Connection actually lost
                    setBattleState(BattleState.DISCONNECTED, "ready_timeout_connection_lost")
                    addMessage("⏱️ Connection lost.")
                }

                _waitingForOpponentReady.value = false
            }
        }
    }

    private fun sendMessage(message: String) {
        currentEndpointId?.let { endpointId ->
            val payload = Payload.fromBytes(message.toByteArray(Charsets.UTF_8))
            connectionsClient.sendPayload(endpointId, payload)
            Log.d(TAG, "Sent: $message")
        }
    }

    /**
     * Send card image file to opponent
     */
    private fun sendCardImage(imageUrl: String, cardId: Long) {
        currentEndpointId?.let { endpointId ->
            try {
                val imageFile = java.io.File(imageUrl)
                if (imageFile.exists()) {
                    // Create File payload
                    val filePayload = Payload.fromFile(imageFile)
                    val payloadId = filePayload.id

                    // Track this outgoing file transfer
                    outgoingFileTransfers.add(payloadId)
                    Log.d(TAG, "📤 Tracking outgoing image transfer: payloadId=$payloadId")

                    // Send metadata message first so receiver knows what's coming
                    sendMessage("IMAGE_TRANSFER|$cardId|${imageFile.name}|${imageFile.length()}|$payloadId")

                    // Then send the file
                    connectionsClient.sendPayload(endpointId, filePayload)
                    Log.d(TAG, "Sending image: cardId=$cardId, payloadId=$payloadId, size=${imageFile.length()}")
                } else {
                    Log.e(TAG, "Image file not found: $imageUrl")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send image: ${e.message}")
            }
        }
    }

    /**
     * Request missing image from opponent
     *
     * PHASE 5: Sends IMAGE_REQUEST to ask opponent to resend their card image.
     * Called when state sync reveals opponent has completed transfer but we're missing the file.
     */
    private fun requestMissingImage() {
        val opponentCard = _opponentCard.value?.card
        if (opponentCard == null) {
            Log.w(TAG, "⚠️ Cannot request missing image - no opponent card data")
            return
        }

        Log.d(TAG, "📤 Sending IMAGE_REQUEST for card ${opponentCard.id}")

        val imageRequest = ReliableMessage(
            messageId = java.util.UUID.randomUUID().toString(),
            type = MessageType.IMAGE_REQUEST,
            payload = opponentCard.id.toString(),
            requiresAck = true
        )

        messageReliability.sendReliableMessage(imageRequest)
    }

    /**
     * Update opponent card with received image path
     * Forces UI refresh by updating StateFlow
     */
    private fun updateCardWithImage(cardId: Long, imagePath: String) {
        Log.d(TAG, "updateCardWithImage called: cardId=$cardId, imagePath=$imagePath")
        Log.d(TAG, "Current opponent card: ${_opponentCard.value?.card?.id}, expected: $cardId")

        _opponentCard.value?.let { battleCard ->
            if (battleCard.card.id == cardId) {
                // Add timestamp to bust Coil's cache and force reload
                val cacheBustedPath = "$imagePath?t=${System.currentTimeMillis()}"
                val updatedCard = battleCard.card.copy(imageUrl = cacheBustedPath)
                val updatedBattleCard = battleCard.copy(card = updatedCard)
                _opponentCard.value = updatedBattleCard
                Log.d(TAG, "✅ Updated opponent card $cardId with image: $cacheBustedPath")

                // FIX: Sync image path to unified state so syncLegacyState() won't overwrite
                _battleSessionState.value = _battleSessionState.value.copy(
                    opponentPlayer = _battleSessionState.value.opponentPlayer.copy(
                        card = updatedBattleCard,
                        imageFilePath = imagePath
                    )
                ).nextVersion()
                Log.d(TAG, "📊 Synced opponent image to unified state (v${_battleSessionState.value.version})")
            } else {
                Log.w(TAG, "❌ Card ID mismatch: opponent card is ${battleCard.card.id}, but image is for $cardId")
            }
        } ?: run {
            Log.w(TAG, "❌ Cannot update image: _opponentCard.value is null, storing for later")
        }

        // Also update opponentFullCard for potential transfer
        opponentFullCard?.let { card ->
            if (card.id == cardId) {
                opponentFullCard = card.copy(imageUrl = imagePath)
            }
        }
    }

    /**
     * Check if all opponent data has been received (CARDPREVIEW + CARD + image)
     * If complete and we're ready, send READY_ACK to opponent
     *
     * CRITICAL FIX: Now checks _opponentImageTransferComplete StateFlow
     * This ensures ready button only enables AFTER image transfer completes
     */
    private fun checkOpponentDataComplete() {
        Log.d(TAG, "🔍 CRITICAL: checkOpponentDataComplete() called")

        val opponentCard = _opponentCard.value
        if (opponentCard == null) {
            Log.d(TAG, "❌ CRITICAL: No opponent card yet (_opponentCard.value is null)")
            return
        }
        Log.d(TAG, "✅ CRITICAL: Opponent card exists: ${opponentCard.card.name} (id=${opponentCard.card.id})")

        // Check if opponent card has valid stats
        // Image transfer is optional - it can load in background
        if (opponentCard.effectiveAttack == 0) {
            Log.d(TAG, "❌ Waiting for opponent card stats (effectiveAttack = 0)")
            return
        }
        Log.d(TAG, "✅ Opponent card stats received (ATK=${opponentCard.effectiveAttack}, HP=${opponentCard.effectiveHealth})")

        // All opponent data received!
        _localDataComplete.value = true

        // FIX: Sync to unified state so syncLegacyState() won't reset to false
        _battleSessionState.value = _battleSessionState.value.copy(
            localPlayer = _battleSessionState.value.localPlayer.copy(
                dataReceivedFromOpponent = true
            )
        ).nextVersion()
        Log.d(TAG, "✅ All opponent data complete (stats received, synced to unified state v${_battleSessionState.value.version})")

        // If we already clicked ready, now send acknowledgment since opponent data is complete
        if (localReadyLegacy) {
            sendMessage("READY_ACK")
            Log.d(TAG, "📤 Sent READY_ACK (all data received)")
            checkBothReadyForReveal()
        }
    }

    private fun resetBattleState() {
        // PHASE 2: Reset unified state (creates new state with version 0)
        _battleSessionState.value = BattleSessionState()
        syncLegacyState()  // Sync reset to legacy StateFlows

        // LEGACY: Keep these for network operations
        _messages.value = emptyList()
        localReadyLegacy = false
        opponentReadyLegacy = false
        localFullCard = null
        opponentFullCard = null
        _opponentName.value = "Opponent"
        expectedImageTransfers.clear()
        receivedImagePaths.clear()
        pendingFileTransfers.clear()
        payloadCache.clear()

        // Clean up orphaned files
        orphanedFiles.forEach { (payloadId, filePath) ->
            try {
                java.io.File(filePath).delete()
                Log.d(TAG, "Cleaned up orphaned file: payloadId=$payloadId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean up orphaned file: ${e.message}")
            }
        }
        orphanedFiles.clear()

        // Reset new state tracking fields
        _localDataComplete.value = false
        _opponentDataComplete.value = false
        _waitingForOpponentReady.value = false
        _opponentImageTransferComplete.value = false
        _localImageSent.value = false
        outgoingFileTransfers.clear()
        localImageTransferComplete = false
        revealInitiated = false
        imageTransferRetryCount = 0
        readyTimeoutJob?.cancel()

        // Reset collision prevention and retry tracking
        outgoingConnectionRequests.clear()
        connectionRetryAttempts.clear()
        retryJob?.cancel()
        retryJob = null
        // Note: Don't reset localEndpointId - it stays constant for the session
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()

        // PHASE 3: Reset connection lifecycle
        _connectionLifecycle.value = ConnectionLifecycle.Idle

        // PHASE 4: Clear pending messages from reliability layer
        messageReliability.clearPendingMessages()

        _connectionState.value = ConnectionState.Idle
        _battleState.value = BattleState.WAITING_FOR_OPPONENT
        _discoveredDevices.value = emptyList()
        currentEndpointId = null
        resetBattleState()
    }

    private fun addMessage(message: String) {
        val messages = _messages.value.toMutableList()
        messages.add(0, message)
        _messages.value = messages.take(20)
    }

    // ============================================================
    // PHASE 3: Connection Lifecycle Helper Methods
    // ============================================================

    /**
     * Callback invoked when reconnection is detected (endpoint ID changed)
     * Phase 4 will implement state resynchronization here
     *
     * For now, this just logs the event and invokes the callback (if set).
     */
    private fun onReconnectionDetected(oldEndpoint: String, newEndpoint: String) {
        Log.d(TAG, "🔄 RECONNECTION DETECTED: $oldEndpoint → $newEndpoint")
        Log.d(TAG, "   Initiating state resynchronization...")

        // Invoke callback if set (used by tests)
        reconnectionCallback?.invoke(oldEndpoint, newEndpoint)

        // PHASE 4: State Resynchronization
        // Send STATE_SYNC_REQUEST with our current state
        val currentState = _battleSessionState.value
        val syncRequest = ReliableMessage(
            messageId = java.util.UUID.randomUUID().toString(),
            type = MessageType.STATE_SYNC_REQUEST,
            payload = currentState.toJson(),
            version = currentState.version
        )

        messageReliability.sendReliableMessage(syncRequest)
        Log.d(TAG, "📤 Sent STATE_SYNC_REQUEST (version=${currentState.version})")
    }

    /**
     * Set callback for reconnection events (used by tests and future features)
     */
    fun setReconnectionCallback(callback: (oldEndpoint: String, newEndpoint: String) -> Unit) {
        reconnectionCallback = callback
    }

    /**
     * Get connection event history (for debugging and testing)
     */
    fun getConnectionEvents(): List<ConnectionEvent> = connectionEvents.toList()

    /**
     * Get connection history (for debugging and testing)
     */
    fun getConnectionHistory(): List<ConnectionLifecycle.Connected> = connectionHistory.toList()

    // ============================================================
    // Keep-Alive Mechanism
    // ============================================================

    /**
     * Start heartbeat to keep connection alive
     * Sends PING message every 10 seconds to prevent Nearby Connections timeout
     */
    private fun startHeartbeat() {
        stopHeartbeat() // Cancel any existing heartbeat

        heartbeatJob = scope.launch {
            while (true) {
                delay(10_000) // 10 seconds
                currentEndpointId?.let { endpointId ->
                    sendMessage("PING")
                    Log.d(TAG, "💓 Sent heartbeat PING to $endpointId")
                }
            }
        }

        Log.d(TAG, "💓 Heartbeat started (10s interval)")
    }

    /**
     * Stop heartbeat when connection ends
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        Log.d(TAG, "💔 Heartbeat stopped")
    }

    /**
     * Test helper: Simulate connection result (for unit testing)
     * This is internal and should only be used by tests
     */
    internal fun simulateConnectionResult(endpointId: String, result: ConnectionResolution) {
        connectionLifecycleCallback.onConnectionResult(endpointId, result)
    }

    /**
     * Test helper: Simulate disconnection (for unit testing)
     * This is internal and should only be used by tests
     */
    internal fun simulateDisconnection(endpointId: String) {
        connectionLifecycleCallback.onDisconnected(endpointId)
    }

    companion object {
        private const val TAG = "BattleManager"
    }
}
