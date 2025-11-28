package com.rotdex.data.manager

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.rotdex.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    // Connection lifecycle callbacks
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with: ${info.endpointName}")
            addMessage("Connection initiated with ${info.endpointName}")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connection successful!")
                    currentEndpointId = endpointId
                    _connectionState.value = ConnectionState.Connected(endpointId)
                    _battleState.value = BattleState.CARD_SELECTION
                    _opponentIsThinking.value = true  // Opponent is now selecting their card
                    addMessage("Connected! Select your card for battle.")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    _connectionState.value = ConnectionState.Error("Connection rejected")
                    addMessage("Connection rejected")
                }
                else -> {
                    _connectionState.value = ConnectionState.Error("Connection failed")
                    addMessage("Connection failed")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from: $endpointId")
            _connectionState.value = ConnectionState.Disconnected
            _battleState.value = BattleState.DISCONNECTED
            currentEndpointId = null
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
            val devices = _discoveredDevices.value.toMutableList()
            devices.add("${info.endpointName}|$endpointId")
            _discoveredDevices.value = devices
            Log.d(TAG, "✅ discoveredDevices updated: ${_discoveredDevices.value.size} devices")
            addMessage("Found: ${info.endpointName}")
        }

        override fun onEndpointLost(endpointId: String) {
            val devices = _discoveredDevices.value.filterNot { it.contains(endpointId) }
            _discoveredDevices.value = devices
            Log.d(TAG, "🔴 Endpoint lost, discoveredDevices now: ${_discoveredDevices.value.size} devices")
        }
    }

    /**
     * Start as host (advertise)
     */
    fun startAsHost(name: String) {
        playerName = name
        isHost = true
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
     */
    fun startAutoDiscovery(name: String) {
        val effectiveName = name.ifEmpty { "player-${System.currentTimeMillis() % 10000}" }
        playerName = effectiveName
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
     * Connect to a discovered host
     */
    fun connectToHost(endpointId: String) {
        _connectionState.value = ConnectionState.Connecting
        addMessage("Connecting...")

        connectionsClient.requestConnection(playerName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { e ->
                _connectionState.value = ConnectionState.Error("Failed: ${e.message}")
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
     */
    fun selectCard(card: Card) {
        val battleCard = BattleCard.fromCard(card)
        _localCard.value = battleCard
        localFullCard = card
        addMessage("Selected: ${card.name}")

        // IMPORTANT: Send CARDPREVIEW first, then image
        // This ensures opponent creates _opponentCard before image arrives
        // Format: CARDPREVIEW|id|name|rarity
        val previewData = listOf(
            "CARDPREVIEW",
            card.id.toString(),
            card.name,
            card.rarity.name
        ).joinToString("|")
        sendMessage(previewData)
        Log.d(TAG, "📤 Sent CARDPREVIEW for card ${card.id} (${card.name})")

        // Then send card image (will arrive separately, possibly before CARDPREVIEW due to payload ordering)
        sendCardImage(card.imageUrl, card.id)

        // Mark that we've sent our image
        localImageTransferComplete = true
        Log.d(TAG, "✅ Marked local image transfer as initiated")
    }

    /**
     * Mark ready to battle
     */
    fun setReady() {
        val card = localFullCard ?: return
        val battleCard = _localCard.value ?: return

        _localReady.value = true
        _canClickReady.value = false  // Disable button after click
        localReadyLegacy = true

        // Send full card data (with stats) for battle
        // Format: CARD|id|name|attack|health|rarity|prompt|biography (image was sent earlier)
        val cardData = listOf(
            "CARD",
            card.id.toString(),
            card.name,
            battleCard.effectiveAttack.toString(),
            battleCard.effectiveHealth.toString(),
            card.rarity.name,
            card.prompt.replace("|", "~"),
            card.biography.replace("|", "~")
        ).joinToString("|")
        sendMessage(cardData)

        sendMessage("READY|true")
        addMessage("Ready to battle!")

        // NEW: Enter waiting state with timeout
        _waitingForOpponentReady.value = true
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

        // Note: State will transition to BATTLE_COMPLETE when animation finishes
        // or when user skips (controlled by ViewModel)
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
        val parts = message.split("|")
        when (parts[0]) {
            "IMAGE_TRANSFER" -> {
                // IMAGE_TRANSFER|cardId|fileName|size|payloadId
                // IMPORTANT: Can arrive BEFORE or AFTER FILE payload
                val cardId = parts[1].toLongOrNull() ?: 0
                val fileName = parts[2]
                val payloadId = parts.getOrNull(4)?.toLongOrNull() ?: 0

                Log.d(TAG, "Received IMAGE_TRANSFER metadata: cardId=$cardId, payloadId=$payloadId, fileName=$fileName")

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
                // CARDPREVIEW|id|name|rarity - no stats yet, image will arrive separately
                val id = parts[1].toLongOrNull() ?: 0
                val name = parts[2]
                val rarity = try { CardRarity.valueOf(parts[3]) } catch (e: Exception) { CardRarity.COMMON }

                Log.d(TAG, "📩 Received CARDPREVIEW: id=$id, name=$name, rarity=$rarity")

                // Check if we already have the full CARD data - don't overwrite with preview
                if (_opponentCard.value != null && _opponentCard.value!!.effectiveAttack > 0) {
                    Log.d(TAG, "Ignoring CARDPREVIEW - already have full CARD data with stats")
                    return
                }

                // Check if we already have a received image for this card
                val existingImagePath = receivedImagePaths[id]
                Log.d(TAG, "Available received images: ${receivedImagePaths.keys}")

                // Create preview card with placeholder stats (will be updated by CARD message)
                val imageUrl = if (existingImagePath != null) {
                    Log.d(TAG, "✅ Received CARDPREVIEW with existing image: $existingImagePath")
                    "$existingImagePath?t=${System.currentTimeMillis()}"
                } else {
                    ""
                }

                val previewCard = BattleCard(
                    card = Card(
                        id = id,
                        name = name,
                        imageUrl = imageUrl,
                        rarity = rarity,
                        prompt = "",
                        biography = "",
                        createdAt = System.currentTimeMillis()
                    ),
                    effectiveAttack = 0,
                    effectiveHealth = 0,
                    currentHealth = 0
                )

                _opponentCard.value = previewCard
                _opponentIsThinking.value = false
                Log.d(TAG, "✅ Set _opponentCard to card $id (without stats yet)")

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
                _opponentCard.value = BattleCard(
                    card = opponentCardData,
                    effectiveAttack = attack,
                    effectiveHealth = health,
                    currentHealth = health
                )
                _statsRevealed.value = true
                addMessage("Opponent revealed: $name (ATK:$attack HP:$health)")
            }
            "READY" -> {
                val ready = parts[1] == "true"
                opponentReadyLegacy = ready
                _opponentReady.value = ready  // Update StateFlow
                addMessage("Opponent is ready!")
                checkBothReadyForReveal()
            }
            "READY_ACK" -> {
                Log.d(TAG, "📩 Received READY_ACK from opponent")
                _opponentDataComplete.value = true
                _waitingForOpponentReady.value = false
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
                _battleState.value = BattleState.BATTLE_COMPLETE
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
            Log.d(TAG, "✅ BOTH PLAYERS READY FOR REVEAL!")
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
                Log.d(TAG, "⏳ NON-HOST: Waiting for REVEAL_START from host")
            }
        }
    }

    /**
     * Start timeout protection for ready state
     * Prevents infinite waiting if opponent disconnects or encounters error
     *
     * CRITICAL FIX: Increased timeout from 30s to 45s to account for image transfer time
     */
    private fun startReadyTimeout() {
        readyTimeoutJob?.cancel()
        readyTimeoutJob = scope.launch {
            kotlinx.coroutines.delay(45000)  // 45 seconds (increased from 30s)

            // Only timeout if still waiting (not if battle has started)
            if (_waitingForOpponentReady.value) {
                Log.w(TAG, "⏱️ TIMEOUT: Battle didn't start in time (45s)")
                addMessage("⏱️ Connection timeout. Please retry.")
                _battleState.value = BattleState.DISCONNECTED
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
                _opponentCard.value = battleCard.copy(card = updatedCard)
                Log.d(TAG, "✅ Updated opponent card $cardId with image: $cacheBustedPath")
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

        // CRITICAL FIX: Check StateFlow instead of private var
        if (!_opponentImageTransferComplete.value) {
            Log.d(TAG, "❌ CRITICAL: Image transfer not complete yet (_opponentImageTransferComplete = false)")
            return
        }
        Log.d(TAG, "✅ CRITICAL: Image transfer complete (_opponentImageTransferComplete = true)")

        if (opponentCard.effectiveAttack == 0) {
            Log.d(TAG, "❌ CRITICAL: Waiting for CARD with full stats (effectiveAttack = 0)")
            return
        }
        Log.d(TAG, "✅ CRITICAL: Full stats received (ATK=${opponentCard.effectiveAttack}, HP=${opponentCard.effectiveHealth})")

        // All data received!
        _localDataComplete.value = true
        Log.d(TAG, "✅✅✅ CRITICAL: ALL OPPONENT DATA COMPLETE!")
        Log.d(TAG, "✅✅✅ CRITICAL: Setting _localDataComplete = true")

        // CRITICAL FIX: Now enable the ready button since all opponent data is received
        if (!localReadyLegacy) {
            _canClickReady.value = true
            Log.d(TAG, "✅✅✅ CRITICAL: ENABLING READY BUTTON (_canClickReady = true)")
        } else {
            Log.d(TAG, "⏭️ CRITICAL: Already ready (localReadyLegacy = true), skipping button enable")
        }

        // If we already clicked ready, now send acknowledgment
        if (localReadyLegacy) {
            sendMessage("READY_ACK")
            Log.d(TAG, "📤 CRITICAL: Sent READY_ACK (all data received)")
            checkBothReadyForReveal()
        } else {
            Log.d(TAG, "⏭️ CRITICAL: Not ready yet, waiting for user to click ready button")
        }
    }

    private fun resetBattleState() {
        _localCard.value = null
        _opponentCard.value = null
        _battleStory.value = emptyList()
        _battleResult.value = null
        _messages.value = emptyList()
        localReadyLegacy = false
        opponentReadyLegacy = false
        _localReady.value = false
        _opponentReady.value = false
        _canClickReady.value = false  // CRITICAL FIX: Reset to false, enabled when opponent data complete
        _opponentIsThinking.value = false
        localFullCard = null
        opponentFullCard = null
        _statsRevealed.value = false
        _shouldRevealCards.value = false
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
        localImageTransferComplete = false
        revealInitiated = false
        imageTransferRetryCount = 0
        readyTimeoutJob?.cancel()
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
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

    companion object {
        private const val TAG = "BattleManager"
    }
}
